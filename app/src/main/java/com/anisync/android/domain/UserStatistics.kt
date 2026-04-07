package com.anisync.android.domain

import androidx.compose.runtime.Immutable

/**
 * Comprehensive user statistics for anime/manga consumption.
 * Contains detailed breakdowns by genre, format, score distribution, etc.
 */
@Immutable
data class UserStatistics(
    val userId: Int,
    val userName: String,
    val animeStats: AnimeStatistics,
    val mangaStats: MangaStatistics?
)

/**
 * Anime-specific statistics.
 */
@Immutable
data class AnimeStatistics(
    val totalCount: Int,
    val episodesWatched: Int,
    val minutesWatched: Int,
    val daysWatched: Float, // Converted from minutes
    val meanScore: Float,
    val statusDistribution: List<StatusStat>,
    val genreDistribution: List<GenreStat>,
    val scoreDistribution: List<ScoreStat>,
    val formatDistribution: List<FormatStat>,
    val releaseYearDistribution: List<ReleaseYearStat>,
    val studioDistribution: List<StudioStat>
)

/**
 * Manga-specific statistics.
 */
@Immutable
data class MangaStatistics(
    val totalCount: Int,
    val chaptersRead: Int,
    val meanScore: Float
)

/**
 * Statistics for a specific status (watching, completed, etc.)
 */
@Immutable
data class StatusStat(
    val status: String,
    val count: Int
)

/**
 * Statistics for a specific genre.
 */
@Immutable
@kotlinx.serialization.Serializable
data class GenreStat(
    val genre: String,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float = 0f // Converted from minutes
)

/**
 * Statistics for a specific score value (1-10 or 1-100 depending on user settings).
 */
@Immutable
data class ScoreStat(
    val score: Int,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

/**
 * Statistics for a specific format (TV, Movie, OVA, etc.)
 */
@Immutable
data class FormatStat(
    val format: String,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

/**
 * Statistics for a specific release year.
 */
@Immutable
data class ReleaseYearStat(
    val year: Int,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

/**
 * Statistics for a specific studio.
 */
@Immutable
data class StudioStat(
    val studioName: String,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)
