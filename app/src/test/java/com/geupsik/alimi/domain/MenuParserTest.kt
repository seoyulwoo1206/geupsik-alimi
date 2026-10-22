package com.geupsik.alimi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuParserTest {

    @Test
    fun `요즘 형식 - 괄호 안 번호`() {
        assertEquals(
            Dish("돼지국밥", setOf(5, 6, 10, 13, 16)),
            MenuParser.parseDish("돼지국밥 (5.6.10.13.16)"),
        )
    }

    @Test
    fun `예전 형식 - 이름에 바로 붙은 번호`() {
        assertEquals(
            Dish("감자채볶음", setOf(1, 5, 10, 13)),
            MenuParser.parseDish("감자채볶음1.5.10.13."),
        )
    }

    @Test
    fun `번호 하나짜리`() {
        assertEquals(Dish("배추김치", setOf(9)), MenuParser.parseDish("배추김치 (9)"))
        assertEquals(Dish("우유", setOf(2)), MenuParser.parseDish("우유2."))
    }

    @Test
    fun `번호가 없으면 이름 그대로`() {
        assertEquals(Dish("쌀밥", emptySet()), MenuParser.parseDish("쌀밥 "))
        assertEquals(Dish("귤(중앙)", emptySet()), MenuParser.parseDish("귤(중앙) "))
    }

    @Test
    fun `이름 속 괄호와 숫자는 건드리지 않는다`() {
        assertEquals(
            Dish("배추김치(자율)", setOf(9)),
            MenuParser.parseDish("배추김치(자율) (9)"),
        )
        assertEquals(
            Dish("(도)오징어까스", setOf(1, 5, 6, 13, 17)),
            MenuParser.parseDish("(도)오징어까스 (1.5.6.13.17)"),
        )
        assertEquals(Dish("우유200", emptySet()), MenuParser.parseDish("우유200"))
        assertEquals(Dish("비타민C음료 2", emptySet()), MenuParser.parseDish("비타민C음료 2"))
    }

    @Test
    fun `범위 밖 번호는 버린다`() {
        assertEquals(Dish("특식", setOf(1)), MenuParser.parseDish("특식 (1.25)"))
    }

    @Test
    fun `br 태그로 나눈다`() {
        val dishes = MenuParser.parse("쌀밥 <br/>돼지국밥 (5.6.10.13.16)<br/>총각김치 (9)<br/>")
        assertEquals(listOf("쌀밥", "돼지국밥", "총각김치"), dishes.map { it.name })
        assertEquals(setOf(9), dishes.last().allergyNumbers)
    }

    @Test
    fun `br 표기가 달라도 나눈다`() {
        val dishes = MenuParser.parse("밥<br>국 (5)<BR />김치 (9)")
        assertEquals(listOf("밥", "국", "김치"), dishes.map { it.name })
    }
}
