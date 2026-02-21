package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val searchRepository: SearchRepository,
    private val appSettings: com.anisync.android.data.AppSettings
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage

    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var hasLoadedInitially = false

    // State flow used internally to funnel Debounce logic easily for searching
    private val searchTrigger = MutableStateFlow(SearchTriggerState())

    private data class SearchTriggerState(
        val query: String = "",
        val filterHash: Int = 0 
    )

    init {
        observeSearchQuery()
    }

    fun onAction(action: DiscoverAction) {
        when (action) {
            is DiscoverAction.OnScreenVisible -> onScreenVisible()
            is DiscoverAction.OnMediaTypeChange -> onMediaTypeChange(action.type)
            is DiscoverAction.Refresh -> refresh()
            is DiscoverAction.OnSearchQueryChange -> onSearchQueryChange(action.query)
            is DiscoverAction.OnSearchActiveChange -> onSearchActiveChange(action.active)
            is DiscoverAction.OnSearch -> onSearch(action.query)
            is DiscoverAction.UpdateFilters -> updateFilters(action.filters)
            is DiscoverAction.ClearFilters -> clearFilters()
        }
    }

    private fun onScreenVisible() {
        if (!hasLoadedInitially) {
            hasLoadedInitially = true
            loadDiscoveryData()
        }
    }

    private fun onMediaTypeChange(type: com.anisync.android.type.MediaType) {
        val currentState = _uiState.value as? DiscoverUiState.Success
        if (currentState?.mediaType != type) {
            
            // Maintain search strings and toggles when switching types if we are in Success
            if (currentState != null) {
                _uiState.update { 
                    currentState.copy(mediaType = type, searchResults = emptyList()) 
                }
            } else {
                _uiState.update { DiscoverUiState.Success(mediaType = type) }
            }
            
            loadDiscoveryData()
            
            // Re-trigger search with new type if query exists
            if (currentState?.searchQuery?.isNotEmpty() == true) {
                onSearch(currentState.searchQuery)
            }
        }
    }

    private fun refresh() {
        loadDiscoveryData(isRefresh = true)
    }

    private fun onSearchQueryChange(query: String) {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        
        _uiState.update { 
            currentState.copy(
                searchQuery = query,
                searchResults = if (query.isEmpty()) emptyList() else currentState.searchResults
            )
        }
        
        searchTrigger.value = SearchTriggerState(query, currentState.searchFilters.hashCode())
    }

    private fun onSearchActiveChange(active: Boolean) {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        _uiState.update { currentState.copy(isSearchActive = active) }
    }

    private fun onSearch(query: String) {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        _uiState.update { currentState.copy(searchQuery = query) }
        searchTrigger.value = SearchTriggerState(query, currentState.searchFilters.hashCode())
    }

    private fun updateFilters(filters: com.anisync.android.domain.SearchFilters) {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        _uiState.update { currentState.copy(searchFilters = filters) }
        searchTrigger.value = SearchTriggerState(currentState.searchQuery, filters.hashCode())
    }

    private fun clearFilters() {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        _uiState.update { currentState.copy(searchFilters = com.anisync.android.domain.SearchFilters()) }
        searchTrigger.value = SearchTriggerState(currentState.searchQuery, 0)
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchTrigger
                .debounce(300L)
                .distinctUntilChanged()
                .collect { trigger ->
                    val currentState = _uiState.value as? DiscoverUiState.Success ?: return@collect
                    val query = currentState.searchQuery
                    
                    if (query.isBlank()) {
                        _uiState.update { 
                            currentState.copy(searchResults = emptyList(), isSearching = false) 
                        }
                        return@collect
                    }

                    _uiState.update { currentState.copy(isSearching = true) }
                    
                    when (val result = searchRepository.searchMedia(query, currentState.mediaType, currentState.searchFilters)) {
                        is Result.Success -> {
                            _uiState.update { 
                                (it as? DiscoverUiState.Success)?.copy(
                                    searchResults = result.data,
                                    isSearching = false
                                ) ?: it
                            }
                        }
                        is Result.Error -> {
                            _uiState.update { 
                                (it as? DiscoverUiState.Success)?.copy(
                                    searchResults = emptyList(),
                                    isSearching = false
                                ) ?: it
                            }
                        }
                    }
                }
        }
    }

    private fun loadDiscoveryData(isRefresh: Boolean = false) {
        val currentState = _uiState.value
        val mediaType = (currentState as? DiscoverUiState.Success)?.mediaType 
            ?: com.anisync.android.type.MediaType.ANIME

        viewModelScope.launch {
            val startTime = if (isRefresh) System.currentTimeMillis() else 0L
            
            if (isRefresh && currentState is DiscoverUiState.Success) {
                _uiState.update { currentState.copy(isRefreshing = true) }
            } else if (currentState !is DiscoverUiState.Success) {
                _uiState.update { DiscoverUiState.Loading }
            }

            // Parallel fetching
            val trendingDeferred = async { discoverRepository.getTrending(mediaType) }
            val popularDeferred = async { discoverRepository.getPopular(mediaType) }
            val upcomingDeferred = async { discoverRepository.getUpcoming(mediaType) }
            val tbaDeferred = async { discoverRepository.getTBA(mediaType) }

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
                    val baseState = it as? DiscoverUiState.Success ?: DiscoverUiState.Success()
                    baseState.copy(
                        trending = trendingResult.data,
                        popular = popularResult.data,
                        upcoming = upcomingResult.data,
                        tba = tbaResult.data,
                        mediaType = mediaType,
                        isRefreshing = false
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
                
                // One final safety pass to ensure refreshing ends
                _uiState.update { 
                    (it as? DiscoverUiState.Success)?.copy(isRefreshing = false) ?: it 
                }
            }
        }
    }
}
