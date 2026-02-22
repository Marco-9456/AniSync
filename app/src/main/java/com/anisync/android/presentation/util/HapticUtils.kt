package com.anisync.android.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * A thin wrapper around Compose's [HapticFeedback] that respects the app's
 * internal haptic toggle.
 *
 * This delegates all haptic work to Compose's [LocalHapticFeedback], which
 * internally uses [android.view.View.performHapticFeedback] with
 * [android.view.HapticFeedbackConstants]. This is the recommended approach per
 * Android's haptics documentation because:
 *
 * - **No VIBRATE permission needed.**
 * - **Built-in device-optimized fallbacks** — the OS automatically provides the
 *   best effect for the device's hardware.
 * - **Action-oriented constants** — effects are consistent with the system UX.
 * - **Respects system Touch Feedback setting.**
 *
 * The only addition over raw [LocalHapticFeedback] is gating haptic calls behind
 * the app's own `hapticEnabled` preference.
 */
class AppHapticFeedback(
    private val composeFeedback: HapticFeedback,
    private val isEnabledProvider: () -> Boolean
) {
    /**
     * Performs haptic feedback of the given [type] only if the app's haptic
     * setting is enabled.
     *
     * Choose [type] based on interaction frequency and importance:
     *
     * | Frequency / Importance | Recommended type |
     * |---|---|
     * | High freq, low importance (sliders) | [HapticFeedbackType.SegmentFrequentTick] |
     * | Medium freq, low importance (chips, toggles) | [HapticFeedbackType.TextHandleMove] |
     * | Low freq, medium importance (score ticks) | [HapticFeedbackType.SegmentTick] |
     * | Low freq, high importance (favorite) | [HapticFeedbackType.LongPress] |
     * | Low freq, high importance (save/submit) | [HapticFeedbackType.Confirm] |
     */
    fun performHapticFeedback(type: HapticFeedbackType) {
        if (!isEnabledProvider()) return
        composeFeedback.performHapticFeedback(type)
    }
}

/**
 * Remembers an [AppHapticFeedback] that delegates to Compose's
 * [LocalHapticFeedback] while respecting the app's haptic enabled setting
 * from [LocalAppSettings].
 *
 * Usage:
 * ```
 * val haptic = rememberHapticFeedback()
 * haptic.performHapticFeedback(HapticFeedbackType.Confirm)
 * ```
 */
@Composable
fun rememberHapticFeedback(): AppHapticFeedback {
    val composeFeedback = LocalHapticFeedback.current
    val appSettings = LocalAppSettings.current
    // Observe the setting so recomposition is triggered if it changes.
    val hapticEnabled by appSettings.hapticEnabled.collectAsStateWithLifecycle(initialValue = true)

    return remember(composeFeedback, hapticEnabled) {
        AppHapticFeedback(
            composeFeedback = composeFeedback,
            isEnabledProvider = { hapticEnabled }
        )
    }
}
