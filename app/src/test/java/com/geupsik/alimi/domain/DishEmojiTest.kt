package com.geupsik.alimi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DishEmojiTest {

    private fun check(expected: String, vararg names: String) {
        names.forEach { assertEquals(it, expected, DishEmoji.of(it)) }
    }

    @Test
    fun `이름 끝의 음식 종류를 따른다`() {
        check("🍲", "소고기감자국", "백합탕", "얼큰콩나물국", "양송이크림스프")
        check("🍚", "보리밥", "귀리밥", "쌀밥(자율)")
        check("🍛", "불고기크림리조또덮밥")
        check("🥘", "꼬마전복멸치볶음", "한입떡갈비조림")
        check("🥗", "아삭콩나물깻잎무침")
        check("🍖", "닭다리살스테이크", "동파육")
    }

    @Test
    fun `반찬·간식·과일`() {
        check("🥬", "배추김치", "깍두기", "석박지")
        check("🥛", "시리얼/우유", "초코우유", "요거트/쨈")
        check("🥐", "갈릭크로와상")
        check("🍊", "오렌지", "귤")
        check("🍗", "나쵸맛킹치킨")
        check("🥚", "스크램블에그")
        check("🧃", "패션후르츠주스")
    }

    @Test
    fun `한 글자 낱말은 다른 낱말 속에서는 고르지 않는다`() {
        // "배추"의 배, "전복"의 전이 과일·전으로 잡히면 안 된다
        check("🥬", "배추겉절이")
        check("🥞", "부추전")
        check("🍞", "몽블랑빵")
    }

    @Test
    fun `모르는 메뉴는 기본 그릇`() {
        check(DishEmoji.DEFAULT, "특식", "")
    }
}
