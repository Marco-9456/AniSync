package com.anisync.android.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class ActivityInsightsTest {

    private val oldest = LocalDate.of(2026, 4, 27)

    private fun everyDay(days: Int, amount: (LocalDate) -> Int = { 2 }): Map<LocalDate, Int> =
        (0 until days).associate { oldest.plusDays(it.toLong()).let { date -> date to amount(date) } }

    @Test
    fun `no activity has no insights`() {
        assertNull(activityInsights(emptyMap()))
        assertNull(activityInsights(mapOf(oldest to 0)))
    }

    @Test
    fun `a streak over every kept day is open ended`() {
        val insights = activityInsights(everyDay(ACTIVITY_RETENTION_DAYS))!!
        assertEquals(183, insights.longestStreak)
        assertTrue(insights.streakIsOpenEnded)
    }

    @Test
    fun `a streak that starts after the oldest kept day is exact`() {
        val days = everyDay(ACTIVITY_RETENTION_DAYS) { if (it == oldest.plusDays(10)) 0 else 1 }
        val insights = activityInsights(days)!!
        assertEquals(172, insights.longestStreak)
        assertFalse(insights.streakIsOpenEnded)
    }

    @Test
    fun `a short history is never open ended`() {
        val insights = activityInsights(everyDay(30))!!
        assertEquals(30, insights.longestStreak)
        assertFalse(insights.streakIsOpenEnded)
    }

    @Test
    fun `a streak runs across a month boundary`() {
        val days = mapOf(
            LocalDate.of(2026, 9, 28) to 1,
            LocalDate.of(2026, 9, 29) to 1,
            LocalDate.of(2026, 9, 30) to 3,
            LocalDate.of(2026, 10, 1) to 1,
            LocalDate.of(2026, 10, 2) to 0,
            LocalDate.of(2026, 10, 3) to 1,
        )
        val insights = activityInsights(days)!!
        assertEquals(4, insights.longestStreak)
        assertEquals(LocalDate.of(2026, 9, 28), insights.streakStart)
        assertEquals(LocalDate.of(2026, 10, 1), insights.streakEnd)
    }

    @Test
    fun `per day spreads the total over the whole window`() {
        val days = mapOf(oldest to 4, oldest.plusDays(3) to 2)
        val insights = activityInsights(days)!!
        assertEquals(4, insights.days)
        assertEquals(1.5, insights.perDay, 0.0001)
    }

    @Test
    fun `the busiest weekday and day are by amount`() {
        val sunday = LocalDate.of(2026, 7, 12)
        val days = mapOf(
            sunday to 8,
            sunday.plusDays(7) to 3,
            sunday.plusDays(1) to 5,
            sunday.plusDays(2) to 1,
        )
        val insights = activityInsights(days)!!
        assertEquals(DayOfWeek.SUNDAY, insights.busiestWeekday)
        assertEquals(11, insights.busiestWeekdayTotal)
        assertEquals(sunday, insights.busiestDay)
        assertEquals(8, insights.busiestDayTotal)
        assertEquals(7, insights.byWeekday.size)
    }

    @Test
    fun `a tie for the busiest day goes to the later one`() {
        val days = mapOf(oldest to 6, oldest.plusDays(5) to 6)
        assertEquals(oldest.plusDays(5), activityInsights(days)!!.busiestDay)
    }

    @Test
    fun `months at the edges of the window are marked partial`() {
        val days = mapOf(oldest to 1, LocalDate.of(2026, 10, 27) to 2, LocalDate.of(2026, 6, 15) to 4)
        val months = activityInsights(days)!!.byMonth
        assertEquals(YearMonth.of(2026, 4), months.first().month)
        assertTrue(months.first().startsLate)
        assertEquals(27, months.first().firstDay.dayOfMonth)
        assertEquals(YearMonth.of(2026, 10), months.last().month)
        assertTrue(months.last().endsEarly)
        val june = months.single { it.month == YearMonth.of(2026, 6) }
        assertEquals(4, june.total)
        assertFalse(june.startsLate)
        assertFalse(june.endsEarly)
        assertEquals(0, months.single { it.month == YearMonth.of(2026, 5) }.total)
    }
}
