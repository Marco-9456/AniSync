package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchRepository
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Success(
        val trending: List<LibraryEntry>,
        val popular: List<LibraryEntry>,
        val upcoming: List<LibraryEntry>,
        val tba: List<LibraryEntry>
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

    // --- Search State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LibraryEntry>>(emptyList())
    val searchResults: StateFlow<List<LibraryEntry>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // --- Search Filters State ---
    private val _searchFilters = MutableStateFlow(SearchFilters())
    val searchFilters: StateFlow<SearchFilters> = _searchFilters.asStateFlow()

    init {
        loadDiscoveryData()
        observeSearchQuery()
    }

    fun onMediaTypeChange(type: MediaType) {
        if (_mediaType.value != type) {
            _mediaType.value = type
            loadDiscoveryData()
            // If searching, re-trigger search with new type
            if (_searchQuery.value.isNotEmpty()) {
                onSearch(_searchQuery.value)
            }
        }
    }

    fun refresh() {
        loadDiscoveryData(isRefresh = true)
    }

    // --- Search Logic ---

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
        }
    }

    fun onSearchActiveChange(active: Boolean) {
        _isSearchActive.value = active
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
        // Search flow below will pick this up
    }

    fun updateFilters(filters: SearchFilters) {
        _searchFilters.value = filters
    }

    fun clearFilters() {
        _searchFilters.value = SearchFilters()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            combine(
                _searchQuery,
                _searchFilters
            ) { query, filters -> query to filters }
                .debounce(300L)
                .distinctUntilChanged()
                .collect { (query, filters) ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                        return@collect
                    }

                    _isSearching.value = true
                    
                    when (val result = searchRepository.searchMedia(query, _mediaType.value, filters)) {
                        is Result.Success -> _searchResults.value = result.data
                        is Result.Error -> _searchResults.value = emptyList()
                    }
                    
                    _isSearching.value = false
                }
        }
    }

    private fun loadDiscoveryData(isRefresh: Boolean = false) {
        val currentType = _mediaType.value
        viewModelScope.launch {
            val startTime = if (isRefresh) System.currentTimeMillis() else 0L
            
            if (isRefresh) {
                _isRefreshing.value = true
            } else if (_uiState.value !is DiscoverUiState.Success) {
                _uiState.update { DiscoverUiState.Loading }
            }

            // Parallel fetching
            val trendingDeferred = async { discoverRepository.getTrending(currentType) }
            val popularDeferred = async { discoverRepository.getPopular(currentType) }
            val upcomingDeferred = async { discoverRepository.getUpcoming(currentType) }
            val tbaDeferred = async { discoverRepository.getTBA(currentType) }

            val trendingResult = trendingDeferred.await()
            val popularResult = popularDeferred.await()
            val upcomingResult = upcomingDeferred.await()
            val tbaResult = tbaDeferred.await()

            // Check if all succeeded
            if (trendingResult is Result.Success && 
                popularResult is Result.Success && 
                upcomingResult is Result.Success &&
                tbaResult is Result.Success) {
                _uiState.update {
                    DiscoverUiState.Success(
                        trending = trendingResult.data,
                        popular = popularResult.data,
                        upcoming = upcomingResult.data,
                        tba = tbaResult.data
                    )
                }
            } else {
                // Get first error message
                val errorMessage = when {
                    trendingResult is Result.Error -> trendingResult.message
                    popularResult is Result.Error -> popularResult.message
                    upcomingResult is Result.Error -> upcomingResult.message
                    tbaResult is Result.Error -> tbaResult.message
                    else -> "Unknown error"
                }
                _uiState.update { DiscoverUiState.Error(errorMessage) }
            }
            
            // Ensure minimum display duration for pull-to-refresh indicator (800ms)
            if (isRefresh) {
                val elapsed = System.currentTimeMillis() - startTime
                val minDisplayDuration = 800L
                if (elapsed < minDisplayDuration) {
                    delay(minDisplayDuration - elapsed)
                }
            }
            
            _isRefreshing.value = false
        }
    }
}