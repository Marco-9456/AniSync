package com.anisync.android.presentation.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateThreadViewModel @Inject constructor(
    private val forumRepository: ForumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateThreadUiState())
    val uiState: StateFlow<CreateThreadUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<CreateThreadAction>()
    val actions: SharedFlow<CreateThreadAction> = _actions.asSharedFlow()

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
            is CreateThreadAction.ShowSnackbar -> {
                viewModelScope.launch { _actions.emit(action) }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value

        // Validate
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
                    _actions.emit(CreateThreadAction.ShowSnackbar(result.message))
                }
            }
        }
    }
}
