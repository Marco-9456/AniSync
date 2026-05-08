package com.anisync.android.presentation.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumComment
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.PaginatedResult
import com.anisync.android.domain.Result
import com.anisync.android.domain.parser.RichTextParser
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
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

private const val DEFAULT_PER_PAGE = 25

@HiltViewModel
class ThreadDetailViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThreadDetailUiState())
    val uiState: StateFlow<ThreadDetailUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<ThreadDetailAction>()
    val actions: SharedFlow<ThreadDetailAction> = _actions.asSharedFlow()

    fun onAction(action: ThreadDetailAction) {
        when (action) {
            is ThreadDetailAction.Load -> load(action.threadId, action.targetCommentId)
            is ThreadDetailAction.LoadMoreComments -> loadMoreComments()
            is ThreadDetailAction.LoadEarlierComments -> loadEarlierComments()
            is ThreadDetailAction.JumpToPage -> jumpToPage(action.page)
            is ThreadDetailAction.JumpToFirstPage -> jumpToPage(1)
            is ThreadDetailAction.JumpToLatestPage -> jumpToPage(_uiState.value.lastPage)
            is ThreadDetailAction.ShowPageJumper ->
                _uiState.update { it.copy(isPageJumperVisible = true) }

            is ThreadDetailAction.HidePageJumper ->
                _uiState.update { it.copy(isPageJumperVisible = false) }

            is ThreadDetailAction.PrependScrollConsumed ->
                _uiState.update { it.copy(pendingPrependCount = 0) }

            is ThreadDetailAction.ScrollToTopConsumed ->
                _uiState.update { it.copy(pendingScrollToTop = false) }

            is ThreadDetailAction.ToggleLike -> toggleLike(action)
            is ThreadDetailAction.OpenReply -> {
                _uiState.update {
                    it.copy(
                        isReplySheetVisible = true,
                        replyTargetCommentId = action.parentCommentId,
                        replyTargetAuthorName = action.parentAuthorName,
                        replyPrefillBody = null,
                        editingCommentId = null
                    )
                }
            }

            is ThreadDetailAction.CloseReply -> {
                _uiState.update {
                    it.copy(
                        isReplySheetVisible = false,
                        replyTargetCommentId = null,
                        replyPrefillBody = null,
                        editingCommentId = null
                    )
                }
            }

            is ThreadDetailAction.SubmitReply -> submitReply(action)
            is ThreadDetailAction.ToggleSave -> toggleSave()
            is ThreadDetailAction.ToggleSubscribe -> toggleSubscribe()
            is ThreadDetailAction.ChangeCommentSort -> changeCommentSort(action)
            is ThreadDetailAction.EditThread -> _uiState.update { it.copy(pendingThreadEdit = true) }
            is ThreadDetailAction.ConsumeThreadEdit -> _uiState.update { it.copy(pendingThreadEdit = false) }
            is ThreadDetailAction.SubmitThreadEdit -> submitThreadEdit(action.body)
            is ThreadDetailAction.DeleteThread -> deleteThread()
            is ThreadDetailAction.EditComment -> openCommentEdit(action.commentId)
            is ThreadDetailAction.DeleteComment -> deleteComment(action.commentId)
        }
    }

    private fun submitThreadEdit(body: String) {
        val thread = _uiState.value.thread ?: return
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReply = true) }
            val result = forumRepository.createThread(
                title = thread.title,
                body = trimmed,
                categoryIds = thread.categories.map { it.id },
                id = thread.id
            )
            when (result) {
                is Result.Success -> {
                    val updated = thread.copy(
                        body = result.data.body,
                        bodyMarkdown = trimmed
                    )
                    val parsed = result.data.body?.let { html ->
                        withContext(Dispatchers.Default) { RichTextParser.parse(html) }
                    }
                    _uiState.update {
                        it.copy(
                            isSubmittingReply = false,
                            pendingThreadEdit = false,
                            thread = updated,
                            parsedBody = parsed
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSubmittingReply = false) }
                    showResultError(result)
                }
            }
        }
    }

    private fun openCommentEdit(commentId: Int) {
        val comment = _uiState.value.findComment(commentId) ?: return
        _uiState.update {
            it.copy(
                isReplySheetVisible = true,
                editingCommentId = commentId,
                replyTargetCommentId = null,
                replyTargetAuthorName = null,
                replyPrefillBody = comment.bodyMarkdown ?: comment.body
            )
        }
    }

    private fun deleteThread() {
        val threadId = _uiState.value.thread?.id ?: return
        viewModelScope.launch {
            when (val result = forumRepository.deleteThread(threadId)) {
                is Result.Success -> _uiState.update { it.copy(threadDeleted = true) }
                is Result.Error -> showResultError(result)
            }
        }
    }

    private fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            when (val result = forumRepository.deleteComment(commentId)) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(
                        comments = state.comments.removeComment(commentId),
                        totalComments = (state.totalComments - 1).coerceAtLeast(0)
                    )
                }
                is Result.Error -> showResultError(result)
            }
        }
    }

    private fun load(threadId: Int, targetCommentId: Int?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    anchorCommentId = null,
                    pendingPrependCount = 0,
                    pendingScrollToTop = false,
                )
            }

            val sort = _uiState.value.commentSort

            val threadDeferred = async { forumRepository.getThread(threadId) }
            val savedDeferred = async { forumRepository.isThreadSaved(threadId) }
            val viewerDeferred = async { forumRepository.getViewerId() }

            // If we have a deep-link target, find its page first; otherwise start at 1.
            val targetPageDeferred = async {
                if (targetCommentId != null) {
                    forumRepository.findCommentPage(
                        threadId,
                        targetCommentId,
                        sort,
                        DEFAULT_PER_PAGE
                    )
                } else null
            }

            val threadResult = threadDeferred.await()
            val isSaved = savedDeferred.await()
            val viewerId = viewerDeferred.await()

            if (threadResult is Result.Error) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = threadResult.message)
                }
                return@launch
            }
            val thread = (threadResult as Result.Success).data

            val targetPageResult = targetPageDeferred.await()
            val resolvedPage = when (targetPageResult) {
                is Result.Success -> targetPageResult.data
                else -> 1
            }
            val anchorId = if (targetCommentId != null && targetPageResult is Result.Success) {
                targetCommentId
            } else null
            val targetMissed =
                targetCommentId != null && targetPageResult !is Result.Success

            val commentsResult =
                forumRepository.getComments(threadId, page = resolvedPage, sort = sort)

            val parsedBody = thread.body?.let { rawHtml ->
                withContext(Dispatchers.Default) {
                    RichTextParser.parse(html = rawHtml)
                }
            }

            val commentsData = (commentsResult as? Result.Success)?.data
            val resolvedLastPage = commentsData?.lastPage?.takeIf { it > 0 }
                ?: resolvedPage
            val total = commentsData?.total?.takeIf { it > 0 }
                ?: thread.replyCount

            _uiState.update {
                it.copy(
                    isLoading = false,
                    thread = thread,
                    parsedBody = parsedBody,
                    comments = commentsData?.items ?: emptyList(),
                    loadedPageRange = resolvedPage..resolvedPage,
                    lastPage = resolvedLastPage,
                    totalComments = total,
                    hasMoreComments = resolvedPage < resolvedLastPage,
                    hasEarlierComments = resolvedPage > 1,
                    anchorCommentId = anchorId,
                    isSaved = isSaved,
                    viewerId = viewerId,
                    errorMessage = null,
                )
            }

            if (targetMissed) {
                toastManager.showToast(
                    ToastType.INFO,
                    message = "Comment unavailable, showing thread from start"
                )
            }
        }
    }

    private fun loadMoreComments() {
        val state = _uiState.value
        val threadId = state.thread?.id ?: return
        val range = state.loadedPageRange ?: return
        if (state.isLoadingMoreComments || !state.hasMoreComments) return
        val nextPage = range.last + 1

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreComments = true) }
            when (val result =
                forumRepository.getComments(threadId, nextPage, sort = state.commentSort)) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        val newRange = (current.loadedPageRange?.first ?: nextPage)..nextPage
                        current.copy(
                            isLoadingMoreComments = false,
                            comments = current.comments + data.items,
                            loadedPageRange = newRange,
                            lastPage = data.lastPage.takeIf { it > 0 } ?: current.lastPage,
                            totalComments = data.total.takeIf { it > 0 } ?: current.totalComments,
                            hasMoreComments = nextPage < (data.lastPage.takeIf { it > 0 }
                                ?: current.lastPage),
                            hasEarlierComments = newRange.first > 1,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingMoreComments = false) }
                }
            }
        }
    }

    private fun loadEarlierComments() {
        val state = _uiState.value
        val threadId = state.thread?.id ?: return
        val range = state.loadedPageRange ?: return
        if (state.isLoadingEarlierComments || !state.hasEarlierComments) return
        val prevPage = range.first - 1
        if (prevPage < 1) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEarlierComments = true) }
            when (val result =
                forumRepository.getComments(threadId, prevPage, sort = state.commentSort)) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        val newRange = prevPage..(current.loadedPageRange?.last ?: prevPage)
                        current.copy(
                            isLoadingEarlierComments = false,
                            comments = data.items + current.comments,
                            loadedPageRange = newRange,
                            lastPage = data.lastPage.takeIf { it > 0 } ?: current.lastPage,
                            totalComments = data.total.takeIf { it > 0 } ?: current.totalComments,
                            hasEarlierComments = newRange.first > 1,
                            hasMoreComments = newRange.last < (data.lastPage.takeIf { it > 0 }
                                ?: current.lastPage),
                            pendingPrependCount = data.items.size,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingEarlierComments = false) }
                }
            }
        }
    }

    private fun jumpToPage(page: Int) {
        val state = _uiState.value
        val threadId = state.thread?.id ?: return
        val target = page.coerceIn(1, state.lastPage.coerceAtLeast(1))
        // No-op if already showing that page exclusively
        if (state.loadedPageRange == target..target && !state.isLoadingMoreComments) {
            _uiState.update { it.copy(isPageJumperVisible = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingMoreComments = true,
                    isPageJumperVisible = false,
                    anchorCommentId = null,
                )
            }
            when (val result =
                forumRepository.getComments(threadId, target, sort = state.commentSort)) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        current.copy(
                            isLoadingMoreComments = false,
                            comments = data.items,
                            loadedPageRange = target..target,
                            lastPage = data.lastPage.takeIf { it > 0 } ?: current.lastPage,
                            totalComments = data.total.takeIf { it > 0 } ?: current.totalComments,
                            hasMoreComments = target < (data.lastPage.takeIf { it > 0 }
                                ?: current.lastPage),
                            hasEarlierComments = target > 1,
                            pendingScrollToTop = true,
                            pendingPrependCount = 0,
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
        _uiState.update {
            it.copy(
                commentSort = action.sort,
                commentSortLabel = action.label,
                isLoadingMoreComments = true,
                comments = emptyList(),
                anchorCommentId = null,
                loadedPageRange = null,
            )
        }

        viewModelScope.launch {
            when (val result =
                forumRepository.getComments(threadId, page = 1, sort = action.sort)) {
                is Result.Success -> {
                    val data: PaginatedResult<ForumComment> = result.data
                    _uiState.update { current ->
                        current.copy(
                            isLoadingMoreComments = false,
                            comments = data.items,
                            loadedPageRange = 1..1,
                            lastPage = data.lastPage.takeIf { it > 0 } ?: 1,
                            totalComments = data.total.takeIf { it > 0 } ?: current.totalComments,
                            hasMoreComments = data.hasNextPage,
                            hasEarlierComments = false,
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
            val state = _uiState.value
            val editingId = state.editingCommentId
            val parentId = state.replyTargetCommentId

            val result = when {
                editingId != null -> forumRepository.createComment(
                    threadId = action.threadId,
                    comment = action.body,
                    id = editingId
                )
                parentId != null -> forumRepository.replyToComment(action.threadId, action.body, parentId)
                else -> forumRepository.createComment(action.threadId, action.body)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val updatedComments = when {
                            editingId != null -> current.comments.replaceComment(editingId, result.data, action.body)
                            parentId != null -> current.comments.map { it.insertReply(parentId, result.data) }
                            else -> current.comments + result.data
                        }
                        current.copy(
                            isSubmittingReply = false,
                            isReplySheetVisible = false,
                            replyTargetCommentId = null,
                            replyPrefillBody = null,
                            editingCommentId = null,
                            comments = updatedComments,
                            scrollToBottom = editingId == null && parentId == null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isSubmittingReply = false) }
                    showResultError(result)
                }
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

/** Walks the comment tree (incl. children) and removes the comment with [commentId]. */
private fun List<ForumComment>.removeComment(commentId: Int): List<ForumComment> =
    this.mapNotNull { c ->
        if (c.id == commentId) null
        else c.copy(childComments = c.childComments.removeComment(commentId))
    }

/**
 * Replaces the comment with [commentId] in the tree with the server-returned [updated],
 * preserving existing childComments and stamping [enteredMarkdown] as the new
 * [ForumComment.bodyMarkdown] so subsequent re-edits prefill correctly.
 */
private fun List<ForumComment>.replaceComment(
    commentId: Int,
    updated: ForumComment,
    enteredMarkdown: String
): List<ForumComment> = this.map { c ->
    if (c.id == commentId) {
        updated.copy(
            bodyMarkdown = enteredMarkdown,
            childComments = c.childComments
        )
    } else {
        c.copy(childComments = c.childComments.replaceComment(commentId, updated, enteredMarkdown))
    }
}

/** Locate a comment by id anywhere in the tree (depth-first). */
private fun ThreadDetailUiState.findComment(commentId: Int): ForumComment? {
    fun search(list: List<ForumComment>): ForumComment? {
        for (c in list) {
            if (c.id == commentId) return c
            search(c.childComments)?.let { return it }
        }
        return null
    }
    return search(comments)
}
