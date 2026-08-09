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
 * Weekly Calendar: a seven day strip over the schedule for the selected day.
 *
 * The old Glance version got this one most visibly wrong. It grabbed the episode list in
 * provideGlance before composition, so tapping a day redrew the strip against the previous day
 * episodes and only fixed itself when a second update landed. Here the day is written, read back,
 * queried and rendered once.
 */
class WeeklyCalendarWidgetProvider :
    AniSyncWidgetProvider<WeeklyCalendarWidgetProvider.Snapshot>() {

    data class Snapshot(
        val episodes: List<AiringScheduleEntity>,
        val selectedDay: Int,
        val myListOnly: Boolean
    )

    override val declaredSizes = WidgetSizes.ladder(minHeightDp = 250)

    override val listViewId = R.id.widget_list
    override val listEmptyViewId = R.id.widget_empty

    override suspend fun snapshot(context: Context, appWidgetId: Int): Snapshot {
        val deps = context.widgetDeps()
        val ownerId = deps.activeOwnerId()
        val selectedDay = WidgetState.getInt(context, appWidgetId, STATE_DAY, 0).coerceIn(0, DAYS - 1)
        val myListOnly = WidgetState.getBoolean(context, appWidgetId, STATE_MY_LIST, false)

        val start = WidgetTime.startOfDay(selectedDay)
        val end = start + WidgetTime.DAY
        val dao = deps.airingScheduleDao()
        // No cap, the list scrolls so we show the whole day instead of whatever happened to fit.
        val episodes = if (myListOnly) {
            dao.getAiringBetweenForUser(ownerId, start, end)
        } else {
            dao.getAiringBetween(ownerId, start, end)
        }
        return Snapshot(episodes, selectedDay, myListOnly)
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
        val views = RemoteViews(context.packageName, R.layout.widget_weekly_calendar)
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

        views.removeAllViews(R.id.widget_days)
        repeat(DAYS) { offset ->
            views.addView(
                R.id.widget_days,
                dayCell(context, appWidgetId, offset, snapshot.selectedDay, colors)
            )
        }

        views.setTextViewText(
            R.id.widget_empty_title,
            context.getString(
                if (snapshot.myListOnly) R.string.widget_calendar_empty_mine
                else R.string.widget_calendar_empty_all
            )
        )
        // setEmptyView swaps these once the host binds the adapter. Setting them here as well means
        // the right one is already up in the first frame.
        val isEmpty = snapshot.episodes.isEmpty()
        views.setViewVisibility(R.id.widget_empty, if (isEmpty) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_list, if (isEmpty) View.GONE else View.VISIBLE)

        // One template for all rows, each row fills in which series it opens.
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
        // Follows the device 12/24 hour setting. The time is a pill here, not a fixed column, so an
        // AM/PM suffix has room.
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

    private fun dayCell(
        context: Context,
        appWidgetId: Int,
        offset: Int,
        selected: Int,
        colors: WidgetColors
    ): RemoteViews {
        val isSelected = offset == selected
        val letter = WidgetTime.narrowWeekday(offset)
        val number = WidgetTime.dayOfMonth(offset)
        return RemoteViews(context.packageName, R.layout.widget_weekly_calendar_day).apply {
            setTextViewText(R.id.day_name, letter)
            setTextViewText(R.id.day_number, number.toString())
            WidgetTheme.daySelection(
                views = this,
                rootId = R.id.day_root,
                nameId = R.id.day_name,
                numberId = R.id.day_number,
                selected = isSelected,
                colors = colors
            )
            setContentDescription(
                R.id.day_root,
                context.getString(
                    if (isSelected) R.string.a11y_widget_day_selected else R.string.a11y_widget_day,
                    WidgetTime.shortWeekday(offset),
                    number
                )
            )
            setOnClickPendingIntent(
                R.id.day_root,
                WidgetIntents.setState(
                    context,
                    WeeklyCalendarWidgetProvider::class.java,
                    appWidgetId,
                    STATE_DAY,
                    offset.toString()
                )
            )
        }
    }

    private companion object {
        const val STATE_DAY = "selected_day"
        const val STATE_MY_LIST = "my_list_only"
        const val DAYS = 7
        const val COVER_WIDTH_DP = 56
    }
}
