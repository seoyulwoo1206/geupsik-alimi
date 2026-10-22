package com.geupsik.alimi.notification

import com.geupsik.alimi.data.model.DayMeals
import com.geupsik.alimi.domain.Allergy
import com.geupsik.alimi.ui.components.ALLERGY_DISCLAIMER
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NotificationText(
    val title: String,
    /** 알림을 접었을 때 보이는 한 줄 */
    val summary: String,
    /** 알림을 펼쳤을 때 보이는 전체 내용 */
    val body: String,
)

/** 알림에 들어갈 글을 만든다. 안드로이드 코드와 분리해서 단위 테스트할 수 있게 했다. */
object MealNotificationText {

    const val MARK = "⚠"
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
    private const val LUNCH_CODE = 2

    /**
     * @param day 오늘 급식 캐시. 받아 오지 못했으면 null이거나 weekFetched=false
     * @return 알림을 보내지 않아야 하면 null (주말·방학처럼 급식이 없는 날)
     */
    fun build(date: LocalDate, day: DayMeals?, allergies: Set<Int>): NotificationText? {
        if (day == null || (day.meals.isEmpty() && !day.weekFetched)) {
            // 못 받아 왔을 때는 평일에만 알려 준다. 주말엔 원래 급식이 없는 경우가 대부분이다.
            if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) return null
            val message = "급식 정보를 불러오지 못했어요. 앱을 열어 확인해 주세요."
            return NotificationText("오늘의 급식", message, message)
        }
        if (day.meals.isEmpty()) return null

        val warnings = mutableListOf<String>()
        val mealLines = day.meals.map { meal ->
            val dishes = meal.dishes.joinToString(", ") { dish ->
                val matched = Allergy.matched(dish, allergies)
                if (matched.isEmpty()) {
                    dish.name
                } else {
                    warnings += "· ${dish.name}: ${Allergy.names(matched)}"
                    "${dish.name}$MARK"
                }
            }
            "[${meal.name}] $dishes"
        }
        val distinctWarnings = warnings.distinct()

        val summary = if (distinctWarnings.isNotEmpty()) {
            "$MARK 알레르기 주의 메뉴가 ${distinctWarnings.size}개 있어요"
        } else {
            val main = day.meals.firstOrNull { it.code == LUNCH_CODE } ?: day.meals.first()
            main.dishes.joinToString(", ") { it.name }
        }

        val body = buildString {
            append(mealLines.joinToString("\n"))
            if (distinctWarnings.isNotEmpty()) {
                append("\n\n$MARK 알레르기 주의\n")
                append(distinctWarnings.joinToString("\n"))
                append("\n\n")
                append(ALLERGY_DISCLAIMER)
            }
        }

        return NotificationText("오늘의 급식 · ${date.format(DATE_FORMAT)}", summary, body)
    }
}
