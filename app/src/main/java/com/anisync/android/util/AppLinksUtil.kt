package com.anisync.android.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri

object AppLinksUtil {

    private const val DOMAIN_ANILIST = "anilist.co"

    fun isDomainVerified(context: Context, domain: String = DOMAIN_ANILIST): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(DomainVerificationManager::class.java)
            if (manager != null) {
                try {
                    val userState = manager.getDomainVerificationUserState(context.packageName)
                    val hostState = userState?.hostToStateMap?.get(domain)
                    return hostState == DomainVerificationUserState.DOMAIN_STATE_VERIFIED
                } catch (_: PackageManager.NameNotFoundException) {
                    // Ignore
                }
            }
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun openAppLinksSettings(context: Context) {
        try {
            // Samsung Android 12+ specific workaround to avoid Settings app crash
            if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
                val intent = Intent("android.settings.MANAGE_DOMAIN_URLS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                // Standard approach for all other manufacturers
                val intent = Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            // Ultimate fallback to App Info page if everything else fails
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }
}