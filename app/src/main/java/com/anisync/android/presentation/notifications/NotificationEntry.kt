package com.anisync.android.presentation.notifications

import com.anisync.android.domain.ActivityLikeNotification
import com.anisync.android.domain.ActivityReplyLikeNotification
import com.anisync.android.domain.FollowingNotification
import com.anisync.android.domain.Notification
import com.anisync.android.domain.ThreadCommentLikeNotification
import com.anisync.android.domain.ThreadLikeNotification
import com.anisync.android.domain.User

/**
 * A logical row in the inbox: either a single notification or a fold of
 * like/comment notifications sharing the same target. Grouping is intentionally
 * narrow — only likes and replies fold, since those carry no per-item content
 * worth surfacing on its own. Airings, follows, mentions, messages and media
 * changes always stay individual.
 */
data class NotificationEntry(
    val key: String,
    val representative: Notification,
    val all: List<Notification>,
    val actors: List<User>
) {
    val count: Int get() = all.size
    val isGrouped: Boolean get() = count > 1
}

fun groupNotifications(items: List<Notification>): List<NotificationEntry> {
    if (items.isEmpty()) return emptyList()

    val sorted = items.sortedByDescending { it.createdAt }
    val output = mutableListOf<NotificationEntry>()
    val grouped = mutableMapOf<String, MutableList<Notification>>()

    for (n in sorted) {
        val key = groupKey(n)
        if (key == null) {
            output += singleEntry(n)
        } else {
            val bucket = grouped[key]
            if (bucket == null) {
                grouped[key] = mutableListOf(n)
                // Placeholder preserves chronological position; collapsed below.
                output += NotificationEntry(
                    key = key,
                    representative = n,
                    all = emptyList(),
                    actors = emptyList()
                )
            } else {
                bucket += n
            }
        }
    }

    return output.map { entry ->
        val members = grouped[entry.key]
        if (members == null) entry else collapse(entry.key, members)
    }
}

private fun singleEntry(n: Notification): NotificationEntry = NotificationEntry(
    key = "single_${n.id}",
    representative = n,
    all = listOf(n),
    actors = listOfNotNull(actorOf(n))
)

private fun collapse(key: String, members: List<Notification>): NotificationEntry {
    val rep = members.first()
    val actors = members.mapNotNull(::actorOf).distinctBy { it.id }
    return NotificationEntry(
        key = key,
        representative = rep,
        all = members,
        actors = actors
    )
}

private fun groupKey(n: Notification): String? = when (n) {
    is ActivityLikeNotification -> n.activityId?.let { "act_like_$it" }
    is ActivityReplyLikeNotification -> n.activityId?.let { "act_reply_like_$it" }
    is ThreadLikeNotification -> "thread_like_${n.threadId}"
    is ThreadCommentLikeNotification -> "thread_cmt_like_${n.threadId}_${n.commentId ?: 0}"
    else -> null
}

private fun actorOf(n: Notification): User? = when (n) {
    is FollowingNotification -> n.user
    is ActivityLikeNotification -> n.user
    is com.anisync.android.domain.ActivityReplyNotification -> n.user
    is com.anisync.android.domain.ActivityReplySubscribedNotification -> n.user
    is ActivityReplyLikeNotification -> n.user
    is com.anisync.android.domain.ActivityMentionNotification -> n.user
    is com.anisync.android.domain.ActivityMessageNotification -> n.user
    is com.anisync.android.domain.ThreadCommentReplyNotification -> n.user
    is com.anisync.android.domain.ThreadCommentSubscribedNotification -> n.user
    is com.anisync.android.domain.ThreadCommentMentionNotification -> n.user
    is ThreadLikeNotification -> n.user
    is ThreadCommentLikeNotification -> n.user
    else -> null
}
