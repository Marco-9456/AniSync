package com.anisync.android.data.fetcher

import com.anisync.android.domain.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ResourceFetcherTest {

    /** Tiny in-memory cache mimicking a DAO. */
    private class FakeCache<K, T> {
        private val map = ConcurrentHashMap<K, T>()
        suspend fun read(key: K): T? = map[key]
        suspend fun write(key: K, value: T) { map[key] = value }
        fun seed(key: K, value: T) { map[key] = value }
    }

    private fun newFetcher(
        cache: FakeCache<String, String>,
        network: suspend (String) -> String,
        clock: FakeClock = FakeClock(),
        staleAfter: Long = ResourceFetcher.DEFAULT_STALE_AFTER_MS,
        cooldown: Long = ResourceFetcher.DEFAULT_COOLDOWN_MS
    ) = ResourceFetcher(
        readCache = { cache.read(it) },
        writeCache = { k, v -> cache.write(k, v) },
        fetchNetwork = network,
        staleAfterMs = staleAfter,
        cooldownMs = cooldown,
        clock = clock
    )

    @Test
    fun `CacheOnly emits cached value`() = runBlocking {
        val cache = FakeCache<String, String>().apply { seed("k", "cached") }
        val fetcher = newFetcher(cache, network = { error("must not call") })
        val emissions = fetcher.observe("k", CachePolicy.CacheOnly).toList()
        assertEquals(1, emissions.size)
        val data = emissions[0] as ResourceState.Data<String>
        assertEquals("cached", data.value)
        assertTrue(data.fromCache)
    }

    @Test
    fun `CacheOnly emits nothing on empty cache`() = runBlocking {
        val cache = FakeCache<String, String>()
        val fetcher = newFetcher(cache, network = { error("must not call") })
        val emissions = fetcher.observe("k", CachePolicy.CacheOnly).toList()
        assertEquals(0, emissions.size)
    }

    @Test
    fun `NetworkOnly emits Loading then Data`() = runBlocking {
        val cache = FakeCache<String, String>()
        val fetcher = newFetcher(cache, network = { "fresh" })
        val emissions = fetcher.observe("k", CachePolicy.NetworkOnly).toList()
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ResourceState.Loading)
        val data = emissions[1] as ResourceState.Data<String>
        assertEquals("fresh", data.value)
        assertFalse(data.fromCache)
        assertFalse(data.isStale)
    }

    @Test
    fun `NetworkOnly emits Error with staleValue on failure`() = runBlocking {
        val cache = FakeCache<String, String>().apply { seed("k", "cached") }
        val fetcher = newFetcher(cache, network = { throw RuntimeException("net down") })
        val emissions = fetcher.observe("k", CachePolicy.NetworkOnly).toList()
        assertEquals(2, emissions.size)
        val err = emissions[1] as ResourceState.Error
        assertEquals("cached", err.staleValue)
    }

    @Test
    fun `CacheFirst skips network when cached`() = runBlocking {
        val cache = FakeCache<String, String>().apply { seed("k", "cached") }
        val networkCalls = AtomicInteger(0)
        val fetcher = newFetcher(cache, network = {
            networkCalls.incrementAndGet()
            "fresh"
        })
        val emissions = fetcher.observe("k", CachePolicy.CacheFirst).toList()
        assertEquals(1, emissions.size)
        val data = emissions[0] as ResourceState.Data<String>
        assertEquals("cached", data.value)
        assertTrue(data.fromCache)
        assertEquals(0, networkCalls.get())
    }

    @Test
    fun `CacheFirst falls through to network when cache empty`() = runBlocking {
        val cache = FakeCache<String, String>()
        val fetcher = newFetcher(cache, network = { "fresh" })
        val emissions = fetcher.observe("k", CachePolicy.CacheFirst).toList()
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ResourceState.Loading)
        val data = emissions[1] as ResourceState.Data<String>
        assertEquals("fresh", data.value)
        assertFalse(data.fromCache)
    }

    @Test
    fun `CacheAndNetwork emits cache then network (SWR)`() = runBlocking {
        val cache = FakeCache<String, String>().apply { seed("k", "cached") }
        val fetcher = newFetcher(cache, network = { "fresh" })
        val emissions = fetcher.observe("k", CachePolicy.CacheAndNetwork).toList()
        assertEquals(2, emissions.size)
        val first = emissions[0] as ResourceState.Data<String>
        val second = emissions[1] as ResourceState.Data<String>
        assertEquals("cached", first.value)
        assertTrue(first.fromCache)
        assertEquals("fresh", second.value)
        assertFalse(second.fromCache)
    }

    @Test
    fun `CacheAndNetwork on empty cache emits Loading then Data`() = runBlocking {
        val cache = FakeCache<String, String>()
        val fetcher = newFetcher(cache, network = { "fresh" })
        val emissions = fetcher.observe("k", CachePolicy.CacheAndNetwork).toList()
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ResourceState.Loading)
        assertEquals("fresh", (emissions[1] as ResourceState.Data<String>).value)
    }

    @Test
    fun `CacheAndNetwork emits Error with staleValue on network fail`() = runBlocking {
        val cache = FakeCache<String, String>().apply { seed("k", "cached") }
        val fetcher = newFetcher(cache, network = { throw RuntimeException("nope") })
        val emissions = fetcher.observe("k", CachePolicy.CacheAndNetwork).toList()
        assertEquals(2, emissions.size)
        val first = emissions[0] as ResourceState.Data<String>
        val err = emissions[1] as ResourceState.Error
        assertEquals("cached", first.value)
        assertEquals("cached", err.staleValue)
    }

    @Test
    fun `CacheAndNetworkIfStale fresh cache short-circuits network`() = runBlocking {
        val clock = FakeClock(initialMs = 1_000_000L)
        val cache = FakeCache<String, String>()
        val networkCalls = AtomicInteger(0)
        val fetcher = newFetcher(cache, network = {
            networkCalls.incrementAndGet()
            "fresh"
        }, clock = clock)

        // Seed cache + record fetch time by running NetworkOnly first.
        fetcher.observe("k", CachePolicy.NetworkOnly).toList()
        assertEquals(1, networkCalls.get())

        // 30s later: still fresh (threshold 60s).
        clock.advanceBy(30_000L)
        val emissions = fetcher.observe(
            "k",
            CachePolicy.CacheAndNetworkIfStale(staleAfterMs = 60_000L)
        ).toList()

        assertEquals(1, emissions.size) // no network revalidation
        val data = emissions[0] as ResourceState.Data<String>
        assertEquals("fresh", data.value)
        assertFalse(data.isStale)
        assertEquals(1, networkCalls.get())
    }

    @Test
    fun `CacheAndNetworkIfStale stale cache fires revalidation`() = runBlocking {
        val clock = FakeClock(initialMs = 1_000_000L)
        val cache = FakeCache<String, String>()
        val networkCalls = AtomicInteger(0)
        val fetcher = newFetcher(cache, network = {
            "v${networkCalls.incrementAndGet()}"
        }, clock = clock)

        fetcher.observe("k", CachePolicy.NetworkOnly).toList()
        clock.advanceBy(120_000L) // past threshold

        val emissions = fetcher.observe(
            "k",
            CachePolicy.CacheAndNetworkIfStale(staleAfterMs = 60_000L)
        ).toList()

        assertEquals(2, emissions.size)
        val stale = emissions[0] as ResourceState.Data<String>
        val fresh = emissions[1] as ResourceState.Data<String>
        assertEquals("v1", stale.value)
        assertTrue(stale.isStale)
        assertEquals("v2", fresh.value)
        assertFalse(fresh.isStale)
        assertEquals(2, networkCalls.get())
    }

    @Test
    fun `refresh respects cooldown`() = runBlocking {
        val clock = FakeClock(initialMs = 1_000L)
        val cache = FakeCache<String, String>()
        val networkCalls = AtomicInteger(0)
        val fetcher = newFetcher(cache, network = {
            "v${networkCalls.incrementAndGet()}"
        }, clock = clock, cooldown = 10_000L)

        val first = fetcher.refresh("k") as Result.Success<String>
        assertEquals("v1", first.data)

        clock.advanceBy(5_000L) // inside cooldown
        val suppressed = fetcher.refresh("k") as Result.Error
        assertEquals(ResourceFetcher.COOLDOWN_ERROR_CODE, suppressed.code)
        assertEquals(1, networkCalls.get())

        clock.advanceBy(10_000L) // past cooldown
        val second = fetcher.refresh("k") as Result.Success<String>
        assertEquals("v2", second.data)
    }

    @Test
    fun `refresh force bypasses cooldown`() = runBlocking {
        val clock = FakeClock(initialMs = 1_000L)
        val cache = FakeCache<String, String>()
        val networkCalls = AtomicInteger(0)
        val fetcher = newFetcher(cache, network = {
            "v${networkCalls.incrementAndGet()}"
        }, clock = clock, cooldown = 10_000L)

        fetcher.refresh("k")
        clock.advanceBy(1_000L)
        val forced = fetcher.refresh("k", force = true) as Result.Success<String>
        assertEquals("v2", forced.data)
        assertEquals(2, networkCalls.get())
    }

    @Test
    fun `concurrent identical refresh calls share one network call`() = runBlocking {
        val cache = FakeCache<String, String>()
        val networkCalls = AtomicInteger(0)
        val fetcher = newFetcher(cache, network = {
            networkCalls.incrementAndGet()
            delay(100)
            "v"
        })

        // force=true bypasses cooldown so both calls actually reach the
        // inflight tracker (the dedup primitive we're testing here).
        val results = coroutineScope {
            val a = async { fetcher.refresh("k", force = true) }
            val b = async {
                delay(10)
                fetcher.refresh("k", force = true)
            }
            awaitAll(a, b)
        }

        assertEquals(1, networkCalls.get())
        results.forEach { r ->
            r as Result.Success<String>
            assertEquals("v", r.data)
        }
    }

    @Test
    fun `invalidate clears cooldown for key`() = runBlocking {
        val clock = FakeClock(initialMs = 1_000L)
        val cache = FakeCache<String, String>()
        val networkCalls = AtomicInteger(0)
        val fetcher = newFetcher(cache, network = {
            "v${networkCalls.incrementAndGet()}"
        }, clock = clock, cooldown = 10_000L)

        fetcher.refresh("k")
        clock.advanceBy(1_000L)
        assertTrue((fetcher.refresh("k") as Result.Error).code == ResourceFetcher.COOLDOWN_ERROR_CODE)

        fetcher.invalidate("k")
        val afterInvalidate = fetcher.refresh("k") as Result.Success<String>
        assertEquals("v2", afterInvalidate.data)
    }
}
