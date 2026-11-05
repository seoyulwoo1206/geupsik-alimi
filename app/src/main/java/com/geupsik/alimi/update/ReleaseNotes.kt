package com.geupsik.alimi.update

/** 깃허브 릴리스 설명(마크다운)을 업데이트 팝업에 보여줄 글로 바꾼다. */
object ReleaseNotes {

    /** 이 제목부터는 브라우저로 받는 사람을 위한 안내라 앱 안에서는 뺀다 */
    private val INSTALL_SECTION = Regex("""^#+\s*설치\s*$""")
    private val HEADING_MARK = Regex("""^#+\s*""")

    fun clean(body: String): String =
        body.lines()
            .takeWhile { !INSTALL_SECTION.matches(it.trim()) }
            .joinToString("\n") { line ->
                line.trimEnd()
                    .replace(HEADING_MARK, "")
                    .replace("**", "")
                    .replace(Regex("""^[-*]\s+"""), "· ")
            }
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
}
