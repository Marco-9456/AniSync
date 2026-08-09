package com.anisync.android.widget.core

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews

/**
 * Attaches a scrolling list to a widget.
 *
 * ScrollView and HorizontalScrollView are out. Neither is annotated @RemoteView in the framework,
 * so RemoteViews.apply throws InflateException at inflate time, with no compile error and no lint
 * warning first. The only scrolling containers allowed are the adapter ones: ListView, GridView,
 * StackView, AdapterViewFlipper.
 *
 * Two ways to fill one:
 *
 *  - API 31+ takes [RemoteViews.RemoteCollectionItems], which rides inside the RemoteViews. No
 *    service, no binding, no second round trip, the rows land in the same update as the rest.
 *  - API 26 to 30 has to go through a RemoteViewsService. The host binds to
 *    [WidgetCollectionService] and it recomputes the rows. That is the extra round trip, hence the
 *    fallback.
 *
 * Row taps use a template plus a fill-in intent, so a tap is still a plain PendingIntent.
 */
object WidgetCollection {

    fun attach(
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        listViewId: Int,
        provider: Class<*>,
        items: List<RemoteViews>,
        emptyViewId: Int
    ) {
        views.setEmptyView(listViewId, emptyViewId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val builder = RemoteViews.RemoteCollectionItems.Builder()
                .setHasStableIds(true)
                // All rows use the same layout. Telling the host lets it recycle views instead of
                // re-inflating on every scroll.
                .setViewTypeCount(1)
            items.forEachIndexed { index, item -> builder.addItem(index.toLong(), item) }
            views.setRemoteAdapter(listViewId, builder.build())
        } else {
            views.setRemoteAdapter(listViewId, serviceIntent(context, appWidgetId, provider))
        }
    }

    /**
     * Tells the host to re-fetch a service backed list.
     *
     * Only does anything below API 31, where the rows sit behind a binder instead of in the
     * RemoteViews. Harmless above that, just pointless.
     */
    fun notifyChanged(context: Context, appWidgetId: Int, listViewId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        runCatching {
            AppWidgetManager.getInstance(context)
                ?.notifyAppWidgetViewDataChanged(appWidgetId, listViewId)
        }
    }

    /**
     * The intent that identifies one widget instance list.
     *
     * The data Uri is what makes it unique. RemoteViewsService keys factories by intent and intent
     * equality ignores extras, so without it every instance shares a factory and shows the same rows.
     */
    private fun serviceIntent(context: Context, appWidgetId: Int, provider: Class<*>): Intent =
        Intent(context, WidgetCollectionService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_PROVIDER, provider.name)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

    const val EXTRA_PROVIDER = "com.anisync.android.widget.extra.PROVIDER"
}
