package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.DiscoverViewMode
import com.anisync.android.domain.ADULT_GENRES
import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.MediaTag
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchRepository
import com.anisync.android.domain.SearchType
import com.anisync.android.type.MediaType
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val searchRepository: SearchRepository,
    private val appSettings: com.anisync.android.data.AppSettings,
    private val searchLauncher: com.anisync.android.domain.DiscoverSearchLauncher
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
        observeViewMode()
        observeSearchLaunchRequests()
    }

    /**
     * External "open search with these filters" requests (ranking cards, genre/tag
     * chips on media details). Applies the preset filters over a blank query, fires
     * the search, and bumps [DiscoverUiState.Success.searchOverlayRequest] so the
     * screen expands the search overlay. Waits for the first Success state so a
     * request made before Discover ever composed isn't dropped.
     */
    private fun observeSearchLaunchRequests() {
        viewModelScope.launch {
            searchLauncher.pending.collect { filters ->
                if (filters == null) return@collect
                _uiState.first { it is DiscoverUiState.Success }
                searchLauncher.consume()
                val currentState = _uiState.value as? DiscoverUiState.Success ?: return@collect
                _uiState.update {
                    currentState.copy(
                        searchQuery = "",
                        searchFilters = filters,
                        activeCategory = ResultCategory.ALL,
                        searchOverlayRequest = currentState.searchOverlayRequest + 1
                    )
                }
                loadTaxonomyIfNeeded()
                searchTrigger.value = SearchTriggerState("", filters.hashCode())
            }
        }
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

    private fun observeViewMode() {
        viewModelScope.launch {
            appSettings.discoverSearchViewMode.collect { mode ->
                _uiState.update {
                    (it as? DiscoverUiState.Success)?.copy(viewMode = mode) ?: it
                }
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
            is DiscoverAction.LoadMoreResults -> loadMoreResults()
            is DiscoverAction.UpdateFilters -> updateFilters(action.filters)
            is DiscoverAction.ClearFilters -> clearFilters()
            is DiscoverAction.LoadTaxonomy -> loadTaxonomyIfNeeded()
            is DiscoverAction.OnViewModeChange -> appSettings.setDiscoverSearchViewMode(action.mode)
            is DiscoverAction.OnCategoryChange -> onCategoryChange(action.category)
        }
    }

    private fun onCategoryChange(category: ResultCategory) {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        _uiState.update { currentState.copy(activeCategory = category) }
    }

    private fun onMediaTypeChange(type: MediaType) {
        val currentState = _uiState.value as? DiscoverUiState.Success
        if (currentState?.mediaType != type) {
            appSettings.setDiscoverMediaType(type)
            if (currentState != null) {
                _uiState.update {
                    currentState.copy(
                        mediaType = type,
                        searchAnime = emptyList(),
                        searchManga = emptyList()
                    )
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

    /**
     * Appends the next page of the active category's results. Media buckets page
     * independently; the four entity buckets share one request/page counter, so
     * paging one entity category also grows the other three. A fresh search (new
     * trigger) resets the paging state wholesale, which implicitly invalidates any
     * in-flight append: the guard re-reads the state after the response and drops
     * the page if the query/filters changed underneath it.
     */
    private fun loadMoreResults() {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        val paging = currentState.searchPaging
        val category = currentState.activeCategory
        if (paging.isLoadingMore || !paging.hasNextFor(category)) return

        val wantAnime = category == ResultCategory.ANIME
        val wantManga = category == ResultCategory.MANGA
        val wantEntities = !wantAnime && !wantManga
        val nextPage = when {
            wantAnime -> paging.animePage + 1
            wantManga -> paging.mangaPage + 1
            else -> paging.entitiesPage + 1
        }
        val query = currentState.searchQuery
        val filters = currentState.searchFilters

        _uiState.update { currentState.copy(searchPaging = paging.copy(isLoadingMore = true)) }
        viewModelScope.launch {
            val result = searchRepository.searchEverything(
                query = query,
                filters = filters,
                page = nextPage,
                wantAnime = wantAnime,
                wantManga = wantManga,
                wantEntities = wantEntities
            )

            _uiState.update { existing ->
                val st = existing as? DiscoverUiState.Success ?: return@update existing
                // A newer search replaced the results while this page was in flight.
                if (st.searchQuery != query || st.searchFilters != filters) return@update existing

                when (result) {
                    is Result.Error -> st.copy(
                        searchPaging = st.searchPaging.copy(isLoadingMore = false)
                    )

                    is Result.Success -> {
                        val data = result.data
                        val grouped = st.groupedResults
                        // Appended entities go through the same type projection as the
                        // initial page so a type-pinned search can't resurrect buckets.
                        val appended = data.grouped.projectFor(filters.searchType)
                        st.copy(
                            searchAnime = (st.searchAnime + data.anime).distinctBy { it.mediaId },
                            searchManga = (st.searchManga + data.manga).distinctBy { it.mediaId },
                            groupedResults = grouped.copy(
                                characters = (grouped.characters + appended.characters).distinctBy { it.id },
                                staff = (grouped.staff + appended.staff).distinctBy { it.id },
                                users = (grouped.users + appended.users).distinctBy { it.id },
                                studios = (grouped.studios + appended.studios).distinctBy { it.id }
                            ),
                            searchPaging = st.searchPaging.copy(
                                isLoadingMore = false,
                                animePage = if (wantAnime) nextPage else st.searchPaging.animePage,
                                animeHasNext = if (wantAnime) data.animeHasNextPage else st.searchPaging.animeHasNext,
                                mangaPage = if (wantManga) nextPage else st.searchPaging.mangaPage,
                                mangaHasNext = if (wantManga) data.mangaHasNextPage else st.searchPaging.mangaHasNext,
                                entitiesPage = if (wantEntities) nextPage else st.searchPaging.entitiesPage,
                                charactersHasNext = if (wantEntities) data.charactersHasNextPage else st.searchPaging.charactersHasNext,
                                staffHasNext = if (wantEntities) data.staffHasNextPage else st.searchPaging.staffHasNext,
                                usersHasNext = if (wantEntities) data.usersHasNextPage else st.searchPaging.usersHasNext,
                                studiosHasNext = if (wantEntities) data.studiosHasNextPage else st.searchPaging.studiosHasNext
                            )
                        )
                    }
                }
            }
        }
    }

    private fun updateFilters(filters: SearchFilters) {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        // When the user changes search type, reset the result category so we don't
        // get stuck on a tab that the new query may never populate.
        val resetCategory = filters.searchType != currentState.searchFilters.searchType
        _uiState.update {
            currentState.copy(
                searchFilters = filters,
                activeCategory = if (resetCategory) ResultCategory.ALL else currentState.activeCategory
            )
        }
        searchTrigger.value = SearchTriggerState(currentState.searchQuery, filters.hashCode())
    }

    private fun clearFilters() {
        val currentState = _uiState.value as? DiscoverUiState.Success ?: return
        _uiState.update {
            currentState.copy(
                searchFilters = SearchFilters(),
                activeCategory = ResultCategory.ALL
            )
        }
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
                .debounce(350L)
                .distinctUntilChanged()
                .collectLatest { trigger ->
                    val currentState =
                        _uiState.value as? DiscoverUiState.Success ?: return@collectLatest
                    val query = currentState.searchQuery
                    val filters = currentState.searchFilters

                    if (!currentState.shouldSearch()) {
                        _uiState.update {
                            currentState.copy(
                                searchAnime = emptyList(),
                                searchManga = emptyList(),
                                groupedResults = GroupedSearchResults(),
                                isSearching = false
                            )
                        }
                        return@collectLatest
                    }

                    _uiState.update { currentState.copy(isSearching = true) }

                    // Which buckets to surface — same gating as the old 3-request
                    // fan-out, but it now rides a single SearchEverything request:
                    //   ANIME/MANGA explicit → that media bucket only
                    //   null (Auto)          → both media buckets + entities
                    //   non-media type       → entities only (projected below)
                    // Entities require an actual search string, so they're gated on
                    // a non-blank query.
                    val wantAnime = filters.searchType == SearchType.ANIME ||
                        (filters.searchType == null && !filters.isNonMediaType)
                    val wantManga = filters.searchType == SearchType.MANGA ||
                        (filters.searchType == null && !filters.isNonMediaType)
                    val wantEntities = query.isNotBlank() &&
                        filters.searchType != SearchType.ANIME &&
                        filters.searchType != SearchType.MANGA

                    val result = searchRepository.searchEverything(
                        query = query,
                        filters = filters,
                        wantAnime = wantAnime,
                        wantManga = wantManga,
                        wantEntities = wantEntities
                    )

                    val data = (result as? Result.Success)?.data
                    val animeEntries = data?.anime.orEmpty()
                    val mangaEntries = data?.manga.orEmpty()
                    val grouped = (data?.grouped ?: GroupedSearchResults())
                        .projectFor(filters.searchType)
                    val error = (result as? Result.Error)?.message

                    _uiState.update { existing ->
                        (existing as? DiscoverUiState.Success)?.let { st ->
                            st.copy(
                                searchAnime = animeEntries,
                                searchManga = mangaEntries,
                                groupedResults = grouped,
                                isSearching = false,
                                searchError = error,
                                activeCategory = st.activeCategory.clampedTo(
                                    availableCategories(animeEntries, mangaEntries, grouped)
                                ),
                                // Fresh search = page 1 of every bucket.
                                searchPaging = SearchPaging(
                                    animeHasNext = data?.animeHasNextPage ?: false,
                                    mangaHasNext = data?.mangaHasNextPage ?: false,
                                    charactersHasNext = data?.charactersHasNextPage ?: false,
                                    staffHasNext = data?.staffHasNextPage ?: false,
                                    usersHasNext = data?.usersHasNextPage ?: false,
                                    studiosHasNext = data?.studiosHasNextPage ?: false
                                )
                            )
                        } ?: existing
                    }
                }
        }
    }

    private fun loadDiscoveryData(isRefresh: Boolean = false) {
        val currentState = _uiState.value
        val mediaType = (currentState as? DiscoverUiState.Success)?.mediaType
            ?: appSettings.discoverMediaType.value

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
            val newlyAddedDeferred = async { discoverRepository.getNewlyAdded(mediaType) }
            val tbaDeferred = async { discoverRepository.getTBA(mediaType) }
            // Recent reviews ride alongside the media sections but never gate the
            // screen — a reviews failure just hides that one section.
            val recentReviewsDeferred =
                async { discoverRepository.getRecentReviews(mediaType = mediaType, page = 1) }

            val trendingResult = trendingDeferred.await()
            val popularResult = popularDeferred.await()
            val upcomingResult = upcomingDeferred.await()
            val newlyAddedResult = newlyAddedDeferred.await()
            val tbaResult = tbaDeferred.await()
            val recentReviews =
                (recentReviewsDeferred.await() as? Result.Success)?.data?.reviews ?: emptyList()

            if (trendingResult is Result.Success &&
                popularResult is Result.Success &&
                upcomingResult is Result.Success &&
                newlyAddedResult is Result.Success &&
                tbaResult is Result.Success
            ) {

                _uiState.update {
                    val baseState = it as? DiscoverUiState.Success ?: DiscoverUiState.Success(
                        viewMode = appSettings.discoverSearchViewMode.value
                    )
                    baseState.copy(
                        trending = trendingResult.data,
                        popular = popularResult.data,
                        upcoming = upcomingResult.data,
                        newlyAdded = newlyAddedResult.data,
                        tba = tbaResult.data,
                        recentReviews = recentReviews,
                        mediaType = mediaType,
                        isRefreshing = false
                    )
                }
            } else {
                val errorMessage = when {
                    trendingResult is Result.Error -> trendingResult.message
                    popularResult is Result.Error -> popularResult.message
                    upcomingResult is Result.Error -> upcomingResult.message
                    newlyAddedResult is Result.Error -> newlyAddedResult.message
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
        searchQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH || searchFilters.hasActiveFilters
}

/**
 * Minimum query length before a text-driven search fires. Single-character queries
 * match almost everything and just burn rate-limit budget; filters can still drive
 * a search with a shorter/blank query.
 */
private const val MIN_SEARCH_QUERY_LENGTH = 2

data class SearchTaxonomy(
    val genres: List<String> = emptyList(),
    val tags: List<MediaTag> = emptyList(),
    val loaded: Boolean = false
)

/**
 * Resolve the AniList media type to use for media-search queries. The type
 * chip overrides the screen-level Anime/Manga toggle when set to an explicit
 * media value; non-media types fall back to the toggle (media query is
 * skipped entirely in that case — see [SearchFilters.isNonMediaType]).
 */
private fun SearchFilters.effectiveMediaType(screenSelection: MediaType): MediaType =
    when (searchType) {
        SearchType.ANIME -> MediaType.ANIME
        SearchType.MANGA -> MediaType.MANGA
        else -> screenSelection
    }

/**
 * Strip categories the user didn't ask for. When the Type chip is non-media,
 * we want only the selected entity bucket to surface — the other buckets
 * would clutter the results header and the section list.
 */
private fun GroupedSearchResults.projectFor(searchType: SearchType?): GroupedSearchResults =
    when (searchType) {
        SearchType.CHARACTERS -> GroupedSearchResults(characters = characters)
        SearchType.STAFF -> GroupedSearchResults(staff = staff)
        SearchType.USERS -> GroupedSearchResults(users = users)
        SearchType.STUDIOS -> GroupedSearchResults(studios = studios)
        // Media-pinned types: searchMedia carries the result; the entity
        // buckets must stay empty so the category chips don't surface them.
        SearchType.ANIME, SearchType.MANGA -> GroupedSearchResults()
        null -> this
    }

private fun availableCategories(
    animeEntries: List<com.anisync.android.domain.LibraryEntry>,
    mangaEntries: List<com.anisync.android.domain.LibraryEntry>,
    grouped: GroupedSearchResults
): Set<ResultCategory> = buildSet {
    add(ResultCategory.ALL)
    if (animeEntries.isNotEmpty()) add(ResultCategory.ANIME)
    if (mangaEntries.isNotEmpty()) add(ResultCategory.MANGA)
    if (grouped.characters.isNotEmpty()) add(ResultCategory.CHARACTERS)
    if (grouped.staff.isNotEmpty()) add(ResultCategory.STAFF)
    if (grouped.users.isNotEmpty()) add(ResultCategory.USERS)
    if (grouped.studios.isNotEmpty()) add(ResultCategory.STUDIOS)
}

private fun ResultCategory.clampedTo(available: Set<ResultCategory>): ResultCategory =
    if (this in available) this else ResultCategory.ALL
