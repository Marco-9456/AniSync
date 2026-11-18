package com.anisync.android.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPermissionsTest {

    private val viewer = 7
    private val other = 9

    private fun activity(type: ActivityType, author: Int, recipient: Int? = null, mod: Boolean = false) =
        UserActivity(id = 1, type = type, timestamp = 0L, userId = author, recipientId = recipient, isAuthorMod = mod)

    @Test
    fun `the author can delete and edit a post`() {
        val post = activity(ActivityType.TEXT, author = viewer)
        assertTrue(post.canDelete(viewer))
        assertTrue(post.canEdit(viewer))
    }

    @Test
    fun `a list update can be deleted but never edited`() {
        val update = activity(ActivityType.MEDIA_LIST, author = viewer)
        assertTrue(update.canDelete(viewer))
        assertFalse(update.canEdit(viewer))
    }

    @Test
    fun `the recipient can delete a message on their profile but not edit it`() {
        val message = activity(ActivityType.MESSAGE, author = other, recipient = viewer)
        assertTrue(message.canDelete(viewer))
        assertFalse(message.canEdit(viewer))
    }

    @Test
    fun `a message from a moderator stays`() {
        assertFalse(activity(ActivityType.MESSAGE, author = other, recipient = viewer, mod = true).canDelete(viewer))
    }

    @Test
    fun `someone else's post and a signed out viewer get neither`() {
        val post = activity(ActivityType.TEXT, author = other)
        assertFalse(post.canDelete(viewer))
        assertFalse(post.canEdit(viewer))
        assertFalse(activity(ActivityType.TEXT, author = viewer).canDelete(null))
    }

    @Test
    fun `the activity page follows the same rule`() {
        fun detail(author: Int, recipient: Int?, isMessage: Boolean, isListUpdate: Boolean = false) = ActivityDetail(
            id = 1, body = "", createdAt = 0L, likeCount = 0, isLiked = false, replyCount = 0, siteUrl = null,
            isMessage = isMessage, isListUpdate = isListUpdate, isPrivate = false, authorId = author, authorName = "",
            authorAvatarUrl = null, recipientId = recipient, recipientName = null, recipientAvatarUrl = null, replies = emptyList()
        )
        assertFalse(detail(author = viewer, recipient = null, isMessage = false, isListUpdate = true).canEdit(viewer))
        assertTrue(detail(author = viewer, recipient = other, isMessage = true).canEdit(viewer))
        assertTrue(detail(author = other, recipient = viewer, isMessage = true).canDelete(viewer))
        assertFalse(detail(author = other, recipient = viewer, isMessage = true).canEdit(viewer))
    }
}
