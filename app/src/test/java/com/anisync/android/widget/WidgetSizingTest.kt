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
 * Plain JVM, no Android. Worth pinning because both mistakes are silent: too many rows clips content
 * off the bottom with no error, too few leaves a widget looking broken at a size nobody tried.
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
        // 300 - 64 = 236 usable, so two 90dp rows and a bit, not two and a half.
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
        // RemoteViews rejects more than 16 sized entries.
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
        // A minimum past every rung still has to declare one size or updateAppWidget throws.
        assertTrue(WidgetSizes.ladder(minHeightDp = 9_000).isNotEmpty())
    }

    @Test
    fun narrowAndShort_matchTheRungsTheyGuard() {
        assertTrue(WidgetSizes.isNarrow(SizeFCompat(180f, 300f)))
        assertFalse(WidgetSizes.isNarrow(SizeFCompat(300f, 300f)))

        // Short covers the 100 and 150 rungs. Neither fits a header, a filter row and a whole card,
        // which is why Airing Today drops its chrome for a hero there.
        assertTrue(WidgetSizes.isShort(SizeFCompat(300f, 100f)))
        assertTrue(WidgetSizes.isShort(SizeFCompat(300f, 150f)))
        assertFalse(WidgetSizes.isShort(SizeFCompat(300f, 200f)))
    }
}
