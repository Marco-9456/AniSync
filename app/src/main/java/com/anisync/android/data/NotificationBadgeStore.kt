package com.anisync.android.data

import com.anisync.android.GetViewerQuery
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the authenticated viewer's unread-notification count for the
 * inbox badge. Source of truth is AniList's `Viewer.unreadNotificationCount`,
 * but writers can clear it optimistically when the inbox opens (server
 * resets it via `resetNotificationCount=true` on the next notifications
 * fetch) and debug callers can bump it locally so the UI is testable
 * without waiting for real notifications.
 */
@Singleton
class NotificationBadgeStore @Inject constructor(
    private val apolloClient: ApolloClient
) {
    /** AniList-truth count fetched from `Viewer.unreadNotificationCount`. */
    private val _serverCount = MutableStateFlow(0)

    /**
     * Local-only count for debug testing. Decoupled from the server
     * count so refreshes don't clobber a fake bump — that would mask
     * the persistence behaviour we want the test to exercise (real
     * unreads only clear when the inbox is opened, never on a plain
     * profile resume).
     */
    private val _debugCount = MutableStateFlow(0)

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    /**
     * Update from a count already obtained out-of-band (e.g. piggy-backed on
     * a `GetUserProfile` query that included `Viewer.unreadNotificationCount`).
     * Lets us skip the separate `GetViewer` round-trip when the profile
     * refresh already fetched the value — see Horizon A patch A2.
     */
    fun setFromServer(count: Int) {
        _serverCount.value = count.coerceAtLeast(0)
        recompute()
    }

    /** Network refresh; keeps the previous value on failure (offline, rate-limit). */
    suspend fun refresh() {
        try {
            val response = apolloClient
                .query(GetViewerQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
            val count = response.data?.Viewer?.unreadNotificationCount ?: return
            _serverCount.value = count.coerceAtLeast(0)
            recompute()
        } catch (_: Exception) {
            // Keep last-known value
        }
    }

    /**
     * Optimistic clear when the user opens the inbox; reconciles with
     * the server on the next refresh. Also drops any debug bump so a
     * test cycle (bump → open inbox) returns to the zero state cleanly.
     */
    fun clearOptimistically() {
        _serverCount.value = 0
        _debugCount.value = 0
        recompute()
    }

    /** Clears all counts when switching accounts so the badge doesn't carry over. */
    fun reset() {
        _serverCount.value = 0
        _debugCount.value = 0
        recompute()
    }

    /** Debug-only: simulate a new unread notification so the badge can be verified. */
    fun bumpForDebug(by: Int = 1) {
        _debugCount.value = (_debugCount.value + by).coerceAtLeast(0)
        recompute()
    }

    private fun recompute() {
        _unreadCount.value = (_serverCount.value + _debugCount.value).coerceAtLeast(0)
    }
}
