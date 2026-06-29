package com.anisync.android.presentation.forum

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ContentLimits
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchRepository
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 300L
private const val MIN_SEARCH_QUERY_LENGTH = 2

@HiltViewModel
class CreateThreadViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val searchRepository: SearchRepository,
    private val toastManager: ToastManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateThreadUiState())
    val uiState: StateFlow<CreateThreadUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<CreateThreadAction>()
    val actions: SharedFlow<CreateThreadAction> = _actions.asSharedFlow()

    private var searchJob: Job? = null

    init {
        // Pre-attach the media passed via the CreateThread route (launched from a
        // media's discussions / the forum search media filter). mediaId == 0 means
        // the plain FAB entry point — nothing to seed.
        val mediaId = savedStateHandle.get<Int>("mediaId") ?: 0
        if (mediaId > 0) {
            val title = savedStateHandle.get<String>("mediaTitle").orEmpty()
            val cover = savedStateHandle.get<String>("mediaCoverUrl").orEmpty().ifBlank { null }
            _uiState.update {
                it.copy(
                    selectedMediaCategories = listOf(buildPrefillMedia(mediaId, title, cover)),
                    prefilledMediaCategoryIds = listOf(mediaId),
                )
            }
        }
    }

    /**
     * Minimal [LibraryEntry] for a pre-attached media. Only [LibraryEntry.mediaId]
     * is used at submit (mapped to `mediaCategories`); title/cover drive the chip.
     */
    private fun buildPrefillMedia(mediaId: Int, title: String, coverUrl: String?) = LibraryEntry(
        id = mediaId,
        mediaId = mediaId,
        titleRomaji = null,
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = title.ifBlank { "Selected media" },
        coverUrl = coverUrl,
        progress = 0,
        totalEpisodes = null,
        totalChapters = null,
        totalVolumes = null,
        type = null,
        status = LibraryStatus.UNKNOWN
    )

    fun onAction(action: CreateThreadAction) {
        when (action) {
            is CreateThreadAction.OnTitleChange -> {
                _uiState.update { it.copy(title = action.value, titleError = null) }
            }
            is CreateThreadAction.OnBodyChange -> {
                _uiState.update { it.copy(body = action.value, bodyError = null) }
            }
            is CreateThreadAction.ToggleCategory -> toggleCategory(action.categoryId)
            is CreateThreadAction.TogglePreview -> {
                _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
            }
            is CreateThreadAction.Submit -> submit()
            is CreateThreadAction.NavigateUp -> {
                viewModelScope.launch { _actions.emit(action) }
            }
            is CreateThreadAction.OnMediaSearchQueryChange -> onSearchQueryChange(action.query)
            is CreateThreadAction.OnMediaSearchTypeChange -> onSearchTypeChange(action.type)
            is CreateThreadAction.AddMediaCategory -> addMediaCategory(action.entry)
            is CreateThreadAction.RemoveMediaCategory -> _uiState.update { current ->
                current.copy(
                    selectedMediaCategories = current.selectedMediaCategories
                        .filterNot { it.mediaId == action.mediaId }
                )
            }
        }
    }

    /**
     * Toggles a forum category, enforcing AniList's rules:
     * - Selecting a standalone category (Misc / AniList Apps / Bug Reports /
     *   Site Feedback) replaces the whole selection — it can't be combined.
     *   AniList Apps additionally drops any attached media.
     * - Otherwise the selection is capped at [MAX_THREAD_CATEGORIES].
     */
    private fun toggleCategory(id: Int) {
        val current = _uiState.value
        when {
            id in current.selectedCategoryIds ->
                _uiState.update {
                    it.copy(selectedCategoryIds = it.selectedCategoryIds - id, categoryError = null)
                }
            id in exclusiveCategoryIds ->
                _uiState.update {
                    it.copy(
                        selectedCategoryIds = setOf(id),
                        selectedMediaCategories = if (id == ANILIST_APPS_CATEGORY_ID) emptyList()
                        else it.selectedMediaCategories,
                        categoryError = null
                    )
                }
            !current.canSelectMoreCategories ->
                toastManager.showToast(
                    ToastType.INFO,
                    message = "You can select up to $MAX_THREAD_CATEGORIES categories"
                )
            else ->
                _uiState.update {
                    it.copy(selectedCategoryIds = it.selectedCategoryIds + id, categoryError = null)
                }
        }
    }

    /** Attaches a media category, ignoring duplicates and capping at [MAX_THREAD_MEDIA_CATEGORIES]. */
    private fun addMediaCategory(entry: LibraryEntry) {
        val current = _uiState.value
        when {
            current.selectedMediaCategories.any { it.mediaId == entry.mediaId } -> Unit
            !current.canAttachMoreMedia ->
                toastManager.showToast(
                    ToastType.INFO,
                    message = "You can attach up to $MAX_THREAD_MEDIA_CATEGORIES media"
                )
            else ->
                _uiState.update {
                    it.copy(selectedMediaCategories = it.selectedMediaCategories + entry)
                }
        }
    }

    private fun submit() {
        val state = _uiState.value

        val titleBounds = ContentLimits.ThreadTitle
        val bodyBounds = ContentLimits.ThreadBody

        var hasError = false
        val titleLength = state.title.trim().length
        if (titleLength < titleBounds.min) {
            _uiState.update {
                it.copy(titleError = "Title must be at least ${titleBounds.min} characters")
            }
            hasError = true
        } else if (titleLength > titleBounds.max) {
            _uiState.update {
                it.copy(titleError = "Title must be at most ${titleBounds.max} characters")
            }
            hasError = true
        }
        val bodyLength = state.body.length
        if (bodyLength > bodyBounds.max) {
            _uiState.update {
                it.copy(bodyError = "Body must be at most ${bodyBounds.max} characters")
            }
            hasError = true
        }
        if (state.selectedCategoryIds.isEmpty()) {
            _uiState.update { it.copy(categoryError = "Please select at least one category") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            when (val result = forumRepository.createThread(
                title = state.title.trim(),
                body = state.body.trim(),
                categoryIds = state.selectedCategoryIds.toList(),
                mediaCategoryIds = state.selectedMediaCategories.map { it.mediaId }
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _actions.emit(CreateThreadAction.NavigateUp)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showResultError(result)
                }
            }
        }
    }

    private fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(mediaSearchQuery = query, mediaSearchError = null) }
        searchJob?.cancel()
        if (query.length < MIN_SEARCH_QUERY_LENGTH) {
            _uiState.update { it.copy(mediaSearchResults = emptyList(), isMediaSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runMediaSearch()
        }
    }

    private fun onSearchTypeChange(type: com.anisync.android.type.MediaType) {
        if (type == _uiState.value.mediaSearchType) return
        _uiState.update { it.copy(mediaSearchType = type, mediaSearchResults = emptyList()) }
        searchJob?.cancel()
        if (_uiState.value.mediaSearchQuery.length >= MIN_SEARCH_QUERY_LENGTH) {
            searchJob = viewModelScope.launch { runMediaSearch() }
        }
    }

    private suspend fun runMediaSearch() {
        val state = _uiState.value
        _uiState.update { it.copy(isMediaSearching = true) }
        when (val result = searchRepository.searchMedia(
            query = state.mediaSearchQuery,
            type = state.mediaSearchType
        )) {
            is Result.Success -> _uiState.update {
                it.copy(mediaSearchResults = result.data.entries, isMediaSearching = false)
            }
            is Result.Error -> _uiState.update {
                it.copy(
                    isMediaSearching = false,
                    mediaSearchError = result.message,
                    mediaSearchResults = emptyList()
                )
            }
        }
    }

    private fun showResultError(result: Result.Error) {
        toastManager.showResultError(result)
    }
}
