package com.anisync.android.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/**
 * The two background-work escapes AniSync asks for during onboarding: an exemption from Doze
 * batching, and an opt-out of the "pause app activity if unused" hibernation Android applies after
 * months of disuse. Both decide whether the 15-minute notification poll actually runs on time.
 */
object BackgroundWorkUtil {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return runCatching { power.isIgnoringBatteryOptimizations(context.packageName) }
            .getOrDefault(false)
    }

    /**
     * Opens the one-tap system dialog that grants the exemption. Only an app holding
     * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS is offered it, so ROMs that strip the dialog fall through
     * to the full battery-optimization list.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData("package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(direct)
        } catch (_: ActivityNotFoundException) {
            openBatteryOptimizationSettings(context)
        }
    }

    private fun openBatteryOptimizationSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { openAppDetails(context) }
    }

    /** True when Android will not hibernate AniSync after months idle. Always true below API 30. */
    fun isHibernationExempt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        return runCatching { context.packageManager.isAutoRevokeWhitelisted }.getOrDefault(false)
    }

    /**
     * Opens the screen carrying the "Pause app activity if unused" toggle. There is no dialog to
     * grant this, so the user turns it off themselves.
     */
    fun openHibernationSettings(context: Context) {
        val intent = Intent(Intent.ACTION_AUTO_REVOKE_PERMISSIONS)
            .setData("package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            openAppDetails(context)
        }
    }

    private fun openAppDetails(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData("package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
