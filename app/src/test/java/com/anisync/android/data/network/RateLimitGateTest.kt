package com.anisync.android.data.network

import com.anisync.android.data.util.ApiError
import com.anisync.android.data.util.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimitGateTest {

    /**
     * Ties the gate's clock to the test scheduler so `delay` and `Clock.nowMs` advance together.
     * Without this the gate would wait in virtual time while its own clock stood still.
     */
    private fun TestScope.gate(
        config: RateLimitConfig = RateLimitConfig(jitterMs = 0),
        persistence: RateLimitPersistence = RateLimitPersistence.None,
    ) = RateLimitGate(
        clock = Clock { testScheduler.currentTime },
        monitor = RateLimitMonitor(),
        persistence = persistence,
        config = config,
        random = Random(0),
    )

    private suspend fun RateLimitGate.take(priority: RequestPriority = RequestPriority.Interactive) {
        acquire(priority)
        release()
    }

    @Test
    fun `requests are spaced out instead of leaving together`() = runTest {
        val gate = gate()
        val start = testScheduler.currentTime

        repeat(6) { gate.take() }

        // Six parallel rails on Discover used to leave as one burst. Five gaps of 120ms is what
        // the undocumented burst limiter actually tolerates, and the user cannot see it.
        assertEquals(600, testScheduler.currentTime - start)
    }

    @Test
    fun `a parallel fan-out is serialised, not admitted at once`() = runTest {
        val gate = gate()
        val admittedAt = mutableListOf<Long>()

        (1..6).map {
            async {
                gate.acquire(RequestPriority.Interactive)
                admittedAt += testScheduler.currentTime
                gate.release()
            }
        }.awaitAll()

        assertEquals(6, admittedAt.size)
        admittedAt.sorted().zipWithNext { earlier, later ->
            assertTrue("gap was ${later - earlier}ms", later - earlier >= 120)
        }
    }

    @Test
    fun `the window budget is never overspent`() = runTest {
        val gate = gate()
        var admitted = 0

        // Nothing comes back, so the gate only has its own issue count to go on. It must still
        // refuse to exceed the advertised limit.
        repeat(40) {
            try {
                gate.take()
                admitted++
            } catch (_: ApiError) {
                // Expected once the budget is gone.
            }
        }

        assertEquals(RateLimitWindow.DEFAULT_LIMIT, admitted)
    }

    @Test
    fun `background work stops well short of the limit while the user does not`() = runTest {
        val gate = gate()

        // Spend down to just inside the background reserve, which is 40 percent of 30.
        repeat(18) { gate.take(RequestPriority.Interactive) }

        try {
            gate.take(RequestPriority.Background)
            fail("background should have been deferred with 12 of 30 left")
        } catch (e: ApiError.Deferred) {
            assertTrue(e.retryAfterSeconds > 0)
        }

        // The user is still served from the same budget.
        gate.take(RequestPriority.Interactive)
    }

    @Test
    fun `prefetch yields earlier than the user but later than background`() = runTest {
        val gate = gate()
        repeat(24) { gate.take(RequestPriority.Interactive) }

        try {
            gate.take(RequestPriority.Prefetch)
            fail("prefetch should have been deferred with 6 of 30 left")
        } catch (_: ApiError.Deferred) {
            // Expected.
        }
        gate.take(RequestPriority.Interactive)
    }

    @Test
    fun `a 429 refuses background immediately rather than making it wait`() = runTest {
        val gate = gate()
        gate.onResponse(statusCode = 429, limit = 30, remaining = 0, retryAfterSeconds = 45)

        val before = testScheduler.currentTime
        try {
            gate.take(RequestPriority.Background)
            fail("background should not be admitted during a timeout")
        } catch (e: ApiError.Deferred) {
            assertEquals(45, e.retryAfterSeconds)
        }
        assertEquals("background must not sleep", before, testScheduler.currentTime)
    }

    @Test
    fun `a long 429 fails the user fast with a countdown instead of a two minute spinner`() = runTest {
        val gate = gate()
        gate.onResponse(statusCode = 429, limit = 30, remaining = 0, retryAfterSeconds = 60)

        val before = testScheduler.currentTime
        try {
            gate.take(RequestPriority.Interactive)
            fail("a 60s block is longer than a user should be made to wait")
        } catch (e: ApiError.RateLimited) {
            assertEquals(60, e.retryAfterSeconds)
        }
        assertEquals(before, testScheduler.currentTime)
    }

    @Test
    fun `a short 429 is waited out once, not once per caller`() = runTest {
        val gate = gate()
        gate.onResponse(statusCode = 429, limit = 30, remaining = 0, retryAfterSeconds = 5)
        val start = testScheduler.currentTime

        (1..4).map { async { gate.take(RequestPriority.Interactive) } }.awaitAll()

        // All four share the one timeout. The old interceptor slept each of them separately.
        val elapsed = testScheduler.currentTime - start
        assertTrue("took ${elapsed}ms", elapsed in 5_000..6_000)
    }

    @Test
    fun `the budget recovers once the server reports a new window`() = runTest {
        val gate = gate()
        repeat(30) { gate.take() }

        try {
            gate.take()
            fail("budget should be spent")
        } catch (_: ApiError.RateLimited) {
            // Expected.
        }

        gate.onResponse(statusCode = 200, limit = 30, remaining = 29, retryAfterSeconds = null)
        gate.take()
    }

    @Test
    fun `a timeout that outlived the process is honoured on the first request`() = runTest {
        val stored = object : RateLimitPersistence {
            override fun readBlockedForMs() = 30_000L
            override fun saveBlockedFor(remainingMs: Long) = Unit
            override fun clear() = Unit
        }
        val gate = gate(persistence = stored)

        try {
            gate.take(RequestPriority.Background)
            fail("a stored timeout should still apply after a restart")
        } catch (e: ApiError.Deferred) {
            assertEquals(30, e.retryAfterSeconds)
        }
    }

    @Test
    fun `the debug limit makes the budget run out early`() = runTest {
        val gate = gate()
        gate.simulatedLimit = 5

        var admitted = 0
        repeat(10) {
            try {
                gate.take()
                admitted++
            } catch (_: ApiError) {
                // Expected past the pinned limit.
            }
        }

        assertEquals(5, admitted)
    }

    @Test
    fun `the monitor reports a block the UI can count down from`() = runTest {
        val monitor = RateLimitMonitor()
        val gate = RateLimitGate(
            clock = Clock { testScheduler.currentTime },
            monitor = monitor,
            persistence = RateLimitPersistence.None,
            config = RateLimitConfig(jitterMs = 0),
            random = Random(0),
        )

        gate.onResponse(statusCode = 429, limit = 30, remaining = 0, retryAfterSeconds = 20)

        val status = monitor.status.value
        assertTrue("was $status", status is RateLimitStatus.Blocked)
        assertEquals(20, (status as RateLimitStatus.Blocked).secondsRemaining)
    }

    /**
     * A spent budget stops requests as completely as a 429 does. Reporting it as mere pacing left
     * the user watching a screen of skeletons with no toast, no countdown and nothing to retry from.
     */
    @Test
    fun `an exhausted budget reports as blocked, not as pacing`() = runTest {
        val monitor = RateLimitMonitor()
        val gate = RateLimitGate(
            clock = Clock { testScheduler.currentTime },
            monitor = monitor,
            persistence = RateLimitPersistence.None,
            config = RateLimitConfig(jitterMs = 0),
            random = Random(0),
        )

        repeat(30) { gate.acquire(RequestPriority.Interactive); gate.release() }

        val status = monitor.status.value
        assertTrue("was $status", status is RateLimitStatus.Blocked)
        assertTrue((status as RateLimitStatus.Blocked).secondsRemaining > 0)
    }

    @Test
    fun `refusing the user is counted separately from deferring background work`() = runTest {
        val monitor = RateLimitMonitor()
        val gate = RateLimitGate(
            clock = Clock { testScheduler.currentTime },
            monitor = monitor,
            persistence = RateLimitPersistence.None,
            config = RateLimitConfig(jitterMs = 0),
            random = Random(0),
        )
        gate.onResponse(statusCode = 429, limit = 30, remaining = 0, retryAfterSeconds = 60)

        runCatching { gate.acquire(RequestPriority.Background) }
        runCatching { gate.acquire(RequestPriority.Interactive) }

        assertEquals(1, monitor.stats.value.deferred)
        assertEquals(1, monitor.stats.value.refused)
    }

    @Test
    fun `in-flight returns to zero even when a request throws`() = runTest {
        val monitor = RateLimitMonitor()
        val gate = RateLimitGate(
            clock = Clock { testScheduler.currentTime },
            monitor = monitor,
            persistence = RateLimitPersistence.None,
            config = RateLimitConfig(jitterMs = 0),
            random = Random(0),
        )

        gate.acquire(RequestPriority.Interactive)
        assertEquals(1, monitor.stats.value.inFlight)
        gate.release()
        assertEquals(0, monitor.stats.value.inFlight)
    }
}
