package com.anisync.android.domain.recap

/** How a month's episodes compare with the month before, the words WM2 puts under the bars. */
enum class PeriodChange { MORE_THAN_TWICE, MORE, ABOUT_THE_SAME, FEWER, LESS_THAN_HALF }

/** Null when there is nothing to compare with. */
fun comparePeriods(current: Int, previous: Int?): PeriodChange? {
    if (previous == null || previous <= 0) return null
    val ratio = current.toDouble() / previous
    return when {
        ratio > 2.0 -> PeriodChange.MORE_THAN_TWICE
        ratio >= 1.15 -> PeriodChange.MORE
        ratio > 0.85 -> PeriodChange.ABOUT_THE_SAME
        ratio >= 0.5 -> PeriodChange.FEWER
        else -> PeriodChange.LESS_THAN_HALF
    }
}
