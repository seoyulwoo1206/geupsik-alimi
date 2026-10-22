package com.geupsik.alimi.data.repository

import com.geupsik.alimi.data.local.MealDao
import com.geupsik.alimi.data.local.MealEntity
import com.geupsik.alimi.data.local.WeekFetchEntity
import com.geupsik.alimi.data.model.DayMeals
import com.geupsik.alimi.data.model.Meal
import com.geupsik.alimi.data.model.School
import com.geupsik.alimi.data.remote.MealRow
import com.geupsik.alimi.data.remote.NeisApi
import com.geupsik.alimi.data.remote.NeisPage
import com.geupsik.alimi.data.remote.NeisParser
import com.geupsik.alimi.domain.MealDates
import com.geupsik.alimi.domain.MenuParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * 급식은 일주일치(월~일)를 한 번에 받아 Room에 저장하고,
 * 화면과 알림은 항상 Room에서 읽는다. 그래서 인터넷이 없어도 받아 둔 메뉴는 보인다.
 */
class MealRepository(
    private val api: NeisApi,
    private val dao: MealDao,
    private val json: Json,
    private val apiKey: String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val isSampleMode: Boolean get() = apiKey.isBlank()

    /** 같은 주를 동시에 두 번 받지 않도록 한 번에 하나씩만 받는다. */
    private val refreshLock = Mutex()

    fun observeDay(school: School, date: LocalDate): Flow<DayMeals> {
        val key = school.cacheKey
        return combine(
            dao.observeDay(key, MealDates.toYmd(date)),
            dao.observeWeekFetch(key, MealDates.toYmd(MealDates.weekStart(date))),
        ) { meals, fetch -> DayMeals(meals.map { it.toMeal() }, weekFetched = fetch != null) }
    }

    suspend fun getDay(school: School, date: LocalDate): DayMeals {
        val key = school.cacheKey
        val meals = dao.getDay(key, MealDates.toYmd(date))
        val fetch = dao.getWeekFetch(key, MealDates.toYmd(MealDates.weekStart(date)))
        return DayMeals(meals.map { it.toMeal() }, weekFetched = fetch != null)
    }

    /**
     * date가 들어 있는 주를 받아 온다. 이미 받아 둔 지 얼마 안 됐으면 건너뛴다.
     * 실패하면 예외를 던지고, 기존 캐시는 그대로 남는다.
     */
    suspend fun refreshWeek(school: School, date: LocalDate, force: Boolean = false) {
        val key = school.cacheKey
        val from = MealDates.weekStart(date)
        val to = MealDates.weekEnd(date)
        val fromYmd = MealDates.toYmd(from)
        val toYmd = MealDates.toYmd(to)

        refreshLock.withLock {
            val previous = dao.getWeekFetch(key, fromYmd)
            if (!force && previous != null && isFresh(previous, weekEnd = to)) return

            val rows = fetchRange(school, from, to)
            val meals = rows
                .filter { it.date in fromYmd..toYmd }
                .mapNotNull { it.toEntity(key) }
            dao.replaceWeek(key, fromYmd, toYmd, meals, WeekFetchEntity(key, fromYmd, now()))
            pruneOldCache(from)
        }
    }

    /** 지난주 이전은 바뀔 일이 거의 없으니 한 번 받으면 끝, 이번 주 이후는 몇 시간마다 다시 받는다. */
    private fun isFresh(fetch: WeekFetchEntity, weekEnd: LocalDate): Boolean {
        if (weekEnd.isBefore(LocalDate.now())) return true
        return now() - fetch.fetchedAtMillis < REFETCH_AFTER_MILLIS
    }

    private suspend fun fetchRange(school: School, from: LocalDate, to: LocalDate): List<MealRow> {
        val first = fetchPage(school, from, to, pageIndex = 1)
        if (first.totalCount <= first.rows.size) return first.rows

        if (!isSampleMode) {
            val all = first.rows.toMutableList()
            var pageIndex = 2
            while (all.size < first.totalCount) {
                val page = fetchPage(school, from, to, pageIndex++)
                if (page.rows.isEmpty()) break
                all += page.rows
            }
            return all
        }

        // 샘플 모드는 페이지를 넘겨도 늘 처음 5개만 준다. 하루씩 나눠 받으면 하루 최대 3끼라 다 받을 수 있다.
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .toList()
            .flatMap { day -> fetchPage(school, day, day, pageIndex = 1).rows }
    }

    private suspend fun fetchPage(
        school: School,
        from: LocalDate,
        to: LocalDate,
        pageIndex: Int,
    ): NeisPage<MealRow> {
        val response = api.mealServiceDietInfo(
            key = apiKey.ifBlank { null },
            officeCode = school.officeCode,
            schoolCode = school.schoolCode,
            fromDate = MealDates.toYmd(from),
            toDate = MealDates.toYmd(to),
            pageIndex = pageIndex,
        )
        return NeisParser.parse(response, "mealServiceDietInfo", MealRow.serializer(), json)
    }

    private suspend fun pruneOldCache(currentWeekStart: LocalDate) {
        val before = MealDates.toYmd(currentWeekStart.minusWeeks(KEEP_WEEKS))
        dao.deleteMealsBefore(before)
        dao.deleteWeekFetchesBefore(before)
    }

    private fun MealRow.toEntity(schoolKey: String): MealEntity? {
        val code = mealCode.trim().toIntOrNull() ?: return null
        val raw = dishes?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return MealEntity(
            schoolKey = schoolKey,
            date = date.trim(),
            mealCode = code,
            mealName = mealName?.trim()?.ifEmpty { null } ?: defaultMealName(code),
            dishesRaw = raw,
            calories = calories?.trim()?.ifEmpty { null },
        )
    }

    private fun MealEntity.toMeal() = Meal(
        code = mealCode,
        name = mealName,
        dishes = MenuParser.parse(dishesRaw),
        calories = calories,
    )

    private fun defaultMealName(code: Int) = when (code) {
        1 -> "조식"
        2 -> "중식"
        3 -> "석식"
        else -> "급식"
    }

    private companion object {
        val REFETCH_AFTER_MILLIS = TimeUnit.HOURS.toMillis(6)
        const val KEEP_WEEKS = 8L
    }
}

val School.cacheKey: String get() = "$officeCode-$schoolCode"
