package com.anisync.android.data.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
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

    private val inflight = ConcurrentHashMap<String, Entry>()

    private class Entry(val context: CoroutineContext, val deferred: Deferred<Any?>) {
        /** Callers currently inside [deduplicate] for this entry. The last one out cancels it. */
        val waiters = AtomicInteger(0)
    }

    /**
     * Run [block] once even if many callers ask for the same [key]. All
     * concurrent callers await the same [Deferred] and receive the same
     * result.
     *
     * The block runs in this tracker's own scope rather than the first caller's, so cancelling that
     * caller does not strand everyone who joined it. The flip side is that nothing upstream can
     * cancel it either, so this counts its waiters: when the last one leaves, the shared work is
     * cancelled. Without that, closing a screen mid-request left the request running to completion
     * with its response thrown away, which on a 30 a minute budget is the one thing worth avoiding.
     *
     * @param context added to the context the block runs under, overriding what the first caller
     *   brought with it. Pass anything the shared work should own rather than inherit.
     * @param onJoin called with the running block's context when this caller joined work that was
     *   already in flight, rather than starting it. Lets a joiner raise something the leader set.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> deduplicate(
        key: String,
        context: CoroutineContext = EmptyCoroutineContext,
        onJoin: (CoroutineContext) -> Unit = {},
        block: suspend () -> T,
    ): T {
        // The caller's context elements have to come along or ambient state such as the request
        // priority would be lost on the way. Its Job must not: that is what would tie the shared
        // work to one caller's lifetime.
        val inherited = currentCoroutineContext().minusKey(Job) + context
        var joined = false
        val entry = inflight.compute(key) { _, existing ->
            // Only work still running can be joined. A completed entry that its last waiter has not
            // yet cleaned up would otherwise hand the next caller a finished request's response.
            val live = existing?.takeIf { it.deferred.isActive }
            joined = live != null
            val entry = live ?: Entry(inherited, scope.async(inherited) { block() })
            entry.waiters.incrementAndGet()
            entry
        }!!
        if (joined) onJoin(entry.context)

        return try {
            entry.deferred.await() as T
        } finally {
            release(key, entry)
        }
    }

    /**
     * Drops this caller's claim on [entry], cancelling the shared work if it was the last one.
     *
     * Both halves happen under the map's own per-key lock, because they have to agree: a caller
     * arriving between the count reaching zero and the key being freed would otherwise join work
     * that is about to be cancelled.
     *
     * Nothing here re-enters the map. The completed Deferred is never cleaned up by a completion
     * handler, which with [Dispatchers.Unconfined] could run inside this very lambda, and the
     * `cancel` only ever fires with no waiters left to resume.
     */
    private fun release(key: String, entry: Entry) {
        inflight.compute(key) { _, existing ->
            if (entry.waiters.decrementAndGet() > 0) return@compute existing
            entry.deferred.cancel()
            if (existing === entry) null else existing
        }
    }
}
