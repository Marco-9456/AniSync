package com.anisync.android.domain

import com.anisync.android.type.NotificationType

enum class NotificationFilter(val types: List<NotificationType>) {
    ALL(NotificationType.knownEntries),
    AIRING(listOf(NotificationType.AIRING)),
    STATUS(listOf(
        NotificationType.ACTIVITY_LIKE,
        NotificationType.ACTIVITY_REPLY,
        NotificationType.ACTIVITY_REPLY_LIKE,
        NotificationType.ACTIVITY_REPLY_SUBSCRIBED,
        NotificationType.ACTIVITY_MENTION
    )),
    MESSAGES(listOf(NotificationType.ACTIVITY_MESSAGE)),
    FORUM(listOf(
        NotificationType.THREAD_COMMENT_REPLY,
        NotificationType.THREAD_SUBSCRIBED,
        NotificationType.THREAD_COMMENT_MENTION,
        NotificationType.THREAD_LIKE,
        NotificationType.THREAD_COMMENT_LIKE
    )),
    FOLLOWS(listOf(NotificationType.FOLLOWING)),
    MEDIA(listOf(
        NotificationType.RELATED_MEDIA_ADDITION,
        NotificationType.MEDIA_DATA_CHANGE,
        NotificationType.MEDIA_MERGE,
        NotificationType.MEDIA_DELETION
    ))
}
