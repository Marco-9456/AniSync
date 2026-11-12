package com.anisync.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SupportPromptRulesTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val installedAt = 1_000L * day
    private val now = installedAt + 30 * day

    private val on34 = SupportPromptState(versionCode = 23, versionName = "3.4.0")

    private fun launch(state: SupportPromptState, code: Int, name: String, at: Long = now) =
        SupportPromptRules.onLaunch(state, code, name, installedAt, at)

    @Test
    fun freshInstallNeverAsks() {
        val state = launch(SupportPromptState(), 24, "3.5.0")
        assertFalse(state.pending)
        assertEquals(24, state.versionCode)
    }

    @Test
    fun featureUpdateArmsTheCard() {
        assertTrue(launch(on34, 24, "3.5.0").pending)
    }

    @Test
    fun fixReleaseDoesNotAsk() {
        assertFalse(launch(SupportPromptState(versionCode = 24, versionName = "3.5.0"), 25, "3.5.1").pending)
    }

    @Test
    fun updateFromTheOldPromptCountsWhenItEndsInZero() {
        val legacy = SupportPromptState(versionCode = 23)
        assertTrue(launch(legacy, 24, "3.5.0").pending)
        assertFalse(launch(legacy, 24, "3.5.1").pending)
    }

    @Test
    fun previewSuffixIsIgnored() {
        assertTrue(SupportPromptRules.isFeatureUpdate("3.4.0", "3.5.0-preview"))
        assertFalse(SupportPromptRules.isFeatureUpdate("3.5.0-preview", "3.5.1-preview"))
    }

    @Test
    fun youngInstallWaits() {
        val state = SupportPromptRules.onLaunch(on34, 24, "3.5.0", installedAt = now - 3 * day, now = now)
        assertFalse(state.pending)
    }

    @Test
    fun relaunchOnTheSameBuildChangesNothing() {
        val armed = launch(on34, 24, "3.5.0")
        assertEquals(armed, launch(armed, 24, "3.5.0", at = now + day))
    }

    @Test
    fun notNowKeepsQuietForNinetyDays() {
        val answered = SupportPromptRules.onNotNow(launch(on34, 24, "3.5.0"), now)
        assertFalse(answered.pending)
        assertFalse(launch(answered, 25, "3.6.0", at = now + 30 * day).pending)
        assertTrue(launch(answered, 25, "3.6.0", at = now + 91 * day).pending)
    }

    @Test
    fun twoNotNowAnswersInARowTurnRemindersOff() {
        val first = SupportPromptRules.onNotNow(launch(on34, 24, "3.5.0"), now)
        assertTrue(first.remindersOn)
        val second = SupportPromptRules.onNotNow(launch(first, 25, "3.6.0", at = now + 91 * day), now + 91 * day)
        assertFalse(second.remindersOn)
        assertFalse(launch(second, 26, "3.7.0", at = now + 400 * day).pending)
    }

    @Test
    fun openingATipPageKeepsQuietForAYear() {
        val tipped = SupportPromptRules.onTipOpened(launch(on34, 24, "3.5.0"), now)
        assertFalse(tipped.pending)
        assertFalse(launch(tipped, 25, "3.6.0", at = now + 200 * day).pending)
        assertTrue(launch(tipped, 25, "3.6.0", at = now + 366 * day).pending)
    }

    @Test
    fun tippingResetsTheNotNowStreak() {
        val once = SupportPromptRules.onNotNow(on34, now)
        assertEquals(0, SupportPromptRules.onTipOpened(once, now).notNowStreak)
    }

    @Test
    fun remindersOffDropsAnArmedCard() {
        val off = SupportPromptRules.withReminders(launch(on34, 24, "3.5.0"), on = false)
        assertFalse(SupportPromptRules.canShow(off))
        assertFalse(SupportPromptRules.withReminders(off, on = true).pending)
    }
}
