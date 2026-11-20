package com.anisync.android.presentation.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.SearchSurface
import com.anisync.android.domain.ADULT_GENRES
import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaTag
import com.anisync.android.domain.OriginCountry
import com.anisync.android.domain.Result
import com.anisync.android.domain.ResultCategory
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchRepository
import com.anisync.android.domain.SearchSort
import com.anisync.android.domain.SearchSortColumn
import com.anisync.android.domain.seasonOfMonth
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.discover.components.BrowseChip
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DiscoverSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchRepository: SearchRepository,
    private val detailsRepository: DetailsRepository,
    private val appSettings: AppSettings,
    private val toastManager: ToastManager
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage
    val showAdultContent = appSettings.showAdultContent

    private val preset = savedStateHandle.presetSearch()

    private val _uiState = MutableStateFlow(DiscoverSearchUiState(listsWhenEmpty = preset?.sort != null))
    val uiState: StateFlow<DiscoverSearchUiState> = _uiState.asStateFlow()

    private val _taxonomy = MutableStateFlow(SearchTaxonomy())
    val taxonomy: StateFlow<SearchTaxonomy> = _taxonomy.asStateFlow()

    private val searchTrigger = MutableStateFlow(SearchTriggerState())

    private data class SearchTriggerState(
        val query: String = "",
        val filterHash: Int = 0,
        /** Tells the Studios chip apart from a cleared search, which carries the same query and filters. */
        val browsingStudios: Boolean = false,
        /** The search key or a recent search, which skip the pause that waits for typing to stop. */
        val submitted: Boolean = false
    ) {
        fun isSameSearch(other: SearchTriggerState): Boolean =
            copy(submitted = false) == other.copy(submitted = false)
    }

    /** The search the results on screen came from. */
    private var lastRunTrigger = SearchTriggerState()

    init {
        observeSearchQuery()
        observeAdultContent()
        observeViewMode()
        observeRecentSearches()
        preset?.let { openPresetSearch(it.toFilters(), it.category) }
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            appSettings.recentSearches(SearchSurface.DISCOVER).collect { recent ->
                _uiState.update { it.copy(recentSearches = recent) }
            }
        }
    }

    /**
     * Applies a preset over a blank query, fires the search, and bumps
     * [DiscoverSearchUiState.presetRequest] so the screen opens the search on it.
     */
    private fun openPresetSearch(filters: SearchFilters, category: ResultCategory) {
        _uiState.update {
            it.copy(
                searchQuery = "",
                submittedQuery = "",
                searchFilters = filters,
                activeCategory = category,
                presetRequest = it.presetRequest + 1
            )
        }
        loadTaxonomyIfNeeded()
        searchTrigger.value = SearchTriggerState(
            query = "",
            filterHash = filters.hashCode(),
            browsingStudios = category == ResultCategory.STUDIOS
        )
    }

    /**
     * Strip adult-only filter values when the user turns Show adult content off,
     * so a stale Hentai genre or NSFW tag doesn't keep silently filtering results.
     */
    private fun observeAdultContent() {
        viewModelScope.launch {
            showAdultContent.collect { enabled ->
                if (enabled) return@collect
                val state = _uiState.value
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
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
    }

    fun onAction(action: DiscoverSearchAction) {
        when (action) {
            is DiscoverSearchAction.OnSearchQueryChange -> onSearchQueryChange(action.query)
            is DiscoverSearchAction.OnSearch -> onSearch(action.query)
            is DiscoverSearchAction.LoadMoreResults -> loadMoreResults()
            is DiscoverSearchAction.UpdateFilters -> updateFilters(action.filters)
            is DiscoverSearchAction.ClearFilters -> clearFilters()
            is DiscoverSearchAction.LoadTaxonomy -> loadTaxonomyIfNeeded()
            is DiscoverSearchAction.OnViewModeChange -> appSettings.setDiscoverSearchViewMode(action.mode)
            is DiscoverSearchAction.OnCategoryChange -> onCategoryChange(action.category)
            is DiscoverSearchAction.RemoveRecentSearch ->
                appSettings.removeRecentSearch(SearchSurface.DISCOVER, action.query)
            is DiscoverSearchAction.ClearRecentSearches -> appSettings.clearRecentSearches(SearchSurface.DISCOVER)
            is DiscoverSearchAction.OnBrowseChip -> onBrowseChip(action.chip, action.mediaType)
            is DiscoverSearchAction.AddToPlanning -> addToPlanning(action.mediaId)
        }
    }

    private fun onCategoryChange(category: ResultCategory) {
        val currentState = _uiState.value
        _uiState.update { currentState.copy(activeCategory = category) }
    }

    /**
     * Typing waits for the search key. Emptying the field searches for nothing at once, which
     * clears the results.
     */
    private fun onSearchQueryChange(query: String) {
        if (query.isEmpty()) onSearch(query) else _uiState.update { it.copy(searchQuery = query) }
    }

    private fun onSearch(query: String) {
        val trigger = SearchTriggerState(query, _uiState.value.searchFilters.hashCode(), submitted = true)
        _uiState.update { state ->
            val submitted = state.copy(searchQuery = query, submittedQuery = query)
            // Spinning from the tap, since the request only starts a moment later and until then an
            // empty result list reads as No results.
            val startsSearch = submitted.shouldSearch() && !trigger.isSameSearch(lastRunTrigger)
            submitted.copy(isSearching = state.isSearching || startsSearch)
        }
        searchTrigger.value = trigger
        appSettings.addRecentSearch(SearchSurface.DISCOVER, query)
    }

    /**
     * Appends the next page of the active category's results. Media buckets page
     * independently; the four entity buckets share one request/page counter, so
     * paging one entity category also grows the other three. Studios listed before
     * a name is typed page alone. A fresh search (new
     * trigger) resets the paging state wholesale, which implicitly invalidates any
     * in-flight append: the guard re-reads the state after the response and drops
     * the page if the query/filters changed underneath it.
     */
    private fun loadMoreResults() {
        val currentState = _uiState.value
        val paging = currentState.searchPaging
        val category = currentState.activeCategory
        if (paging.isLoadingMore || !paging.hasNextFor(category)) return

        val wantAnime = category == ResultCategory.ANIME
        val wantManga = category == ResultCategory.MANGA
        val browsingStudios = currentState.isBrowsingStudios()
        val wantEntities = !wantAnime && !wantManga && !browsingStudios
        val wantStudios = wantEntities || browsingStudios
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
                wantEntities = wantEntities,
                wantStudios = wantStudios
            )

            // Outside the update block, which re-runs under contention. The results already on
            // screen are kept either way, so the toast is the only thing saying the next page
            // failed rather than simply not existing. It repeats the block's staleness test: a
            // newer search has already replaced these results, and Retry would run the new query.
            val latest = _uiState.value
            if (result is Result.Error &&
                latest.searchQuery == query &&
                latest.searchFilters == filters
            ) {
                toastManager.showResultError(result, toastManager.retryAction { loadMoreResults() })
            }

            _uiState.update { st ->
                // A newer search replaced the results while this page was in flight.
                if (st.searchQuery != query || st.searchFilters != filters) return@update st

                when (result) {
                    is Result.Error -> st.copy(
                        searchPaging = st.searchPaging.copy(isLoadingMore = false)
                    )

                    is Result.Success -> {
                        val data = result.data
                        val grouped = st.groupedResults
                        val appended = data.grouped
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
                                entitiesPage = if (wantStudios) nextPage else st.searchPaging.entitiesPage,
                                charactersHasNext = if (wantEntities) data.charactersHasNextPage else st.searchPaging.charactersHasNext,
                                staffHasNext = if (wantEntities) data.staffHasNextPage else st.searchPaging.staffHasNext,
                                usersHasNext = if (wantEntities) data.usersHasNextPage else st.searchPaging.usersHasNext,
                                studiosHasNext = if (wantStudios) data.studiosHasNextPage else st.searchPaging.studiosHasNext
                            )
                        )
                    }
                }
            }
        }
    }

    /** A filter narrows what is typed, so changing one runs the search for it as well. */
    private fun updateFilters(filters: SearchFilters) {
        val currentState = _uiState.value
        _uiState.update { it.copy(searchFilters = filters, submittedQuery = it.searchQuery) }
        searchTrigger.value = SearchTriggerState(currentState.searchQuery, filters.hashCode())
    }

    /** The order is a choice about how to read the results rather than a filter, so it stays. */
    private fun clearFilters() {
        updateFilters(SearchFilters(sort = _uiState.value.searchFilters.sort))
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
                .debounce { if (it.submitted) 0L else 350L }
                .distinctUntilChanged { old, new -> old.isSameSearch(new) }
                .collectLatest { trigger ->
                    lastRunTrigger = trigger
                    val currentState = _uiState.value
                    val query = currentState.searchQuery
                    val filters = currentState.searchFilters

                    if (!currentState.shouldSearch()) {
                        _uiState.update {
                            currentState.copy(
                                searchAnime = emptyList(),
                                searchManga = emptyList(),
                                groupedResults = GroupedSearchResults(),
                                isSearching = false,
                                searchError = null
                            )
                        }
                        return@collectLatest
                    }

                    _uiState.update { currentState.copy(isSearching = true) }

                    // Every category rides the one SearchEverything request, so the rail can
                    // show what each holds. Characters, staff, users and studios are found by
                    // name alone, so they wait for a query. The Studios chip is the exception
                    // and lists the most favourited studios until a name narrows them.
                    val browsingStudios = currentState.isBrowsingStudios()
                    val result = searchRepository.searchEverything(
                        query = query,
                        filters = filters,
                        wantAnime = !browsingStudios,
                        wantManga = !browsingStudios,
                        wantEntities = query.isNotBlank(),
                        wantStudios = query.isNotBlank() || browsingStudios
                    )

                    val data = (result as? Result.Success)?.data
                    val animeEntries = data?.anime.orEmpty()
                    val mangaEntries = data?.manga.orEmpty()
                    val grouped = data?.grouped ?: GroupedSearchResults()
                    val error = (result as? Result.Error)?.message

                    _uiState.update { st ->
                        st.copy(
                            searchAnime = animeEntries,
                            searchManga = mangaEntries,
                            groupedResults = grouped,
                            searchTotals = SearchTotals(
                                anime = data?.animeTotal ?: 0,
                                manga = data?.mangaTotal ?: 0
                            ),
                            isSearching = false,
                            searchError = error,
                            activeCategory = st.activeCategory.clampedTo(
                                populatedCategories(animeEntries, mangaEntries, grouped)
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
                    }
                }
        }
    }

    /**
     * A browse chip is a saved search. Rather than inventing a browse screen per axis, each chip
     * applies the filters that describe it and opens the search overlay on the results.
     *
     * Schedule is the exception and never reaches here: the screen sends it to the airing calendar.
     */
    private fun onBrowseChip(chip: BrowseChip, mediaType: MediaType) {
        val tabCategory =
            if (mediaType == MediaType.ANIME) ResultCategory.ANIME else ResultCategory.MANGA
        when (chip) {
            BrowseChip.SEASONAL -> {
                val now = Calendar.getInstance()
                openPresetSearch(
                    SearchFilters(
                        season = seasonOfMonth(now.get(Calendar.MONTH)),
                        yearRange = IntRangeFilter(
                            min = now.get(Calendar.YEAR),
                            max = now.get(Calendar.YEAR)
                        )
                    ),
                    ResultCategory.ANIME
                )
            }
            BrowseChip.TOP_100 -> openPresetSearch(
                SearchFilters(sort = SearchSort(SearchSortColumn.SCORE)),
                tabCategory
            )
            BrowseChip.STUDIOS -> openPresetSearch(SearchFilters(), ResultCategory.STUDIOS)
            BrowseChip.MANHWA -> openPresetSearch(
                SearchFilters(country = OriginCountry.SOUTH_KOREA),
                ResultCategory.MANGA
            )
            BrowseChip.MANHUA -> openPresetSearch(
                SearchFilters(country = OriginCountry.CHINA),
                ResultCategory.MANGA
            )
            BrowseChip.LIGHT_NOVELS -> openPresetSearch(
                SearchFilters(formats = setOf(MediaFormat.NOVEL)),
                ResultCategory.MANGA
            )
            // Genres has no single preset to apply, so it opens the overlay where the genre
            // picker already lives.
            BrowseChip.GENRES -> openPresetSearch(SearchFilters(), tabCategory)
            BrowseChip.SCHEDULE -> Unit
        }
    }

    /**
     * No result handling beyond the request itself: the mutation writes through to Room, and the
     * row's own list indicator is fed by [com.anisync.android.presentation.util.LocalLibraryStatuses],
     * so a success shows up as the status chip replacing the button.
     */
    private fun addToPlanning(mediaId: Int) {
        viewModelScope.launch {
            detailsRepository.updateMediaListEntry(mediaId, LibraryStatus.PLANNING)
        }
    }

    private fun DiscoverSearchUiState.shouldSearch(): Boolean =
        searchQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH ||
            searchFilters.hasActiveFilters ||
            isBrowsingStudios() ||
            (listsWhenEmpty && searchQuery.isBlank())

    private fun DiscoverSearchUiState.isBrowsingStudios(): Boolean =
        activeCategory == ResultCategory.STUDIOS && searchQuery.isBlank()
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

/** The categories this search actually returned something for. */
private fun populatedCategories(
    animeEntries: List<LibraryEntry>,
    mangaEntries: List<LibraryEntry>,
    grouped: GroupedSearchResults
): Set<ResultCategory> = buildSet {
    if (animeEntries.isNotEmpty()) add(ResultCategory.ANIME)
    if (mangaEntries.isNotEmpty()) add(ResultCategory.MANGA)
    if (grouped.characters.isNotEmpty()) add(ResultCategory.CHARACTERS)
    if (grouped.staff.isNotEmpty()) add(ResultCategory.STAFF)
    if (grouped.users.isNotEmpty()) add(ResultCategory.USERS)
    if (grouped.studios.isNotEmpty()) add(ResultCategory.STUDIOS)
}

private fun ResultCategory.clampedTo(populated: Set<ResultCategory>): ResultCategory =
    if (this in populated) this else ResultCategory.ALL
