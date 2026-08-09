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
import com.anisync.android.widget.core.WidgetSizes
import com.anisync.android.widget.core.WidgetState
import com.anisync.android.widget.core.WidgetTheme
import com.anisync.android.widget.core.activeOwnerId
import com.anisync.android.widget.core.widgetDeps
import com.anisync.android.worker.LibrarySyncWorker

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

    data class Snapshot(val rows: List<Row>, val type: MediaType, val syncing: Boolean = false)

    override val declaredSizes = WidgetSizes.ladder(minHeightDp = 100)

    override val listViewId = R.id.widget_list
    override val listEmptyViewId = R.id.widget_empty

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

        // Nothing cached for this type usually means the app has never opened its tab, not that the
        // account has nothing in progress. Ask for a sync and say so, rather than claiming the list
        // is empty. Deduped per type, so rendering repeatedly does not restart the fetch.
        val syncing = rows.isEmpty()
        if (syncing) {
            runCatching { LibrarySyncWorker.enqueue(context, type) }
        }

        return Snapshot(rows, type, syncing)
    }

    override fun coverRequests(context: Context, snapshot: Snapshot) =
        snapshot.rows.map {
            WidgetImageLoader.CoverRequest(it.entry.mediaId, it.entry.coverUrl)
        }

    override fun coverSize(context: Context, snapshot: Snapshot): Size =
        WidgetImageBudget.posterSize(
            context = context,
            displayWidthDp = COVER_WIDTH_DP,
            imageCount = snapshot.rows.size,
            variantCount = declaredSizes.size
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
        WidgetTheme.applyChrome(views, colors)
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

        val isEmpty = snapshot.rows.isEmpty()
        views.setViewVisibility(R.id.widget_empty, if (isEmpty) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_list, if (isEmpty) View.GONE else View.VISIBLE)
        views.setTextViewText(
            R.id.widget_empty_body,
            context.getString(
                when {
                    snapshot.syncing && isManga -> R.string.widget_wp_syncing_manga
                    isManga -> R.string.widget_wp_empty_body_manga
                    else -> R.string.widget_wp_empty_body
                }
            )
        )

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
        val isManga = snapshot.type == MediaType.MANGA
        return snapshot.rows.map { row -> item(context, row, isManga, covers, colors) }
    }

    private fun item(
        context: Context,
        row: Row,
        isManga: Boolean,
        covers: Map<Int, Bitmap?>,
        colors: WidgetColors
    ): RemoteViews {
        val entry = row.entry

        // Episodes released so far. For a series with no announced length this is the only end the
        // bar can measure against, and it is arguably the more useful one anyway: what it shows is
        // whether the account is caught up, not how far through an unknown whole it is.
        val aired = entry.nextAiringEpisode?.minus(1)?.takeIf { it > 0 }
        val scale = row.total ?: aired
        val againstAired = row.total == null && aired != null

        val progressFraction = scale
            ?.takeIf { it > 0 }
            ?.let { (entry.progress.toFloat() / it).coerceIn(0f, 1f) }
            ?: 0f

        // The marker sits where the latest aired episode falls. Pointless when the bar already ends
        // there, which is exactly the unknown-total case.
        val airedFraction = if (!isManga && !againstAired) {
            val total = row.total
            if (aired != null && total != null && total > 0 && aired > entry.progress) {
                (aired.toFloat() / total).coerceIn(0f, 1f)
            } else {
                null
            }
        } else {
            null
        }

        val behind = aired?.minus(entry.progress)?.coerceAtLeast(0)
        val remainingLabel = when {
            row.remaining != null -> context.getString(
                if (isManga) R.string.widget_wp_left_manga else R.string.widget_wp_left,
                row.remaining
            )
            // No end to count down to, so count against what is out instead.
            behind != null && behind > 0 -> context.getString(R.string.widget_wp_behind, behind)
            behind != null -> context.getString(R.string.widget_wp_caught_up)
            else -> context.getString(R.string.widget_wp_ongoing)
        }

        val meta = when {
            row.total != null ->
                context.getString(R.string.widget_wp_progress, entry.progress, row.total)
            againstAired ->
                context.getString(R.string.widget_wp_progress_aired, entry.progress, aired)
            else -> context.getString(
                if (isManga) R.string.widget_wp_read else R.string.widget_wp_current_ep,
                entry.progress
            )
        }

        return RemoteViews(context.packageName, R.layout.widget_item_progress).apply {
            WidgetTheme.card(this, R.id.item_root, colors)
            WidgetTheme.poster(this, R.id.item_cover, colors)
            WidgetTheme.text(this, R.id.item_title, colors.onSurface, colors)
            WidgetTheme.text(this, R.id.item_meta, colors.onSurfaceVariant, colors)
            WidgetTheme.badge(this, R.id.item_remaining, colors.primaryContainer, colors.onPrimaryContainer, colors)
            WidgetTheme.badge(this, R.id.item_ongoing, colors.surfaceVariant, colors.onSurfaceVariant, colors)
            setTextViewText(R.id.item_title, entry.titleUserPreferred)
            setTextViewText(R.id.item_remaining, remainingLabel)
            setTextViewText(R.id.item_meta, meta)

            if (scale == null) {
                // Nothing to measure against at all: no announced total and no broadcast schedule,
                // which in practice means manga. A bar here could only ever read empty, which looks
                // broken rather than unknown, so the row shows the count instead.
                setViewVisibility(R.id.item_bar, View.GONE)
                setViewVisibility(R.id.item_ongoing, View.VISIBLE)
                setTextViewText(
                    R.id.item_ongoing,
                    context.getString(
                        if (isManga) R.string.widget_wp_ongoing_manga
                        else R.string.widget_wp_ongoing_anime,
                        entry.progress
                    )
                )
            } else {
                setViewVisibility(R.id.item_bar, View.VISIBLE)
                setViewVisibility(R.id.item_ongoing, View.GONE)
                setImageViewBitmap(
                    R.id.item_bar,
                    WidgetProgressRenderer.bar(
                        context = context,
                        // Rasterised at a fixed width and stretched by scaleType fitXY, because a
                        // row in a collection does not know how wide the widget is. Rendered wide
                        // so the common case shrinks it, which hides the cap distortion that
                        // stretching a narrow bitmap would show.
                        widthDp = BAR_RENDER_WIDTH_DP,
                        heightDp = BAR_HEIGHT_DP,
                        progress = progressFraction,
                        trackColor = colors.surfaceVariant,
                        fillColor = colors.primary,
                        airedFraction = airedFraction,
                        // The same hue as the fill, faded. A distinct colour needs explaining;
                        // a lighter shade of "watched" reads as "watchable" on its own.
                        airedColor = WidgetProgressRenderer.withAlpha(colors.primary, AIRED_ALPHA)
                    )
                )
            }

            covers[entry.mediaId]?.let { setImageViewBitmap(R.id.item_cover, it) }
            setContentDescription(R.id.item_root, describe(context, row, isManga))
            setOnClickFillInIntent(R.id.item_root, WidgetIntents.openMediaFillIn(entry.mediaId))
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

        const val COVER_WIDTH_DP = 52
        const val BAR_HEIGHT_DP = 6

        /** How visible the aired-but-unwatched band is against the track. */
        const val AIRED_ALPHA = 110

        /** Wide enough that stretching never softens the rounded ends. */
        const val BAR_RENDER_WIDTH_DP = 480
    }
}
