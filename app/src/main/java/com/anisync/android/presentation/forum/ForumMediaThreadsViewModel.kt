package com.anisync.android.presentation.forum

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.Result
import com.anisync.android.domain.ThreadEventBus
import com.anisync.android.domain.ThreadSortOption
import com.anisync.android.domain.ThreadUpdate
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForumMediaThreadsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isPaginating: Boolean = false,
    val threads: ImmutableList<ForumThread> = persistentListOf(),
    val savedThreadIds: ImmutableSet<Int> = persistentSetOf(),
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val errorMessage: String? = null
)

/**
 * Lists every forum thread that has a given media as a `mediaCategory`. Backed by
 * the same rate-limit-safe [ForumRepository.searchThreads] used by the search
 * overlay and the media-detail Discussions preview.
 */
@HiltViewModel
class ForumMediaThreadsViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val threadEventBus: ThreadEventBus,
    private val toastManager: ToastManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaId: Int = checkNotNull(savedStateHandle["mediaId"]) {
        "mediaId is required for ForumMediaThreadsViewModel"
    }

    private val _uiState = MutableStateFlow(ForumMediaThreadsUiState())
    val uiState: StateFlow<ForumMediaThreadsUiState> = _uiState.asStateFlow()

    init {
        loadSavedIds()
        load(page = 1)
        observeThreadEvents()
    }

    /** Patches this list when a thread is mutated elsewhere (detail / other lists). */
    private fun observeThreadEvents() {
        viewModelScope.launch {
            threadEventBus.events.collect { u ->
                _uiState.update { state ->
                    state.copy(
                        threads = if (u.deleted) {
                            state.threads.filterNot { it.id == u.id }.toPersistentList()
                        } else {
                            state.threads.map { if (it.id == u.id) u.applyTo(it) else it }
                                .toPersistentList()
                        },
                        savedThreadIds = when (u.isSaved) {
                            true -> (state.savedThreadIds + u.id).toPersistentSet()
                            false -> (state.savedThreadIds - u.id).toPersistentSet()
                            null -> state.savedThreadIds
                        }
                    )
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        load(page = 1, replaceExisting = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasNextPage || state.isLoading || state.isPaginating) return
        _uiState.update { it.copy(isPaginating = true) }
        load(page = state.currentPage + 1)
    }

    private fun load(page: Int, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            if (page == 1 && !_uiState.value.isRefreshing) {
                _uiState.update { it.copy(isLoading = true) }
            }
            when (val result = forumRepository.searchThreads(
                mediaCategoryId = mediaId,
                sort = ThreadSortOption.RECENTLY_REPLIED,
                page = page
            )) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        val updated = if (replaceExisting || page == 1) {
                            data.items.distinctBy { it.id }.toPersistentList()
                        } else {
                            (current.threads + data.items).distinctBy { it.id }.toPersistentList()
                        }
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            threads = updated,
                            hasNextPage = data.hasNextPage,
                            currentPage = data.currentPage,
                            errorMessage = null
                        )
                    }
                }

                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isPaginating = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private fun loadSavedIds() {
        viewModelScope.launch {
            val saved = forumRepository.getSavedThreads()
            _uiState.update { it.copy(savedThreadIds = saved.map { t -> t.id }.toPersistentSet()) }
        }
    }

    fun toggleSave(thread: ForumThread) {
        val isSaved = _uiState.value.savedThreadIds.contains(thread.id)
        _uiState.update { state ->
            val ids =
                if (isSaved) state.savedThreadIds - thread.id else state.savedThreadIds + thread.id
            state.copy(savedThreadIds = ids.toPersistentSet())
        }
        threadEventBus.publish(ThreadUpdate(thread.id, isSaved = !isSaved))
        viewModelScope.launch {
            if (isSaved) {
                forumRepository.unsaveThread(thread.id)
                toastManager.showToast(ToastType.SUCCESS, message = "Thread unsaved")
            } else {
                forumRepository.saveThread(thread)
                toastManager.showToast(ToastType.SUCCESS, message = "Thread saved")
            }
        }
    }

    fun toggleSubscribe(thread: ForumThread) {
        val wasSubscribed = thread.isSubscribed
        _uiState.update { state ->
            state.copy(
                threads = state.threads.map {
                    if (it.id == thread.id) it.copy(isSubscribed = !wasSubscribed) else it
                }.toPersistentList()
            )
        }
        threadEventBus.publish(ThreadUpdate(thread.id, isSubscribed = !wasSubscribed))
        viewModelScope.launch {
            val result = forumRepository.toggleThreadSubscription(thread.id, !wasSubscribed)
            if (result is Result.Error) {
                _uiState.update { state ->
                    state.copy(
                        threads = state.threads.map {
                            if (it.id == thread.id) it.copy(isSubscribed = wasSubscribed) else it
                        }.toPersistentList()
                    )
                }
                toastManager.showResultError(result)
            }
        }
    }
}
