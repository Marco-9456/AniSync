package com.anisync.android.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.anisync.android.R

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
