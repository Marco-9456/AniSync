package com.anisync.android.presentation.library

import android.util.Log
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
import kotlin.system.measureTimeMillis

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
                        Triple(
                            state.sortOption to state.isAscending,
                            state.searchQuery,
                            state.titleLanguage
                        )
                    }.distinctUntilChanged()
                ) { (entries, favorites, listPrefs), (sortParams, query, titleLang) ->
                    val (sort, ascending) = sortParams
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

                    var pipelineResult: Triple<List<LibraryEntry>, Map<LibraryStatus, List<LibraryEntry>>, LibraryCustomData>? =
                        null

                    // OPTIMIZATION: Logging performance metric
                    val timeTaken = measureTimeMillis {

                        // OPTIMIZATION: Pre-calculate localized and lowercase titles to avoid O(N log N) redundant computations.
                        // Generating strings inside `.thenBy{}` is highly CPU intensive. Caching it upfront fixes the lag spike.
                        class SortableEntry(val entry: LibraryEntry, val sortTitle: String)

                        val sortableEntries =
                            entries.map { SortableEntry(it, it.getTitle(titleLang).lowercase()) }

                        // Sorting
                        val sortedEntries = when (sort) {
                            LibrarySort.TITLE -> sortableEntries.sortedBy { it.sortTitle }
                            LibrarySort.PROGRESS -> sortableEntries.sortedWith(
                                compareByDescending<SortableEntry> { it.entry.progress }
                                    .thenBy { it.sortTitle }
                            )

                            LibrarySort.AIRING_SOON -> sortableEntries.sortedWith(
                                compareBy<SortableEntry, Long?>(nullsLast()) { it.entry.nextAiringEpisodeTime }
                                    .thenBy { it.sortTitle }
                            )

                            LibrarySort.SCORE -> sortableEntries.sortedWith(
                                compareByDescending<SortableEntry> { it.entry.score }
                                    .thenBy { it.sortTitle }
                            )

                            LibrarySort.LAST_UPDATED -> sortableEntries.sortedWith(
                                compareByDescending<SortableEntry> { it.entry.updatedAt }
                                    .thenBy { it.sortTitle }
                            )

                            LibrarySort.LAST_ADDED -> sortableEntries.sortedWith(
                                compareByDescending<SortableEntry> { it.entry.createdAt }
                                    .thenBy { it.sortTitle }
                            )

                            LibrarySort.START_DATE -> sortableEntries.sortedWith(
                                compareByDescending<SortableEntry> { it.entry.startedAt }
                                    .thenBy { it.sortTitle }
                            )

                            LibrarySort.RELEASE_DATE -> sortableEntries.sortedWith(
                                compareByDescending<SortableEntry> { it.entry.mediaStartDate }
                                    .thenBy { it.sortTitle }
                            )
                        }

                        // Direction & Filtering
                        val directedEntries =
                            if (ascending) sortedEntries else sortedEntries.reversed()
                        val filteredEntries = if (query.isBlank()) {
                            directedEntries.map { it.entry }
                        } else {
                            val lowerQuery = query.lowercase()
                            directedEntries.filter { it.sortTitle.contains(lowerQuery) }
                                .map { it.entry }
                        }

                        // Grouping
                        val grouped = filteredEntries.groupBy { it.status }

                        // Custom Lists
                        val customNames = mutableSetOf<String>()
                        val customEntriesMap = mutableMapOf<String, MutableList<LibraryEntry>>()

                        filteredEntries.forEach { entry ->
                            entry.customLists.forEach { customListName ->
                                customNames.add(customListName)
                                customEntriesMap.getOrPut(customListName) { mutableListOf() }
                                    .add(entry)
                            }
                        }

                        // Include all defined custom list names from settings (listOrder)
                        // so that empty lists still appear as tabs and in the edit sheet
                        customNames.addAll(listOrder)

                        // Sort custom names according to listOrder, with new lists at the end alphabetically
                        val sortedCustomNames = customNames.toList().sortedWith(
                            compareBy<String> { name ->
                                val index = listOrder.indexOf(name)
                                if (index == -1) Int.MAX_VALUE else index
                            }.thenBy { it }
                        )

                        // Sort favorites (using existing logic since favorites list is typically small)
                        val sortedFavorites =
                            favorites.sortedBy { it.getTitle(titleLang).lowercase() }

                        pipelineResult = Triple(
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

                    Log.d("Performance", "Library sorting and filtering took ${timeTaken}ms")
                    pipelineResult!!
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