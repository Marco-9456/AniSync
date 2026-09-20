package com.anisync.android.data

import com.anisync.android.data.network.RequestPriority
import com.anisync.android.data.network.withRequestPriority
import com.anisync.android.GetViewerQuery
import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.doNotStore
import com.apollographql.cache.normalized.fetchPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The unread count behind the Profile tab badge.
 *
 * AniList's `Viewer.unreadNotificationCount` is the only figure it serves, and it clears all at
 * once or not at all. Rows read one at a time therefore stay in that figure until the whole inbox
 * is marked read, so the badge subtracts what [NotificationReadStore] has recorded as read locally.
 *
 * Every server figure is stamped with the moment its request left. One that set off before the
 * user marked the inbox read is describing a state the user has already moved past, and is dropped
 * rather than allowed to put the badge back.
 */
@Singleton
class NotificationBadgeStore @Inject constructor(
    private val apolloClient: ApolloClient
) {
    /** AniList's figure, or null until one has been fetched. */
    private val _serverCount = MutableStateFlow<Int?>(null)
    val serverUnreadCount: StateFlow<Int?> = _serverCount.asStateFlow()

    /** Rows AniList still counts as unread that this device has already marked read. */
    private val _localReadCount = MutableStateFlow(0)

    /**
     * Local-only count for debug testing. Decoupled from the server
     * count so refreshes don't clobber a fake bump — that would mask
     * the persistence behaviour we want the test to exercise (real
     * unreads only clear when the user marks them read, never on a plain
     * profile resume or a visit to the inbox).
     */
    private val _debugCount = MutableStateFlow(0)

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    /** When the user last marked the inbox read, for judging figures that were already in flight. */
    @Volatile
    private var markedReadAt: Long = 0

    /**
     * Update from a count already obtained out-of-band (e.g. piggy-backed on
     * a `GetUserProfile` query that included `Viewer.unreadNotificationCount`).
     * Lets us skip the separate `GetViewer` round-trip when the profile
     * refresh already fetched the value.
     *
     * [requestStartedAt] is when that request left the device. Callers serving a figure from cache
     * must not call this at all, as a cached figure has no such moment and can be any age.
     */
    fun setFromServer(count: Int, requestStartedAt: Long) {
        if (requestStartedAt < markedReadAt) return
        _serverCount.value = count.coerceAtLeast(0)
        recompute()
    }

    /**
     * Network refresh; keeps the previous value on failure (offline, rate-limit).
     *
     * Fires on every resume of the main screen, so it is tagged background: a badge count is never
     * worth a request the screen the user just opened is about to need.
     */
    suspend fun refresh(): Unit = withRequestPriority(RequestPriority.Background) {
        val startedAt = System.currentTimeMillis()
        try {
            val response = apolloClient
                .query(GetViewerQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .doNotStore(true)
                .execute()
            val count = response.data?.Viewer?.unreadNotificationCount
                ?: return@withRequestPriority
            setFromServer(count, startedAt)
        } catch (_: Exception) {
            // Keep last-known value
        }
    }

    /**
     * The whole inbox was marked read. Zeroes the badge and discards any figure still in flight,
     * because AniList's own reset rides on a request that has not answered yet.
     */
    fun markedAllRead() {
        markedReadAt = System.currentTimeMillis()
        _serverCount.value = 0
        _localReadCount.value = 0
        _debugCount.value = 0
        recompute()
    }

    /** How many of AniList's unread rows this device has already marked read one by one. */
    fun setLocalReadCount(count: Int) {
        _localReadCount.value = count.coerceAtLeast(0)
        recompute()
    }

    /** Clears all counts when switching accounts so the badge doesn't carry over. */
    fun reset() {
        markedReadAt = 0
        _serverCount.value = null
        _localReadCount.value = 0
        _debugCount.value = 0
        recompute()
    }

    /** Debug-only: simulate a new unread notification so the badge can be verified. */
    fun bumpForDebug(by: Int = 1) {
        _debugCount.value = (_debugCount.value + by).coerceAtLeast(0)
        recompute()
    }

    private fun recompute() {
        val server = (_serverCount.value ?: 0) - _localReadCount.value
        _unreadCount.value = server.coerceAtLeast(0) + _debugCount.value
    }
}
