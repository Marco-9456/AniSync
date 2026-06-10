package com.anisync.android.presentation.forum

import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.ThreadUpdate

/**
 * Applies a [ThreadUpdate]'s non-null engagement fields onto [thread]. Shared by
 * every forum list ViewModel so cross-screen sync stays consistent. [ThreadUpdate.isSaved]
 * and [ThreadUpdate.deleted] are handled by the collector (they affect the saved-id
 * set / list membership, not the thread object itself).
 */
internal fun ThreadUpdate.applyTo(thread: ForumThread): ForumThread = thread.copy(
    isLiked = isLiked ?: thread.isLiked,
    likeCount = likeCount ?: thread.likeCount,
    isSubscribed = isSubscribed ?: thread.isSubscribed,
    replyCount = replyCount ?: thread.replyCount
)
