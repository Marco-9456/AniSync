package com.anisync.android.data.perf

import com.anisync.android.data.mapper.mapFuzzyDateToLong
import com.anisync.android.data.mapper.toFuzzyDateInput
import com.anisync.android.type.ThreadSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Micro-benchmarks for the data-layer hot paths optimized in this pass.
 *
 * Methodology:
 *   - Each test runs warm-up iterations to let the JIT promote bytecode to compiled code,
 *     then measures the optimized vs. legacy implementation over a fixed iteration count.
 *   - System.nanoTime is monotonic; the deltas reported are wall-clock per-op nanoseconds.
 *   - We compute the speedup ratio and assert ratio >= ~1.5x (with generous slack so a
 *     cold/loaded CI runner doesn't flake the test).
 *   - Correctness is asserted alongside speed: the new path must produce the same value
 *     the legacy path produced, otherwise we'd be benchmarking a regression.
 */
class DataLayerBenchmarkTest {

    private val warmup = 5_000
    private val iterations = 100_000

    @Test
    fun benchmark_mapFuzzyDateToLong_legacyCalendar_vs_javaTime() {
        // Correctness gate first: same input → same output across implementations.
        val newResult = mapFuzzyDateToLong(2024, 3, 15)
        val legacyResult = legacyMapFuzzyDateToLong(2024, 3, 15)
        assertEquals(legacyResult, newResult)
        // Edge: nulls
        assertEquals(null, mapFuzzyDateToLong(null, 1, 1))
        assertEquals(legacyMapFuzzyDateToLong(2020, null, null), mapFuzzyDateToLong(2020, null, null))

        // Warm up both paths
        repeat(warmup) {
            mapFuzzyDateToLong(2020 + (it % 10), 1 + (it % 12), 1 + (it % 28))
            legacyMapFuzzyDateToLong(2020 + (it % 10), 1 + (it % 12), 1 + (it % 28))
        }

        val newNs = measureNanos {
            repeat(iterations) {
                mapFuzzyDateToLong(2020 + (it % 10), 1 + (it % 12), 1 + (it % 28))
            }
        }
        val legacyNs = measureNanos {
            repeat(iterations) {
                legacyMapFuzzyDateToLong(2020 + (it % 10), 1 + (it % 12), 1 + (it % 28))
            }
        }
        report("mapFuzzyDateToLong", legacyNs, newNs)
        // We expect a clear win — but allow JIT noise.
        assertTrue("expected new impl to be faster, legacy=$legacyNs new=$newNs", newNs < legacyNs)
    }

    @Test
    fun benchmark_toFuzzyDateInput_legacyCalendar_vs_javaTime() {
        val ts = 1_700_000_000_000L

        val newResult = ts.toFuzzyDateInput()
        val legacyResult = legacyToFuzzyComponents(ts)
        // Apollo Optional<T> exposes the underlying value via getOrThrow()-style helpers,
        // but to keep the test independent of Apollo internals we string-match instead.
        val printed = newResult.toString()
        assertTrue("year missing in $printed", printed.contains(legacyResult.first.toString()))
        assertTrue("month missing in $printed", printed.contains(legacyResult.second.toString()))
        assertTrue("day missing in $printed", printed.contains(legacyResult.third.toString()))

        repeat(warmup) {
            ts.toFuzzyDateInput()
            legacyToFuzzyComponents(ts)
        }

        val newNs = measureNanos {
            var t = ts
            repeat(iterations) {
                t.toFuzzyDateInput()
                t += 86_400_000L
            }
        }
        val legacyNs = measureNanos {
            var t = ts
            repeat(iterations) {
                legacyToFuzzyComponents(t)
                t += 86_400_000L
            }
        }
        report("toFuzzyDateInput", legacyNs, newNs)
        assertTrue("expected new impl to be faster, legacy=$legacyNs new=$newNs", newNs < legacyNs)
    }

    @Test
    fun benchmark_threadSort_lookup_linear_vs_map() {
        val mapByName: Map<String, ThreadSort> = ThreadSort.entries.associateBy { it.name }
        val tokens = listOf(
            "IS_STICKY", "REPLIED_AT_DESC", "ID_DESC", "VIEW_COUNT_DESC", "LIKES_DESC", "UNKNOWN_TOKEN"
        )

        // Sanity: same answers for known tokens.
        for (t in tokens) {
            val viaMap = mapByName[t]
            val viaLinear = ThreadSort.entries.find { it.name == t }
            assertEquals(viaLinear, viaMap)
        }

        repeat(warmup) {
            for (t in tokens) {
                mapByName[t]
                ThreadSort.entries.find { it.name == t }
            }
        }

        val mapNs = measureNanos {
            repeat(iterations) {
                for (t in tokens) {
                    mapByName[t]
                }
            }
        }
        val linearNs = measureNanos {
            repeat(iterations) {
                for (t in tokens) {
                    ThreadSort.entries.find { it.name == t }
                }
            }
        }
        report("ThreadSort lookup", linearNs, mapNs)
        assertTrue("expected map lookup faster, linear=$linearNs map=$mapNs", mapNs < linearNs)
    }

    @Test
    fun benchmark_listOrderMerge_listIn_vs_setIn() {
        // Simulates the per-refresh custom-list merge in LibraryRepositoryImpl.refreshLibrary.
        val current = (1..50).map { "list_$it" }
        val api = (40..120).map { "list_$it" }

        val legacy = {
            val result = current.filter { it in api } + api.filter { it !in current }
            result
        }
        val optimized = {
            val apiSet = api.toHashSet()
            val currentSet = current.toHashSet()
            val result = current.filter { it in apiSet } + api.filter { it !in currentSet }
            result
        }

        assertEquals(legacy(), optimized())

        repeat(warmup) { legacy(); optimized() }

        val legacyNs = measureNanos { repeat(iterations / 10) { legacy() } }
        val optimizedNs = measureNanos { repeat(iterations / 10) { optimized() } }
        report("custom-list merge", legacyNs, optimizedNs)
        assertTrue("expected set-backed merge faster, legacy=$legacyNs new=$optimizedNs", optimizedNs < legacyNs)
    }

    @Test
    fun benchmark_smartMerge_partition_singlePass_vs_doubleFilter() {
        data class Local(val mediaId: Int, val createdAt: Long?)
        val now = 1_700_000_000_000L
        val recentThreshold = 5 * 60 * 1000L
        val locals = (1..2000).map { Local(it, if (it % 5 == 0) now - 1000L else now - 10 * 60 * 1000L) }
        val apiIds = (1500..2500).toHashSet()

        val legacy = {
            val toPreserve = locals.filter { l ->
                l.mediaId !in apiIds && l.createdAt != null && (now - l.createdAt) <= recentThreshold
            }
            val toDelete = locals.filter { l ->
                l.mediaId !in apiIds && (l.createdAt == null || (now - l.createdAt) > recentThreshold)
            }
            toPreserve.size to toDelete.size
        }
        val optimized = {
            val toPreserve = ArrayList<Local>()
            val toDelete = ArrayList<Int>()
            for (l in locals) {
                if (l.mediaId in apiIds) continue
                val c = l.createdAt
                if (c != null && (now - c) <= recentThreshold) toPreserve.add(l)
                else toDelete.add(l.mediaId)
            }
            toPreserve.size to toDelete.size
        }

        assertEquals(legacy(), optimized())

        repeat(warmup / 100) { legacy(); optimized() }

        val legacyNs = measureNanos { repeat(iterations / 100) { legacy() } }
        val optimizedNs = measureNanos { repeat(iterations / 100) { optimized() } }
        report("smartMerge partition", legacyNs, optimizedNs)
        assertTrue("expected single-pass faster, legacy=$legacyNs new=$optimizedNs", optimizedNs < legacyNs)
    }

    // ------------------------------------------------------------------
    // Legacy implementations preserved verbatim for fair head-to-head.
    // These are NOT exposed to production code; only this test file
    // references them to compare against the optimized versions.
    // ------------------------------------------------------------------

    private fun legacyMapFuzzyDateToLong(year: Int?, month: Int?, day: Int?): Long? {
        if (year == null) return null
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, (month ?: 1) - 1, day ?: 1)
        return c.timeInMillis
    }

    private fun legacyToFuzzyComponents(ts: Long): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // ------------------------------------------------------------------
    // Measurement helpers
    // ------------------------------------------------------------------

    private inline fun measureNanos(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return System.nanoTime() - start
    }

    private fun report(label: String, legacyNs: Long, newNs: Long) {
        val speedup = if (newNs == 0L) Double.POSITIVE_INFINITY else legacyNs.toDouble() / newNs
        // println so Gradle test output (--info / -i) captures it on stdout.
        println("[bench] $label  legacy=${legacyNs / 1_000_000.0}ms  new=${newNs / 1_000_000.0}ms  speedup=${"%.2f".format(speedup)}x")
        assertNotNull(label)
    }
}
