package com.anisync.android.widget.core

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.anisync.android.R
import kotlinx.coroutines.runBlocking

/**
 * Serves widget rows on API 26 to 30, where RemoteCollectionItems does not exist.
 *
 * The host binds here and pulls rows one at a time. Nothing above API 30 uses it, there the rows
 * ride inside the RemoteViews and this is never bound.
 *
 * The factory reads Room again rather than picking up a cache the provider left behind. A cache
 * looks faster and is wrong: the host can bind after our process was killed and restarted, when any
 * in-memory handoff is gone and the list would come back empty.
 */
class WidgetCollectionService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val providerName = intent.getStringExtra(WidgetCollection.EXTRA_PROVIDER)
        return Factory(appWidgetId, providerName)
    }

    private inner class Factory(
        private val appWidgetId: Int,
        private val providerName: String?
    ) : RemoteViewsFactory {

        private var rows: List<RemoteViews> = emptyList()

        override fun onCreate() = Unit

        override fun onDataSetChanged() {
            // Binder thread, and the host is blocking on it anyway, so this is allowed to be slow.
            // runBlocking is right here and nowhere else in this package.
            rows = runCatching {
                val provider = providerName
                    ?.let { Class.forName(it).getDeclaredConstructor().newInstance() }
                    as? AniSyncWidgetProvider<*>
                    ?: return@runCatching emptyList()
                runBlocking { provider.loadItems(applicationContext, appWidgetId) }
            }.getOrDefault(emptyList())
        }

        override fun onDestroy() {
            rows = emptyList()
        }

        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews =
            rows.getOrNull(position) ?: RemoteViews(packageName, R.layout.widget_row_placeholder)

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = true
    }
}
