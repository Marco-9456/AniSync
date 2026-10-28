package com.anisync.android.domain.recap

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.ZonedDateTime

/** How many days the Library banner keeps pointing at a new recap. */
const val RECAP_BANNER_DAYS = 7

/**
 * When the recap job runs next: the 1st of the coming month at [time], or today when today is the
 * 1st and [time] is still ahead.
 */
fun nextRecapRun(clock: Clock, time: LocalTime): ZonedDateTime {
    val now = ZonedDateTime.now(clock)
    val today = now.toLocalDate()
    val firstOfMonth = today.withDayOfMonth(1)
    val thisMonth = firstOfMonth.atTime(time).atZone(now.zone)
    return if (today.dayOfMonth == 1 && thisMonth.isAfter(now)) {
        thisMonth
    } else {
        firstOfMonth.plusMonths(1).atTime(time).atZone(now.zone)
    }
}

/** The recaps that become ready on [date]: last month on every 1st, and last year on 1 January. */
fun recapsDueOn(date: LocalDate): List<RecapPeriod> {
    if (date.dayOfMonth != 1) return emptyList()
    val lastMonth = RecapPeriod.Month(YearMonth.from(date).minusMonths(1))
    return if (date.month == Month.JANUARY) listOf(RecapPeriod.Year(date.year - 1), lastMonth) else listOf(lastMonth)
}

/**
 * The recaps the Library banner may point at on [today], the preferred one first, during the first
 * [RECAP_BANNER_DAYS] days of a month. In January the year comes before December.
 */
fun recapBannerCandidates(today: LocalDate): List<RecapPeriod> {
    if (today.dayOfMonth > RECAP_BANNER_DAYS) return emptyList()
    return recapsDueOn(today.withDayOfMonth(1))
}
