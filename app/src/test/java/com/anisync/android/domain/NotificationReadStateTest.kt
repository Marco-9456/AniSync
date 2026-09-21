package com.anisync.android.domain

import com.anisync.android.type.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationReadStateTest {

    private fun notification(id: Int, createdAt: Int = id): Notification = FollowingNotification(
        id = id,
        type = NotificationType.FOLLOWING,
        createdAt = createdAt,
        context = "followed you",
        user = null
    )

    /** Newest first, the order AniList returns and the inbox keeps. */
    private val inbox = listOf(70, 60, 50, 40, 30).map { notification(it) }

    @Test
    fun `an account with no record marks the newest rows unread`() {
        val state = NotificationReadState().anchoredTo(inbox, unreadCount = 2, hasMoreItems = true)

        assertTrue(state.anchored)
        assertEquals(listOf(70, 60), inbox.filter { state.isUnread(it) }.map { it.id })
    }

    @Test
    fun `no unread count means the whole inbox has been read`() {
        val state = NotificationReadState().anchoredTo(inbox, unreadCount = 0, hasMoreItems = true)

        assertTrue(state.anchored)
        assertTrue(inbox.none { state.isUnread(it) })
    }

    @Test
    fun `a count reaching past the loaded rows waits for the next page`() {
        val firstPage = NotificationReadState()
            .anchoredTo(inbox.take(2), unreadCount = 4, hasMoreItems = true)

        assertFalse(firstPage.anchored)
        assertTrue(inbox.take(2).all { firstPage.isUnread(it) })

        val secondPage = firstPage.anchoredTo(inbox, unreadCount = 4, hasMoreItems = true)

        assertTrue(secondPage.anchored)
        assertEquals(listOf(70, 60, 50, 40), inbox.filter { secondPage.isUnread(it) }.map { it.id })
    }

    @Test
    fun `the end of the inbox settles a count that reaches past it`() {
        val state = NotificationReadState()
            .anchoredTo(inbox, unreadCount = 9, hasMoreItems = false)

        assertTrue(state.anchored)
        assertTrue(inbox.all { state.isUnread(it) })
    }

    /** The bug this replaced: a later notification used to drag read rows back into the New list. */
    @Test
    fun `rows read once stay read when a newer notification arrives`() {
        val state = NotificationReadState()
            .anchoredTo(inbox, unreadCount = 2, hasMoreItems = false)
            .markAllRead(inbox)

        val arrived = notification(80)

        assertTrue(state.isUnread(arrived))
        assertTrue(inbox.none { state.isUnread(it) })
    }

    @Test
    fun `an anchored state is never re-derived from the unread count`() {
        val read = NotificationReadState().markAllRead(inbox)
        val reanchored = read.anchoredTo(inbox, unreadCount = 5, hasMoreItems = false)

        assertEquals(read, reanchored)
        assertTrue(inbox.none { reanchored.isUnread(it) })
    }

    @Test
    fun `the watermark never moves backwards`() {
        val read = NotificationReadState().markAllRead(inbox)
        val staleList = inbox.drop(3)

        assertEquals(read.readThroughId, read.markAllRead(staleList).readThroughId)
    }

    @Test
    fun `reading one row leaves the others alone`() {
        val state = NotificationReadState()
            .anchoredTo(inbox, unreadCount = 3, hasMoreItems = false)
            .markRead(listOf(notification(60)))

        assertEquals(listOf(70, 50), inbox.filter { state.isUnread(it) }.map { it.id })
        assertEquals(setOf(60), state.readIds)
    }

    @Test
    fun `marking everything read clears the ids it makes redundant`() {
        val state = NotificationReadState()
            .anchoredTo(inbox, unreadCount = 3, hasMoreItems = false)
            .markRead(listOf(notification(60)))
            .markAllRead(inbox)

        assertTrue(state.readIds.isEmpty())
        assertTrue(inbox.none { state.isUnread(it) })
    }

    @Test
    fun `ids above a new watermark survive a list that omits them`() {
        val state = NotificationReadState()
            .anchoredTo(inbox, unreadCount = 5, hasMoreItems = false)
            .markRead(listOf(notification(70)))
            .markAllRead(inbox.drop(1))

        assertEquals(setOf(70), state.readIds)
        assertTrue(state.isRead(notification(70)))
    }

    @Test
    fun `notifications sharing a second are separated by id`() {
        val sameSecond = listOf(notification(12, createdAt = 500), notification(11, createdAt = 500))
        val state = NotificationReadState()
            .anchoredTo(sameSecond, unreadCount = 1, hasMoreItems = false)

        assertTrue(state.isUnread(sameSecond[0]))
        assertTrue(state.isRead(sameSecond[1]))
    }

    @Test
    fun `individually read ids are capped at the newest`() {
        val many = (1..NotificationReadState.MAX_TRACKED_IDS + 50).map { notification(it) }
        val state = NotificationReadState()
            .anchoredTo(many, unreadCount = many.size, hasMoreItems = false)
            .markRead(many)

        assertEquals(NotificationReadState.MAX_TRACKED_IDS, state.readIds.size)
        assertTrue(state.readIds.contains(many.last().id))
        assertFalse(state.readIds.contains(many.first().id))
    }

    @Test
    fun `an empty inbox with nothing more to load is settled`() {
        val state = NotificationReadState()
            .anchoredTo(emptyList(), unreadCount = 0, hasMoreItems = false)

        assertTrue(state.anchored)
    }

    @Test
    fun `reading the last unread row catches up with the server count`() {
        val anchored = NotificationReadState().anchoredTo(inbox, unreadCount = 2, hasMoreItems = true)
        val unread = inbox.filter { anchored.isUnread(it) }

        val partly = anchored.markRead(unread.take(1))
        assertFalse(partly.coversUnread(inbox, serverUnreadCount = 2))

        val all = partly.markRead(unread)
        assertTrue(all.coversUnread(inbox, serverUnreadCount = 2))
    }

    @Test
    fun `a row the server counts but the device has not loaded holds the reset back`() {
        val state = NotificationReadState()
            .anchoredTo(inbox, unreadCount = 2, hasMoreItems = true)
            .let { it.markRead(inbox.filter(it::isUnread)) }

        // AniList counts three unread; the third arrived after the last page load.
        assertFalse(state.coversUnread(inbox, serverUnreadCount = 3))
    }

    @Test
    fun `a row the device shows unread holds the reset back past a stale count`() {
        val arrived = notification(80)
        val loaded = listOf(arrived) + inbox
        val state = NotificationReadState()
            .anchoredTo(inbox, unreadCount = 2, hasMoreItems = true)
            .let { it.markRead(inbox.filter(it::isUnread)) }

        // The count has not caught up with the row that arrived, so it still reads 2.
        assertTrue(state.isUnread(arrived))
        assertFalse(state.coversUnread(loaded, serverUnreadCount = 2))
    }

    @Test
    fun `an inbox the server calls read needs no reset`() {
        val state = NotificationReadState().anchoredTo(inbox, unreadCount = 0, hasMoreItems = false)

        assertFalse(state.coversUnread(inbox, serverUnreadCount = 0))
    }
}
