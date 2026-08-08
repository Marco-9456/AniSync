package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.View
import android.widget.RemoteViews
import androidx.core.util.SizeFCompat
import com.anisync.android.R
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.type.MediaType
import com.anisync.android.widget.core.AniSyncWidgetProvider
import com.anisync.android.widget.core.WidgetChips
import com.anisync.android.widget.core.WidgetColors
import com.anisync.android.widget.core.WidgetImageBudget
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntents
import com.anisync.android.widget.core.WidgetProgressRenderer
import com.anisync.android.widget.core.WidgetRows
import com.anisync.android.widget.core.WidgetSizes
import com.anisync.android.widget.core.WidgetState
import com.anisync.android.widget.core.activeOwnerId
import com.anisync.android.widget.core.widgetDeps

/**
 * Watch Progress: the series closest to finishing, fewest episodes or chapters remaining first.
 *
 * Entries with no known total sort last. They are still worth showing, since an ongoing series the
 * account is mid-way through is exactly what someone opens this widget for, but "unknown remaining"
 * cannot be ranked against a real count.
 */
class WatchProgressWidgetProvider :
    AniSyncWidgetProvider<WatchProgressWidgetProvider.Snapshot>() {

    data class Row(
        val entry: LibraryEntryEntity,
        val total: Int?,
        val remaining: Int?
    )

    data class Snapshot(val rows: List<Row>, val type: MediaType)

    override val declaredSizes = WidgetSizes.ladder(minHeightDp = 100)

    override suspend fun snapshot(context: Context, appWidgetId: Int): Snapshot {
        val deps = context.widgetDeps()
        val type = if (WidgetState.getString(context, appWidgetId, STATE_TYPE, ANIME) == MANGA) {
            MediaType.MANGA
        } else {
            MediaType.ANIME
        }
        val entries = deps.libraryDao().getInProgress(deps.activeOwnerId(), type)

        val rows = entries
            .map { entry ->
                val total = if (type == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
                val remaining = total?.let { (it - entry.progress).coerceAtLeast(0) }
                Row(entry, total, remaining)
            }
            // Unknown totals last, then fewest remaining, then most recently touched.
            .sortedWith(
                compareBy<Row> { it.remaining == null }
                    .thenBy { it.remaining ?: Int.MAX_VALUE }
                    .thenByDescending { it.entry.lastUpdated }
            )
            .take(MAX_ROWS)

        return Snapshot(rows, type)
    }

    override fun coverRequests(context: Context, snapshot: Snapshot) =
        snapshot.rows.map {
            WidgetImageLoader.CoverRequest(it.entry.mediaId, it.entry.coverUrl)
        }

    override fun coverSize(context: Context, snapshot: Snapshot): Size =
        WidgetImageBudget.posterSize(
            context = context,
            displayWidthDp = COVER_WIDTH_DP,
            imageCount = snapshot.rows.size
        )

    override fun build(
        context: Context,
        appWidgetId: Int,
        size: SizeFCompat,
        snapshot: Snapshot,
        covers: Map<Int, Bitmap?>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_watch_progress)
        val colors = WidgetColors.of(context)
        val isManga = snapshot.type == MediaType.MANGA

        WidgetChips.apply(views, context, R.id.chip_anime, !isManga, colors)
        WidgetChips.apply(views, context, R.id.chip_manga, isManga, colors)
        views.setOnClickPendingIntent(
            R.id.chip_anime,
            WidgetIntents.setState(context, javaClass, appWidgetId, STATE_TYPE, ANIME)
        )
        views.setOnClickPendingIntent(
            R.id.chip_manga,
            WidgetIntents.setState(context, javaClass, appWidgetId, STATE_TYPE, MANGA)
        )
        views.setContentDescription(
            R.id.chip_anime,
            context.getString(R.string.a11y_widget_type_anime)
        )
        views.setContentDescription(
            R.id.chip_manga,
            context.getString(R.string.a11y_widget_type_manga)
        )

        views.setViewVisibility(
            R.id.widget_title,
            if (WidgetSizes.isNarrow(size)) View.GONE else View.VISIBLE
        )

        if (snapshot.rows.isEmpty()) {
            views.setViewVisibility(R.id.widget_list, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_empty_body,
                context.getString(
                    if (isManga) R.string.widget_wp_empty_body_manga
                    else R.string.widget_wp_empty_body
                )
            )
            return views
        }

        views.setViewVisibility(R.id.widget_empty, View.GONE)
        views.setViewVisibility(R.id.widget_list, View.VISIBLE)
        views.removeAllViews(R.id.widget_list)

        val rows = WidgetRows.fit(size, ROW_HEIGHT_DP, CHROME_DP, MAX_ROWS)
        // The bar spans the card minus the cover, its margin, and the card padding.
        val barWidthDp = (size.width - BAR_INSET_DP).toInt().coerceAtLeast(MIN_BAR_WIDTH_DP)
        snapshot.rows.take(rows).forEach { row ->
            views.addView(
                R.id.widget_list,
                row(context, appWidgetId, row, snapshot.type, covers, colors, barWidthDp)
            )
        }
        return views
    }

    private fun row(
        context: Context,
        appWidgetId: Int,
        row: Row,
        type: MediaType,
        covers: Map<Int, Bitmap?>,
        colors: WidgetColors,
        barWidthDp: Int
    ): RemoteViews {
        val isManga = type == MediaType.MANGA
        val entry = row.entry
        val progressFraction = row.total
            ?.takeIf { it > 0 }
            ?.let { (entry.progress.toFloat() / it).coerceIn(0f, 1f) }
            ?: 0f

        // The aired marker only means something for anime that is still broadcasting, and only when
        // the account is actually behind it.
        val airedFraction = if (!isManga) {
            val aired = entry.nextAiringEpisode?.minus(1)
            val total = row.total
            if (aired != null && total != null && total > 0 && aired > entry.progress) {
                (aired.toFloat() / total).coerceIn(0f, 1f)
            } else {
                null
            }
        } else {
            null
        }

        val remainingLabel = row.remaining?.let {
            context.getString(
                if (isManga) R.string.widget_wp_left_manga else R.string.widget_wp_left,
                it
            )
        } ?: context.getString(R.string.widget_wp_ongoing)

        val meta = row.total?.let {
            context.getString(R.string.widget_wp_progress, entry.progress, it)
        } ?: context.getString(
            if (isManga) R.string.widget_wp_read else R.string.widget_wp_current_ep,
            entry.progress
        )

        return RemoteViews(context.packageName, R.layout.widget_watch_progress_item).apply {
            setTextViewText(R.id.item_title, entry.titleUserPreferred)
            setTextViewText(R.id.item_remaining, remainingLabel)
            setTextViewText(R.id.item_meta, meta)
            setImageViewBitmap(
                R.id.item_bar,
                WidgetProgressRenderer.bar(
                    context = context,
                    widthDp = barWidthDp,
                    heightDp = BAR_HEIGHT_DP,
                    progress = progressFraction,
                    trackColor = colors.surface,
                    fillColor = colors.primary,
                    airedFraction = airedFraction,
                    dotColor = colors.tertiaryContainer
                )
            )
            covers[entry.mediaId]?.let { setImageViewBitmap(R.id.item_cover, it) }
            setContentDescription(R.id.item_root, describe(context, row, isManga))
            setOnClickPendingIntent(
                R.id.item_root,
                WidgetIntents.openMedia(context, appWidgetId, entry.mediaId)
            )
        }
    }

    private fun describe(context: Context, row: Row, isManga: Boolean): String {
        val entry = row.entry
        val total = row.total
        val remaining = row.remaining
        return if (total != null && remaining != null) {
            context.getString(
                if (isManga) R.string.a11y_wp_card_manga else R.string.a11y_wp_card_anime,
                entry.titleUserPreferred,
                entry.progress,
                total,
                remaining
            )
        } else {
            context.getString(
                if (isManga) R.string.a11y_wp_card_unknown_manga else R.string.a11y_wp_card_unknown,
                entry.titleUserPreferred,
                entry.progress
            )
        }
    }

    private companion object {
        const val STATE_TYPE = "media_type"
        const val ANIME = "ANIME"
        const val MANGA = "MANGA"

        const val MAX_ROWS = 8
        const val COVER_WIDTH_DP = 40
        const val ROW_HEIGHT_DP = 84
        const val CHROME_DP = 64
        const val BAR_HEIGHT_DP = 6

        /** Card padding, cover, and the gap between them, doubled for both sides of the widget. */
        const val BAR_INSET_DP = 94
        const val MIN_BAR_WIDTH_DP = 60
    }
}
