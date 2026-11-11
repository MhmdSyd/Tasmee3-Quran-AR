package com.tasmee3.app.match

import com.tasmee3.app.mushaf.MushafLine
import com.tasmee3.app.mushaf.MushafPage

data class ExpectedWord(val id: String, val text: String, val verseStart: Boolean = false) {
    val normalized: String = ArabicNormalizer.normalize(text)
}

/** Why a word is in the review list. Declared in priority order (first = shown in the summary). */
enum class ReviewReason { WRONG_WORD, SKIPPED, LONG_PAUSE, HINTED }

enum class IgnoreKind { REPEAT, RESTART }

sealed interface MatchEvent {
    /** A word became visible. [firstTry] is true only if it needed no help and no retry. */
    data class Revealed(val index: Int, val firstTry: Boolean, val reasons: Set<ReviewReason>) : MatchEvent

    /** The word at [index] was not recognized: [attempt] 1 shows the error, attempt 2 reveals the word. */
    data class WrongTry(val index: Int, val heard: String, val attempt: Int) : MatchEvent

    /** A heard word was part of a repeat or restart and caused no error. */
    data class Ignored(val heard: String, val kind: IgnoreKind) : MatchEvent

    data object Completed : MatchEvent
}

data class MatcherConfig(
    val lookahead: Int = 2,                        // how many expected words may be skipped
    val pauseMs: Long = 5_000,                     // silence before a word that counts as a long pause
    val revealAfterSecondFailure: Boolean = true,  // handoff: show the word after the 2nd failed try
    val fuzzy: Boolean = true,                     // forgive small recognition slips (see WordComparer)
)

data class ReviewItem(val index: Int, val word: ExpectedWord, val reasons: Set<ReviewReason>) {
    val primaryReason: ReviewReason get() = reasons.minOf { it.ordinal }.let { ReviewReason.entries[it] }
}

data class Summary(
    val total: Int,
    val firstTryCorrect: Int,
    val review: List<ReviewItem>,
    val notRecited: Int,
    val completed: Boolean,
) {
    /** Words correct on the first try / all words of the session (words never recited count as not correct). */
    val accuracy: Double get() = if (total == 0) 0.0 else firstTryCorrect.toDouble() / total
}

object ExpectedWords {
    /** Spoken words of one mushaf page, in reading order. */
    fun fromPage(page: MushafPage): List<ExpectedWord> =
        page.lines.filterIsInstance<MushafLine.Words>()
            .flatMap { it.words }
            .filter { it.isSpoken }
            .map { ExpectedWord(it.location, it.text, verseStart = it.location.substringAfterLast(':') == "1") }
}

/**
 * Follows the expected text word by word. Feed it one recognized segment at a time with the AUDIO clock
 * (milliseconds) so results do not depend on how long decoding took. Pure Kotlin, no Android classes.
 *
 * Per heard word, in order: replay of earlier text (ignored) -> matches the next expected word -> two heard
 * words glued match it -> same as the previous word (repeat, ignored) -> matches one of the next
 * [MatcherConfig.lookahead] words (the words in between are skipped) -> longer repeat/restart of earlier
 * text (ignored) -> wrong word.
 * A "try" is one segment: at most one failed try is counted per segment, so one garbage segment can never
 * reveal several words.
 */
class RecitationMatcher(val words: List<ExpectedWord>, private val config: MatcherConfig = MatcherConfig()) {
    var position = 0
        private set
    val isComplete: Boolean get() = position >= words.size

    private val fails = IntArray(words.size)
    private val reasons = Array(words.size) { sortedSetOf<ReviewReason>() }
    private val revealed = BooleanArray(words.size)
    private var replay = -1
    private var replayKind = IgnoreKind.REPEAT
    private var lastEndMs: Long? = null
    private var completedEmitted = false

    fun isRevealed(index: Int) = revealed[index]
    fun reasonsOf(index: Int): Set<ReviewReason> = reasons[index]
    fun isFirstTry(index: Int) = revealed[index] && reasons[index].isEmpty()

