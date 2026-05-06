package com.anisync.android.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ActivityReply
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.CommentNode
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
import org.jsoup.Jsoup
import javax.inject.Inject

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()

    private val _finishedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finishedEvents: SharedFlow<Unit> = _finishedEvents.asSharedFlow()

    private val mentionRegex = Regex("^@([A-Za-z0-9_]+)")

    fun onAction(action: ActivityDetailAction) {
        when (action) {
            is ActivityDetailAction.Load -> load(action.activityId, refreshing = false)
            is ActivityDetailAction.Refresh -> {
                val id = _uiState.value.activity?.id ?: return
                load(id, refreshing = true)
            }
            is ActivityDetailAction.ToggleActivityLike -> toggleActivityLike()
            is ActivityDetailAction.ToggleReplyLike -> toggleReplyLike(action.replyId)
            is ActivityDetailAction.OpenReply -> _uiState.update {
                it.copy(
                    isReplySheetVisible = true,
                    replyingToReplyId = action.replyId,
                    replyingToAuthor = action.authorName,
                    replyPrefillBody = action.authorName?.takeIf { action.replyId != null }?.let { "@$it " },
                    editingReplyId = null,
                    editingActivity = false
                )
            }
            is ActivityDetailAction.CloseReply -> _uiState.update {
                it.copy(
                    isReplySheetVisible = false,
                    replyingToReplyId = null,
                    replyingToAuthor = null,
                    replyPrefillBody = null,
                    editingReplyId = null,
                    editingActivity = false
                )
            }
            is ActivityDetailAction.SubmitReply -> submitReply(action.text)
            is ActivityDetailAction.DeleteActivity -> deleteActivity()
            is ActivityDetailAction.DeleteReply -> deleteReply(action.replyId)
            is ActivityDetailAction.ConsumeScrollToBottom -> _uiState.update { it.copy(scrollToBottom = false) }
            is ActivityDetailAction.EditActivity -> openActivityEdit()
            is ActivityDetailAction.EditReply -> openReplyEdit(action.replyId)
        }
    }

    private fun openActivityEdit() {
        val activity = _uiState.value.activity ?: return
        _uiState.update {
            it.copy(
                isReplySheetVisible = true,
                editingActivity = true,
                editingReplyId = null,
                replyingToReplyId = null,
                replyingToAuthor = null,
                replyPrefillBody = activity.bodyMarkdown ?: activity.body
            )
        }
    }

    private fun openReplyEdit(replyId: Int) {
        val reply = _uiState.value.activity?.replies?.firstOrNull { it.id == replyId } ?: return
        _uiState.update {
            it.copy(
                isReplySheetVisible = true,
                editingReplyId = replyId,
                editingActivity = false,
                replyingToReplyId = null,
                replyingToAuthor = null,
                replyPrefillBody = reply.bodyMarkdown ?: reply.body
            )
        }
    }

    private fun load(activityId: Int, refreshing: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refreshing,
                    isRefreshing = refreshing,
                    errorMessage = null
                )
            }
            val activityDeferred = async { repository.getActivity(activityId) }
            val viewerDeferred = async { repository.getViewerId() }

            when (val activityResult = activityDeferred.await()) {
                is Result.Success -> {
                    val activity = activityResult.data
                    val parsed = withContext(Dispatchers.Default) {
                        RichTextParser.parse(html = activity.body)
                    }
                    val nodes = withContext(Dispatchers.Default) {
                        buildReplyTree(activity.replies)
                    }
                    val viewerId = viewerDeferred.await()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            activity = activity,
                            parsedBody = parsed,
                            replyNodes = nodes,
                            viewerId = viewerId,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    if (activityResult.code == 404) {
                        // Activity was deleted or not found — show a toast and pop back
                        // instead of a dead-end error screen (e.g. when opening from a notification)
                        toastManager.showToast(activityResult.code, activityResult.message)
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                        _finishedEvents.tryEmit(Unit)
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, isRefreshing = false, errorMessage = activityResult.message)
                        }
                    }
                }
            }
        }
    }

    private fun toggleActivityLike() {
        val current = _uiState.value.activity ?: return
        val wasLiked = current.isLiked
        val optimistic = current.copy(
            isLiked = !wasLiked,
            likeCount = (if (wasLiked) current.likeCount - 1 else current.likeCount + 1).coerceAtLeast(0)
        )
        _uiState.update { it.copy(activity = optimistic) }
        viewModelScope.launch {
            when (val result = repository.toggleActivityLike(current.id)) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(
                        activity = state.activity?.copy(
                            likeCount = result.data.likeCount,
                            isLiked = result.data.isLiked
                        )
                    )
                }
                is Result.Error -> _uiState.update { it.copy(activity = current) }
            }
        }
    }

    private fun toggleReplyLike(replyId: Int) {
        val activity = _uiState.value.activity ?: return
        val original = activity.replies.firstOrNull { it.id == replyId } ?: return
        val wasLiked = original.isLiked
        val flipped = original.copy(
            isLiked = !wasLiked,
            likeCount = (if (wasLiked) original.likeCount - 1 else original.likeCount + 1).coerceAtLeast(0)
        )
        applyReplyChange(replyId) { flipped }

        viewModelScope.launch {
            when (val result = repository.toggleReplyLike(replyId)) {
                is Result.Success -> applyReplyChange(replyId) {
                    it.copy(isLiked = result.data.isLiked, likeCount = result.data.likeCount)
                }
                is Result.Error -> applyReplyChange(replyId) { original }
            }
        }
    }

    private fun applyReplyChange(replyId: Int, transform: (ActivityReply) -> ActivityReply) {
        _uiState.update { state ->
            val activity = state.activity ?: return@update state
            val updatedReplies = activity.replies.map { r -> if (r.id == replyId) transform(r) else r }
            val newActivity = activity.copy(replies = updatedReplies)
            state.copy(activity = newActivity, replyNodes = buildReplyTree(updatedReplies))
        }
    }

    private fun submitReply(text: String) {
        val state = _uiState.value
        val activityId = state.activity?.id ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        when {
            state.editingActivity -> submitActivityEdit(activityId, trimmed)
            state.editingReplyId != null -> submitReplyEdit(activityId, state.editingReplyId, trimmed)
            else -> submitNewReply(activityId, trimmed)
        }
    }

    private fun submitNewReply(activityId: Int, trimmed: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReply = true) }
            when (val result = repository.sendReply(activityId, trimmed)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        val activity = state.activity ?: return@update state
                        val newReplies = activity.replies + result.data
                        state.copy(
                            isSubmittingReply = false,
                            isReplySheetVisible = false,
                            replyingToReplyId = null,
                            replyingToAuthor = null,
                            replyPrefillBody = null,
                            activity = activity.copy(
                                replies = newReplies,
                                replyCount = activity.replyCount + 1
                            ),
                            replyNodes = buildReplyTree(newReplies),
                            scrollToBottom = true
                        )
                    }
                    refreshSilent(activityId)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSubmittingReply = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun submitReplyEdit(activityId: Int, replyId: Int, trimmed: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReply = true) }
            when (val result = repository.sendReply(activityId, trimmed, id = replyId)) {
                is Result.Success -> {
                    val updated = result.data.copy(bodyMarkdown = trimmed)
                    _uiState.update { state ->
                        val activity = state.activity ?: return@update state
                        val newReplies = activity.replies.map { r ->
                            if (r.id == replyId) updated else r
                        }
                        state.copy(
                            isSubmittingReply = false,
                            isReplySheetVisible = false,
                            editingReplyId = null,
                            replyPrefillBody = null,
                            activity = activity.copy(replies = newReplies),
                            replyNodes = buildReplyTree(newReplies)
                        )
                    }
                    refreshSilent(activityId)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSubmittingReply = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun submitActivityEdit(activityId: Int, trimmed: String) {
        val activity = _uiState.value.activity ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReply = true) }
            val result = if (activity.isMessage) {
                val recipientId = activity.recipientId
                if (recipientId == null) {
                    _uiState.update {
                        it.copy(isSubmittingReply = false, errorMessage = "Missing recipient")
                    }
                    return@launch
                }
                repository.saveMessageActivity(
                    id = activityId,
                    recipientId = recipientId,
                    message = trimmed,
                    isPrivate = activity.isPrivate
                )
            } else {
                repository.saveTextActivity(trimmed, id = activityId)
            }
            when (result) {
                is Result.Success -> {
                    refreshSilent(activityId)
                    _uiState.update {
                        it.copy(
                            isSubmittingReply = false,
                            isReplySheetVisible = false,
                            editingActivity = false,
                            replyPrefillBody = null
                        )
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSubmittingReply = false, errorMessage = result.message)
                }
            }
        }
    }

    private suspend fun refreshSilent(activityId: Int) {
        when (val result = repository.getActivity(activityId)) {
            is Result.Success -> {
                val activity = result.data
                val parsed = withContext(Dispatchers.Default) { RichTextParser.parse(html = activity.body) }
                val nodes = withContext(Dispatchers.Default) { buildReplyTree(activity.replies) }
                _uiState.update {
                    it.copy(activity = activity, parsedBody = parsed, replyNodes = nodes)
                }
            }
            is Result.Error -> Unit
        }
    }

    private fun deleteActivity() {
        val id = _uiState.value.activity?.id ?: return
        viewModelScope.launch {
            when (val result = repository.deleteActivity(id)) {
                is Result.Success -> _finishedEvents.tryEmit(Unit)
                is Result.Error -> {
                    if (result.code != null) {
                        toastManager.showToast(result.code, result.message)
                    } else {
                        toastManager.showToast(ToastType.INFO, message = result.message)
                    }
                }
            }
        }
    }

    private fun deleteReply(replyId: Int) {
        val activity = _uiState.value.activity ?: return
        val removed = activity.replies.firstOrNull { it.id == replyId } ?: return
        val optimisticReplies = activity.replies.filter { it.id != replyId }
        _uiState.update { state ->
            state.copy(
                activity = activity.copy(
                    replies = optimisticReplies,
                    replyCount = (activity.replyCount - 1).coerceAtLeast(0)
                ),
                replyNodes = buildReplyTree(optimisticReplies)
            )
        }
        viewModelScope.launch {
            when (repository.deleteReply(replyId)) {
                is Result.Success -> Unit
                is Result.Error -> _uiState.update { state ->
                    val current = state.activity ?: return@update state
                    val restored = (current.replies + removed).sortedBy { it.createdAt }
                    state.copy(
                        activity = current.copy(replies = restored, replyCount = current.replyCount + 1),
                        replyNodes = buildReplyTree(restored)
                    )
                }
            }
        }
    }

    /**
     * Emulates threading on AniList's flat reply list by parsing a leading @username mention.
     * A reply starting with "@Alice" nests under Alice's most recent earlier reply in this activity.
     */
    private fun buildReplyTree(replies: List<ActivityReply>): List<CommentNode> {
        if (replies.isEmpty()) return emptyList()
        val sorted = replies.sortedBy { it.createdAt }
        val placeholders = sorted.map { reply ->
            reply to mutableListOf<CommentNode>()
        }
        val byId = placeholders.associateBy { it.first.id }

        val parentOf = HashMap<Int, Int>(sorted.size)
        val latestByAuthor = HashMap<String, Int>() // lowercase username -> reply id

        for ((reply, _) in placeholders) {
            val mention = extractLeadingMention(reply.body)
            if (mention != null) {
                val parentId = latestByAuthor[mention.lowercase()]
                if (parentId != null && parentId != reply.id) {
                    parentOf[reply.id] = parentId
                }
            }
            latestByAuthor[reply.authorName.lowercase()] = reply.id
        }

        fun toNode(reply: ActivityReply, children: List<CommentNode>) = CommentNode(
            id = reply.id,
            body = reply.body,
            likeCount = reply.likeCount,
            isLiked = reply.isLiked,
            authorId = reply.authorId,
            authorName = reply.authorName,
            authorAvatarUrl = reply.authorAvatarUrl,
            createdAt = reply.createdAt,
            childComments = children
        )

        // Build bottom-up: accumulate children per parent, then materialize roots
        val childLists = HashMap<Int, MutableList<ActivityReply>>()
        for ((reply, _) in placeholders) {
            val parentId = parentOf[reply.id]
            if (parentId != null && byId.containsKey(parentId)) {
                childLists.getOrPut(parentId) { mutableListOf() }.add(reply)
            }
        }

        fun build(reply: ActivityReply): CommentNode {
            val kids = childLists[reply.id].orEmpty().map { build(it) }
            return toNode(reply, kids)
        }

        return sorted
            .filter { parentOf[it.id] == null }
            .map { build(it) }
    }

    private fun extractLeadingMention(html: String): String? {
        if (html.isBlank()) return null
        val text = try {
            Jsoup.parse(html).text().trimStart()
        } catch (_: Exception) {
            html.trimStart()
        }
        val match = mentionRegex.find(text) ?: return null
        return match.groupValues[1]
    }
}
