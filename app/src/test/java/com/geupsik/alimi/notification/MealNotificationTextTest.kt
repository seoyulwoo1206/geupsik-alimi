package com.geupsik.alimi.notification

import com.geupsik.alimi.data.model.DayMeals
import com.geupsik.alimi.data.model.Meal
import com.geupsik.alimi.domain.Dish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MealNotificationTextTest {

    private val thursday = LocalDate.of(2026, 10, 22)
    private val saturday = LocalDate.of(2026, 10, 24)

    private val lunch = Meal(
        code = 2,
        name = "중식",
        dishes = listOf(
            Dish("쌀밥", emptySet()),
            Dish("김치전", setOf(1, 5, 6, 9, 13)),
            Dish("쿠키", setOf(1, 2, 4, 5, 6, 16)),
        ),
        calories = "900 Kcal",
    )

    @Test
    fun `알레르기가 없으면 메뉴를 그대로 보여준다`() {
        val text = MealNotificationText.build(thursday, DayMeals(listOf(lunch), true), emptySet())!!
        assertEquals("오늘의 급식 · 10월 22일 (목)", text.title)
        assertEquals("쌀밥, 김치전, 쿠키", text.summary)
        assertEquals("[중식] 쌀밥, 김치전, 쿠키", text.body)
    }

    @Test
    fun `등록한 알레르기가 든 메뉴에 표시하고 이유를 적는다`() {
        val text = MealNotificationText.build(thursday, DayMeals(listOf(lunch), true), setOf(4, 5))!!
        assertEquals("⚠ 알레르기 주의 메뉴가 2개 있어요", text.summary)
        assertTrue(text.body.startsWith("[중식] 쌀밥, 김치전⚠, 쿠키⚠"))
        assertTrue(text.body.contains("· 김치전: 대두"))
        assertTrue(text.body.contains("· 쿠키: 땅콩·대두"))
        assertTrue(text.body.contains("참고용"))
        assertFalse(text.body.contains("쌀밥⚠"))
    }

    @Test
    fun `급식 없는 날은 알림을 보내지 않는다`() {
        assertNull(MealNotificationText.build(saturday, DayMeals(emptyList(), true), emptySet()))
        assertNull(MealNotificationText.build(thursday, DayMeals(emptyList(), true), emptySet()))
    }

    @Test
    fun `못 받아 왔으면 평일에만 앱을 열어 보라고 알린다`() {
        val text = MealNotificationText.build(thursday, DayMeals(emptyList(), false), emptySet())
        assertTrue(text!!.summary.contains("불러오지 못했어요"))
        assertNull(MealNotificationText.build(saturday, null, emptySet()))
    }
}
