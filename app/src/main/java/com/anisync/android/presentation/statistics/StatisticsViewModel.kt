package com.anisync.android.presentation.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.Result
import com.anisync.android.domain.StatisticsRepository
import com.anisync.android.domain.UserStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Statistics screen.
 * Handles fetching and exposing user statistics data.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get userId from navigation arguments
    private val userId: Int = savedStateHandle.get<Int>("userId") ?: 0

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
            
            when (val result = statisticsRepository.getUserStatistics(userId)) {
                is Result.Success -> {
                    _uiState.value = StatisticsUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = StatisticsUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Refresh statistics data with pull-to-refresh indicator.
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            when (val result = statisticsRepository.getUserStatistics(userId)) {
                is Result.Success -> {
                    _uiState.value = StatisticsUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = StatisticsUiState.Error(result.message)
                }
            }
            
            _isRefreshing.value = false
        }
    }

    fun retry() {
        loadStatistics()
    }
}

/**
 * UI State for the Statistics screen.
 */
sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data class Success(val statistics: UserStatistics) : StatisticsUiState
    data class Error(val message: String) : StatisticsUiState
}
