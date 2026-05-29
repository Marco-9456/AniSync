package com.anisync.android.presentation.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Coalesces rapid, idempotent mutations (likes, favourites, ratings, progress)
 * keyed by entity id, so a burst of taps collapses into at most one network call.
 *
 * Each [submit] cancels the pending flush for that key and reschedules; only the
 * value that survives [debounceMs] of quiet is committed. If the settled value
 * equals the last server-committed value — e.g. like → unlike → like, or +1 then
 * −1 on progress — **nothing** is sent. Optimistic UI updates happen at the call
 * site, immediately; this class only governs when (and whether) the network fires.
 *
 * The ViewModel owns optimistic state and error rollback. This is deliberately the
 * single throttle primitive shared across like/favourite/rating/progress sites
 * (see P5) instead of each ViewModel hand-rolling its own debounce.
 *
 * @param V the desired settled value. For toggle endpoints (ToggleLike,
 *   ToggleFavourite) use [Boolean] and have [commit] issue a single toggle when the
 *   target differs from committed. For absolute endpoints (SaveMediaListEntry) use
 *   the absolute value (e.g. progress as [Int]).
 */
class MutationCoalescer<K : Any, V>(
    private val scope: CoroutineScope,
    private val debounceMs: Long = 500L,
    /**
     * Sends the settled [value] for [key]; returns true if it reached the server.
     * Returning false (e.g. the call errored and the site rolled back) leaves the
     * committed baseline unchanged so the next [submit] re-evaluates and retries.
     */
    private val commit: suspend (key: K, value: V) -> Boolean
) {
    private val jobs = ConcurrentHashMap<K, Job>()
    private val committed = ConcurrentHashMap<K, V>()

    /**
     * Record the value currently in sync with the server for [key] so a coalesced
     * no-op (return-to-original) can be detected. Call when the entity first loads.
     * Does not overwrite a value already being tracked.
     */
    fun seed(key: K, value: V) {
        committed.putIfAbsent(key, value)
    }

    /** Force [key]'s committed baseline to [value] (e.g. after an external refresh). */
    fun reset(key: K, value: V) {
        committed[key] = value
    }

    /**
     * Schedule [target] as the new desired value for [key], replacing any pending
     * flush. After [debounceMs] of quiet it is committed via [commit] — unless it
     * already matches the committed value, in which case the burst was a no-op.
     */
    fun submit(key: K, target: V) {
        jobs.remove(key)?.cancel()
        jobs[key] = scope.launch {
            delay(debounceMs)
            if (committed[key] == target) {
                jobs.remove(key)
                return@launch
            }
            if (commit(key, target)) committed[key] = target
            jobs.remove(key)
        }
    }

    /** Cancel any pending flush for [key] without committing (e.g. on entity removal). */
    fun cancel(key: K) {
        jobs.remove(key)?.cancel()
    }
}
