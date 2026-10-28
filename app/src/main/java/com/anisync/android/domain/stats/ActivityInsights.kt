package com.anisync.android.domain.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * The Activity view's insights, read from AniList's activity history. AniList only keeps about
 * [ACTIVITY_RETENTION_DAYS] days, so the window runs from the oldest day it kept to the last day it
 * has counted, and a streak that reaches back to the oldest kept day may be longer than it shows.
 */
data class ActivityInsights(
    val total: Int,
    val days: Int,
    val longestStreak: Int,
    /** True when the longest streak starts on the oldest day AniList still keeps. */
    val streakIsOpenEnded: Boolean,
    val busiestWeekday: DayOfWeek,
    val busiestWeekdayTotal: Int,
    val busiestDay: LocalDate,
    val busiestDayTotal: Int,
    val byWeekday: Map<DayOfWeek, Int>,
    val byMonth: List<MonthActivity>,
) {
    val perDay: Double get() = total.toDouble() / days
}

data class MonthActivity(
    val month: YearMonth,
    val total: Int,
    /** The first and last day of the month that fall inside the window. */
    val firstDay: LocalDate,
    val lastDay: LocalDate,
) {
    val startsLate: Boolean get() = firstDay.dayOfMonth > 1
    val endsEarly: Boolean get() = lastDay != month.atEndOfMonth()
}

/** Null when there is no activity to read. */
fun activityInsights(amounts: Map<LocalDate, Int>): ActivityInsights? {
    val active = amounts.filterValues { it > 0 }
    if (active.isEmpty()) return null
    val first = amounts.keys.min()
    val last = amounts.keys.max()
    val days = (last.toEpochDay() - first.toEpochDay() + 1).toInt()

    var bestRun = 0
    var bestStart = first
    var run = 0
    var runStart = first
    var day = first
    while (!day.isAfter(last)) {
        if ((amounts[day] ?: 0) > 0) {
            if (run == 0) runStart = day
            run++
            if (run >= bestRun) {
                bestRun = run
                bestStart = runStart
            }
        } else {
            run = 0
        }
        day = day.plusDays(1)
    }

    val byWeekday = DayOfWeek.entries.associateWith { 0 }.toMutableMap()
    active.forEach { (date, amount) -> byWeekday[date.dayOfWeek] = byWeekday.getValue(date.dayOfWeek) + amount }
    val busiestWeekday = byWeekday.entries.maxWith(compareBy { it.value }).key
    val busiestDay = active.entries.maxWith(compareBy({ it.value }, { it.key }))

    val byMonth = generateSequence(YearMonth.from(first)) { it.plusMonths(1) }
        .takeWhile { !it.isAfter(YearMonth.from(last)) }
        .map { month ->
            val from = maxOf(month.atDay(1), first)
            val to = minOf(month.atEndOfMonth(), last)
            MonthActivity(
                month = month,
                total = active.filterKeys { !it.isBefore(from) && !it.isAfter(to) }.values.sum(),
                firstDay = from,
                lastDay = to,
            )
        }
        .toList()

    return ActivityInsights(
        total = active.values.sum(),
        days = days,
        longestStreak = bestRun,
        streakIsOpenEnded = bestStart == first && days >= ACTIVITY_RETENTION_DAYS - RECOUNT_LAG_DAYS,
        busiestWeekday = busiestWeekday,
        busiestWeekdayTotal = byWeekday.getValue(busiestWeekday),
        busiestDay = busiestDay.key,
        busiestDayTotal = busiestDay.value,
        byWeekday = byWeekday,
        byMonth = byMonth,
    )
}

/** How many days of activity history AniList keeps. */
const val ACTIVITY_RETENTION_DAYS = 183

/** AniList recounts the history about every two days, so the newest days can be missing. */
private const val RECOUNT_LAG_DAYS = 3
