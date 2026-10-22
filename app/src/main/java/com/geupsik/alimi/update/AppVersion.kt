package com.geupsik.alimi.update

/**
 * "0.1.0", "v0.2.0", "v1.0" 같은 버전을 숫자 조각으로 비교한다.
 * 숫자로 읽을 수 없으면 null.
 */
data class AppVersion(val parts: List<Int>) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val size = maxOf(parts.size, other.parts.size)
        for (i in 0 until size) {
            val a = parts.getOrElse(i) { 0 }
            val b = other.parts.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    override fun toString(): String = parts.joinToString(".")

    companion object {
        private val VERSION = Regex("""^[vV]?(\d+(?:\.\d+)*)""")

        fun parse(text: String): AppVersion? {
            val match = VERSION.find(text.trim()) ?: return null
            return AppVersion(match.groupValues[1].split('.').map { it.toInt() })
        }
    }
}
