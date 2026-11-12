package com.tasmee3.app.match

import com.tasmee3.app.mushaf.MushafLine
import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.mushaf.MushafWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpectedWordsTest {
    @Test
    fun takesSpokenWordsInOrderAndFlagsVerseStarts() {
        val page = MushafPage(
            number = 5, surah = 2, juz = 1,
            lines = listOf(
                MushafLine.SurahHeader(1, "عنوان", 2),
                MushafLine.Basmala(2),
                MushafLine.Words(3, listOf(MushafWord("2:1:1", "كلمةأ", 0), MushafWord("2:1:2", "كلمةب", 1))),
                MushafLine.Words(4, listOf(MushafWord("2:1:3", "", 1), MushafWord("2:2:1", "كلمةج", 0))),
            )
        )
        val words = ExpectedWords.fromPage(page)
        assertEquals(listOf("2:1:1", "2:1:2", "2:2:1"), words.map { it.id })
        assertTrue(words[0].verseStart)
        assertFalse(words[1].verseStart)
        assertTrue(words[2].verseStart)
    }
}

class FirstVerseTest {
    @Test
    fun firstVerseUsesTheFirstSpokenWordIgnoringMarkOnlyTokens() {
        val page = MushafPage(
            number = 132, surah = 6, juz = 7,
            lines = listOf(
                MushafLine.Words(1, listOf(MushafWord("6:35:30", "", 35), MushafWord("6:36:1", "كلمة", 0)))
            )
        )
        assertEquals(6 to 36, page.firstVerse())
        assertEquals(null, MushafPage(1, 1, 1, emptyList()).firstVerse())
    }
}
