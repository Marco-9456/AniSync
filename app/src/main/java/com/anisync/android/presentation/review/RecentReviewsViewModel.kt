package com.anisync.android.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchRepository
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Debounce before a picker query fires — matches the forum search pickers. */
private const val SEARCH_DEBOUNCE_MS = 350L

/** Minimum query length before a picker search fires. */
private const val MIN_SEARCH_QUERY_LENGTH = 2

@HiltViewModel
class RecentReviewsViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val searchRepository: SearchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialFilter =
        ReviewMediaFilter.fromRouteArg(savedStateHandle.get<String>("mediaType"))

    private val _uiState = MutableStateFlow(
        RecentReviewsUiState(filter = initialFilter, mediaPickerType = initialMediaPickerType(initialFilter))
    )
    val uiState: StateFlow<RecentReviewsUiState> = _uiState.asStateFlow()

    // Reviews can shift between pages as new ones are published; dedupe by id so a
    // review fetched on page 1 doesn't reappear when page 2 loads.
    private var knownIds: Set<Int> = emptySet()

    private var mediaPickerJob: Job? = null
    private var authorPickerJob: Job? = null

    init {
        loadInitial()
    }

    fun onAction(action: RecentReviewsAction) {
        when (action) {
            is RecentReviewsAction.LoadNextPage -> loadNextPage()
            is RecentReviewsAction.Retry -> loadInitial()

            is RecentReviewsAction.SetFilter -> {
                if (_uiState.value.filter == action.filter) return
                _uiState.update { it.copy(filter = action.filter) }
                reload()
            }

            is RecentReviewsAction.SetSort -> {
                if (_uiState.value.sort == action.sort) return
                _uiState.update { it.copy(sort = action.sort) }
                reload()
            }

            is RecentReviewsAction.OnMediaPickerQueryChange -> onMediaPickerQueryChange(action.query)
            is RecentReviewsAction.OnMediaPickerTypeChange -> onMediaPickerTypeChange(action.type)
            is RecentReviewsAction.SelectMedia -> {
                _uiState.update {
                    it.copy(
                        media = action.entry,
                        mediaPickerQuery = "",
                        mediaPickerResults = persistentListOf()
                    )
                }
                reload()
            }

            is RecentReviewsAction.ClearMedia -> {
                if (_uiState.value.media == null) return
                _uiState.update { it.copy(media = null) }
                reload()
            }

            is RecentReviewsAction.OnAuthorPickerQueryChange -> onAuthorPickerQueryChange(action.query)
            is RecentReviewsAction.SelectAuthor -> {
                _uiState.update {
                    it.copy(
                        author = action.user,
                        authorPickerQuery = "",
                        authorPickerResults = persistentListOf()
                    )
                }
                reload()
            }

            is RecentReviewsAction.ClearAuthor -> {
                if (_uiState.value.author == null) return
                _uiState.update { it.copy(author = null) }
                reload()
            }
        }
    }

    /** Reset the list and refetch from page 1 after a filter change. */
    private fun reload() {
        _uiState.update {
            it.copy(reviews = emptyList(), currentPage = 1, hasNextPage = true)
        }
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            knownIds = emptySet()

            when (val result = fetch(page = 1)) {
                is Result.Success -> {
                    knownIds = result.data.reviews.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            reviews = result.data.reviews,
                            hasNextPage = result.data.hasNextPage,
                            currentPage = 1,
                            isLoading = false
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun loadNextPage() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasNextPage || current.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            when (val result = fetch(page = current.currentPage + 1)) {
                is Result.Success -> {
                    val newReviews = result.data.reviews.filter { it.id !in knownIds }
                    knownIds = knownIds + newReviews.map { it.id }
                    _uiState.update {
                        it.copy(
                            reviews = it.reviews + newReviews,
                            hasNextPage = result.data.hasNextPage,
                            currentPage = current.currentPage + 1,
                            isLoadingMore = false
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    /**
     * Build the repository call from the current filter state. A specific [media]
     * filter supersedes the media-type toggle (the media already fixes the type),
     * so the type is sent as null to avoid an impossible type/media combination.
     */
    private suspend fun fetch(page: Int): Result<com.anisync.android.domain.UserReviewsPage> {
        val state = _uiState.value
        return discoverRepository.getRecentReviews(
            mediaType = if (state.media != null) null else state.filter.toMediaType(),
            mediaId = state.media?.mediaId,
            userId = state.author?.id,
            sort = state.sort,
            page = page
        )
    }

    // ---- Media filter picker ----

    private fun onMediaPickerQueryChange(query: String) {
        _uiState.update { it.copy(mediaPickerQuery = query, pickerError = null) }
        mediaPickerJob?.cancel()
        if (query.trim().length < MIN_SEARCH_QUERY_LENGTH) {
            _uiState.update {
                it.copy(mediaPickerResults = persistentListOf(), isMediaPickerSearching = false)
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
        when (
            val result = searchRepository.searchMedia(
                query = state.mediaPickerQuery,
                type = state.mediaPickerType
            )
        ) {
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
                it.copy(authorPickerResults = persistentListOf(), isAuthorPickerSearching = false)
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

    /** Seed the media picker's anime/manga toggle from the active media-type filter. */
    private fun initialMediaPickerType(filter: ReviewMediaFilter): MediaType =
        filter.toMediaType() ?: MediaType.ANIME
}
