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
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    /** Network refresh; keeps the previous value on failure (offline, rate-limit). */
    suspend fun refresh() {
        try {
            val response = apolloClient
                .query(GetViewerQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
            val count = response.data?.Viewer?.unreadNotificationCount ?: return
            _unreadCount.value = count.coerceAtLeast(0)
        } catch (_: Exception) {
            // Keep last-known value
        }
    }

    /** Optimistic clear when the user opens the inbox; reconciles on next refresh. */
    fun clearOptimistically() {
        _unreadCount.value = 0
    }

    /** Debug-only: simulate a new unread notification so the badge can be verified. */
    fun bumpForDebug(by: Int = 1) {
        _unreadCount.value = (_unreadCount.value + by).coerceAtLeast(0)
    }
}
