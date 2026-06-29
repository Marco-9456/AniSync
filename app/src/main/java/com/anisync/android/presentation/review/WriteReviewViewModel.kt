package com.anisync.android.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ContentLimits
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.Result
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
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
class WriteReviewViewModel @Inject constructor(
    private val detailsRepository: DetailsRepository,
    private val toastManager: ToastManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaId: Int = checkNotNull(savedStateHandle["mediaId"]) {
        "Media ID is required for WriteReviewViewModel"
    }
    private val mediaTitle: String = savedStateHandle["mediaTitle"] ?: ""

    private val _uiState = MutableStateFlow(
        WriteReviewUiState(mediaId = mediaId, mediaTitle = mediaTitle)
    )
    val uiState: StateFlow<WriteReviewUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WriteReviewEvent>()
    val events: SharedFlow<WriteReviewEvent> = _events.asSharedFlow()

    init {
        loadExistingReview()
    }

    private fun loadExistingReview() {
        viewModelScope.launch {
            when (val result = detailsRepository.getViewerReview(mediaId)) {
                is Result.Success -> {
                    val review = result.data
                    if (review != null) {
                        _uiState.update {
                            it.copy(
                                existingReviewId = review.id,
                                summary = review.summary,
                                body = review.body,
                                initialBody = review.body,
                                score = review.score,
                                isPrivate = review.isPrivate,
                                initialSummary = review.summary,
                                initialScore = review.score,
                                initialIsPrivate = review.isPrivate,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                is Result.Error -> {
                    // Couldn't determine existing review — fall back to create mode.
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onAction(action: WriteReviewAction) {
        when (action) {
            is WriteReviewAction.OnBodyChange ->
                _uiState.update { it.copy(body = action.value, bodyError = null) }
            is WriteReviewAction.OnSummaryChange ->
                _uiState.update { it.copy(summary = action.value, summaryError = null) }
            is WriteReviewAction.OnScoreChange ->
                _uiState.update { it.copy(score = action.value.coerceIn(0, 100), scoreError = null) }
            is WriteReviewAction.OnPrivateChange ->
                _uiState.update { it.copy(isPrivate = action.value) }
            is WriteReviewAction.Submit -> submit()
            is WriteReviewAction.Delete -> delete()
        }
    }

    private fun submit() {
        val state = _uiState.value
        val bodyBounds = ContentLimits.ReviewBody
        val summaryBounds = ContentLimits.ReviewSummary
        val scoreBounds = ContentLimits.ReviewScore

        var hasError = false
        val summaryLen = state.summary.trim().length
        if (!summaryBounds.isValid(summaryLen)) {
            _uiState.update {
                it.copy(summaryError = "${summaryBounds.min}–${summaryBounds.max} characters")
            }
            hasError = true
        }
        if (!scoreBounds.isValid(state.score)) {
            _uiState.update { it.copy(scoreError = "0–100") }
            hasError = true
        }
        if (state.body.trim().length < bodyBounds.min) {
            _uiState.update { it.copy(bodyError = "Min ${bodyBounds.min} characters") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = detailsRepository.saveReview(
                reviewId = state.existingReviewId,
                mediaId = mediaId,
                body = state.body.trim(),
                summary = state.summary.trim(),
                score = state.score,
                private = state.isPrivate
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    toastManager.showToast(ToastType.SUCCESS, message = "Review saved")
                    _events.emit(WriteReviewEvent.Saved)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    toastManager.showResultError(result)
                }
            }
        }
    }

    private fun delete() {
        val reviewId = _uiState.value.existingReviewId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            when (val result = detailsRepository.deleteReview(reviewId, mediaId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    toastManager.showToast(ToastType.SUCCESS, message = "Review deleted")
                    _events.emit(WriteReviewEvent.Deleted)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    toastManager.showResultError(result)
                }
            }
        }
    }
}
