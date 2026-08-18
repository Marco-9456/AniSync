package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ActivityHistoryDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Past this width the bars stop growing. A 900.dp-wide bar carries no more information than a
 * 500.dp one, and the row's day label and count drift apart until they stop reading as one row.
 */
private val WeekContentMaxWidth = 560.dp

/**
 * On a wide card the leftover width goes to the summary beside the days rather than to longer bars,
 * so the block fills the card without the bars overstating what they measure.
 */
private val WeekSummaryColumnWidth = 200.dp
private val WeekWideGap = 20.dp
private val WeekWideContentMaxWidth = WeekContentMaxWidth + WeekWideGap + WeekSummaryColumnWidth

/** Below this the summary stays under the days, where a narrow card has room for it. */
private val WeekWideMinWidth = 660.dp

private val BarHeight = 18.dp
private val RowHeight = 32.dp
/** Matches the seven day rows plus their gaps, so the divider spans the days and nothing else. */
private val WeekSummaryDividerHeight = (RowHeight + 4.dp) * DAYS_IN_WEEK

/** Shortest bar drawn for a day with any activity, so a single activity is still visible. */
private val MinBarWidth = 20.dp

/** How much of AniList's window the user may page back through, mirroring the heatmap's cap. */
private const val MAX_WEEKS_BACK = 52

/**
 * One Sunday-to-Saturday week of [ActivityHistoryDay] data, each day drawn as a bar proportional
 * to the busiest day in that week.
 *
 * The interesting part is not the bars, it is the three states a day can be in. AniList recomputes
 * `activityHistory` roughly every 48 hours, so the current week always ends in days that have no
 * counts yet. Drawing those as zero would read as "you did nothing", so they are drawn as
 * [DayDataState.Awaiting] instead, and days that have not happened yet as [DayDataState.Future].
 */
@Composable
internal fun ActivityWeekBreakdown(
    byDate: Map<LocalDate, ActivityHistoryDay>,
    userId: Int,
    onMediaClick: (Int) -> Unit,
    onActivityClick: (Int) -> Unit
) {
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    var openDay by remember { mutableStateOf<ActivityDay?>(null) }
    val week = remember(byDate, weekOffset) { buildActivityWeek(byDate, weekOffset) }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val locale = LocalConfiguration.current.locales[0]

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val wide = maxWidth >= WeekWideMinWidth
        val contentMaxWidth = if (wide) WeekWideContentMaxWidth else WeekContentMaxWidth

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = contentMaxWidth)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pluralStringResource(
                        R.plurals.statistics_activity_count, week.total, week.total
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.statistics_activity_per_day, week.perDayLabel(locale)),
                    style = MaterialTheme.typography.labelMedium,
                    color = mutedColor
                )
                Spacer(Modifier.width(10.dp))
                ActivityUpdateInfo(tint = labelColor)
            }

            Spacer(Modifier.height(12.dp))

            WeekNavigation(
                week = week,
                onPrevious = { weekOffset -= 1 },
                onNext = { weekOffset += 1 }
            )

            Spacer(Modifier.height(12.dp))

            val days: @Composable ColumnScope.() -> Unit = {
                week.days.forEach { day ->
                    ActivityDayRow(
                        day = day,
                        weekMax = week.busiestAmount,
                        onClick = if (day.amount > 0) ({ openDay = day }) else null
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(WeekWideGap)) {
                    Column(modifier = Modifier.weight(1f), content = days)
                    VerticalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.height(WeekSummaryDividerHeight)
                    )
                    WeekSummary(
                        week = week,
                        stacked = true,
                        modifier = Modifier.width(WeekSummaryColumnWidth - WeekWideGap)
                    )
                }
            } else {
                Column(content = days)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))

                WeekSummary(week = week, stacked = false)
            }
        }
    }

    openDay?.let { day ->
        ActivityDaySheet(
            userId = userId,
            day = day,
            onDismiss = { openDay = null },
            onOpenActivity = { activity ->
                // The sheet is a dead end otherwise: a row names something the user then has to go
                // and find. Close first, so returning does not land back on top of the sheet.
                openDay = null
                val mediaId = activity.mediaId
                if (mediaId != null) onMediaClick(mediaId) else onActivityClick(activity.id)
            }
        )
    }
}

