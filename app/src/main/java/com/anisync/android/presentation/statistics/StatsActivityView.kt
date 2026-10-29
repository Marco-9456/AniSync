package com.anisync.android.presentation.statistics

import android.icu.text.MessageFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.ActivityHistoryDay
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.formatOneDecimal
import com.anisync.android.domain.stats.ActivityInsights
import com.anisync.android.domain.stats.MonthActivity
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.lineBox
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

/** A section header as the stats views space it: 24 above, 4 to the card below. */
@Composable
fun StatsSectionHeader(title: String, modifier: Modifier = Modifier, inset: Dp = 24.dp, top: Dp = 24.dp) {
    SectionHeader(
        title = title,
        modifier = modifier,
        padding = PaddingValues(start = inset, end = inset, top = top + 8.dp, bottom = 12.dp)
    )
}

/**
 * The Activity view's four insights, two by two, or in one row of four where the pane is wide.
 * The busiest day opens what happened on it.
 */
@Composable
fun ActivityInsightTiles(
    insights: ActivityInsights,
    history: List<ActivityHistoryDay>,
    userId: Int,
    onMediaClick: (Int) -> Unit,
    onActivityClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    var openDay by remember { mutableStateOf<ActivityDay?>(null) }
    val total = formatExactCount(insights.total, locale)

    val tiles = listOf(
        InsightTileModel(
            value = if (insights.streakIsOpenEnded) {
                stringResource(R.string.stats_count_open_ended, insights.longestStreak)
            } else {
                insights.longestStreak.toString()
            },
            valueSize = NumberSize,
            label = pluralStringResourceOrDefault(R.plurals.stats_day_streak, insights.longestStreak),
            detail = when {
                insights.streakIsOpenEnded -> stringResource(R.string.stats_streak_every_kept_day)
                insights.longestStreak < 2 -> stringResource(R.string.stats_streak_none)
                else -> stringResource(
                    R.string.stats_streak_range,
                    shortDate(insights.streakStart, locale),
                    shortDate(insights.streakEnd, locale)
                )
            },
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
        ),
        InsightTileModel(
            value = weekdayName(insights.busiestWeekday, locale),
            valueSize = WeekdaySize,
            label = stringResource(R.string.stats_busiest_weekday),
            detail = pluralStringResourceOrDefault(
                R.plurals.stats_weekday_share,
                insights.total,
                formatExactCount(insights.busiestWeekdayTotal, locale),
                total
            ),
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurface,
        ),
        InsightTileModel(
            value = formatOneDecimal(insights.perDay, locale),
            valueSize = NumberSize,
            label = stringResource(R.string.stats_a_day),
            detail = stringResource(
                R.string.stats_activities_over_days,
                pluralStringResourceOrDefault(R.plurals.statistics_activity_count, insights.total, insights.total),
                pluralStringResourceOrDefault(R.plurals.statistics_activity_days, insights.days, insights.days)
            ),
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurface,
        ),
        InsightTileModel(
            value = shortDate(insights.busiestDay, locale),
            valueSize = DateSize,
            label = stringResource(R.string.stats_busiest_day),
            detail = pluralStringResourceOrDefault(
                R.plurals.stats_busiest_day_detail,
                insights.busiestDayTotal,
                insights.busiestDayTotal
            ),
            container = scheme.tertiaryContainer,
            content = scheme.onTertiaryContainer,
            onClick = {
                val bucket = bucketActivityDays(history)[insights.busiestDay]
                openDay = ActivityDay(
                    date = insights.busiestDay,
                    amount = insights.busiestDayTotal,
                    state = DayDataState.Counted,
                    isToday = false,
                    epochSeconds = bucket?.date
                )
            },
        ),
    )

    // The rows measure their tiles' intrinsic heights to keep each row even, which a
    // BoxWithConstraints inside a tile cannot answer, so the value width is worked out here.
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val valueWidth = (maxWidth - TileGap * (columns - 1)) / columns - TilePadding * 2
        Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
            tiles.chunked(columns).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                    row.forEach { tile ->
                        InsightTile(
                            tile = tile,
                            valueWidth = valueWidth,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }

    openDay?.let { day ->
        ActivityDaySheet(
            userId = userId,
            day = day,
            onDismiss = { openDay = null },
            onOpenActivity = { activity ->
                openDay = null
                val mediaId = activity.mediaId
                if (mediaId != null) onMediaClick(mediaId) else onActivityClick(activity.id)
            }
        )
    }
}

private class InsightTileModel(
    val value: String,
    val valueSize: Float,
    val label: String,
    val detail: String,
    val container: Color,
    val content: Color,
    val onClick: (() -> Unit)? = null,
)

private const val NumberSize = 32f
private const val WeekdaySize = 26f
private const val DateSize = 27f
private val TileGap = 8.dp
private val TilePadding = 16.dp

@Composable
private fun InsightTile(tile: InsightTileModel, valueWidth: Dp, modifier: Modifier = Modifier) {
    val heroWide = LocalExpressiveTypography.current.heroWide
    val onClick = tile.onClick
    Column(
        modifier = modifier
            .heightIn(min = InsightTileHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(tile.container)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(TilePadding),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val style = fittedStyle(tile.value, heroWide.style(tile.valueSize.sp, 38.sp, (-1).sp), valueWidth)
        Text(text = tile.value, style = style, color = tile.content, maxLines = 1, modifier = Modifier.lineBox(style))
        Text(text = tile.label, style = MaterialTheme.typography.titleSmall, color = tile.content)
        Text(text = tile.detail, style = MaterialTheme.typography.bodySmall, color = tile.content.copy(alpha = 0.8f))
    }
}

private val InsightTileHeight = 148.dp

/** One bar of a weekday or month chart. A partial month is drawn faded. */
@Immutable
data class ChartBar(val label: String, val count: Int, val isPeak: Boolean, val isPartial: Boolean = false)

/** Activity per weekday, starting on the locale's first day of the week. */
@Composable
fun WeekdayChart(insights: ActivityInsights, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val first = WeekFields.of(locale).firstDayOfWeek
    val bars = (0L until 7L).map { offset ->
        val day = first.plus(offset)
        ChartBar(
            label = day.getDisplayName(TextStyle.SHORT, locale),
            count = insights.byWeekday[day] ?: 0,
            isPeak = day == insights.busiestWeekday
        )
    }
    BarChartCard(bars = bars, barWidth = 40.dp, modifier = modifier)
}

/** Activity per calendar month, the months AniList only partly keeps faded and explained. */
@Composable
fun MonthChart(insights: ActivityInsights, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val peak = insights.byMonth.maxByOrNull { it.total }
    val bars = insights.byMonth.map { month ->
        ChartBar(
            label = month.month.month.getDisplayName(TextStyle.SHORT, locale),
            count = month.total,
            isPeak = month == peak,
            isPartial = month.startsLate || month.endsEarly
        )
    }
    BarChartCard(
        bars = bars,
        barWidth = 36.dp,
        barsInset = 4.dp,
        note = partialMonthsNote(insights.byMonth, locale),
        modifier = modifier
    )
}

@Composable
private fun partialMonthsNote(months: List<MonthActivity>, locale: Locale): String? {
    val first = months.firstOrNull() ?: return null
    val last = months.last()
    val ordinal = remember(locale) { MessageFormat("{0,ordinal}", locale) }
    fun day(date: LocalDate): String = ordinal.format(arrayOf(date.dayOfMonth))
    return when {
        months.size == 1 && (first.startsLate || first.endsEarly) -> stringResource(
            R.string.stats_months_partial_single,
            monthName(first, locale),
            day(first.firstDay),
            day(first.lastDay)
        )
        first.startsLate && last.endsEarly && months.size > 1 -> stringResource(
            R.string.stats_months_partial_both,
            monthName(first, locale),
            day(first.firstDay),
            monthName(last, locale),
            day(last.lastDay)
        )
        first.startsLate -> stringResource(R.string.stats_months_partial_first, monthName(first, locale), day(first.firstDay))
        last.endsEarly -> stringResource(R.string.stats_months_partial_last, monthName(last, locale), day(last.lastDay))
        else -> null
    }
}

private fun monthName(month: MonthActivity, locale: Locale): String =
    month.month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }

/**
 * Bars on a shared baseline, the tallest 112dp, each with its count above and its label below.
 * The busiest bar and its labels take the primary colour.
 */
@Composable
private fun BarChartCard(
    bars: List<ChartBar>,
    barWidth: Dp,
    modifier: Modifier = Modifier,
    barsInset: Dp = 0.dp,
    note: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val max = bars.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val countStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = if (note != null) 10.dp else 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = barsInset),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                val accent = if (bar.isPeak) scheme.primary else scheme.onSurfaceVariant
                val description = bar.label + ", " +
                    pluralStringResourceOrDefault(R.plurals.statistics_activity_count, bar.count, bar.count)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = description }
                ) {
                    Text(text = bar.count.toString(), style = countStyle, color = accent, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(barWidth)
                            .height((MaxBarHeight * bar.count / max).roundToInt().coerceAtLeast(MinBarHeight).dp)
                            .clip(RoundedCornerShape(barWidth / 2))
                            .background(
                                (if (bar.isPeak) scheme.primary else scheme.secondaryContainer)
                                    .copy(alpha = if (bar.isPartial) 0.5f else 1f)
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(text = bar.label, style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1)
                }
            }
        }
        if (note != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = note, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

private const val MaxBarHeight = 112f
private const val MinBarHeight = 6

/** "12 Jul", in the order the locale writes a day and a short month. */
internal fun shortDate(date: LocalDate, locale: Locale): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMM")
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}

internal fun weekdayName(day: DayOfWeek, locale: Locale): String =
    day.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
