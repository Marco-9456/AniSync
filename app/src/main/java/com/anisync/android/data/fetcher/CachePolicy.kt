package com.anisync.android.data.fetcher

/**
 * Cache strategy for a single observe() call on [ResourceFetcher].
 *
 * Horizon B's richer model — distinguishes between "cache only" (offline),
 * "network only" (force fresh), and SWR variants where both cache and
 * network can emit. The Horizon A `com.anisync.android.domain.CachePolicy`
 * enum is a flatter subset and will be deprecated when the per-resource
 * repositories migrate to this fetcher.
 */
sealed interface CachePolicy {
    /** Read cache, emit if present. Never hit network. Useful offline. */
    data object CacheOnly : CachePolicy

    /** Skip cache, fetch network, write cache, emit. */
    data object NetworkOnly : CachePolicy

    /** Emit cache if present; otherwise fetch network. No revalidation. */
    data object CacheFirst : CachePolicy

    /**
     * Emit cache immediately (if present), then fetch network in parallel
     * and emit the fresh value. The classic stale-while-revalidate pattern.
     */
    data object CacheAndNetwork : CachePolicy

    /**
     * Like [CacheAndNetwork] but only revalidates if the cached value is
     * older than [staleAfterMs]. Lets callers cap network traffic on
     * resources that change rarely without giving up freshness entirely.
     */
    data class CacheAndNetworkIfStale(val staleAfterMs: Long) : CachePolicy
}
