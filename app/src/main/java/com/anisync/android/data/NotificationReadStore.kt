package com.anisync.android.data

import android.content.Context
import android.content.SharedPreferences
import com.anisync.android.data.account.AccountStore
import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationReadState
import com.anisync.android.domain.NotificationRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Durable home of [NotificationReadState], one per account.
 *
 * Read state is the device's own record, not a mirror of anything AniList holds, so it lives in
 * SharedPreferences rather than Room: it has to outlive the cache clear, the account switch and the
 * destructive-migration wipe that Room is still configured for.
 *
 * The store also owns the server side of that record. AniList resets an account's unread count
 * only as a side effect of a notifications query carrying `resetNotificationCount`, so that query
 * is fired here, from a scope that outlives the inbox screen, and is retried on the next visit when
 * it fails. Its response doubles as the authority on the newest notification in the account, which
 * is what lets Mark all read work from a filtered tab. It goes out for Mark all read and, since the
 * count is the whole of what AniList keeps, once the last unread row has been read one at a time.
 */
@Singleton
class NotificationReadStore @Inject constructor(
    @ApplicationContext context: Context,
    private val accountStore: AccountStore,
    private val notificationRepository: NotificationRepository,
    private val badgeStore: NotificationBadgeStore
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val flows = mutableMapOf<Int, MutableStateFlow<NotificationReadState>>()

    /**
     * Read state of the account active at the moment of the call. Switching accounts rebuilds every
     * screen ViewModel through `sessionEpoch`, so a holder of this flow never outlives its account.
     */
    fun state(): StateFlow<NotificationReadState> = flowFor(activeAccountId()).asStateFlow()

    /** Re-applies the locally-read count to the badge without opening the inbox. */
    fun syncBadge() {
        badgeStore.setLocalReadCount(flowFor(activeAccountId()).value.readIds.size)
    }

    /** Places the watermark on the first visit of an account this device has no record of. */
    fun anchor(items: List<Notification>, serverUnreadCount: Int, hasMoreItems: Boolean) {
        update { it.anchoredTo(items, serverUnreadCount, hasMoreItems) }
    }

    /**
     * Marks the rows of one opened notification read. [inbox] is everything the screen has loaded,
     * which is what decides whether that was the last unread row.
     */
    fun markRead(items: List<Notification>, inbox: List<Notification>) {
        val accountId = activeAccountId()
        update(accountId) { it.markRead(items) }
        pushResetWhenNothingIsUnread(accountId, inbox)
    }

    /**
     * Reports the inbox as read to AniList once the device has read everything AniList still counts
     * as unread.
     *
     * A single row cannot be reported on its own: `resetNotificationCount` is the only lever
     * AniList offers and it clears the whole count. So the rows are held here until the last of
     * them is read, and only then does the all-or-nothing reset go out. Without it a user who
     * reads their notifications one at a time, having opted out of Mark read on open, clears the
     * inbox here and still sees every one of them unread on the website.
     *
     * Both the loaded rows and the server's figure have to agree that nothing is left, which is
     * what [NotificationReadState.coversUnread] decides.
     */
    private fun pushResetWhenNothingIsUnread(accountId: Int, inbox: List<Notification>) {
        val serverUnread = badgeStore.serverUnreadCount.value ?: return
        if (!flowFor(accountId).value.coversUnread(inbox, serverUnread)) return
        // A reset already in flight covers this one; a failed one is retried on the next visit.
        if (isResetPending(accountId)) return
        setResetPending(accountId, true)
        scope.launch { pushReset(accountId) }
    }

    /**
     * Marks the whole inbox read: locally at once, on AniList as soon as the request lands.
     *
     * [items] only has to cover what the screen is showing. The reset query returns page one
     * unfiltered, so the watermark is corrected to the true newest notification once it answers.
     */
    fun markAllRead(items: List<Notification>) {
        val accountId = activeAccountId()
        update(accountId) { it.markAllRead(items) }
        badgeStore.markedAllRead()
        setResetPending(accountId, true)
        scope.launch { pushReset(accountId) }
    }

    /**
     * Drops the account's record entirely, for read tracking being switched off. Switching it back
     * on then starts from AniList's unread count rather than from a watermark left behind weeks ago.
     */
    fun forget() {
        val accountId = activeAccountId()
        val flow = flowFor(accountId)
        flow.value = NotificationReadState()
        persist(accountId, flow.value)
        setResetPending(accountId, false)
        badgeStore.setLocalReadCount(0)
    }

    /** Retries a reset that never reached AniList, so the badge cannot come back from the dead. */
    fun retryPendingReset() {
        val accountId = activeAccountId()
        if (!isResetPending(accountId)) return
        scope.launch { pushReset(accountId) }
    }

    private suspend fun pushReset(accountId: Int) {
        if (accountId != activeAccountId()) return
        val result = notificationRepository.getNotificationsPage(
            page = 1,
            typeFilter = null,
            resetUnreadCount = true
        )
        if (result !is Result.Success) return
        setResetPending(accountId, false)
        if (accountId != activeAccountId()) return
        // AniList counts everything up to this moment as read, including anything that arrived
        // between the tap and the response, so the watermark follows the reset rather than the tap.
        update(accountId) { it.markAllRead(result.data.items) }
        badgeStore.markedAllRead()
    }

    private fun update(
        accountId: Int = activeAccountId(),
        transform: (NotificationReadState) -> NotificationReadState
    ) {
        val flow = flowFor(accountId)
        while (true) {
            val current = flow.value
            val updated = transform(current)
            if (updated == current) return
            // The reset this store fires lands on its own thread, so a tap and that response can
            // arrive at the same state together.
            if (!flow.compareAndSet(current, updated)) continue
            persist(accountId, updated)
            if (accountId == activeAccountId()) badgeStore.setLocalReadCount(updated.readIds.size)
            return
        }
    }

    @Synchronized
    private fun flowFor(accountId: Int): MutableStateFlow<NotificationReadState> =
        flows.getOrPut(accountId) { MutableStateFlow(load(accountId)) }

    private fun activeAccountId(): Int = accountStore.activeAccount.value?.id ?: NO_ACCOUNT

    private fun load(accountId: Int): NotificationReadState = NotificationReadState(
        anchored = prefs.getBoolean(key(KEY_ANCHORED, accountId), false),
        readThroughCreatedAt = prefs.getInt(key(KEY_READ_THROUGH_AT, accountId), 0),
        readThroughId = prefs.getInt(key(KEY_READ_THROUGH_ID, accountId), 0),
        readIds = prefs.getString(key(KEY_READ_IDS, accountId), null)
            ?.split(',')
            ?.mapNotNullTo(mutableSetOf()) { it.toIntOrNull() }
            ?: emptySet()
    )

    private fun persist(accountId: Int, state: NotificationReadState) {
        prefs.edit()
            .putBoolean(key(KEY_ANCHORED, accountId), state.anchored)
            .putInt(key(KEY_READ_THROUGH_AT, accountId), state.readThroughCreatedAt)
            .putInt(key(KEY_READ_THROUGH_ID, accountId), state.readThroughId)
            .putString(key(KEY_READ_IDS, accountId), state.readIds.joinToString(","))
            .apply()
    }

    private fun isResetPending(accountId: Int): Boolean =
        prefs.getBoolean(key(KEY_RESET_PENDING, accountId), false)

    private fun setResetPending(accountId: Int, pending: Boolean) {
        prefs.edit().putBoolean(key(KEY_RESET_PENDING, accountId), pending).apply()
    }

    private fun key(prefix: String, accountId: Int) = "${prefix}_$accountId"

    private companion object {
        const val PREFS_NAME = "notification_read_state"
        const val KEY_ANCHORED = "anchored"
        const val KEY_READ_THROUGH_AT = "read_through_at"
        const val KEY_READ_THROUGH_ID = "read_through_id"
        const val KEY_READ_IDS = "read_ids"
        const val KEY_RESET_PENDING = "reset_pending"

        /** Signed out, or an account whose id is not known yet. */
        const val NO_ACCOUNT = 0
    }
}
