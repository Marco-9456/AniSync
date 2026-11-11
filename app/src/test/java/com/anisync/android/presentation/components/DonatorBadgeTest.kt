package com.anisync.android.presentation.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The website's rules, read off its activity, forum and profile components. */
class DonatorBadgeTest {

    @Test
    fun `next to a name the first three tiers read Donator`() {
        (1..3).forEach { tier ->
            assertNull(donatorBadgeText(tier, "Senpai", DonatorBadgePlacement.NextToName))
            assertFalse(isRainbowDonator(tier, DonatorBadgePlacement.NextToName))
        }
    }

    @Test
    fun `tier four shows its own text and five adds the rainbow`() {
        assertEquals("Senpai", donatorBadgeText(4, "Senpai", DonatorBadgePlacement.NextToName))
        assertFalse(isRainbowDonator(4, DonatorBadgePlacement.NextToName))
        assertEquals("Senpai", donatorBadgeText(5, "Senpai", DonatorBadgePlacement.NextToName))
        assertTrue(isRainbowDonator(5, DonatorBadgePlacement.NextToName))
    }

    @Test
    fun `the profile header always shows the text over the rainbow`() {
        assertEquals("Senpai", donatorBadgeText(1, "Senpai", DonatorBadgePlacement.ProfileHeader))
        assertTrue(isRainbowDonator(1, DonatorBadgePlacement.ProfileHeader))
        assertFalse(isRainbowDonator(0, DonatorBadgePlacement.ProfileHeader))
    }

    @Test
    fun `a blank text falls back to Donator`() {
        assertNull(donatorBadgeText(5, "  ", DonatorBadgePlacement.ProfileHeader))
        assertNull(donatorBadgeText(5, null, DonatorBadgePlacement.NextToName))
    }

    @Test
    fun `the rainbow runs the website's keyframes`() {
        assertClose(Color(0, 105, 255, 181), rainbowAt(0f))
        assertClose(Color(255, 0, 0, 181), rainbowAt(0.30f))
        assertClose(Color(0, 255, 139, 181), rainbowAt(0.60f))
        assertClose(Color(0, 105, 255, 181), rainbowAt(1f))
    }

    @Test
    fun `a name and badge that fit both keep their whole width`() {
        assertEquals(NameAndBadgeWidths(80, 50), nameAndBadgeWidths(available = 200, name = 80, badge = 50, gap = 6, minBadge = 40))
    }

    @Test
    fun `a long badge gives way to the name and ends where the line does`() {
        assertEquals(NameAndBadgeWidths(80, 74), nameAndBadgeWidths(available = 160, name = 80, badge = 300, gap = 6, minBadge = 40))
    }

    @Test
    fun `a badge with too little room beside the whole name stays out`() {
        assertEquals(NameAndBadgeWidths(80, null), nameAndBadgeWidths(available = 120, name = 80, badge = 300, gap = 6, minBadge = 40))
        assertEquals(NameAndBadgeWidths(100, null), nameAndBadgeWidths(available = 100, name = 150, badge = 50, gap = 6, minBadge = 40))
    }

    @Test
    fun `a badge shorter than the minimum still shows when it fits`() {
        assertEquals(NameAndBadgeWidths(80, 30), nameAndBadgeWidths(available = 116, name = 80, badge = 30, gap = 6, minBadge = 40))
    }

    @Test
    fun `no badge or no limit leaves the name alone`() {
        assertEquals(NameAndBadgeWidths(90, null), nameAndBadgeWidths(available = 90, name = 120, badge = null, gap = 6, minBadge = 40))
        assertEquals(NameAndBadgeWidths(120, 300), nameAndBadgeWidths(available = null, name = 120, badge = 300, gap = 6, minBadge = 40))
    }

    private fun assertClose(expected: Color, actual: Color) {
        val tolerance = 1.5f / 255f
        assertEquals(expected.red, actual.red, tolerance)
        assertEquals(expected.green, actual.green, tolerance)
        assertEquals(expected.blue, actual.blue, tolerance)
        assertEquals(expected.alpha, actual.alpha, tolerance)
    }
}
