package com.anisync.android.data.network

import com.anisync.android.data.util.ApiError
import com.anisync.android.data.util.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * The gate against a model of the thing it is guessing about.
 *
 * Every other test here tells the gate what the headers said. This one does not: it runs a server
 * that keeps its own budget and its own window boundary, and only ever answers the way AniList
 * answers. Nothing tells the client where the boundary is, which is the whole difficulty, and the
 * offset between the two is a parameter rather than an accident.
 *
 * The assertions are invariants rather than transcripts. "One 429 per boundary we guessed wrong"
 * holds whatever order the callers happen to run in, where an expected call count would only hold
 * for the interleaving that was recorded.
 *
 * Time is virtual and shared: [Clock] reads the same scheduler that [delay] advances, so a sixty
 * second window costs nothing and a run is reproducible from its seed.
 */
class RateLimitGateInvariantTest {

    @Before
    fun quietNetworkLogs() {
        NetLog.enabled = false
    }

    // region the server

    private class Answer(
        val status: Int,
        val limit: Int?,
        val remaining: Int?,
        val retryAfterSeconds: Long?,
    )

    /**
     * AniList's limiter as documented, with one addition: [windowStartMs] says where its window
     * boundary really sits. The client cannot see it and has to infer one, and the gap between the
     * two is what a rollover guess gets wrong.
     */
    private class FakeAniList(
        private val clock: Clock,
        private val limit: Int,
        windowStartMs: Long,
        private val chaos: Chaos = Chaos.None,
    ) {
        private var windowStart = windowStartMs
        private var used = 0

        var served = 0
            private set

        var rateLimited = 0
            private set

        /** Requests the server was asked to serve while it had nothing left. */
        var overspent = 0
            private set

        fun answer(): Answer {
            val now = clock.nowMs()
            while (now - windowStart >= RateLimitWindow.WINDOW_MS) {
                windowStart += RateLimitWindow.WINDOW_MS
                used = 0
            }
            if (used >= limit) {
                rateLimited++
                overspent++
                val leftMs = windowStart + RateLimitWindow.WINDOW_MS - now
                return chaos.mangle(
                    Answer(429, limit, 0, (leftMs + 999) / 1_000),
                )
            }
            used++
            served++
            return chaos.mangle(Answer(200, limit, limit - used, null))
        }
    }

    /**
     * Header shapes the client has no right to expect but must survive: a limit of zero, a negative
     * count, a `Retry-After` of hours, no rate limit headers at all.
     *
     * None of these are hypothetical in the sense that matters. They are what a proxy, a cache or a
     * maintenance page in front of the API can produce, and the window folds every one of them in
     * without being told which it is getting.
     */
    private fun interface Chaos {
        fun mangle(answer: Answer): Answer

        companion object {
            val None = Chaos { it }

            fun seeded(random: Random) = Chaos { answer ->
                when (random.nextInt(0, 12)) {
                    0 -> Answer(answer.status, null, null, answer.retryAfterSeconds)
                    1 -> Answer(answer.status, 0, answer.remaining, answer.retryAfterSeconds)
                    2 -> Answer(answer.status, answer.limit, -1, answer.retryAfterSeconds)
                    3 -> Answer(answer.status, answer.limit, answer.remaining, 9_999L)
                    4 -> Answer(answer.status, answer.limit, answer.remaining, null)
                    5 -> Answer(answer.status, -5, answer.remaining, answer.retryAfterSeconds)
                    else -> answer
                }
            }
        }
    }

    // endregion

    private sealed interface Outcome {
        data object Served : Outcome
        data object RateLimited : Outcome
        data class Refused(val error: ApiError) : Outcome
    }

    /**
     * One request end to end: wait for the gate, spend a round trip, report what came back.
     *
     * The `finally` is the contract the gate depends on, so the test honours it exactly as
     * [AniListHttpInterceptor] does, cancellation included.
     */
    private suspend fun call(
        gate: RateLimitGate,
        server: FakeAniList,
        priority: RequestPriority = RequestPriority.Interactive,
        latencyMs: Long = LATENCY_MS,
        retriesOn429: Int = 1,
    ): Outcome {
        var attempts = 0
        while (true) {
            attempts++
            try {
                gate.acquire(priority)
            } catch (e: ApiError) {
                return Outcome.Refused(e)
            }
            val answer = try {
                delay(latencyMs)
                server.answer().also {
                    gate.onResponse(it.status, it.limit, it.remaining, it.retryAfterSeconds)
                }
            } finally {
                gate.release()
            }
            if (answer.status != 429) return Outcome.Served
            if (attempts > retriesOn429) return Outcome.RateLimited
            // The 429 has blocked the gate, so the next acquire waits the timeout out before it
            // goes again. That is what RetryPolicy and the error interceptor do between them.
        }
    }

