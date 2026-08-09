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
import com.anisync.android.widget.core.WidgetSizes
import com.anisync.android.widget.core.WidgetState
import com.anisync.android.widget.core.WidgetTheme
import com.anisync.android.widget.core.WidgetTime
import com.anisync.android.widget.core.activeOwnerId
import com.anisync.android.widget.core.widgetDeps
import com.anisync.android.worker.LibrarySyncWorker

/**
 * Watch Progress: whatever is closest to finishing, fewest episodes or chapters left first.
 *
 * Entries with no known total go last. Still worth showing, an ongoing series you are halfway
 * through is exactly what this widget is for, but you cannot rank "unknown" against a real count.
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
            // Unknown totals last, then fewest left, then most recently touched.
            .sortedWith(
                compareBy<Row> { it.remaining == null }
                    .thenBy { it.remaining ?: Int.MAX_VALUE }
                    .thenByDescending { it.entry.lastUpdated }
            )

        // Nothing cached for this type usually means the tab was never opened, not that there is
        // nothing in progress. So ask for a sync and say so instead of claiming it is empty. Deduped
        // per type, so re-rendering does not restart the fetch.
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

        // Episodes out so far. For a series with no announced length this is the only thing the bar
        // can measure against, and probably the more useful one anyway: it says whether you are
        // caught up, not how far into an unknown whole you are.
        val aired = entry.nextAiringEpisode?.minus(1)?.takeIf { it > 0 }
        val scale = row.total ?: aired
        val againstAired = row.total == null && aired != null

        val progressFraction = scale
            ?.takeIf { it > 0 }
            ?.let { (entry.progress.toFloat() / it).coerceIn(0f, 1f) }
            ?: 0f

        // Marker sits where the latest aired episode falls. Pointless when the bar already ends
        // there, which is the unknown total case.
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
            // No end to count down to, so count against what is out.
            behind != null && behind > 0 -> context.getString(R.string.widget_wp_behind, behind)
            behind != null -> context.getString(R.string.widget_wp_caught_up)
            else -> context.getString(R.string.widget_wp_ongoing)
        }

        val airingLine = airingLine(context, entry, row.total, isManga)

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
            WidgetTheme.text(this, R.id.item_airing, colors.onSurfaceVariant, colors)
            WidgetTheme.badge(this, R.id.item_remaining, colors.primaryContainer, colors.onPrimaryContainer, colors)
            setTextViewText(R.id.item_title, entry.titleUserPreferred)
            setTextViewText(R.id.item_remaining, remainingLabel)
            setTextViewText(R.id.item_meta, meta)
            setTextViewText(R.id.item_airing, airingLine.orEmpty())
            setViewVisibility(R.id.item_airing, if (airingLine == null) View.GONE else View.VISIBLE)

            if (scale == null) {
                // Nothing to measure against: no total and no schedule to stand in for one. Most
                // ongoing manga, since AniList has no "latest released chapter" to divide by.
                //
                // Still draw the bar, as a faded band across the whole track. Keeps every row the
                // same shape, and a flat faded bar reads as "length unknown" rather than zero. The
                // pill and the count next to it are what actually say anything.
                setViewVisibility(R.id.item_bar, View.VISIBLE)
                setProgressBar(R.id.item_bar, BAR_MAX, 0, false)
                setInt(R.id.item_bar, "setSecondaryProgress", BAR_MAX)
                WidgetTheme.progressBar(views = this, viewId = R.id.item_bar, colors = colors)
            } else {
                setViewVisibility(R.id.item_bar, View.VISIBLE)
                setProgressBar(
                    R.id.item_bar,
                    BAR_MAX,
                    (progressFraction * BAR_MAX).toInt(),
                    false
                )
                // Secondary is the aired band behind the watched fill. Zero when there is nothing to
                // set apart, which leaves a plain two tone bar.
                setInt(
                    R.id.item_bar,
                    "setSecondaryProgress",
                    ((airedFraction ?: 0f) * BAR_MAX).toInt()
                )
                WidgetTheme.progressBar(
                    views = this,
                    viewId = R.id.item_bar,
                    colors = colors
                )
            }

            covers[entry.mediaId]?.let { setImageViewBitmap(R.id.item_cover, it) }
            setContentDescription(R.id.item_root, describe(context, row, isManga))
            setOnClickFillInIntent(R.id.item_root, WidgetIntents.openMediaFillIn(entry.mediaId))
        }
    }

    /**
     * "ep 11 in 2d", or "finale airs Sat" once the next episode is the last one.
     *
     * Uses the absolute timestamp, not timeUntilAiring, which is a snapshot from the last sync and
     * would count down from whenever that happened to be.
     */
    private fun airingLine(
        context: Context,
        entry: LibraryEntryEntity,
        total: Int?,
        isManga: Boolean
    ): String? {
        if (isManga) return null
        val episode = entry.nextAiringEpisode ?: return null
        val airingAt = entry.nextAiringEpisodeTime?.takeIf { it > 0 } ?: return null
        val now = System.currentTimeMillis() / 1000
        return if (total != null && episode >= total) {
            context.getString(R.string.widget_wp_finale, WidgetTime.weekdayOf(airingAt))
        } else {
            context.getString(
                R.string.widget_wp_next_ep,
                episode,
                WidgetTime.compactCountdown(context, airingAt, now)
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

        const val COVER_WIDTH_DP = 52
        /** Progress bar resolution, so a fraction survives being turned into an int. */
        const val BAR_MAX = 1000
    }
}
