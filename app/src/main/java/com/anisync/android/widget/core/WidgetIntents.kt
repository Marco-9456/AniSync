package com.anisync.android.widget.core

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.anisync.android.MainActivity

/**
 * Every `PendingIntent` a widget hands to the launcher.
 *
 * Two kinds, and the split is the whole reliability story:
 *
 *  - **Navigation** goes straight to [MainActivity] with `getActivity`. The launcher starts the
 *    activity itself, so there is no trampoline and no background-start restriction.
 *  - **State changes** go to the widget's own provider with `getBroadcast`. The provider handles
 *    them in `onReceive` with `goAsync`, writes the new state and pushes fresh `RemoteViews`
 *    directly through `AppWidgetManager`. No scheduler sits between the tap and the pixels.
 *
 * Each `PendingIntent` carries a distinct `data` Uri. `PendingIntent` identity ignores extras, so
 * without it the "All" and "My List" taps would collapse into one intent and the second registered
 * would win for both.
 */
object WidgetIntents {

    const val ACTION_SET_STATE = "com.anisync.android.widget.action.SET_STATE"
    const val ACTION_REFRESH = "com.anisync.android.widget.action.REFRESH"

    const val EXTRA_NAME = "com.anisync.android.widget.extra.NAME"
    const val EXTRA_VALUE = "com.anisync.android.widget.extra.VALUE"

    private const val SCHEME = "anisync-widget"

    private val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /**
     * Writes [name] = [value] for this widget instance and re-renders it.
     *
     * [value] is encoded as a string and decoded by [WidgetState.apply]: "true"/"false" become a
     * boolean, digits become an int, anything else stays a string.
     */
    fun setState(
        context: Context,
        provider: Class<*>,
        appWidgetId: Int,
        name: String,
        value: String
    ): PendingIntent {
        val intent = Intent(context, provider).apply {
            action = ACTION_SET_STATE
            component = ComponentName(context, provider)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_VALUE, value)
            data = uri("state/$appWidgetId/$name/$value")
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /** Re-reads Room and re-renders, without changing any state. */
    fun refresh(context: Context, provider: Class<*>, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, provider).apply {
            action = ACTION_REFRESH
            component = ComponentName(context, provider)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = uri("refresh/$appWidgetId")
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /**
     * Opens a media details screen.
     *
     * `sourceScreen=widget` matches nothing in the app, which is what we want: a cold launch from
     * the home screen has no shared element to morph out of.
     */
    fun openMedia(context: Context, appWidgetId: Int, mediaId: Int): PendingIntent =
        openDeepLink(
            context,
            appWidgetId,
            "anisync://details/$mediaId?sourceScreen=widget",
            "details/$mediaId"
        )

    /** Opens the Discover tab. */
    fun openDiscover(context: Context, appWidgetId: Int): PendingIntent =
        openDeepLink(context, appWidgetId, "anisync://discover", "discover")

    /** Opens the app on its start destination. */
    fun openApp(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = uri("open/$appWidgetId")
        }
        return PendingIntent.getActivity(context, 0, intent, WidgetIntents.flags)
    }

    private fun openDeepLink(
        context: Context,
        appWidgetId: Int,
        deepLink: String,
        tag: String
    ): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply {
            component = ComponentName(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Distinguishes this PendingIntent from its siblings without touching the deep link.
            putExtra(WIDGET_TAG, "$appWidgetId/$tag")
        }
        return PendingIntent.getActivity(context, tagRequestCode(appWidgetId, tag), intent, flags)
    }

    private fun uri(path: String): Uri = "$SCHEME://$path".toUri()

    /**
     * `ACTION_VIEW` intents already carry their target in `data`, which is what distinguishes them,
     * except that two widgets showing the same series would produce the same intent. The request
     * code separates them.
     */
    private fun tagRequestCode(appWidgetId: Int, tag: String): Int =
        (appWidgetId * 31 + tag.hashCode()) and 0x7FFFFFFF

    private const val WIDGET_TAG = "com.anisync.android.widget.extra.TAG"
}
