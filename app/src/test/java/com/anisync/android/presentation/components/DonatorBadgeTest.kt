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

    private fun assertClose(expected: Color, actual: Color) {
        val tolerance = 1.5f / 255f
        assertEquals(expected.red, actual.red, tolerance)
        assertEquals(expected.green, actual.green, tolerance)
        assertEquals(expected.blue, actual.blue, tolerance)
        assertEquals(expected.alpha, actual.alpha, tolerance)
    }
}
