package com.tasmee3.app.match

import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.mushaf.MushafWord

/** Settings choice: reveal each word as it is recited, or a whole verse once it is complete. */
enum class RevealMode { WORD, VERSE }

/** How one word of the page is drawn during recitation. */
enum class WordDisplay { HIDDEN, REVEALED, FRESH, ERROR }

/** What the page view asks for each word. The plain reader shows everything. */
interface WordDisplays {
    fun display(word: MushafWord): WordDisplay
    /** Whether the verse-number circle attached to [word] shows its number. */
    fun markVisible(word: MushafWord): Boolean
}

object AllRevealed : WordDisplays {
    override fun display(word: MushafWord) = WordDisplay.REVEALED
    override fun markVisible(word: MushafWord) = true
}

/** The word that was heard wrongly, for the mistake panel. */
data class ErrorInfo(val index: Int, val heard: String, val verse: Int, val wordInVerse: Int)

/** Immutable picture of the session for the UI (safe to hand to Compose). */
class SessionSnapshot(
    val total: Int,
    val position: Int,
    private val displays: List<WordDisplay>,
    private val indexByLocation: Map<String, Int>,
    val error: ErrorInfo?,
    val completed: Boolean,
) : WordDisplays {
    override fun display(word: MushafWord): WordDisplay {
        val i = indexByLocation[word.location] ?: return WordDisplay.REVEALED
        return displays[i]
    }

    override fun markVisible(word: MushafWord): Boolean {
        val i = if (word.isSpoken) {
            indexByLocation[word.location]
        } else {
            // A verse-end mark alone on a line belongs to the word before it: s:a:w -> s:a:(w-1)
            val parts = word.location.split(':')
            if (parts.size == 3) indexByLocation["${parts[0]}:${parts[1]}:${(parts[2].toIntOrNull() ?: 1) - 1}"] else null
        } ?: return true
        return displays[i] == WordDisplay.REVEALED || displays[i] == WordDisplay.FRESH
    }

    override fun equals(other: Any?) = other is SessionSnapshot && other.position == position &&
        other.error == error && other.displays == displays && other.completed == completed

    override fun hashCode() = 31 * position + displays.hashCode()
}

/**
 * One recitation of one page: the matcher plus everything the screen needs (word states, mistake panel,
 * auto-hint timing). Pure Kotlin. Not thread-safe: call it from one thread or under a lock.
 */
class RecitationSession(
    page: MushafPage,
    basmala: String,
    config: MatcherConfig = MatcherConfig(),
    private val mode: RevealMode = RevealMode.WORD,
) {
    val words: List<ExpectedWord> = ExpectedWords.fromPage(page)
    private val matcher = RecitationMatcher(words, config, basmala)
    private val indexByLocation: Map<String, Int> = words.mapIndexed { i, w -> w.id to i }.toMap()
    private var fresh = -1
    private var freshVerse: String? = null
    private val verseOf: List<String> = words.map { it.id.substringBeforeLast(':') }
    private var error: ErrorInfo? = null
    private var lastProgressMs = 0L
    private var tentative = 0               // words confirmed by a partial result, not yet final
    private var lastFinalEndMs = Long.MIN_VALUE

    val isComplete: Boolean get() = matcher.isComplete

    /** The final result of a phrase (after a pause). Replaces any live preview of that phrase. */
    fun onSegment(text: String, startMs: Long, endMs: Long): List<MatchEvent> {
        tentative = 0
        lastFinalEndMs = endMs
        return matcher.onSegment(text, startMs, endMs).also { apply(it, endMs) }
    }

    /**
     * A partial result while the reciter is still speaking: reveals matched words right away (streaming).
     * Only reveals; mistakes are judged on the final result. Reveals never shrink within a phrase.
     * Returns true if the picture changed. A late partial of a phrase that is already final is ignored.
     */
    fun onPartial(text: String, utteranceStartMs: Long, nowMs: Long): Boolean {
        if (isComplete || utteranceStartMs <= lastFinalEndMs) return false
        val k = matcher.previewCount(text)
        if (k <= tentative) return false
        tentative = k
        lastProgressMs = nowMs
        return true
    }

    fun hint(nowMs: Long): List<MatchEvent> {
        if (tentative > 0) tentative-- // the hinted word was the first previewed one: it is real now
        return matcher.hint().also { apply(it, nowMs) }
    }

    fun summary(): Summary = matcher.summary()

    /** True when the reciter has been silent long enough and nothing is still being recognized. */
    fun shouldAutoHint(nowMs: Long, lastSpeechMs: Long, speaking: Boolean, pendingDecodes: Int, afterMs: Long): Boolean =
        !isComplete && !speaking && pendingDecodes == 0 && nowMs - maxOf(lastProgressMs, lastSpeechMs) >= afterMs

    fun snapshot(): SessionSnapshot {
        val shownUpTo = matcher.position + tentative          // words before this index are shown
        val activeError = error?.takeIf { it.index >= shownUpTo }
        val freshIndex = if (tentative > 0) shownUpTo - 1 else fresh
        val displays = if (mode == RevealMode.VERSE) verseDisplays(shownUpTo, activeError) else words.indices.map { i ->
            when {
                activeError?.index == i -> WordDisplay.ERROR
                i < shownUpTo -> if (i == freshIndex) WordDisplay.FRESH else WordDisplay.REVEALED
                else -> WordDisplay.HIDDEN
            }
        }
        return SessionSnapshot(words.size, shownUpTo, displays, indexByLocation, activeError, isComplete)
    }

    /** A verse shows only when every one of its words on this page has been revealed. */
    private fun verseDisplays(shownUpTo: Int, activeError: ErrorInfo?): List<WordDisplay> {
        val complete = words.indices.groupBy { verseOf[it] }
            .filterValues { idx -> idx.all { it < shownUpTo } }.keys
        val freshKey = if (tentative > 0) verseOf[shownUpTo - 1] else freshVerse
        return words.indices.map { i ->
            when {
                activeError?.index == i -> WordDisplay.ERROR
                verseOf[i] !in complete -> WordDisplay.HIDDEN
                verseOf[i] == freshKey -> WordDisplay.FRESH
                else -> WordDisplay.REVEALED
            }
        }
    }

    private fun apply(events: List<MatchEvent>, nowMs: Long) {
        for (e in events) {
            when (e) {
                is MatchEvent.Revealed -> {
                    fresh = e.index
                    val verse = verseOf[e.index]
                    if (words.indices.filter { verseOf[it] == verse }.all { matcher.isRevealed(it) }) freshVerse = verse
                    if ((error?.index ?: Int.MAX_VALUE) <= e.index) error = null
                    lastProgressMs = nowMs
                }
                is MatchEvent.WrongTry -> {
                    val parts = words[e.index].id.split(':')
                    error = ErrorInfo(e.index, e.heard, parts[1].toInt(), parts[2].toInt())
                    lastProgressMs = nowMs
                }
                else -> Unit
            }
        }
    }
}
