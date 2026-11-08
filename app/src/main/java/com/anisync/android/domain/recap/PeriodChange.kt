package com.anisync.android.domain.recap

import kotlin.math.roundToInt

/** How a month's episodes compare with the month before, the words WM2 puts under the bars. */
enum class PeriodChange { ALMOST_TIMES, TIMES, MORE_THAN_TIMES, MORE, ABOUT_THE_SAME, FEWER, LESS_THAN_HALF }

/** A [change], with the whole [multiple] it is close to when the month at least nearly doubled. */
data class PeriodComparison(val change: PeriodChange, val multiple: Int = 0)

/** The highest multiple the words spell out, more than that reads "more than ten times". */
private const val MAX_MULTIPLE = 10

/** How close to a whole multiple still reads as that multiple rather than almost or more than it. */
private const val MULTIPLE_SLACK = 0.05

/** Null when there is nothing to compare with. 62 against 23 is almost three times, 40 against 20 twice. */
fun comparePeriods(current: Int, previous: Int?): PeriodComparison? {
    if (previous == null || previous <= 0) return null
    val ratio = current.toDouble() / previous
    return when {
        ratio >= 2.0 - 3 * MULTIPLE_SLACK -> {
            val multiple = ratio.roundToInt().coerceIn(2, MAX_MULTIPLE)
            when {
                ratio > MAX_MULTIPLE + MULTIPLE_SLACK -> PeriodComparison(PeriodChange.MORE_THAN_TIMES, MAX_MULTIPLE)
                ratio < multiple - MULTIPLE_SLACK -> PeriodComparison(PeriodChange.ALMOST_TIMES, multiple)
                ratio <= multiple + MULTIPLE_SLACK -> PeriodComparison(PeriodChange.TIMES, multiple)
                else -> PeriodComparison(PeriodChange.MORE_THAN_TIMES, multiple)
            }
        }
        ratio >= 1.15 -> PeriodComparison(PeriodChange.MORE)
        ratio > 0.85 -> PeriodComparison(PeriodChange.ABOUT_THE_SAME)
        ratio >= 0.5 -> PeriodComparison(PeriodChange.FEWER)
        else -> PeriodComparison(PeriodChange.LESS_THAN_HALF)
    }
}
