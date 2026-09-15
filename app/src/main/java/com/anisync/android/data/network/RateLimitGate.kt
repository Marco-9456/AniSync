package com.anisync.android.data.network

import android.util.Log
import com.anisync.android.data.util.ApiError
import com.anisync.android.data.util.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Tunables for [RateLimitGate]. Split out so tests can drive the gate hard without waiting in real
 * time, and so the debug screen can pin a small limit to exercise the refusal paths on a device.
 */
data class RateLimitConfig(
    /**
     * Smallest gap between two outbound requests.
     *
     * AniList runs an undocumented burst limiter on top of the per-minute window, so a fan-out that
     * leaves at once can be refused while still well inside the budget. Six parallel calls spread
     * over about 700ms is invisible to the user and stops looking like a burst.
     */
    val minGapMs: Long = 120L,

    /** Longest a user-facing request will sit in the gate before failing with a countdown instead. */
    val maxInteractiveWaitMs: Long = 20_000L,

    /** Share of the window [RequestPriority.Prefetch] must leave for the user. */
    val prefetchReserveFraction: Double = 0.2,

    /** Share of the window [RequestPriority.Background] must leave for the user. */
    val backgroundReserveFraction: Double = 0.4,

    /** Spread applied to every wait so waiters do not wake in lockstep. */
    val jitterMs: Long = 80L,
)

/**
 * Decides whether an AniList request may go out right now, and holds it back if not.
 *
 * AniList allows 30 requests a minute while degraded. The app can ask for more than that in the
 * first few seconds of a cold start alone, so something has to sit between the callers and the
 * socket. This is it.
 *
 * Three mechanisms, in order of how often they matter:
 *
 * 1. **Spacing.** Every request is at least [RateLimitConfig.minGapMs] after the previous one. This
 *    is what defeats the burst limiter, and it costs nothing when the app is idle.
 * 2. **Stale-aware headroom.** `X-RateLimit-Remaining` is the server's own count and is always one
 *    round trip behind, so [RateLimitWindow] subtracts what has been issued since it was read.
 *    Without that, parallel callers all read the same number and collectively overshoot.
 * 3. **Priority reserves.** Background work stops well short of the limit so the budget is still
 *    there when the user opens a screen.
 *
 * When a request cannot go now, what happens depends on who is asking.
 * [RequestPriority.Interactive] waits, bounded, because someone is watching a spinner.
 * Everything else is refused immediately with [ApiError.Deferred] so a worker can hand the job back
 * to WorkManager rather than hold a coroutine and a wakelock open for a minute.
 *
 * This replaces a token bucket that started full, never read the server's counter, and let each
 * caller sleep through a 429 independently before all of them retried at the same instant.
 *
 * Built by [com.anisync.android.di.NetworkModule] rather than by constructor injection, so the
 * tunables keep their Kotlin defaults instead of needing a binding each.
 */
