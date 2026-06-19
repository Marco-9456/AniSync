package com.anisync.android.presentation.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ActivityDetail
import com.anisync.android.domain.ActivityEventBus
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.ActivityUpdate
import com.anisync.android.domain.ContentLimits
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

/** UI state for the full-screen status editor ([EditActivityScreen]). */
data class EditActivityUiState(
    val isLoading: Boolean = true,
    val initialBody: String = "",
    val minLength: Int = 1,
    val maxLength: Int = ContentLimits.TextActivity.max,
    val isSubmitting: Boolean = false
)

/** One-shot navigation events from [EditActivityViewModel]. */
sealed interface EditActivityEvent {
    /** The edit was saved (and published to the activity bus); the screen should pop. */
    data object Saved : EditActivityEvent
    /** The activity couldn't be loaded; the screen should pop instead of dead-ending. */
    data object LoadFailed : EditActivityEvent
}

/**
 * Backs the full-screen [EditActivityScreen]. Loads the activity (for its current body, plus the
 * message/recipient context the save mutation needs), submits the new body via
 * [ActivityRepository.saveTextActivity] / [ActivityRepository.saveMessageActivity], then re-fetches
 * to obtain the rendered HTML and publishes a body [ActivityUpdate] so the still-alive activity
 * detail patches itself in place — no refetch there — before emitting [EditActivityEvent.Saved].
 */
@HiltViewModel
class EditActivityViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val activityEventBus: ActivityEventBus,
    private val toastManager: ToastManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val activityId: Int = checkNotNull(savedStateHandle["activityId"]) {
        "activityId is required for EditActivityViewModel"
    }

    private val _uiState = MutableStateFlow(EditActivityUiState())
    val uiState: StateFlow<EditActivityUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditActivityEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditActivityEvent> = _events.asSharedFlow()

    private var activity: ActivityDetail? = null

    init {
        loadActivity()
    }

    private fun loadActivity() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getActivity(activityId)) {
                is Result.Success -> {
                    val loaded = result.data
                    activity = loaded
                    val bounds = if (loaded.isMessage) {
                        ContentLimits.MessageActivity
                    } else {
                        ContentLimits.TextActivity
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            initialBody = loaded.bodyMarkdown ?: loaded.body,
                            minLength = bounds.min,
                            maxLength = bounds.max
                        )
                    }
                }

                is Result.Error -> {
                    toastManager.showResultError(result)
                    _events.tryEmit(EditActivityEvent.LoadFailed)
                }
            }
        }
    }

    fun submit(body: String) {
        val current = activity ?: return
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = if (current.isMessage) {
                val recipientId = current.recipientId
                if (recipientId == null) {
                    _uiState.update { it.copy(isSubmitting = false) }
                    toastManager.showToast(ToastType.VALIDATION_ERROR, message = "Missing recipient")
                    return@launch
                }
                repository.saveMessageActivity(
                    id = activityId,
                    recipientId = recipientId,
                    message = trimmed,
                    isPrivate = current.isPrivate
                )
            } else {
                repository.saveTextActivity(trimmed, id = activityId)
            }
            when (result) {
                is Result.Success -> {
                    // The save mutation returns no body, so re-fetch the rendered HTML before
                    // publishing; the open detail then patches its body in place.
                    val freshHtml = (repository.getActivity(activityId) as? Result.Success)?.data?.body
                    activityEventBus.publish(
                        ActivityUpdate(
                            id = activityId,
                            bodyHtml = freshHtml,
                            bodyMarkdown = trimmed
                        )
                    )
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.tryEmit(EditActivityEvent.Saved)
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    toastManager.showResultError(result)
                }
            }
        }
    }
}
