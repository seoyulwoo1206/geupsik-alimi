package com.geupsik.alimi.domain

/** 나이스 급식 정보의 법정 알레르기 유발 식품 19종 */
object Allergy {

    val NUMBER_RANGE = 1..19

    val NAMES: Map<Int, String> = mapOf(
        1 to "난류",
        2 to "우유",
        3 to "메밀",
        4 to "땅콩",
        5 to "대두",
        6 to "밀",
        7 to "고등어",
        8 to "게",
        9 to "새우",
        10 to "돼지고기",
        11 to "복숭아",
        12 to "토마토",
        13 to "아황산류",
        14 to "호두",
        15 to "닭고기",
        16 to "쇠고기",
        17 to "오징어",
        18 to "조개류",
        19 to "잣",
    )

    fun name(number: Int): String = NAMES[number] ?: "$number"

    /** 메뉴에 든 번호 중 내가 등록한 것만. 비어 있으면 안전(으로 표시된) 메뉴 */
    fun matched(dish: Dish, mine: Set<Int>): Set<Int> = dish.allergyNumbers intersect mine

    /** {4, 5} → "땅콩·대두" (번호 순) */
    fun names(numbers: Set<Int>): String = numbers.sorted().joinToString("·") { name(it) }
}
