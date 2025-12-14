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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
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
