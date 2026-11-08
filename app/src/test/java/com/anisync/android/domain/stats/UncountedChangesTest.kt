package com.anisync.android.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class UncountedChangesTest {

    @Test
    fun `lists that match the count have nothing uncounted`() {
        val lists = mapOf("CURRENT" to 6, "COMPLETED" to 2)
        assertEquals(0, uncountedChanges(lists, lists))
    }

    @Test
    fun `titles added since the recount are uncounted`() {
        // AniList last counted 6 watching, the library now also holds 2 completed.
        assertEquals(2, uncountedChanges(mapOf("CURRENT" to 6, "COMPLETED" to 2), mapOf("CURRENT" to 6)))
    }

    @Test
    fun `a title moved between lists counts once`() {
        assertEquals(1, uncountedChanges(mapOf("CURRENT" to 5, "COMPLETED" to 3), mapOf("CURRENT" to 6, "COMPLETED" to 2)))
    }

    @Test
    fun `removed titles are not counted as changes`() {
        assertEquals(0, uncountedChanges(mapOf("CURRENT" to 4), mapOf("CURRENT" to 6)))
    }

    @Test
    fun `a rewatch counts with watching, as AniList counts it`() {
        // Marco9456 on 2026-10-29: the lists hold 6 watching and 1 rewatching, the statistics 7 watching.
        val lists = mapOf("COMPLETED" to 222, "CURRENT" to 6, "REPEATING" to 1, "PLANNING" to 126, "PAUSED" to 10, "DROPPED" to 5)
        val counted = mapOf("COMPLETED" to 211, "PLANNING" to 122, "PAUSED" to 12, "CURRENT" to 7, "DROPPED" to 3)
        assertEquals(17, uncountedChanges(lists, counted))
    }
}
