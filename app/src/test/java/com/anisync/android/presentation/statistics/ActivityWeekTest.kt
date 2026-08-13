package com.anisync.android.presentation.statistics

import com.anisync.android.domain.ActivityHistoryDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class ActivityWeekTest {

    /** A Thursday, so the current week runs Sun 9 August to Sat 15 August. */
    private val today: LocalDate = LocalDate.of(2026, 8, 13)

    private fun day(date: LocalDate, amount: Int): ActivityHistoryDay = ActivityHistoryDay(
        // AniList stamps a day at midnight in its own zone, which lands the day before in UTC.
        date = date.minusDays(1).atStartOfDay(ZoneOffset.UTC).plusHours(23).toEpochSecond(),
        amount = amount,
        level = amount.coerceIn(1, 10)
    )

    private fun history(vararg entries: Pair<LocalDate, Int>): Map<LocalDate, ActivityHistoryDay> =
        bucketActivityDays(entries.map { (date, amount) -> day(date, amount) })

    @Test
    fun `days past the last counted day are awaiting rather than empty`() {
        val byDate = history(
            LocalDate.of(2026, 8, 9) to 6,
            LocalDate.of(2026, 8, 10) to 3,
            LocalDate.of(2026, 8, 11) to 10
        )

        val week = buildActivityWeek(byDate, offset = 0, today = today)

        assertEquals(LocalDate.of(2026, 8, 9), week.start)
        assertEquals(
            listOf(
                DayDataState.Counted,
                DayDataState.Counted,
                DayDataState.Counted,
                DayDataState.Awaiting,
                DayDataState.Awaiting,
                DayDataState.Future,
                DayDataState.Future
            ),
            week.days.map { it.state }
        )
        assertEquals(19, week.total)
        assertEquals(3, week.countedDays)
        assertEquals(2, week.awaitingDays)
        assertEquals(LocalDate.of(2026, 8, 11), week.busiest?.date)
    }

    @Test
    fun `a counted day with no entry is zero, not awaiting`() {
        val byDate = history(
            LocalDate.of(2026, 8, 2) to 5,
            LocalDate.of(2026, 8, 4) to 8,
            LocalDate.of(2026, 8, 11) to 1
        )

        val week = buildActivityWeek(byDate, offset = -1, today = today)

        assertEquals(LocalDate.of(2026, 8, 2), week.start)
        assertTrue(week.days.all { it.state == DayDataState.Counted })
        assertEquals(0, week.days[1].amount)
        assertEquals(2, week.activeDays)
        assertEquals(13, week.total)
    }

    @Test
    fun `the day carries AniList's own bucket timestamp so the sheet can query it`() {
        val date = LocalDate.of(2026, 8, 10)
        val byDate = history(date to 2, LocalDate.of(2026, 8, 11) to 1)

        val week = buildActivityWeek(byDate, offset = 0, today = today)
        val monday = week.days.single { it.date == date }

        assertEquals(day(date, 2).date, monday.epochSeconds)
        assertNull(week.days.single { it.date == LocalDate.of(2026, 8, 12) }.epochSeconds)
    }

    @Test
    fun `the previous week comparison is skipped while that week is still partly uncounted`() {
        val byDate = history(
            LocalDate.of(2026, 8, 9) to 6,
            LocalDate.of(2026, 8, 11) to 4
        )

        assertNull(buildActivityWeek(byDate, offset = 0, today = today).changePercent)
    }

    @Test
    fun `a fully counted previous week gives a percent change`() {
        val byDate = history(
            LocalDate.of(2026, 7, 26) to 4,
            LocalDate.of(2026, 7, 28) to 6,
            LocalDate.of(2026, 8, 2) to 8,
            LocalDate.of(2026, 8, 4) to 7,
            LocalDate.of(2026, 8, 11) to 1
        )

        val week = buildActivityWeek(byDate, offset = -1, today = today)

        assertEquals(15, week.total)
        assertEquals(50, week.changePercent)
    }

    @Test
    fun `paging stops at the current week and at the oldest data AniList returned`() {
        val byDate = history(
            LocalDate.of(2026, 8, 2) to 5,
            LocalDate.of(2026, 8, 11) to 1
        )

        val current = buildActivityWeek(byDate, offset = 0, today = today)
        assertFalse(current.canGoForward)
        assertTrue(current.canGoBack)

        val oldest = buildActivityWeek(byDate, offset = -1, today = today)
        assertFalse(oldest.canGoBack)
        assertTrue(oldest.canGoForward)
    }

    @Test
    fun `the daily average divides by counted days, not by seven`() {
        val byDate = history(
            LocalDate.of(2026, 8, 9) to 6,
            LocalDate.of(2026, 8, 10) to 3,
            LocalDate.of(2026, 8, 11) to 10
        )

        val week = buildActivityWeek(byDate, offset = 0, today = today)

        assertEquals(19, week.total)
        assertEquals(3, week.countedDays)
        assertEquals("6.3", week.perDayLabel(Locale.US))
    }
}
