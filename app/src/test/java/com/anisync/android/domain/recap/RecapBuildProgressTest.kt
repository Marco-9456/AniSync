package com.anisync.android.domain.recap

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class RecapBuildProgressTest {

    private val zone = ZoneOffset.UTC
    private val reports = ArrayList<RecapBuildProgress>()

    private fun seconds(year: Int, month: Int, day: Int): Long = LocalDate.of(year, month, day).atStartOfDay(zone).toEpochSecond()

    private val from = seconds(2025, 1, 1)
    private val now = seconds(2026, 11, 15)

    /** 1 January 2025 to the end of November 2026, built on 15 November 2026. */
    private val tracker = RecapBuildTracker(ActivityWindow(from, seconds(2026, 12, 1)), now, zone, report = { reports += it })

    private fun page(vararg createdAt: Long) = FetchedPage(
        createdAt.mapIndexed { index, at -> FetchedActivity(index, mediaId = 1, status = "watched episode", progress = "1", createdAt = at) },
        hasNextPage = true,
    )

    @Test
    fun `the history counts from now back to the oldest activity read, not to the window's end`() = runTest {
        tracker.onPage(page(seconds(2026, 11, 10), (from + now) / 2))
        val halfway = reports.last()
        assertEquals(0.4f, halfway.fraction, 0.001f)
        assertEquals(YearMonth.of(2025, 12), halfway.readBackTo)
    }

    @Test
    fun `progress only moves forward while older pages come in`() = runTest {
        tracker.onPage(page(seconds(2026, 6, 1)))
        tracker.onPage(page(seconds(2026, 7, 1)))
        tracker.onPage(page(seconds(2025, 2, 1)))
        assertTrue(reports.zipWithNext().all { (before, after) -> after.fraction >= before.fraction })
        assertEquals(YearMonth.of(2026, 6), reports[1].readBackTo)
    }

    @Test
    fun `an empty page reports nothing`() = runTest {
        tracker.onPage(FetchedPage(emptyList(), hasNextPage = false))
        assertTrue(reports.isEmpty())
    }

    @Test
    fun `lookups carry the bar from the end of the history to the end of the build`() = runTest {
        tracker.onTitles(0, 4)
        assertEquals(0.8f, reports.last().fraction, 0.001f)
        assertNull(reports.last().readBackTo)
        tracker.onTitles(4, 4)
        assertEquals(0.9f, reports.last().fraction, 0.001f)
        tracker.onAiring(1, 2)
        assertEquals(0.95f, reports.last().fraction, 0.001f)
        tracker.onAiring(2, 2)
        assertEquals(1f, reports.last().fraction, 0.001f)
    }

    @Test
    fun `a build with no titles to look up still finishes its share`() = runTest {
        tracker.onTitles(0, 0)
        assertEquals(0.9f, reports.last().fraction, 0.001f)
    }
}
