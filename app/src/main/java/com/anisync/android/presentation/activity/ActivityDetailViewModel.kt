package com.anisync.android.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ActivityEventBus
import com.anisync.android.domain.ActivityReply
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.ActivityUpdate
import com.anisync.android.domain.CommentNode
import com.anisync.android.domain.Result
import com.anisync.android.domain.parser.RichTextParser
import com.anisync.android.presentation.components.alert.ToastManager
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
class ActivityDetailViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val activityEventBus: ActivityEventBus,
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()

    // Server-truth (isLiked, likeCount) for the activity so a coalesced like burst
    // rolls back correctly on error. Each tap flips the optimistic UI instantly;
    // the coalescer sends at most one toggle once the burst settles.
    private var activityLikeServer: Pair<Boolean, Int>? = null
    private val activityLikeCoalescer =
        com.anisync.android.presentation.util.MutationCoalescer<Int, Boolean>(viewModelScope) { id, _ ->
            when (val result = repository.toggleActivityLike(id)) {
                is Result.Success -> {
                    val s = result.data
                    activityLikeServer = s.isLiked to s.likeCount
                    _uiState.update { st ->
                        st.copy(activity = st.activity?.copy(isLiked = s.isLiked, likeCount = s.likeCount))
                    }
                    activityEventBus.publish(
                        ActivityUpdate(
                            id,
                            isLiked = s.isLiked,
                            likeCount = s.likeCount
                        )
                    )
                    true
                }
                is Result.Error -> {
                    activityLikeServer?.let { (liked, count) ->
                        _uiState.update { st ->
                            st.copy(activity = st.activity?.copy(isLiked = liked, likeCount = count))
                        }
                    }
                    false
                }
            }
        }

    // Reply ids with a like toggle in flight; a repeat tap is dropped until it
    // completes so rapid tapping can't stack reply-like requests.
    private val inFlightReplyLikes = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    private val _finishedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finishedEvents: SharedFlow<Unit> = _finishedEvents.asSharedFlow()

    init {
        // An external full-screen status edit (the EditActivity route) publishes the new body to the
        // shared bus; patch the open activity in place so returning shows the edit without a refetch.
        viewModelScope.launch {
            activityEventBus.events.collect { update ->
                val current = _uiState.value.activity ?: return@collect
                if (update.id != current.id) return@collect
                if (update.bodyHtml == null && update.bodyMarkdown == null) return@collect
                val newHtml = update.bodyHtml ?: current.body
                val parsed = withContext(Dispatchers.Default) { RichTextParser.parse(html = newHtml) }
                _uiState.update { state ->
                    val activity = state.activity ?: return@update state
                    state.copy(
                        activity = activity.copy(
                            body = update.bodyHtml ?: activity.body,
                            bodyMarkdown = update.bodyMarkdown ?: activity.bodyMarkdown
                        ),
                        parsedBody = parsed
                    )
                }
            }
        }
    }

    fun onAction(action: ActivityDetailAction) {
        when (action) {
            is ActivityDetailAction.Load -> load(action.activityId, refreshing = false)
            is ActivityDetailAction.Refresh -> {
                val id = _uiState.value.activity?.id ?: return
                load(id, refreshing = true)
            }
            is ActivityDetailAction.ToggleActivityLike -> toggleActivityLike()
            is ActivityDetailAction.ToggleSubscription -> toggleSubscription()
            is ActivityDetailAction.ToggleReplyLike -> toggleReplyLike(action.replyId)
            is ActivityDetailAction.OpenReply -> _uiState.update {
                it.copy(
                    isReplySheetVisible = true,
                    replyingToReplyId = action.replyId,
                    replyingToAuthor = action.authorName,
                    replyPrefillBody = action.authorName?.takeIf { action.replyId != null }?.let { "@$it " },
                    editingReplyId = null
                )
            }
            is ActivityDetailAction.CloseReply -> _uiState.update {
                it.copy(
                    isReplySheetVisible = false,
                    replyingToReplyId = null,
                    replyingToAuthor = null,
                    replyPrefillBody = null,
                    editingReplyId = null
                )
            }
            is ActivityDetailAction.SubmitReply -> submitReply(action.text)
            is ActivityDetailAction.DeleteActivity -> deleteActivity()
            is ActivityDetailAction.DeleteReply -> deleteReply(action.replyId)
            is ActivityDetailAction.ConsumeScrollToBottom -> _uiState.update { it.copy(scrollToBottom = false) }
            is ActivityDetailAction.EditReply -> openReplyEdit(action.replyId)
        }
    }

    private fun openReplyEdit(replyId: Int) {
        val reply = _uiState.value.activity?.replies?.firstOrNull { it.id == replyId } ?: return
        _uiState.update {
            it.copy(
                isReplySheetVisible = true,
                editingReplyId = replyId,
                replyingToReplyId = null,
                replyingToAuthor = null,
                replyPrefillBody = reply.bodyMarkdown ?: reply.body
            )
        }
    }

    private fun load(activityId: Int, refreshing: Boolean) {
        // Offline-first: skip a redundant reload when this activity is already loaded (or loading) —
        // e.g. a configuration change re-dispatches Load but the ViewModel kept its state. Pull-to-
        // refresh uses [ActivityDetailAction.Refresh] (refreshing = true) to force a fetch; a retry
        // after an error still loads because errorMessage is set.
        val state = _uiState.value
        if (!refreshing && state.activity?.id == activityId && state.errorMessage == null) {
            return
        }
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
                        toastManager.showResultError(activityResult)
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
        if (activityLikeServer == null) activityLikeServer = wasLiked to current.likeCount
        activityLikeCoalescer.seed(current.id, wasLiked)
        _uiState.update {
            it.copy(
                activity = current.copy(
                    isLiked = !wasLiked,
                    likeCount = (if (wasLiked) current.likeCount - 1 else current.likeCount + 1).coerceAtLeast(0)
                )
            )
        }
        activityEventBus.publish(
            ActivityUpdate(
                current.id,
                isLiked = !wasLiked,
                likeCount = (if (wasLiked) current.likeCount - 1 else current.likeCount + 1).coerceAtLeast(
                    0
                )
            )
        )
        activityLikeCoalescer.submit(current.id, !wasLiked)
    }

    private fun toggleSubscription() {
        val current = _uiState.value.activity ?: return
        val wasSubscribed = current.isSubscribed
        _uiState.update { it.copy(activity = it.activity?.copy(isSubscribed = !wasSubscribed)) }
        activityEventBus.publish(ActivityUpdate(current.id, isSubscribed = !wasSubscribed))
        viewModelScope.launch {
            val result = repository.toggleSubscription(current.id, !wasSubscribed)
            if (result is Result.Error) {
                _uiState.update { it.copy(activity = it.activity?.copy(isSubscribed = wasSubscribed)) }
                activityEventBus.publish(ActivityUpdate(current.id, isSubscribed = wasSubscribed))
                toastManager.showResultError(result)
            }
        }
    }

    private fun toggleReplyLike(replyId: Int) {
        val activity = _uiState.value.activity ?: return
        val original = activity.replies.firstOrNull { it.id == replyId } ?: return
        if (!inFlightReplyLikes.add(replyId)) return
        val wasLiked = original.isLiked
        val flipped = original.copy(
            isLiked = !wasLiked,
            likeCount = (if (wasLiked) original.likeCount - 1 else original.likeCount + 1).coerceAtLeast(0)
        )
        applyReplyChange(replyId) { flipped }

        viewModelScope.launch {
            try {
                when (val result = repository.toggleReplyLike(replyId)) {
                    is Result.Success -> applyReplyChange(replyId) {
                        it.copy(isLiked = result.data.isLiked, likeCount = result.data.likeCount)
                    }
                    is Result.Error -> applyReplyChange(replyId) { original }
                }
            } finally {
                inFlightReplyLikes.remove(replyId)
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
                    _uiState.value.activity?.let {
                        activityEventBus.publish(ActivityUpdate(it.id, replyCount = it.replyCount))
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
                is Result.Success -> {
                    activityEventBus.publish(ActivityUpdate(id, deleted = true))
                    _finishedEvents.tryEmit(Unit)
                }
                is Result.Error -> toastManager.showResultError(result)
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
        _uiState.value.activity?.let {
            activityEventBus.publish(ActivityUpdate(it.id, replyCount = it.replyCount))
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

    private fun buildReplyTree(replies: List<ActivityReply>): List<CommentNode> =
        ReplyThreader.build(replies)
}
