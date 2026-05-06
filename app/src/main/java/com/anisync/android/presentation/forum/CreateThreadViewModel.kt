package com.anisync.android.presentation.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumRepository
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
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateThreadUiState())
    val uiState: StateFlow<CreateThreadUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<CreateThreadAction>()
    val actions: SharedFlow<CreateThreadAction> = _actions.asSharedFlow()

    private var searchJob: Job? = null

    fun onAction(action: CreateThreadAction) {
        when (action) {
            is CreateThreadAction.OnTitleChange -> {
                _uiState.update { it.copy(title = action.value, titleError = null) }
            }
            is CreateThreadAction.OnBodyChange -> {
                _uiState.update { it.copy(body = action.value, bodyError = null) }
            }
            is CreateThreadAction.ToggleCategory -> {
                _uiState.update { current ->
                    val updated = if (action.categoryId in current.selectedCategoryIds) {
                        current.selectedCategoryIds - action.categoryId
                    } else {
                        current.selectedCategoryIds + action.categoryId
                    }
                    current.copy(selectedCategoryIds = updated, categoryError = null)
                }
            }
            is CreateThreadAction.TogglePreview -> {
                _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
            }
            is CreateThreadAction.Submit -> submit()
            is CreateThreadAction.NavigateUp -> {
                viewModelScope.launch { _actions.emit(action) }
            }
            is CreateThreadAction.OnMediaSearchQueryChange -> onSearchQueryChange(action.query)
            is CreateThreadAction.OnMediaSearchTypeChange -> onSearchTypeChange(action.type)
        }
    }

    private fun submit() {
        val state = _uiState.value

        var hasError = false
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title is required") }
            hasError = true
        } else if (state.title.length > 255) {
            _uiState.update { it.copy(titleError = "Title must be under 255 characters") }
            hasError = true
        }
        if (state.body.isBlank()) {
            _uiState.update { it.copy(bodyError = "Body is required") }
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
                categoryIds = state.selectedCategoryIds.toList()
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
                it.copy(mediaSearchResults = result.data, isMediaSearching = false)
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
        if (result.code != null) {
            toastManager.showToast(result.code, result.message)
        } else {
            toastManager.showToast(ToastType.INFO, message = result.message)
        }
    }
}
