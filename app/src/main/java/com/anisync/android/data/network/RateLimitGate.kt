package com.anisync.android.data.network

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

    /**
     * How long the gate holds requests behind a budget no response has confirmed.
     *
     * A rollover refills the window on a guess and one probe's headers correct it. A probe that
     * never comes back must not hold everything else behind it, so past this the guess is spent
     * rather than waited on. Longer than any healthy round trip, shorter than a window.
     */
    val probeTimeoutMs: Long = 10_000L,
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

    /**
     * The last state of [window], as an immutable value.
     *
     * [publish] and [blockedForMs] are called from threads that do not hold [mutex] (a response
     * arriving, a request being released, the retry policy asking). Reading the live window from
     * there let a report be assembled out of fields from either side of a 429, which surfaced as a
     * countdown clearing itself off screen while the block was still on. Everything outside the
     * lock reads this instead, and every path that mutates the window refreshes it before it lets
     * the lock go.
     */
    @Volatile
    private var published = window.snapshot()

    private var lastIssuedAtMs = Long.MIN_VALUE
    private var restored = false

    private val inFlight = AtomicInteger(0)
    private val admitted = AtomicLong(0)
    private val paced = AtomicLong(0)
    private val deferred = AtomicLong(0)
    private val refused = AtomicLong(0)
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
            when (val decision = mutex.withLock { evaluate(priority).also { capture() } }) {
                Decision.Admit -> {
                    admitted.incrementAndGet()
                    publish()
                    NetLog.d(TAG) {
                        "AniSyncNet event=admit priority=$priority " +
                            "remaining=${published.let { effectiveHeadroom(it.limit, it.headroom) }} " +
                            "in_flight=${inFlight.get()}"
                    }
                    return
                }

                is Decision.Wait -> {
                    paced.incrementAndGet()
                    publish()
                    delay(decision.delayMs)
                }

                is Decision.Refuse -> {
                    if (decision.error is ApiError.Deferred) {
                        deferred.incrementAndGet()
                    } else {
                        refused.incrementAndGet()
                    }
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
                NetLog.w(TAG) {
                    "AniSyncNet event=rate_limited retry_after_s=${retryAfterSeconds ?: -1} " +
                        "limit=${window.limit} blocked_ms=${window.blockedForMs()}"
                }
            }
            capture()
        }
        publish()
    }

    /** Counts a retry for the debug readout. */
    fun onRetry() {
        retried.incrementAndGet()
        publish()
    }

    /** Milliseconds still to wait on a 429 timeout, or 0. Read by the retry policy. */
    fun blockedForMs(): Long = published.blockedForMs(clock.nowMs())

    /** Call while holding [mutex], after anything that changed the window. */
    private fun capture() {
        published = window.snapshot()
    }

    private fun evaluate(priority: RequestPriority): Decision {
        // Before anything else: a window whose length has passed refills itself. Nothing else can,
        // because the refill rides on a response and the gate is holding the request that would
        // fetch one.
        window.rolloverIfElapsed()
        val blockedFor = window.blockedForMs()
        if (blockedFor > 0) return holdOrRefuse(priority, blockedFor, blocked = true)

        // A budget nobody has confirmed is a guess about where the server's window boundary sits.
        // One request goes out to find out; the rest wait for its headers rather than spend a
        // window that may not have refilled. The wait is one round trip, not one window, and
        // bounded by probeTimeoutMs so a probe that never returns cannot hold the app behind it.
        if (!window.budgetConfirmed &&
            inFlight.get() > 0 &&
            clock.nowMs() - window.windowStartedAtMs < config.probeTimeoutMs
        ) {
            return Decision.Wait(config.minGapMs)
        }

        val limit = simulatedLimit ?: window.limit
        if (effectiveHeadroom(window.limit, window.headroom()) <= reserveFor(priority, limit)) {
            return holdOrRefuse(priority, window.resetsInMs(), blocked = false)
        }

        val now = clock.nowMs()
        val sinceLast = if (lastIssuedAtMs == Long.MIN_VALUE) Long.MAX_VALUE else now - lastIssuedAtMs
        if (sinceLast < config.minGapMs) return Decision.Wait(config.minGapMs - sinceLast)

        lastIssuedAtMs = now
        window.onIssued()
        // Under the lock, in step with onIssued. Incrementing it after the lock was released left a
        // window where the probe guard above could see nothing in flight and admit a second caller
        // alongside the probe it was supposed to be waiting for.
        inFlight.incrementAndGet()
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
            NetLog.d(TAG) { "AniSyncNet event=deferred priority=$priority wait_s=$seconds blocked=$blocked" }
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
    private fun effectiveHeadroom(limit: Int, headroom: Int): Int {
        val simulated = simulatedLimit ?: return headroom
        val spent = limit - headroom
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
                NetLog.i(TAG) { "AniSyncNet event=restored_block ms=$pending" }
            }
            capture()
        }
    }

    private fun publish() {
        val state = published
        val now = clock.nowMs()
        val blockedFor = state.blockedForMs(now)
        val limit = simulatedLimit ?: state.limit
        val headroom = effectiveHeadroom(state.limit, state.headroom)
        // A spent budget stops requests just as completely as a 429 does, so it reports the same
        // way. Reporting it as mere pacing left the user watching a screen of skeletons with no
        // toast, no countdown and nothing to retry from.
        val waitMs = when {
            blockedFor > 0 -> blockedFor
            headroom <= 0 -> state.resetsInMs(now)
            else -> 0
        }
        monitor.publishStatus(
            when {
                waitMs > 0 -> RateLimitStatus.Blocked(
                    retryAtElapsedMs = now + waitMs,
                    secondsRemaining = ceilSeconds(waitMs),
                )

                headroom <= reserveFor(RequestPriority.Prefetch, limit) -> RateLimitStatus.Pacing

                else -> RateLimitStatus.Clear
            },
        )
        monitor.publishStats(
            RateLimitStats(
                limit = limit,
                remaining = headroom,
                inFlight = inFlight.get(),
                windowResetsInMs = state.resetsInMs(now),
                blockedForMs = blockedFor,
                admitted = admitted.get(),
                paced = paced.get(),
                deferred = deferred.get(),
                refused = refused.get(),
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
