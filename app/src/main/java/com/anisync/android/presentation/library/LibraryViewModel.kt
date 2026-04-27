package com.anisync.android.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
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
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<LibraryAction>()
    val actions: SharedFlow<LibraryAction> = _actions.asSharedFlow()

    private var hasLoadedInitially = false

    private data class LibraryCustomData(
        val customNames: List<String>,
        val customEntriesMap: Map<String, List<LibraryEntry>>,
        val sortedFavorites: List<LibraryEntry>,
        val hiddenListNames: Set<String>,
        val listOrder: List<String>
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
                _uiState.update {
                    it.copy(
                        mediaType = action.type,
                        isLoading = true,
                        errorMessage = null
                    )
                }
                refresh()
            }

            is LibraryAction.OnSortOptionChange -> {
                _uiState.update {
                    it.copy(
                        sortOption = action.sort,
                        isAscending = action.ascending
                    )
                }
            }

            is LibraryAction.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = action.query) }
            }

            is LibraryAction.IncrementProgress -> updateProgress(action.mediaId, 1)
            is LibraryAction.DecrementProgress -> updateProgress(action.mediaId, -1)
            is LibraryAction.UpdateEntry -> updateEntry(action.entry)
            is LibraryAction.DeleteEntry -> deleteEntry(action.entryId, action.mediaId)
            is LibraryAction.ShowSnackbar -> {
                viewModelScope.launch { _actions.emit(LibraryAction.ShowSnackbar(action.message)) }
            }

            is LibraryAction.ToggleListVisibility -> toggleListVisibility(
                action.listName,
                action.hidden
            )

            is LibraryAction.MoveListUp -> moveList(action.listName, -1)
            is LibraryAction.MoveListDown -> moveList(action.listName, 1)
            is LibraryAction.CreateCustomList -> createCustomList(action.listName, action.type)
            is LibraryAction.DeleteCustomList -> deleteCustomList(action.listName)
            is LibraryAction.TogglePrivateVisibility -> appSettings.setShowPrivateEntries(action.show)
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

                    if (entries.isEmpty()) {
                        return@combine Triple(
                            emptyList<LibraryEntry>(),
                            emptyMap<LibraryStatus, List<LibraryEntry>>(),
                            LibraryCustomData(
                                emptyList(),
                                emptyMap(),
                                favorites,
                                hiddenLists,
                                listOrder
                            )
                        )
                    }

                    // Pre-cache localized lowercase titles once. Building a fresh `getTitle().lowercase()`
                    // inside every comparator invocation was the dominant CPU cost on big libraries
                    // (the original code carried an explicit comment about this); we keep that win
                    // and add several more on top.
                    class SortableEntry(val entry: LibraryEntry, val sortTitle: String)

                    val sortableEntries =
                        entries.map { SortableEntry(it, it.getTitle(titleLang).lowercase()) }

                    // Direction-aware comparator: instead of sorting ascending and then doing a full
                    // O(n) `reversed()` allocation for descending, flip the direction inside the
                    // comparator. Saves a whole list copy on every recompose-driven pipeline run.
                    val titleDir = if (ascending) 1 else -1
                    val keyDir = -titleDir // primary keys are descending in the original UX
                    val titleCmp = Comparator<SortableEntry> { a, b ->
                        a.sortTitle.compareTo(b.sortTitle) * titleDir
                    }
                    fun <K : Comparable<K>> primaryDesc(key: (SortableEntry) -> K?): Comparator<SortableEntry> =
                        Comparator { a, b ->
                            val ka = key(a); val kb = key(b)
                            val cmp = when {
                                ka == null && kb == null -> 0
                                ka == null -> 1            // nulls last regardless of direction
                                kb == null -> -1
                                else -> ka.compareTo(kb)
                            }
                            if (cmp != 0) cmp * keyDir else titleCmp.compare(a, b)
                        }

                    val sortedEntries = when (sort) {
                        LibrarySort.TITLE -> sortableEntries.sortedWith(titleCmp)
                        LibrarySort.PROGRESS -> sortableEntries.sortedWith(primaryDesc { it.entry.progress })
                        LibrarySort.AIRING_SOON -> sortableEntries.sortedWith(
                            // AIRING_SOON wants nulls-last with the soonest first; the legacy
                            // `compareBy(nullsLast())` was ascending — we replicate by flipping
                            // direction signs only on the title tiebreak, not the primary key.
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

                    // Single-pass partition: split into the visibility-filtered list (for
                    // status grouping + search) and accumulate custom-list buckets at the
                    // same time. The previous code walked the sorted list 4 separate times
                    // (visibility filter, search filter, groupBy, custom-list filter+forEach).
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

                    val filteredEntries: List<LibraryEntry> = if (query.isBlank()) {
                        ArrayList<LibraryEntry>(visibilityFiltered.size).also { out ->
                            for (s in visibilityFiltered) out.add(s.entry)
                        }
                    } else {
                        val lowerQuery = query.lowercase()
                        ArrayList<LibraryEntry>(visibilityFiltered.size).also { out ->
                            for (s in visibilityFiltered) if (s.sortTitle.contains(lowerQuery)) out.add(s.entry)
                        }
                    }

                    // groupBy is fine — it walks once and uses LinkedHashMap. Status enum count
                    // is tiny so the allocation is bounded.
                    val grouped = filteredEntries.groupBy { it.status }

                    // Pre-compute name → listOrder index once. The legacy comparator called
                    // `listOrder.indexOf(name)` inside compareBy, which is O(n) per probe and
                    // executes O(n log n) times during the sort → O(n²log n) total. With the
                    // map this drops to a single HashMap probe per compare.
                    val orderIndex = HashMap<String, Int>(listOrder.size * 2)
                    listOrder.forEachIndexed { i, n -> orderIndex[n] = i }
                    val sortedCustomNames = customNames.sortedWith(
                        Comparator { a, b ->
                            val ia = orderIndex[a] ?: Int.MAX_VALUE
                            val ib = orderIndex[b] ?: Int.MAX_VALUE
                            if (ia != ib) ia.compareTo(ib) else a.compareTo(b)
                        }
                    )

                    val sortedFavorites = favorites.sortedBy { it.getTitle(titleLang).lowercase() }

                    Triple(
                        filteredEntries,
                        grouped,
                        LibraryCustomData(
                            sortedCustomNames,
                            customEntriesMap,
                            sortedFavorites,
                            hiddenLists,
                            listOrder
                        )
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
                .collect { (filtered, grouped, customData) ->
                    _uiState.update {
                        it.copy(
                            entries = filtered,
                            groupedEntries = grouped,
                            customListNames = customData.customNames,
                            customListEntries = customData.customEntriesMap,
                            favoriteEntries = customData.sortedFavorites,
                            hiddenListNames = customData.hiddenListNames,
                            listOrder = customData.listOrder,
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
                is Result.Error -> _actions.emit(LibraryAction.ShowSnackbar(result.message))
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun updateProgress(mediaId: Int, delta: Int) {
        val entry = _uiState.value.entries.find { it.mediaId == mediaId } ?: return
        val newProgress = (entry.progress + delta).coerceAtLeast(0)

        viewModelScope.launch {
            when (val result = libraryRepository.updateProgress(mediaId, newProgress)) {
                is Result.Success -> {}
                is Result.Error -> _actions.emit(LibraryAction.ShowSnackbar(result.message))
            }
        }
    }

    private fun updateEntry(entry: LibraryEntry) {
        viewModelScope.launch {
            when (val result = libraryRepository.updateEntry(entry)) {
                is Result.Success -> _actions.emit(LibraryAction.ShowSnackbar("Entry updated"))
                is Result.Error -> _actions.emit(LibraryAction.ShowSnackbar(result.message))
            }
        }
    }

    private fun deleteEntry(entryId: Int, mediaId: Int) {
        viewModelScope.launch {
            when (val result = libraryRepository.deleteEntry(entryId, mediaId)) {
                is Result.Success -> _actions.emit(LibraryAction.ShowSnackbar("Entry removed"))
                is Result.Error -> _actions.emit(LibraryAction.ShowSnackbar(result.message))
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

    private fun moveList(listName: String, direction: Int) {
        val currentOrder = _uiState.value.listOrder.toMutableList()
        val allNames = _uiState.value.customListNames.toMutableList()

        // If order is empty, initialize with current custom list names
        val workingOrder =
            if (currentOrder.isEmpty()) allNames.toMutableList() else currentOrder.toMutableList()

        // Add any missing names
        allNames.forEach { if (it !in workingOrder) workingOrder.add(it) }

        val index = workingOrder.indexOf(listName)
        if (index == -1) return
        val newIndex = (index + direction).coerceIn(0, workingOrder.lastIndex)
        if (newIndex == index) return

        workingOrder.removeAt(index)
        workingOrder.add(newIndex, listName)
        
        if (_uiState.value.mediaType == com.anisync.android.type.MediaType.ANIME) {
            appSettings.setAnimeListOrder(workingOrder)
        } else {
            appSettings.setMangaListOrder(workingOrder)
        }
    }

    private fun createCustomList(listName: String, type: com.anisync.android.type.MediaType) {
        viewModelScope.launch {
            when (val result = libraryRepository.createCustomList(listName, type)) {
                is Result.Success -> {
                    _actions.emit(LibraryAction.ShowSnackbar("List '$listName' created."))
                    refresh()
                }
                is Result.Error -> _actions.emit(LibraryAction.ShowSnackbar(result.message))
            }
        }
    }

    private fun deleteCustomList(listName: String) {
        viewModelScope.launch {
            when (val result =
                libraryRepository.deleteCustomList(listName, _uiState.value.mediaType)) {
                is Result.Success -> {
                    // Remove from settings
                    val currentOrder = _uiState.value.listOrder.toMutableList()
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

                    _actions.emit(LibraryAction.ShowSnackbar("List '$listName' deleted"))
                    refresh()
                }

                is Result.Error -> _actions.emit(LibraryAction.ShowSnackbar(result.message))
            }
        }
    }
}