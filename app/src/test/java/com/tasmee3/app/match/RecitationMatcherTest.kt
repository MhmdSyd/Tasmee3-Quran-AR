package com.tasmee3.app.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses plain vocabulary words, not Quran text. Words: 0 كتاب, 1 قلم, 2 مدرسة, 3 شجرة, 4 نهر, 5 جبل, 6 بحر, 7 سماء. */
class RecitationMatcherTest {
    private val vocab = listOf("كتاب", "قلم", "مدرسة", "شجرة", "نهر", "جبل", "بحر", "سماء")

    private fun matcher(
        config: MatcherConfig = MatcherConfig(),
        verseStarts: Set<Int> = setOf(0),
        words: List<String> = vocab,
    ) = RecitationMatcher(words.mapIndexed { i, w -> ExpectedWord("1:1:${i + 1}", w, i in verseStarts) }, config)

    private fun RecitationMatcher.say(text: String, start: Long = 0, end: Long = start + 1000) =
        onSegment(text, start, end)

    private fun List<MatchEvent>.revealed() = filterIsInstance<MatchEvent.Revealed>()

    @Test
    fun perfectRecitationRevealsEverythingOnFirstTry() {
        val m = matcher()
        val events = m.say(vocab.joinToString(" "))
        assertEquals(8, events.revealed().size)
        assertTrue(events.revealed().all { it.firstTry })
        assertTrue(events.last() is MatchEvent.Completed)
        val s = m.summary()
        assertEquals(1.0, s.accuracy, 0.0)
        assertTrue(s.review.isEmpty())
        assertTrue(s.completed)
    }

    @Test
    fun recitationSplitOverSeveralSegmentsWorks() {
        val m = matcher()
        m.say("كتاب قلم", 0, 1500)
        m.say("مدرسة", 2000, 3000)
        val events = m.say("شجرة نهر جبل بحر سماء", 3500, 6000)
        assertTrue(events.last() is MatchEvent.Completed)
        assertEquals(1.0, m.summary().accuracy, 0.0)
    }

    @Test
    fun diacriticsInTheHeardTextAreIgnored() {
        val m = matcher()
        val events = m.say("\u0643\u0650\u062A\u064E\u0627\u0628\u064C \u0642\u064E\u0644\u064E\u0645")
        assertEquals(2, events.revealed().size)
        assertTrue(events.revealed().all { it.firstTry })
    }

    @Test
    fun skippedWordIsRevealedAndMarkedForReview() {
        val m = matcher()
        val events = m.say("كتاب مدرسة شجرة")
        val r = events.revealed()
        assertEquals(listOf(0, 1, 2, 3), r.map { it.index })
        assertFalse(r[1].firstTry)
        assertEquals(setOf(ReviewReason.SKIPPED), r[1].reasons)
        assertTrue(r[2].firstTry)
        val review = m.summary().review
        assertEquals(1, review.size)
        assertEquals(1, review[0].index)
        assertEquals(ReviewReason.SKIPPED, review[0].primaryReason)
    }

    @Test
    fun skippingMoreThanLookaheadIsAWrongTry() {
        val m = matcher()
        val events = m.say("كتاب جبل") // jabal is word 5, three ahead of position 1
        assertEquals(1, events.revealed().size)
        val wrong = events.filterIsInstance<MatchEvent.WrongTry>().single()
        assertEquals(1, wrong.index)
        assertEquals(1, wrong.attempt)
        assertEquals(1, m.position)
    }

    @Test
    fun immediateRepeatIsIgnored() {
        val m = matcher()
        val events = m.say("كتاب كتاب قلم")
        assertEquals(1, events.filterIsInstance<MatchEvent.Ignored>().size)
        assertEquals(IgnoreKind.REPEAT, events.filterIsInstance<MatchEvent.Ignored>().single().kind)
        assertEquals(2, m.position)
        assertTrue(m.summary().review.isEmpty())
    }

    @Test
    fun repeatedWordIsNotMistakenForASkipWhenTheSameWordComesTwoAhead() {
        val m = matcher(words = listOf("كتاب", "قلم", "كتاب", "جبل"))
        val events = m.say("كتاب كتاب قلم كتاب جبل")
        assertEquals(1, events.filterIsInstance<MatchEvent.Ignored>().size)
        assertEquals(4, events.revealed().size)
        assertTrue(events.revealed().all { it.firstTry })
        assertTrue(events.last() is MatchEvent.Completed)
    }

