package com.tasmee3.app.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicNormalizerTest {
    // Plain vocabulary only. Escapes: 064E fatha, 064F damma, 0650 kasra, 0651 shadda, 0652 sukun, 0670 dagger alef.
    @Test
    fun stripsVowelMarks() {
        assertEquals("كتاب", ArabicNormalizer.normalize("\u0643\u0650\u062A\u064E\u0627\u0628\u064F"))
    }

    @Test
    fun stripsDaggerAlefTatweelAndWaqfSigns() {
        assertEquals("كتب", ArabicNormalizer.normalize("\u0643\u0640\u062A\u0670\u0628\u06D6"))
    }

    @Test
    fun unifiesAlefAndAlefMaksura() {
        assertEquals("احمد", ArabicNormalizer.normalize("\u0623\u062D\u0645\u062F"))
        assertEquals("ازرق", ArabicNormalizer.normalize("\u0625\u0632\u0631\u0642"))
        assertEquals("الكتب", ArabicNormalizer.normalize("\u0671\u0644\u0643\u062A\u0628"))
        assertEquals("علي", ArabicNormalizer.normalize("\u0639\u0644\u0649"))
    }

    @Test
    fun hamzaCarriersMatchTheOfficialTextEncoding() {
        // official text: ya + hamza mark (U+0654); older text: hamza-on-ya U+0626. Same letters after normalising.
        assertEquals(ArabicNormalizer.normalize("\u0627\u0645\u0631\u0626"), ArabicNormalizer.normalize("\u0627\u0645\u0631\u064A\u0654"))
        assertEquals(ArabicNormalizer.normalize("\u0644\u0624\u0644\u0624"), ArabicNormalizer.normalize("\u0644\u0648\u0654\u0644\u0648\u0654"))
    }

    @Test
    fun replacesDigitsLatinAndPunctuationWithSpaces() {
        assertEquals("كتاب قلم", ArabicNormalizer.normalize("كتاب! 12 abc قلم."))
        assertEquals("", ArabicNormalizer.normalize("123 !!! ..."))
    }

    @Test
    fun wordsSplitsOnSpaces() {
        assertEquals(listOf("كتاب", "قلم"), ArabicNormalizer.words("  كتاب   قلم "))
        assertTrue(ArabicNormalizer.words("").isEmpty())
    }

    @Test
    fun keyRemovesAlefButNeverReturnsEmpty() {
        assertEquals("كتب", ArabicNormalizer.key("كتاب"))
        assertEquals("ا", ArabicNormalizer.key("ا"))
    }

    @Test
    fun editDistanceBasics() {
        assertEquals(0, ArabicNormalizer.editDistance("كتب", "كتب"))
        assertEquals(1, ArabicNormalizer.editDistance("كتب", "كتم"))
        assertEquals(3, ArabicNormalizer.editDistance("", "كتب"))
        assertFalse(ArabicNormalizer.editDistance("قلم", "كتب") == 0)
    }
}
