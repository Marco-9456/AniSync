package com.anisync.android.presentation.notifications

import com.anisync.android.domain.FollowingNotification
import com.anisync.android.domain.Notification
import com.anisync.android.type.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnreadWindowTest {

    private fun notification(id: Int, createdAt: Int = id): Notification = FollowingNotification(
        id = id,
        type = NotificationType.FOLLOWING,
        createdAt = createdAt,
        context = "followed you",
        user = null
    )

    /** Newest first, the order AniList returns and the list keeps. */
    private val page = listOf(70, 60, 50, 40, 30).map { notification(it) }

    @Test
    fun `takes the newest rows for the unread count`() {
        assertEquals(setOf(70, 60), unreadWindow(emptySet(), page, count = 2))
    }

    @Test
    fun `zero unread marks nothing`() {
        assertTrue(unreadWindow(emptySet(), page, count = 0).isEmpty())
    }

    @Test
    fun `a count past the end of the page marks everything loaded so far`() {
        assertEquals(setOf(70, 60, 50, 40, 30), unreadWindow(emptySet(), page, count = 8))
    }

    @Test
    fun `later pages widen a window the first page could not fill`() {
        val firstPage = page.take(2)
        val afterFirst = unreadWindow(emptySet(), firstPage, count = 4)
        val afterSecond = unreadWindow(afterFirst, page, count = 4)
        assertEquals(setOf(70, 60, 50, 40), afterSecond)
    }

    @Test
    fun `a notification arriving mid-visit joins the window instead of evicting the oldest`() {
        val opened = unreadWindow(emptySet(), page, count = 2)
        val refreshed = listOf(notification(80)) + page
        assertEquals(setOf(80, 70, 60), unreadWindow(opened, refreshed, count = 2))
    }

    @Test
    fun `a fold is unread when any member is`() {
        val entries = listOf(
            NotificationEntry(
                key = "fold",
                representative = notification(60),
                all = listOf(notification(60), notification(20)),
                actors = emptyList()
            ),
            NotificationEntry(
                key = "old",
                representative = notification(20),
                all = listOf(notification(20)),
                actors = emptyList()
            )
        )
        val marked = entries.markUnread(setOf(60))
        assertTrue(marked[0].isUnread)
        assertFalse(marked[1].isUnread)
    }

    @Test
    fun `an empty window clears the flags again`() {
        val entry = NotificationEntry(
            key = "one",
            representative = notification(60),
            all = listOf(notification(60)),
            actors = emptyList(),
            isUnread = true
        )
        assertFalse(listOf(entry).markUnread(emptySet()).single().isUnread)
    }
}
