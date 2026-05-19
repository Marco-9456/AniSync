package com.anisync.android.data.fetcher

/**
 * Emission type for [ResourceFetcher.observe]. Distinguishes loading,
 * data (with provenance), and error states. UI layers can react to
 * `fromCache` to show "stale" badges or to `isStale` to trigger background
 * revalidation.
 */
sealed interface ResourceState<out T> {
    data object Loading : ResourceState<Nothing>

    data class Data<T>(
        val value: T,
        /** True if this emission came from the cache, false if from the network. */
        val fromCache: Boolean,
        /** True if the cached value is older than the configured staleAfter window. */
        val isStale: Boolean
    ) : ResourceState<T>

    /**
     * Network fetch failed. [staleValue] carries the last-known cached value
     * (when available) so the UI can keep rendering it instead of clearing.
     */
    data class Error(
        val cause: Throwable,
        val staleValue: Any? = null
    ) : ResourceState<Nothing>
}
