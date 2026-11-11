package com.tasmee3.app.match

/** Decides whether a heard word is the expected word. Both arguments must already be normalised. */
object WordComparer {
    /** Allowed letter edits (on the alef-free key) by word length. Short words must match exactly. */
    fun maxEdits(keyLength: Int): Int = when {
        keyLength <= 3 -> 0
        keyLength <= 7 -> 1
        else -> 2
    }

    /** Same word, ignoring alef differences. Needs at least 3 letters when alefs are ignored. */
    fun sameWord(expected: String, heard: String): Boolean {
        if (expected == heard) return true
        val ek = ArabicNormalizer.key(expected)
        val hk = ArabicNormalizer.key(heard)
        return ek.length >= 3 && ek == hk
    }

    /** Same word, or close enough to forgive a one or two letter recognition slip (when [fuzzy]). */
    fun matches(expected: String, heard: String, fuzzy: Boolean = true): Boolean {
        if (sameWord(expected, heard)) return true
        if (!fuzzy) return false
        val ek = ArabicNormalizer.key(expected)
        val hk = ArabicNormalizer.key(heard)
        if (ek.length < 4 || hk.length < 3) return false
        return ArabicNormalizer.editDistance(ek, hk) <= maxEdits(ek.length)
    }
}
