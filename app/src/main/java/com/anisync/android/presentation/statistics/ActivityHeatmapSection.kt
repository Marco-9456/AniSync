package com.anisync.android.presentation.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.ActivityHistoryDay
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// AniList's activityHistory only spans ~6 recent months; the grid is sized to the data
// actually returned, bounded by these. MAX leaves headroom in case the API window grows.
private const val MIN_WEEKS = 18
private const val MAX_WEEKS = 53
private const val DAYS_IN_WEEK = 7
private const val SECONDS_PER_DAY = 86_400L

/**
 * GitHub-style activity calendar for an AniList profile. Each cell is a day; the color
 * intensity comes straight from AniList's `UserActivityHistory.level` (1-10), and tapping
 * a day reveals its date and activity count. Data is the account-wide
 * [ActivityHistoryDay] list sourced from the (deprecated) `User.stats.activityHistory`.
 */
@Composable
fun ActivityHeatmapSection(
    days: List<ActivityHistoryDay>,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    val model = remember(days) { buildHeatmapModel(days) }
    var selected by remember(days) { mutableStateOf<HeatmapCell?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.statistics_activity_history),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            val activeColor = MaterialTheme.colorScheme.primary
            val ringColor = MaterialTheme.colorScheme.onSurface
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            val textMeasurer = rememberTextMeasurer()
            val monthStyle = TextStyle(color = labelColor, fontSize = 10.sp)

            Column(Modifier.padding(20.dp)) {
                val sel = selected
                val headline = if (sel != null) {
                    pluralStringResource(
                        R.plurals.statistics_activity_count, sel.amount, sel.amount
                    ) + "  ·  " + sel.date.format(dateFormatter)
                } else {
                    stringResource(
                        R.string.statistics_activity_summary,
                        model.totalActivities,
                        model.activeDays
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    val tooltipState = rememberTooltipState()
                    val scope = rememberCoroutineScope()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(R.string.statistics_activity_update_info))
                            }
                        },
                        state = tooltipState
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(
                                R.string.statistics_activity_update_info
                            ),
                            tint = labelColor,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { scope.launch { tooltipState.show() } }
                                .padding(2.dp)
                                .size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val cellDp = 13.dp
                val gapDp = 4.dp
                val monthRowDp = 16.dp
                val gridWidth = (cellDp + gapDp) * model.weeks.size
                val gridHeight = monthRowDp + (cellDp + gapDp) * DAYS_IN_WEEK

                val scrollState = rememberScrollState()
                // Start scrolled to the most recent week (right edge), like GitHub.
                LaunchedEffect(model, scrollState.maxValue) {
                    scrollState.scrollTo(scrollState.maxValue)
                }

                val a11y = stringResource(R.string.statistics_activity_a11y, model.totalActivities)
                val selectedDate = sel?.date

                Box(Modifier.fillMaxWidth()) {
                  val fadeColor = MaterialTheme.colorScheme.surfaceContainer
                  Box(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .semantics { contentDescription = a11y }
                  ) {
                    Canvas(
                        modifier = Modifier
                            .size(width = gridWidth, height = gridHeight)
                            .pointerInput(model) {
                                val stridePx = cellDp.toPx() + gapDp.toPx()
                                val monthPx = monthRowDp.toPx()
                                detectTapGestures { offset ->
                                    if (offset.y < monthPx) return@detectTapGestures
                                    val w = (offset.x / stridePx).toInt()
                                    val r = ((offset.y - monthPx) / stridePx).toInt()
                                    val cell = model.weeks.getOrNull(w)?.getOrNull(r)
                                    if (cell != null && !cell.afterData) {
                                        selected = if (selected?.date == cell.date) null else cell
                                    }
                                }
                            }
                    ) {
                        val cellPx = cellDp.toPx()
                        val stridePx = cellPx + gapDp.toPx()
                        val monthPx = monthRowDp.toPx()
                        val corner = CornerRadius(cellPx * 0.28f, cellPx * 0.28f)

                        model.monthLabels.forEach { (weekIndex, label) ->
                            drawText(
                                textMeasurer = textMeasurer,
                                text = label,
                                topLeft = Offset(weekIndex * stridePx, 0f),
                                style = monthStyle
                            )
                        }

                        for (w in model.weeks.indices) {
                            val column = model.weeks[w]
                            for (r in 0 until DAYS_IN_WEEK) {
                                val cell = column[r]
                                if (cell.afterData) continue
                                val x = w * stridePx
                                val y = monthPx + r * stridePx
                                drawRoundRect(
                                    color = heatCellColor(cell.level, emptyColor, activeColor),
                                    topLeft = Offset(x, y),
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = corner
                                )
                                if (selectedDate == cell.date) {
                                    drawRoundRect(
                                        color = ringColor,
                                        topLeft = Offset(x, y),
                                        size = Size(cellPx, cellPx),
                                        cornerRadius = corner,
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                  }
                  ScrollEdgeFade(
                      visible = scrollState.canScrollBackward,
                      atStart = true,
                      color = fadeColor,
                      height = gridHeight,
                      modifier = Modifier.align(Alignment.CenterStart)
                  )
                  ScrollEdgeFade(
                      visible = scrollState.canScrollForward,
                      atStart = false,
                      color = fadeColor,
                      height = gridHeight,
                      modifier = Modifier.align(Alignment.CenterEnd)
                  )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.statistics_activity_less),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    listOf(0, 3, 6, 8, 10).forEach { level ->
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(heatCellColor(level, emptyColor, activeColor))
                        )
                    }
                    Text(
                        text = stringResource(R.string.statistics_activity_more),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            }
        }
    }
}

/**
 * A gradient + chevron overlay hinting that the heatmap scrolls. Shown at an edge only
 * when there is more content in that direction, so the cue appears/disappears as the
 * user scrolls (left cue is visible on first open, since it starts at the latest week).
 */
@Composable
private fun ScrollEdgeFade(
    visible: Boolean,
    atStart: Boolean,
    color: Color,
    height: Dp,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .width(36.dp)
                .background(
                    Brush.horizontalGradient(
                        if (atStart) listOf(color, Color.Transparent)
                        else listOf(Color.Transparent, color)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (atStart) Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Maps an AniList activity [level] (1-10, or 0 = no activity) to a heatmap cell color. */
private fun heatCellColor(level: Int, empty: Color, active: Color): Color =
    if (level <= 0) empty
    else active.copy(alpha = 0.30f + 0.70f * ((level.coerceIn(1, 10) - 1) / 9f))

private data class HeatmapCell(
    val date: LocalDate,
    val amount: Int,
    val level: Int,
    /** Past AniList's last counted day — not drawn; absence of data, not zero activity. */
    val afterData: Boolean
)

private data class HeatmapModel(
    /** [WEEK_COUNT] columns, each with [DAYS_IN_WEEK] cells (Sunday..Saturday). */
    val weeks: List<List<HeatmapCell>>,
    /** Week-column index -> short month name, emitted once per month change. */
    val monthLabels: List<Pair<Int, String>>,
    val totalActivities: Int,
    val activeDays: Int
)

private fun buildHeatmapModel(days: List<ActivityHistoryDay>): HeatmapModel {
    // Day buckets are stamped at midnight in AniList's server zone (23:00 UTC during BST),
    // so round to the nearest UTC day — truncating paints summer buckets one day early.
    val byDate = HashMap<LocalDate, ActivityHistoryDay>(days.size)
    for (day in days) {
        val date = LocalDate.ofEpochDay(Math.floorDiv(day.date + SECONDS_PER_DAY / 2, SECONDS_PER_DAY))
        val existing = byDate[date]
        if (existing == null || day.amount > existing.amount) byDate[date] = day
    }

    // Grid ends at the last day AniList has counted (stats lag ~48h) — padding out to
    // today draws empty cells the site doesn't show and reads as missing activity.
    val end = byDate.keys.max()
    val lastSunday = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val earliestSunday = byDate.keys.min().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val spanWeeks = (ChronoUnit.WEEKS.between(earliestSunday, lastSunday).toInt() + 1)
        .coerceIn(MIN_WEEKS, MAX_WEEKS)
    val startSunday = lastSunday.minusWeeks((spanWeeks - 1).toLong())

    val weeks = ArrayList<List<HeatmapCell>>(spanWeeks)
    val monthLabels = ArrayList<Pair<Int, String>>()
    var lastMonth = -1
    var total = 0
    var activeDays = 0

    for (w in 0 until spanWeeks) {
        val weekStart = startSunday.plusWeeks(w.toLong())
        if (weekStart.monthValue != lastMonth) {
            monthLabels += w to weekStart.month.getDisplayName(
                java.time.format.TextStyle.SHORT, Locale.getDefault()
            )
            lastMonth = weekStart.monthValue
        }
        val column = ArrayList<HeatmapCell>(DAYS_IN_WEEK)
        for (dow in 0 until DAYS_IN_WEEK) {
            val date = weekStart.plusDays(dow.toLong())
            if (date.isAfter(end)) {
                column += HeatmapCell(date, amount = 0, level = 0, afterData = true)
            } else {
                val entry = byDate[date]
                val amount = entry?.amount ?: 0
                if (amount > 0) {
                    total += amount
                    activeDays++
                }
                column += HeatmapCell(date, amount, entry?.level ?: 0, afterData = false)
            }
        }
        weeks += column
    }
    return HeatmapModel(weeks, monthLabels, total, activeDays)
}


@Preview(showBackground = true, name = "ActivityHeatmap — light", widthDp = 360)
@Composable
private fun ActivityHeatmapLightPreview() {
    StatPreviewSurface(isDark = false) {
        ActivityHeatmapSection(days = previewActivityDays())
    }
}

@Preview(showBackground = true, name = "ActivityHeatmap — dark", widthDp = 360)
@Composable
private fun ActivityHeatmapDarkPreview() {
    StatPreviewSurface(isDark = true) {
        ActivityHeatmapSection(days = previewActivityDays())
    }
}

private fun previewActivityDays(): List<ActivityHistoryDay> {
    val today = LocalDate.now()
    return (0 until 320).mapNotNull { i ->
        val amount = (i * 7 + i / 3) % 14
        if (amount == 0) return@mapNotNull null
        val date = today.minusDays(i.toLong()).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        ActivityHistoryDay(date = date, amount = amount, level = amount.coerceIn(1, 10))
    }
}