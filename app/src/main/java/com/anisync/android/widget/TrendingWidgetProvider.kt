package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.View
import android.widget.RemoteViews
import androidx.core.util.SizeFCompat
import com.anisync.android.R
import com.anisync.android.data.local.entity.TrendingEntity
import com.anisync.android.widget.core.AniSyncWidgetProvider
import com.anisync.android.widget.core.WidgetColors
import com.anisync.android.widget.core.WidgetImageBudget
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntents
import com.anisync.android.widget.core.WidgetSizes
import com.anisync.android.widget.core.WidgetTheme
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

    override val listViewId = R.id.widget_list
    override val listEmptyViewId = R.id.widget_empty

    override suspend fun snapshot(context: Context, appWidgetId: Int) =
        Snapshot(context.widgetDeps().trendingDao().getTopTrending(TOP_N))

    override fun coverRequests(context: Context, snapshot: Snapshot) =
        snapshot.media.map { WidgetImageLoader.CoverRequest(it.id, it.coverUrl) }

    override fun coverSize(context: Context, snapshot: Snapshot): Size =
        WidgetImageBudget.posterSize(
            context = context,
            displayWidthDp = COVER_WIDTH_DP,
            imageCount = snapshot.media.size,
            variantCount = declaredSizes.size
        )

    override fun build(
        context: Context,
        appWidgetId: Int,
        size: SizeFCompat,
        snapshot: Snapshot,
        covers: Map<Int, Bitmap?>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_trending)
        val colors = WidgetColors.of(context)
        WidgetTheme.applyChrome(views, colors)
        views.setOnClickPendingIntent(
            R.id.widget_search,
            WidgetIntents.openDiscover(context, appWidgetId)
        )
        views.setContentDescription(
            R.id.widget_search,
            context.getString(R.string.a11y_widget_open_discover)
        )
        views.setViewVisibility(
            R.id.widget_title,
            if (WidgetSizes.isNarrow(size)) View.GONE else View.VISIBLE
        )

        val isEmpty = snapshot.media.isEmpty()
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
    ): List<RemoteViews> {
        val colors = WidgetColors.of(context)
        return snapshot.media.map { media ->
            RemoteViews(context.packageName, R.layout.widget_item_trending).apply {
                WidgetTheme.card(this, R.id.item_root, colors)
                WidgetTheme.poster(this, R.id.item_cover, colors)
                WidgetTheme.text(this, R.id.item_title, colors.onSurface, colors)
                WidgetTheme.text(this, R.id.item_score, colors.onSurfaceVariant, colors)
                WidgetTheme.icon(this, R.id.item_star, colors.primary, colors)
                WidgetTheme.badge(this, R.id.item_rank, colors.primary, colors.onPrimary, colors)
                setTextViewText(R.id.item_title, media.titleUserPreferred)
                setTextViewText(
                    R.id.item_rank,
                    context.getString(R.string.widget_trending_rank, media.rank)
                )
                val score = media.averageScore
                // Score and star travel together. A lone star beside nothing reads as a broken row.
                val scoreVisibility = if (score != null) View.VISIBLE else View.GONE
                setViewVisibility(R.id.item_score, scoreVisibility)
                setViewVisibility(R.id.item_star, scoreVisibility)
                if (score != null) {
                    setTextViewText(
                        R.id.item_score,
                        context.getString(R.string.widget_trending_score, score)
                    )
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
                setOnClickFillInIntent(R.id.item_root, WidgetIntents.openMediaFillIn(media.id))
            }
        }
    }

    private companion object {
        const val TOP_N = 10
        const val COVER_WIDTH_DP = 52
    }
}
