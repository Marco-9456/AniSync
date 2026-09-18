package com.anisync.android.data.util

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class InflightTrackerTest {

    @Test
    fun `single caller runs block once and returns result`() = runBlocking {
        val tracker = InflightTracker()
        val result = tracker.deduplicate("k") { 42 }
        assertEquals(42, result)
    }

    @Test
    fun `two concurrent callers with same key share one execution`() = runBlocking {
        val tracker = InflightTracker()
        val invocations = AtomicInteger(0)

        val results = coroutineScope {
            val a = async {
                tracker.deduplicate("shared") {
                    invocations.incrementAndGet()
                    delay(100) // hold the block long enough for the second caller to join
                    "value"
                }
            }
            val b = async {
                // tiny gap so the second deduplicate sees the in-flight entry.
                delay(10)
                tracker.deduplicate("shared") {
                    invocations.incrementAndGet()
                    "shouldNotRun"
                }
            }
            awaitAll(a, b)
        }

        assertEquals("Block must run exactly once for shared key", 1, invocations.get())
        assertEquals(listOf("value", "value"), results)
    }

    @Test
    fun `different keys run independently in parallel`() = runBlocking {
        val tracker = InflightTracker()
        val invocations = AtomicInteger(0)

        coroutineScope {
            val a = async {
                tracker.deduplicate("a") {
                    invocations.incrementAndGet()
                    delay(50)
                    "a"
                }
            }
            val b = async {
                tracker.deduplicate("b") {
                    invocations.incrementAndGet()
                    delay(50)
                    "b"
                }
            }
            assertEquals("a", a.await())
            assertEquals("b", b.await())
        }

        assertEquals(2, invocations.get())
    }

    @Test
    fun `sequential calls with same key re-run the block`() = runBlocking {
        val tracker = InflightTracker()
        val invocations = AtomicInteger(0)

        val first = tracker.deduplicate("k") { invocations.incrementAndGet(); "x" }
        // First Deferred completes + cleanup runs; second call gets a fresh entry.
        val second = tracker.deduplicate("k") { invocations.incrementAndGet(); "x" }

        assertEquals(2, invocations.get())
        assertEquals(first, second)
    }

    @Test
    fun `exception in block propagates to all sharing callers`() = runBlocking {
        val tracker = InflightTracker()
        val boom = RuntimeException("network down")

        val results = coroutineScope {
            val a = async {
                runCatching {
                    tracker.deduplicate("err") {
                        delay(50)
                        throw boom
                    }
                }
            }
            val b = async {
                delay(10)
                runCatching {
                    tracker.deduplicate("err") {
                        fail("should not run")
                        "unused"
                    }
                }
            }
            awaitAll(a, b)
        }

        results.forEach { r ->
            assertTrue("all callers must observe the exception", r.isFailure)
            assertEquals("network down", r.exceptionOrNull()?.message)
        }
    }

    @Test
    fun `after a failed call the key is freed for retry`() = runBlocking {
        val tracker = InflightTracker()
        var firstRan = false
        var secondRan = false

        val firstResult = runCatching {
            tracker.deduplicate("k") {
                firstRan = true
                throw RuntimeException("first fails")
            }
        }
        assertTrue(firstResult.isFailure)

        val second = tracker.deduplicate("k") {
            secondRan = true
            "ok"
        }
        assertTrue(firstRan)
        assertTrue(secondRan)
        assertEquals("ok", second)
    }

    /**
     * The block runs in the tracker's own scope, not the caller's, so anything ambient in the
     * caller's context has to be carried over. The request priority rides in exactly that way, and
     * losing it would let a worker's coalesced request outbid the screen the user is looking at.
     */
    @Test
    fun `the caller's context elements reach the shared block`() = runBlocking {
        val tracker = InflightTracker()
        val seen = withContext(Marker("carried")) {
            tracker.deduplicate("k") { currentCoroutineContext()[Marker]?.value }
        }
        assertEquals("carried", seen)
    }

    /**
     * The block runs detached from the caller's Job, so nothing upstream can cancel it. Leaving it
     * at that meant closing a screen mid-request, or a `collectLatest` replacing a search, ran the
     * request and its retries to completion and discarded the response: a request spent out of a
     * thirty a minute budget for a result nobody would read.
     */
    @Test
    fun `the shared work is cancelled once its last caller goes away`() = runBlocking {
        val tracker = InflightTracker()
        var cancelled = false

        val leader = launch {
            tracker.deduplicate("k") {
                try {
                    delay(5_000)
                } catch (e: CancellationException) {
                    cancelled = true
                    throw e
                }
            }
        }
        delay(50)
        leader.cancelAndJoin()
        delay(50)

        assertTrue("cancelling the only caller must cancel the request", cancelled)
    }

    @Test
    fun `a follower keeps the shared work running after the leader is cancelled`() = runBlocking {
        val tracker = InflightTracker()

        val leader = launch {
            tracker.deduplicate<String>("k") {
                delay(200)
                "value"
            }
        }
        delay(20)
        val follower = async { tracker.deduplicate("k") { "shouldNotRun" } }
        delay(20)
        leader.cancelAndJoin()

        assertEquals("value", follower.await())
    }

    /**
     * Cleanup used to run in every awaiter's `finally`, so a cancelled leader freed the key while
     * its followers were still on the live request. The next caller then started a second one,
     * which is exactly the case the search pipeline produces.
     */
    @Test
    fun `a cancelled leader does not free the key while a follower is still waiting`() = runBlocking {
        val tracker = InflightTracker()
        val invocations = AtomicInteger(0)

        val leader = launch {
            tracker.deduplicate<String>("k") {
                invocations.incrementAndGet()
                delay(200)
                "value"
            }
        }
        delay(20)
        val follower = async { tracker.deduplicate("k") { invocations.incrementAndGet(); "second" } }
        delay(20)
        leader.cancelAndJoin()
        val third = async { tracker.deduplicate("k") { invocations.incrementAndGet(); "third" } }

        assertEquals("value", follower.await())
        assertEquals("value", third.await())
        assertEquals("Block must run exactly once", 1, invocations.get())
    }

    @Test
    fun `the context passed in overrides what the caller brought`() = runBlocking {
        val tracker = InflightTracker()
        val seen = withContext(Marker("caller")) {
            tracker.deduplicate("k", context = Marker("override")) {
                currentCoroutineContext()[Marker]?.value
            }
        }
        assertEquals("override", seen)
    }

    @Test
    fun `onJoin fires for a caller that joined and not for the one that started`() = runBlocking {
        val tracker = InflightTracker()
        val joins = AtomicInteger(0)

        coroutineScope {
            val a = async {
                tracker.deduplicate("k", onJoin = { joins.incrementAndGet() }) {
                    delay(100)
                    "value"
                }
            }
            val b = async {
                delay(10)
                tracker.deduplicate("k", onJoin = { joins.incrementAndGet() }) { "shouldNotRun" }
            }
            awaitAll(a, b)
        }

        assertEquals(1, joins.get())
    }

    private class Marker(val value: String) : AbstractCoroutineContextElement(Marker) {
        companion object : CoroutineContext.Key<Marker>
    }
}
