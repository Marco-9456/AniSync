package com.anisync.android.presentation.forum

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.Result
import com.anisync.android.domain.ThreadEventBus
import com.anisync.android.domain.ThreadUpdate
import com.anisync.android.presentation.components.alert.ToastManager
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

/** UI state for the full-screen thread-body editor ([EditThreadBodyScreen]). */
data class EditThreadBodyUiState(
    val isLoading: Boolean = true,
    val initialBody: String = "",
    val isSubmitting: Boolean = false
)

/** One-shot navigation events from [EditThreadBodyViewModel]. */
sealed interface EditThreadBodyEvent {
    /** The edit was saved (and published to the thread bus); the screen should pop. */
    data object Saved : EditThreadBodyEvent
    /** The thread couldn't be loaded; the screen should pop instead of dead-ending. */
    data object LoadFailed : EditThreadBodyEvent
}

/**
 * Backs the full-screen [EditThreadBodyScreen]. Loads the thread (for its current body, plus the
 * title/categories the edit mutation must preserve), then submits the new body via
 * [ForumRepository.createThread] (AniList reuses the create mutation with an `id`). On success it
 * publishes a body [ThreadUpdate] so the still-alive thread detail patches itself in place — no
 * refetch — then emits [EditThreadBodyEvent.Saved] for the screen to pop.
 */
@HiltViewModel
class EditThreadBodyViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val threadEventBus: ThreadEventBus,
    private val toastManager: ToastManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId: Int = checkNotNull(savedStateHandle["threadId"]) {
        "threadId is required for EditThreadBodyViewModel"
    }

    private val _uiState = MutableStateFlow(EditThreadBodyUiState())
    val uiState: StateFlow<EditThreadBodyUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditThreadBodyEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditThreadBodyEvent> = _events.asSharedFlow()

    private var thread: ForumThread? = null

    init {
        loadThread()
    }

    private fun loadThread() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = forumRepository.getThread(threadId)) {
                is Result.Success -> {
                    val loaded = result.data
                    thread = loaded
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            initialBody = loaded.bodyMarkdown ?: loaded.body.orEmpty()
                        )
                    }
                }

                is Result.Error -> {
                    toastManager.showResultError(result)
                    _events.tryEmit(EditThreadBodyEvent.LoadFailed)
                }
            }
        }
    }

    fun submit(body: String) {
        val current = thread ?: return
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = forumRepository.createThread(
                title = current.title,
                body = trimmed,
                categoryIds = current.categories.map { it.id },
                id = current.id
            )
            when (result) {
                is Result.Success -> {
                    threadEventBus.publish(
                        ThreadUpdate(
                            id = current.id,
                            bodyHtml = result.data.body,
                            bodyMarkdown = trimmed
                        )
                    )
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.tryEmit(EditThreadBodyEvent.Saved)
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    toastManager.showResultError(result)
                }
            }
        }
    }
}
