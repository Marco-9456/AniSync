package com.anisync.android.presentation.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.ForumSearchFilters
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchRepository
import com.anisync.android.domain.ThreadEventBus
import com.anisync.android.domain.ThreadSortField
import com.anisync.android.domain.ThreadSortOption
import com.anisync.android.domain.ThreadUpdate
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
import com.anisync.android.type.MediaType
import com.anisync.android.type.ThreadSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Debounce before a thread search / picker query fires — matches Discover. */
private const val SEARCH_DEBOUNCE_MS = 350L

/** Minimum query length before a text-only search fires; filters bypass this. */
private const val MIN_SEARCH_QUERY_LENGTH = 2

/** How many threads each Overview section previews above its expand button. */
private const val OVERVIEW_SECTION_SIZE = 4

/** How many rate-limit windows a load waits out before giving up and showing the failure. */
private const val MAX_RATE_LIMIT_RETRIES = 3

/** A second of grace on top of the countdown, so the retry lands after the window, not on it. */
private const val RETRY_GRACE_MS = 1_000L

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val searchRepository: SearchRepository,
    private val threadEventBus: ThreadEventBus,
    private val appSettings: AppSettings,
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ForumUiState(
            feed = readSavedFeed(),
            selectedCategoryId = appSettings.forumCategoryId.value,
            overviewOrder = readSavedOverviewOrder(),
            hiddenOverviewSections = readHiddenOverviewSections()
        )
    )
    val uiState: StateFlow<ForumUiState> = _uiState.asStateFlow()

    /** Persisted section order, falling back to the declared one and dropping anything stale. */
    private fun readSavedOverviewOrder(): List<OverviewSection> {
        val saved = appSettings.forumSectionOrder.value
            .mapNotNull { name -> runCatching { OverviewSection.valueOf(name) }.getOrNull() }
        // A section added in a later version is absent from a stored order, so it is appended
        // rather than lost.
        return saved + OverviewSection.entries.filterNot { it in saved }
    }

    private fun readHiddenOverviewSections(): Set<OverviewSection> =
        appSettings.hiddenForumSections.value
            .mapNotNull { name -> runCatching { OverviewSection.valueOf(name) }.getOrNull() }
            .toSet()

    /** Resolve the persisted feed, falling back to the Overview. */
    private fun readSavedFeed(): ForumFeed =
        appSettings.forumFeed.value
            ?.let { name -> runCatching { ForumFeed.valueOf(name) }.getOrNull() }
            ?: ForumFeed.OVERVIEW

    private val _actions = Channel<ForumAction>(Channel.BUFFERED)
    val actions: Flow<ForumAction> = _actions.receiveAsFlow()

    private var hasLoadedInitially = false

    private var mediaPickerJob: Job? = null
    private var authorPickerJob: Job? = null
    private var searchPaginationJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadSavedIds()
        observeForumSearch()
        observeThreadEvents()
    }

    /**
     * Keeps the hub list and the search-results list in sync with mutations made on
     * the thread detail screen or other forum lists (like / subscribe / save / reply
     * / delete) without re-fetching. See [ThreadEventBus].
     */
    private fun observeThreadEvents() {
        viewModelScope.launch {
            threadEventBus.events.collect { u ->
                _uiState.update { state ->
                    state.copy(
                        threads = if (u.deleted) {
                            state.threads.filterNot { it.id == u.id }.toPersistentList()
                        } else {
                            state.threads.map { if (it.id == u.id) u.applyTo(it) else it }
                                .toPersistentList()
                        },
                        searchResults = if (u.deleted) {
                            state.searchResults.filterNot { it.id == u.id }.toPersistentList()
                        } else {
                            state.searchResults.map { if (it.id == u.id) u.applyTo(it) else it }
                                .toPersistentList()
                        },
                        savedThreadIds = when (u.isSaved) {
                            true -> (state.savedThreadIds + u.id).toPersistentSet()
                            false -> (state.savedThreadIds - u.id).toPersistentSet()
                            null -> state.savedThreadIds
                        }
                    )
                }
            }
        }
    }

    /**
     * Called from ForumScreen's LaunchedEffect to trigger initial load.
     * Uses a guard to prevent re-loading when navigating back from a thread.
     */
    fun onScreenVisible() {
        if (!hasLoadedInitially) {
            hasLoadedInitially = true
            load(page = 1)
        }
    }

    fun onAction(action: ForumAction) {
        when (action) {
            is ForumAction.Refresh -> {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                load(page = 1, replaceExisting = true)
            }

            is ForumAction.LoadMore -> {
                if (!_uiState.value.hasNextPage || _uiState.value.isLoading || _uiState.value.isPaginating) return
                _uiState.update { it.copy(isPaginating = true) }
                load(page = _uiState.value.currentPage + 1)
            }

            is ForumAction.OnFeedChange -> {
                if (_uiState.value.feed == action.feed) return
                appSettings.setForumFeed(action.feed.name)
                _uiState.update {
                    it.copy(
                        feed = action.feed,
                        // Recent and New are orderings, so picking one seeds the sort. It is a
                        // starting point, not a lock: every feed still honours the sort control.
                        hubFilters = it.hubFilters.copy(sort = action.feed.defaultSort()),
                        isRefreshing = true,
                        openSheet = null
                    )
                }
                load(page = 1, replaceExisting = true)
            }

            is ForumAction.TogglePinnedExpanded ->
                _uiState.update { it.copy(isPinnedExpanded = !it.isPinnedExpanded) }

            is ForumAction.OpenSheet -> _uiState.update { it.copy(openSheet = action.sheet) }
            is ForumAction.DismissSheet ->
                _uiState.update { it.copy(openSheet = null, actionSheetThread = null) }

            is ForumAction.OpenThreadActions -> _uiState.update {
                it.copy(openSheet = ForumSheet.THREAD_ACTIONS, actionSheetThread = action.thread)
            }

            is ForumAction.OnHubSortChange -> {
                if (_uiState.value.hubFilters.sort == action.sort) return
                _uiState.update {
                    it.copy(hubFilters = it.hubFilters.copy(sort = action.sort))
                }
            }

            is ForumAction.OnHubCategoryChange -> {
                appSettings.setForumCategoryId(action.category?.id)
                _uiState.update { it.copy(selectedCategoryId = action.category?.id) }
            }

            is ForumAction.ToggleHubSubscribedOnly -> _uiState.update {
                it.copy(hubFilters = it.hubFilters.copy(subscribedOnly = !it.hubFilters.subscribedOnly))
            }

            is ForumAction.ResetHubFilters -> {
                appSettings.setForumCategoryId(null)
                _uiState.update {
                    it.copy(
                        hubFilters = ForumSearchFilters(),
                        selectedCategoryId = null
                    )
                }
            }

            is ForumAction.OpenReorderSections ->
                _uiState.update { it.copy(isReorderSheetVisible = true) }

            is ForumAction.DismissReorderSections ->
                _uiState.update { it.copy(isReorderSheetVisible = false) }

            is ForumAction.ReorderOverview -> {
                appSettings.setForumSectionOrder(action.order.map { it.name })
                _uiState.update { it.copy(overviewOrder = action.order) }
            }

            is ForumAction.SetOverviewSectionHidden -> {
                val hidden = if (action.visible) {
                    _uiState.value.hiddenOverviewSections - action.section
                } else {
                    _uiState.value.hiddenOverviewSections + action.section
                }
                appSettings.setHiddenForumSections(hidden.map { it.name }.toSet())
                _uiState.update { it.copy(hiddenOverviewSections = hidden) }
            }

            is ForumAction.ResetOverviewOrder -> {
                appSettings.setForumSectionOrder(emptyList())
                appSettings.setHiddenForumSections(emptySet())
                _uiState.update {
                    it.copy(
                        overviewOrder = OverviewSection.entries,
                        hiddenOverviewSections = emptySet()
                    )
                }
            }

            is ForumAction.ApplyHubFilters -> {
                // The Overview's sections each define their own ordering, so a sort has nowhere to
                // land there. Choosing one drops into Recent with it, the same way a category does,
                // rather than leaving a control that silently does nothing.
                val state = _uiState.value
                val leavesOverview = state.feed == ForumFeed.OVERVIEW &&
                        state.hubFilters.sort != ThreadSortOption.Default
                if (leavesOverview) appSettings.setForumFeed(ForumFeed.RECENT.name)
                _uiState.update {
                    it.copy(
                        feed = if (leavesOverview) ForumFeed.RECENT else it.feed,
                        openSheet = null,
                        isRefreshing = true
                    )
                }
                load(page = 1, replaceExisting = true)
            }

            is ForumAction.OnCategoryChange -> {
                if (_uiState.value.selectedCategoryId == action.categoryId) return
                appSettings.setForumCategoryId(action.categoryId)
                // The Overview is a summary of the other feeds, and one of its sections is itself a
                // category. The thread query takes a single category, so narrowing the summary is
                // not expressible; picking one drops into Recent for that category instead.
                val nextFeed = if (
                    _uiState.value.feed == ForumFeed.OVERVIEW && action.categoryId != null
                ) {
                    appSettings.setForumFeed(ForumFeed.RECENT.name)
                    ForumFeed.RECENT
                } else {
                    _uiState.value.feed
                }
                _uiState.update {
                    it.copy(
                        selectedCategoryId = action.categoryId,
                        feed = nextFeed,
                        isRefreshing = true
                    )
                }
                load(page = 1, replaceExisting = true)
            }

            is ForumAction.ToggleSaveThread -> toggleSave(action.thread)
            is ForumAction.ToggleSubscribeThread -> toggleSubscribe(action.thread)

            // ---- Advanced search ----
            is ForumAction.OnSearchQueryChange -> updateFilters { it.copy(query = action.query) }
            is ForumAction.LoadMoreSearch -> loadMoreSearch()
            is ForumAction.ClearSearchFilters ->
                _uiState.update { it.copy(searchFilters = ForumSearchFilters(query = it.searchFilters.query)) }

            is ForumAction.OnSortChange -> updateFilters { it.copy(sort = action.sort) }
            is ForumAction.OnCategoryFilterChange -> updateFilters { it.copy(category = action.category) }
            is ForumAction.ToggleSubscribedOnly -> updateFilters { it.copy(subscribedOnly = !it.subscribedOnly) }

            is ForumAction.OnMediaPickerQueryChange -> onMediaPickerQueryChange(action.query)
            is ForumAction.OnMediaPickerTypeChange -> onMediaPickerTypeChange(action.type)
            is ForumAction.SelectMediaFilter -> {
                updateFilters { it.copy(media = action.entry) }
                _uiState.update {
                    it.copy(
                        mediaPickerQuery = "",
                        mediaPickerResults = persistentListOf()
                    )
                }
            }

            is ForumAction.ClearMediaFilter -> updateFilters { it.copy(media = null) }

            is ForumAction.OnAuthorPickerQueryChange -> onAuthorPickerQueryChange(action.query)
            is ForumAction.SelectAuthorFilter -> {
                updateFilters { it.copy(author = action.user) }
                _uiState.update {
                    it.copy(
                        authorPickerQuery = "",
                        authorPickerResults = persistentListOf()
                    )
                }
            }

            is ForumAction.ClearAuthorFilter -> updateFilters { it.copy(author = null) }

            // Navigation actions are forwarded to the UI layer
            else -> viewModelScope.launch { _actions.send(action) }
        }
    }

    private fun loadSavedIds() {
        viewModelScope.launch {
            val saved = forumRepository.getSavedThreads()
            _uiState.update { it.copy(savedThreadIds = saved.map { t -> t.id }.toPersistentSet()) }
        }
    }

    private fun toggleSave(thread: ForumThread) {
        val isCurrentlySaved = _uiState.value.savedThreadIds.contains(thread.id)

        // Optimistic update
        _uiState.update { state ->
            if (isCurrentlySaved) {
                state.copy(savedThreadIds = (state.savedThreadIds - thread.id).toPersistentSet())
            } else {
                state.copy(savedThreadIds = (state.savedThreadIds + thread.id).toPersistentSet())
            }
        }
        threadEventBus.publish(ThreadUpdate(thread.id, isSaved = !isCurrentlySaved))

        viewModelScope.launch {
            if (isCurrentlySaved) {
                forumRepository.unsaveThread(thread.id)
                toastManager.showToast(ToastType.SUCCESS, message = "Thread unsaved")
            } else {
                forumRepository.saveThread(thread)
                toastManager.showToast(ToastType.SUCCESS, message = "Thread saved")
            }
            // The Saved collection is the list being looked at, so it has to re-read Room.
            val state = _uiState.value
            if (state.feed == ForumFeed.SAVED) {
                load(page = 1, replaceExisting = true)
            }
        }
    }

    private fun toggleSubscribe(thread: ForumThread) {
        val wasSubscribed = thread.isSubscribed

        // Optimistic update across both the hub list and the search results so the
        // toggle reflects immediately wherever the card is shown.
        _uiState.update { state ->
            state.copy(
                threads = state.threads.map {
                    if (it.id == thread.id) it.copy(isSubscribed = !wasSubscribed) else it
                }.toPersistentList(),
                searchResults = state.searchResults.map {
                    if (it.id == thread.id) it.copy(isSubscribed = !wasSubscribed) else it
                }.toPersistentList()
            )
        }
        threadEventBus.publish(ThreadUpdate(thread.id, isSubscribed = !wasSubscribed))

        viewModelScope.launch {
            val result = forumRepository.toggleThreadSubscription(thread.id, !wasSubscribed)
            if (result is Result.Error) {
                // Revert
                _uiState.update { state ->
                    state.copy(
                        threads = state.threads.map {
                            if (it.id == thread.id) it.copy(isSubscribed = wasSubscribed) else it
                        }.toPersistentList(),
                        searchResults = state.searchResults.map {
                            if (it.id == thread.id) it.copy(isSubscribed = wasSubscribed) else it
                        }.toPersistentList()
                    )
                }
                showResultError(result)
            } else {
                val state = _uiState.value
                if (state.feed == ForumFeed.SUBSCRIBED) {
                    load(page = 1, replaceExisting = true)
                }
            }
        }
    }

    // =========================================================================
    // HUB BROWSE (scope + category rail)
    // =========================================================================

    /**
     * A rate limit is the one failure that says when it will be worth asking again, so it holds the
     * skeleton and retries instead of replacing the screen with an error the viewer can do nothing
     * about. Everything else surfaces straight away.
     */
    private fun load(page: Int, replaceExisting: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            var attempts = 0
            while (true) {
                if (page == 1) _uiState.update { it.copy(isLoading = true) }
                val error = runLoad(page, replaceExisting) ?: return@launch

                val wait = error.countdownSeconds
                if (wait == null || wait <= 0 || ++attempts > MAX_RATE_LIMIT_RETRIES) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            errorMessage = error.message
                        )
                    }
                    return@launch
                }

                // Hold the skeleton. The countdown toast already tells the viewer what is going on.
                _uiState.update {
                    it.copy(isLoading = true, isRefreshing = false, errorMessage = null)
                }
                delay(wait * 1_000L + RETRY_GRACE_MS)
            }
        }
    }

    /** One attempt. Returns the failure to the caller, which decides whether to wait and retry. */
    private suspend fun runLoad(page: Int, replaceExisting: Boolean): Result.Error? {
        run {
            val state = _uiState.value

            if (state.feed == ForumFeed.OVERVIEW) {
                return loadOverview()
            }

            val feedSort = state.hubFilters.sort.forBlankQuery()

            // Saved threads live in Room, so this branch costs no request and works offline. The
            // ordering has to be applied here: there is no query to hang a sort argument on.
            if (state.feed == ForumFeed.SAVED) {
                val saved = forumRepository.getSavedThreads()
                val filtered = state.selectedCategoryId?.let { id ->
                    saved.filter { t -> t.categories.any { c -> c.id == id } }
                } ?: saved
                applyThreads(filtered.sortedBy(feedSort), hasNext = false, page = 1, replace = true)
                return null
            }

            val result = when {
                // GetSubscribedThreads takes no sort argument, so anything but the default has to
                // go through the search endpoint with subscribed = true.
                state.feed == ForumFeed.SUBSCRIBED &&
                        state.selectedCategoryId == null &&
                        feedSort == ThreadSortOption.Default ->
                    forumRepository.getSubscribedThreads(page)

                state.feed == ForumFeed.SUBSCRIBED -> forumRepository.searchThreads(
                    categoryId = state.selectedCategoryId,
                    subscribed = true,
                    sort = feedSort,
                    page = page
                )

                state.hubNeedsSearch -> forumRepository.searchThreads(
                    categoryId = state.selectedCategoryId,
                    mediaCategoryId = state.hubFilters.media?.mediaId,
                    userId = state.hubFilters.author?.id,
                    subscribed = state.hubFilters.subscribedOnly.takeIf { it },
                    sort = feedSort,
                    page = page
                )

                else -> forumRepository.getRecentThreads(page, hubSortParam(feedSort))
            }

            return when (result) {
                is Result.Success -> {
                    applyThreads(
                        items = result.data.items,
                        hasNext = result.data.hasNextPage,
                        page = result.data.currentPage,
                        replace = replaceExisting || page == 1
                    )
                    null
                }

                is Result.Error -> result
            }
        }
    }

    private fun applyThreads(
        items: List<ForumThread>,
        hasNext: Boolean,
        page: Int,
        replace: Boolean
    ) {
        _uiState.update { current ->
            val updated = if (replace) {
                items.distinctBy { it.id }.toPersistentList()
            } else {
                (current.threads + items).distinctBy { it.id }.toPersistentList()
            }
            current.copy(
                isLoading = false,
                isRefreshing = false,
                isPaginating = false,
                threads = updated,
                hasNextPage = hasNext,
                currentPage = page,
                errorMessage = null
            )
        }
    }

    /**
     * Sticky threads lead every Browse ordering, which is what the old Overview feed was doing.
     * They are split out of the list in the UI so the section can be collapsed.
     */
    private fun hubSortParam(sort: ThreadSortOption): String =
        (listOf(ThreadSort.IS_STICKY) + sort.forBlankQuery().apiValue).joinToString(",") { it.name }

    /** The ordering a feed starts on. Recent and New are orderings; the rest inherit the default. */
    private fun ForumFeed.defaultSort(): ThreadSortOption = when (this) {
        ForumFeed.NEW -> ThreadSortOption.NEWEST
        else -> ThreadSortOption.Default
    }

    /**
     * Saved threads are Room rows, so every ordering the sheet offers has to be applied in memory.
     * Leaving them unsorted would have made the sort control a no-op on one feed out of five.
     */
    private fun List<ForumThread>.sortedBy(sort: ThreadSortOption): List<ForumThread> {
        val ordered = when (sort.sortField) {
            ThreadSortField.LAST_REPLY, ThreadSortField.RELEVANCE ->
                sortedBy { it.repliedAt ?: it.createdAt }

            ThreadSortField.CREATED -> sortedBy { it.createdAt }
            ThreadSortField.REPLIES -> sortedBy { it.replyCount }
            ThreadSortField.VIEWS -> sortedBy { it.viewCount }
            ThreadSortField.TITLE -> sortedBy { it.title.lowercase() }
        }
        return if (sort.isAscending) ordered else ordered.reversed()
    }

    /** SEARCH_MATCH only ranks against a query, so it degrades to the default ordering here. */
    private fun ThreadSortOption.forBlankQuery(): ThreadSortOption =
        if (this == ThreadSortOption.RELEVANCE) ThreadSortOption.Default else this

    /**
     * The Overview is three short previews rather than a list of its own: what is being replied to,
     * what is being discussed as it airs, and what has just been posted. Each section's expand
     * button switches to the feed it previews, so no section is a dead end.
     */
    private suspend fun loadOverview(): Result.Error? {
        when (val recent = forumRepository.getRecentThreads(1, "IS_STICKY,REPLIED_AT_DESC")) {
            is Result.Error -> return recent

            is Result.Success -> {
                val items = recent.data.items
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isPaginating = false,
                        errorMessage = null,
                        hasNextPage = false,
                        overviewPinned = items.filter { t -> t.isSticky }.toPersistentList(),
                        overviewRecent = items.filterNot { t -> t.isSticky }
                            .take(OVERVIEW_SECTION_SIZE).toPersistentList()
                    )
                }
            }
        }

        val release = forumRepository.searchThreads(
            categoryId = RELEASE_DISCUSSION_CATEGORY_ID,
            sort = ThreadSortOption.Default,
            page = 1,
            allowCached = true
        )
        if (release is Result.Success) {
            _uiState.update {
                it.copy(
                    overviewRelease = release.data.items
                        .take(OVERVIEW_SECTION_SIZE).toPersistentList()
                )
            }
        }

        val newly = forumRepository.getRecentThreads(1, "CREATED_AT_DESC")
        if (newly is Result.Success) {
            _uiState.update {
                it.copy(
                    overviewNew = newly.data.items
                        .filterNot { t -> t.isSticky }
                        .take(OVERVIEW_SECTION_SIZE).toPersistentList()
                )
            }
        }
        // The first section already rendered, so a failure in the other two leaves the Overview
        // short rather than empty, and is not worth a full-screen error.
        return null
    }

    // =========================================================================
    // ADVANCED SEARCH (overlay)
    // =========================================================================

    private fun updateFilters(transform: (ForumSearchFilters) -> ForumSearchFilters) {
        _uiState.update { it.copy(searchFilters = transform(it.searchFilters)) }
    }

    /**
     * Drives the search list off filter changes: debounced, deduped by value, and
     * latest-wins so rapid typing/filter taps cancel the in-flight request rather
     * than queueing requests. Mirrors DiscoverViewModel.observeSearchQuery.
     */
    @OptIn(FlowPreview::class)
    private fun observeForumSearch() {
        viewModelScope.launch {
            _uiState
                .map { it.searchFilters }
                .distinctUntilChanged()
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { filters -> runThreadSearch(filters) }
        }
    }

    private fun ForumSearchFilters.shouldSearch(): Boolean =
        query.trim().length >= MIN_SEARCH_QUERY_LENGTH || hasActiveFilters

    private suspend fun runThreadSearch(filters: ForumSearchFilters) {
        if (!filters.shouldSearch()) {
            _uiState.update {
                it.copy(
                    searchResults = persistentListOf(),
                    isSearching = false,
                    searchIsPaginating = false,
                    searchHasNextPage = false,
                    searchCurrentPage = 1,
                    searchError = null
                )
            }
            return
        }

        _uiState.update { it.copy(isSearching = true, searchError = null) }
        when (val result = fetchThreads(filters, page = 1)) {
            is Result.Success -> {
                val data = result.data
                _uiState.update {
                    it.copy(
                        searchResults = data.items.distinctBy { t -> t.id }.toPersistentList(),
                        searchHasNextPage = data.hasNextPage,
                        searchCurrentPage = data.currentPage,
                        isSearching = false,
                        searchIsPaginating = false,
                        searchError = null
                    )
                }
            }

            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchIsPaginating = false,
                        searchError = result.message
                    )
                }
            }
        }
    }

    private fun loadMoreSearch() {
        val state = _uiState.value
        if (!state.searchHasNextPage || state.isSearching || state.searchIsPaginating) return
        val filters = state.searchFilters
        val nextPage = state.searchCurrentPage + 1
        _uiState.update { it.copy(searchIsPaginating = true) }
        searchPaginationJob?.cancel()
        searchPaginationJob = viewModelScope.launch {
            when (val result = fetchThreads(filters, page = nextPage)) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        current.copy(
                            searchResults = (current.searchResults + data.items)
                                .distinctBy { it.id }
                                .toPersistentList(),
                            searchHasNextPage = data.hasNextPage,
                            searchCurrentPage = data.currentPage,
                            searchIsPaginating = false
                        )
                    }
                }

                is Result.Error -> _uiState.update {
                    it.copy(searchIsPaginating = false, searchError = result.message)
                }
            }
        }
    }

    private suspend fun fetchThreads(filters: ForumSearchFilters, page: Int) =
        forumRepository.searchThreads(
            search = filters.query,
            categoryId = filters.category?.id,
            mediaCategoryId = filters.media?.mediaId,
            userId = filters.author?.id,
            subscribed = filters.subscribedOnly.takeIf { it },
            sort = filters.sort,
            page = page
        )

    // ---- Media filter picker ----

    private fun onMediaPickerQueryChange(query: String) {
        _uiState.update { it.copy(mediaPickerQuery = query, pickerError = null) }
        mediaPickerJob?.cancel()
        if (query.trim().length < MIN_SEARCH_QUERY_LENGTH) {
            _uiState.update {
                it.copy(
                    mediaPickerResults = persistentListOf(),
                    isMediaPickerSearching = false
                )
            }
            return
        }
        mediaPickerJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runMediaPickerSearch()
        }
    }

    private fun onMediaPickerTypeChange(type: MediaType) {
        if (type == _uiState.value.mediaPickerType) return
        _uiState.update { it.copy(mediaPickerType = type, mediaPickerResults = persistentListOf()) }
        mediaPickerJob?.cancel()
        if (_uiState.value.mediaPickerQuery.trim().length >= MIN_SEARCH_QUERY_LENGTH) {
            mediaPickerJob = viewModelScope.launch { runMediaPickerSearch() }
        }
    }

    private suspend fun runMediaPickerSearch() {
        val state = _uiState.value
        _uiState.update { it.copy(isMediaPickerSearching = true) }
        when (val result = searchRepository.searchMedia(
            query = state.mediaPickerQuery,
            type = state.mediaPickerType
        )) {
            is Result.Success -> _uiState.update {
                it.copy(
                    mediaPickerResults = result.data.entries.toPersistentList(),
                    isMediaPickerSearching = false
                )
            }

            is Result.Error -> _uiState.update {
                it.copy(
                    isMediaPickerSearching = false,
                    pickerError = result.message,
                    mediaPickerResults = persistentListOf()
                )
            }
        }
    }

    // ---- Author filter picker ----

    private fun onAuthorPickerQueryChange(query: String) {
        _uiState.update { it.copy(authorPickerQuery = query, pickerError = null) }
        authorPickerJob?.cancel()
        if (query.trim().length < MIN_SEARCH_QUERY_LENGTH) {
            _uiState.update {
                it.copy(
                    authorPickerResults = persistentListOf(),
                    isAuthorPickerSearching = false
                )
            }
            return
        }
        authorPickerJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runAuthorPickerSearch()
        }
    }

    private suspend fun runAuthorPickerSearch() {
        val query = _uiState.value.authorPickerQuery
        _uiState.update { it.copy(isAuthorPickerSearching = true) }
        when (val result = searchRepository.searchAll(query)) {
            is Result.Success -> _uiState.update {
                it.copy(
                    authorPickerResults = result.data.users.toPersistentList(),
                    isAuthorPickerSearching = false
                )
            }

            is Result.Error -> _uiState.update {
                it.copy(
                    isAuthorPickerSearching = false,
                    pickerError = result.message,
                    authorPickerResults = persistentListOf()
                )
            }
        }
    }

    private fun showResultError(result: Result.Error) {
        toastManager.showResultError(result)
    }
}