    fun onSegment(text: String, startMs: Long, endMs: Long): List<MatchEvent> {
        val events = ArrayList<MatchEvent>()
        val heard = ArabicNormalizer.words(text)
        var pausePending = lastEndMs?.let { startMs - it > config.pauseMs } ?: false
        var failedThisSegment = false

        fun applyPause() {
            if (pausePending && !isComplete) {
                reasons[position].add(ReviewReason.LONG_PAUSE)
                pausePending = false
            }
        }

        var i = 0
        while (i < heard.size && !isComplete) {
            val h = heard[i]

            if (replay >= 0) {
                if (WordComparer.matches(words[replay].normalized, h, config.fuzzy)) {
                    replay++
                    events.add(MatchEvent.Ignored(h, replayKind))
                    if (replay >= position) replay = -1
                    i++
                    continue
                }
                replay = -1 // the reciter left the repeated text; handle this word normally
            }

            val expected = words[position].normalized
            val merged = if (i + 1 < heard.size) h + heard[i + 1] else null
            if (WordComparer.matches(expected, h, config.fuzzy)) {
                applyPause()
                accept(position, events)
                i++
                continue
            }
            if (merged != null && WordComparer.matches(expected, merged, config.fuzzy)) {
                applyPause()
                accept(position, events)
                i += 2
                continue
            }
            // Saying the previous word again is far more likely a stutter than a skip that lands on the same word.
            if (position > 0 && WordComparer.sameWord(words[position - 1].normalized, h)) {
                replayKind = IgnoreKind.REPEAT
                events.add(MatchEvent.Ignored(h, IgnoreKind.REPEAT))
                i++
                continue
            }
            val skip = lookaheadSkip(h)
            if (skip > 0) {
                applyPause()
                skipTo(position + skip, events)
                i++
                continue
            }
            val back = replayStart(h)
            if (back >= 0) {
                replayKind = if (back >= position - 3) IgnoreKind.REPEAT else IgnoreKind.RESTART
                replay = back + 1
                events.add(MatchEvent.Ignored(h, replayKind))
                if (replay >= position) replay = -1
                i++
                continue
            }
            if (!failedThisSegment) {
                failedThisSegment = true
                applyPause()
                fail(position, h, events)
            }
            i++
        }
        lastEndMs = endMs
        completeIfDone(events)
        return events
    }

    /** Reveals the next word without counting it as correct on the first try. */
    fun hint(): List<MatchEvent> {
        val events = ArrayList<MatchEvent>()
        if (isComplete) return events
        reasons[position].add(ReviewReason.HINTED)
        replay = -1
        reveal(position, events)
        completeIfDone(events)
        return events
    }

    fun summary(): Summary {
        val review = words.indices
            .filter { revealed[it] && reasons[it].isNotEmpty() }
            .map { ReviewItem(it, words[it], reasons[it].toSet()) }
        return Summary(
            total = words.size,
            firstTryCorrect = words.indices.count { isFirstTry(it) },
            review = review,
            notRecited = words.size - position,
            completed = isComplete,
        )
    }

    private fun lookaheadSkip(heard: String): Int {
        for (k in 1..config.lookahead) {
            val idx = position + k
            if (idx >= words.size) break
            if (WordComparer.sameWord(words[idx].normalized, heard)) return k
        }
        return 0
    }

    /** Index of an earlier word this heard word restarts or repeats, or -1. Nearest one wins. */
    private fun replayStart(heard: String): Int {
        var j = position - 1
        val limit = maxOf(0, position - 400)
        while (j >= limit) {
            if ((j >= position - 3 || words[j].verseStart) && WordComparer.sameWord(words[j].normalized, heard)) return j
            j--
        }
        return -1
    }

    private fun accept(index: Int, events: MutableList<MatchEvent>) {
        if (fails[index] > 0) reasons[index].add(ReviewReason.WRONG_WORD)
        reveal(index, events)
    }

    private fun skipTo(target: Int, events: MutableList<MatchEvent>) {
        for (p in position until target) {
            reasons[p].add(if (fails[p] > 0) ReviewReason.WRONG_WORD else ReviewReason.SKIPPED)
            reveal(p, events)
        }
        accept(target, events)
    }

    private fun fail(index: Int, heard: String, events: MutableList<MatchEvent>) {
        fails[index]++
        if (fails[index] >= 2 && config.revealAfterSecondFailure) {
            reasons[index].add(ReviewReason.WRONG_WORD)
            reveal(index, events)
        } else {
            events.add(MatchEvent.WrongTry(index, heard, fails[index]))
        }
    }

    private fun reveal(index: Int, events: MutableList<MatchEvent>) {
        revealed[index] = true
        position = index + 1
        events.add(MatchEvent.Revealed(index, reasons[index].isEmpty(), reasons[index].toSet()))
    }

    private fun completeIfDone(events: MutableList<MatchEvent>) {
        if (isComplete && !completedEmitted) {
            completedEmitted = true
            events.add(MatchEvent.Completed)
        }
    }
}
