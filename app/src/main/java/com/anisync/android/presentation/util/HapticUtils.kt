package com.anisync.android.presentation.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

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
 * Provides haptic feedback utilities that respect system settings.
 * Uses both Compose's HapticFeedback API and View-based fallback for maximum compatibility.
 */
class HapticFeedbackHelper(
    private val hapticFeedback: HapticFeedback,
    private val view: View
) {
    
    /**
     * Performs haptic feedback of the specified type.
     * Uses Compose's HapticFeedback API with View-based fallback.
     * 
     * @param type The type of haptic feedback to perform
     */
    fun performHapticFeedback(type: HapticType) {
        when (type) {
            HapticType.Click -> {
                // Use Compose API first (more reliable)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                // Also trigger view-based feedback for stronger vibration on some devices
                view.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
            HapticType.LongPress -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            HapticType.Confirm -> {
                // CONFIRM constant is available on API 30+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(
                        HapticFeedbackConstants.CONFIRM,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                } else {
                    // Fallback for older devices
                    view.performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
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
 * 
 * Usage:
 * ```kotlin
 * val haptic = rememberHapticFeedback()
 * Button(onClick = { 
 *     haptic.click()
 *     // ... action
 * }) { ... }
 * ```
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackHelper {
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(hapticFeedback, view) { HapticFeedbackHelper(hapticFeedback, view) }
}
