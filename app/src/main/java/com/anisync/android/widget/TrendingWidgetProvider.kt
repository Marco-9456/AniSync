package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.widget.RemoteViews
import androidx.core.util.SizeFCompat
import com.anisync.android.R
import com.anisync.android.data.local.entity.TrendingEntity
import com.anisync.android.widget.core.AniSyncWidgetProvider
import com.anisync.android.widget.core.WidgetImageBudget
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntents
import com.anisync.android.widget.core.WidgetRows
import com.anisync.android.widget.core.WidgetSizes
import com.anisync.android.widget.core.widgetDeps

/**
 * Trending: the top ten trending anime.
 *
 * Not account scoped, because trending is the same list for everyone. The search affordance opens
 * Discover, which is the one piece of this widget that used to be an empty TODO.
 */
class TrendingWidgetProvider : AniSyncWidgetProvider<TrendingWidgetProvider.Snapshot>() {

    data class Snapshot(val media: List<TrendingEntity>)

    override val declaredSizes = WidgetSizes.ladder(minHeightDp = 100)

    override suspend fun snapshot(context: Context, appWidgetId: Int) =
        Snapshot(context.widgetDeps().trendingDao().getTopTrending(MAX_ROWS))

    override fun coverRequests(context: Context, snapshot: Snapshot) =
        snapshot.media.map { WidgetImageLoader.CoverRequest(it.id, it.coverUrl) }

    override fun coverSize(context: Context, snapshot: Snapshot): Size =
        WidgetImageBudget.posterSize(
            context = context,
            displayWidthDp = COVER_WIDTH_DP,
            imageCount = snapshot.media.size
        )

    override fun build(
        context: Context,
        appWidgetId: Int,
        size: SizeFCompat,
        snapshot: Snapshot,
        covers: Map<Int, Bitmap?>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_trending)
        views.setOnClickPendingIntent(
            R.id.widget_search,
            WidgetIntents.openDiscover(context, appWidgetId)
        )
        views.setContentDescription(
            R.id.widget_search,
            context.getString(R.string.a11y_widget_open_discover)
        )

        if (snapshot.media.isEmpty()) {
            views.setViewVisibility(R.id.widget_list, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
            return views
        }

        views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)
        views.setViewVisibility(R.id.widget_list, android.view.View.VISIBLE)
        views.removeAllViews(R.id.widget_list)

        val rows = WidgetRows.fit(
            size = size,
            rowHeightDp = ROW_HEIGHT_DP,
            chromeDp = CHROME_DP,
            max = MAX_ROWS
        )
        snapshot.media.take(rows).forEach { media ->
            views.addView(R.id.widget_list, row(context, appWidgetId, media, covers))
        }
        return views
    }

    private fun row(
        context: Context,
        appWidgetId: Int,
        media: TrendingEntity,
        covers: Map<Int, Bitmap?>
    ): RemoteViews {
        val rankLabel = context.getString(R.string.widget_trending_rank, media.rank)
        val score = media.averageScore
        return RemoteViews(context.packageName, R.layout.widget_trending_item).apply {
            setTextViewText(R.id.item_title, media.titleUserPreferred)
            setTextViewText(R.id.item_rank, rankLabel)
            if (score != null) {
                setViewVisibility(R.id.item_score, android.view.View.VISIBLE)
                setTextViewText(R.id.item_score, context.getString(R.string.widget_trending_score, score))
            } else {
                setViewVisibility(R.id.item_score, android.view.View.GONE)
            }
            covers[media.id]?.let { setImageViewBitmap(R.id.item_cover, it) }
            setContentDescription(
                R.id.item_root,
                context.getString(
                    R.string.a11y_widget_trending_row,
                    media.rank,
                    media.titleUserPreferred
                )
            )
            setOnClickPendingIntent(
                R.id.item_root,
                WidgetIntents.openMedia(context, appWidgetId, media.id)
            )
        }
    }

    private companion object {
        const val MAX_ROWS = 10
        const val COVER_WIDTH_DP = 44
        const val ROW_HEIGHT_DP = 90
        const val CHROME_DP = 64
    }
}
