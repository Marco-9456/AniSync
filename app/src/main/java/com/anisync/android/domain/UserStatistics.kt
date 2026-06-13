package com.anisync.android.domain

import androidx.compose.runtime.Immutable

/**
 * Comprehensive user statistics for anime/manga consumption.
 */
@Immutable
data class UserStatistics(
    val userId: Int,
    val userName: String,
    val scoreFormat: ScoreFormat? = null,
    val animeStats: AnimeStatistics,
    val mangaStats: MangaStatistics?,
    val activityHistory: List<ActivityHistoryDay> = emptyList()
)

/**
 * One day in the profile activity heatmap. [date] is the AniList day bucket (Unix
 * seconds); [level] is AniList's own 1-10 intensity, used directly to color the cell.
 */
@Immutable
data class ActivityHistoryDay(
    val date: Long,
    val amount: Int,
    val level: Int
)

@Immutable
data class AnimeStatistics(
    val totalCount: Int,
    val episodesWatched: Int,
    val minutesWatched: Int,
    val daysWatched: Float,
    val meanScore: Float,
    val standardDeviation: Float,
    val statusDistribution: List<StatusStat>,
    val genreDistribution: List<GenreStat>,
    val tagDistribution: List<TagStat>,
    val scoreDistribution: List<ScoreStat>,
    val formatDistribution: List<FormatStat>,
    val releaseYearDistribution: List<ReleaseYearStat>,
    val startYearDistribution: List<StartYearStat>,
    val lengthDistribution: List<LengthStat>,
    val studioDistribution: List<StudioStat>,
    val voiceActorDistribution: List<VoiceActorStat>,
    val staffDistribution: List<StaffStat>,
    val countryDistribution: List<CountryStat>
)

@Immutable
data class MangaStatistics(
    val totalCount: Int,
    val chaptersRead: Int,
    val volumesRead: Int,
    val meanScore: Float,
    val standardDeviation: Float,
    val statusDistribution: List<StatusStat>,
    val genreDistribution: List<GenreStat>,
    val tagDistribution: List<TagStat>,
    val scoreDistribution: List<ScoreStat>,
    val formatDistribution: List<FormatStat>,
    val releaseYearDistribution: List<ReleaseYearStat>,
    val startYearDistribution: List<StartYearStat>,
    val lengthDistribution: List<LengthStat>,
    val staffDistribution: List<StaffStat>,
    val countryDistribution: List<CountryStat>
)

@Immutable
data class StatusStat(
    val status: String,
    val count: Int
)

@Immutable
@kotlinx.serialization.Serializable
data class GenreStat(
    val genre: String,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float = 0f
)

@Immutable
data class TagStat(
    val id: Int,
    val name: String,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

@Immutable
data class ScoreStat(
    val score: Int,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

@Immutable
data class FormatStat(
    val format: String,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

@Immutable
data class ReleaseYearStat(
    val year: Int,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

@Immutable
data class StartYearStat(
    val year: Int,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

@Immutable
data class LengthStat(
    val length: String,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

@Immutable
data class StudioStat(
    val id: Int,
    val studioName: String,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

@Immutable
data class VoiceActorStat(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

@Immutable
data class StaffStat(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)

@Immutable
data class CountryStat(
    val countryCode: String,
    val count: Int,
    val meanScore: Float,
    val hoursWatched: Float
)