    @Test
    fun repeatedPhraseIsIgnoredAndRecitationContinues() {
        val m = matcher()
        val events = m.say("كتاب قلم كتاب قلم مدرسة")
        assertEquals(2, events.filterIsInstance<MatchEvent.Ignored>().size)
        assertEquals(3, m.position)
        assertEquals(3, m.summary().firstTryCorrect)
        assertTrue(m.summary().review.isEmpty())
    }

    @Test
    fun wrongWordFlaggedFirstThenRevealedOnSecondFailedTry() {
        val m = matcher()
        val first = m.say("كتاب جبل")
        assertEquals(1, first.filterIsInstance<MatchEvent.WrongTry>().single().attempt)
        assertFalse(m.isRevealed(1))

        val second = m.say("جبل", 3000, 4000)
        val r = second.revealed().single()
        assertEquals(1, r.index)
        assertEquals(setOf(ReviewReason.WRONG_WORD), r.reasons)
        assertFalse(r.firstTry)
        assertEquals(2, m.position)
    }

    @Test
    fun correctRetryAfterAWrongTryIsNotFirstTry() {
        val m = matcher()
        m.say("كتاب جبل")
        val events = m.say("قلم مدرسة", 3000, 4000)
        val r = events.revealed()
        assertEquals(setOf(ReviewReason.WRONG_WORD), r[0].reasons)
        assertFalse(r[0].firstTry)
        assertTrue(r[1].firstTry)
    }

    @Test
    fun continuingAfterAWrongWordRevealsItAsWrong() {
        val m = matcher()
        m.say("كتاب جبل")
        val events = m.say("مدرسة شجرة", 3000, 4000)
        val r = events.revealed()
        assertEquals(listOf(1, 2, 3), r.map { it.index })
        assertEquals(setOf(ReviewReason.WRONG_WORD), r[0].reasons)
        assertTrue(r[1].firstTry && r[2].firstTry)
    }

    @Test
    fun oneGarbageSegmentCountsAsOneFailedTryOnly() {
        val m = matcher()
        val events = m.say("جبل بحر سماء")
        assertEquals(1, events.size)
        assertTrue(events[0] is MatchEvent.WrongTry)
        assertEquals(0, m.position)
    }

    @Test
    fun restartFromVerseStartIsIgnoredUntilTheReciterCatchesUp() {
        val m = matcher(verseStarts = setOf(0, 4))
        m.say("كتاب قلم مدرسة شجرة نهر جبل", 0, 6000)
        assertEquals(6, m.position)
        val events = m.say("كتاب قلم مدرسة شجرة نهر جبل بحر سماء", 6500, 12000)
        val ignored = events.filterIsInstance<MatchEvent.Ignored>()
        assertEquals(6, ignored.size)
        assertEquals(IgnoreKind.RESTART, ignored.first().kind)
        assertEquals(listOf(6, 7), events.revealed().map { it.index })
        assertTrue(events.last() is MatchEvent.Completed)
        assertEquals(1.0, m.summary().accuracy, 0.0)
    }

    @Test
    fun silenceAndNoiseDoNothing() {
        val m = matcher()
        assertTrue(m.say("").isEmpty())
        assertTrue(m.say("   ").isEmpty())
        assertTrue(m.say("123 !!! ...").isEmpty())
        assertEquals(0, m.position)
        assertEquals(0, m.summary().firstTryCorrect)
        assertEquals(8, m.say(vocab.joinToString(" ")).revealed().size)
    }

    @Test
    fun longPauseBeforeAWordMarksItForReview() {
        val m = matcher()
        m.say("كتاب قلم", 0, 2000)
        val events = m.say("مدرسة", 9000, 10000)   // 7 s of silence
        val r = events.revealed().single()
        assertEquals(setOf(ReviewReason.LONG_PAUSE), r.reasons)
        assertFalse(r.firstTry)
        assertTrue(m.say("شجرة", 10500, 11500).revealed().single().firstTry)
    }

    @Test
    fun waitingBeforeTheFirstWordIsNotAPause() {
        val m = matcher()
        val r = m.say("كتاب", 20_000, 21_000).revealed().single()
        assertTrue(r.firstTry)
    }

    @Test
    fun hintRevealsNextWordWithoutFirstTryCredit() {
        val m = matcher()
        val r = m.hint().filterIsInstance<MatchEvent.Revealed>().single()
        assertEquals(0, r.index)
        assertEquals(setOf(ReviewReason.HINTED), r.reasons)
        m.say("قلم مدرسة شجرة نهر جبل بحر سماء")
        val s = m.summary()
        assertEquals(7, s.firstTryCorrect)
        assertEquals(7.0 / 8.0, s.accuracy, 1e-9)
        assertEquals(listOf(0), s.review.map { it.index })
    }

