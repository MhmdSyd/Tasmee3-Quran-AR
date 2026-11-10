package com.tasmee3.app.mushaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Validates the real bundled data. Skipped locally if scripts/fetch_assets.sh has not been run;
 * in CI the assets are fetched first, so this always runs there.
 */
class MushafAssetsTest {
    private val dir = File("src/main/assets/mushaf")

    @Test
    fun allPagesAreWellFormed() {
        assumeTrue("assets not fetched", File(dir, "meta.json").exists())
        var verseEnds = 0
        var lastJuz = 1
        for (n in 1..604) {
            val page = MushafParser.parsePage(File(dir, "p%03d.json".format(Locale.ROOT, n)).readText())
            assertEquals(n, page.number)
            assertEquals("page $n line count", if (n <= 2) 8 else 15, page.lines.size)
            page.lines.forEachIndexed { i, l -> assertEquals("page $n line numbering", i + 1, l.number) }
            assertTrue("juz order on page $n", page.juz in lastJuz..30)
            lastJuz = page.juz
            for (line in page.lines.filterIsInstance<MushafLine.Words>()) {
                assertTrue("page $n line ${line.number} too many items", line.words.size <= 16)
                verseEnds += line.words.count { it.verseEnd > 0 }
            }
        }
        assertEquals(6236, verseEnds)
        assertEquals(30, lastJuz)
    }

    @Test
    fun metaIsComplete() {
        assumeTrue("assets not fetched", File(dir, "meta.json").exists())
        val meta = MushafParser.parseMeta(File(dir, "meta.json").readText())
        assertEquals(114, meta.surahNames.size)
        assertEquals(4, meta.basmala.split(' ').size)
    }
}