@Composable
private fun WeekNavigation(
    week: ActivityWeek,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val rangeFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = onPrevious,
            enabled = week.canGoBack,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_activity_week_previous)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    R.string.statistics_activity_week_range,
                    week.start.format(rangeFormatter),
                    week.start.plusDays(6).format(rangeFormatter)
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when (week.offset) {
                    0 -> stringResource(R.string.statistics_activity_week_current)
                    -1 -> stringResource(R.string.statistics_activity_week_previous)
                    else -> pluralStringResource(
                        R.plurals.statistics_activity_weeks_ago,
                        abs(week.offset),
                        abs(week.offset)
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        FilledTonalIconButton(
            onClick = onNext,
            enabled = week.canGoForward,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_activity_week_next)
            )
        }
    }
}

@Composable
private fun ActivityDayRow(day: ActivityDay, weekMax: Int, onClick: (() -> Unit)?) {
    val counted = day.state == DayDataState.Counted
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = when (day.state) {
            DayDataState.Counted -> 0.08f
            DayDataState.Awaiting -> 0.06f
            DayDataState.Future -> 0.04f
        }
    )

    val locale = LocalConfiguration.current.locales[0]
    val fullDayName = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val description = when (day.state) {
        DayDataState.Counted -> stringResource(
            R.string.a11y_activity_day,
            fullDayName,
            pluralStringResource(R.plurals.statistics_activity_count, day.amount, day.amount)
        )
        DayDataState.Awaiting -> stringResource(R.string.a11y_activity_day_awaiting, fullDayName)
        DayDataState.Future -> fullDayName
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (day.isToday) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                else Modifier
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = if (day.isToday) 8.dp else 0.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                day.isToday -> MaterialTheme.colorScheme.primary
                day.state == DayDataState.Future -> muted
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = muted,
            modifier = Modifier.width(24.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(BarHeight)
                .clip(CircleShape)
                .background(trackColor),
            contentAlignment = Alignment.CenterStart
        ) {
            if (counted && day.amount > 0) {
                val fraction = day.amount.toFloat() / weekMax.coerceAtLeast(1)
                Box(
                    Modifier
                        .width((maxWidth * fraction).coerceAtLeast(MinBarWidth))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.35f + 0.65f * fraction
                            )
                        )
                )
            } else if (day.state == DayDataState.Awaiting) {
                Text(
                    text = if (day.isToday) stringResource(R.string.statistics_activity_today_pending)
                    else stringResource(R.string.statistics_activity_awaiting_inline),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = when (day.state) {
                DayDataState.Counted -> day.amount.toString()
                DayDataState.Awaiting -> stringResource(R.string.statistics_activity_no_value)
                DayDataState.Future -> ""
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            color = if (counted && day.amount > 0) MaterialTheme.colorScheme.onSurface else muted,
            modifier = Modifier.width(32.dp)
        )

        // Only days with something to show advertise a tap.
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = muted,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Spacer(Modifier.width(16.dp))
        }
    }
}

@Composable
private fun WeekSummary(
    week: ActivityWeek,
    stacked: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val busiest = week.busiest
    val stats = buildList {
        add(
            stringResource(R.string.statistics_activity_busiest_day) to (
                busiest?.let {
                    stringResource(
                        R.string.statistics_activity_busiest_value,
                        it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        it.amount
                    )
                } ?: stringResource(R.string.statistics_activity_no_value)
                )
        )
        if (week.awaitingDays > 0) {
            add(
                stringResource(R.string.statistics_activity_days_counted) to stringResource(
                    R.string.statistics_activity_day_ratio, week.countedDays, DAYS_IN_WEEK
                )
            )
            add(
                stringResource(R.string.statistics_activity_awaiting) to pluralStringResource(
                    R.plurals.statistics_activity_days, week.awaitingDays, week.awaitingDays
                )
            )
        } else {
            add(
                stringResource(R.string.statistics_activity_active_days) to stringResource(
                    R.string.statistics_activity_day_ratio, week.activeDays, DAYS_IN_WEEK
                )
            )
            add(
                stringResource(R.string.statistics_activity_vs_previous) to when {
                    week.changePercent == null -> stringResource(R.string.statistics_activity_no_value)
                    week.changePercent >= 0 -> stringResource(
                        R.string.statistics_activity_percent_up, week.changePercent
                    )
                    else -> stringResource(
                        R.string.statistics_activity_percent_down, abs(week.changePercent)
                    )
                }
            )
        }
    }

    val stat: @Composable (String, String, Modifier) -> Unit = { label, value, statModifier ->
        Column(
            modifier = statModifier,
            horizontalAlignment = if (stacked) Alignment.Start else Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = if (stacked) TextAlign.Start else TextAlign.Center
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (stacked) TextAlign.Start else TextAlign.Center
            )
        }
    }

    if (stacked) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            stats.forEach { (label, value) -> stat(label, value, Modifier.fillMaxWidth()) }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEach { (label, value) -> stat(label, value, Modifier.weight(1f)) }
        }
    }
}

