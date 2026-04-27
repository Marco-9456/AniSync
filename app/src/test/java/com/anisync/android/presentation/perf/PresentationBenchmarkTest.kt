package com.anisync.android.presentation.perf

import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.util.stripHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Micro-benchmarks for presentation-layer hot paths refactored in this pass.
 *
 *  - LibraryViewModel pipeline: full sort + filter + custom-list partition for a 2000-entry
 *    library, exercising the worst-case `listOrder.indexOf(name)` path.
 *  - StringUtils.stripHtml: typical AniList description with mixed tags + entities.
 *
 * Each test runs warm-up + measured loops and asserts the optimized path is faster.
 */
class PresentationBenchmarkTest {

    private val warmup = 20
    private val iterations = 200

    // ------------------------------------------------------------------
    // Sample library with 2k entries spread across 6 custom lists and varied
    // private/hidden flags so every branch of the pipeline gets exercised.
    // ------------------------------------------------------------------
    private val customListNames = listOf("Favourites", "Top 10", "Rewatch", "Hyped", "Drop later", "Backlog")
    private val library: List<LibraryEntry> = (0 until 2000).map { i ->
        val customs = when (i % 7) {
            0 -> listOf(customListNames[0])
            1 -> listOf(customListNames[1], customListNames[2])
            2 -> listOf(customListNames[3])
            3 -> emptyList()
            4 -> listOf(customListNames[4], customListNames[5])
            5 -> listOf(customListNames[2])
            else -> emptyList()
        }
        LibraryEntry(
            id = i,
            mediaId = i,
            titleRomaji = "Romaji Title $i",
            titleEnglish = "English Title $i",
            titleNative = "ネイティブ $i",
            titleUserPreferred = "User Pref $i",
            coverUrl = null,
            progress = i % 24,
            totalEpisodes = 24,
            totalChapters = null,
            totalVolumes = null,
            type = null,
            status = LibraryStatus.entries[i % LibraryStatus.entries.size],
            score = (i % 100).toDouble(),
            updatedAt = (i * 1000L),
            createdAt = (i * 500L),
            startedAt = if (i % 3 == 0) (i * 600L) else null,
            mediaStartDate = (i * 700L),
            nextAiringEpisodeTime = if (i % 4 == 0) (i * 800L) else null,
            customLists = customs,
            isPrivate = i % 13 == 0,
            hiddenFromStatusLists = i % 17 == 0
        )
    }

    @Test
    fun benchmark_libraryPipeline_legacy_vs_optimized() {
        val listOrder = listOf("Top 10", "Hyped", "Backlog", "Favourites", "Rewatch", "Drop later")
        val showPrivate = false
        val query = "Romaji Title 12"

        val (legacyFiltered, legacyCustomNames, legacyCustomMap) =
            legacyPipeline(library, listOrder, showPrivate, query)
        val (newFiltered, newCustomNames, newCustomMap) =
            optimizedPipeline(library, listOrder, showPrivate, query)

        assertEquals(legacyFiltered.size, newFiltered.size)
        assertEquals(legacyCustomNames.toSet(), newCustomNames.toSet())
        assertEquals(legacyCustomMap.keys, newCustomMap.keys)

        repeat(warmup) {
            legacyPipeline(library, listOrder, showPrivate, query)
            optimizedPipeline(library, listOrder, showPrivate, query)
        }

        val legacyNs = measureNanos {
            repeat(iterations) { legacyPipeline(library, listOrder, showPrivate, query) }
        }
        val newNs = measureNanos {
            repeat(iterations) { optimizedPipeline(library, listOrder, showPrivate, query) }
        }
        report("library pipeline (2000 entries)", legacyNs, newNs)
        assertTrue("expected new pipeline faster, legacy=$legacyNs new=$newNs", newNs < legacyNs)
    }

