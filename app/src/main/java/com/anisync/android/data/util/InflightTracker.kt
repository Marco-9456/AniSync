package com.anisync.android.data.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Dedup concurrent identical work keyed by string. The second caller does
 * NOT re-run the suspend block — instead it awaits the first caller's
 * [Deferred], so a fan-out of N identical requests collapses to one.
 *
 * Replaces the `ConcurrentHashMap<String, Mutex>` helpers the repositories
 * grew, which serialized callers but then ran the block again for each of
 * them: two identical requests back to back instead of one.
 *
 * Caveats:
 * - The suspend block captures the FIRST caller's lexical scope. Second
 *   caller's block is discarded — fine for identical keys, but callers
 *   must ensure key uniqueness encodes all meaningful params.
 * - The internal scope uses [Dispatchers.Unconfined]; the suspend block
 *   inside is expected to switch dispatchers via its own suspend functions
 *   (e.g. Apollo's IO dispatcher).
 */
class InflightTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val inflight = ConcurrentHashMap<String, Deferred<Any?>>()

    /**
     * Run [block] once even if many callers ask for the same [key]. All
     * concurrent callers await the same [Deferred] and receive the same
     * result. After completion the entry is removed, so subsequent (non-
     * overlapping) callers re-run the block.
     *
     * Cleanup intentionally happens AFTER `deferred.await()` returns,
     * NOT inside the async block — ConcurrentHashMap forbids modifying
     * the map from within `computeIfAbsent`. With [Dispatchers.Unconfined]
     * the inner `block()` may run synchronously inside the compute lambda,
     * so a `finally { inflight.remove(key) }` inside `async { … }` would
     * trigger "Recursive update" at runtime.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> deduplicate(key: String, block: suspend () -> T): T {
        // The block runs in this tracker's own scope so a cancelled leader does not strand the
        // followers, but the caller's context elements have to come along or ambient state such as
        // the request priority would be lost on the way.
        val inherited = currentCoroutineContext().minusKey(Job)
        val deferred = inflight.computeIfAbsent(key) { scope.async(inherited) { block() } }
        return try {
            deferred.await() as T
        } finally {
            // remove(key, value) — only removes if value still matches, so
            // we don't clobber a fresh entry created by a later caller after
            // this Deferred completed.
            inflight.remove(key, deferred)
        }
    }
}
