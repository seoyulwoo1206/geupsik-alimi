package com.geupsik.alimi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SchoolQueryTest {

    @Test
    fun `띄어쓰기와 입력 중인 낱자를 뗀다`() {
        assertEquals("경기과학고", SchoolQuery.normalize(" 경기 과학고 "))
        assertEquals("서울", SchoolQuery.normalize("서울ㄱ"))
        assertEquals("서울", SchoolQuery.normalize("서울ㄱㅗ"))
    }

    @Test
    fun `줄임말은 풀어서 한 번 더 찾는다`() {
        assertEquals(listOf("경기과고", "경기과학고"), SchoolQuery.candidates("경기과고"))
        assertEquals(listOf("대원외고", "대원외국어고"), SchoolQuery.candidates("대원 외고"))
        assertEquals(listOf("이화여고", "이화여자고"), SchoolQuery.candidates("이화여고"))
    }

    @Test
    fun `줄임말이 아니면 그대로 하나만`() {
        assertEquals(listOf("경기과학고"), SchoolQuery.candidates("경기과학고"))
        assertEquals(listOf("한빛중"), SchoolQuery.candidates("한빛중"))
    }
}
