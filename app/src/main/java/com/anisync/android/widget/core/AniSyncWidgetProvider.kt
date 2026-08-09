package com.anisync.android.widget.core

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.RemoteViews
import androidx.core.util.SizeFCompat
import androidx.core.widget.updateAppWidget
import com.anisync.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Base class for the home screen widgets.
 *
 * A tap arrives here as a broadcast. goAsync keeps it alive, we write the new state, read Room and
 * push the RemoteViews. One pass, same process, no WorkManager in between, so a tap is as fast
 * after an hour of idle as it is right after the last one.
 *
 * snapshot() is read once per render, after the state write, so the selection and the content can
 * never disagree.
 *
 * @param S the widget's snapshot: its state plus whatever it read out of Room.
 */
abstract class AniSyncWidgetProvider<S : Any> : AppWidgetProvider() {

    /**
     * Sizes this widget declares.
     *
     * On API 31+ the launcher gets one RemoteViews per entry and switches between them itself, so
     * a resize never calls back into us. Below that only the matching one is built.
     */
    internal abstract val declaredSizes: List<SizeFCompat>

    /** Reads this instance's state and its Room rows. No image loading, no rendering. */
    internal abstract suspend fun snapshot(context: Context, appWidgetId: Int): S

    /** Covers this snapshot needs, or empty when the widget shows none. */
    internal abstract fun coverRequests(
        context: Context,
        snapshot: S
    ): List<WidgetImageLoader.CoverRequest>

    /** Decode size for those covers. Comes from [WidgetImageBudget], never a hardcoded number. */
    internal abstract fun coverSize(context: Context, snapshot: S): Size

    /**
     * Chrome for one declared size: header, controls, empty state. Called once per size per render.
     * Rows come from [items] and are attached separately.
     */
    internal abstract fun build(
        context: Context,
        appWidgetId: Int,
        size: SizeFCompat,
        snapshot: S,
        covers: Map<Int, Bitmap?>
    ): RemoteViews

    /**
     * The scrolling rows, or empty if the widget has no list.
     *
     * No size parameter on purpose: a row looks the same at every widget size, so we build the rows
     * once and attach the same set to every variant.
     */
    internal open fun items(
        context: Context,
        appWidgetId: Int,
        snapshot: S,
        covers: Map<Int, Bitmap?>
    ): List<RemoteViews> = emptyList()

    /** The ListView [items] feeds, or 0 if the widget has no list. */
    internal open val listViewId: Int get() = 0

    /** Shown by the list when it has no rows. Ignored if [listViewId] is 0. */
    internal open val listEmptyViewId: Int get() = 0

    /**
     * Rebuilds the rows from scratch for [WidgetCollectionService] on API 26 to 30.
     *
     * Takes a fresh snapshot instead of reusing the render's. The host can bind to the service long
     * after that render, often in a newly started process.
     */
    internal suspend fun loadItems(context: Context, appWidgetId: Int): List<RemoteViews> {
        val snapshot = snapshot(context, appWidgetId)
        val requests = coverRequests(context, snapshot)
        val covers = if (requests.isEmpty()) {
            emptyMap()
        } else {
            WidgetImageLoader.loadCovers(context, requests, coverSize(context, snapshot))
        }
        return items(context, appWidgetId, snapshot, covers)
    }

    final override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = goAsync {
        appWidgetIds.forEach { render(context, it) }
    }

