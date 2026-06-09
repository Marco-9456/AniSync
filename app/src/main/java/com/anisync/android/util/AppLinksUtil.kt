package com.anisync.android.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
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
     * Opens [url] in a web browser, never in AniSync. anilist.co is a verified app link, so a plain
     * ACTION_VIEW is routed straight to AniSync (stable or debug) and skips any chooser — which is
     * why excluding components doesn't help. Instead we locate an installed browser via a neutral
     * host AniSync does not handle, then target that package explicitly; an explicit package
     * overrides app-link verification routing. Falls back to a chooser that excludes AniSync.
     */
    fun openInBrowser(context: Context, url: String) {
        val uri = url.toUri()
        val pm = context.packageManager

        val probe = Intent(Intent.ACTION_VIEW, "https://www.example.com".toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val browserPackage = pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .firstOrNull { !it.startsWith("com.anisync.android") }

        val view = Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            if (browserPackage != null) {
                context.startActivity(view.setPackage(browserPackage))
            } else {
                context.startActivity(chooserExcludingAniSync(pm, view))
            }
        } catch (_: ActivityNotFoundException) {
            runCatching {
                context.startActivity(
                    chooserExcludingAniSync(pm, Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                )
            }
        }
    }

    /** A chooser for [view] with every AniSync handler stripped out (last-resort when no browser resolves). */
    private fun chooserExcludingAniSync(pm: PackageManager, view: Intent): Intent {
        val excluded = pm.queryIntentActivities(view, PackageManager.MATCH_ALL)
            .map { it.activityInfo }
            .filter { it.packageName.startsWith("com.anisync.android") }
            .map { ComponentName(it.packageName, it.name) }
            .toTypedArray()
        return Intent.createChooser(view, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (excluded.isNotEmpty()) putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excluded)
        }
    }

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