package com.tasmee3.app.match

import com.tasmee3.app.mushaf.MushafLine
import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.mushaf.MushafWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecitationSessionTest {
    // Dummy words. Verse 5 holds words 1-3 (the last carries the verse-end mark); verse 6 starts on the next line
    // with a mark-only token for verse 5 first, as the real data does when a mark wraps.
    private val page = MushafPage(
        number = 9, surah = 2, juz = 1,
        lines = listOf(
            MushafLine.Words(1, listOf(
                MushafWord("2:5:1", "كتاب", 0), MushafWord("2:5:2", "قلم", 0), MushafWord("2:5:3", "مدرسة", 0),
            )),
            MushafLine.Words(2, listOf(
                MushafWord("2:5:4", "", 5), MushafWord("2:6:1", "شجرة", 0), MushafWord("2:6:2", "نهر", 6),
            )),
        )
    )

    private fun session() = RecitationSession(page, "بسم الله")

    @Test
    fun startsFullyHidden() {
        val snap = session().snapshot()
        assertEquals(5, snap.total)
        assertEquals(0, snap.position)
        assertTrue((page.lines[0] as MushafLine.Words).words.all { snap.display(it) == WordDisplay.HIDDEN })
        assertNull(snap.error)
    }

    @Test
    fun revealingMarksLatestWordFresh() {
        val s = session()
        s.onSegment("كتاب قلم", 0, 2000)
        val snap = s.snapshot()
        val w = (page.lines[0] as MushafLine.Words).words
        assertEquals(WordDisplay.REVEALED, snap.display(w[0]))
        assertEquals(WordDisplay.FRESH, snap.display(w[1]))
        assertEquals(WordDisplay.HIDDEN, snap.display(w[2]))
        assertEquals(2, snap.position)
    }

    @Test
    fun wrongWordShowsErrorInfoThenClearsWhenRevealed() {
        val s = session()
        s.onSegment("كتاب جبل", 0, 2000)
        val snap = s.snapshot()
        val e = snap.error!!
        assertEquals(1, e.index)
        assertEquals(5, e.verse)
        assertEquals(2, e.wordInVerse)
        assertEquals(WordDisplay.ERROR, snap.display((page.lines[0] as MushafLine.Words).words[1]))
        s.onSegment("قلم", 3000, 4000)
        assertNull(s.snapshot().error)
    }

    @Test
    fun verseMarkShowsOnlyWhenItsWordIsRevealed() {
        val s = session()
        val markOnly = (page.lines[1] as MushafLine.Words).words[0]
        assertFalse(s.snapshot().markVisible(markOnly))
        s.onSegment("كتاب قلم مدرسة", 0, 3000)
        assertTrue(s.snapshot().markVisible(markOnly))
        val lastWord = (page.lines[1] as MushafLine.Words).words[2]
        assertFalse(s.snapshot().markVisible(lastWord))
    }

    @Test
    fun hintRevealsNextWordAndCountsAsReview() {
        val s = session()
        s.hint(1000)
        assertEquals(1, s.snapshot().position)
        assertEquals(1, s.summary().review.size)
        assertEquals(ReviewReason.HINTED, s.summary().review[0].primaryReason)
    }

    @Test
    fun autoHintWaitsForSilenceAndPendingDecodes() {
        val s = session()
        assertFalse(s.shouldAutoHint(4000, 0, false, 0, 5000))
        assertTrue(s.shouldAutoHint(5000, 0, false, 0, 5000))
        assertFalse(s.shouldAutoHint(9000, 0, true, 0, 5000))   // still speaking
        assertFalse(s.shouldAutoHint(9000, 0, false, 1, 5000))  // still recognizing
        assertFalse(s.shouldAutoHint(9000, 6000, false, 0, 5000)) // spoke at 6 s
        s.onSegment("كتاب", 7000, 8000)
        assertFalse(s.shouldAutoHint(12000, 8000, false, 0, 5000))
        assertTrue(s.shouldAutoHint(13000, 8000, false, 0, 5000))
    }

    @Test
    fun completingEveryWordMarksTheSessionComplete() {
        val s = session()
        s.onSegment("كتاب قلم مدرسة شجرة نهر", 0, 6000)
        assertTrue(s.isComplete)
        assertTrue(s.snapshot().completed)
        assertFalse(s.shouldAutoHint(60000, 0, false, 0, 5000))
        assertEquals(1.0, s.summary().accuracy, 0.0)
    }
}
