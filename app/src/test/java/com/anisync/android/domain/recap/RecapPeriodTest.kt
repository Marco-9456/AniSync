package com.anisync.android.domain.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

class RecapPeriodTest {

    @Test
    fun `keys read back as the same period`() {
        val month = RecapPeriod.Month(YearMonth.of(2026, 9))
        assertEquals("2026-09", month.key)
        assertEquals(month, RecapPeriod.fromKey("2026-09"))
        assertEquals(RecapPeriod.Year(2025), RecapPeriod.fromKey("2025"))
        assertNull(RecapPeriod.fromKey("September"))
    }

    @Test
    fun `previous steps back one month or one year`() {
        assertEquals(RecapPeriod.Month(YearMonth.of(2025, 12)), RecapPeriod.Month(YearMonth.of(2026, 1)).previous)
        assertEquals(RecapPeriod.Year(2024), RecapPeriod.Year(2025).previous)
    }

    @Test
    fun `a period holds its own days only`() {
        val month = RecapPeriod.Month(YearMonth.of(2026, 2))
        assertTrue(LocalDate.of(2026, 2, 28) in month)
        assertFalse(LocalDate.of(2026, 3, 1) in month)
        assertTrue(LocalDate.of(2025, 12, 31) in RecapPeriod.Year(2025))
    }

    @Test
    fun `the window starts and ends at local midnight`() {
        val zone = ZoneOffset.ofHours(3)
        val window = RecapPeriod.Month(YearMonth.of(2026, 9)).window(zone)
        assertEquals(LocalDateTime.of(2026, 9, 1, 0, 0).toEpochSecond(zone), window.from)
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0).toEpochSecond(zone), window.to)
        assertEquals(window.from - 1, window.createdAfter)
        assertEquals(window.to, window.createdBefore)
    }

    @Test
    fun `a narrowed window keeps the second it ends on`() {
        val window = ActivityWindow(1_000, 2_001)
        assertEquals(ActivityWindow(1_000, 1_501), window.endingAt(1_500))
        assertNull(window.endingAt(2_000))
    }

    @Test
    fun `a window splits only when its pages run past the depth limit`() {
        assertFalse(mustSplit(pagesRead = 99, hasNextPage = true))
        assertTrue(mustSplit(pagesRead = MAX_ACTIVITY_PAGES, hasNextPage = true))
        assertFalse(mustSplit(pagesRead = MAX_ACTIVITY_PAGES, hasNextPage = false))
        assertEquals(100, MAX_ACTIVITY_PAGES)
    }
}
