package com.anisync.android.presentation.components.filtersheet

import org.junit.Assert.assertEquals
import org.junit.Test

class TriStateTest {

    @Test
    fun `a tap cycles off, included, excluded and back`() {
        assertEquals(TriState.INCLUDED, TriState.OFF.next())
        assertEquals(TriState.EXCLUDED, TriState.INCLUDED.next())
        assertEquals(TriState.OFF, TriState.EXCLUDED.next())
    }

    @Test
    fun `a long press excludes straight away and clears an exclusion`() {
        assertEquals(TriState.EXCLUDED, TriState.OFF.longPressed())
        assertEquals(TriState.EXCLUDED, TriState.INCLUDED.longPressed())
        assertEquals(TriState.OFF, TriState.EXCLUDED.longPressed())
    }
}
