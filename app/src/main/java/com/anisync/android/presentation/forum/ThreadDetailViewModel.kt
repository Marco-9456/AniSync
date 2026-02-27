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
class ThreadDetailViewModel @Inject constructor(
    private val forumRepository: ForumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThreadDetailUiState())
    val uiState: StateFlow<ThreadDetailUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<ThreadDetailAction>()
    val actions: SharedFlow<ThreadDetailAction> = _actions.asSharedFlow()

    fun onAction(action: ThreadDetailAction) {
        when (action) {
            is ThreadDetailAction.Load -> load(action.threadId)
            is ThreadDetailAction.LoadMoreComments -> loadMoreComments()
            is ThreadDetailAction.ToggleLike -> toggleLike(action)
            is ThreadDetailAction.OpenReply -> {
                _uiState.update {
                    it.copy(
                        isReplySheetVisible = true,
                        replyTargetCommentId = action.parentCommentId,
                        replyTargetAuthorName = action.parentAuthorName
                    )
                }
            }
            is ThreadDetailAction.CloseReply -> {
                _uiState.update { it.copy(isReplySheetVisible = false, replyTargetCommentId = null) }
            }
            is ThreadDetailAction.SubmitReply -> submitReply(action)
            is ThreadDetailAction.ShowSnackbar -> viewModelScope.launch { _actions.emit(action) }
            else -> viewModelScope.launch { _actions.emit(action) }
        }
    }

    private fun load(threadId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load thread and first page of comments in parallel
            val threadResult = forumRepository.getThread(threadId)
            val commentsResult = forumRepository.getComments(threadId, page = 1)

            if (threadResult is Result.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = threadResult.message) }
                return@launch
            }

            val thread = (threadResult as Result.Success).data
            val commentsData = (commentsResult as? Result.Success)?.data

            _uiState.update {
                it.copy(
                    isLoading = false,
                    thread = thread,
                    comments = commentsData?.items ?: emptyList(),
                    hasMoreComments = commentsData?.hasNextPage ?: false,
                    currentCommentPage = 1,
                    errorMessage = null
                )
            }
        }
    }

    private fun loadMoreComments() {
        val threadId = _uiState.value.thread?.id ?: return
        if (!_uiState.value.hasMoreComments || _uiState.value.isLoadingMoreComments) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreComments = true) }
            val nextPage = _uiState.value.currentCommentPage + 1

            when (val result = forumRepository.getComments(threadId, nextPage)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoadingMoreComments = false,
                            comments = current.comments + result.data.items,
                            hasMoreComments = result.data.hasNextPage,
                            currentCommentPage = nextPage
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingMoreComments = false) }
                    _actions.emit(ThreadDetailAction.ShowSnackbar(result.message))
                }
            }
        }
    }

    private fun toggleLike(action: ThreadDetailAction.ToggleLike) {
        // Optimistic update
        _uiState.update { state ->
            if (action.isThread) {
                val thread = state.thread ?: return@update state
                val newLikeCount = if (action.currentLiked) thread.likeCount - 1 else thread.likeCount + 1
                state.copy(thread = thread.copy(isLiked = !action.currentLiked, likeCount = newLikeCount.coerceAtLeast(0)))
            } else {
                val updatedComments = state.comments.map { comment ->
                    if (comment.id == action.id) {
                        val newCount = if (action.currentLiked) comment.likeCount - 1 else comment.likeCount + 1
                        comment.copy(isLiked = !action.currentLiked, likeCount = newCount.coerceAtLeast(0))
                    } else comment
                }
                state.copy(comments = updatedComments)
            }
        }

        // Actual API call
        viewModelScope.launch {
            val result = if (action.isThread) {
                forumRepository.toggleLikeThread(action.id)
            } else {
                forumRepository.toggleLikeComment(action.id)
            }
            if (result is Result.Error) {
                // Revert optimistic update
                _uiState.update { state ->
                    if (action.isThread) {
                        val thread = state.thread ?: return@update state
                        val revertedCount = if (action.currentLiked) thread.likeCount + 1 else thread.likeCount - 1
                        state.copy(thread = thread.copy(isLiked = action.currentLiked, likeCount = revertedCount.coerceAtLeast(0)))
                    } else {
                        val reverted = state.comments.map { comment ->
                            if (comment.id == action.id) {
                                val revertedCount = if (action.currentLiked) comment.likeCount + 1 else comment.likeCount - 1
                                comment.copy(isLiked = action.currentLiked, likeCount = revertedCount.coerceAtLeast(0))
                            } else comment
                        }
                        state.copy(comments = reverted)
                    }
                }
                _actions.emit(ThreadDetailAction.ShowSnackbar(result.message))
            }
        }
    }

    private fun submitReply(action: ThreadDetailAction.SubmitReply) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReply = true) }
            val parentId = _uiState.value.replyTargetCommentId

            val result = if (parentId != null) {
                forumRepository.replyToComment(action.threadId, action.body, parentId)
            } else {
                forumRepository.createComment(action.threadId, action.body)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            isSubmittingReply = false,
                            isReplySheetVisible = false,
                            replyTargetCommentId = null,
                            comments = current.comments + result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSubmittingReply = false) }
                    _actions.emit(ThreadDetailAction.ShowSnackbar(result.message))
                }
            }
        }
    }
}
