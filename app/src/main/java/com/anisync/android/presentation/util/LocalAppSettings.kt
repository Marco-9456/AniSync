package com.anisync.android.presentation.util

import androidx.compose.runtime.compositionLocalOf
import com.anisync.android.data.AppSettings

/**
 * CompositionLocal for providing AppSettings throughout the app's Compose UI.
 * This allows any composable to access app settings without explicit parameter passing.
 */
val LocalAppSettings = compositionLocalOf<AppSettings> { 
    error("AppSettings not provided. Make sure to provide it using CompositionLocalProvider.") 
}