    private fun gate(
        clock: Clock,
        monitor: RateLimitMonitor,
        config: RateLimitConfig = defaultConfig,
        seed: Int = 0,
    ) = RateLimitGate(
        clock = clock,
        monitor = monitor,
        persistence = RateLimitPersistence.None,
        config = config,
        random = Random(seed),
    )

    /**
     * The headline case, and the one the rollover guess was written for.
     *
     * The server's window runs twenty seconds behind where the client will infer it, so when the
     * client's minute is up the server's is not. The client has no way to know that and assumes a
     * full budget, which is correct to do: refusing to guess deadlocks, because the refill rides on
     * a response and the gate is holding the request that would fetch one.
     *
     * What it must not do is spend the guess on a burst. Only the probe can be wrong, because only
     * the probe goes out before the headers land, so exactly one request may be refused however
     * many callers are queued behind it. Spacing alone does not give that: a queue drains one
     * request per gap and a gap is shorter than a round trip, so without the hold the whole queue
     * leaves before the first answer and every one of them is refused.
     */
    @Test
    fun `a window boundary guessed wrong costs one refusal, not a burst`() = runTest {
        val scheduler = testScheduler
        val clock = Clock { scheduler.currentTime }
        val monitor = RateLimitMonitor()
        val gate = gate(clock, monitor)
        val server = FakeAniList(clock, limit = LIMIT, windowStartMs = OFFSET_MS)

        // Spend the window the ordinary way, so the gate infers its boundary from real headers.
        repeat(LIMIT) { assertEquals(Outcome.Served, call(gate, server)) }

        // Past the client's minute, short of the server's.
        delay(RateLimitWindow.WINDOW_MS)

        val storm = coroutineScope {
            List(STORM) { async { call(gate, server) } }.awaitAll()
        }

        assertEquals(
            "only the probe may be refused, the rest wait behind it",
            1,
            server.rateLimited,
        )
        assertTrue(
            "every caller has to settle, whatever the probe found: $storm",
            storm.all { it is Outcome.Served },
        )
        assertEquals("the gate must not leak a slot", 0, monitor.stats.value.inFlight)
    }

    /**
     * The same hold, with the probe never coming back.
     *
     * Holding on an unconfirmed budget is right until it is the only thing happening. A request
     * that hangs would otherwise park every other caller behind it for as long as the socket stays
     * open, which is a worse failure than spending a guess, so the hold is bounded.
     */
    @Test
    fun `a probe that never answers does not hold the rest of the app behind it`() = runTest {
        val scheduler = testScheduler
        val clock = Clock { scheduler.currentTime }
        val monitor = RateLimitMonitor()
        val config = defaultConfig.copy(probeTimeoutMs = 2_000L)
        val gate = gate(clock, monitor, config)
        val server = FakeAniList(clock, limit = LIMIT, windowStartMs = 0)
        val hung = CompletableDeferred<Unit>()

        repeat(LIMIT) { assertEquals(Outcome.Served, call(gate, server)) }
        delay(RateLimitWindow.WINDOW_MS)

        val outcome = coroutineScope {
            val probe = async {
                gate.acquire(RequestPriority.Interactive)
                try {
                    hung.await()
                } finally {
                    gate.release()
                }
            }
            val waiter = async { call(gate, server) }
            val settled = waiter.await()
            hung.complete(Unit)
            probe.await()
            settled
        }

        assertEquals(
            "the wait is bounded by probeTimeoutMs, not by the socket",
            Outcome.Served,
            outcome,
        )
        assertEquals(0, monitor.stats.value.inFlight)
    }

    /**
     * Nothing to guess about here: the server has told the client what is left, repeatedly. Under a
     * fan-out far larger than the budget, the client's own accounting has to be enough to stay
     * inside it without the server ever having to say no.
     */
    @Test
    fun `a fan-out well past the budget never has to be refused`() = runTest {
        val scheduler = testScheduler
        val clock = Clock { scheduler.currentTime }
        val monitor = RateLimitMonitor()
        val gate = gate(clock, monitor)
        val server = FakeAniList(clock, limit = LIMIT, windowStartMs = 0)

        val outcomes = coroutineScope {
            List(LIMIT * 4) { async { call(gate, server) } }.awaitAll()
        }

        assertEquals("the client's own count must be enough", 0, server.overspent)
        assertTrue(
            "no caller may be lost: ${outcomes.groupingBy { it::class.simpleName }.eachCount()}",
            outcomes.all { it is Outcome.Served || it is Outcome.Refused },
        )
        assertEquals(0, monitor.stats.value.inFlight)
    }

