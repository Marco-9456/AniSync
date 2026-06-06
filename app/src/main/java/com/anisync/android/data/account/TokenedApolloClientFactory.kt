package com.anisync.android.data.account

import com.anisync.android.di.AuthorizationInterceptor
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds [ApolloClient]s bound to a specific account's bearer token, for talking to AniList as an
 * account that is **not** the active one (background notification polling, add-time identity resolve).
 *
 * Each client carries its own `Authorization` header and a small, **isolated** in-memory normalized
 * cache (no SQLite) so cross-account data can't bleed into the active account's persistent cache.
 * It still installs the shared [AuthorizationInterceptor] so per-account requests are paced by the
 * same rate-limit / token-bucket logic — the interceptor skips adding/clearing auth and skips the
 * global session-expired trigger when a request already carries an `Authorization` header.
 *
 * Clients are cached by token (accounts are few and tokens stable); [evict] closes one on removal.
 */
@Singleton
class TokenedApolloClientFactory @Inject constructor(
    private val authorizationInterceptor: AuthorizationInterceptor,
) {
    private val clients = ConcurrentHashMap<String, ApolloClient>()

    fun create(token: String): ApolloClient = clients.getOrPut(token) {
        ApolloClient.Builder()
            .serverUrl(ANILIST_URL)
            .addHttpInterceptor(authorizationInterceptor)
            .addHttpHeader("Authorization", "Bearer $token")
            .normalizedCache(MemoryCacheFactory(maxSizeBytes = MEMORY_CACHE_SIZE))
            .build()
    }

    /** Closes and drops the cached client for [token] (call when its account is removed). */
    fun evict(token: String) {
        clients.remove(token)?.close()
    }

    companion object {
        private const val ANILIST_URL = "https://graphql.anilist.co"
        private const val MEMORY_CACHE_SIZE = 1 * 1024 * 1024 // 1 MB, throwaway
    }
}
