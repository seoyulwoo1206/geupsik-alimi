package com.geupsik.alimi.domain

/** 메뉴 한 줄. allergyNumbers는 나이스 알레르기 번호(1~19). */
data class Dish(val name: String, val allergyNumbers: Set<Int>)

/**
 * 나이스 DDISH_NM을 메뉴 목록으로 바꾼다.
 *
 * 메뉴는 <br/>로 구분되고, 알레르기 번호는 메뉴 이름 뒤에 붙어 있다.
 *   요즘 형식:  "돼지국밥 (5.6.10.13.16)"
 *   예전 형식:  "감자채볶음1.5.10.13."
 */
object MenuParser {

    private val LINE_BREAK = Regex("""<br\s*/?>|\r?\n""", RegexOption.IGNORE_CASE)

    /** 끝에 붙은 "(5.6.10)" 또는 "( 5.6.10. )" */
    private val PAREN_NUMBERS = Regex("""\s*\(\s*(\d{1,2}(?:\.\d{1,2})*)\.?\s*\)\s*$""")

    /**
     * 끝에 붙은 "1.5.10.13." 처럼 마침표로 끝나는 숫자 묶음.
     * "우유200" 같은 이름을 잘못 자르지 않도록 마지막 마침표가 꼭 있어야 한다.
     */
    private val TRAILING_NUMBERS = Regex("""(?<![\d.])(\d{1,2}(?:\.\d{1,2})*)\.\s*$""")

    fun parse(raw: String): List<Dish> =
        raw.split(LINE_BREAK)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(::parseDish)
            .filter { it.name.isNotEmpty() }

    fun parseDish(line: String): Dish {
        val text = line.trim()
        val match = PAREN_NUMBERS.find(text) ?: TRAILING_NUMBERS.find(text)
            ?: return Dish(text, emptySet())

        val numbers = match.groupValues[1]
            .split('.')
            .mapNotNull { it.toIntOrNull() }
            .filter { it in Allergy.NUMBER_RANGE }
            .toSet()
        val name = text.removeRange(match.range).trim()
        return Dish(name, numbers)
    }
}
