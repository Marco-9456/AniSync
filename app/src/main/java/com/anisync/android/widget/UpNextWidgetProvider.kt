package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.widget.RemoteViews
import androidx.core.util.SizeFCompat
import com.anisync.android.R
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.widget.core.AniSyncWidgetProvider
import com.anisync.android.widget.core.WidgetImageBudget
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntents
import com.anisync.android.widget.core.WidgetRows
import com.anisync.android.widget.core.WidgetSizes
import com.anisync.android.widget.core.WidgetTime
import com.anisync.android.widget.core.activeOwnerId
import com.anisync.android.widget.core.widgetDeps

/**
 * Up Next: the next episodes from the account's watching list.
 *
 * No state of its own, so a tap only ever opens media details. The interesting part is the row
 * count, which comes from the size the launcher asked for rather than a constant.
 */
class UpNextWidgetProvider : AniSyncWidgetProvider<UpNextWidgetProvider.Snapshot>() {

    data class Snapshot(val episodes: List<AiringScheduleEntity>, val nowSeconds: Long)

    override val declaredSizes = WidgetSizes.ladder(minHeightDp = 100)

    override suspend fun snapshot(context: Context, appWidgetId: Int): Snapshot {
        val deps = context.widgetDeps()
        val now = System.currentTimeMillis() / 1000
        val episodes = deps.airingScheduleDao().getAiringBetweenForUser(
            ownerId = deps.activeOwnerId(),
            startTime = now,
            endTime = now + LOOKAHEAD_DAYS * WidgetTime.DAY
        )
        return Snapshot(episodes.take(MAX_ROWS), now)
    }

    override fun coverRequests(context: Context, snapshot: Snapshot) =
        snapshot.episodes.map { WidgetImageLoader.CoverRequest(it.id, it.coverUrl) }

    override fun coverSize(context: Context, snapshot: Snapshot): Size =
        WidgetImageBudget.posterSize(
            context = context,
            displayWidthDp = COVER_WIDTH_DP,
            imageCount = snapshot.episodes.size
        )

    override fun build(
        context: Context,
        appWidgetId: Int,
        size: SizeFCompat,
        snapshot: Snapshot,
        covers: Map<Int, Bitmap?>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_up_next)
        val showHeader = !WidgetSizes.isShort(size)
        views.setViewVisibility(R.id.widget_header, if (showHeader) VISIBLE else GONE)
        views.setOnClickPendingIntent(
            android.R.id.background,
            WidgetIntents.openApp(context, appWidgetId)
        )

        if (snapshot.episodes.isEmpty()) {
            views.setViewVisibility(R.id.widget_list, GONE)
            views.setViewVisibility(R.id.widget_empty, VISIBLE)
            return views
        }

        views.setViewVisibility(R.id.widget_empty, GONE)
        views.setViewVisibility(R.id.widget_list, VISIBLE)
        views.removeAllViews(R.id.widget_list)

        val rows = WidgetRows.fit(
            size = size,
            rowHeightDp = ROW_HEIGHT_DP,
            chromeDp = LIST_PADDING_DP + if (showHeader) HEADER_HEIGHT_DP else 0,
            max = MAX_ROWS
        )
        snapshot.episodes.take(rows).forEach { episode ->
            views.addView(R.id.widget_list, row(context, appWidgetId, episode, snapshot, covers))
        }
        return views
    }

    private fun row(
        context: Context,
        appWidgetId: Int,
        episode: AiringScheduleEntity,
        snapshot: Snapshot,
        covers: Map<Int, Bitmap?>
    ): RemoteViews {
        val countdown = WidgetTime.countdown(context, episode.airingAt, snapshot.nowSeconds)
        val episodeLabel = context.getString(R.string.widget_episode_short, episode.episode)
        return RemoteViews(context.packageName, R.layout.widget_up_next_item).apply {
            setTextViewText(R.id.item_title, episode.titleUserPreferred)
            setTextViewText(R.id.item_episode, episodeLabel)
            setTextViewText(R.id.item_time, countdown)
            covers[episode.id]?.let { setImageViewBitmap(R.id.item_cover, it) }
            setContentDescription(
                R.id.item_root,
                context.getString(
                    R.string.a11y_widget_up_next_row,
                    episode.titleUserPreferred,
                    episode.episode,
                    countdown
                )
            )
            setOnClickPendingIntent(
                R.id.item_root,
                WidgetIntents.openMedia(context, appWidgetId, episode.mediaId)
            )
        }
    }

    private companion object {
        const val VISIBLE = android.view.View.VISIBLE
        const val GONE = android.view.View.GONE

        const val LOOKAHEAD_DAYS = 30L
        const val MAX_ROWS = 5
        const val COVER_WIDTH_DP = 44
        const val ROW_HEIGHT_DP = 90
        const val HEADER_HEIGHT_DP = 40
        const val LIST_PADDING_DP = 24
    }
}
