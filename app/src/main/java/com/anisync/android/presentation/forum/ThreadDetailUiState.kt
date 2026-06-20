package com.anisync.android.presentation.forum

import androidx.compose.runtime.Stable
import com.anisync.android.domain.ForumComment
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.parser.ParsedRichText

@Stable
data class ThreadDetailUiState(
    val isLoading: Boolean = true,
    val thread: ForumThread? = null,
    val parsedBody: ParsedRichText? = null,
    val comments: List<ForumComment> = emptyList(),
    val isLoadingMoreComments: Boolean = false,
    val isLoadingEarlierComments: Boolean = false,
    val hasMoreComments: Boolean = false,
    val hasEarlierComments: Boolean = false,
    val loadedPageRange: IntRange? = null,
    val lastPage: Int = 1,
    val totalComments: Int = 0,
    val perPage: Int = 25,
    val anchorCommentId: Int? = null,
    val isPageJumperVisible: Boolean = false,
    val commentSort: String = "ID",
    val commentSortLabel: String = "Oldest",
    val isReplySheetVisible: Boolean = false,
    val replyTargetCommentId: Int? = null,
    val replyTargetAuthorName: String? = null,
    val replyPrefillBody: String? = null,
    val isSubmittingReply: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val scrollToBottom: Boolean = false,
    val pendingScrollToTop: Boolean = false,
    val pendingPrependCount: Int = 0,
    val viewerId: Int? = null,
    /** Set when the user opened the compose sheet to edit an existing comment. */
    val editingCommentId: Int? = null,
    /** Emits once after a successful thread deletion so the screen can pop. */
    val threadDeleted: Boolean = false
)

sealed interface ThreadDetailAction {
    data class Load(
        val threadId: Int,
        val targetCommentId: Int? = null,
        val forceRefresh: Boolean = false,
    ) : ThreadDetailAction
    data object LoadMoreComments : ThreadDetailAction
    data object LoadEarlierComments : ThreadDetailAction
    data class JumpToPage(val page: Int) : ThreadDetailAction
    data object JumpToFirstPage : ThreadDetailAction
    data object JumpToLatestPage : ThreadDetailAction
    data object ShowPageJumper : ThreadDetailAction
    data object HidePageJumper : ThreadDetailAction
    data object PrependScrollConsumed : ThreadDetailAction
    data object ScrollToTopConsumed : ThreadDetailAction
    data class ToggleLike(val isThread: Boolean, val id: Int, val currentLiked: Boolean) :
        ThreadDetailAction

    data class OpenReply(val parentCommentId: Int?, val parentAuthorName: String?) :
        ThreadDetailAction

    data object CloseReply : ThreadDetailAction
    data class SubmitReply(val threadId: Int, val body: String) : ThreadDetailAction
    data object ToggleSave : ThreadDetailAction
    data object ToggleSubscribe : ThreadDetailAction
    data class ChangeCommentSort(val sort: String, val label: String) : ThreadDetailAction
    data object DeleteThread : ThreadDetailAction
    data class EditComment(val commentId: Int) : ThreadDetailAction
    data class DeleteComment(val commentId: Int) : ThreadDetailAction
}
