package com.jaedhc.lume.processing

class ProcessorUtils {
    companion object {
        fun normalizeToIso(it: String): String? {
            val m1 = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})""").matchEntire(it)
            if (m1 != null) {
                val d = m1.groupValues[1].padStart(2, '0')
                val mo = m1.groupValues[2].padStart(2, '0')
                val y = m1.groupValues[3].let { if (it.length==2) "20$it" else it }
                return "$y-$mo-$d"
            }
            return null
        }

        fun String.lineOrFirstWord(): String = this.lines().firstOrNull()?.trim() ?: this.trim()
    }
}