class RateLimitGate(
    private val clock: Clock,
    private val monitor: RateLimitMonitor,
    private val persistence: RateLimitPersistence,
    private val config: RateLimitConfig = RateLimitConfig(),
    private val random: Random = Random.Default,
) {

    private val mutex = Mutex()
    private val window = RateLimitWindow(clock)

    private var lastIssuedAtMs = Long.MIN_VALUE
    private var restored = false

    private val inFlight = AtomicInteger(0)
    private val admitted = AtomicLong(0)
    private val paced = AtomicLong(0)
    private val deferred = AtomicLong(0)
    private val retried = AtomicLong(0)
    private val rateLimited = AtomicLong(0)

    /**
     * Overrides the limit the gate believes it has, for the debug screen.
     *
     * Pinning this to a small number is the only practical way to exercise the refusal and 429
     * paths on a device without hammering AniList to get there.
     */
    @Volatile
    var simulatedLimit: Int? = null

    /**
     * Suspends until this request may be sent.
     *
     * @throws ApiError.Deferred when a non-interactive request cannot be afforded right now
     * @throws ApiError.RateLimited when an interactive request would have to wait longer than
     *   [RateLimitConfig.maxInteractiveWaitMs]
     */
    suspend fun acquire(priority: RequestPriority) {
        restoreOnce()
        while (true) {
            when (val decision = mutex.withLock { evaluate(priority) }) {
                Decision.Admit -> {
                    admitted.incrementAndGet()
                    inFlight.incrementAndGet()
                    publish()
                    Log.d(
                        TAG,
                        "AniSyncNet event=admit priority=$priority " +
                            "remaining=${effectiveHeadroom()} in_flight=${inFlight.get()}",
                    )
                    return
                }

                is Decision.Wait -> {
                    paced.incrementAndGet()
                    publish()
                    delay(decision.delayMs)
                }

                is Decision.Refuse -> {
                    if (decision.error is ApiError.Deferred) deferred.incrementAndGet()
                    publish()
                    throw decision.error
                }
            }
        }
    }

    /** Balances [acquire]. Always call it, including when the request threw. */
    fun release() {
        inFlight.updateAndGet { (it - 1).coerceAtLeast(0) }
        publish()
    }

    /**
     * Folds a response's rate limit headers back into the window.
     *
     * Called for every response, success or not, because the headers are on all of them and they
     * are the only authoritative reading the client ever gets.
     */
    suspend fun onResponse(statusCode: Int, limit: Int?, remaining: Int?, retryAfterSeconds: Long?) {
        mutex.withLock {
            window.onHeaders(limit, remaining)
            if (statusCode == HTTP_TOO_MANY_REQUESTS) {
                rateLimited.incrementAndGet()
                window.onRateLimited(retryAfterSeconds)
                persistence.saveBlockedFor(window.blockedForMs())
                Log.w(
                    TAG,
                    "AniSyncNet event=rate_limited retry_after_s=${retryAfterSeconds ?: -1} " +
                        "limit=${window.limit} blocked_ms=${window.blockedForMs()}",
                )
            }
        }
        publish()
    }

    /** Counts a retry for the debug readout. */
    fun onRetry() {
        retried.incrementAndGet()
        publish()
    }

    /** Milliseconds still to wait on a 429 timeout, or 0. Read by the retry policy. */
    fun blockedForMs(): Long = window.blockedForMs()

    private fun evaluate(priority: RequestPriority): Decision {
        val blockedFor = window.blockedForMs()
        if (blockedFor > 0) return holdOrRefuse(priority, blockedFor, blocked = true)

        val limit = simulatedLimit ?: window.limit
        if (effectiveHeadroom() <= reserveFor(priority, limit)) {
            return holdOrRefuse(priority, window.resetsInMs(), blocked = false)
        }

        val now = clock.nowMs()
        val sinceLast = if (lastIssuedAtMs == Long.MIN_VALUE) Long.MAX_VALUE else now - lastIssuedAtMs
        if (sinceLast < config.minGapMs) return Decision.Wait(config.minGapMs - sinceLast)

        lastIssuedAtMs = now
        window.onIssued()
        return Decision.Admit
    }

    /**
     * The user waits, everything else bounces.
     *
     * A worker that suspends here would hold its coroutine, and often a wakelock, for up to a
     * minute for a job WorkManager is happy to run later.
     */
    private fun holdOrRefuse(priority: RequestPriority, waitMs: Long, blocked: Boolean): Decision {
        val seconds = ceilSeconds(waitMs)
        if (priority != RequestPriority.Interactive) {
            Log.d(TAG, "AniSyncNet event=deferred priority=$priority wait_s=$seconds blocked=$blocked")
            return Decision.Refuse(ApiError.Deferred(seconds))
        }
        if (waitMs > config.maxInteractiveWaitMs) {
            return Decision.Refuse(ApiError.RateLimited(seconds, window.limit))
        }
        return Decision.Wait(waitMs + jitter())
    }

    /**
     * Headroom the gate acts on, which is the real headroom unless the debug screen has pinned a
     * smaller limit. Simulation works off what has already been spent in the window rather than
     * clamping the count, so the budget actually runs out instead of sitting at the pinned number.
     */
    private fun effectiveHeadroom(): Int {
        val simulated = simulatedLimit ?: return window.headroom()
        val spent = window.limit - window.headroom()
        return (simulated - spent).coerceAtLeast(0)
    }

    private fun reserveFor(priority: RequestPriority, limit: Int): Int = when (priority) {
        RequestPriority.Interactive -> 0
        RequestPriority.Prefetch -> (limit * config.prefetchReserveFraction).roundToInt()
        RequestPriority.Background -> (limit * config.backgroundReserveFraction).roundToInt()
    }

    private fun jitter(): Long =
        if (config.jitterMs <= 0) 0 else random.nextLong(config.jitterMs)

    /**
     * Reinstates a 429 timeout that outlived the process.
     *
     * Force-stopping during a timeout and reopening used to start from a clean slate and walk
     * straight back into the limiter.
     */
    private suspend fun restoreOnce() {
        if (restored) return
        mutex.withLock {
            if (restored) return@withLock
            restored = true
            val pending = persistence.readBlockedForMs()
            if (pending > 0) {
                window.restoreBlockedFor(pending)
                Log.i(TAG, "AniSyncNet event=restored_block ms=$pending")
            }
        }
    }

    private fun publish() {
        val blockedFor = window.blockedForMs()
        val limit = simulatedLimit ?: window.limit
        monitor.publishStatus(
            when {
                blockedFor > 0 -> RateLimitStatus.Blocked(
                    retryAtElapsedMs = clock.nowMs() + blockedFor,
                    secondsRemaining = ceilSeconds(blockedFor),
                )

                effectiveHeadroom() <= reserveFor(RequestPriority.Prefetch, limit) ->
                    RateLimitStatus.Pacing(effectiveHeadroom(), limit)

                else -> RateLimitStatus.Clear
            },
        )
        monitor.publishStats(
            RateLimitStats(
                limit = limit,
                remaining = effectiveHeadroom(),
                inFlight = inFlight.get(),
                windowResetsInMs = window.resetsInMs(),
                blockedForMs = blockedFor,
                admitted = admitted.get(),
                paced = paced.get(),
                deferred = deferred.get(),
                retried = retried.get(),
                rateLimited = rateLimited.get(),
            ),
        )
    }

    private sealed interface Decision {
        data object Admit : Decision
        data class Wait(val delayMs: Long) : Decision
        data class Refuse(val error: ApiError) : Decision
    }

    companion object {
        private const val TAG = "RateLimitGate"
        const val HTTP_TOO_MANY_REQUESTS = 429

        private fun ceilSeconds(ms: Long): Long = (ms + 999) / 1000
    }
}