    @Test
    fun benchmark_stripHtml_legacy_vs_optimized() {
        val html = ("<p>Hello <b>world</b> &amp; good <i>day</i>! " +
                "&nbsp;Some &lt;tag&gt; like &quot;text&quot; here.</p>" +
                "<div class='youtube'>video</div><br>line2 with <a href='x'>link</a>").repeat(20)

        val newOut = html.stripHtml()
        val legacyOut = legacyStripHtml(html)
        if (legacyOut != newOut) {
            // Print the first divergence so we can fix the impl quickly.
            val limit = minOf(legacyOut.length, newOut.length)
            var i = 0
            while (i < limit && legacyOut[i] == newOut[i]) i++
            val ctxStart = (i - 20).coerceAtLeast(0)
            val ctxEnd = (i + 30).coerceAtMost(limit)
            println("[diff @ $i] legacy='${legacyOut.substring(ctxStart, ctxEnd)}' new='${newOut.substring(ctxStart, ctxEnd)}'")
        }
        assertEquals(legacyOut, newOut)

        repeat(warmup * 5) {
            html.stripHtml()
            legacyStripHtml(html)
        }

        val newNs = measureNanos { repeat(iterations * 10) { html.stripHtml() } }
        val legacyNs = measureNanos { repeat(iterations * 10) { legacyStripHtml(html) } }
        report("stripHtml", legacyNs, newNs)
        assertTrue("expected new stripHtml faster, legacy=$legacyNs new=$newNs", newNs < legacyNs)
    }

    @Test
    fun benchmark_customListSort_indexOf_vs_indexMap() {
        // Direct head-to-head on the custom-list sort comparator: the legacy
        // `compareBy { listOrder.indexOf(name) }` is O(n) per probe and runs O(n log n)
        // times during sort. With a precomputed name->index map that drops to O(1).
        val names = (0 until 200).map { "list_$it" }
        val listOrder = names.shuffled()

        val legacy: () -> List<String> = {
            names.sortedWith(
                compareBy<String> { name ->
                    val i = listOrder.indexOf(name)
                    if (i == -1) Int.MAX_VALUE else i
                }.thenBy { it }
            )
        }
        val optimized: () -> List<String> = {
            val idx = HashMap<String, Int>(listOrder.size * 2)
            listOrder.forEachIndexed { i, n -> idx[n] = i }
            names.sortedWith(
                Comparator { a, b ->
                    val ia = idx[a] ?: Int.MAX_VALUE
                    val ib = idx[b] ?: Int.MAX_VALUE
                    if (ia != ib) ia.compareTo(ib) else a.compareTo(b)
                }
            )
        }

        assertEquals(legacy(), optimized())

        repeat(warmup) { legacy(); optimized() }
        val legacyNs = measureNanos { repeat(iterations) { legacy() } }
        val newNs = measureNanos { repeat(iterations) { optimized() } }
        report("custom-list name sort", legacyNs, newNs)
        assertTrue("expected map-backed sort faster, legacy=$legacyNs new=$newNs", newNs < legacyNs)
    }

    // ------------------------------------------------------------------
    // Inline copies of the legacy and optimized library pipeline. Both
    // operate on a plain List<LibraryEntry> and produce the same shape so
    // we can compare apples-to-apples.
    // ------------------------------------------------------------------

    private data class PipelineOutput(
        val filtered: List<LibraryEntry>,
        val customNames: List<String>,
        val customMap: Map<String, List<LibraryEntry>>
    )

