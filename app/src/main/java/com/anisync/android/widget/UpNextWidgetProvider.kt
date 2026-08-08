package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.View
import android.widget.RemoteViews
import androidx.core.util.SizeFCompat
import com.anisync.android.R
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.widget.core.AniSyncWidgetProvider
import com.anisync.android.widget.core.WidgetColors
import com.anisync.android.widget.core.WidgetImageBudget
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntents
import com.anisync.android.widget.core.WidgetMediaRow
import com.anisync.android.widget.core.WidgetSizes
import com.anisync.android.widget.core.WidgetTheme
import com.anisync.android.widget.core.WidgetTime
import com.anisync.android.widget.core.activeOwnerId
import com.anisync.android.widget.core.widgetDeps

/**
 * Up Next: the next episodes from the watching list.
 *
 * No state of its own, so a tap here only ever opens media details.
 */
class UpNextWidgetProvider : AniSyncWidgetProvider<UpNextWidgetProvider.Snapshot>() {

    data class Snapshot(val episodes: List<AiringScheduleEntity>, val nowSeconds: Long)

    override val declaredSizes = WidgetSizes.ladder(minHeightDp = 100)

    override val listViewId = R.id.widget_list
    override val listEmptyViewId = R.id.widget_empty

    override suspend fun snapshot(context: Context, appWidgetId: Int): Snapshot {
        val deps = context.widgetDeps()
        val now = System.currentTimeMillis() / 1000
        val episodes = deps.airingScheduleDao().getAiringBetweenForUser(
            ownerId = deps.activeOwnerId(),
            startTime = now,
            endTime = now + LOOKAHEAD_DAYS * WidgetTime.DAY
        )
        return Snapshot(episodes, now)
    }

    override fun coverRequests(context: Context, snapshot: Snapshot) =
        snapshot.episodes.map { WidgetImageLoader.CoverRequest(it.id, it.coverUrl) }

    override fun coverSize(context: Context, snapshot: Snapshot): Size =
        WidgetImageBudget.posterSize(
            context = context,
            displayWidthDp = COVER_WIDTH_DP,
            imageCount = snapshot.episodes.size,
            variantCount = declaredSizes.size
        )

    override fun build(
        context: Context,
        appWidgetId: Int,
        size: SizeFCompat,
        snapshot: Snapshot,
        covers: Map<Int, Bitmap?>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_up_next)
        val colors = WidgetColors.of(context)
        WidgetTheme.applyChrome(views, colors)
        views.setViewVisibility(
            R.id.widget_header,
            if (WidgetSizes.isShort(size)) View.GONE else View.VISIBLE
        )

        val isEmpty = snapshot.episodes.isEmpty()
        views.setViewVisibility(R.id.widget_empty, if (isEmpty) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_list, if (isEmpty) View.GONE else View.VISIBLE)

        views.setPendingIntentTemplate(
            R.id.widget_list,
            WidgetIntents.mediaTemplate(context, appWidgetId)
        )
        return views
    }

    override fun items(
        context: Context,
        appWidgetId: Int,
        snapshot: Snapshot,
        covers: Map<Int, Bitmap?>
    ): List<RemoteViews> = snapshot.episodes.map { episode ->
        val countdown = WidgetTime.countdown(context, episode.airingAt, snapshot.nowSeconds)
        WidgetMediaRow.build(
            context = context,
            title = episode.titleUserPreferred,
            subtitle = context.getString(R.string.widget_episode_long, episode.episode),
            trailing = countdown,
            mediaId = episode.mediaId,
            cover = covers[episode.id],
            colors = WidgetColors.of(context),
            contentDescription = context.getString(
                R.string.a11y_widget_up_next_row,
                episode.titleUserPreferred,
                episode.episode,
                countdown
            )
        )
    }

    private companion object {
        const val LOOKAHEAD_DAYS = 30L
        const val COVER_WIDTH_DP = 52
    }
}
