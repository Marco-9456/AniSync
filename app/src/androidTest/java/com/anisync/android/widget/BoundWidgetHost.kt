package com.anisync.android.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.anisync.android.widget.core.WidgetState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * An AppWidgetHost that binds real widget instances and records what they publish.
 *
 * Shared by the interaction and screenshot tests, both want the same thing: a widget the system
 * treats as real, and a handle on the RemoteViews it gets. Binding needs permission:
 *
 *     adb shell appwidget grantbind --package com.anisync.android.debug
 *
 * [bind] returns null without that grant, so callers can skip instead of failing.
 */
class BoundWidgetHost(private val context: Context) : AppWidgetHost(context, HOST_ID) {

    private val manager = AppWidgetManager.getInstance(context)
    private val latches = mutableMapOf<Int, CountDownLatch>()
    private val published = mutableMapOf<Int, AtomicReference<RemoteViews?>>()
    private val bound = mutableListOf<Int>()

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = object : AppWidgetHostView(context) {
        override fun updateAppWidget(remoteViews: RemoteViews?) {
            // Not calling super on purpose. Inflating here is not what the interaction tests
            // measure and a failure would muddy the timings. The screenshot test inflates itself.
            published.getOrPut(appWidgetId) { AtomicReference() }.set(remoteViews)
            latches[appWidgetId]?.countDown()
        }
    }

    fun bind(provider: Class<*>): Int? {
        val id = allocateAppWidgetId()
        if (!manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, provider))) {
            deleteAppWidgetId(id)
            return null
        }
        bound += id
        createView(context, id, manager.getAppWidgetInfo(id))
        // Replays the last views for every listened id, not only this one.
        startListening()
        bound.forEach { drain(it) }
        return id
    }

    fun lastPublished(appWidgetId: Int): RemoteViews? = published[appWidgetId]?.get()

    fun arm(appWidgetId: Int) {
        latches[appWidgetId] = CountDownLatch(1)
    }

    fun await(appWidgetId: Int, timeoutMs: Long): Boolean =
        latches[appWidgetId]?.await(timeoutMs, TimeUnit.MILLISECONDS) ?: false

    /** Runs [action] and waits for the next push for [appWidgetId]. Returns elapsed ms. */
    fun fireAndAwait(appWidgetId: Int, timeoutMs: Long, action: () -> Unit): Long? {
        arm(appWidgetId)
        val start = System.nanoTime()
        action()
        val arrived = await(appWidgetId, timeoutMs)
        val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
        return if (arrived) elapsed else null
    }

    /** Resizes the instance, the same as dragging it on the home screen. */
    fun resize(appWidgetId: Int, widthDp: Int, heightDp: Int) {
        manager.updateAppWidgetOptions(
            appWidgetId,
            Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            }
        )
    }

    fun broadcastUpdate(provider: Class<*>, appWidgetId: Int) {
        context.sendBroadcast(
            Intent(context, provider).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
        )
    }

    fun release() {
        runCatching { stopListening() }
        bound.forEach { id ->
            WidgetState.clear(context, id)
            runCatching { deleteAppWidgetId(id) }
        }
        bound.clear()
    }

    /** Waits until nothing else has arrived for [QUIET_MS]. */
    private fun drain(appWidgetId: Int) {
        val deadline = System.currentTimeMillis() + DRAIN_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            arm(appWidgetId)
            if (!await(appWidgetId, QUIET_MS)) return
        }
    }

    companion object {
        const val HOST_ID = 0x4E53
        const val QUIET_MS = 400L
        const val DRAIN_TIMEOUT_MS = 5_000L
    }
}
