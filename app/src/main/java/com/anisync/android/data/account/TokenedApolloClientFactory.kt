package com.anisync.android.data.account

import com.anisync.android.cache.Cache.cache
import com.anisync.android.data.network.AniListErrorInterceptor
import com.anisync.android.data.network.AniListHttpInterceptor
import com.anisync.android.data.network.AniListIdentity
import com.anisync.android.data.network.RequestCoalescer
import com.anisync.android.data.network.TokenScopedContext
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds [ApolloClient]s bound to a specific account's bearer token, for talking to AniList as an
 * account that is **not** the active one (background notification polling, add-time identity resolve).
 *
 * Each client carries its own `Authorization` header and a small, **isolated** in-memory normalized
 * cache (no SQLite) so cross-account data can't bleed into the active account's persistent cache.
 * It still installs the shared network interceptors so per-account requests are paced by the same
 * [com.anisync.android.data.network.RateLimitGate] as everything else. [TokenScopedContext] marks
 * the client so a rejected token fails that one account instead of ending the active session, and
 * the HTTP interceptor leaves the `Authorization` header these clients already carry alone.
 *
 * Clients are cached by token (accounts are few and tokens stable); [evict] closes one on removal.
 */
@Singleton
class TokenedApolloClientFactory @Inject constructor(
    private val httpInterceptor: AniListHttpInterceptor,
    private val errorInterceptor: AniListErrorInterceptor,
) {
    private val clients = ConcurrentHashMap<String, ApolloClient>()

    fun create(token: String): ApolloClient = clients.getOrPut(token) {
        ApolloClient.Builder()
            .serverUrl(AniListIdentity.ENDPOINT)
            .addHttpInterceptor(httpInterceptor)
            // One coalescer per client: a shared one would key two accounts' identical queries the
            // same and hand one account the other's viewer.
            .addInterceptor(RequestCoalescer(), ApolloInterceptor.InsertionPoint.BeforeNetwork)
            .addInterceptor(errorInterceptor, ApolloInterceptor.InsertionPoint.BeforeNetwork)
            .addHttpHeader(AniListIdentity.HEADER_AUTHORIZATION, "Bearer $token")
            .addExecutionContext(TokenScopedContext)
            .cache(MemoryCacheFactory(maxSizeBytes = MEMORY_CACHE_SIZE))
            .build()
    }

    /** Closes and drops the cached client for [token] (call when its account is removed). */
    fun evict(token: String) {
        clients.remove(token)?.close()
    }

    companion object {
        private const val MEMORY_CACHE_SIZE = 1 * 1024 * 1024 // 1 MB, throwaway
    }
}