    final override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) = goAsync {
        // Below API 31 we only sent one layout, so a resize means rebuilding the right one.
        render(context, appWidgetId)
    }

    final override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetState.clear(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WidgetIntents.ACTION_SET_STATE -> {
                val appWidgetId = intent.widgetId() ?: return
                val name = intent.getStringExtra(WidgetIntents.EXTRA_NAME) ?: return
                val value = intent.getStringExtra(WidgetIntents.EXTRA_VALUE) ?: return
                goAsync {
                    // Write first, then render, so the snapshot cannot pick up the old value.
                    WidgetState.apply(context, appWidgetId, name, value)
                    render(context, appWidgetId)
                }
            }

            WidgetIntents.ACTION_REFRESH -> {
                val appWidgetId = intent.widgetId() ?: return
                goAsync { render(context, appWidgetId) }
            }

            else -> super.onReceive(context, intent)
        }
    }

    /**
     * One render pass.
     *
     * Text never waits on images. Covers get [FAST_COVER_TIMEOUT_MS]; a cache hit usually beats it
     * and we publish once. If not, text goes out straight away and the images follow in a second
     * update. Both publishes use the same [snapshot], so the slow path can show a missing cover but
     * never stale content.
     */
    internal suspend fun render(context: Context, appWidgetId: Int) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        // Widget removed while its broadcast was in flight. The responsive helper throws on that
        // instead of doing nothing.
        if (manager.getAppWidgetInfo(appWidgetId) == null) return
        val snapshot = try {
            snapshot(context, appWidgetId)
        } catch (t: Throwable) {
            Log.e(TAG, "Snapshot failed for ${javaClass.simpleName}/$appWidgetId", t)
            publishError(context, manager, appWidgetId)
            return
        }

        val requests = coverRequests(context, snapshot)
        if (requests.isEmpty()) {
            publish(context, manager, appWidgetId, snapshot, emptyMap())
            return
        }

        val size = coverSize(context, snapshot)
        val fast = withTimeoutOrNull(FAST_COVER_TIMEOUT_MS) {
            WidgetImageLoader.loadCovers(context, requests, size)
        }
        if (fast != null) {
            publish(context, manager, appWidgetId, snapshot, fast)
            return
        }

        publish(context, manager, appWidgetId, snapshot, emptyMap())
        // Bounded because this runs inside the broadcast, and broadcasts to one receiver are
        // serialised. An unbounded wait here would hold up the next tap too. Under Doze there is no
        // network at all, so every request would sit until Coil times out. A missing cover is fixed
        // by the next update, a tap that feels stuck is not.
        val loaded = withTimeoutOrNull(COVER_LOAD_BUDGET_MS) {
            WidgetImageLoader.loadCovers(context, requests, size)
        } ?: return
        publish(context, manager, appWidgetId, snapshot, loaded)
    }

    private fun publish(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        snapshot: S,
        covers: Map<Int, Bitmap?>
    ) {
        try {
            val rows = if (listViewId != 0) {
                items(context, appWidgetId, snapshot, covers)
            } else {
                emptyList()
            }
            manager.updateAppWidget(appWidgetId, declaredSizes) { size ->
                build(context, appWidgetId, size, snapshot, covers).also { views ->
                    if (listViewId != 0) {
                        WidgetCollection.attach(
                            views = views,
                            context = context,
                            appWidgetId = appWidgetId,
                            listViewId = listViewId,
                            provider = javaClass,
                            items = rows,
                            emptyViewId = listEmptyViewId
                        )
                    }
                }
            }
            if (listViewId != 0) {
                WidgetCollection.notifyChanged(context, appWidgetId, listViewId)
            }
        } catch (t: IllegalArgumentException) {
            // Removed between the check above and this call. Nothing to draw on, nothing broken,
            // so no error card and no stack trace.
            Log.d(TAG, "Skipped ${javaClass.simpleName}/$appWidgetId, no longer bound")
        } catch (t: Throwable) {
            Log.e(TAG, "Update rejected for ${javaClass.simpleName}/$appWidgetId", t)
            publishError(context, manager, appWidgetId)
        }
    }

    private fun publishError(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        runCatching {
            val views = RemoteViews(context.packageName, R.layout.widget_error).apply {
                WidgetTheme.panel(this, R.id.widget_error_root, WidgetColors.of(context))
                setOnClickPendingIntent(
                    R.id.widget_error_root,
                    WidgetIntents.refresh(context, this@AniSyncWidgetProvider.javaClass, appWidgetId)
                )
            }
            manager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun Intent.widgetId(): Int? =
        getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            .takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }

    private companion object {
        const val TAG = "AniSyncWidget"

        /** How long we wait for covers before painting the text without them. */
        const val FAST_COVER_TIMEOUT_MS = 120L

        /** Ceiling on the whole cover pass so one render cannot block the next tap. */
        const val COVER_LOAD_BUDGET_MS = 2_500L
    }
}

/**
 * Runs [block] while holding the broadcast open.
 *
 * Glance had its own version of this and kept it internal. finish() goes in a finally so a render
 * that throws cannot leak the receiver, and we swallow IllegalStateException from it because some
 * OEM builds claim the broadcast is already done.
 */
private fun BroadcastReceiver.goAsync(block: suspend () -> Unit) {
    val pendingResult = goAsync()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch {
        try {
            try {
                // coroutineScope so a failing child lands in the catch below instead of the
                // CoroutineExceptionHandler, which would take the process down.
                coroutineScope { block() }
            } catch (t: Throwable) {
                Log.e("AniSyncWidget", "Widget broadcast failed", t)
            } finally {
                scope.cancel()
            }
        } finally {
            // Last thing we do. The process can be killed the moment this returns.
            runCatching { pendingResult.finish() }
        }
    }
}
