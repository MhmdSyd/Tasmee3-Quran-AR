package com.tasmee3.app.mushaf

object ArabicDigits {
    /** 12 -> "١٢", independent of the device locale. */
    fun fromInt(n: Int): String =
        n.toString().map { if (it in '0'..'9') '\u0660' + (it - '0') else it }.joinToString("")

    /** Accepts Latin, Arabic-Indic or Persian digits. Returns null when not a number. */
    fun toInt(s: String): Int? = s.trim().map {
        when (it) {
            in '\u0660'..'\u0669' -> '0' + (it - '\u0660')
            in '\u06F0'..'\u06F9' -> '0' + (it - '\u06F0')
            else -> it
        }
    }.joinToString("").toIntOrNull()
}