    /**
     * Background work is refused rather than parked. A worker holding a coroutine, and often a
     * wakelock, for a minute is a cost WorkManager exists to avoid, and the reserve is what keeps
     * the last of the window for whoever is watching a spinner.
     */
    @Test
    fun `background work yields the reserve and is told how long to wait`() = runTest {
        val scheduler = testScheduler
        val clock = Clock { scheduler.currentTime }
        val monitor = RateLimitMonitor()
        val gate = gate(clock, monitor)
        val server = FakeAniList(clock, limit = LIMIT, windowStartMs = 0)

        // Spend down to inside the background reserve, which is 40% of the window.
        repeat(LIMIT - (LIMIT * 4 / 10)) { call(gate, server) }

        val refused = call(gate, server, priority = RequestPriority.Background)
        val interactive = call(gate, server, priority = RequestPriority.Interactive)

        val error = (refused as Outcome.Refused).error
        assertTrue("was $error", error is ApiError.Deferred)
        assertTrue(
            "a refusal has to say when to come back",
            (error as ApiError.Deferred).retryAfterSeconds > 0,
        )
        assertEquals("the user still gets through", Outcome.Served, interactive)
    }

    /**
     * Everything above, shuffled, for a hundred seeds.
     *
     * The scripted cases each pin one mechanism. This one exists for the interleavings nobody
     * thought to write down: a 429 landing while another response is being folded in, a background
     * refusal in the middle of a rollover, a `Retry-After` of hours, a limit header of zero, a
     * window that turns over while the queue is draining. The seed is printed on failure, and a
     * seed is the whole reproduction.
     *
     * The invariants are the ones that have to survive all of it. Not how many requests went out,
     * which depends on the shuffle, but that the client is never refused more than once per
     * boundary it had to guess, that every caller settles, and that no slot is left held.
     */
    @Test
    fun `invariants hold across a hundred shuffled runs`() = runTest {
        repeat(SEEDS) { seed ->
            val random = Random(seed)
            val scheduler = testScheduler
            val clock = Clock { scheduler.currentTime }
            val monitor = RateLimitMonitor()
            val gate = gate(clock, monitor, seed = seed)
            val server = FakeAniList(
                clock = clock,
                limit = LIMIT,
                windowStartMs = random.nextLong(0, RateLimitWindow.WINDOW_MS),
                chaos = Chaos.seeded(random),
            )

            val outcomes = coroutineScope {
                List(LIMIT * 3) {
                    async {
                        delay(random.nextLong(0, 4_000))
                        call(
                            gate = gate,
                            server = server,
                            priority = PRIORITIES[random.nextInt(PRIORITIES.size)],
                            latencyMs = random.nextLong(10, 900),
                        )
                    }
                }.awaitAll()
            }

            // Two windows are in play: the one the run starts in and the one it crosses into. A
            // boundary the client had to guess may cost it the probe, and nothing beyond that.
            assertTrue(
                "seed $seed: refused ${server.rateLimited} times, expected at most one per guessed " +
                    "boundary",
                server.rateLimited <= MAX_GUESSED_BOUNDARIES,
            )
            assertTrue(
                "seed $seed: a caller never settled",
                outcomes.size == LIMIT * 3,
            )
            assertEquals("seed $seed: leaked a slot", 0, monitor.stats.value.inFlight)
        }
    }

    private companion object {
        const val LIMIT = 10
        const val STORM = 8
        const val SEEDS = 100
        const val LATENCY_MS = 500L
        const val OFFSET_MS = 20_000L

        /** A run spans at most this many boundaries the client could not have been told about. */
        const val MAX_GUESSED_BOUNDARIES = 2

        val PRIORITIES = RequestPriority.entries.toTypedArray()

        /**
         * Production spacing and reserves. The interactive wait is longer than the default so a
         * caller held across a boundary waits it out rather than being handed a countdown, which
         * is what the app does on a screen that is already showing a skeleton.
         */
        val defaultConfig = RateLimitConfig(
            minGapMs = 120L,
            maxInteractiveWaitMs = 40_000L,
            jitterMs = 0L,
        )
    }
}
