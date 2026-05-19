package com.anisync.android.data.fetcher

import com.anisync.android.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

/**
 * Generic stale-while-revalidate fetcher backed by a per-resource cache and
 * an inflight tracker. Repositories in Horizon B compose one [ResourceFetcher]
 * per resource (profile, social, reviews, stats, library) and expose
 * observe/refresh through it instead of hand-rolling Apollo + Room glue.
 *
 * Key features:
 * - SWR via [CachePolicy.CacheAndNetwork]: emit cache instantly, then network.
 * - In-flight dedup via [InflightTracker]: concurrent identical calls share
 *   one network request.
 * - Per-key [FetchCooldown]: rate-limits refresh calls. Force-refresh bypasses.
 * - Staleness tracking via injected [Clock] so the same primitive works in
 *   tests with virtual time.
 *
 * @param K cache key (e.g. userId for profile, page for paginated reads)
 * @param T cached value type
 * @param readCache called to fetch the locally cached value (Room DAO read).
 *   Return `null` if no cache entry. Suspending — Room flows are async.
 * @param writeCache called after a successful network fetch (Room DAO upsert).
 * @param fetchNetwork called to fetch fresh data over the network (Apollo).
 *   Should throw on error so [observe] can emit [ResourceState.Error].
 * @param staleAfterMs default staleness threshold for [CachePolicy.CacheAndNetwork].
 *   Older cached values get `isStale = true` in the emission.
 * @param cooldownMs minimum gap between non-force [refresh] calls per key.
 * @param clock time source; default uses [SystemClock.elapsedRealtime].
 */
class ResourceFetcher<K : Any, T : Any>(
    private val readCache: suspend (K) -> T?,
    private val writeCache: suspend (K, T) -> Unit,
    private val fetchNetwork: suspend (K) -> T,
    private val staleAfterMs: Long = DEFAULT_STALE_AFTER_MS,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val clock: Clock = Clock.System
) {

    private val inflight = InflightTracker()

    /** When the network last successfully wrote a value for this key. */
    private val lastFetchedAt = ConcurrentHashMap<K, Long>()

    /** Per-key cooldown gate for [refresh]. */
    private val cooldowns = ConcurrentHashMap<K, FetchCooldown>()

    /**
     * Observe a resource. Emissions depend on [policy]:
     *
     * - [CachePolicy.CacheOnly]: at most one emission — the cached value, or
     *   nothing if cache is empty.
     * - [CachePolicy.NetworkOnly]: Loading → Data(network) or Loading → Error.
     * - [CachePolicy.CacheFirst]: Data(cache) if present, else Loading →
     *   Data(network) or Error.
     * - [CachePolicy.CacheAndNetwork]: Data(cache) or Loading, then Data(network)
     *   or Error(staleValue = cached).
     * - [CachePolicy.CacheAndNetworkIfStale]: same as CacheAndNetwork but only
     *   issues the network fetch when the cache is older than the threshold.
     */
    fun observe(key: K, policy: CachePolicy): Flow<ResourceState<T>> = flow {
        val cached = runCatching { readCache(key) }.getOrNull()

        when (policy) {
            CachePolicy.CacheOnly -> {
                if (cached != null) emit(dataEmission(key, cached, fromCache = true))
            }

            CachePolicy.NetworkOnly -> emitNetwork(key, fallback = cached)

            CachePolicy.CacheFirst -> {
                if (cached != null) {
                    emit(dataEmission(key, cached, fromCache = true))
                } else {
                    emitNetwork(key, fallback = null)
                }
            }

            CachePolicy.CacheAndNetwork -> {
                if (cached != null) {
                    emit(dataEmission(key, cached, fromCache = true))
                } else {
                    emit(ResourceState.Loading)
                }
                emitNetwork(key, fallback = cached, skipLoading = true)
            }

            is CachePolicy.CacheAndNetworkIfStale -> {
                val isStale = cached == null || isStaleAfter(key, policy.staleAfterMs)
                if (cached != null) {
                    emit(ResourceState.Data(cached, fromCache = true, isStale = isStale))
                    if (!isStale) return@flow
                } else {
                    emit(ResourceState.Loading)
                }
                emitNetwork(key, fallback = cached, skipLoading = true)
            }
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<ResourceState<T>>.emitNetwork(
        key: K,
        fallback: T?,
        skipLoading: Boolean = false
    ) {
        if (!skipLoading) emit(ResourceState.Loading)
        try {
            val fresh = doFetch(key)
            emit(ResourceState.Data(fresh, fromCache = false, isStale = false))
        } catch (e: Throwable) {
            emit(ResourceState.Error(e, staleValue = fallback))
        }
    }

    private fun dataEmission(key: K, value: T, fromCache: Boolean): ResourceState.Data<T> =
        ResourceState.Data(value, fromCache = fromCache, isStale = isStaleAfter(key, staleAfterMs))

    /**
     * Force a fetch (writing the cache) and return its [Result]. Honours the
     * per-key cooldown unless [force] is true. Used by pull-to-refresh and
     * by SelectTab handlers — anywhere the caller wants a guaranteed network
     * round-trip with bounded retry.
     */
    suspend fun refresh(key: K, force: Boolean = false): Result<T> {
        if (!force) {
            val gate = cooldowns.computeIfAbsent(key) {
                FetchCooldown(gestureFloorMs = cooldownMs, autoFloorMs = cooldownMs, clock = clock)
            }
            if (!gate.shouldFetch(userInitiated = true)) {
                // Explicit signal so callers can distinguish "no fetch happened"
                // from "fetch failed". Cached value is intentionally NOT returned
                // here — that's what observe(CacheOnly) is for.
                return Result.Error("Refresh suppressed by cooldown", code = COOLDOWN_ERROR_CODE)
            }
        }
        return try {
            Result.Success(doFetch(key))
        } catch (e: Throwable) {
            Result.Error(
                message = e.message ?: "Fetch failed",
                exception = e
            )
        }
    }

    /**
     * Drop bookkeeping for [key]. Does not clear external cache — callers
     * must do that through their DAO if they want the value gone.
     */
    fun invalidate(key: K) {
        lastFetchedAt.remove(key)
        cooldowns.remove(key)
    }

    private suspend fun doFetch(key: K): T = inflight.deduplicate("rf:$key") {
        val data = fetchNetwork(key)
        writeCache(key, data)
        lastFetchedAt[key] = clock.nowMs()
        data
    }

    private fun isStaleAfter(key: K, threshold: Long): Boolean {
        val ts = lastFetchedAt[key] ?: return true
        return clock.nowMs() - ts >= threshold
    }

    companion object {
        const val DEFAULT_STALE_AFTER_MS: Long = 5 * 60_000L
        const val DEFAULT_COOLDOWN_MS: Long = 15_000L

        /**
         * Sentinel error code returned from [refresh] when the per-key cooldown
         * suppressed the network call. Callers can pattern-match on this to
         * distinguish a real failure from a "skip, you just refreshed" outcome.
         */
        const val COOLDOWN_ERROR_CODE: Int = -100
    }
}
