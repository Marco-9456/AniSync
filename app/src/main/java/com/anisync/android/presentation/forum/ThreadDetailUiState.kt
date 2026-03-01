package com.anisync.android.presentation.forum

import androidx.compose.runtime.Stable
import com.anisync.android.domain.ForumComment
import com.anisync.android.domain.ForumThread

@Stable
data class ThreadDetailUiState(
    val isLoading: Boolean = true,
    val thread: ForumThread? = null,
    val comments: List<ForumComment> = emptyList(),
    val isLoadingMoreComments: Boolean = false,
    val hasMoreComments: Boolean = false,
    val currentCommentPage: Int = 1,
    val commentSort: String = "ID",
    val commentSortLabel: String = "Oldest",
    val isReplySheetVisible: Boolean = false,
    val replyTargetCommentId: Int? = null, // null = replying to thread
    val replyTargetAuthorName: String? = null,
    val isSubmittingReply: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val scrollToBottom: Boolean = false
)

sealed interface ThreadDetailAction {
    data class Load(val threadId: Int) : ThreadDetailAction
    data object LoadMoreComments : ThreadDetailAction
    data class ToggleLike(val isThread: Boolean, val id: Int, val currentLiked: Boolean) : ThreadDetailAction
    data class OpenReply(val parentCommentId: Int?, val parentAuthorName: String?) : ThreadDetailAction
    data object CloseReply : ThreadDetailAction
    data class SubmitReply(val threadId: Int, val body: String) : ThreadDetailAction
    data object ToggleSave : ThreadDetailAction
    data object ToggleSubscribe : ThreadDetailAction
    data class ChangeCommentSort(val sort: String, val label: String) : ThreadDetailAction
    data class ShowSnackbar(val message: String) : ThreadDetailAction
}
