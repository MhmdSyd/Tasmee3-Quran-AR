package com.tasmee3.app.match

/**
 * Letters-only normalisation used on BOTH the expected word and the recognized word before comparing.
 * Rules (also mirrored in tools/eval_model.py):
 *  1. Remove vowel marks U+064B-U+065F, dagger alef U+0670, Quranic annotation/waqf signs U+06D6-U+06ED,
 *     tatweel U+0640 and the invisible direction marks U+200E/U+200F.
 *  2. Alef with hamza/madda (U+0623, U+0625, U+0622) and alef wasla (U+0671) become plain alef U+0627.
 *  3. Alef maksura U+0649 becomes ya U+064A. Hamza on ya U+0626 becomes ya and hamza on waw U+0624 becomes waw:
 *     the official Quran text writes them as a plain letter plus a hamza mark (which rule 1 removes).
 *  4. Everything that is not an Arabic letter (digits, punctuation, Latin) becomes a space.
 *  5. Runs of spaces collapse to one; the result is trimmed.
 * Not changed on purpose: ta marbuta, the stand-alone hamza U+0621, ha.
 */
object ArabicNormalizer {
    private fun isStripped(c: Char) =
        c in '\u064B'..'\u065F' || c == '\u0670' || c in '\u06D6'..'\u06ED' ||
            c == '\u0640' || c == '\u200E' || c == '\u200F'

    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when {
                isStripped(c) -> Unit
                c == '\u0623' || c == '\u0625' || c == '\u0622' || c == '\u0671' -> sb.append('\u0627')
                c == '\u0649' || c == '\u0626' -> sb.append('\u064A')
                c == '\u0624' -> sb.append('\u0648')
                c in '\u0621'..'\u064A' -> sb.append(c)
                else -> sb.append(' ')
            }
        }
        return sb.toString().trim().replace(Regex(" +"), " ")
    }

    /** Normalised words of a free text (what the speech model returned). */
    fun words(text: String): List<String> =
        normalize(text).split(' ').filter { it.isNotEmpty() }

    /**
     * Comparison key: a normalised word without any alef. Uthmani spelling drops many alefs
     * (written with a dagger alef or nothing) that ordinary spelling keeps, e.g. a model may write
     * the long-aa word with an alef where the mushaf has none.
     */
    fun key(normalized: String): String {
        val k = normalized.replace("\u0627", "")
        return if (k.isEmpty()) normalized else k
    }

    fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }
}
