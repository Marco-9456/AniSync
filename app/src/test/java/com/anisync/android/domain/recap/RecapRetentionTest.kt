package com.anisync.android.domain.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RecapRetentionTest {

    private val november4 = LocalDate.of(2026, 11, 4)

    private fun month(year: Int, month: Int) = RecapPeriod.Month(YearMonth.of(year, month))

    @Test
    fun `thirteen months keep a year and the month before it`() {
        assertEquals(YearMonth.of(2025, 10), firstKeptMonth(november4, DEFAULT_KEPT_MONTHS))
        val stored = listOf(month(2025, 9), month(2025, 10), month(2026, 10), RecapPeriod.Year(2025), RecapPeriod.Year(2024))
        assertEquals(listOf(month(2025, 9), RecapPeriod.Year(2024)), expiredPeriods(stored, november4, DEFAULT_KEPT_MONTHS))
    }

    @Test
    fun `a year stays as long as its December does`() {
        val year = listOf(RecapPeriod.Year(2025))
        assertTrue(expiredPeriods(year, LocalDate.of(2027, 1, 31), DEFAULT_KEPT_MONTHS).isEmpty())
        assertEquals(year, expiredPeriods(year, LocalDate.of(2027, 2, 1), DEFAULT_KEPT_MONTHS))
    }

    @Test
    fun `a first build stores last year and the kept months up to now`() {
        val periods = backfillPeriods(november4, DEFAULT_KEPT_MONTHS)
        assertEquals(RecapPeriod.Year(2025), periods.first())
        assertEquals(month(2025, 10), periods[1])
        assertEquals(month(2026, 11), periods.last())
        assertEquals(1 + 14, periods.size)
    }

    @Test
    fun `in January a first build reaches back to the start of last year`() {
        val periods = backfillPeriods(LocalDate.of(2026, 1, 15), DEFAULT_KEPT_MONTHS)
        assertEquals(listOf(RecapPeriod.Year(2025), month(2025, 1)), periods.take(2))
        assertEquals(month(2026, 1), periods.last())
    }

    @Test
    fun `keeping one month builds nothing retention would delete`() {
        assertEquals(listOf(month(2026, 10), month(2026, 11)), backfillPeriods(november4, keptMonths = 1))
    }

    @Test
    fun `a period is finished the day after it ends`() {
        assertFalse(month(2026, 10).isFinishedOn(LocalDate.of(2026, 10, 31)))
        assertTrue(month(2026, 10).isFinishedOn(LocalDate.of(2026, 11, 1)))
        assertFalse(RecapPeriod.Year(2026).isFinishedOn(november4))
    }

    @Test
    fun `the settings stamp changes with every choice`() {
        val keys = listOf(
            RecapSettings(), RecapSettings(includeManga = false), RecapSettings(countRepeats = true), RecapSettings(showAdult = true),
        ).map { it.key }
        assertEquals("m1r0a0", keys.first())
        assertEquals(keys.size, keys.toSet().size)
    }
}
