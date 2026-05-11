package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ADULT_GENRES
import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.MediaTag
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    val showAdultContent = appSettings.showAdultContent

    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _taxonomy = MutableStateFlow(SearchTaxonomy())
    val taxonomy: StateFlow<SearchTaxonomy> = _taxonomy.asStateFlow()

    private val searchTrigger = MutableStateFlow(SearchTriggerState())

    private data class SearchTriggerState(
        val query: String = "",
        val filterHash: Int = 0
    )

    init {
        loadDiscoveryData()
        observeSearchQuery()
        observeAdultContent()
    }

    /**
     * Strip adult-only filter values when the user turns Show adult content off,
     * so a stale Hentai genre or NSFW tag doesn't keep silently filtering results.
     */
    private fun observeAdultContent() {
        viewModelScope.launch {
            showAdultContent.collect { enabled ->
                if (enabled) return@collect
                val state = _uiState.value as? DiscoverUiState.Success ?: return@collect
                val nsfwTagNames = _taxonomy.value.tags
                    .asSequence()
                    .filter { it.isAdult }
                    .map { it.name }
                    .toSet()
                val current = state.searchFilters
                val cleaned = current.copy(
                    genresIncluded = current.genresIncluded - ADULT_GENRES,
                    genresExcluded = current.genresExcluded - ADULT_GENRES,
                    tagsIncluded = current.tagsIncluded - nsfwTagNames,
                    tagsExcluded = current.tagsExcluded - nsfwTagNames,
                    adultMode = if (current.adultMode == AdultMode.ONLY) AdultMode.ANY
                    else current.adultMode
                )
                if (cleaned != current) updateFilters(cleaned)
            }
        }
    }

    fun onAction(action: DiscoverAction) {
        when (action) {
            is DiscoverAction.OnMediaTypeChange -> onMediaTypeChange(action.type)
            is DiscoverAction.Refresh -> refresh()
            is DiscoverAction.OnSearchQueryChange -> onSearchQueryChange(action.query)
            is DiscoverAction.OnSearchActiveChange -> onSearchActiveChange(action.active)
            is DiscoverAction.OnSearch -> onSearch(action.query)
            is DiscoverAction.UpdateFilters -> updateFilters(action.filters)
            is DiscoverAction.ClearFilters -> clearFilters()
            is DiscoverAction.LoadTaxonomy -> loadTaxonomyIfNeeded()
        }
    }

    private fun onMediaTypeChange(type: com.anisync.android.type.MediaType) {
        val currentState = _uiState.value as? DiscoverUiState.Success
        if (currentState?.mediaType != type) {
            if (currentState != null) {
                _uiState.update {
                    currentState.copy(mediaType = type, searchResults = emptyList())
                }
            } else {
                _uiState.update { DiscoverUiState.Success(mediaType = type) }
            }

            loadDiscoveryData()

            if (currentState?.shouldSearch() == true) {
                searchTrigger.value = SearchTriggerState(
                    currentState.searchQuery,
                    currentState.searchFilters.hashCode()
                )
            }
        }
    }

    private fun refresh() {
        loadDiscoveryData(isRefresh = true)
    }

    private fun onSearchQueryChange(query: String) {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        _uiState.update { currentState.copy(searchQuery = query) }
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

    private fun loadTaxonomyIfNeeded() {
        if (_taxonomy.value.loaded) return
        viewModelScope.launch {
            val genresDeferred = async { searchRepository.getGenres() }
            val tagsDeferred = async { searchRepository.getTags() }
            val genres = (genresDeferred.await() as? Result.Success)?.data.orEmpty()
            val tags = (tagsDeferred.await() as? Result.Success)?.data.orEmpty()
            _taxonomy.value = SearchTaxonomy(genres = genres, tags = tags, loaded = true)
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchTrigger
                .debounce(300L)
                .distinctUntilChanged()
                .collectLatest { trigger ->
                    val currentState =
                        _uiState.value as? DiscoverUiState.Success ?: return@collectLatest
                    val query = currentState.searchQuery

                    if (!currentState.shouldSearch()) {
                        _uiState.update {
                            currentState.copy(
                                searchResults = emptyList(),
                                groupedResults = com.anisync.android.domain.GroupedSearchResults(),
                                isSearching = false
                            )
                        }
                        return@collectLatest
                    }

                    _uiState.update { currentState.copy(isSearching = true) }

                    val mediaDeferred = async {
                        searchRepository.searchMedia(
                            query = query,
                            type = currentState.mediaType,
                            filters = currentState.searchFilters
                        )
                    }
                    // SearchAll only kicks in once the user has typed something —
                    // it's a multi-entity (character/staff/user/studio) lookup
                    // that needs an actual search string.
                    val allDeferred = if (query.isNotBlank()) {
                        async { searchRepository.searchAll(query) }
                    } else null

                    val mediaResult = mediaDeferred.await()
                    val allResult = allDeferred?.await()

                    when (mediaResult) {
                        is Result.Success -> {
                            val grouped = when (allResult) {
                                is Result.Success -> allResult.data
                                is Result.Error -> com.anisync.android.domain.GroupedSearchResults()
                                null -> com.anisync.android.domain.GroupedSearchResults()
                            }
                            _uiState.update {
                                (it as? DiscoverUiState.Success)?.copy(
                                    searchResults = mediaResult.data.entries,
                                    groupedResults = grouped,
                                    isSearching = false,
                                    searchError = null
                                ) ?: it
                            }
                        }

                        is Result.Error -> {
                            _uiState.update {
                                (it as? DiscoverUiState.Success)?.copy(
                                    searchResults = emptyList(),
                                    groupedResults = com.anisync.android.domain.GroupedSearchResults(),
                                    isSearching = false,
                                    searchError = mediaResult.message
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

            val trendingDeferred = async { discoverRepository.getTrending(mediaType) }
            val popularDeferred = async { discoverRepository.getPopular(mediaType) }
            val upcomingDeferred = async { discoverRepository.getUpcoming(mediaType) }
            val tbaDeferred = async { discoverRepository.getTBA(mediaType) }

            val trendingResult = trendingDeferred.await()
            val popularResult = popularDeferred.await()
            val upcomingResult = upcomingDeferred.await()
            val tbaResult = tbaDeferred.await()

            if (trendingResult is Result.Success &&
                popularResult is Result.Success &&
                upcomingResult is Result.Success &&
                tbaResult is Result.Success
            ) {

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
                val errorMessage = when {
                    trendingResult is Result.Error -> trendingResult.message
                    popularResult is Result.Error -> popularResult.message
                    upcomingResult is Result.Error -> upcomingResult.message
                    tbaResult is Result.Error -> tbaResult.message
                    else -> "Unknown error"
                }
                _uiState.update { DiscoverUiState.Error(errorMessage) }
            }

            if (isRefresh) {
                val elapsed = System.currentTimeMillis() - startTime
                val minDisplayDuration = 800L
                if (elapsed < minDisplayDuration) {
                    delay(minDisplayDuration - elapsed)
                }

                _uiState.update {
                    (it as? DiscoverUiState.Success)?.copy(isRefreshing = false) ?: it
                }
            }
        }
    }

    private fun DiscoverUiState.Success.shouldSearch(): Boolean =
        searchQuery.isNotBlank() || searchFilters.hasActiveFilters
}

data class SearchTaxonomy(
    val genres: List<String> = emptyList(),
    val tags: List<MediaTag> = emptyList(),
    val loaded: Boolean = false
)
