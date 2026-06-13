package com.anisync.android.data.mapper

import com.anisync.android.domain.ActivityMediaType
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserActivity
import com.anisync.android.fragment.ActivityFields
import com.anisync.android.type.MediaType

internal fun ActivityFields.toDomain(): UserActivity? {
    onListActivity?.let { l ->
        val lastReply = l.replies?.lastOrNull()
        return UserActivity(
            id = l.id ?: 0,
            type = ActivityType.MEDIA_LIST,
            status = l.status,
            progress = l.progress,
            mediaId = l.media?.id,
            mediaTitle = l.media?.title?.userPreferred ?: "Unknown",
            mediaCoverUrl = l.media?.coverImage?.medium,
            mediaCover = com.anisync.android.domain.CoverImage.of(l.media?.coverImage?.medium, l.media?.coverImage?.large, l.media?.coverImage?.extraLarge),
            mediaCoverColor = l.media?.coverImage?.color,
            mediaIsAdult = l.media?.isAdult == true,
            mediaType = l.media?.type.toActivityMediaType(),
            timestamp = (l.createdAt?.toLong() ?: 0L) * 1000L,
            mediaScore = null,
            userId = l.user?.id,
            userName = l.user?.name,
            userAvatarUrl = l.user?.avatar?.medium,
            replyUserName = lastReply?.user?.name,
            replyUserAvatarUrl = lastReply?.user?.avatar?.medium,
            repliedAt = lastReply?.createdAt?.toLong(),
            lastReplyId = lastReply?.id,
            likeCount = l.likeCount ?: 0,
            replyCount = l.replyCount ?: 0,
            isLiked = l.isLiked == true,
            isLocked = l.isLocked == true,
            isSubscribed = l.isSubscribed == true
        )
    }
    onTextActivity?.let { t ->
        val lastReply = t.replies?.lastOrNull()
        return UserActivity(
            id = t.id ?: 0,
            type = ActivityType.TEXT,
            text = t.text,
            bodyMarkdown = t.textRaw,
            timestamp = (t.createdAt?.toLong() ?: 0L) * 1000L,
            userId = t.user?.id,
            userName = t.user?.name,
            userAvatarUrl = t.user?.avatar?.medium,
            likeCount = t.likeCount ?: 0,
            replyCount = t.replyCount ?: 0,
            isLiked = t.isLiked == true,
            isLocked = t.isLocked == true,
            isSubscribed = t.isSubscribed == true,
            isPinned = t.isPinned == true,
            replyUserName = lastReply?.user?.name,
            replyUserAvatarUrl = lastReply?.user?.avatar?.medium,
            repliedAt = lastReply?.createdAt?.toLong(),
            lastReplyId = lastReply?.id
        )
    }
    onMessageActivity?.let { m ->
        val lastReply = m.replies?.lastOrNull()
        return UserActivity(
            id = m.id ?: 0,
            type = ActivityType.MESSAGE,
            text = m.message,
            bodyMarkdown = m.rawMessage,
            timestamp = (m.createdAt?.toLong() ?: 0L) * 1000L,
            userId = m.messenger?.id,
            userName = m.messenger?.name,
            userAvatarUrl = m.messenger?.avatar?.medium,
            recipientId = m.recipient?.id,
            recipientName = m.recipient?.name,
            recipientAvatarUrl = m.recipient?.avatar?.medium,
            likeCount = m.likeCount ?: 0,
            replyCount = m.replyCount ?: 0,
            isLiked = m.isLiked == true,
            isLocked = m.isLocked == true,
            isSubscribed = m.isSubscribed == true,
            isPrivate = m.isPrivate == true,
            isAuthorMod = !m.messenger?.moderatorRoles.isNullOrEmpty(),
            replyUserName = lastReply?.user?.name,
            replyUserAvatarUrl = lastReply?.user?.avatar?.medium,
            repliedAt = lastReply?.createdAt?.toLong(),
            lastReplyId = lastReply?.id
        )
    }
    return null
}

private fun MediaType?.toActivityMediaType(): ActivityMediaType? = when (this) {
    MediaType.ANIME -> ActivityMediaType.ANIME
    MediaType.MANGA -> ActivityMediaType.MANGA
    else -> null
}
