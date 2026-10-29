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
}
