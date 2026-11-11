package com.tasmee3.app.match

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordComparerTest {
    @Test fun identicalWordsMatch() = assertTrue(WordComparer.matches("كتاب", "كتاب"))

    @Test
    fun alefDifferencesAreForgivenOnThreeLetterKeys() {
        // Uthmani spelling without alef vs ordinary spelling with it.
        assertTrue(WordComparer.matches("كتب", "كتاب", fuzzy = false))
    }

    @Test
    fun shortWordsMustBeExact() {
        assertFalse(WordComparer.matches("ربك", "ربي"))
        assertFalse(WordComparer.matches("من", "في"))
    }

    @Test
    fun mediumWordsForgiveOneLetter() {
        assertTrue(WordComparer.matches("مستقيم", "مستقيمن"))
        assertFalse(WordComparer.matches("مستقيم", "مستقيمن", fuzzy = false))
    }

    @Test
    fun longWordsForgiveTwoLettersButNotThree() {
        assertTrue(WordComparer.matches("استخلفناكم", "استخلفناهم"))
        assertFalse(WordComparer.matches("استخلفناكم", "استخلفتهن"))
    }

    @Test
    fun differentWordsDoNotMatch() {
        assertFalse(WordComparer.matches("كتاب", "جبل"))
        assertFalse(WordComparer.matches("مدرسة", "شجرة"))
    }
}
