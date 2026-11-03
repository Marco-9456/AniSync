package com.anisync.android.presentation.recap

import org.junit.Assert.assertEquals
import org.junit.Test

class RecapMonogramTest {

    @Test
    fun `two words give their first letters`() {
        assertEquals("KA", monogram("Kyoto Animation"))
        assertEquals("SK", monogram("Studio Khara"))
        assertEquals("A1", monogram("A-1 Pictures"))
        assertEquals("TE", monogram("TMS Entertainment"))
    }

    @Test
    fun `one word takes its capitals when it mixes cases`() {
        assertEquals("GH", monogram("GoHands"))
        assertEquals("JC", monogram("J.C.Staff"))
    }

    @Test
    fun `one word in one case takes its first two letters`() {
        assertEquals("MA", monogram("MAPPA"))
        assertEquals("XE", monogram("Xebec"))
        assertEquals("UF", monogram("ufotable"))
    }
}
