package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.GetPopularAnimeUseCase
import com.anisync.android.domain.GetTrendingAnimeUseCase
import com.anisync.android.domain.GetUpcomingAnimeUseCase
import com.anisync.android.domain.LibraryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Success(
        val trending: List<LibraryEntry>,
        val popular: List<LibraryEntry>,
        val upcoming: List<LibraryEntry>
    ) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val getTrendingAnimeUseCase: GetTrendingAnimeUseCase,
    private val getPopularAnimeUseCase: GetPopularAnimeUseCase,
    private val getUpcomingAnimeUseCase: GetUpcomingAnimeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadDiscoveryData()
    }

    private fun loadDiscoveryData() {
        viewModelScope.launch {
            _uiState.update { DiscoverUiState.Loading }
            try {
                // Fetch all lists in parallel
                val trendingDeferred = async { getTrendingAnimeUseCase() }
                val popularDeferred = async { getPopularAnimeUseCase() }
                val upcomingDeferred = async { getUpcomingAnimeUseCase() }

                val trending = trendingDeferred.await()
                val popular = popularDeferred.await()
                val upcoming = upcomingDeferred.await()

                _uiState.update {
                    DiscoverUiState.Success(
                        trending = trending,
                        popular = popular,
                        upcoming = upcoming
                    )
                }
            } catch (e: Exception) {
                _uiState.update { DiscoverUiState.Error(e.message ?: "Unknown error") }
            }
        }
    }
}

