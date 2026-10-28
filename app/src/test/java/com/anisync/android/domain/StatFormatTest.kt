package com.anisync.android.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StatFormatTest {

    @Test
    fun `minutes split into days, hours and minutes`() {
        assertEquals(DurationParts(168, 9, 29), splitMinutes(168 * 1440 + 9 * 60 + 29))
        assertEquals(DurationParts(0, 17, 9), splitMinutes(17 * 60 + 9))
        assertEquals(DurationParts(0, 0, 0), splitMinutes(-5))
    }

    @Test
    fun `hours keep one decimal, rounded half up`() {
        assertEquals("15.1", formatHours(906, Locale.US))
        assertEquals("16.6", formatHours(996, Locale.US))
        assertEquals("0.1", formatHours(3, Locale.US))
        assertEquals("15,1", formatHours(906, Locale.GERMANY))
    }

    @Test
    fun `a rate keeps one decimal`() {
        assertEquals("2.7", formatOneDecimal(489.0 / 183, Locale.US))
        assertEquals("2.7", formatOneDecimal(2.65, Locale.US))
        assertEquals("3.0", formatOneDecimal(3.0, Locale.US))
    }

    @Test
    fun `personal totals stay exact`() {
        assertEquals("13,170", formatExactCount(13_170, Locale.US))
        assertEquals("13.170", formatExactCount(13_170, Locale.GERMANY))
    }

    @Test
    fun `community counts are exact up to 9,999`() {
        assertEquals("1,668", formatCommunityCount(1_668, Locale.US))
        assertEquals("9,999", formatCommunityCount(9_999, Locale.US))
    }

    @Test
    fun `larger community counts cut down without rounding up`() {
        assertEquals("10K", formatCommunityCount(10_000, Locale.US))
        assertEquals("28K", formatCommunityCount(28_410, Locale.US))
        assertEquals("339K", formatCommunityCount(339_876, Locale.US))
        assertEquals("1.2M", formatCommunityCount(1_299_999, Locale.US))
        assertEquals("12M", formatCommunityCount(12_345_678, Locale.US))
    }
}