    @Test
    fun twoHeardWordsThatFormOneExpectedWordAreAccepted() {
        val m = matcher()
        val events = m.say("كتاب قلم مدر سة")
        assertEquals(3, events.revealed().size)
        assertTrue(events.revealed().all { it.firstTry })
    }

    @Test
    fun smallRecognitionSlipIsForgivenOnlyWhenFuzzy() {
        val words = listOf("مستقيم", "صراط")
        val fuzzy = matcher(words = words)
        assertEquals(1, fuzzy.say("مستقيمن").revealed().size)
        val strict = matcher(MatcherConfig(fuzzy = false), words = words)
        assertEquals(0, strict.say("مستقيمن").revealed().size)
    }

    @Test
    fun withoutRevealAfterSecondFailureTheWordStaysHidden() {
        val m = matcher(MatcherConfig(revealAfterSecondFailure = false))
        m.say("جبل", 0, 1000)
        val third = m.say("جبل", 2000, 3000)
        m.say("جبل", 4000, 5000)
        assertEquals(0, m.position)
        assertEquals(2, (third.single() as MatchEvent.WrongTry).attempt)
    }

    @Test
    fun completedSessionReportsOnceAndIgnoresMoreSpeech() {
        val m = matcher()
        m.say(vocab.joinToString(" "))
        assertTrue(m.isComplete)
        assertTrue(m.say("كتاب قلم", 20_000, 21_000).isEmpty())
        assertTrue(m.hint().isEmpty())
    }

    @Test
    fun summaryCountsUnrecitedWordsAsNotCorrect() {
        val m = matcher()
        m.say("كتاب قلم")
        val s = m.summary()
        assertEquals(8, s.total)
        assertEquals(2, s.firstTryCorrect)
        assertEquals(6, s.notRecited)
        assertFalse(s.completed)
        assertEquals(0.25, s.accuracy, 1e-9)
    }
}

class RecitationMatcherBasmalaTest {
    private val basmala = "بسم الله الرحمن الرحيم"
    private fun words(vararg texts: String, first: String = "2:1:") =
        texts.mapIndexed { i, t -> ExpectedWord("$first${i + 1}", t, i == 0) }

    private fun surahStartAt(id: String) = RecitationMatcher(
        listOf(ExpectedWord(id, "كتاب", true), ExpectedWord(id.dropLast(1) + "2", "قلم")), basmala = basmala
    )

    @Test
    fun basmalaBeforeASurahStartIsIgnoredAndCostsNothing() {
        val m = surahStartAt("2:1:1")
        val events = m.onSegment("بسم الله الرحمن الرحيم كتاب قلم", 0, 5000)
        assertEquals(4, events.filterIsInstance<MatchEvent.Ignored>().size)
        assertTrue(events.filterIsInstance<MatchEvent.Ignored>().all { it.kind == IgnoreKind.BASMALA })
        assertEquals(2, events.filterIsInstance<MatchEvent.Revealed>().size)
        assertEquals(1.0, m.summary().accuracy, 0.0)
    }

    @Test
    fun basmalaIsStillOptional() {
        val m = surahStartAt("2:1:1")
        m.onSegment("كتاب قلم", 0, 3000)
        assertEquals(1.0, m.summary().accuracy, 0.0)
    }

    @Test
    fun basmalaIsNotIgnoredInTheMiddleOfASurah() {
        val m = RecitationMatcher(words("كتاب", "قلم", first = "2:5:"), basmala = basmala)
        val events = m.onSegment("بسم", 0, 1000)
        assertTrue(events.single() is MatchEvent.WrongTry)
    }

    @Test
    fun alFatihaBasmalaIsARealVerseNotIgnored() {
        val m = RecitationMatcher(
            listOf("بسم", "الله", "الرحمن", "الرحيم").mapIndexed { i, t -> ExpectedWord("1:1:${i + 1}", t, i == 0) },
            basmala = basmala
        )
        val events = m.onSegment("بسم الله الرحمن الرحيم", 0, 4000)
        assertEquals(0, events.filterIsInstance<MatchEvent.Ignored>().size)
        assertEquals(4, events.filterIsInstance<MatchEvent.Revealed>().size)
    }

    @Test
    fun partialBasmalaThenTextStillWorks() {
        val m = surahStartAt("3:1:1")
        val events = m.onSegment("بسم الله كتاب قلم", 0, 4000)
        assertEquals(2, events.filterIsInstance<MatchEvent.Ignored>().size)
        assertEquals(2, events.filterIsInstance<MatchEvent.Revealed>().size)
    }
}
