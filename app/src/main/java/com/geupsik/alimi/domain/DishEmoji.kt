package com.geupsik.alimi.domain

/**
 * 메뉴 이름으로 어울리는 음식 이모지를 고른다.
 *
 * 한국 급식 메뉴는 "소고기감자국", "닭다리살스테이크"처럼 끝에 음식 종류가 오므로,
 * 이름 안에서 가장 뒤쪽에 나오는 낱말을 고른다. 한 글자 낱말(밥, 국, 떡…)은
 * "전복", "배추"처럼 다른 낱말 속에 섞이기 쉬워서 이름 끝에 있을 때만 쓴다.
 */
object DishEmoji {

    const val DEFAULT = "🍽️"

    private val KEYWORDS: List<Pair<String, String>> = listOf(
        // 밥·죽
        "밥" to "🍚", "덮밥" to "🍛", "볶음밥" to "🍛", "비빔밥" to "🍛", "카레" to "🍛", "커리" to "🍛",
        "김밥" to "🍙", "주먹밥" to "🍙", "초밥" to "🍣", "유부초밥" to "🍣",
        "죽" to "🥣", "시리얼" to "🥣", "그래놀라" to "🥣",
        // 국물
        "국" to "🍲", "탕" to "🍲", "찌개" to "🍲", "전골" to "🍲", "스프" to "🍲", "수프" to "🍲", "개장" to "🍲",
        // 면
        "면" to "🍜", "국수" to "🍜", "우동" to "🍜", "라면" to "🍜", "짜장" to "🍜", "짬뽕" to "🍜",
        "스파게티" to "🍝", "파스타" to "🍝",
        // 김치·나물·샐러드
        "김치" to "🥬", "깍두기" to "🥬", "석박지" to "🥬", "섞박지" to "🥬", "겉절이" to "🥬", "총각" to "🥬",
        "나물" to "🥗", "무침" to "🥗", "샐러드" to "🥗", "쌈" to "🥬",
        // 고기
        "고기" to "🍖", "불고기" to "🍖", "갈비" to "🍖", "스테이크" to "🍖", "제육" to "🍖", "수육" to "🍖",
        "보쌈" to "🍖", "돈육" to "🍖", "삼겹" to "🍖", "미트볼" to "🍖", "함박" to "🍖", "동파육" to "🍖",
        "돈까스" to "🍖", "돈가스" to "🍖", "까스" to "🍖", "가스" to "🍖",
        "닭" to "🍗", "치킨" to "🍗", "윙" to "🍗", "너겟" to "🍗",
        "소시지" to "🌭", "핫도그" to "🌭", "햄" to "🥓", "베이컨" to "🥓",
        // 해산물
        "생선" to "🐟", "고등어" to "🐟", "삼치" to "🐟", "갈치" to "🐟", "연어" to "🐟", "동태" to "🐟",
        "명태" to "🐟", "코다리" to "🐟", "가자미" to "🐟", "임연수" to "🐟", "꽁치" to "🐟", "아귀" to "🐟",
        "참치" to "🐟", "멸치" to "🐟", "어묵" to "🍢", "오뎅" to "🍢",
        "새우" to "🦐", "오징어" to "🦑", "쭈꾸미" to "🦑", "주꾸미" to "🦑", "낙지" to "🦑",
        "조개" to "🦪", "홍합" to "🦪", "바지락" to "🦪", "전복" to "🦪",
        // 반찬
        "볶음" to "🥘", "조림" to "🥘", "찜" to "🥘", "구이" to "🥘", "강정" to "🥘", "튀김" to "🍤",
        "전" to "🥞", "부침" to "🥞", "만두" to "🥟", "떡" to "🍡", "떡볶이" to "🍡",
        "계란" to "🥚", "달걀" to "🥚", "에그" to "🥚", "두부" to "🧈", "감자" to "🥔", "고구마" to "🍠",
        "옥수수" to "🌽", "김" to "🍙", "자반" to "🍙", "말랭이" to "🍠",
        // 빵·간식
        "빵" to "🍞", "토스트" to "🍞", "식빵" to "🍞", "베이글" to "🥯", "크로와상" to "🥐", "크루아상" to "🥐",
        "와플" to "🧇", "팬케이크" to "🥞", "케이크" to "🍰", "머핀" to "🧁", "도넛" to "🍩", "쿠키" to "🍪",
        "피자" to "🍕", "버거" to "🍔", "햄버거" to "🍔", "샌드위치" to "🥪", "타코" to "🌮", "또띠아" to "🌯",
        "부리또" to "🌯", "나쵸" to "🌮", "슈" to "🍮", "푸딩" to "🍮", "젤리" to "🍬",
        "아이스크림" to "🍦", "빙수" to "🍧", "초코" to "🍫",
        // 마실 것
        "우유" to "🥛", "요거트" to "🥛", "요구르트" to "🥛", "두유" to "🥛",
        "주스" to "🧃", "쥬스" to "🧃", "에이드" to "🧃", "음료" to "🧃", "식혜" to "🧃", "스무디" to "🧃",
        // 과일
        "사과" to "🍎", "귤" to "🍊", "오렌지" to "🍊", "한라봉" to "🍊", "천혜향" to "🍊", "바나나" to "🍌",
        "포도" to "🍇", "샤인머스캣" to "🍇", "수박" to "🍉", "딸기" to "🍓", "멜론" to "🍈", "참외" to "🍈",
        "파인애플" to "🍍", "키위" to "🥝", "복숭아" to "🍑", "체리" to "🍒", "망고" to "🥭", "배" to "🍐",
        "토마토" to "🍅", "과일" to "🍓", "후르츠" to "🍓",
    )

    fun of(name: String): String {
        // 괄호 속 설명(예: "쌀밥(자율)")과 띄어쓰기는 빼고 본다
        val text = name.replace(Regex("""\([^)]*\)"""), "").filterNot { it.isWhitespace() }
        var best: Pair<String, String>? = null
        var bestEnd = -1
        for (pair in KEYWORDS) {
            val word = pair.first
            val end = if (word.length == 1) {
                // 한 글자 낱말은 이름 끝이나 "/" 앞에 올 때만 본다 (예: "토스트/잼"의 토스트, "보리밥")
                text.split('/').let { parts ->
                    var position = -1
                    var offset = 0
                    for (part in parts) {
                        if (part.endsWith(word)) position = offset + part.length
                        offset += part.length + 1
                    }
                    position
                }
            } else {
                text.lastIndexOf(word).let { if (it < 0) -1 else it + word.length }
            }
            if (end < 0) continue
            // 더 뒤에서 끝나는 낱말, 같으면 더 긴 낱말(볶음밥 > 밥)을 고른다
            if (end > bestEnd || (end == bestEnd && word.length > best!!.first.length)) {
                best = pair
                bestEnd = end
            }
        }
        return best?.second ?: DEFAULT
    }
}
