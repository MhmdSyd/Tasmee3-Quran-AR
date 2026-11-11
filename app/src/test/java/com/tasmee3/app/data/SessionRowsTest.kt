package com.tasmee3.app.data

import com.tasmee3.app.match.ExpectedWord
import com.tasmee3.app.match.ReviewItem
import com.tasmee3.app.match.ReviewReason
import com.tasmee3.app.settings.RecitationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRowsTest {
    @Test
    fun rowsKeepIdTextPrimaryReasonAndHeard() {
        val items = listOf(
            ReviewItem(1, ExpectedWord("2:5:2", "قلم"), setOf(ReviewReason.WRONG_WORD, ReviewReason.LONG_PAUSE), "جبل"),
            ReviewItem(4, ExpectedWord("2:6:1", "شجرة"), setOf(ReviewReason.SKIPPED)),
        )
        val rows = SessionRows.reviewRows(items)
        assertEquals(ReviewRow("2:5:2", "قلم", "WRONG_WORD", "جبل"), rows[0])
        assertEquals("SKIPPED", rows[1].reason)
        assertNull(rows[1].heard)
    }

    @Test
    fun defaultSettingsMatchTheDesign() {
        val s = RecitationSettings()
        assertTrue(s.autoHint)
        assertTrue(s.revealAfterSecondTry)
        assertFalse(s.errorSound)
        assertTrue(s.matcherConfig().revealAfterSecondFailure)
        assertFalse(s.copy(revealAfterSecondTry = false).matcherConfig().revealAfterSecondFailure)
    }
}
