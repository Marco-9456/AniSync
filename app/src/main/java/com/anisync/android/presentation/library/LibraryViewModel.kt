package com.anisync.android.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
import com.anisync.android.presentation.util.LIBRARY_ALL_TAB_ID
import com.anisync.android.presentation.util.LIBRARY_FAVORITES_TAB_ID
import com.anisync.android.util.getTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val profileRepository: ProfileRepository,
    private val appSettings: AppSettings,
    private val toastManager: ToastManager
) : ViewModel() {

    companion object {
        /** Canonical tab identifiers for the built-in (non-custom) tabs. */
        val DEFAULT_TAB_IDS = listOf(
            "status:CURRENT",
            "status:REPEATING",
            "status:PAUSED",
            "status:COMPLETED",
            "status:PLANNING",
            "status:DROPPED",
            "status:FAVORITES"
        )
    }

    private val _uiState = MutableStateFlow(
        LibraryUiState(
            mediaType = appSettings.libraryMediaType.value,
            isGridView = appSettings.libraryGridView.value,
            sortOption = runCatching { LibrarySort.valueOf(appSettings.librarySortOption.value) }
                .getOrDefault(LibrarySort.AIRING_SOON),
            isAscending = appSettings.librarySortAscending.value
        )
    )
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<LibraryAction>()
    val actions: SharedFlow<LibraryAction> = _actions.asSharedFlow()

    private var hasLoadedInitially = false
    private var hasRestoredTab = false

    private data class LibraryComputed(
        val allEntries: List<LibraryEntry>,
        val grouped: Map<LibraryStatus, List<LibraryEntry>>,
        val customNames: List<String>,
        val customEntries: Map<String, List<LibraryEntry>>,
        val favorites: List<LibraryEntry>,
        val hiddenListNames: Set<String>,
        val tabOrder: List<String>,
        val tabCounts: Map<String, Int>,
        val searchMatches: List<LibraryEntry>,
        val searchMatchesByCategory: Map<String, List<LibraryEntry>>
    )

    init {
        appSettings.titleLanguage.onEach { lang ->
            _uiState.update { it.copy(titleLanguage = lang) }
        }.launchIn(viewModelScope)
        
        appSettings.showPrivateEntries.onEach { show ->
            _uiState.update { it.copy(showPrivateEntries = show) }
        }.launchIn(viewModelScope)
        
        appSettings.userScoreFormat.onEach { format ->
            _uiState.update { it.copy(userScoreFormat = format) }
        }.launchIn(viewModelScope)

        observeLibraryData()
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.OnScreenVisible -> onScreenVisible()
            is LibraryAction.Refresh -> refresh()
            is LibraryAction.OnMediaTypeChange -> {
                hasRestoredTab = false
                appSettings.setLibraryMediaType(action.type)
                _uiState.update {
                    it.copy(
                        mediaType = action.type,
                        isLoading = true,
                        errorMessage = null,
                        initialTabId = null,
                        activeSearchCategory = LIBRARY_ALL_TAB_ID
                    )
                }
                refresh()
            }

            is LibraryAction.OnSortOptionChange -> {
                appSettings.setLibrarySort(action.sort.name, action.ascending)
                _uiState.update {
                    it.copy(
                        sortOption = action.sort,
                        isAscending = action.ascending
                    )
                }
            }

            is LibraryAction.OnSearchQueryChange -> {
                _uiState.update {
                    it.copy(
                        searchQuery = action.query,
                        // A blank query resets the category chips back to "All".
                        activeSearchCategory = if (action.query.isBlank()) LIBRARY_ALL_TAB_ID else it.activeSearchCategory
                    )
                }
            }

            is LibraryAction.OnSearchCategoryChange -> {
                _uiState.update { it.copy(activeSearchCategory = action.categoryId) }
            }

            is LibraryAction.OnSearchOpened -> {
                // Seed the category to the tab search was opened from (the "search this list" case);
                // the UI falls back to "All" if that list has no matches for the current query.
                _uiState.update { it.copy(activeSearchCategory = action.currentTabId) }
            }

            is LibraryAction.IncrementProgress -> updateProgress(action.mediaId, 1)
            is LibraryAction.DecrementProgress -> updateProgress(action.mediaId, -1)
            is LibraryAction.UpdateEntry -> updateEntry(action.entry)
            is LibraryAction.DeleteEntry -> deleteEntry(action.entryId, action.mediaId)
            is LibraryAction.ToggleListVisibility -> toggleListVisibility(
                action.listName,
                action.hidden
            )

            is LibraryAction.ReorderTabs -> reorderTabs(action.tabOrder)
            is LibraryAction.CreateCustomList -> createCustomList(action.listName, action.type)
            is LibraryAction.DeleteCustomList -> deleteCustomList(action.listName)
            is LibraryAction.TogglePrivateVisibility -> appSettings.setShowPrivateEntries(action.show)
            is LibraryAction.SetGridView -> {
                appSettings.setLibraryGridView(action.isGrid)
                _uiState.update { it.copy(isGridView = action.isGrid) }
            }
            is LibraryAction.OnTabSelected -> saveSelectedTab(action.tabId)
            is LibraryAction.ConsumeInitialTab -> _uiState.update { it.copy(initialTabId = null) }
        }
    }

    private fun observeLibraryData() {
        viewModelScope.launch {
            _uiState
                .map { it.mediaType }
                .distinctUntilChanged()
                .flatMapLatest { type ->
                    val listOrderFlow = if (type == com.anisync.android.type.MediaType.ANIME) appSettings.animeListOrder else appSettings.mangaListOrder
                    val hiddenListsFlow = if (type == com.anisync.android.type.MediaType.ANIME) appSettings.hiddenAnimeLists else appSettings.hiddenMangaLists
                    
                    combine(
                        libraryRepository.observeLibrary("", type),
                        profileRepository.observeProfile(),
                        listOrderFlow,
                        hiddenListsFlow
                    ) { libraryEntries, profile, listOrder, hiddenLists ->
                        val favorites =
                            profile?.favoriteAnime?.filter { it.type == type } ?: emptyList()
                        Triple(libraryEntries, favorites, listOrder to hiddenLists)
                    }
                }
                .combine(
                    _uiState.map { state ->
                        listOf(
                            state.sortOption,
                            state.isAscending,
                            state.searchQuery,
                            state.titleLanguage,
                            state.showPrivateEntries
                        )
                    }.distinctUntilChanged()
                ) { (entries, favorites, listPrefs), combinedState ->
                    val sort = combinedState[0] as LibrarySort
                    val ascending = combinedState[1] as Boolean
                    val query = combinedState[2] as String
                    val titleLang = combinedState[3] as com.anisync.android.data.TitleLanguage
                    val showPrivate = combinedState[4] as Boolean
                    val (listOrder, hiddenLists) = listPrefs

                    // No early return for an empty library: the sort/group/count logic below all
                    // collapses to empties cleanly, and favorites/custom lists can still be non-empty.
                    class SortableEntry(val entry: LibraryEntry, val sortTitle: String)

                    // Guard against any duplicate-media rows still cached locally (e.g. a stale id=0
                    // optimistic-add row not yet reconciled by a sync): render one card per media,
                    // otherwise the lazy grid crashes on the colliding key.
                    val dedupedEntries = entries
                        .groupBy { it.mediaId }
                        .map { (_, dups) -> dups.maxByOrNull { it.updatedAt ?: 0L } ?: dups.first() }
                    val sortableEntries =
                        dedupedEntries.map { SortableEntry(it, it.getTitle(titleLang).lowercase()) }

                    val titleDir = if (ascending) 1 else -1
                    val keyDir = -titleDir
                    val titleCmp = Comparator<SortableEntry> { a, b ->
                        a.sortTitle.compareTo(b.sortTitle) * titleDir
                    }
                    fun <K : Comparable<K>> primaryDesc(key: (SortableEntry) -> K?): Comparator<SortableEntry> =
                        Comparator { a, b ->
                            val ka = key(a); val kb = key(b)
                            val cmp = when {
                                ka == null && kb == null -> 0
                                ka == null -> 1
                                kb == null -> -1
                                else -> ka.compareTo(kb)
                            }
                            if (cmp != 0) cmp * keyDir else titleCmp.compare(a, b)
                        }

                    val sortedEntries = when (sort) {
                        LibrarySort.TITLE -> sortableEntries.sortedWith(titleCmp)
                        LibrarySort.PROGRESS -> sortableEntries.sortedWith(primaryDesc { it.entry.progress })
                        LibrarySort.AIRING_SOON -> sortableEntries.sortedWith(
                            Comparator { a, b ->
                                val ka = a.entry.nextAiringEpisodeTime
                                val kb = b.entry.nextAiringEpisodeTime
                                val cmp = when {
                                    ka == null && kb == null -> 0
                                    ka == null -> 1
                                    kb == null -> -1
                                    else -> ka.compareTo(kb)
                                }
                                val withDir = if (ascending) cmp else -cmp
                                if (withDir != 0) withDir else titleCmp.compare(a, b)
                            }
                        )
                        LibrarySort.SCORE -> sortableEntries.sortedWith(primaryDesc { it.entry.score })
                        LibrarySort.LAST_UPDATED -> sortableEntries.sortedWith(primaryDesc { it.entry.updatedAt })
                        LibrarySort.LAST_ADDED -> sortableEntries.sortedWith(primaryDesc { it.entry.createdAt })
                        LibrarySort.START_DATE -> sortableEntries.sortedWith(primaryDesc { it.entry.startedAt })
                        LibrarySort.RELEASE_DATE -> sortableEntries.sortedWith(primaryDesc { it.entry.mediaStartDate })
                    }

                    val customEntriesMap = HashMap<String, MutableList<LibraryEntry>>()
                    val customNames = HashSet<String>()
                    val visibilityFiltered = ArrayList<SortableEntry>(sortedEntries.size)
                    for (s in sortedEntries) {
                        val e = s.entry
                        val notPrivate = showPrivate || e.isPrivate != true
                        if (notPrivate) {
                            for (name in e.customLists) {
                                customNames.add(name)
                                customEntriesMap.getOrPut(name) { ArrayList() }.add(e)
                            }
                        }
                        if (notPrivate && !e.hiddenFromStatusLists) {
                            visibilityFiltered.add(s)
                        }
                    }
                    customNames.addAll(listOrder)

                    // The tabs (and their count badges) always show the full lists — the search box
                    // no longer shrinks them. Searching is computed separately below and surfaced
                    // only in the search overlay (#91).
                    val allEntries = ArrayList<LibraryEntry>(visibilityFiltered.size).also { out ->
                        for (s in visibilityFiltered) out.add(s.entry)
                    }
                    val grouped = allEntries.groupBy { it.status }

                    val customNamesSet = customNames.toSet()
                    val tabOrder = buildTabOrder(listOrder, customNamesSet)

                    // Extract sorted custom names from the tab order for the UI
                    val sortedCustomNames = tabOrder.filter { !it.startsWith("status:") && it in customNamesSet }

                    val sortedFavorites = favorites.sortedBy { it.getTitle(titleLang).lowercase() }

                    // Raw per-tab counts (independent of the query) for the tab badges.
                    val tabCounts = buildMap {
                        put(LIBRARY_ALL_TAB_ID, allEntries.size)
                        grouped.forEach { (status, list) -> put("status:${status.name}", list.size) }
                        put(LIBRARY_FAVORITES_TAB_ID, sortedFavorites.size)
                        customEntriesMap.forEach { (name, list) -> put(name, list.size) }
                    }

                    // Search matches every title variant + notes, grouped by list so the overlay can
                    // offer Discover-style category chips.
                    val trimmedQuery = query.trim()
                    val searchMatches: List<LibraryEntry>
                    val searchMatchesByCategory: Map<String, List<LibraryEntry>>
                    if (trimmedQuery.isBlank()) {
                        searchMatches = emptyList()
                        searchMatchesByCategory = emptyMap()
                    } else {
                        val lowerQuery = trimmedQuery.lowercase()
                        val matches = allEntries.filter { it.matchesQuery(lowerQuery) }
                        val byCategory = LinkedHashMap<String, List<LibraryEntry>>()
                        matches.groupBy { it.status }.forEach { (status, list) ->
                            byCategory["status:${status.name}"] = list
                        }
                        sortedFavorites.filter { it.matchesQuery(lowerQuery) }
                            .takeIf { it.isNotEmpty() }
                            ?.let { byCategory[LIBRARY_FAVORITES_TAB_ID] = it }
                        customEntriesMap.forEach { (name, list) ->
                            list.filter { it.matchesQuery(lowerQuery) }
                                .takeIf { it.isNotEmpty() }
                                ?.let { byCategory[name] = it }
                        }
                        searchMatches = matches
                        searchMatchesByCategory = byCategory
                    }

                    LibraryComputed(
                        allEntries = allEntries,
                        grouped = grouped,
                        customNames = sortedCustomNames,
                        customEntries = customEntriesMap,
                        favorites = sortedFavorites,
                        hiddenListNames = hiddenLists,
                        tabOrder = tabOrder,
                        tabCounts = tabCounts,
                        searchMatches = searchMatches,
                        searchMatchesByCategory = searchMatchesByCategory
                    )
                }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Unknown error"
                        )
                    }
                }
                .collect { computed ->
                    // On first emission, resolve saved tab with fallback ("all" is always visible).
                    val resolvedInitialTab = if (!hasRestoredTab) {
                        hasRestoredTab = true
                        val savedTabId = if (_uiState.value.mediaType == com.anisync.android.type.MediaType.ANIME) {
                            appSettings.lastSelectedAnimeTab.value
                        } else {
                            appSettings.lastSelectedMangaTab.value
                        }
                        val visibleTabs = computed.tabOrder.filter { it !in computed.hiddenListNames }
                        when {
                            savedTabId != null && savedTabId in visibleTabs -> savedTabId
                            // With nothing saved, default to Watching rather than the first tab ("All").
                            "status:CURRENT" in visibleTabs -> "status:CURRENT"
                            else -> null
                        }
                    } else {
                        null
                    }

                    _uiState.update {
                        it.copy(
                            entries = computed.allEntries,
                            groupedEntries = computed.grouped,
                            customListNames = computed.customNames,
                            customListEntries = computed.customEntries,
                            favoriteEntries = computed.favorites,
                            hiddenListNames = computed.hiddenListNames,
                            tabOrder = computed.tabOrder,
                            tabCounts = computed.tabCounts,
                            searchMatches = computed.searchMatches,
                            searchMatchesByCategory = computed.searchMatchesByCategory,
                            initialTabId = resolvedInitialTab ?: it.initialTabId,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun onScreenVisible() {
        if (!hasLoadedInitially) {
            hasLoadedInitially = true
            refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val result = libraryRepository.refreshLibrary("", _uiState.value.mediaType)) {
                is Result.Success -> {} // Automatically updated via Flow
                is Result.Error -> showResultError(result)
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    // Coalesces rapid +/- taps on the same media into one SaveMediaListEntry of the
    // settled value (was one network save per tap). Each tap updates the UI
    // optimistically across every list the entry appears in; a +1 then −1 nets to a
    // no-op. On failure the optimistic value rolls back to the last known-good one.
    private val progressBaseline = java.util.concurrent.ConcurrentHashMap<Int, Int>()
    private val progressCoalescer =
        com.anisync.android.presentation.util.MutationCoalescer<Int, Int>(viewModelScope, debounceMs = 600L) { mediaId, progress ->
            when (val result = libraryRepository.updateProgress(mediaId, progress)) {
                is Result.Success -> {
                    progressBaseline[mediaId] = progress
                    true
                }
                is Result.Error -> {
                    progressBaseline[mediaId]?.let { patchEntryProgress(mediaId, it) }
                    showResultError(result)
                    false
                }
            }
        }

    private fun updateProgress(mediaId: Int, delta: Int) {
        val entry = _uiState.value.entries.find { it.mediaId == mediaId } ?: return
        progressCoalescer.seed(mediaId, entry.progress)
        progressBaseline.putIfAbsent(mediaId, entry.progress)
        val newProgress = (entry.progress + delta).coerceAtLeast(0)
        patchEntryProgress(mediaId, newProgress)
        progressCoalescer.submit(mediaId, newProgress)
    }

    /** Optimistically set [mediaId]'s progress across every list it appears in. */
    private fun patchEntryProgress(mediaId: Int, newProgress: Int) {
        fun List<LibraryEntry>.patched(): List<LibraryEntry> =
            map { if (it.mediaId == mediaId) it.copy(progress = newProgress) else it }
        _uiState.update { st ->
            st.copy(
                entries = st.entries.patched(),
                groupedEntries = st.groupedEntries.mapValues { it.value.patched() },
                customListEntries = st.customListEntries.mapValues { it.value.patched() },
                favoriteEntries = st.favoriteEntries.patched()
            )
        }
    }

    private fun updateEntry(entry: LibraryEntry) {
        viewModelScope.launch {
            when (val result = libraryRepository.updateEntry(entry)) {
                is Result.Success -> toastManager.showToast(ToastType.SUCCESS, message = "Entry updated")
                is Result.Error -> showResultError(result)
            }
        }
    }

    private fun deleteEntry(entryId: Int, mediaId: Int) {
        viewModelScope.launch {
            when (val result = libraryRepository.deleteEntry(entryId, mediaId)) {
                is Result.Success -> toastManager.showToast(ToastType.SUCCESS, message = "Entry removed")
                is Result.Error -> showResultError(result)
            }
        }
    }

    private fun toggleListVisibility(listName: String, hidden: Boolean) {
        val current = _uiState.value.hiddenListNames.toMutableSet()
        if (hidden) current.add(listName) else current.remove(listName)
        
        if (_uiState.value.mediaType == com.anisync.android.type.MediaType.ANIME) {
            appSettings.setHiddenAnimeLists(current)
        } else {
            appSettings.setHiddenMangaLists(current)
        }
    }

    /**
     * Saves the new full tab order after a drag-to-reorder operation.
     */
    private fun reorderTabs(newOrder: List<String>) {
        if (_uiState.value.mediaType == com.anisync.android.type.MediaType.ANIME) {
            appSettings.setAnimeListOrder(newOrder)
        } else {
            appSettings.setMangaListOrder(newOrder)
        }
    }

    /**
     * Persist the selected tab for the current media type.
     */
    private fun saveSelectedTab(tabId: String) {
        if (_uiState.value.mediaType == com.anisync.android.type.MediaType.ANIME) {
            appSettings.setLastSelectedAnimeTab(tabId)
        } else {
            appSettings.setLastSelectedMangaTab(tabId)
        }
    }

    /**
     * Builds a unified tab order from the stored order and discovered custom list names.
     * Handles backward compatibility: if the stored order contains no "status:" entries,
     * it's treated as a legacy custom-only order and the default tabs are prepended.
     */
    private fun buildTabOrder(storedOrder: List<String>, customNames: Set<String>): List<String> {
        fun isKnown(id: String) =
            id == LIBRARY_ALL_TAB_ID || id.startsWith("status:") || id in customNames

        val hasStatusEntries = storedOrder.any { it.startsWith("status:") }

        val base: List<String> = if (!hasStatusEntries) {
            // Legacy or empty format: stored order contains only custom list names (or nothing)
            val storedSet = storedOrder.toSet()
            DEFAULT_TAB_IDS +
                    storedOrder.filter { it in customNames } +
                    customNames.filter { it !in storedSet }
        } else {
            // Unified format: stored order contains everything the user has arranged
            val storedSet = storedOrder.toSet()
            val result = storedOrder.filter { isKnown(it) }.toMutableList()

            // Append any missing default tabs (safety net)
            for (tab in DEFAULT_TAB_IDS) {
                if (tab !in storedSet) result.add(tab)
            }

            // Append any new custom lists not in stored order
            for (name in customNames) {
                if (name !in storedSet) result.add(name)
            }

            result
        }

        // The "All" tab always exists and is reorderable/hideable like any other. If the stored order
        // predates it (existing users) or is a legacy order, pin it first; once the user has moved or
        // hidden it, that choice lives in the stored order and is respected here.
        return if (LIBRARY_ALL_TAB_ID in base) base else listOf(LIBRARY_ALL_TAB_ID) + base
    }

    private fun createCustomList(listName: String, type: com.anisync.android.type.MediaType) {
        viewModelScope.launch {
            when (val result = libraryRepository.createCustomList(listName, type)) {
                is Result.Success -> {
                    toastManager.showToast(ToastType.SUCCESS, message = "List '$listName' created.")
                    refresh()
                }
                is Result.Error -> showResultError(result)
            }
        }
    }

    private fun deleteCustomList(listName: String) {
        viewModelScope.launch {
            when (val result =
                libraryRepository.deleteCustomList(listName, _uiState.value.mediaType)) {
                is Result.Success -> {
                    // Remove from settings
                    val currentOrder = _uiState.value.tabOrder.toMutableList()
                    currentOrder.remove(listName)
                    
                    val hiddenLists = _uiState.value.hiddenListNames.toMutableSet()
                    hiddenLists.remove(listName)
                    
                    if (_uiState.value.mediaType == com.anisync.android.type.MediaType.ANIME) {
                        appSettings.setAnimeListOrder(currentOrder)
                        appSettings.setHiddenAnimeLists(hiddenLists)
                    } else {
                        appSettings.setMangaListOrder(currentOrder)
                        appSettings.setHiddenMangaLists(hiddenLists)
                    }

                    toastManager.showToast(ToastType.SUCCESS, message = "List '$listName' deleted")
                    refresh()
                }

                is Result.Error -> showResultError(result)
            }
        }
    }

    private fun showResultError(result: Result.Error) {
        toastManager.showResultError(result)
    }
}

/**
 * Case-insensitive match of [lowerQuery] against every title variant and the entry's notes, so
 * search finds a title regardless of the user's display-language preference (#91) and still matches
 * on note text (#75).
 */
private fun LibraryEntry.matchesQuery(lowerQuery: String): Boolean =
    titleRomaji?.contains(lowerQuery, ignoreCase = true) == true ||
        titleEnglish?.contains(lowerQuery, ignoreCase = true) == true ||
        titleNative?.contains(lowerQuery, ignoreCase = true) == true ||
        titleUserPreferred.contains(lowerQuery, ignoreCase = true) ||
        notes?.contains(lowerQuery, ignoreCase = true) == true