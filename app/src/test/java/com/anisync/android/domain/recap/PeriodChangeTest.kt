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
    fun `september against august reads almost three times`() {
        assertEquals(PeriodComparison(PeriodChange.ALMOST_TIMES, 3), comparePeriods(62, 23))
    }

    @Test
    fun `whole multiples read as that many times`() {
        assertEquals(PeriodComparison(PeriodChange.TIMES, 2), comparePeriods(40, 20))
        assertEquals(PeriodComparison(PeriodChange.TIMES, 3), comparePeriods(60, 20))
        assertEquals(PeriodComparison(PeriodChange.ALMOST_TIMES, 2), comparePeriods(37, 20))
        assertEquals(PeriodComparison(PeriodChange.MORE_THAN_TIMES, 2), comparePeriods(46, 20))
    }

    @Test
    fun `past ten times reads more than ten times`() {
        assertEquals(PeriodComparison(PeriodChange.MORE_THAN_TIMES, 10), comparePeriods(300, 20))
        assertEquals(PeriodComparison(PeriodChange.TIMES, 10), comparePeriods(200, 20))
    }

    @Test
    fun `small moves read about the same`() {
        assertEquals(PeriodComparison(PeriodChange.ABOUT_THE_SAME), comparePeriods(21, 20))
        assertEquals(PeriodComparison(PeriodChange.ABOUT_THE_SAME), comparePeriods(18, 20))
        assertEquals(PeriodComparison(PeriodChange.MORE), comparePeriods(23, 20))
        assertEquals(PeriodComparison(PeriodChange.MORE), comparePeriods(36, 20))
    }

    @Test
    fun `drops read fewer or less than half`() {
        assertEquals(PeriodComparison(PeriodChange.FEWER), comparePeriods(10, 20))
        assertEquals(PeriodComparison(PeriodChange.LESS_THAN_HALF), comparePeriods(9, 20))
    }
}
