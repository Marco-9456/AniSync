package com.anisync.android.data.fetcher

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
}
