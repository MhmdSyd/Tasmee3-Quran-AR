package com.tasmee3.app.match

import com.tasmee3.app.mushaf.MushafParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/** Runs the matcher over the REAL bundled pages (skipped locally if assets were not fetched; runs in CI). */
class RecitationMatcherAssetsTest {
    private val dir = File("src/main/assets/mushaf")

    @Test
    fun perfectRecitationOfEveryPageIsAllFirstTry() {
        assumeTrue("assets not fetched", File(dir, "meta.json").exists())
        for (n in 1..604) {
            val page = MushafParser.parsePage(File(dir, "p%03d.json".format(Locale.ROOT, n)).readText())
            val words = ExpectedWords.fromPage(page)
            assertTrue("page $n has an empty normalised word", words.all { it.normalized.isNotEmpty() })
            val m = RecitationMatcher(words)
            var i = 0
            var t = 0L
            var step = 3
            while (i < words.size) {
                val end = minOf(words.size, i + step)
                m.onSegment(words.subList(i, end).joinToString(" ") { it.text }, t, t + 1500)
                t += 2500
                i = end
                step = 3 + (step % 7) // 3..9 words per segment, deterministic
            }
            val s = m.summary()
            assertTrue("page $n not completed", s.completed)
            assertEquals("page $n first-try", s.total, s.firstTryCorrect)
        }
    }
}
