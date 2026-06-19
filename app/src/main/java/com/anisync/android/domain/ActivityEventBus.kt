package com.anisync.android.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A change to a single activity's engagement state (or, via [bodyHtml] /
 * [bodyMarkdown], its edited body). Only the fields that changed are non-null;
 * collectors patch matching items and leave the rest untouched. The body fields
 * let the full-screen status editor publish an edit back to the open activity
 * detail without a refetch — [bodyHtml] is the rendered AniList HTML,
 * [bodyMarkdown] the raw source kept for re-editing. [created] signals that a
 * brand-new activity was posted (by the full-screen composer); the feed reloads
 * to pull it in rather than patching an existing item, so [id] is irrelevant then.
 */
data class ActivityUpdate(
    val id: Int,
    val isLiked: Boolean? = null,
    val likeCount: Int? = null,
    val isSubscribed: Boolean? = null,
    val replyCount: Int? = null,
    val deleted: Boolean = false,
    val created: Boolean = false,
    val bodyHtml: String? = null,
    val bodyMarkdown: String? = null
)

/**
 * App-wide bus that keeps every screen showing the same activity in sync without
 * a refetch. The activity detail screen and the feed publish their optimistic
 * mutations here; list screens (feed) collect and patch their cached items so a
 * like / subscribe / reply / delete is reflected the instant the user returns.
 *
 * Process-scoped singleton; uses a buffered [MutableSharedFlow] with no replay so
 * a newly-subscribed collector doesn't re-apply stale events.
 */
@Singleton
class ActivityEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<ActivityUpdate>(extraBufferCapacity = 32)
    val events: SharedFlow<ActivityUpdate> = _events.asSharedFlow()

    fun publish(update: ActivityUpdate) {
        _events.tryEmit(update)
    }
}
