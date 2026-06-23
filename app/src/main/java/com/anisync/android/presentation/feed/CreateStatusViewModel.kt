package com.anisync.android.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/** UI state for the full-screen new-status composer ([CreateStatusScreen]). */
data class CreateStatusUiState(
    val isSubmitting: Boolean = false,
    val minLength: Int = ContentLimits.TextActivity.min,
    val maxLength: Int = ContentLimits.TextActivity.max
)

/** One-shot navigation events from [CreateStatusViewModel]. */
sealed interface CreateStatusEvent {
    /** The status was posted (and a refresh signalled to the feed); the screen should pop. */
    data object Posted : CreateStatusEvent
}

/**
 * Backs the full-screen [CreateStatusScreen]. Posts a new text activity via
 * [ActivityRepository.saveTextActivity], then publishes a `created` [ActivityUpdate] so the still-
 * alive feed reloads to pull the new status in (the save mutation returns no id/body, so a patch
 * isn't possible), before emitting [CreateStatusEvent.Posted].
 */
@HiltViewModel
class CreateStatusViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val activityEventBus: ActivityEventBus,
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateStatusUiState())
    val uiState: StateFlow<CreateStatusUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateStatusEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CreateStatusEvent> = _events.asSharedFlow()

    fun submit(body: String) {
        if (_uiState.value.isSubmitting) return
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = repository.saveTextActivity(trimmed)) {
                is Result.Success -> {
                    activityEventBus.publish(ActivityUpdate(id = 0, created = true))
                    _uiState.update { it.copy(isSubmitting = false) }
                    toastManager.showToast(ToastType.SUCCESS, message = "Status posted")
                    _events.tryEmit(CreateStatusEvent.Posted)
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    toastManager.showResultError(result)
                }
            }
        }
    }
}
