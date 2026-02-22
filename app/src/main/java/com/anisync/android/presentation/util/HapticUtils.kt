package com.anisync.android.presentation.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * A wrapper around [View.performHapticFeedback] that respects the app's
 * internal haptic toggle **and** provides automatic fallback for newer
 * [HapticFeedbackConstants] that don't exist on older API levels.
 *
 * ### Why not Compose's [LocalHapticFeedback][androidx.compose.ui.platform.LocalHapticFeedback]?
 *
 * Compose's default implementation passes the requested constant to
 * [View.performHapticFeedback] but **ignores the `Boolean` return value**.
 * On devices where a constant is unsupported (e.g. `SEGMENT_TICK` on API < 34)
 * the call silently does nothing.
 *
 * By calling [View.performHapticFeedback] directly we can observe the return
 * value and cascade to a universally-supported fallback constant. If even
 * the fallback fails (e.g. Samsung One UI Core disables view-level haptics),
 * we use [Vibrator] directly as a last resort.
 *
 * ### Fallback chain
 *
 * 1. **Primary constant** — preserves rich, device-optimized haptics
 * 2. **Primary constant** with [HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING]
 * 3. **Fallback constant** with [HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING]
 * 4. **[Vibrator]** as a nuclear fallback (requires `VIBRATE` permission)
 */
class AppHapticFeedback(
    private val view: View,
    private val vibrator: Vibrator,
    private val isEnabledProvider: () -> Boolean
) {
    /**
     * Performs haptic feedback of the given [type] only if the app's haptic
     * setting is enabled. Tries progressively more aggressive strategies
     * until one succeeds.
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

        val primary = type.toPrimaryConstant()

        // 1. Try the ideal constant WITHOUT flags — preserves rich haptics.
        if (view.performHapticFeedback(primary)) return

        // 2. Try with FLAG_IGNORE_VIEW_SETTING (Samsung may disable the
        //    view's hapticFeedbackEnabled property).
        if (view.performHapticFeedback(
                primary,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        ) return

        // 3. Try a universally-supported fallback constant with the flag.
        val fallback = type.toFallbackConstant()
        if (fallback != null && view.performHapticFeedback(
                fallback,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        ) return

        // 4. Nuclear fallback: use the Vibrator directly.
        vibratorFallback(type)
    }

    // ------------------------------------------------------------------
    // Mapping helpers
    // ------------------------------------------------------------------

    /**
     * Maps a Compose [HapticFeedbackType] to the ideal
     * [HapticFeedbackConstants] integer. Some of these are API 34+ and will
     * silently return `false` on older devices — that's intentional; the
     * caller cascades to [toFallbackConstant].
     */
    @SuppressLint("InlinedApi") // Safe: constants are just ints, no method call
    private fun HapticFeedbackType.toPrimaryConstant(): Int = when (this) {
        HapticFeedbackType.LongPress -> HapticFeedbackConstants.LONG_PRESS
        HapticFeedbackType.TextHandleMove -> HapticFeedbackConstants.TEXT_HANDLE_MOVE
        HapticFeedbackType.Confirm -> HapticFeedbackConstants.CONFIRM
        HapticFeedbackType.SegmentTick -> HapticFeedbackConstants.SEGMENT_TICK
        HapticFeedbackType.SegmentFrequentTick -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        else -> HapticFeedbackConstants.LONG_PRESS
    }

    /**
     * Returns a universally-supported fallback constant, or `null` if the
     * primary type is already universal (e.g. [HapticFeedbackType.LongPress]).
     */
    private fun HapticFeedbackType.toFallbackConstant(): Int? = when (this) {
        HapticFeedbackType.SegmentFrequentTick -> HapticFeedbackConstants.CLOCK_TICK
        HapticFeedbackType.SegmentTick -> HapticFeedbackConstants.CLOCK_TICK
        HapticFeedbackType.Confirm -> HapticFeedbackConstants.CONTEXT_CLICK
        HapticFeedbackType.TextHandleMove -> HapticFeedbackConstants.CLOCK_TICK
        else -> null
    }

    /**
     * Last-resort vibration using the [Vibrator] service directly.
     * Uses [VibrationEffect.createPredefined] on API 29+, otherwise a short
     * one-shot vibration.
     */
    private fun vibratorFallback(type: HapticFeedbackType) {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effectId = when (type) {
                HapticFeedbackType.LongPress,
                HapticFeedbackType.Confirm -> VibrationEffect.EFFECT_HEAVY_CLICK

                HapticFeedbackType.SegmentTick,
                HapticFeedbackType.TextHandleMove -> VibrationEffect.EFFECT_TICK

                HapticFeedbackType.SegmentFrequentTick -> VibrationEffect.EFFECT_TICK
                else -> VibrationEffect.EFFECT_TICK
            }
            runCatching { vibrator.vibrate(VibrationEffect.createPredefined(effectId)) }
        } else {
            // API 26–28: simple one-shot vibration.
            val (duration, amplitude) = when (type) {
                HapticFeedbackType.LongPress,
                HapticFeedbackType.Confirm -> 30L to 180

                else -> 10L to 80
            }
            runCatching {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            }
        }
    }
}

/**
 * Remembers an [AppHapticFeedback] that uses the current [View] and
 * [Vibrator] to deliver haptic feedback while respecting the app's haptic
 * enabled setting from [LocalAppSettings].
 *
 * Usage:
 * ```
 * val haptic = rememberHapticFeedback()
 * haptic.performHapticFeedback(HapticFeedbackType.Confirm)
 * ```
 */
@Composable
fun rememberHapticFeedback(): AppHapticFeedback {
    val view = LocalView.current
    val context = LocalContext.current
    val appSettings = LocalAppSettings.current
    val hapticEnabled by appSettings.hapticEnabled.collectAsStateWithLifecycle(initialValue = true)

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    return remember(view, vibrator, hapticEnabled) {
        AppHapticFeedback(
            view = view,
            vibrator = vibrator,
            isEnabledProvider = { hapticEnabled }
        )
    }
}
