package com.anisync.android.presentation.statistics

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anisync.android.domain.AnimeStatistics
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.MangaStatistics
import com.anisync.android.domain.Result
import com.anisync.android.domain.StatisticsRepository
import com.anisync.android.domain.StudioStat
import com.anisync.android.presentation.navigation.Statistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Statistics screen.
 * Handles fetching user statistics and transforming them into optimized UI models
 * on a background thread to ensure smooth UI performance.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Int = savedStateHandle.toRoute<Statistics>().userId

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = StatisticsUiState.Loading
            fetchAndProcessData()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true
            fetchAndProcessData()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchAndProcessData() {
        val startTime = SystemClock.elapsedRealtime()

        when (val result = statisticsRepository.getUserStatistics(userId)) {
            is Result.Success -> {
                // Perform data transformation on Default dispatcher to keep the UI thread free
                val processedData = withContext(Dispatchers.Default) {
                    val animeUi = processAnimeStats(result.data.animeStats)
                    val mangaUi = result.data.mangaStats?.let { processMangaStats(it) }

                    StatisticsUiModel(animeUi, mangaUi)
                }

                _uiState.value = StatisticsUiState.Success(processedData)

                val duration = SystemClock.elapsedRealtime() - startTime
                Log.d("StatisticsPerf", "Data fetch and process took: ${duration}ms")
            }

            is Result.Error -> {
                _uiState.value = StatisticsUiState.Error(result.message)
            }
        }
    }

    /**
     * Pre-calculates distribution data for the UI to avoid expensive operations during composition.
     */
    private fun processAnimeStats(stats: AnimeStatistics): AnimeStatisticsUi {
        // Fill Score Gaps
        val maxScoreCount = stats.scoreDistribution.maxOfOrNull { it.count } ?: 1
        val fullScoreDistribution = (1..10).map { score ->
            val count = stats.scoreDistribution.find { it.score == score }?.count ?: 0
            ScoreUiModel(score, count, count.toFloat() / maxScoreCount.coerceAtLeast(1))
        }

        // Sort and Limit Years (Take last 10)
        val sortedYears = stats.releaseYearDistribution
            .sortedBy { it.year }
            .takeLast(10)

        val maxYearCount = sortedYears.maxOfOrNull { it.count } ?: 1
        val processedYears = sortedYears.map {
            YearUiModel(it.year, it.count, it.count.toFloat() / maxYearCount.coerceAtLeast(1))
        }

        // Limit heavy lists to top 20 for summary views
        val topGenres = stats.genreDistribution.take(20)
        val topStudios = stats.studioDistribution.take(20)

        return AnimeStatisticsUi(
            totalCount = stats.totalCount,
            daysWatched = stats.daysWatched.toDouble(), // Convert Float to Double
            meanScore = stats.meanScore.toDouble(),     // Convert Float to Double
            episodesWatched = stats.episodesWatched,
            scoreDistribution = fullScoreDistribution,
            genreDistribution = topGenres,
            formatDistribution = stats.formatDistribution,
            releaseYearDistribution = processedYears,
            studioDistribution = topStudios
        )
    }

    private fun processMangaStats(stats: MangaStatistics): MangaStatisticsUi {
        return MangaStatisticsUi(
            totalCount = stats.totalCount,
            chaptersRead = stats.chaptersRead,
            meanScore = stats.meanScore.toDouble() // Convert Float to Double
        )
    }

    fun retry() {
        loadStatistics()
    }
}

// MARK: - UI States & Models

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data class Success(val data: StatisticsUiModel) : StatisticsUiState
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