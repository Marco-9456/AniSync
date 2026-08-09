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
import com.anisync.android.widget.core.WidgetChips
import com.anisync.android.widget.core.WidgetColors
import com.anisync.android.widget.core.WidgetImageBudget
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntents
import com.anisync.android.widget.core.WidgetMediaRow
import com.anisync.android.widget.core.WidgetSizes
import com.anisync.android.widget.core.WidgetState
import com.anisync.android.widget.core.WidgetTheme
import com.anisync.android.widget.core.WidgetTime
import com.anisync.android.widget.core.activeOwnerId
import com.anisync.android.widget.core.widgetDeps

/**
 * Airing Today: everything airing between midnight and midnight, filtered to the list or not.
 *
 * The filter is read before the query rather than after, so My List no longer decodes a cover for
 * every show airing today and then throws most of them away.
 */
class AiringTodayWidgetProvider : AniSyncWidgetProvider<AiringTodayWidgetProvider.Snapshot>() {

    data class Snapshot(
        val episodes: List<AiringScheduleEntity>,
        val myListOnly: Boolean,
        val nowSeconds: Long
    ) {
        /**
         * What the hero shows: the next episode still to air, or the last one once the day is done.
         * Showing nothing on a day that has entries would be wrong.
         */
        val next: AiringScheduleEntity?
            get() = episodes.firstOrNull { it.airingAt > nowSeconds } ?: episodes.lastOrNull()
    }

    override val declaredSizes = WidgetSizes.ladder(minHeightDp = 100)

    override val listViewId = R.id.widget_list
    override val listEmptyViewId = R.id.widget_empty

    override suspend fun snapshot(context: Context, appWidgetId: Int): Snapshot {
        val deps = context.widgetDeps()
        val ownerId = deps.activeOwnerId()
        val myListOnly = WidgetState.getBoolean(context, appWidgetId, STATE_MY_LIST, false)
        val start = WidgetTime.startOfToday()
        val end = start + WidgetTime.DAY

        val dao = deps.airingScheduleDao()
        // No cap, the list scrolls so we show the whole day.
        val episodes = if (myListOnly) {
            dao.getAiringBetweenForUser(ownerId, start, end)
        } else {
            dao.getAiringBetween(ownerId, start, end)
        }
        return Snapshot(episodes, myListOnly, System.currentTimeMillis() / 1000)
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
        val views = RemoteViews(context.packageName, R.layout.widget_airing_today)
        val colors = WidgetColors.of(context)
        WidgetTheme.applyChrome(views, colors)

        WidgetChips.apply(views, context, R.id.chip_all, !snapshot.myListOnly, colors)
        WidgetChips.apply(views, context, R.id.chip_mine, snapshot.myListOnly, colors)
        views.setOnClickPendingIntent(
            R.id.chip_all,
            WidgetIntents.setState(context, javaClass, appWidgetId, STATE_MY_LIST, "false")
        )
        views.setOnClickPendingIntent(
            R.id.chip_mine,
            WidgetIntents.setState(context, javaClass, appWidgetId, STATE_MY_LIST, "true")
        )
        views.setContentDescription(R.id.chip_all, context.getString(R.string.a11y_widget_filter_all))
        views.setContentDescription(R.id.chip_mine, context.getString(R.string.a11y_widget_filter_mine))
        views.setViewVisibility(
            R.id.widget_title,
            if (WidgetSizes.isNarrow(size)) View.GONE else View.VISIBLE
        )

        val isEmpty = snapshot.episodes.isEmpty()
        // At short sizes a list is just a header and one clipped row. The old widget gave the whole
        // surface to a single card here and dropped the chrome with it, same as this.
        val short = WidgetSizes.isShort(size)
        val hero = !isEmpty && short
        // Chips live in the header, so hiding it takes the filter too. Fine at this size, a hero
        // card plus a filter row leaves no room for the card.
        views.setViewVisibility(R.id.widget_header, if (short) View.GONE else View.VISIBLE)

        views.setViewVisibility(R.id.widget_empty, if (isEmpty) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_hero, if (hero) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_list, if (!isEmpty && !hero) View.VISIBLE else View.GONE)

        if (isEmpty) {
            views.setTextViewText(
                R.id.widget_empty_title,
                context.getString(
                    if (snapshot.myListOnly) R.string.widget_airing_today_empty_mine
                    else R.string.widget_airing_today_empty_all
                )
            )
            views.setTextViewText(
                R.id.widget_empty_body,
                context.getString(
                    if (snapshot.myListOnly) R.string.widget_airing_today_empty_mine_hint
                    else R.string.widget_airing_today_empty_all_hint
                )
            )
            return views
        }

        if (hero) {
            snapshot.next?.let { bindHero(context, views, appWidgetId, it, covers, colors) }
        }

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
        val time = WidgetTime.clock(context, episode.airingAt)
        WidgetMediaRow.build(
            context = context,
            title = episode.titleUserPreferred,
            subtitle = context.getString(R.string.widget_episode_long, episode.episode),
            trailing = time,
            mediaId = episode.mediaId,
            cover = covers[episode.id],
            colors = WidgetColors.of(context),
            contentDescription = context.getString(
                R.string.a11y_widget_airing_row,
                episode.titleUserPreferred,
                episode.episode,
                time
            )
        )
    }

    private fun bindHero(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        episode: AiringScheduleEntity,
        covers: Map<Int, Bitmap?>,
        colors: WidgetColors
    ) {
        // The hero is not a shared row, so it gets themed here instead of by the row builder.
        // Missing these is why the card kept the device palette while the rest followed the app.
        WidgetTheme.card(views, R.id.widget_hero, colors)
        WidgetTheme.poster(views, R.id.hero_cover, colors)
        WidgetTheme.icon(views, R.id.hero_clock, colors.primary, colors)
        WidgetTheme.text(views, R.id.hero_time, colors.primary, colors)
        WidgetTheme.text(views, R.id.hero_title, colors.onSurface, colors)
        WidgetTheme.badge(
            views,
            R.id.hero_episode,
            colors.tertiaryContainer,
            colors.onTertiaryContainer,
            colors
        )

        val time = WidgetTime.clock(context, episode.airingAt)
        views.setTextViewText(R.id.hero_time, context.getString(R.string.widget_airing_at, time))
        views.setTextViewText(R.id.hero_title, episode.titleUserPreferred)
        views.setTextViewText(
            R.id.hero_episode,
            context.getString(R.string.widget_episode_badge, episode.episode)
        )
        covers[episode.id]?.let { views.setImageViewBitmap(R.id.hero_cover, it) }
        views.setContentDescription(
            R.id.widget_hero,
            context.getString(
                R.string.a11y_widget_airing_row,
                episode.titleUserPreferred,
                episode.episode,
                time
            )
        )
        // Not part of the collection, so it carries its own PendingIntent instead of filling in
        // against the list template.
        views.setOnClickPendingIntent(
            R.id.widget_hero,
            WidgetIntents.openMedia(context, appWidgetId, episode.mediaId)
        )
    }

    private companion object {
        const val STATE_MY_LIST = "my_list_only"
        const val COVER_WIDTH_DP = 64
    }
}
