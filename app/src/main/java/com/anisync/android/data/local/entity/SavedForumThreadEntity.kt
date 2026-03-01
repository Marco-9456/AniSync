package com.anisync.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for locally saved (bookmarked) forum threads.
 * This is an app-internal feature — not backed by the AniList API.
 */
@Entity(tableName = "saved_forum_threads")
data class SavedForumThreadEntity(
    @PrimaryKey val threadId: Int,
    val title: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val replyCount: Int,
    val viewCount: Int,
    val likeCount: Int,
    @ColumnInfo(defaultValue = "0") val isLiked: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isLocked: Boolean = false,
    val repliedAt: Long? = null,
    val replyUserName: String? = null,
    val replyUserAvatarUrl: String? = null,
    @ColumnInfo(defaultValue = "[]") val categories: List<com.anisync.android.domain.ForumCategory> = emptyList(),
    val mediaTitle: String? = null,
    val mediaCoverUrl: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)
