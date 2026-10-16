package com.anisync.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The preference stores the constant's name, so renaming one silently resets everyone who picked
 * it back to Default.
 */
class DisplaySizeTest {

    @Test
    fun `stored names keep their scale`() {
        val expected = mapOf("SMALL" to 0.9f, "DEFAULT" to 1f, "LARGE" to 1.1f, "LARGER" to 1.2f)
        assertEquals(expected, DisplaySize.entries.associate { it.name to it.scale })
    }

    @Test
    fun `largest step keeps a 411dp phone at 320dp or more`() {
        val largest = DisplaySize.entries.maxOf { it.scale }
        assertTrue(411 / largest >= 320f)
    }
}
