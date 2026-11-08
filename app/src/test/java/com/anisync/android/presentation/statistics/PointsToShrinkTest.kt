package com.anisync.android.presentation.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PointsToShrinkTest {

    /** The loop the search replaced: a point at a time until the text fits or the floor is reached. */
    private fun pointByPoint(floor: Int, fits: (Int) -> Boolean): Int {
        var points = 0
        while (points < floor && !fits(points)) points++
        return points
    }

    @Test
    fun `lands where the point by point loop did`() {
        for (floor in 0..190) {
            for (fitsFrom in 0..floor + 2) {
                val fits = { points: Int -> points >= fitsFrom }
                assertEquals("floor $floor, fits from $fitsFrom", pointByPoint(floor, fits), pointsToShrink(floor, fits))
            }
        }
    }

    @Test
    fun `a long way down takes a handful of tries`() {
        var tries = 0
        val points = pointsToShrink(186) { tries++; it >= 150 }
        assertEquals(150, points)
        assertTrue("took $tries", tries <= 10)
    }

    @Test
    fun `text that fits is left alone after one try`() {
        var tries = 0
        assertEquals(0, pointsToShrink(40) { tries++; true })
        assertEquals(1, tries)
    }
}
