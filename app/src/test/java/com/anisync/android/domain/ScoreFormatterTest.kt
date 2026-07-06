package com.anisync.android.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the score-badge rendering across every AniList rating system. The raw [LibraryEntry.score]
 * is returned in the account's own [ScoreFormat] scale (UserLibrary.graphql fetches `score` with no
 * `format:` arg), so each format must read its value on its own scale.
 */
class ScoreFormatterTest {

    @Test
    fun `null and zero render as no score for every format`() {
        for (format in ScoreFormat.entries) {
            assertEquals("No score", formatScore(null, format))
            assertEquals("No score", formatScore(0.0, format))
        }
    }

    @Test
    fun `point 100 renders the integer on the 0-100 scale`() {
        assertEquals("87", formatScore(87.0, ScoreFormat.POINT_100))
        assertEquals("100", formatScore(100.0, ScoreFormat.POINT_100))
    }

    @Test
    fun `point 10 renders the integer on the 0-10 scale`() {
        assertEquals("9", formatScore(9.0, ScoreFormat.POINT_10))
    }

    @Test
    fun `point 10 decimal keeps one decimal place`() {
        assertEquals("7.5", formatScore(7.5, ScoreFormat.POINT_10_DECIMAL))
        assertEquals("8.0", formatScore(8.0, ScoreFormat.POINT_10_DECIMAL))
    }

    @Test
    fun `point 5 renders filled stars on the 0-5 scale`() {
        assertEquals("★★★", formatScore(3.0, ScoreFormat.POINT_5))
        assertEquals("★★★★★", formatScore(5.0, ScoreFormat.POINT_5))
    }

    @Test
    fun `point 3 renders smileys on the 1-3 scale`() {
        assertEquals(":(", formatScore(1.0, ScoreFormat.POINT_3))
        assertEquals(":|", formatScore(2.0, ScoreFormat.POINT_3))
        assertEquals(":)", formatScore(3.0, ScoreFormat.POINT_3))
    }
}
