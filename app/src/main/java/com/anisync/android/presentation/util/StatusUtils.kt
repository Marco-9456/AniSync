package com.anisync.android.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType

@Composable
fun LibraryStatus.toLabel(type: MediaType?): String {
    val isManga = type == MediaType.MANGA
    return when(this) {
        LibraryStatus.CURRENT -> if (isManga) stringResource(R.string.status_reading) else stringResource(R.string.status_watching)
        LibraryStatus.PLANNING -> stringResource(R.string.status_planning)
        LibraryStatus.COMPLETED -> stringResource(R.string.status_completed)
        LibraryStatus.PAUSED -> stringResource(R.string.status_paused)
        LibraryStatus.DROPPED -> stringResource(R.string.status_dropped)
        LibraryStatus.REPEATING -> if (isManga) stringResource(R.string.status_rereading) else stringResource(R.string.status_rewatching)
        LibraryStatus.UNKNOWN -> stringResource(R.string.unknown)
    }
}

/** Status accent color, shared by the Following cards and the profile list rows. */
fun LibraryStatus.toColor(): Color = when (this) {
    LibraryStatus.CURRENT -> Color(0xFF4CAF50)
    LibraryStatus.COMPLETED -> Color(0xFF2196F3)
    LibraryStatus.PLANNING -> Color(0xFF9C27B0)
    LibraryStatus.PAUSED -> Color(0xFFFF9800)
    LibraryStatus.DROPPED -> Color(0xFFF44336)
    LibraryStatus.REPEATING -> Color(0xFF009688)
    LibraryStatus.UNKNOWN -> Color(0xFF9E9E9E)
}

fun LibraryStatus.toIcon(type: MediaType?): ImageVector {
    val isManga = type == MediaType.MANGA
    return when(this) {
        LibraryStatus.CURRENT -> if (isManga) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PlayArrow
        LibraryStatus.PLANNING -> Icons.Default.Event
        LibraryStatus.COMPLETED -> Icons.Default.Check
        LibraryStatus.DROPPED -> Icons.Default.Delete
        LibraryStatus.PAUSED -> Icons.Default.Pause
        LibraryStatus.REPEATING -> Icons.Default.Repeat
        else -> Icons.Default.Add
    }
}

@Composable
fun MediaStatus.toLabel(): String {
    return when (this) {
        MediaStatus.RELEASING -> stringResource(R.string.media_status_airing)
        MediaStatus.FINISHED -> stringResource(R.string.media_status_finished)
        MediaStatus.NOT_YET_RELEASED -> stringResource(R.string.media_status_not_yet_released)
        MediaStatus.CANCELLED -> stringResource(R.string.media_status_cancelled)
        MediaStatus.HIATUS -> stringResource(R.string.media_status_hiatus)
        MediaStatus.UNKNOWN__ -> ""
    }
}

/**
 * Converts a MediaFormat enum to a user-friendly display label.
 * For example: TV -> "TV Series", MOVIE -> "Movie", OVA -> "OVA", etc.
 */
@Composable
fun MediaFormat.toLabel(): String {
    return when (this) {
        MediaFormat.TV -> stringResource(R.string.format_tv)
        MediaFormat.TV_SHORT -> stringResource(R.string.format_tv_short)
        MediaFormat.MOVIE -> stringResource(R.string.format_movie)
        MediaFormat.SPECIAL -> stringResource(R.string.format_special)
        MediaFormat.OVA -> stringResource(R.string.format_ova)
        MediaFormat.ONA -> stringResource(R.string.format_ona)
        MediaFormat.MUSIC -> stringResource(R.string.format_music)
        MediaFormat.MANGA -> stringResource(R.string.media_type_manga)
        MediaFormat.NOVEL -> stringResource(R.string.format_novel)
        MediaFormat.ONE_SHOT -> stringResource(R.string.format_one_shot)
        MediaFormat.UNKNOWN__ -> stringResource(R.string.unknown)
    }
}

/**
 * Formats a snake_case string (e.g., "SOME_STATUS") to Title Case (e.g., "Some Status").
 * Returns null if the input is null.
 */
fun String?.formatAsTitle(): String? {
    if (this == null) return null
    return this.replace("_", " ")
        .split(" ")
        .joinToString(" ") { word ->
            when (word.uppercase()) {
                "TV", "OVA", "ONA" -> word.uppercase()
                else -> word.lowercase().replaceFirstChar { char -> char.uppercase() }
            }
        }
}


