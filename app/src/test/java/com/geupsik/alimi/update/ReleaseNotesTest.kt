package com.geupsik.alimi.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTest {

    @Test
    fun `제목 표시와 굵게 표시를 지우고 설치 안내는 뺀다`() {
        val body = """
            ## 새로 생긴 것
            - **일주일 보기**: 한 주 급식을 봐요.
            - 메뉴 표시를 지웠어요.

            알림 시간을 확인해 주세요.

            ## 설치
            아래 apk를 받아서 설치하세요.
        """.trimIndent()
        assertEquals(
            "새로 생긴 것\n· 일주일 보기: 한 주 급식을 봐요.\n· 메뉴 표시를 지웠어요.\n\n알림 시간을 확인해 주세요.",
            ReleaseNotes.clean(body),
        )
    }

    @Test
    fun `설치 제목이 없으면 끝까지 쓴다`() {
        assertEquals("버그를 고쳤어요.", ReleaseNotes.clean("버그를 고쳤어요.\r\n"))
    }
}
