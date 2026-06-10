package com.anisync.android.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A change to a single forum thread's engagement state. Only the fields that
 * changed are non-null; collectors patch matching threads and leave the rest
 * untouched. [isSaved] toggles the locally-bookmarked set.
 */
data class ThreadUpdate(
    val id: Int,
    val isLiked: Boolean? = null,
    val likeCount: Int? = null,
    val isSubscribed: Boolean? = null,
    val replyCount: Int? = null,
    val isSaved: Boolean? = null,
    val deleted: Boolean = false
)

/**
 * App-wide bus that keeps every forum surface showing the same thread in sync
 * without a refetch. The thread detail screen and the forum list screens publish
 * their optimistic mutations here; every forum list (hub, per-media, category)
 * collects and patches its cached threads so a like / subscribe / save / reply /
 * delete is reflected the instant the user returns.
 *
 * Process-scoped singleton; buffered [MutableSharedFlow] with no replay.
 */
@Singleton
class ThreadEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<ThreadUpdate>(extraBufferCapacity = 32)
    val events: SharedFlow<ThreadUpdate> = _events.asSharedFlow()

    fun publish(update: ThreadUpdate) {
        _events.tryEmit(update)
    }
}
