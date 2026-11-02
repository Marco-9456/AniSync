package com.anisync.android.domain.recap

import java.time.LocalDate
import java.time.YearMonth

/** How many finished months of recaps a device keeps, a year and the month before it. */
const val DEFAULT_KEPT_MONTHS = 13

/** The oldest month whose recap a device still keeps on [today]. */
fun firstKeptMonth(today: LocalDate, keptMonths: Int): YearMonth =
    YearMonth.from(today).minusMonths(keptMonths.coerceAtLeast(1).toLong())

/** The stored periods that ended before [firstKeptMonth], which retention deletes. */
fun expiredPeriods(periods: Collection<RecapPeriod>, today: LocalDate, keptMonths: Int): List<RecapPeriod> {
    val keptFrom = firstKeptMonth(today, keptMonths).atDay(1)
    return periods.filter { it.lastDay.isBefore(keptFrom) }
}

/** True once [today] is past the last day of the period. */
fun RecapPeriod.isFinishedOn(today: LocalDate): Boolean = today.isAfter(lastDay)

/**
 * What a first build stores on [today]: last year, and every month from the start of last year to
 * the month in progress, leaving out whatever retention would delete straight away.
 */
fun backfillPeriods(today: LocalDate, keptMonths: Int): List<RecapPeriod> {
    val current = YearMonth.from(today)
    val months = generateSequence(YearMonth.of(today.year - 1, 1)) { it.plusMonths(1) }
        .takeWhile { it <= current }
        .map { RecapPeriod.Month(it) }
        .toList()
    val periods = listOf(RecapPeriod.Year(today.year - 1)) + months
    return periods - expiredPeriods(periods, today, keptMonths).toSet()
}
