package com.anisync.android.presentation.statistics

import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.StudioStat

// MARK: - Actions

sealed interface StatisticsAction {
    data object Refresh : StatisticsAction
    data object Retry : StatisticsAction
}

// MARK: - UI States & Models

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data class Success(
        val data: StatisticsUiModel,
        val isRefreshing: Boolean = false
    ) : StatisticsUiState
    data class Error(val message: String) : StatisticsUiState
}

data class StatisticsUiModel(
    val animeStats: AnimeStatisticsUi,
    val mangaStats: MangaStatisticsUi?
)

data class AnimeStatisticsUi(
    val totalCount: Int,
    val daysWatched: Double, // Kept as Double for consistency/formatting
    val meanScore: Double,   // Changed to Double to match daysWatched and formatter
    val episodesWatched: Int,
    val scoreDistribution: List<ScoreUiModel>,
    val genreDistribution: List<GenreStat>,
    val formatDistribution: List<FormatStat>,
    val releaseYearDistribution: List<YearUiModel>,
    val studioDistribution: List<StudioStat>
)

data class MangaStatisticsUi(
    val totalCount: Int,
    val chaptersRead: Int,
    val meanScore: Double // Changed to Double
)

data class ScoreUiModel(
    val score: Int,
    val count: Int,
    val heightFraction: Float
)

data class YearUiModel(
    val year: Int,
    val count: Int,
    val heightFraction: Float
)
