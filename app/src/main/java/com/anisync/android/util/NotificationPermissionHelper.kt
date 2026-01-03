package com.anisync.android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Helper to check and monitor notification permission status.
 * Handles both runtime permission (Android 13+) and system settings.
 */
object NotificationPermissionHelper {
    
    /**
     * Check if the app has notification permission.
     * Returns true if:
     * - Android < 13: Notifications are enabled in system settings
     * - Android 13+: POST_NOTIFICATIONS permission is granted AND notifications are enabled
     */
    fun hasNotificationPermission(context: Context): Boolean {
        val systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            permissionGranted && systemEnabled
        } else {
            systemEnabled
        }
    }
    
    /**
     * Check if notifications are enabled in system settings.
     * This covers the case where user disables from system settings.
     */
    fun areNotificationsEnabledInSystem(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    /**
     * Check if we need to request the POST_NOTIFICATIONS permission.
     * Only applicable on Android 13+.
     */
    fun needsRuntimePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
