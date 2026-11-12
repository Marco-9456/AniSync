package com.anisync.android.data

import java.util.concurrent.TimeUnit

/** What the support card remembers between launches. */
internal data class SupportPromptState(
    /** The build the last launch ran, 0 before anything was recorded. */
    val versionCode: Int = 0,
    /** That build's name, null when it came from the old prompt, which only kept the code. */
    val versionName: String? = null,
    /** A feature update armed the card, and nobody has answered it yet. */
    val pending: Boolean = false,
    /** Nothing arms the card again before this moment. */
    val quietUntil: Long = 0L,
    /** How many times in a row the card was answered with Not now. */
    val notNowStreak: Int = 0,
    /** The Support reminders switch. */
    val remindersOn: Boolean = true,
)

/**
 * When the support card may ask. It waits for a feature update on an install at least two weeks
 * old, then for a list edit to reach AniList, and an answer keeps it quiet for months. Kept apart
 * from storage so the rules can be tested on the JVM.
 */
internal object SupportPromptRules {

    val MIN_INSTALL_AGE_MS = TimeUnit.DAYS.toMillis(14)
    val NOT_NOW_QUIET_MS = TimeUnit.DAYS.toMillis(90)
    val TIP_QUIET_MS = TimeUnit.DAYS.toMillis(365)

    /** Not now this many times in a row turns reminders off. */
    const val NOT_NOW_LIMIT = 2

    fun onLaunch(
        state: SupportPromptState,
        versionCode: Int,
        versionName: String,
        installedAt: Long,
        now: Long,
    ): SupportPromptState {
        if (state.versionCode == versionCode) return state
        val recorded = state.copy(versionCode = versionCode, versionName = versionName)
        // A fresh install, or a downgrade: nothing new has been delivered.
        if (state.versionCode == 0 || versionCode < state.versionCode) return recorded
        val arm = state.remindersOn &&
            isFeatureUpdate(state.versionName, versionName) &&
            now >= state.quietUntil &&
            now - installedAt >= MIN_INSTALL_AGE_MS
        return if (arm) recorded.copy(pending = true) else recorded
    }

    fun canShow(state: SupportPromptState): Boolean = state.pending && state.remindersOn

    fun onNotNow(state: SupportPromptState, now: Long): SupportPromptState {
        val streak = state.notNowStreak + 1
        return state.copy(
            pending = false,
            quietUntil = now + NOT_NOW_QUIET_MS,
            notNowStreak = streak,
            remindersOn = state.remindersOn && streak < NOT_NOW_LIMIT,
        )
    }

    /** A tip page was opened, from the card or the Support screen. */
    fun onTipOpened(state: SupportPromptState, now: Long): SupportPromptState = state.copy(
        pending = false,
        quietUntil = maxOf(state.quietUntil, now + TIP_QUIET_MS),
        notNowStreak = 0,
    )

    fun withReminders(state: SupportPromptState, on: Boolean): SupportPromptState =
        if (on) state.copy(remindersOn = true, notNowStreak = 0) else state.copy(remindersOn = false, pending = false)

    /**
     * Whether going from [previous] to [current] brought new features: the first two parts of the
     * version moved, as from 3.4.2 to 3.5.0, and not only the third, as from 3.5.0 to 3.5.1. With
     * no previous name to compare, a version ending in .0 counts.
     */
    fun isFeatureUpdate(previous: String?, current: String): Boolean {
        val to = parts(current) ?: return true
        val from = previous?.let(::parts) ?: return to.patch == 0
        return to.major != from.major || to.minor != from.minor
    }

    private data class Version(val major: Int, val minor: Int, val patch: Int)

    private fun parts(name: String): Version? {
        val numbers = name.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }
        if (numbers.size < 2) return null
        return Version(numbers[0], numbers[1], numbers.getOrElse(2) { 0 })
    }
}
