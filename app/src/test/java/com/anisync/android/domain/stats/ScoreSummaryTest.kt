package com.anisync.android.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreSummaryTest {

    @Test
    fun `nothing scored has no median or most given`() {
        val summary = summarizeScores(mapOf(0 to 12))
        assertEquals(0, summary.scored)
        assertNull(summary.median)
        assertNull(summary.mostGiven)
        assertFalse(summary.hasSpread)
    }

    @Test
    fun `unscored entries are left out`() {
        val summary = summarizeScores(mapOf(0 to 40, 80 to 3, 90 to 2))
        assertEquals(5, summary.scored)
        assertEquals(80, summary.median)
    }

    @Test
    fun `an even count takes the lower middle`() {
        assertEquals(70, summarizeScores(mapOf(60 to 1, 70 to 1, 80 to 1, 90 to 1)).median)
    }

    @Test
    fun `the median follows the counts, not the buckets`() {
        assertEquals(85, summarizeScores(mapOf(5 to 2, 50 to 3, 85 to 20, 100 to 12)).median)
    }

    @Test
    fun `a tie for most given goes to the higher score`() {
        assertEquals(90, summarizeScores(mapOf(70 to 4, 90 to 4, 100 to 1)).mostGiven)
    }

    @Test
    fun `five scored titles are enough for a spread`() {
        assertFalse(summarizeScores(mapOf(80 to 4)).hasSpread)
        assertTrue(summarizeScores(mapOf(80 to 5)).hasSpread)
    }
}
