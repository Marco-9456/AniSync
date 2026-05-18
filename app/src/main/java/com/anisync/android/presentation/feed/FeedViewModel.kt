package com.anisync.android.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.FeedRepository
import com.anisync.android.domain.FeedScope
import com.anisync.android.domain.Result
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 25

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val activityRepository: ActivityRepository,
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _actions = Channel<FeedAction>(Channel.BUFFERED)
    val actions: Flow<FeedAction> = _actions.receiveAsFlow()

    private var hasLoadedInitially = false
    private var loadJob: Job? = null

    fun onScreenVisible() {
        if (!hasLoadedInitially) {
            hasLoadedInitially = true
            load(page = 1)
            viewModelScope.launch {
                val viewerId = activityRepository.getViewerId()
                if (viewerId != null) {
                    _uiState.update { it.copy(viewerId = viewerId) }
                }
            }
        }
    }

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.Refresh -> {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                load(page = 1, replaceExisting = true)
            }

            is FeedAction.LoadMore -> {
                val s = _uiState.value
                if (!s.hasNextPage || s.isLoading || s.isPaginating) return
                _uiState.update { it.copy(isPaginating = true) }
                load(page = s.currentPage + 1)
            }

            is FeedAction.OnFilterChange -> {
                if (_uiState.value.filter == action.filter) return
                _uiState.update {
                    it.copy(
                        filter = action.filter,
                        items = persistentListOf(),
                        hasNextPage = false,
                        currentPage = 1,
                        isLoading = true,
                        isRefreshing = false,
                        isPaginating = false,
                        errorMessage = null
                    )
                }
                load(page = 1, replaceExisting = true)
            }

            is FeedAction.OnScopeChange -> {
                if (_uiState.value.scope == action.scope) return
                _uiState.update {
                    it.copy(
                        scope = action.scope,
                        items = persistentListOf(),
                        hasNextPage = false,
                        currentPage = 1,
                        isLoading = true,
                        isRefreshing = false,
                        isPaginating = false,
                        errorMessage = null
                    )
                }
                load(page = 1, replaceExisting = true)
            }

            is FeedAction.OnMediaTypeChange -> {
                if (_uiState.value.mediaType == action.mediaType) return
                _uiState.update {
                    it.copy(
                        mediaType = action.mediaType,
                        items = persistentListOf(),
                        hasNextPage = false,
                        currentPage = 1,
                        isLoading = true,
                        isRefreshing = false,
                        isPaginating = false,
                        errorMessage = null
                    )
                }
                load(page = 1, replaceExisting = true)
            }

            is FeedAction.ToggleSubscribe -> toggleSubscribe(action.activityId)

            is FeedAction.ToggleLike -> toggleLike(action.activityId)

            is FeedAction.DeleteActivity -> deleteActivity(action.activityId)

            is FeedAction.OpenCompose -> {
                _uiState.update { it.copy(isComposeSheetVisible = true, composeError = null) }
            }

            is FeedAction.DismissCompose -> {
                if (_uiState.value.isPostingStatus) return
                _uiState.update {
                    it.copy(isComposeSheetVisible = false, composeError = null)
                }
            }

            is FeedAction.PostStatus -> postStatus(action.text)

            is FeedAction.EditActivity -> {
                val target = _uiState.value.items.firstOrNull { it.id == action.activityId } ?: return
                _uiState.update { it.copy(editingActivity = target) }
            }

            is FeedAction.DismissEdit -> {
                if (_uiState.value.isSavingEdit) return
                _uiState.update { it.copy(editingActivity = null) }
            }

            is FeedAction.SubmitEdit -> submitEdit(action.text)
        }
    }

    private fun submitEdit(text: String) {
        val target = _uiState.value.editingActivity ?: return
        if (_uiState.value.isSavingEdit) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        _uiState.update { it.copy(isSavingEdit = true) }
        viewModelScope.launch {
            val result = when (target.type) {
                ActivityType.TEXT ->
                    activityRepository.saveTextActivity(trimmed, id = target.id)
                ActivityType.MESSAGE -> {
                    val recipientId = target.recipientId
                    if (recipientId == null) {
                        Result.Error("Missing recipient")
                    } else {
                        activityRepository.saveMessageActivity(
                            id = target.id,
                            recipientId = recipientId,
                            message = trimmed,
                            isPrivate = target.isPrivate
                        )
                    }
                }
                else -> Result.Error("Cannot edit this activity type")
            }
            when (result) {
                is Result.Success -> {
                    toastManager.showToast(ToastType.SUCCESS, message = "Activity updated")
                    _uiState.update { it.copy(isSavingEdit = false, editingActivity = null) }
                    load(page = 1, replaceExisting = true)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSavingEdit = false) }
                    showResultError(result)
                }
            }
        }
    }

    private fun postStatus(text: String) {
        if (_uiState.value.isPostingStatus) return
        _uiState.update { it.copy(isPostingStatus = true, composeError = null) }
        viewModelScope.launch {
            when (val result = activityRepository.saveTextActivity(text)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isPostingStatus = false,
                            isComposeSheetVisible = false,
                            composeError = null
                        )
                    }
                    toastManager.showToast(ToastType.SUCCESS, message = "Status posted")
                    load(page = 1, replaceExisting = true)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isPostingStatus = false, composeError = result.message)
                    }
                    showResultError(result)
                }
            }
        }
    }

    private fun toggleLike(activityId: Int) {
        val current = _uiState.value.items.firstOrNull { it.id == activityId } ?: return
        if (_uiState.value.pendingLikeIds.contains(activityId)) return
        val wasLiked = current.isLiked

        _uiState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.id == activityId) {
                        it.copy(
                            isLiked = !wasLiked,
                            likeCount = (it.likeCount + if (wasLiked) -1 else 1).coerceAtLeast(0)
                        )
                    } else it
                }.toPersistentList(),
                pendingLikeIds = (state.pendingLikeIds + activityId).toPersistentSet()
            )
        }

        viewModelScope.launch {
            val result = activityRepository.toggleActivityLike(activityId)
            _uiState.update { state ->
                val pending = (state.pendingLikeIds - activityId).toPersistentSet()
                when (result) {
                    is Result.Success -> {
                        val server = result.data
                        state.copy(
                            items = state.items.map {
                                if (it.id == activityId) {
                                    it.copy(isLiked = server.isLiked, likeCount = server.likeCount)
                                } else it
                            }.toPersistentList(),
                            pendingLikeIds = pending
                        )
                    }
                    is Result.Error -> state.copy(
                        items = state.items.map {
                            if (it.id == activityId) {
                                it.copy(
                                    isLiked = wasLiked,
                                    likeCount = current.likeCount
                                )
                            } else it
                        }.toPersistentList(),
                        pendingLikeIds = pending
                    )
                }
            }
            if (result is Result.Error) {
                showResultError(result)
            }
        }
    }

    private fun deleteActivity(activityId: Int) {
        if (_uiState.value.pendingDeleteIds.contains(activityId)) return
        val snapshot = _uiState.value.items
        val target = snapshot.firstOrNull { it.id == activityId } ?: return

        _uiState.update { state ->
            state.copy(
                items = state.items.filterNot { it.id == activityId }.toPersistentList(),
                pendingDeleteIds = (state.pendingDeleteIds + activityId).toPersistentSet()
            )
        }

        viewModelScope.launch {
            val result = activityRepository.deleteActivity(activityId)
            _uiState.update { state ->
                state.copy(pendingDeleteIds = (state.pendingDeleteIds - activityId).toPersistentSet())
            }
            when (result) {
                is Result.Success -> toastManager.showToast(ToastType.SUCCESS, message = "Activity deleted")
                is Result.Error -> {
                    // Restore the activity at its previous index
                    _uiState.update { state ->
                        val restored = state.items.toMutableList()
                        val originalIndex = snapshot.indexOfFirst { it.id == activityId }
                        if (originalIndex >= 0 && restored.none { it.id == activityId }) {
                            restored.add(originalIndex.coerceAtMost(restored.size), target)
                        }
                        state.copy(items = restored.toPersistentList())
                    }
                    showResultError(result)
                }
            }
        }
    }

    private fun toggleSubscribe(activityId: Int) {
        val current = _uiState.value.items.firstOrNull { it.id == activityId } ?: return
        val wasSubscribed = current.isSubscribed

        _uiState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.id == activityId) it.copy(isSubscribed = !wasSubscribed) else it
                }.toPersistentList()
            )
        }

        viewModelScope.launch {
            val result = activityRepository.toggleSubscription(activityId, !wasSubscribed)
            if (result is Result.Error) {
                _uiState.update { state ->
                    state.copy(
                        items = state.items.map {
                            if (it.id == activityId) it.copy(isSubscribed = wasSubscribed) else it
                        }.toPersistentList()
                    )
                }
                showResultError(result)
            }
        }
    }

    private fun load(page: Int, replaceExisting: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (page == 1 && !_uiState.value.isRefreshing && !_uiState.value.isLoading) {
                _uiState.update { it.copy(isLoading = true) }
            }

            val state = _uiState.value

            if (state.scope == FeedScope.FOLLOWING) {
                val viewerId = activityRepository.getViewerId()
                if (viewerId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            isAuthenticated = false,
                            items = persistentListOf(),
                            hasNextPage = false,
                            currentPage = 1,
                            errorMessage = null
                        )
                    }
                    return@launch
                }
            }

            when (val result = feedRepository.getFeed(
                page = page,
                perPage = PAGE_SIZE,
                filter = state.filter,
                scope = state.scope,
                mediaType = state.mediaType
            )) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        val merged = if (replaceExisting || page == 1) {
                            data.items
                        } else {
                            (current.items + data.items).distinctBy { it.id }
                        }
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            isAuthenticated = true,
                            items = merged.toPersistentList(),
                            hasNextPage = data.hasNextPage,
                            currentPage = data.currentPage,
                            errorMessage = null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
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
    }

    private fun showResultError(result: Result.Error) {
        toastManager.showResultError(result)
    }
}
