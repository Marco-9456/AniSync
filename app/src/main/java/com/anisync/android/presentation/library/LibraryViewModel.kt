package com.anisync.android.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Result
import com.anisync.android.type.MediaType
import com.anisync.android.util.getTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(
        val entries: List<LibraryEntry>,
        val groupedEntries: Map<LibraryStatus, List<LibraryEntry>>
    ) : LibraryUiState

    data class Error(val message: String) : LibraryUiState
}

private data class LibraryDataInputs(
    val entries: List<LibraryEntry>,
    val sort: LibrarySort,
    val ascending: Boolean,
    val query: String,
    val titleLanguage: TitleLanguage
)

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
    private val libraryRepository: LibraryRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _mediaType = MutableStateFlow(MediaType.ANIME)
    val mediaType: StateFlow<MediaType> = _mediaType.asStateFlow()

    private val _sortOption = MutableStateFlow(LibrarySort.AIRING_SOON)
    val sortOption: StateFlow<LibrarySort> = _sortOption.asStateFlow()

    val titleLanguage = appSettings.titleLanguage

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    val uiState: StateFlow<LibraryUiState> = _mediaType
        .flatMapLatest { type ->
            libraryRepository.observeLibrary("", type)
        }
        .combine(_sortOption) { entries, sort -> Pair(entries, sort) }
        .combine(_isAscending) { (entries, sort), ascending -> Triple(entries, sort, ascending) }
        .combine(_searchQuery) { (entries, sort, ascending), query ->
            Triple(entries, Triple(sort, ascending, query), null)
        }
        .combine(appSettings.titleLanguage) { (entries, params, _), titleLanguage ->
            val (sort, ascending, query) = params
            LibraryDataInputs(entries, sort, ascending, query, titleLanguage)
        }
        .onStart {
            emit(LibraryDataInputs(emptyList(), LibrarySort.TITLE, true, "", TitleLanguage.ROMAJI))
        }
        .flatMapLatest { inputs ->
            // Perform sorting, filtering, and grouping on the background thread
            flow<LibraryUiState> {
                if (inputs.entries.isEmpty()) {
                    emit(LibraryUiState.Success(emptyList(), emptyMap()))
                    return@flow
                }

                val titleLang = inputs.titleLanguage

                // Sorting
                val sortedEntries = when (inputs.sort) {
                    LibrarySort.TITLE -> inputs.entries.sortedBy {
                        it.getTitle(titleLang).lowercase()
                    }

                    LibrarySort.PROGRESS -> inputs.entries.sortedWith(
                        compareByDescending<LibraryEntry> { it.progress }
                            .thenBy { it.getTitle(titleLang).lowercase() }
                    )

                    LibrarySort.AIRING_SOON -> inputs.entries.sortedWith(
                        compareBy<LibraryEntry, Long?>(nullsLast()) { it.nextAiringEpisodeTime }
                            .thenBy { it.getTitle(titleLang).lowercase() }
                    )

                    LibrarySort.SCORE -> inputs.entries.sortedWith(
                        compareByDescending<LibraryEntry> { it.score }
                            .thenBy { it.getTitle(titleLang).lowercase() }
                    )

                    LibrarySort.LAST_UPDATED -> inputs.entries.sortedWith(
                        compareByDescending<LibraryEntry> { it.updatedAt }
                            .thenBy { it.getTitle(titleLang).lowercase() }
                    )

                    LibrarySort.LAST_ADDED -> inputs.entries.sortedWith(
                        compareByDescending<LibraryEntry> { it.createdAt }
                            .thenBy { it.getTitle(titleLang).lowercase() }
                    )

                    LibrarySort.START_DATE -> inputs.entries.sortedWith(
                        compareByDescending<LibraryEntry> { it.startedAt }
                            .thenBy { it.getTitle(titleLang).lowercase() }
                    )

                    LibrarySort.RELEASE_DATE -> inputs.entries.sortedWith(
                        compareByDescending<LibraryEntry> { it.mediaStartDate }
                            .thenBy { it.getTitle(titleLang).lowercase() }
                    )
                }

                // Direction & Filtering
                val directedEntries =
                    if (inputs.ascending) sortedEntries else sortedEntries.reversed()

                val filteredEntries = if (inputs.query.isBlank()) {
                    directedEntries
                } else {
                    directedEntries.filter {
                        it.getTitle(titleLang).contains(inputs.query, ignoreCase = true)
                    }
                }

                // Grouping
                val grouped = filteredEntries.groupBy { it.status }

                emit(LibraryUiState.Success(filteredEntries, grouped))
            }
                .flowOn(Dispatchers.Default)
        }
        .onStart { emit(LibraryUiState.Loading) }
        .catch { e -> emit(LibraryUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState.Loading
        )

    private var hasLoadedInitially = false

    fun onScreenVisible() {
        if (!hasLoadedInitially) {
            hasLoadedInitially = true
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val result = libraryRepository.refreshLibrary("", _mediaType.value)) {
                is Result.Success -> {} // Automatically updated via Flow
                is Result.Error -> _events.emit(LibraryEvent.ShowSnackbar(result.message))
            }
            _isRefreshing.value = false
        }
    }

    fun onMediaTypeChange(type: MediaType) {
        if (_mediaType.value != type) {
            _mediaType.value = type
            refresh()
        }
    }

    fun onSortOptionChange(option: LibrarySort, ascending: Boolean) {
        _sortOption.value = option
        _isAscending.value = ascending
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun incrementProgress(mediaId: Int) = updateProgress(mediaId, 1)

    fun decrementProgress(mediaId: Int) = updateProgress(mediaId, -1)

    private fun updateProgress(mediaId: Int, delta: Int) {
        val currentState = uiState.value
        if (currentState is LibraryUiState.Success) {
            val entry = currentState.entries.find { it.mediaId == mediaId } ?: return
            val newProgress = (entry.progress + delta).coerceAtLeast(0)

            viewModelScope.launch {
                when (val result = libraryRepository.updateProgress(mediaId, newProgress)) {
                    is Result.Success -> {}
                    is Result.Error -> _events.emit(LibraryEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    fun updateEntry(entry: LibraryEntry) {
        viewModelScope.launch {
            when (val result = libraryRepository.updateEntry(entry)) {
                is Result.Success -> _events.emit(LibraryEvent.ShowSnackbar("Entry updated"))
                is Result.Error -> _events.emit(LibraryEvent.ShowSnackbar(result.message))
            }
        }
    }

    fun deleteEntry(entryId: Int, mediaId: Int) {
        viewModelScope.launch {
            when (val result = libraryRepository.deleteEntry(entryId, mediaId)) {
                is Result.Success -> _events.emit(LibraryEvent.ShowSnackbar("Entry removed"))
                is Result.Error -> _events.emit(LibraryEvent.ShowSnackbar(result.message))
            }
        }
    }
}