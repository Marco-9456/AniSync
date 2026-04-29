package com.anisync.android.presentation.components.likes

/**
 * What the "Liked by" sheet should fetch likes for. AniList exposes likes on
 * four distinct objects, each behind its own query, but the sheet's UI is the
 * same for all of them.
 */
sealed interface LikesTarget {
    val id: Int

    data class Activity(override val id: Int) : LikesTarget
    data class ActivityReply(override val id: Int) : LikesTarget
    data class Thread(override val id: Int) : LikesTarget
    data class ThreadComment(override val id: Int) : LikesTarget
}
