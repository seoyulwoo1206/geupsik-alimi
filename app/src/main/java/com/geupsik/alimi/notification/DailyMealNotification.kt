package com.geupsik.alimi.notification

import com.geupsik.alimi.data.prefs.UserPrefs
import com.geupsik.alimi.data.repository.MealRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.LocalDate

/** 오늘 급식을 캐시에서 읽어 알림을 보낸다. 알람이 울렸을 때와 설정의 "테스트 알림"에서 쓴다. */
class DailyMealNotification(
    private val prefs: UserPrefs,
    private val mealRepository: MealRepository,
    private val notifier: MealNotifier,
) {
    suspend fun send(today: LocalDate = LocalDate.now()) {
        val school = prefs.school.first() ?: return

        // 캐시가 오래됐으면 잠깐 새로 받아 본다. 인터넷이 없거나 느리면 캐시만 쓴다.
        tryRefresh { mealRepository.refreshWeek(school, today) }

        val day = mealRepository.getDay(school, today)
        val allergies = prefs.allergies.first()
        MealNotificationText.build(today, day, allergies)?.let(notifier::show)

        // 금~일에는 다음 주 것도 미리 받아 둔다. 월요일 아침에 인터넷이 없어도 알림이 가게.
        if (today.dayOfWeek >= DayOfWeek.FRIDAY) {
            tryRefresh { mealRepository.refreshWeek(school, today.plusWeeks(1)) }
        }
    }

    private suspend fun tryRefresh(block: suspend () -> Unit) {
        try {
            withTimeoutOrNull(REFRESH_TIMEOUT_MILLIS) { block() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 오류는 무시하고 캐시를 쓴다
        }
    }

    private companion object {
        /** 브로드캐스트 리시버는 10초 안에 끝나야 해서 넉넉히 짧게 잡는다 */
        const val REFRESH_TIMEOUT_MILLIS = 4_000L
    }
}
