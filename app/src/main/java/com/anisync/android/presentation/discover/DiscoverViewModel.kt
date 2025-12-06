package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.type.MediaType
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
    private val discoverRepository: DiscoverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _mediaType = MutableStateFlow(MediaType.ANIME)
    val mediaType: StateFlow<MediaType> = _mediaType.asStateFlow()

    init {
        loadDiscoveryData()
    }
    
    fun onMediaTypeChange(type: MediaType) {
        if (_mediaType.value != type) {
            _mediaType.value = type
            loadDiscoveryData()
        }
    }

    private fun loadDiscoveryData() {
        val currentType = _mediaType.value
        viewModelScope.launch {
            _uiState.update { DiscoverUiState.Loading }
            try {
                // Fetch all lists in parallel
                val trendingDeferred = async { discoverRepository.getTrending(currentType) }
                val popularDeferred = async { discoverRepository.getPopular(currentType) }
                val upcomingDeferred = async { discoverRepository.getUpcoming(currentType) }

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