/** Whether AniList has actually counted a day yet, which is not the same as the day being empty. */
internal enum class DayDataState { Counted, Awaiting, Future }

internal data class ActivityDay(
    val date: LocalDate,
    val amount: Int,
    val state: DayDataState,
    val isToday: Boolean,
    /**
     * AniList's own bucket timestamp for the day, which is midnight in *its* zone rather than in
     * UTC. The day sheet queries from here, so the list it opens covers the same hours AniList
     * counted into the bar.
     */
    val epochSeconds: Long?
)

internal data class ActivityWeek(
    val start: LocalDate,
    /** 0 = the week containing today, negative = further back. */
    val offset: Int,
    val days: List<ActivityDay>,
    val total: Int,
    val countedDays: Int,
    val activeDays: Int,
    val awaitingDays: Int,
    val busiest: ActivityDay?,
    /** Percent change against the previous week, or null when that week is not fully counted. */
    val changePercent: Int?,
    val canGoBack: Boolean,
    val canGoForward: Boolean
) {
    val busiestAmount: Int get() = busiest?.amount ?: 0

    /** Averaged over counted days only — dividing by 7 understates a half-counted current week. */
    fun perDayLabel(locale: Locale): String {
        if (countedDays == 0) return "0"
        val perDay = total.toFloat() / countedDays
        return String.format(locale, "%.1f", perDay)
    }
}

internal fun buildActivityWeek(
    byDate: Map<LocalDate, ActivityHistoryDay>,
    offset: Int,
    today: LocalDate = LocalDate.now()
): ActivityWeek {
    val lastCounted = byDate.keys.maxOrNull() ?: today
    val earliestStart = (byDate.keys.minOrNull() ?: today)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val currentStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val start = currentStart.plusWeeks(offset.toLong())

    val days = (0 until DAYS_IN_WEEK).map { index ->
        val date = start.plusDays(index.toLong())
        val state = when {
            date.isAfter(today) -> DayDataState.Future
            date.isAfter(lastCounted) -> DayDataState.Awaiting
            else -> DayDataState.Counted
        }
        val entry = byDate[date]
        ActivityDay(
            date = date,
            amount = if (state == DayDataState.Counted) entry?.amount ?: 0 else 0,
            state = state,
            isToday = date == today,
            epochSeconds = entry?.date
        )
    }

    val counted = days.filter { it.state == DayDataState.Counted }
    val previousTotal = previousWeekTotal(byDate, start, lastCounted)
    val total = counted.sumOf { it.amount }

    return ActivityWeek(
        start = start,
        offset = offset,
        days = days,
        total = total,
        countedDays = counted.size,
        activeDays = counted.count { it.amount > 0 },
        awaitingDays = days.count { it.state == DayDataState.Awaiting },
        busiest = counted.filter { it.amount > 0 }.maxByOrNull { it.amount },
        changePercent = when {
            previousTotal == null || previousTotal == 0 -> null
            else -> (((total - previousTotal) * 100f) / previousTotal).roundToInt()
        },
        canGoBack = start.isAfter(earliestStart) && offset > -MAX_WEEKS_BACK,
        canGoForward = offset < 0
    )
}

/** Null when the previous week is not fully counted, so a partial week never skews the comparison. */
private fun previousWeekTotal(
    byDate: Map<LocalDate, ActivityHistoryDay>,
    start: LocalDate,
    lastCounted: LocalDate
): Int? {
    val previousStart = start.minusWeeks(1)
    if (previousStart.plusDays((DAYS_IN_WEEK - 1).toLong()).isAfter(lastCounted)) return null
    return (0 until DAYS_IN_WEEK).sumOf { byDate[previousStart.plusDays(it.toLong())]?.amount ?: 0 }
}

@Preview(showBackground = true, name = "ActivityWeek — dark", widthDp = 360)
@Composable
private fun ActivityWeekDarkPreview() {
    StatPreviewSurface(isDark = true) {
        ActivityWeekBreakdown(
            byDate = bucketActivityDays(previewActivityDays()),
            userId = 1,
            onMediaClick = {},
            onActivityClick = {}
        )
    }
}

@Preview(showBackground = true, name = "ActivityWeek — tablet width", widthDp = 800)
@Composable
private fun ActivityWeekWidePreview() {
    StatPreviewSurface(isDark = true) {
        ActivityWeekBreakdown(
            byDate = bucketActivityDays(previewActivityDays()),
            userId = 1,
            onMediaClick = {},
            onActivityClick = {}
        )
    }
}
