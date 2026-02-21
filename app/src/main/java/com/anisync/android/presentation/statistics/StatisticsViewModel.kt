package com.anisync.android.presentation.statistics

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anisync.android.domain.AnimeStatistics
import com.anisync.android.domain.MangaStatistics
import com.anisync.android.domain.Result
import com.anisync.android.domain.StatisticsRepository
import com.anisync.android.presentation.navigation.Statistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    init {
        loadStatistics()
    }
    
    fun onAction(action: StatisticsAction) {
        when (action) {
            is StatisticsAction.Refresh -> refresh()
            is StatisticsAction.Retry -> loadStatistics()
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = StatisticsUiState.Loading
            fetchAndProcessData()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is StatisticsUiState.Success) {
                if (currentState.isRefreshing) return@launch
                _uiState.update { currentState.copy(isRefreshing = true) }
                fetchAndProcessData()
            }
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

                _uiState.value = StatisticsUiState.Success(data = processedData, isRefreshing = false)

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
}