package com.anisync.android.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import com.anisync.android.R
import java.text.NumberFormat

/**
 * Formats time until airing in a locale-aware manner using plural resources.
 * Returns the most significant time unit (days > hours > minutes).
 *
 * @param seconds The time in seconds until airing
 * @return A formatted string like "5d", "3h", or "45m"
 */
@Composable
fun formatTimeUntilAiring(seconds: Int): String {
    val days = seconds / 86400
    if (days > 0) return pluralStringResource(R.plurals.time_days, days, days)
    
    val hours = (seconds % 86400) / 3600
    if (hours > 0) return pluralStringResource(R.plurals.time_hours, hours, hours)
    
    val minutes = (seconds % 3600) / 60
    return pluralStringResource(R.plurals.time_minutes, minutes, minutes)
}

/**
 * Adaptive D:H:M:S countdown formatter. Drops higher units when zero.
 *   95412 -> "1d 02:30:12"
 *    8412 -> "02:20:12"
 *     412 -> "06:52"
 *      12 -> "00:12"
 */
fun formatCountdownAdaptive(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "00:00"
    val d = totalSeconds / 86400
    val h = (totalSeconds % 86400) / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        d > 0 -> "%dd %02d:%02d:%02d".format(d, h, m, s)
        h > 0 -> "%02d:%02d:%02d".format(h, m, s)
        else  -> "%02d:%02d".format(m, s)
    }
}

/**
 * Compact human-readable number: 1234 -> "1.2K", 1500000 -> "1.5M".
 * Integer values stay integer ("1K" not "1.0K").
 */
fun formatCompactNumber(value: Int): String = when {
    value >= 1_000_000 -> {
        val v = value / 1_000_000.0
        if (v % 1.0 == 0.0) "%.0fM".format(v) else "%.1fM".format(v)
    }
    value >= 1_000 -> {
        val v = value / 1_000.0
        if (v % 1.0 == 0.0) "%.0fK".format(v) else "%.1fK".format(v)
    }
    else -> value.toString()
}

/**
 * Formats the number of episodes behind using plural resources.
 *
 * @param count The number of episodes behind
 * @return A formatted string like "1 EP BEHIND" or "3 EPS BEHIND"
 */
@Composable
fun formatEpisodesBehind(count: Int): String {
    return pluralStringResource(R.plurals.episodes_behind, count, count)
}

/**
 * Formats episode count using plural resources.
 *
 * @param count The number of episodes
 * @return A formatted string like "1 Ep" or "12 Eps"
 */
@Composable
fun formatEpisodesCount(count: Int): String {
    return pluralStringResource(R.plurals.episodes_count, count, count)
}

/**
 * Formats chapter count using plural resources.
 *
 * @param count The number of chapters
 * @return A formatted string like "1 Ch" or "10 Ch"
 */
@Composable
fun formatChaptersCount(count: Int): String {
    return pluralStringResource(R.plurals.chapters_count, count, count)
}

/**
 * Formats a decimal number using the device's current locale.
 * Respects locale-specific decimal separators (e.g., "." for US, "," for Germany).
 *
 * @param value The decimal value to format
 * @param fractionDigits The number of decimal places (default: 1)
 * @return A locale-formatted string like "8.5" (US) or "8,5" (DE)
 */
@Composable
fun formatDecimal(value: Double, fractionDigits: Int = 1): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale, fractionDigits) {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }
    }
    return formatter.format(value)
}

/**
 * Formats a decimal number using the device's current locale.
 * Float overload for convenience.
 *
 * @param value The float value to format
 * @param fractionDigits The number of decimal places (default: 1)
 * @return A locale-formatted string like "8.5" (US) or "8,5" (DE)
 */
@Composable
fun formatDecimal(value: Float, fractionDigits: Int = 1): String {
    return formatDecimal(value.toDouble(), fractionDigits)
}