    private fun legacyPipeline(
        entries: List<LibraryEntry>,
        listOrder: List<String>,
        showPrivate: Boolean,
        query: String
    ): PipelineOutput {
        class Sortable(val entry: LibraryEntry, val sortTitle: String)
        val sortable = entries.map { Sortable(it, it.titleUserPreferred.lowercase()) }
        val sorted = sortable.sortedWith(
            compareByDescending<Sortable> { it.entry.updatedAt }.thenBy { it.sortTitle }
        )
        val directed = sorted.reversed()
        val visibilityFiltered = directed.filter { e ->
            (showPrivate || e.entry.isPrivate != true) && !e.entry.hiddenFromStatusLists
        }
        val filtered = if (query.isBlank()) visibilityFiltered.map { it.entry }
        else {
            val q = query.lowercase()
            visibilityFiltered.filter { it.sortTitle.contains(q) }.map { it.entry }
        }
        val customNames = mutableSetOf<String>()
        val customMap = mutableMapOf<String, MutableList<LibraryEntry>>()
        directed.filter { showPrivate || it.entry.isPrivate != true }
            .forEach { s ->
                s.entry.customLists.forEach { name ->
                    customNames.add(name)
                    customMap.getOrPut(name) { mutableListOf() }.add(s.entry)
                }
            }
        customNames.addAll(listOrder)
        val sortedCustomNames = customNames.toList().sortedWith(
            compareBy<String> { name ->
                val idx = listOrder.indexOf(name)
                if (idx == -1) Int.MAX_VALUE else idx
            }.thenBy { it }
        )
        return PipelineOutput(filtered, sortedCustomNames, customMap)
    }

    private fun optimizedPipeline(
        entries: List<LibraryEntry>,
        listOrder: List<String>,
        showPrivate: Boolean,
        query: String
    ): PipelineOutput {
        class Sortable(val entry: LibraryEntry, val sortTitle: String)
        val sortable = entries.map { Sortable(it, it.titleUserPreferred.lowercase()) }
        val titleDir = -1 // descending overall (matches legacy reverse() semantics for this bench)
        val keyDir = -titleDir
        val titleCmp = Comparator<Sortable> { a, b ->
            a.sortTitle.compareTo(b.sortTitle) * titleDir
        }
        val sorted = sortable.sortedWith(
            Comparator { a, b ->
                val ka = a.entry.updatedAt
                val kb = b.entry.updatedAt
                val cmp = when {
                    ka == null && kb == null -> 0
                    ka == null -> 1
                    kb == null -> -1
                    else -> ka.compareTo(kb)
                }
                if (cmp != 0) cmp * keyDir else titleCmp.compare(a, b)
            }
        )
        val customMap = HashMap<String, MutableList<LibraryEntry>>()
        val customNames = HashSet<String>()
        val visibilityFiltered = ArrayList<Sortable>(sorted.size)
        for (s in sorted) {
            val e = s.entry
            val notPrivate = showPrivate || e.isPrivate != true
            if (notPrivate) {
                for (n in e.customLists) {
                    customNames.add(n)
                    customMap.getOrPut(n) { ArrayList() }.add(e)
                }
            }
            if (notPrivate && !e.hiddenFromStatusLists) visibilityFiltered.add(s)
        }
        customNames.addAll(listOrder)
        val filtered: List<LibraryEntry> = if (query.isBlank()) {
            ArrayList<LibraryEntry>(visibilityFiltered.size).also { for (s in visibilityFiltered) it.add(s.entry) }
        } else {
            val q = query.lowercase()
            ArrayList<LibraryEntry>(visibilityFiltered.size).also {
                for (s in visibilityFiltered) if (s.sortTitle.contains(q)) it.add(s.entry)
            }
        }
        val orderIdx = HashMap<String, Int>(listOrder.size * 2)
        listOrder.forEachIndexed { i, n -> orderIdx[n] = i }
        val sortedCustomNames = customNames.sortedWith(
            Comparator { a, b ->
                val ia = orderIdx[a] ?: Int.MAX_VALUE
                val ib = orderIdx[b] ?: Int.MAX_VALUE
                if (ia != ib) ia.compareTo(ib) else a.compareTo(b)
            }
        )
        return PipelineOutput(filtered, sortedCustomNames, customMap)
    }

    private fun legacyStripHtml(s: String): String {
        return s.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
    }

    private inline fun measureNanos(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return System.nanoTime() - start
    }

    private fun report(label: String, legacyNs: Long, newNs: Long) {
        val speedup = if (newNs == 0L) Double.POSITIVE_INFINITY else legacyNs.toDouble() / newNs
        println("[bench] $label  legacy=${legacyNs / 1_000_000.0}ms  new=${newNs / 1_000_000.0}ms  speedup=${"%.2f".format(speedup)}x")
        assertNotNull(label)
    }
}
