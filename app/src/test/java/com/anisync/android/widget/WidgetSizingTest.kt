package com.anisync.android.widget

import androidx.core.util.SizeFCompat
import com.anisync.android.widget.core.WidgetRows
import com.anisync.android.widget.core.WidgetSizes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arithmetic behind how much a widget draws.
 *
 * Pure JVM, no Android needed. Worth pinning because both mistakes here are silent: too many rows
 * means content clipped off the bottom with no error anywhere, and too few means a widget that
 * looks broken at a size nobody tested.
 */
class WidgetSizingTest {

    @Test
    fun fit_neverReturnsZeroRows() {
        // A widget shorter than its own chrome still has to show something.
        val rows = WidgetRows.fit(SizeFCompat(300f, 40f), rowHeightDp = 90, chromeDp = 64, max = 5)
        assertEquals(1, rows)
    }

    @Test
    fun fit_countsWholeRowsOnly() {
        // 300 - 64 = 236 usable, which is two 90dp rows and change, not two and a half.
        val rows = WidgetRows.fit(SizeFCompat(300f, 300f), rowHeightDp = 90, chromeDp = 64, max = 10)
        assertEquals(2, rows)
    }

    @Test
    fun fit_respectsTheCap() {
        val rows = WidgetRows.fit(SizeFCompat(300f, 2000f), rowHeightDp = 90, chromeDp = 64, max = 5)
        assertEquals(5, rows)
    }

    @Test
    fun fit_growsWithHeight() {
        val short = WidgetRows.fit(SizeFCompat(300f, 200f), rowHeightDp = 72, chromeDp = 64, max = 12)
        val tall = WidgetRows.fit(SizeFCompat(300f, 400f), rowHeightDp = 72, chromeDp = 64, max = 12)
        assertTrue("$tall should exceed $short", tall > short)
    }

    @Test
    fun ladder_staysWithinThePlatformLimit() {
        // RemoteViews rejects more than 16 sized entries outright.
        assertTrue(WidgetSizes.ladder(minHeightDp = 100).size <= 16)
        assertTrue(WidgetSizes.ladder(minHeightDp = 150).size <= 16)
    }

    @Test
    fun ladder_dropsRungsBelowTheMinimum() {
        val ladder = WidgetSizes.ladder(minHeightDp = 150)
        assertTrue(ladder.isNotEmpty())
        assertTrue(ladder.none { it.height < 150f })
    }

    @Test
    fun ladder_neverEmptyEvenForATallMinimum() {
        // A minimum past every rung still has to declare one size, or updateAppWidget throws.
        assertTrue(WidgetSizes.ladder(minHeightDp = 9_000).isNotEmpty())
    }

    @Test
    fun narrowAndShort_matchTheRungsTheyGuard() {
        assertTrue(WidgetSizes.isNarrow(SizeFCompat(180f, 300f)))
        assertFalse(WidgetSizes.isNarrow(SizeFCompat(300f, 300f)))
        assertTrue(WidgetSizes.isShort(SizeFCompat(300f, 100f)))
        assertFalse(WidgetSizes.isShort(SizeFCompat(300f, 150f)))
    }
}
