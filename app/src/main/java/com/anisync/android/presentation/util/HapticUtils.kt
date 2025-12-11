package com.anisync.android.presentation.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
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
 * Provides haptic feedback utilities that respect both system and app settings.
 * Uses both Compose's HapticFeedback API and View-based fallback for maximum compatibility.
 * 
 * @param hapticFeedback Compose haptic feedback API
 * @param view Android View for fallback haptics
 * @param isEnabledProvider Lambda that returns current enabled state (checked on each call)
 */
class HapticFeedbackHelper(
    private val hapticFeedback: HapticFeedback,
    private val view: View,
    private val isEnabledProvider: () -> Boolean
) {
    
    /**
     * Performs haptic feedback of the specified type.
     * Only performs feedback if haptic is enabled in app settings.
     * 
     * @param type The type of haptic feedback to perform
     */
    fun performHapticFeedback(type: HapticType) {
        // Check if haptic feedback is enabled in app settings
        if (!isEnabledProvider()) return
        
        when (type) {
            HapticType.Click -> {
                // Use Compose API (reliable and respects system settings)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                // Also trigger view-based feedback for stronger vibration on some devices
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            HapticType.LongPress -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            HapticType.Confirm -> {
                // CONFIRM constant is available on API 30+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    // Fallback for older devices
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            }
        }
    }
    
    /**
     * Convenience method for light click feedback.
     * Use this for buttons, chips, toggles, etc.
     */
    fun click() = performHapticFeedback(HapticType.Click)
    
    /**
     * Convenience method for long press feedback.
     * Use this for long-press actions and context menus.
     */
    fun longPress() = performHapticFeedback(HapticType.LongPress)
    
    /**
     * Convenience method for confirmation feedback.
     * Use this for successful action completion.
     */
    fun confirm() = performHapticFeedback(HapticType.Confirm)
}

/**
 * Remembers a HapticFeedbackHelper instance for use in composables.
 * Reads haptic enabled setting from AppSettings.
 * 
 * Usage:
 * ```kotlin
 * val haptic = rememberHapticFeedback(appSettings)
 * Button(onClick = { 
 *     haptic.click()
 *     // ... action
 * }) { ... }
 * ```
 * 
 * @param appSettings The app settings to check for haptic enabled state
 */
@Composable
fun rememberHapticFeedback(appSettings: AppSettings): HapticFeedbackHelper {
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    val hapticEnabled by appSettings.hapticEnabled.collectAsState()
    
    // Create a stable reference that captures the current enabled state
    return remember(hapticFeedback, view) { 
        HapticFeedbackHelper(
            hapticFeedback = hapticFeedback,
            view = view,
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
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    val appSettings = LocalAppSettings.current
    
    return remember(hapticFeedback, view) { 
        HapticFeedbackHelper(
            hapticFeedback = hapticFeedback,
            view = view,
            isEnabledProvider = { appSettings.hapticEnabled.value }
        )
    }
}

