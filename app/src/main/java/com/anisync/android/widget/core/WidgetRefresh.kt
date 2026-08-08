package com.anisync.android.widget.core

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.anisync.android.widget.AiringTodayWidgetProvider
import com.anisync.android.widget.TrendingWidgetProvider
import com.anisync.android.widget.UpNextWidgetProvider
import com.anisync.android.widget.WatchProgressWidgetProvider
import com.anisync.android.widget.WeeklyCalendarWidgetProvider

/**
 * How the rest of the app pushes fresh data onto the home screen.
 *
 * Sends an explicit APPWIDGET_UPDATE to our own providers. It lands in onUpdate and renders in the
 * same pass, nothing scheduled or queued, so bumping progress shows on the widget while the user is
 * still looking at it.
 *
 * Callers do not have to know which widgets are placed. A provider with no instances gets an empty
 * id array and does nothing.
 */
object WidgetRefresh {

    private val allProviders: List<Class<out AppWidgetProvider>> = listOf(
        UpNextWidgetProvider::class.java,
        AiringTodayWidgetProvider::class.java,
        WeeklyCalendarWidgetProvider::class.java,
        TrendingWidgetProvider::class.java,
        WatchProgressWidgetProvider::class.java,
    )

    /** Refreshes every widget on the home screen. */
    fun all(context: Context) = allProviders.forEach { refresh(context, it) }

    /** Refreshes one widget type, for callers that only touched data it reads. */
    fun refresh(context: Context, provider: Class<out AppWidgetProvider>) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(context, provider).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
        )
    }
}
