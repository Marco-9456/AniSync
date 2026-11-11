package com.anisync.android.presentation.statistics

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AniListDayWindowTest {

    private fun day(date: LocalDate, bucket: String?) = ActivityDay(
        date = date,
        amount = 1,
        state = DayDataState.Counted,
        isToday = false,
        epochSeconds = bucket?.let { Instant.parse(it).epochSecond }
    )

    private fun seconds(instant: String) = Instant.parse(instant).epochSecond

    @Test
    fun `the day the clocks go back runs 25 hours`() {
        val (from, to) = aniListDayWindow(day(LocalDate.of(2026, 10, 25), "2026-10-24T23:00:00Z"))

        assertEquals(seconds("2026-10-24T23:00:00Z"), from)
        assertEquals(seconds("2026-10-26T00:00:00Z"), to)
    }

    @Test
    fun `the day the clocks go forward runs 23 hours`() {
        val (from, to) = aniListDayWindow(day(LocalDate.of(2026, 3, 29), "2026-03-29T00:00:00Z"))

        assertEquals(seconds("2026-03-29T23:00:00Z"), to)
        assertEquals(23 * 3600L, to - from)
    }

    @Test
    fun `a summer day runs from 23 UTC to 23 UTC`() {
        val (from, to) = aniListDayWindow(day(LocalDate.of(2026, 7, 14), "2026-07-13T23:00:00Z"))

        assertEquals(24 * 3600L, to - from)
        assertEquals(seconds("2026-07-14T23:00:00Z"), to)
    }

    @Test
    fun `a day without a bucket starts at London midnight`() {
        val (from, to) = aniListDayWindow(day(LocalDate.of(2026, 11, 3), null))

        assertEquals(seconds("2026-11-03T00:00:00Z"), from)
        assertEquals(seconds("2026-11-04T00:00:00Z"), to)
    }
}
