package com.anisync.android.domain.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodChangeTest {

    @Test
    fun `nothing to compare with has no change`() {
        assertNull(comparePeriods(51, null))
        assertNull(comparePeriods(51, 0))
    }

    @Test
    fun `more than double reads more than twice`() {
        assertEquals(PeriodChange.MORE_THAN_TWICE, comparePeriods(62, 23))
        assertEquals(PeriodChange.MORE, comparePeriods(40, 20))
    }

    @Test
    fun `small moves read about the same`() {
        assertEquals(PeriodChange.ABOUT_THE_SAME, comparePeriods(21, 20))
        assertEquals(PeriodChange.ABOUT_THE_SAME, comparePeriods(18, 20))
        assertEquals(PeriodChange.MORE, comparePeriods(23, 20))
    }

    @Test
    fun `drops read fewer or less than half`() {
        assertEquals(PeriodChange.FEWER, comparePeriods(10, 20))
        assertEquals(PeriodChange.LESS_THAN_HALF, comparePeriods(9, 20))
    }
}
