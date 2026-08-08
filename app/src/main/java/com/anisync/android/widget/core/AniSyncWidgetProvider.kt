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
 * Base for every AniSync home-screen widget.
 *
 * The tap path is the reason this class exists. A tap arrives as a broadcast on this provider,
 * [goAsync] holds the broadcast open, the new state is committed synchronously, Room is read, and
 * the result goes straight to `AppWidgetManager.updateAppWidget`. Everything happens in this one
 * process, in one pass, with no job scheduler in between, so a tap applies at the same speed after
 * the device has been idle for an hour as it does a second after the last one.
 *
 * There is exactly one source of truth per render: [snapshot] is read once, after the state write,
 * and every [build] call for that render sees the same value. The screen cannot show a selection
 * that disagrees with its content.
 *
 * @param S the widget's own snapshot type: its state plus the rows it read out of Room.
 */
abstract class AniSyncWidgetProvider<S : Any> : AppWidgetProvider() {

    /**
     * Sizes this widget declares.
     *
     * From API 31 the launcher is handed one `RemoteViews` per entry and picks between them without
     * calling back into the app, so resizing never waits on us. Below 31 only the entry matching
     * the current size is built.
     */
    internal abstract val declaredSizes: List<SizeFCompat>

    /** Reads this instance's state and its Room rows. No image loading, no rendering. */
    internal abstract suspend fun snapshot(context: Context, appWidgetId: Int): S

    /** Covers this snapshot needs, or empty when the widget shows none. */
    internal abstract fun coverRequests(
        context: Context,
        snapshot: S
    ): List<WidgetImageLoader.CoverRequest>

    /** Decode size for those covers, from [WidgetImageBudget], never a constant. */
    internal abstract fun coverSize(context: Context, snapshot: S): Size

    /** Builds the views for one declared size. Called once per size, per render. */
    internal abstract fun build(
        context: Context,
        appWidgetId: Int,
        size: SizeFCompat,
        snapshot: S,
        covers: Map<Int, Bitmap?>
    ): RemoteViews

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
        // Below API 31 only one layout was sent, so a resize needs the matching one built.
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
                    // Committed before the snapshot is taken, so the render below cannot read the
                    // value the user just replaced.
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
     * Text never waits on images. Covers get [FAST_COVER_TIMEOUT_MS] to arrive, which the memory
     * and disk caches normally beat, and the widget is published once. When they do not, the text
     * is published immediately and a second update carries the images in. Both publishes render
     * from the same [snapshot], so the slow path shows missing covers, never stale content.
     */
    internal suspend fun render(context: Context, appWidgetId: Int) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        // A widget removed while its broadcast was in flight has no info, and the responsive
        // helper throws on that rather than no-opping.
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
        val loaded = WidgetImageLoader.loadCovers(context, requests, size)
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
            manager.updateAppWidget(appWidgetId, declaredSizes) { size ->
                build(context, appWidgetId, size, snapshot, covers)
            }
        } catch (t: Throwable) {
            // An oversized RemoteViews throws here rather than failing silently on the host side.
            Log.e(TAG, "Update rejected for ${javaClass.simpleName}/$appWidgetId", t)
            publishError(context, manager, appWidgetId)
        }
    }

    private fun publishError(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        runCatching {
            val views = RemoteViews(context.packageName, R.layout.widget_error).apply {
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

        /**
         * How long a render waits for covers before painting text without them.
         *
         * Long enough that a cache hit publishes once, short enough that a cold network fetch never
         * holds the first frame.
         */
        const val FAST_COVER_TIMEOUT_MS = 120L
    }
}

/**
 * Runs [block] while holding the broadcast open.
 *
 * The library equivalent is internal to Glance, and this is the piece worth keeping from it: the
 * broadcast is finished in a `finally` so a thrown render can never leak the receiver, and
 * `IllegalStateException` from `finish()` is swallowed because some OEM builds report the broadcast
 * as already finished.
 */
private fun BroadcastReceiver.goAsync(block: suspend () -> Unit) {
    val pendingResult = goAsync()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch {
        try {
            try {
                // coroutineScope so a failing child surfaces here instead of reaching the
                // CoroutineExceptionHandler and taking the process with it.
                coroutineScope { block() }
            } catch (t: Throwable) {
                Log.e("AniSyncWidget", "Widget broadcast failed", t)
            } finally {
                scope.cancel()
            }
        } finally {
            // Last call. The process may be killed the moment it returns.
            runCatching { pendingResult.finish() }
        }
    }
}
