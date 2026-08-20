package com.anisync.android.widget.core

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.anisync.android.widget.UpNextWidgetProvider

/**
 * Pin-to-home-screen requests. Launchers may decline the capability entirely (the stock behaviour
 * before Android 8, and still the case on a few third-party ones), so every caller has to be able
 * to hide its entry point when [isSupported] is false rather than fire a request that goes nowhere.
 */
object WidgetPin {

    fun isSupported(context: Context): Boolean =
        runCatching { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }
            .getOrDefault(false)

    /** Asks the launcher to place an Up Next widget. Returns false when it declined outright. */
    fun requestUpNext(context: Context): Boolean = runCatching {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        manager.requestPinAppWidget(
            ComponentName(context, UpNextWidgetProvider::class.java),
            null,
            null
        )
    }.getOrDefault(false)

    /** How many AniSync widgets of any kind are currently placed. */
    fun placedCount(context: Context): Int = runCatching {
        val manager = AppWidgetManager.getInstance(context)
        manager.installedProviders
            .filter { it.provider.packageName == context.packageName }
            .sumOf { manager.getAppWidgetIds(it.provider).size }
    }.getOrDefault(0)
}
