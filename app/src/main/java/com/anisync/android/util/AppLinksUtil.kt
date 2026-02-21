package com.anisync.android.util

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

    /**
     * Checks if the app is verified to open the specified domain.
     * On Android 12+ (API 31+), checks the DomainVerificationManager.
     * On earlier versions, returns true since App Links prompt is not required in the same way.
     */
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

    /**
     * Opens the "Open by default" settings page for the app to allow the user
     * to manually add verified links.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun openAppLinksSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
