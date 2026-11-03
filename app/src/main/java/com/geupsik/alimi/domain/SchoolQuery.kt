package com.geupsik.alimi.domain

/** 학교 검색어 정리와 줄임말 풀기 */
object SchoolQuery {

    /** 자주 쓰는 줄임말. 나이스는 이름 일부로 찾기 때문에 "경기과고"처럼 줄이면 결과가 없다. */
    private val ABBREVIATIONS = listOf(
        "과고" to "과학고",
        "외고" to "외국어고",
        "예고" to "예술고",
        "체고" to "체육고",
        "여고" to "여자고",
        "여중" to "여자중",
    )

    /**
     * 나이스에 보낼 수 있게 정리한다. 띄어쓰기를 없애고(학교 이름에는 띄어쓰기가 없다),
     * 한글 입력 중에 끝에 붙는 낱자(ㄱ, ㅏ 등)를 뗀다.
     */
    fun normalize(query: String): String =
        query.filterNot { it.isWhitespace() }.trimEnd { it in 'ㄱ'..'ㆎ' }

    /** 차례대로 검색해 볼 검색어. 첫 번째는 항상 정리한 검색어 그대로다. */
    fun candidates(query: String): List<String> {
        val base = normalize(query)
        val expanded = ABBREVIATIONS.firstNotNullOfOrNull { (short, full) ->
            if (base.endsWith(short) && !base.endsWith(full)) base.dropLast(short.length) + full else null
        }
        return listOfNotNull(base, expanded).distinct()
    }
}
