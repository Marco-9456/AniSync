package com.anisync.android.presentation.forum

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumComment
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.parser.ParserConfig
import com.anisync.android.domain.parser.RichTextParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                _uiState.update {
                    it.copy(
                        isReplySheetVisible = false,
                        replyTargetCommentId = null
                    )
                }
            }

            is ThreadDetailAction.SubmitReply -> submitReply(action)
            is ThreadDetailAction.ToggleSave -> toggleSave()
            is ThreadDetailAction.ToggleSubscribe -> toggleSubscribe()
            is ThreadDetailAction.ChangeCommentSort -> changeCommentSort(action)
            else -> {}
        }
    }

    private fun load(threadId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val sort = _uiState.value.commentSort

            val threadDeferred = async { forumRepository.getThread(threadId) }
            val commentsDeferred =
                async { forumRepository.getComments(threadId, page = 1, sort = sort) }
            val savedDeferred = async { forumRepository.isThreadSaved(threadId) }

            val threadResult = threadDeferred.await()
            val commentsResult = commentsDeferred.await()
            val isSaved = savedDeferred.await()

            if (threadResult is Result.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = threadResult.message) }
                return@launch
            }

            val thread = (threadResult as Result.Success).data
            val commentsData = (commentsResult as? Result.Success)?.data

            val parsedBody = thread.body?.let { rawHtml ->
                withContext(Dispatchers.Default) {
                    RichTextParser.parse(
                        html = rawHtml,
                        config = ParserConfig(
                            linkColor = Color(0xFF005FB8), // MaterialTheme primary equivalent fallback
                            codeBackground = Color(0xFFE2E2E2), // MaterialTheme surfaceContainerHighest equivalent fallback
                            spoilerColor = Color(0xFF44474E) // MaterialTheme onSurfaceVariant equivalent fallback
                        )
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    thread = thread,
                    parsedBody = parsedBody,
                    comments = commentsData?.items ?: emptyList(),
                    hasMoreComments = commentsData?.hasNextPage ?: false,
                    currentCommentPage = 1,
                    isSaved = isSaved,
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
            val sort = _uiState.value.commentSort

            when (val result = forumRepository.getComments(threadId, nextPage, sort = sort)) {
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
                }
            }
        }
    }

    private fun changeCommentSort(action: ThreadDetailAction.ChangeCommentSort) {
        val threadId = _uiState.value.thread?.id ?: return
        _uiState.update { it.copy(commentSort = action.sort, commentSortLabel = action.label) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreComments = true, comments = emptyList()) }
            when (val result =
                forumRepository.getComments(threadId, page = 1, sort = action.sort)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMoreComments = false,
                            comments = result.data.items,
                            hasMoreComments = result.data.hasNextPage,
                            currentCommentPage = 1
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingMoreComments = false) }
                }
            }
        }
    }

    private fun toggleLike(action: ThreadDetailAction.ToggleLike) {
        _uiState.update { state ->
            if (action.isThread) {
                val thread = state.thread ?: return@update state
                val newLikeCount =
                    if (action.currentLiked) thread.likeCount - 1 else thread.likeCount + 1
                state.copy(
                    thread = thread.copy(
                        isLiked = !action.currentLiked,
                        likeCount = newLikeCount.coerceAtLeast(0)
                    )
                )
            } else {
                state.copy(comments = state.comments.map { it.updateCommentLike(action) })
            }
        }

        viewModelScope.launch {
            val result = if (action.isThread) {
                forumRepository.toggleLikeThread(action.id)
            } else {
                forumRepository.toggleLikeComment(action.id)
            }

            if (result is Result.Error) {
                _uiState.update { state ->
                    if (action.isThread) {
                        val thread = state.thread ?: return@update state
                        val revertedCount =
                            if (action.currentLiked) thread.likeCount + 1 else thread.likeCount - 1
                        state.copy(
                            thread = thread.copy(
                                isLiked = action.currentLiked,
                                likeCount = revertedCount.coerceAtLeast(0)
                            )
                        )
                    } else {
                        val revertAction = action.copy(currentLiked = !action.currentLiked)
                        state.copy(comments = state.comments.map { it.updateCommentLike(revertAction) })
                    }
                }
            }
        }
    }

    private fun toggleSave() {
        val thread = _uiState.value.thread ?: return
        val wasSaved = _uiState.value.isSaved

        _uiState.update { it.copy(isSaved = !wasSaved) }

        viewModelScope.launch {
            try {
                if (wasSaved) {
                    forumRepository.unsaveThread(thread.id)
                } else {
                    forumRepository.saveThread(thread)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaved = wasSaved) }
            }
        }
    }

    private fun toggleSubscribe() {
        val thread = _uiState.value.thread ?: return
        val wasSubscribed = thread.isSubscribed

        _uiState.update { it.copy(thread = thread.copy(isSubscribed = !wasSubscribed)) }

        viewModelScope.launch {
            val result = forumRepository.toggleThreadSubscription(thread.id, !wasSubscribed)
            if (result is Result.Error) {
                _uiState.update { it.copy(thread = it.thread?.copy(isSubscribed = wasSubscribed)) }
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
                        val updatedComments = if (parentId != null) {
                            current.comments.map { it.insertReply(parentId, result.data) }
                        } else {
                            current.comments + result.data
                        }
                        current.copy(
                            isSubmittingReply = false,
                            isReplySheetVisible = false,
                            replyTargetCommentId = null,
                            comments = updatedComments,
                            scrollToBottom = parentId == null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isSubmittingReply = false) }
                }
            }
        }
    }
}

/**
 * Recursively walks the comment tree to toggle the like state
 * of the comment matching [action].id.
 */
private fun ForumComment.updateCommentLike(action: ThreadDetailAction.ToggleLike): ForumComment {
    return if (id == action.id) {
        val newCount = if (action.currentLiked) likeCount - 1 else likeCount + 1
        copy(isLiked = !action.currentLiked, likeCount = newCount.coerceAtLeast(0))
    } else {
        copy(childComments = childComments.map { it.updateCommentLike(action) })
    }
}

/**
 * Recursively walks the comment tree to insert [reply] as a child
 * of the comment with [parentId].
 */
private fun ForumComment.insertReply(parentId: Int, reply: ForumComment): ForumComment {
    return if (id == parentId) {
        copy(childComments = childComments + reply)
    } else {
        copy(childComments = childComments.map { it.insertReply(parentId, reply) })
    }
}