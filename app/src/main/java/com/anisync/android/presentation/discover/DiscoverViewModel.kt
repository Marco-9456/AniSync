package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.DiscoverViewMode
import com.anisync.android.domain.ADULT_GENRES
import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.AiringEpisode
import com.anisync.android.domain.CalendarRepository
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.DiscoverSection
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.MediaTag
import com.anisync.android.domain.OriginCountry
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchRepository
import com.anisync.android.domain.SearchType
import com.anisync.android.domain.SortOption
import com.anisync.android.presentation.discover.components.BrowseChip
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val calendarRepository: CalendarRepository,
    private val detailsRepository: DetailsRepository,
    private val searchRepository: SearchRepository,
    private val appSettings: com.anisync.android.data.AppSettings,
    private val searchLauncher: com.anisync.android.domain.DiscoverSearchLauncher
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage
    val showAdultContent = appSettings.showAdultContent

    private val _uiState = MutableStateFlow(
        DiscoverUiState(mediaType = appSettings.discoverMediaType.value)
    )
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _taxonomy = MutableStateFlow(SearchTaxonomy())
    val taxonomy: StateFlow<SearchTaxonomy> = _taxonomy.asStateFlow()

    private val searchTrigger = MutableStateFlow(SearchTriggerState())

    private data class SearchTriggerState(
        val query: String = "",
        val filterHash: Int = 0
    )

    init {
        observeSectionOrder()
        loadDiscoveryData()
        observeSearchQuery()
        observeAdultContent()
        observeViewMode()
        observeSearchLaunchRequests()
    }

    /**
     * External "open search with these filters" requests (ranking cards, genre/tag
     * chips on media details). Applies the preset filters over a blank query, fires
     * the search, and bumps [DiscoverUiState.searchOverlayRequest] so the
     * screen expands the search overlay. Waits for the first Success state so a
     * request made before Discover ever composed isn't dropped.
     */
    private fun observeSearchLaunchRequests() {
        viewModelScope.launch {
            searchLauncher.pending.collect { filters ->
                if (filters == null) return@collect
                searchLauncher.consume()
                val currentState = _uiState.value
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
            is DiscoverAction.ReorderSections -> reorderSections(action.order)
            is DiscoverAction.SetSectionVisible -> setSectionVisible(action.section, action.visible)
            is DiscoverAction.ResetSectionOrder -> resetSectionOrder()
            is DiscoverAction.OnBrowseChip -> onBrowseChip(action.chip)
            is DiscoverAction.AddToPlanning -> addToPlanning(action.mediaId)
            is DiscoverAction.RetrySection -> retrySection(action.section)
        }
    }

    private fun onCategoryChange(category: ResultCategory) {
        val currentState = _uiState.value
        _uiState.update { currentState.copy(activeCategory = category) }
    }

    private fun onMediaTypeChange(type: MediaType) {
        val currentState = _uiState.value
        if (currentState.mediaType == type) return
        appSettings.setDiscoverMediaType(type)
        // The tabs do not share a rail set, so the order and the feeds both start over.
        _uiState.update {
            currentState.copy(
                mediaType = type,
                feeds = DiscoverFeeds(),
                sectionOrder = visibleSections(type),
                hiddenSections = hiddenSections(type),
                searchAnime = emptyList(),
                searchManga = emptyList()
            )
        }

        loadDiscoveryData()

        if (currentState.shouldSearch()) {
            searchTrigger.value = SearchTriggerState(
                currentState.searchQuery,
                currentState.searchFilters.hashCode()
            )
        }
    }

    private fun refresh() {
        loadDiscoveryData(isRefresh = true)
    }

    private fun onSearchQueryChange(query: String) {
        val currentState = _uiState.value
        _uiState.update { currentState.copy(searchQuery = query) }
        searchTrigger.value = SearchTriggerState(query, currentState.searchFilters.hashCode())
    }

    private fun onSearchActiveChange(active: Boolean) {
        val currentState = _uiState.value
        _uiState.update { currentState.copy(isSearchActive = active) }
    }

    private fun onSearch(query: String) {
        val currentState = _uiState.value
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
        val currentState = _uiState.value
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
        val currentState = _uiState.value
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
        val currentState = _uiState.value
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
                    val currentState = _uiState.value
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

                    _uiState.update { st ->
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
                    }
                }
        }
    }

    /**
     * Fans every rail out and lets each land on its own.
     *
     * The old version awaited all five media requests and, if any one of them failed, replaced the
     * whole screen with a single error. Each rail now writes only its own slice of the state, so a
     * failed request costs that rail and nothing else. Only rails the current tab actually shows
     * are requested: the airing timeline is anime only and Releasing now is manga only.
     */
    private fun loadDiscoveryData(isRefresh: Boolean = false) {
        val mediaType = _uiState.value.mediaType
        val supported = DiscoverSection.defaultOrder(mediaType).toSet()

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.update { state ->
                state.copy(
                    isRefreshing = isRefresh,
                    feeds = state.feeds.let { f ->
                        f.copy(
                            trending = f.trending.copy(isLoading = true, error = null),
                            airingToday = f.airingToday.copy(isLoading = true, error = null),
                            releasing = f.releasing.copy(isLoading = true, error = null),
                            popular = f.popular.copy(isLoading = true, error = null),
                            notYetReleased = f.notYetReleased.copy(isLoading = true, error = null),
                            newlyAdded = f.newlyAdded.copy(isLoading = true, error = null),
                            reviews = f.reviews.copy(isLoading = true, error = null)
                        )
                    }
                )
            }

            val jobs = buildList {
                add(launch {
                    loadEntries(mediaType, { discoverRepository.getTrending(it) }) { f, v -> f.copy(trending = v) }
                })
                add(launch {
                    loadEntries(mediaType, { discoverRepository.getPopular(it) }) { f, v -> f.copy(popular = v) }
                })
                add(launch {
                    loadEntries(mediaType, { discoverRepository.getNotYetReleased(it) }) { f, v -> f.copy(notYetReleased = v) }
                })
                add(launch {
                    loadEntries(mediaType, { discoverRepository.getNewlyAdded(it) }) { f, v -> f.copy(newlyAdded = v) }
                })
                add(launch { loadReviews(mediaType) })
                if (DiscoverSection.AIRING_TODAY in supported) {
                    add(launch { loadAiringToday(mediaType) })
                }
                if (DiscoverSection.RELEASING_NOW in supported) {
                    add(launch {
                        loadEntries(mediaType, { discoverRepository.getReleasing(it) }) { f, v -> f.copy(releasing = v) }
                    })
                }
            }
            jobs.forEach { it.join() }

            if (isRefresh) {
                // The pull-to-refresh spinner reads as a glitch if it vanishes instantly.
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < MIN_REFRESH_SPINNER_MS) delay(MIN_REFRESH_SPINNER_MS - elapsed)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** Run one media rail's request and fold the outcome into its own slot. */
    private suspend fun loadEntries(
        mediaType: MediaType,
        request: suspend (MediaType) -> Result<List<LibraryEntry>>,
        assign: (DiscoverFeeds, SectionFeed<LibraryEntry>) -> DiscoverFeeds
    ) {
        val feed = when (val result = request(mediaType)) {
            is Result.Success -> SectionFeed(items = result.data, isLoading = false)
            is Result.Error -> SectionFeed<LibraryEntry>(isLoading = false, error = result.message)
        }
        // A tab switch mid-flight must not write the old tab's rail over the new one.
        _uiState.update {
            if (it.mediaType != mediaType) it else it.copy(feeds = assign(it.feeds, feed))
        }
    }

    private suspend fun loadReviews(mediaType: MediaType) {
        val result = discoverRepository.getRecentReviews(mediaType = mediaType, page = 1)
        val feed = when (result) {
            is Result.Success -> SectionFeed(items = result.data.reviews, isLoading = false)
            is Result.Error -> SectionFeed<MediaReview>(isLoading = false, error = result.message)
        }
        _uiState.update {
            if (it.mediaType != mediaType) it else it.copy(feeds = it.feeds.copy(reviews = feed))
        }
    }

    /**
     * Today's airing schedule, in the device's own day. `airingSchedules` is sorted by TIME, so the
     * list arrives in the order the timeline draws it. Manga has no counterpart, which is why this
     * only runs on the Anime tab.
     */
    private suspend fun loadAiringToday(mediaType: MediaType) {
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000
        val result = calendarRepository.getWeekSchedule(dayStart, dayStart + DAY_SECONDS)
        val feed = when (result) {
            is Result.Success -> SectionFeed(items = result.data, isLoading = false)
            is Result.Error -> SectionFeed<AiringEpisode>(isLoading = false, error = result.message)
        }
        _uiState.update {
            if (it.mediaType != mediaType) it else it.copy(feeds = it.feeds.copy(airingToday = feed))
        }
    }

    /**
     * A browse chip is a saved search. Rather than inventing a browse screen per axis, each chip
     * applies the filters that describe it and opens the search overlay on the results, which is
     * the same path the genre and tag chips on media details already take.
     *
     * Schedule is the exception and never reaches here: the screen sends it to the airing calendar.
     */
    private fun onBrowseChip(chip: BrowseChip) {
        val current = _uiState.value
        val mediaSearchType =
            if (current.mediaType == MediaType.ANIME) SearchType.ANIME else SearchType.MANGA
        val filters = when (chip) {
            BrowseChip.SEASONAL -> {
                val now = Calendar.getInstance()
                SearchFilters(
                    searchType = SearchType.ANIME,
                    season = now.currentSeason(),
                    yearRange = IntRangeFilter(
                        min = now.get(Calendar.YEAR),
                        max = now.get(Calendar.YEAR)
                    )
                )
            }
            BrowseChip.TOP_100 -> SearchFilters(
                searchType = mediaSearchType,
                sort = SortOption.SCORE_DESC
            )
            BrowseChip.STUDIOS -> SearchFilters(searchType = SearchType.STUDIOS)
            BrowseChip.MANHWA -> SearchFilters(
                searchType = SearchType.MANGA,
                country = OriginCountry.SOUTH_KOREA
            )
            BrowseChip.MANHUA -> SearchFilters(
                searchType = SearchType.MANGA,
                country = OriginCountry.CHINA
            )
            BrowseChip.LIGHT_NOVELS -> SearchFilters(
                searchType = SearchType.MANGA,
                formats = setOf(MediaFormat.NOVEL)
            )
            // Genres has no single preset to apply, so it opens the overlay where the genre
            // picker already lives.
            BrowseChip.GENRES -> SearchFilters(searchType = mediaSearchType)
            BrowseChip.SCHEDULE -> return
        }
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchFilters = filters,
                activeCategory = ResultCategory.ALL,
                searchOverlayRequest = it.searchOverlayRequest + 1
            )
        }
        loadTaxonomyIfNeeded()
        searchTrigger.value = SearchTriggerState("", filters.hashCode())
    }

    /**
     * Add the spotlight title to Planning.
     *
     * No result handling beyond the request itself: the mutation writes through to Room, and the
     * card's own list indicator is fed by [com.anisync.android.presentation.util.LocalLibraryStatuses],
     * so a success shows up as the status chip replacing the button.
     */
    private fun addToPlanning(mediaId: Int) {
        viewModelScope.launch {
            detailsRepository.updateMediaListEntry(mediaId, LibraryStatus.PLANNING, progress = 0)
        }
    }

    /**
     * Re-run one rail's request.
     *
     * Reloading the whole screen to recover a single failed section would throw away six
     * responses that already arrived, and spend six requests against the rate limit to do it.
     */
    private fun retrySection(section: DiscoverSection) {
        val mediaType = _uiState.value.mediaType
        if (!section.supports(mediaType)) return
        _uiState.update { it.copy(feeds = it.feeds.markLoading(section)) }
        viewModelScope.launch {
            when (section) {
                DiscoverSection.TRENDING ->
                    loadEntries(mediaType, { discoverRepository.getTrending(it) }) { f, v -> f.copy(trending = v) }
                DiscoverSection.POPULAR ->
                    loadEntries(mediaType, { discoverRepository.getPopular(it) }) { f, v -> f.copy(popular = v) }
                DiscoverSection.NOT_YET_RELEASED ->
                    loadEntries(mediaType, { discoverRepository.getNotYetReleased(it) }) { f, v -> f.copy(notYetReleased = v) }
                DiscoverSection.NEWLY_ADDED ->
                    loadEntries(mediaType, { discoverRepository.getNewlyAdded(it) }) { f, v -> f.copy(newlyAdded = v) }
                DiscoverSection.RELEASING_NOW ->
                    loadEntries(mediaType, { discoverRepository.getReleasing(it) }) { f, v -> f.copy(releasing = v) }
                DiscoverSection.AIRING_TODAY -> loadAiringToday(mediaType)
                DiscoverSection.REVIEWS -> loadReviews(mediaType)
            }
        }
    }

    /** Mirror the persisted rail order and hidden set into the state for the active tab. */
    private fun observeSectionOrder() {
        viewModelScope.launch {
            combine(
                appSettings.discoverAnimeSectionOrder,
                appSettings.discoverMangaSectionOrder,
                appSettings.hiddenDiscoverAnimeSections,
                appSettings.hiddenDiscoverMangaSections
            ) { _, _, _, _ -> Unit }.collect {
                _uiState.update { state ->
                    state.copy(
                        sectionOrder = visibleSections(state.mediaType),
                        hiddenSections = hiddenSections(state.mediaType)
                    )
                }
            }
        }
    }

    /** The stored order for [type], hidden rails removed. */
    private fun visibleSections(type: MediaType): List<DiscoverSection> {
        val hidden = hiddenSections(type)
        return orderedSections(type).filterNot { it in hidden }
    }

    /** The stored order for [type], hidden rails included, which is what the reorder sheet lists. */
    private fun orderedSections(type: MediaType): List<DiscoverSection> = DiscoverSection.resolveOrder(
        type,
        if (type == MediaType.ANIME) appSettings.discoverAnimeSectionOrder.value
        else appSettings.discoverMangaSectionOrder.value
    )

    private fun hiddenSections(type: MediaType): Set<DiscoverSection> =
        (
            if (type == MediaType.ANIME) appSettings.hiddenDiscoverAnimeSections.value
            else appSettings.hiddenDiscoverMangaSections.value
            )
            .mapNotNull(DiscoverSection::fromId)
            .toSet()

    /** Full order for the reorder sheet, hidden rails still in place. */
    fun sectionsForReorder(): List<DiscoverSection> = orderedSections(_uiState.value.mediaType)

    private fun reorderSections(order: List<DiscoverSection>) {
        appSettings.setDiscoverSectionOrder(_uiState.value.mediaType, order.map { it.id })
    }

    private fun setSectionVisible(section: DiscoverSection, visible: Boolean) {
        val type = _uiState.value.mediaType
        val hidden = hiddenSections(type)
        val next = if (visible) hidden - section else hidden + section
        appSettings.setHiddenDiscoverSections(type, next.map { it.id }.toSet())
    }

    private fun resetSectionOrder() {
        val type = _uiState.value.mediaType
        appSettings.setDiscoverSectionOrder(type, emptyList())
        appSettings.setHiddenDiscoverSections(type, emptySet())
    }

    private fun DiscoverUiState.shouldSearch(): Boolean =
        searchQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH || searchFilters.hasActiveFilters
}

/**
 * Minimum query length before a text-driven search fires. Single-character queries
 * match almost everything and just burn rate-limit budget; filters can still drive
 * a search with a shorter/blank query.
 */
private const val MIN_SEARCH_QUERY_LENGTH = 2

/** How long the refresh spinner stays up even when the network beats it. */
private const val MIN_REFRESH_SPINNER_MS = 800L

private const val DAY_SECONDS = 24L * 60 * 60

/** AniList buckets the year into four seasons starting in December. */
private fun Calendar.currentSeason(): MediaSeason = when (get(Calendar.MONTH)) {
    Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> MediaSeason.WINTER
    Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> MediaSeason.SPRING
    Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> MediaSeason.SUMMER
    else -> MediaSeason.FALL
}

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
