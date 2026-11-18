package com.anisync.android.domain

/** AniList lets the author delete an activity, and the recipient a message on their profile unless a moderator wrote it. */
fun UserActivity.canDelete(viewerId: Int?): Boolean =
    deletableBy(viewerId, authorId = userId, recipientId = recipientId, isMessage = type == ActivityType.MESSAGE, isAuthorMod = isAuthorMod)

/** Only written posts can be edited. List updates come from the list itself. */
fun UserActivity.canEdit(viewerId: Int?): Boolean =
    viewerId != null && userId == viewerId && (type == ActivityType.TEXT || type == ActivityType.MESSAGE)

fun ActivityDetail.canDelete(viewerId: Int?): Boolean =
    deletableBy(viewerId, authorId = authorId, recipientId = recipientId, isMessage = isMessage, isAuthorMod = isAuthorMod)

fun ActivityDetail.canEdit(viewerId: Int?): Boolean =
    viewerId != null && authorId == viewerId && !isListUpdate

private fun deletableBy(viewerId: Int?, authorId: Int?, recipientId: Int?, isMessage: Boolean, isAuthorMod: Boolean): Boolean =
    viewerId != null && (authorId == viewerId || (isMessage && recipientId == viewerId && !isAuthorMod))
