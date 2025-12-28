package com.anisync.android.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.Result
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val entries: List<LibraryEntry>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

sealed interface LibraryEvent {
    data class ShowSnackbar(val message: String) : LibraryEvent
}

enum class LibrarySort {
    TITLE,
    PROGRESS,
    AIRING_SOON,
    SCORE,
    LAST_UPDATED,
    LAST_ADDED,
    START_DATE,
    RELEASE_DATE
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    // MediaType State (Default: Anime)
    private val _mediaType = MutableStateFlow(MediaType.ANIME)
    val mediaType: StateFlow<MediaType> = _mediaType.asStateFlow()

    // Sort Option State (Default: Title)
    private val _sortOption = MutableStateFlow(LibrarySort.TITLE)
    val sortOption: StateFlow<LibrarySort> = _sortOption.asStateFlow()

    // Sort Direction State (Default: Ascending)
    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Search query for local library filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    /**
     * Observe library from Room via Flow.
     * Automatically switches when mediaType changes and re-sorts when sortOption changes.
     */
    val uiState: StateFlow<LibraryUiState> = _mediaType
        .flatMapLatest { type ->
            libraryRepository.observeLibrary("", type)
        }
        .combine(_sortOption) { entries, sort ->
            Pair(entries, sort)
        }
        .combine(_isAscending) { (entries, sort), ascending ->
            Triple(entries, sort, ascending)
        }
        .combine(_searchQuery) { (entries, sort, ascending), query ->
            val baseSortedEntries = when (sort) {
                LibrarySort.TITLE -> entries.sortedBy { it.title.lowercase() }
                LibrarySort.PROGRESS -> entries.sortedWith(
                    compareByDescending<LibraryEntry> { it.progress }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySort.AIRING_SOON -> entries.sortedWith(
                    compareBy<LibraryEntry, Int?>(nullsLast()) { it.timeUntilAiring }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySort.SCORE -> entries.sortedWith(
                    compareByDescending<LibraryEntry> { it.score }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySort.LAST_UPDATED -> entries.sortedWith(
                    compareByDescending<LibraryEntry> { it.updatedAt }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySort.LAST_ADDED -> entries.sortedWith(
                    compareByDescending<LibraryEntry> { it.createdAt }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySort.START_DATE -> entries.sortedWith(
                    compareByDescending<LibraryEntry> { it.startedAt }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySort.RELEASE_DATE -> entries.sortedWith(
                    compareByDescending<LibraryEntry> { it.mediaStartDate }
                        .thenBy { it.title.lowercase() }
                )
            }
            // Apply direction: if ascending, keep sorted as is; if descending, reverse
            val sortedEntries = if (ascending) baseSortedEntries else baseSortedEntries.reversed()
            // Filter by search query if present
            val filteredEntries = if (query.isBlank()) {
                sortedEntries
            } else {
                sortedEntries.filter { it.title.contains(query, ignoreCase = true) }
            }
            LibraryUiState.Success(filteredEntries) as LibraryUiState
        }
        .onStart { emit(LibraryUiState.Loading) }
        .catch { e -> emit(LibraryUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState.Loading
        )

    init {
        // Trigger initial refresh from network
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val result = libraryRepository.refreshLibrary("", _mediaType.value)) {
                is Result.Success -> {
                    // DB updated, Flow emits automatically
                }
                is Result.Error -> {
                    _events.emit(LibraryEvent.ShowSnackbar(result.message))
                }
            }
            _isRefreshing.value = false
        }
    }
    
    fun onMediaTypeChange(type: MediaType) {
        if (_mediaType.value != type) {
            _mediaType.value = type
            refresh() // Refresh from network for new type
        }
    }

    fun onSortOptionChange(option: LibrarySort, ascending: Boolean) {
        _sortOption.value = option
        _isAscending.value = ascending
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun incrementProgress(mediaId: Int) {
        updateProgress(mediaId, 1)
    }

    fun decrementProgress(mediaId: Int) {
        updateProgress(mediaId, -1)
    }

    private fun updateProgress(mediaId: Int, delta: Int) {
        val currentState = uiState.value
        if (currentState is LibraryUiState.Success) {
            val entry = currentState.entries.find { it.mediaId == mediaId } ?: return
            val newProgress = (entry.progress + delta).coerceAtLeast(0)

            viewModelScope.launch {
                when (val result = libraryRepository.updateProgress(mediaId, newProgress)) {
                    is Result.Success -> {
                        // Local DB updated, Flow emits automatically
                    }
                    is Result.Error -> {
                        _events.emit(LibraryEvent.ShowSnackbar(result.message))
                    }
                }
            }
        }
    }

    fun updateEntry(entry: LibraryEntry) {
        viewModelScope.launch {
            when (val result = libraryRepository.updateEntry(entry)) {
                is Result.Success -> {
                    _events.emit(LibraryEvent.ShowSnackbar("Entry updated"))
                }
                is Result.Error -> {
                    _events.emit(LibraryEvent.ShowSnackbar(result.message))
                }
            }
        }
    }
}

