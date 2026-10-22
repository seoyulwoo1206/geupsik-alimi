package com.geupsik.alimi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AllergyTest {

    @Test
    fun `19종이 모두 이름을 가진다`() {
        assertEquals((1..19).toList(), Allergy.NAMES.keys.sorted())
    }

    @Test
    fun `등록한 번호만 골라낸다`() {
        val dish = Dish("돼지국밥", setOf(5, 6, 10, 13, 16))
        assertEquals(setOf(5, 10), Allergy.matched(dish, setOf(4, 5, 10)))
        assertTrue(Allergy.matched(dish, setOf(1, 2)).isEmpty())
    }

    @Test
    fun `이름은 번호 순으로 가운뎃점으로 잇는다`() {
        assertEquals("땅콩·대두", Allergy.names(setOf(5, 4)))
        assertEquals("우유", Allergy.names(setOf(2)))
    }
}
