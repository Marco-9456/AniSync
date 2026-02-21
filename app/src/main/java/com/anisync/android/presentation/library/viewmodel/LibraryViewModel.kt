package com.anisync.android.presentation.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.Result
import com.anisync.android.presentation.library.state.LibraryEvent
import com.anisync.android.presentation.library.state.LibrarySort
import com.anisync.android.presentation.library.state.LibraryUiState
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
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    private var hasLoadedInitially = false

    init {
        appSettings.titleLanguage.onEach { lang ->
            _uiState.update { it.copy(titleLanguage = lang) }
        }.launchIn(viewModelScope)

        observeLibraryData()
    }

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.OnScreenVisible -> onScreenVisible()
            is LibraryEvent.Refresh -> refresh()
            is LibraryEvent.OnMediaTypeChange -> {
                _uiState.update { it.copy(mediaType = event.type, isLoading = true, errorMessage = null) }
                refresh()
            }
            is LibraryEvent.OnSortOptionChange -> {
                _uiState.update { it.copy(sortOption = event.sort, isAscending = event.ascending) }
            }
            is LibraryEvent.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is LibraryEvent.IncrementProgress -> updateProgress(event.mediaId, 1)
            is LibraryEvent.DecrementProgress -> updateProgress(event.mediaId, -1)
            is LibraryEvent.UpdateEntry -> updateEntry(event.entry)
            is LibraryEvent.DeleteEntry -> deleteEntry(event.entryId, event.mediaId)
            is LibraryEvent.ShowSnackbar -> {
                viewModelScope.launch { _events.emit(LibraryEvent.ShowSnackbar(event.message)) }
            }
        }
    }

    private fun observeLibraryData() {
        viewModelScope.launch {
            _uiState
                .map { it.mediaType }
                .distinctUntilChanged()
                .flatMapLatest { type ->
                    libraryRepository.observeLibrary("", type)
                }
                .combine(
                    _uiState.map { state ->
                        Triple(
                            state.sortOption to state.isAscending,
                            state.searchQuery,
                            state.titleLanguage
                        )
                    }.distinctUntilChanged()
                ) { entries, (sortParams, query, titleLang) ->
                    val (sort, ascending) = sortParams

                    if (entries.isEmpty()) {
                        return@combine Pair(emptyList<LibraryEntry>(), emptyMap())
                    }

                    // Sorting
                    val sortedEntries = when (sort) {
                        LibrarySort.TITLE -> entries.sortedBy {
                            it.getTitle(titleLang).lowercase()
                        }
                        LibrarySort.PROGRESS -> entries.sortedWith(
                            compareByDescending<LibraryEntry> { it.progress }
                                .thenBy { it.getTitle(titleLang).lowercase() }
                        )
                        LibrarySort.AIRING_SOON -> entries.sortedWith(
                            compareBy<LibraryEntry, Long?>(nullsLast()) { it.nextAiringEpisodeTime }
                                .thenBy { it.getTitle(titleLang).lowercase() }
                        )
                        LibrarySort.SCORE -> entries.sortedWith(
                            compareByDescending<LibraryEntry> { it.score }
                                .thenBy { it.getTitle(titleLang).lowercase() }
                        )
                        LibrarySort.LAST_UPDATED -> entries.sortedWith(
                            compareByDescending<LibraryEntry> { it.updatedAt }
                                .thenBy { it.getTitle(titleLang).lowercase() }
                        )
                        LibrarySort.LAST_ADDED -> entries.sortedWith(
                            compareByDescending<LibraryEntry> { it.createdAt }
                                .thenBy { it.getTitle(titleLang).lowercase() }
                        )
                        LibrarySort.START_DATE -> entries.sortedWith(
                            compareByDescending<LibraryEntry> { it.startedAt }
                                .thenBy { it.getTitle(titleLang).lowercase() }
                        )
                        LibrarySort.RELEASE_DATE -> entries.sortedWith(
                            compareByDescending<LibraryEntry> { it.mediaStartDate }
                                .thenBy { it.getTitle(titleLang).lowercase() }
                        )
                    }

                    // Direction & Filtering
                    val directedEntries = if (ascending) sortedEntries else sortedEntries.reversed()
                    val filteredEntries = if (query.isBlank()) {
                        directedEntries
                    } else {
                        directedEntries.filter {
                            it.getTitle(titleLang).contains(query, ignoreCase = true)
                        }
                    }

                    // Grouping
                    val grouped = filteredEntries.groupBy { it.status }

                    Pair(filteredEntries, grouped)
                }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error") }
                }
                .collect { (filtered, grouped) ->
                    _uiState.update {
                        it.copy(
                            entries = filtered,
                            groupedEntries = grouped,
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
                is Result.Error -> _events.emit(LibraryEvent.ShowSnackbar(result.message))
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
                is Result.Error -> _events.emit(LibraryEvent.ShowSnackbar(result.message))
            }
        }
    }

    private fun updateEntry(entry: LibraryEntry) {
        viewModelScope.launch {
            when (val result = libraryRepository.updateEntry(entry)) {
                is Result.Success -> _events.emit(LibraryEvent.ShowSnackbar("Entry updated"))
                is Result.Error -> _events.emit(LibraryEvent.ShowSnackbar(result.message))
            }
        }
    }

    private fun deleteEntry(entryId: Int, mediaId: Int) {
        viewModelScope.launch {
            when (val result = libraryRepository.deleteEntry(entryId, mediaId)) {
                is Result.Success -> _events.emit(LibraryEvent.ShowSnackbar("Entry removed"))
                is Result.Error -> _events.emit(LibraryEvent.ShowSnackbar(result.message))
            }
        }
    }
}