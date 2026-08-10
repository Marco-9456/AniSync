package com.anisync.android.presentation.notifications

import com.anisync.android.domain.Notification

/**
 * Which notifications count as unread.
 *
 * AniList has no per-notification read flag, only `Viewer.unreadNotificationCount`, so the unread
 * set is the newest [count] rows as they stood the moment the inbox opened. That number is read off
 * [com.anisync.android.data.NotificationBadgeStore] before the first page load asks the server to
 * reset it, which is why nothing new has to be fetched.
 *
 * The result is unioned with [previous] and never shrinks. Two cases depend on that: a first page
 * shorter than [count] widens the window as later pages arrive, and a notification that lands
 * mid-visit joins the window instead of pushing the oldest unread row back to read while the user
 * is looking at it.
 */
fun unreadWindow(previous: Set<Int>, newestFirst: List<Notification>, count: Int): Set<Int> {
    if (count <= 0) return previous
    return previous + newestFirst.take(count).map { it.id }
}

/**
 * Flags the rows holding an unread notification. A fold of likes or replies is unread when any
 * member is, and since members are always older than the representative, unread rows stay a prefix
 * of the list.
 */
fun List<NotificationEntry>.markUnread(unreadIds: Set<Int>): List<NotificationEntry> = map { entry ->
    val unread = unreadIds.isNotEmpty() && entry.all.any { it.id in unreadIds }
    if (unread == entry.isUnread) entry else entry.copy(isUnread = unread)
}
