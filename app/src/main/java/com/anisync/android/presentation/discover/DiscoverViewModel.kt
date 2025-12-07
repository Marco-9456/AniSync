package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchRepository
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
    private val discoverRepository: DiscoverRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _mediaType = MutableStateFlow(MediaType.ANIME)
    val mediaType: StateFlow<MediaType> = _mediaType.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LibraryEntry>>(emptyList())
    val searchResults: StateFlow<List<LibraryEntry>> = _searchResults.asStateFlow()

    init {
        loadDiscoveryData()
        observeSearchQuery()
    }

    fun onMediaTypeChange(type: MediaType) {
        if (_mediaType.value != type) {
            _mediaType.value = type
            loadDiscoveryData()
            // Also re-trigger search if active? For now, standard discovery reload.
        }
    }

    fun refresh() {
        loadDiscoveryData(isRefresh = true)
    }

    // Search Actions
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSearchActiveChange(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            // Optional: Clear query or keep it? Material guidelines usually keep it until cleared manually.
            // But if closing means "cancel", maybe not. Let's keep it simple.
        }
    }

    fun onSearch(query: String) {
        // Trigger search explicitly if needed, but we use reactive flow below
        _searchQuery.value = query
        // Usually we might want to close the active state or show full results page,
        // but for this "embedded" search, we might just keep showing results in the expanded sheet.
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500L)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    try {
                        val results = searchRepository.searchMedia(query, _mediaType.value)
                        _searchResults.value = results
                    } catch (e: Exception) {
                        // Handle error or just show empty
                        _searchResults.value = emptyList()
                    }
                }
        }
    }

    private fun loadDiscoveryData(isRefresh: Boolean = false) {
        val currentType = _mediaType.value
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _uiState.update { DiscoverUiState.Loading }
            }

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
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
