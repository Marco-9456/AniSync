package com.anisync.android.domain

/**
 * Community statistics for a single media, mirroring the sections of AniList's
 * media Stats tab: rankings, recent activity, airing progression and the
 * score/status distributions.
 */
data class MediaStats(
    val rankings: List<MediaRanking> = emptyList(),
    val recentActivity: List<MediaTrendPoint> = emptyList(),
    val airingProgression: List<MediaAiringTrend> = emptyList(),
    val scoreDistribution: List<MediaScoreSlice> = emptyList(),
    val statusDistribution: List<MediaStatusSlice> = emptyList()
) {
    val isEmpty: Boolean
        get() = rankings.isEmpty() && recentActivity.isEmpty() &&
            airingProgression.isEmpty() && scoreDistribution.isEmpty() &&
            statusDistribution.isEmpty()
}

/**
 * One "#N Highest Rated / Most Popular …" entry from `Media.rankings`.
 * Serializable because it is also cached inside [MediaDetails]'s Room row.
 */
@kotlinx.serialization.Serializable
data class MediaRanking(
    val rank: Int,
    val type: MediaRankingType,
    val year: Int?,
    /** AniList season name (e.g. "SPRING") when the ranking is seasonal. */
    val season: String?,
    val allTime: Boolean,
    /** Human-readable scope from the API, e.g. "highest rated all time". */
    val context: String
)

@kotlinx.serialization.Serializable
enum class MediaRankingType { RATED, POPULAR }

/** One day of sitewide activity for the media (`Media.trends`). */
data class MediaTrendPoint(
    val dateSeconds: Long,
    val activity: Int
)

/**
 * Score/watcher snapshot for one released episode (`trends(releasing: true)`).
 * The API records a row per day; this is the latest (most settled) row for the
 * episode, and [dateSeconds] is the day it was recorded.
 */
data class MediaAiringTrend(
    val episode: Int,
    val dateSeconds: Long,
    val averageScore: Int?,
    val watching: Int?
)

/** Amount of list entries holding one score bucket (10..100). */
data class MediaScoreSlice(val score: Int, val amount: Int)

/** Amount of list entries in one watching/reading status. */
data class MediaStatusSlice(val status: String, val amount: Int)
