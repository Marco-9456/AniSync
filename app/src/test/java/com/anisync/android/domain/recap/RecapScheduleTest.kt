package com.anisync.android.domain.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class RecapScheduleTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val nine = LocalTime.of(9, 0)

    private fun clockAt(dateTime: LocalDateTime) = Clock.fixed(dateTime.atZone(zone).toInstant(), zone)

    @Test
    fun `mid month the next run is the coming 1st`() {
        val next = nextRecapRun(clockAt(LocalDateTime.of(2026, 10, 28, 14, 0)), nine)
        assertEquals(LocalDateTime.of(2026, 11, 1, 9, 0), next.toLocalDateTime())
        assertEquals(zone, next.zone)
    }

    @Test
    fun `on the 1st before the time the run is today`() {
        val next = nextRecapRun(clockAt(LocalDateTime.of(2026, 11, 1, 8, 0)), nine)
        assertEquals(LocalDateTime.of(2026, 11, 1, 9, 0), next.toLocalDateTime())
    }

    @Test
    fun `on the 1st after the time the run is next month`() {
        val next = nextRecapRun(clockAt(LocalDateTime.of(2026, 11, 1, 9, 0)), nine)
        assertEquals(LocalDateTime.of(2026, 12, 1, 9, 0), next.toLocalDateTime())
    }

    @Test
    fun `the run in December rolls into January`() {
        val next = nextRecapRun(clockAt(LocalDateTime.of(2026, 12, 15, 9, 0)), nine)
        assertEquals(LocalDateTime.of(2027, 1, 1, 9, 0), next.toLocalDateTime())
    }

    @Test
    fun `the time holds across a daylight saving change`() {
        val next = nextRecapRun(clockAt(LocalDateTime.of(2026, 10, 20, 9, 0)), nine)
        assertEquals(LocalTime.of(9, 0), next.toLocalTime())
    }

    @Test
    fun `the 1st of a month makes last month ready`() {
        assertEquals(listOf(RecapPeriod.Month(YearMonth.of(2026, 9))), recapsDueOn(LocalDate.of(2026, 10, 1)))
        assertTrue(recapsDueOn(LocalDate.of(2026, 10, 2)).isEmpty())
    }

    @Test
    fun `1 January makes the year and December ready`() {
        assertEquals(
            listOf(RecapPeriod.Year(2025), RecapPeriod.Month(YearMonth.of(2025, 12))),
            recapsDueOn(LocalDate.of(2026, 1, 1)),
        )
    }

    @Test
    fun `the banner points at a recap for seven days`() {
        assertEquals(listOf(RecapPeriod.Month(YearMonth.of(2026, 10))), recapBannerCandidates(LocalDate.of(2026, 11, 7)))
        assertTrue(recapBannerCandidates(LocalDate.of(2026, 11, 8)).isEmpty())
        assertEquals(RecapPeriod.Year(2026), recapBannerCandidates(LocalDate.of(2027, 1, 3)).first())
    }
}
