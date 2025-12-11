package com.anisync.android.presentation.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.anisync.android.data.AppSettings

/**
 * Types of haptic feedback available for UI interactions.
 */
enum class HapticType {
    /** Light feedback for button clicks and chip selection */
    Click,
    /** Medium feedback for long press actions */
    LongPress,
    /** Strong feedback for successful action confirmation */
    Confirm
}

/**
 * Provides haptic feedback utilities using the system Vibrator.
 * Bypasses system "Touch feedback" setting to respect the app's internal "Haptic Feedback" toggle.
 * 
 * @param context Context used to retrieve the Vibrator service
 * @param isEnabledProvider Lambda that returns current enabled state (checked on each call)
 */
class HapticFeedbackHelper(
    context: Context,
    private val isEnabledProvider: () -> Boolean
) {
    
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(android.os.VibratorManager::class.java)
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Performs haptic feedback of the specified type.
     * Only performs feedback if haptic is enabled in app settings.
     * Uses Vibrator directly to ensure feedback is delivered.
     * 
     * @param type The type of haptic feedback to perform
     */
    fun performHapticFeedback(type: HapticType) {
        // Check if haptic feedback is enabled in app settings
        if (!isEnabledProvider()) return
        
        // Check if device has a vibrator
        if (vibrator == null || !vibrator.hasVibrator()) return

        try {
            when (type) {
                HapticType.Click -> vibrateClick()
                HapticType.LongPress -> vibrateLongPress()
                HapticType.Confirm -> vibrateConfirm()
            }
        } catch (e: Exception) {
            // Fail silently if vibration fails
        }
    }
    
    private fun vibrateClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            // Fallback for API 26-28
            vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibrateLongPress() {
        // Long press is typically slightly longer/heavier
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Heavy click or Tick can represent long press depending on preference, 
            // but standard Heavy Click is good.
           vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibrateConfirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            // Double pulse
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 10, 50, 10), -1))
        }
    }
    
    /**
     * Convenience method for light click feedback.
     */
    fun click() = performHapticFeedback(HapticType.Click)
    
    /**
     * Convenience method for long press feedback.
     */
    fun longPress() = performHapticFeedback(HapticType.LongPress)
    
    /**
     * Convenience method for confirmation feedback.
     */
    fun confirm() = performHapticFeedback(HapticType.Confirm)
}

/**
 * Remembers a HapticFeedbackHelper instance for use in composables.
 * Reads haptic enabled setting from AppSettings.
 * 
 * @param appSettings The app settings to check for haptic enabled state
 */
@Composable
fun rememberHapticFeedback(appSettings: AppSettings): HapticFeedbackHelper {
    val context = LocalContext.current
    val hapticEnabled by appSettings.hapticEnabled.collectAsState()
    
    return remember(context) { 
        HapticFeedbackHelper(
            context = context,
            isEnabledProvider = { appSettings.hapticEnabled.value }
        )
    }
}

/**
 * Remembers a HapticFeedbackHelper that respects app settings.
 * Automatically reads the haptic enabled setting from LocalAppSettings.
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackHelper {
    val context = LocalContext.current
    val appSettings = LocalAppSettings.current
    // We observe state here to trigger recomposition if setting changes, 
    // ensuring validity of the closure if needed, though provider handles value reading.
    val hapticEnabled by appSettings.hapticEnabled.collectAsState()
    
    return remember(context, appSettings) { 
        HapticFeedbackHelper(
            context = context,
            isEnabledProvider = { appSettings.hapticEnabled.value }
        )
    }
}


