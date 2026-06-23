package com.anisync.android.presentation.activity

import androidx.compose.runtime.Stable
import com.anisync.android.domain.ActivityDetail
import com.anisync.android.domain.CommentNode
import com.anisync.android.domain.parser.ParsedRichText

@Stable
data class ActivityDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val activity: ActivityDetail? = null,
    val parsedBody: ParsedRichText? = null,
    val replyNodes: List<CommentNode> = emptyList(),
    val errorMessage: String? = null,
    val isReplySheetVisible: Boolean = false,
    val replyingToReplyId: Int? = null,
    val replyingToAuthor: String? = null,
    val replyPrefillBody: String? = null,
    val isSubmittingReply: Boolean = false,
    val viewerId: Int? = null,
    val scrollToBottom: Boolean = false,
    /** Set when the user opened the compose sheet to edit an existing reply. */
    val editingReplyId: Int? = null
)

sealed interface ActivityDetailAction {
    data class Load(val activityId: Int) : ActivityDetailAction
    data object Refresh : ActivityDetailAction
    data object ToggleActivityLike : ActivityDetailAction
    data object ToggleSubscription : ActivityDetailAction
    data class ToggleReplyLike(val replyId: Int) : ActivityDetailAction
    data class OpenReply(val replyId: Int?, val authorName: String?) : ActivityDetailAction
    data object CloseReply : ActivityDetailAction
    data class SubmitReply(val text: String) : ActivityDetailAction
    data object DeleteActivity : ActivityDetailAction
    data class DeleteReply(val replyId: Int) : ActivityDetailAction
    data object ConsumeScrollToBottom : ActivityDetailAction
    data class EditReply(val replyId: Int) : ActivityDetailAction
}
