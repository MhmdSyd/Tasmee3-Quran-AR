package com.tasmee3.app.match

import com.tasmee3.app.mushaf.MushafLine
import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.mushaf.MushafWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Live (streaming) reveal. Dummy vocabulary words: 0 كتاب, 1 قلم, 2 مدرسة, 3 شجرة, 4 نهر, 5 جبل. */
class StreamingTest {
    private val vocab = listOf("كتاب", "قلم", "مدرسة", "شجرة", "نهر", "جبل")
    private fun matcher(basmala: String = "", first: String = "2:5:") = RecitationMatcher(
        vocab.mapIndexed { i, w -> ExpectedWord("$first${i + 1}", w, i == 0) }, basmala = basmala
    )

    private val page = MushafPage(
        number = 9, surah = 2, juz = 1,
        lines = listOf(
            MushafLine.Words(1, vocab.mapIndexed { i, w -> MushafWord("2:5:${i + 1}", w, if (i == 5) 5 else 0) })
        )
    )
    private val w = (page.lines[0] as MushafLine.Words).words
    private fun session(mode: RevealMode = RevealMode.WORD) = RecitationSession(page, "", mode = mode)

    // ---- matcher preview
    @Test
    fun previewCountsConsecutiveMatchesAndChangesNothing() {
        val m = matcher()
        assertEquals(3, m.previewCount("كتاب قلم مدرسة"))
        assertEquals(0, m.position)
        assertEquals(0, m.summary().firstTryCorrect)
    }

    @Test
    fun previewStopsAtTheFirstWrongWordWithoutFlaggingIt() {
        val m = matcher()
        assertEquals(1, m.previewCount("كتاب جبل شجرة"))
        assertEquals(0, m.summary().review.size)
    }

    @Test
    fun previewOfSilenceOrNoiseIsZero() {
        val m = matcher()
        assertEquals(0, m.previewCount(""))
        assertEquals(0, m.previewCount("123 !!!"))
    }

    @Test
    fun previewIgnoresARepeatedWordAndGluedWords() {
        val m = matcher()
        assertEquals(2, m.previewCount("كتاب كتاب قلم"))
        assertEquals(3, m.previewCount("كتاب قلم مدر سة"))
    }

    @Test
    fun previewSkipsABasmalaAtASurahStartOnly() {
        val atStart = matcher(basmala = "بسم الله الرحمن الرحيم", first = "3:1:")
        assertEquals(2, atStart.previewCount("بسم الله الرحمن الرحيم كتاب قلم"))
        val inMiddle = matcher(basmala = "بسم الله الرحمن الرحيم", first = "3:5:")
        assertEquals(0, inMiddle.previewCount("بسم الله الرحمن الرحيم كتاب قلم"))
    }

    // ---- session streaming
    @Test
    fun partialResultRevealsWordsBeforeThePhraseEnds() {
        val s = session()
        assertTrue(s.onPartial("كتاب", 1000, 1500))
        assertEquals(WordDisplay.FRESH, s.snapshot().display(w[0]))
        assertEquals(WordDisplay.HIDDEN, s.snapshot().display(w[1]))
        assertTrue(s.onPartial("كتاب قلم", 1000, 2000))
        assertEquals(WordDisplay.REVEALED, s.snapshot().display(w[0]))
        assertEquals(WordDisplay.FRESH, s.snapshot().display(w[1]))
        assertEquals(2, s.snapshot().position)
    }

    @Test
    fun revealsNeverShrinkWithinAPhrase() {
        val s = session()
        s.onPartial("كتاب قلم مدرسة", 1000, 3000)
        assertFalse(s.onPartial("كتاب قلم", 1000, 3500)) // a later, shorter guess changes nothing
        assertEquals(3, s.snapshot().position)
    }

    @Test
    fun finalResultReplacesThePreviewAndStillScoresFirstTry() {
        val s = session()
        s.onPartial("كتاب قلم", 1000, 2000)
        s.onSegment("كتاب قلم مدرسة", 1000, 3000)
        assertEquals(3, s.snapshot().position)
        assertEquals(3, s.summary().firstTryCorrect)
    }

    @Test
    fun aLatePartialOfAnAlreadyFinishedPhraseIsIgnored() {
        val s = session()
        s.onSegment("كتاب قلم", 1000, 3000)
        assertFalse(s.onPartial("كتاب قلم مدرسة", 1000, 3200)) // started before the final ended
        assertEquals(2, s.snapshot().position)
        assertTrue(s.onPartial("مدرسة", 4000, 4500))            // next phrase is accepted
        assertEquals(3, s.snapshot().position)
    }

    @Test
    fun aWrongWordIsNotFlaggedByAPartialOnlyByTheFinal() {
        val s = session()
        s.onPartial("كتاب جبل", 1000, 2000)
        assertNull(s.snapshot().error)
        s.onSegment("كتاب جبل", 1000, 3000)
        assertEquals(1, s.snapshot().error!!.index)
    }

    @Test
    fun aPartialThatCoversTheErrorWordClearsItOnScreen() {
        val s = session()
        s.onSegment("كتاب جبل", 1000, 3000)
        assertEquals(WordDisplay.ERROR, s.snapshot().display(w[1]))
        s.onPartial("قلم", 4000, 4500)
        assertNull(s.snapshot().error)
        assertEquals(WordDisplay.FRESH, s.snapshot().display(w[1]))
    }

    @Test
    fun hintTakesOverTheFirstPreviewedWord() {
        val s = session()
        s.onPartial("كتاب قلم", 1000, 2000)
        s.hint(2100)
        assertEquals(2, s.snapshot().position)
        assertEquals(1, s.summary().review.size)
    }

    @Test
    fun verseModeStaysHiddenUntilTheWholeVerseIsHeard() {
        val s = session(RevealMode.VERSE)
        s.onPartial("كتاب قلم مدرسة شجرة نهر", 1000, 5000)
        assertEquals(WordDisplay.HIDDEN, s.snapshot().display(w[0]))
        s.onPartial("كتاب قلم مدرسة شجرة نهر جبل", 1000, 6000)
        // the whole verse appears at once and is highlighted as the newest verse
        assertEquals(WordDisplay.FRESH, s.snapshot().display(w[0]))
        assertEquals(WordDisplay.FRESH, s.snapshot().display(w[5]))
    }
}
