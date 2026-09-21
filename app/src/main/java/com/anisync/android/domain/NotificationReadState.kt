package com.anisync.android.domain

/**
 * Which of an account's notifications have been read.
 *
 * AniList has no per-notification read flag. All it offers is `Viewer.unreadNotificationCount` and
 * a `resetNotificationCount` side effect on the notifications query, both of them all-or-nothing,
 * so read state has to be kept on the device.
 *
 * It is kept as a watermark plus the few ids read out of order above it. Notifications are
 * immutable and always arrive newest first, so "read" is a position in that order rather than a set
 * that would have to hold every id the account has ever seen. Everything at or below
 * [readThroughCreatedAt] / [readThroughId] is read, and the watermark only ever moves forward.
 * That is what keeps an already-read notification read once newer ones arrive, and across process
 * death, since the whole state is four persisted values.
 *
 * Ordering is the (createdAt, id) pair rather than createdAt alone: notifications that share a
 * second need a tie-break, and AniList ids ascend with creation time.
 */
data class NotificationReadState(
    /**
     * True once the watermark belongs to the user rather than to a guess.
     *
     * A fresh install knows nothing, so the first inbox load places the watermark from the server's
     * unread count: the newest `count` rows are unread and everything older is read. That guess is
     * re-made on every page until it is complete, which is what [anchoredTo] decides. Marking all
     * read ends the guessing for good.
     */
    val anchored: Boolean = false,
    val readThroughCreatedAt: Int = 0,
    val readThroughId: Int = 0,
    /** Read individually, above the watermark. Cleared whenever the watermark passes them. */
    val readIds: Set<Int> = emptySet()
) {
    fun isRead(notification: Notification): Boolean =
        notification.id in readIds || !isAboveWatermark(notification)

    fun isUnread(notification: Notification): Boolean = !isRead(notification)

    private fun isAboveWatermark(notification: Notification): Boolean =
        notification.createdAt > readThroughCreatedAt ||
            (notification.createdAt == readThroughCreatedAt && notification.id > readThroughId)

    /**
     * Whether reading has caught up with everything AniList still counts as unread.
     *
     * That is the one moment the app can report reading back: `resetNotificationCount` clears the
     * whole count or nothing, so a row read on its own has to wait for the last of them. Both
     * halves have to agree, because either can be behind on its own. [serverUnreadCount] is
     * refreshed on resume rather than continuously, and a notification that arrived after the last
     * page load is only in that figure, not in [inbox].
     */
    fun coversUnread(inbox: List<Notification>, serverUnreadCount: Int): Boolean =
        serverUnreadCount > 0 &&
            readIds.size >= serverUnreadCount &&
            inbox.none { isUnread(it) }

    /** Marks single rows read without moving the watermark, so nothing older is swept up. */
    fun markRead(items: List<Notification>): NotificationReadState {
        val added = items.filter { isUnread(it) }
        if (added.isEmpty()) return this
        return copy(readIds = capped(readIds + added.map { it.id }))
    }

    /**
     * Moves the watermark to the newest of [items], which marks everything older read in one value.
     *
     * The watermark never moves backwards, so a shorter or staler list than the one already read
     * through is a no-op. Ids above the new watermark survive, as a list that omits a row the user
     * read individually must not turn it unread again.
     */
    fun markAllRead(items: List<Notification>): NotificationReadState {
        val newest = items.maxWithOrNull(OLDEST_FIRST)
        if (newest == null || !isAboveWatermark(newest)) {
            return copy(anchored = true, readIds = readIds.filterTo(mutableSetOf()) { it > readThroughId })
        }
        return copy(
            anchored = true,
            readThroughCreatedAt = newest.createdAt,
            readThroughId = newest.id,
            readIds = readIds.filterTo(mutableSetOf()) { it > newest.id }
        )
    }

    /**
     * Places the watermark from the server's [unreadCount], the one number AniList gives for an
     * account whose read state this device has never recorded.
     *
     * The count can reach past the rows loaded so far. The watermark then sits just under the
     * oldest loaded row, which marks every loaded row unread, and stays unanchored so the next page
     * can push it further down. Only a page that reaches past the count, or the end of the inbox,
     * settles it.
     */
    fun anchoredTo(
        items: List<Notification>,
        unreadCount: Int,
        hasMoreItems: Boolean
    ): NotificationReadState {
        if (anchored) return this
        val ordered = items.sortedWith(NEWEST_FIRST)
        val boundary = when {
            ordered.isEmpty() -> return copy(anchored = !hasMoreItems)
            unreadCount <= 0 -> ordered.first().let { it.createdAt to it.id }
            ordered.size > unreadCount -> ordered[unreadCount].let { it.createdAt to it.id }
            else -> ordered.last().let { it.createdAt to it.id - 1 }
        }
        val settled = unreadCount <= 0 || ordered.size > unreadCount || !hasMoreItems
        return copy(
            anchored = settled,
            readThroughCreatedAt = boundary.first,
            readThroughId = boundary.second,
            readIds = readIds.filterTo(mutableSetOf()) { it > boundary.second }
        )
    }

    companion object {
        /**
         * Ceiling on individually-read ids. Reaching it takes hundreds of taps without one Mark all
         * read, and the oldest go first, so the cost of the cap is an old row turning new again.
         */
        const val MAX_TRACKED_IDS = 400

        val NEWEST_FIRST: Comparator<Notification> =
            compareByDescending<Notification> { it.createdAt }.thenByDescending { it.id }

        private val OLDEST_FIRST: Comparator<Notification> =
            compareBy<Notification> { it.createdAt }.thenBy { it.id }

        private fun capped(ids: Set<Int>): Set<Int> =
            if (ids.size <= MAX_TRACKED_IDS) ids
            else ids.sortedDescending().take(MAX_TRACKED_IDS).toSet()
    }
}
