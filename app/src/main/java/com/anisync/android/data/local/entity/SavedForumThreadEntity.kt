package com.anisync.android.data.local.entity

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
    val savedAt: Long = System.currentTimeMillis()
)
