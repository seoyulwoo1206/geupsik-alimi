package com.geupsik.alimi.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    private fun v(text: String) = AppVersion.parse(text)!!

    @Test
    fun `태그 앞의 v는 무시한다`() {
        assertEquals(v("0.2.0"), v("v0.2.0"))
        assertEquals(v("1.0.0"), v("V1.0.0"))
    }

    @Test
    fun `숫자 조각별로 비교한다`() {
        assertTrue(v("0.2.0") > v("0.1.0"))
        assertTrue(v("0.10.0") > v("0.9.0"))
        assertTrue(v("1.0.0") > v("0.99.99"))
    }

    @Test
    fun `자릿수가 달라도 빈 자리는 0으로 본다`() {
        assertEquals(0, v("1.0").compareTo(v("1.0.0")))
        assertTrue(v("1.0.1") > v("1.0"))
    }

    @Test
    fun `뒤에 붙은 글자는 무시하고 숫자로 읽을 수 없으면 null`() {
        assertEquals(v("1.2.0"), v("v1.2.0-beta"))
        assertNull(AppVersion.parse("latest"))
    }
}
