package com.anisync.android.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** A span of minutes in whole days, hours and minutes. */
data class DurationParts(val days: Int, val hours: Int, val minutes: Int)

fun splitMinutes(totalMinutes: Int): DurationParts {
    val minutes = totalMinutes.coerceAtLeast(0)
    return DurationParts(days = minutes / MINUTES_PER_DAY, hours = minutes % MINUTES_PER_DAY / 60, minutes = minutes % 60)
}

/** Hours to one decimal, rounded half up, as the genre and studio hours read ("15.1 h"). */
fun formatHours(totalMinutes: Int, locale: Locale): String =
    oneDecimal(BigDecimal(totalMinutes).divide(BigDecimal(60), 1, RoundingMode.HALF_UP), locale)

/** A rate such as activities a day, to one decimal. */
fun formatOneDecimal(value: Double, locale: Locale): String =
    oneDecimal(BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP), locale)

/** A personal total, always exact: 13,170 episodes stays 13,170. */
fun formatExactCount(value: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)

/**
 * A community count such as popularity or favourites: exact up to 9,999, then cut to thousands or
 * millions without rounding up, so 28,410 reads 28K and 1,668 is never 1.7K.
 */
fun formatCommunityCount(value: Int, locale: Locale): String = when {
    value < 10_000 -> formatExactCount(value, locale)
    value < 1_000_000 -> formatExactCount(value / 1_000, locale) + "K"
    value < 10_000_000 -> oneDecimal(BigDecimal(value).divide(BigDecimal(1_000_000), 1, RoundingMode.DOWN), locale) + "M"
    else -> formatExactCount(value / 1_000_000, locale) + "M"
}

private fun oneDecimal(value: BigDecimal, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(value)

private const val MINUTES_PER_DAY = 24 * 60
