package com.anisync.android.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType
import java.util.Locale

/** Format and year, which every result card leads its facts with: "TV · 2023". */
@Composable
fun LibraryEntry.formatAndYear(): String? = formatAndYear(format, releaseYear)

@Composable
fun formatAndYear(format: MediaFormat?, year: Int?): String? =
    listOfNotNull(format?.toLabel(), year?.toString()).joinToString(" · ").ifEmpty { null }

/**
 * "28 eps" or "120 ch". Null when AniList has no length, and for a single episode or chapter,
 * where the format already says it: a movie is one episode.
 */
@Composable
fun LibraryEntry.lengthFact(mediaType: MediaType? = type): String? {
    val manga = mediaType == MediaType.MANGA
    val total = (if (manga) totalChapters else totalEpisodes)?.takeIf { it > 1 } ?: return null
    return stringResource(
        if (manga) R.string.library_chapter_count else R.string.library_episode_count,
        total
    )
}

/** Where the title is in its run: "Releasing", "Finished", "Not yet released". */
@Composable
fun LibraryEntry.releaseStatusLabel(): String? = mediaStatus
    ?.let { MediaStatus.safeValueOf(it) }
    ?.takeIf { it != MediaStatus.UNKNOWN__ }
    ?.toLabel()

/** The community score on the ten-point scale result cards print it in: "8.7". */
fun LibraryEntry.communityScoreLabel(): String? = averageScore
    ?.takeIf { it > 0 }
    ?.let { String.format(Locale.US, "%.1f", it / 10.0) }
