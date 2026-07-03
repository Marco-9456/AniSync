package com.anisync.android.presentation.details.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.MediaAiringTrend
import com.anisync.android.domain.MediaRanking
import com.anisync.android.domain.MediaRankingType
import com.anisync.android.domain.MediaScoreSlice
import com.anisync.android.domain.MediaStats
import com.anisync.android.domain.MediaStatusSlice
import com.anisync.android.domain.MediaTrendPoint
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.details.MediaStatsState
import com.anisync.android.presentation.statistics.StatPreviewSurface
import com.anisync.android.presentation.util.formatCompactNumber
import com.anisync.android.ui.theme.LocalExpressiveTypography
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/**
 * The media-detail Stats tab: community rankings, the recent sitewide activity
 * trend, per-episode airing progression (score + watchers) and the status/score
 * distributions — AniList's media Stats page rebuilt in the app's stats
 * vocabulary (24dp cards, expressive numerics, eyebrow labels).
 */
fun LazyListScope.mediaStatsTabContent(
    state: MediaStatsState,
    meanScore: Int?,
    isManga: Boolean,
    onRetry: () -> Unit,
    onRankingClick: (MediaRanking) -> Unit = {}
) {
    val stats = state.stats

    if (!state.initialized) {
        if (state.isError) {
            item(key = "stats_error") { StatsMessageBox(isError = true, onRetry = onRetry) }
        } else {
            item(key = "stats_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppCircularProgressIndicator()
                }
            }
        }
        return
    }

    if (stats == null || stats.isEmpty) {
        item(key = "stats_empty") { StatsMessageBox(isError = false, onRetry = null) }
        return
    }

    if (stats.rankings.isNotEmpty()) {
        item(key = "stats_rankings") {
            StatsSectionSpacer()
            RankingsSection(rankings = stats.rankings, onRankingClick = onRankingClick)
        }
    }

    if (stats.recentActivity.size >= 2) {
        item(key = "stats_activity") {
            StatsSectionSpacer()
            ActivityPerDaySection(points = stats.recentActivity)
        }
    }

    val scorePoints = stats.airingProgression.filter { it.averageScore != null }
    if (scorePoints.size >= 2) {
        item(key = "stats_score_progression") {
            StatsSectionSpacer()
            ScoreProgressionSection(points = scorePoints)
        }
    }

    val watcherPoints = stats.airingProgression.filter { (it.watching ?: 0) > 0 }
    if (watcherPoints.size >= 2) {
        item(key = "stats_watchers_progression") {
            StatsSectionSpacer()
            WatchersProgressionSection(points = watcherPoints)
        }
    }

    if (stats.statusDistribution.any { it.amount > 0 }) {
        item(key = "stats_status_distribution") {
            StatsSectionSpacer()
            StatusDistributionSection(slices = stats.statusDistribution, isManga = isManga)
        }
    }

    if (stats.scoreDistribution.any { it.amount > 0 }) {
        item(key = "stats_score_distribution") {
            StatsSectionSpacer()
            ScoreDistributionSection(slices = stats.scoreDistribution, meanScore = meanScore)
        }
    }
}

@Composable
private fun StatsSectionSpacer() {
    Spacer(modifier = Modifier.height(24.dp))
}

/** Shared empty/error body for the tab (spinner-less states). */
@Composable
private fun StatsMessageBox(isError: Boolean, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(if (isError) R.string.media_stats_error else R.string.media_stats_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (isError && onRetry != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Rankings
// ---------------------------------------------------------------------------

@Composable
private fun RankingsSection(rankings: List<MediaRanking>, onRankingClick: (MediaRanking) -> Unit) {
    Column {
        SectionHeader(
            title = stringResource(R.string.media_stats_rankings),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        // Same pill design AND layout as the overview Information section: pills
        // spread across up to three horizontally-scrollable rows instead of a tall
        // wrap. Tapping opens Discover search with this ranking's scope preset.
        val perRow = (rankings.size + 2) / 3
        rankings.chunked(perRow).forEachIndexed { index, rowRankings ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(rowRankings, key = { it.context + it.season + it.year }) { ranking ->
                    RankingInfoPill(ranking = ranking, onClick = { onRankingClick(ranking) })
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Recent activity per day
// ---------------------------------------------------------------------------

@Composable
private fun ActivityPerDaySection(points: List<MediaTrendPoint>) {
    val expressive = LocalExpressiveTypography.current
    val peak = remember(points) { points.maxByOrNull { it.activity } }
    val maxActivity = (peak?.activity ?: 0).coerceAtLeast(1)
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val axisFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    // Tap a bar to inspect that day; the headline swaps from the peak to it.
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val selected = selectedIndex?.let(points::getOrNull)

    Column {
        SectionHeader(
            title = stringResource(R.string.media_stats_activity_per_day),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                if (peak != null) {
                    val headline = selected ?: peak
                    Text(
                        text = stringResource(
                            if (selected != null) R.string.media_stats_day_activity
                            else R.string.media_stats_peak_activity
                        ).uppercase(),
                        style = expressive.statLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatCompactNumber(headline.activity),
                            style = expressive.statNumericLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = headline.dateSeconds.toUtcDate().format(dateFormatter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // TalkBack gets the summary; the per-bar tap targets are visual-only.
                val chartDescription = peak?.let {
                    stringResource(
                        R.string.media_stats_activity_chart_a11y,
                        points.size,
                        formatCompactNumber(it.activity),
                        it.dateSeconds.toUtcDate().format(dateFormatter)
                    )
                }.orEmpty()

                // Label roughly five evenly-spaced days along the axis.
                val labelStep = remember(points) { (points.size + 4) / 5 }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clearAndSetSemantics { contentDescription = chartDescription },
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    points.forEachIndexed { index, point ->
                        val animatedHeight = remember { Animatable(0f) }
                        val targetFraction = point.activity / maxActivity.toFloat()
                        LaunchedEffect(targetFraction) {
                            animatedHeight.animateTo(
                                targetFraction,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                        // Dim everything but the inspected day while a selection is active.
                        val barColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (selectedIndex == null || selectedIndex == index) 1f else 0.35f
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedIndex = if (selectedIndex == index) null else index
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animatedHeight.value.coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(barColor)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(modifier = Modifier.height(14.dp)) {
                                if (index % labelStep == 0) {
                                    Text(
                                        text = point.dateSeconds.toUtcDate().format(axisFormatter),
                                        style = expressive.numericMono.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Long.toUtcDate(): LocalDate =
    Instant.ofEpochSecond(this).atOffset(ZoneOffset.UTC).toLocalDate()

// ---------------------------------------------------------------------------
// Airing progression (score / watchers line charts)
// ---------------------------------------------------------------------------

@Composable
private fun ScoreProgressionSection(points: List<MediaAiringTrend>) {
    val expressive = LocalExpressiveTypography.current
    val latest = points.last()
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    // Tap a point to inspect it; the headline swaps from the latest episode to it
    // and the byline becomes the day that snapshot was recorded.
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val selected = selectedIndex?.let(points::getOrNull)

    Column {
        SectionHeader(
            title = stringResource(R.string.media_stats_score_progression),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = (
                        if (selected != null) stringResource(R.string.media_stats_episode_eyebrow, selected.episode)
                        else stringResource(R.string.media_stats_latest_score)
                        ).uppercase(),
                    style = expressive.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = (selected ?: latest).averageScore?.toString().orEmpty(),
                        style = expressive.statNumericLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = selected?.dateSeconds?.toUtcDate()?.format(dateFormatter)
                            ?: stringResource(R.string.media_stats_episode_byline, latest.episode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                TrendLineChart(
                    values = points.map { (it.averageScore ?: 0).toFloat() },
                    xLabels = episodeAxisLabels(points),
                    lineColor = MaterialTheme.colorScheme.primary,
                    formatValue = { it.toInt().toString() },
                    contentDescription = stringResource(
                        R.string.media_stats_score_chart_a11y,
                        points.first().episode,
                        points.last().episode,
                        points.minOf { it.averageScore ?: 0 },
                        points.maxOf { it.averageScore ?: 0 }
                    ),
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }
    }
}

@Composable
private fun WatchersProgressionSection(points: List<MediaAiringTrend>) {
    val expressive = LocalExpressiveTypography.current
    val peak = points.maxBy { it.watching ?: 0 }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    // Same tap-to-inspect contract as the score chart above.
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val selected = selectedIndex?.let(points::getOrNull)

    Column {
        SectionHeader(
            title = stringResource(R.string.media_stats_watchers_progression),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = (
                        if (selected != null) stringResource(R.string.media_stats_episode_eyebrow, selected.episode)
                        else stringResource(R.string.media_stats_peak_watchers)
                        ).uppercase(),
                    style = expressive.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatCompactNumber((selected ?: peak).watching ?: 0),
                        style = expressive.statNumericLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = selected?.dateSeconds?.toUtcDate()?.format(dateFormatter)
                            ?: stringResource(R.string.media_stats_episode_byline, peak.episode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                TrendLineChart(
                    values = points.map { (it.watching ?: 0).toFloat() },
                    xLabels = episodeAxisLabels(points),
                    lineColor = MaterialTheme.colorScheme.tertiary,
                    formatValue = { formatCompactNumber(it.toInt()) },
                    contentDescription = stringResource(
                        R.string.media_stats_watchers_chart_a11y,
                        points.first().episode,
                        points.last().episode,
                        formatCompactNumber(peak.watching ?: 0),
                        peak.episode
                    ),
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }
    }
}

/** Every point carries its episode label; the chart thins whatever doesn't fit. */
private fun episodeAxisLabels(points: List<MediaAiringTrend>): List<String?> =
    points.map { it.episode.toString() }

/** Vertical range of a trend plot plus the three gridline values (max/mid/min). */
private data class TrendDomain(val min: Float, val max: Float, val gridValues: List<Float>)

private fun trendDomain(values: List<Float>): TrendDomain {
    var lo = values.min()
    var hi = values.max()
    if (lo == hi) {
        lo -= 1f
        hi += 1f
    }
    // Pad the domain so the line doesn't kiss the card edges; gridlines stay on
    // the true data extremes so their labels read as real values.
    val pad = (hi - lo) * 0.12f
    return TrendDomain(lo - pad, hi + pad, gridValues = listOf(hi, (lo + hi) / 2f, lo))
}

/**
 * Minimal smooth line chart: soft area fill, rounded stroke through every point,
 * three horizontal gridlines, and episode labels below. The y-axis labels stay
 * pinned while the plot itself scrolls horizontally whenever the points don't
 * fit at a readable spacing (long-runners: 25 four-digit episodes), landing on
 * the newest point. When everything fits, the plot spreads across the full
 * width and reveals left-to-right on first composition.
 *
 * Tapping the plot selects the nearest point (tap it again to clear); the
 * selection is marked with a dashed guideline and an emphasized dot, and the
 * caller swaps its headline to the selected value. TalkBack reads the chart as
 * one node via [contentDescription].
 */
@Composable
private fun TrendLineChart(
    values: List<Float>,
    xLabels: List<String?>,
    lineColor: Color,
    formatValue: (Float) -> String,
    contentDescription: String,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) return

    val expressive = LocalExpressiveTypography.current
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = expressive.numericMono.copy(fontSize = 10.sp, color = labelColor)
    val domain = remember(values) { trendDomain(values) }
    // Let the long-lived tap detector observe the latest selection/callback
    // without restarting on every tap.
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val bottomGutter = 20.dp

    BoxWithConstraints(modifier.clearAndSetSemantics { this.contentDescription = contentDescription }) {
        val yAxisWidth = 44.dp
        // Wide enough for a 4-digit episode label plus breathing room.
        val pointSpacing = 40.dp
        val edgeInset = 12.dp
        val viewportWidth = maxWidth - yAxisWidth
        val neededWidth = pointSpacing * (values.size - 1) + edgeInset * 2
        val scrollable = neededWidth > viewportWidth
        val plotWidth = if (scrollable) neededWidth else viewportWidth

        val scrollState = rememberScrollState()
        // Land on the newest episode when the plot overflows.
        LaunchedEffect(values, scrollable) {
            if (scrollable) scrollState.scrollTo(scrollState.maxValue)
        }
        // The reveal sweep only makes sense when the whole plot is visible.
        val reveal = remember { Animatable(0f) }
        LaunchedEffect(values, scrollable) {
            reveal.snapTo(if (scrollable) 1f else 0f)
            if (!scrollable) {
                reveal.animateTo(1f, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
            }
        }

        Row(Modifier.fillMaxSize()) {
            // Pinned y-axis labels, aligned to the same domain as the plot.
            Canvas(
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
            ) {
                val chartH = size.height - bottomGutter.toPx()
                domain.gridValues.forEach { gv ->
                    val gy = chartH * (1f - (gv - domain.min) / (domain.max - domain.min))
                    val layout = textMeasurer.measure(formatValue(gv), axisStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            size.width - layout.size.width - 8.dp.toPx(),
                            (gy - layout.size.height / 2f).coerceIn(0f, chartH - layout.size.height)
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (scrollable) Modifier.horizontalScroll(scrollState) else Modifier)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(plotWidth)
                        .fillMaxHeight()
                        .pointerInput(values) {
                            detectTapGestures { offset ->
                                val inset = edgeInset.toPx()
                                val innerW = size.width - inset * 2
                                if (innerW <= 0f) return@detectTapGestures
                                val index = ((offset.x - inset) / innerW * (values.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, values.lastIndex)
                                currentOnSelect(if (index == currentSelected) null else index)
                            }
                        }
                ) {
                    val chartH = size.height - bottomGutter.toPx()
                    val inset = edgeInset.toPx()
                    val innerW = size.width - inset * 2
                    val dotRadius = (if (scrollable || values.size <= 20) 3.5.dp else 2.5.dp).toPx()

                    fun x(index: Int) = inset + innerW * index / (values.size - 1)
                    fun y(value: Float) =
                        chartH * (1f - (value - domain.min) / (domain.max - domain.min))

                    domain.gridValues.forEach { gv ->
                        val gy = y(gv)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // X labels, drawn right-to-left so the newest point always keeps
                    // its label; anything that would overlap an already-drawn label
                    // is skipped (this is what thins them in the fitted layout).
                    var lastLabelStart = Float.POSITIVE_INFINITY
                    for (index in xLabels.indices.reversed()) {
                        val label = xLabels[index] ?: continue
                        val layout = textMeasurer.measure(label, axisStyle)
                        val left = (x(index) - layout.size.width / 2f)
                            .coerceIn(0f, size.width - layout.size.width)
                        if (left + layout.size.width > lastLabelStart - 6.dp.toPx()) continue
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(left, size.height - layout.size.height)
                        )
                        lastLabelStart = left
                    }

                    // Smooth path through the points (horizontal-midpoint cubics — no
                    // vertical overshoot).
                    val linePath = Path().apply {
                        moveTo(x(0), y(values[0]))
                        for (i in 1 until values.size) {
                            val midX = (x(i - 1) + x(i)) / 2f
                            cubicTo(midX, y(values[i - 1]), midX, y(values[i]), x(i), y(values[i]))
                        }
                    }
                    val areaPath = Path().apply {
                        addPath(linePath)
                        lineTo(x(values.lastIndex), chartH)
                        lineTo(x(0), chartH)
                        close()
                    }

                    clipRect(right = size.width * reveal.value) {
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
                                startY = 0f,
                                endY = chartH
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        values.forEachIndexed { index, value ->
                            drawCircle(
                                color = lineColor,
                                radius = dotRadius,
                                center = Offset(x(index), y(value))
                            )
                        }
                    }

                    // Selection marker: dashed guideline through the point plus a
                    // soft halo behind an emphasized dot. Drawn outside the reveal
                    // clip — selection is user-initiated, after the sweep.
                    selectedIndex?.let { sel ->
                        if (sel <= values.lastIndex) {
                            val center = Offset(x(sel), y(values[sel]))
                            drawLine(
                                color = lineColor.copy(alpha = 0.45f),
                                start = Offset(center.x, 0f),
                                end = Offset(center.x, chartH),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )
                            drawCircle(
                                color = lineColor.copy(alpha = 0.22f),
                                radius = dotRadius + 7.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = lineColor,
                                radius = dotRadius + 2.dp.toPx(),
                                center = center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Status distribution
// ---------------------------------------------------------------------------

private val STATUS_ORDER = listOf("CURRENT", "PLANNING", "COMPLETED", "DROPPED", "PAUSED", "REPEATING")

@Composable
private fun StatusDistributionSection(slices: List<MediaStatusSlice>, isManga: Boolean) {
    val expressive = LocalExpressiveTypography.current
    val ordered = remember(slices) {
        slices.filter { it.amount > 0 }
            .sortedBy { STATUS_ORDER.indexOf(it.status).let { i -> if (i == -1) STATUS_ORDER.size else i } }
    }
    val total = remember(ordered) { ordered.sumOf { it.amount } }
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.error
    )
    val groupedFormat = rememberGroupedNumberFormat()

    Column {
        SectionHeader(
            title = stringResource(R.string.media_stats_status_distribution),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.media_stats_total_entries).uppercase(),
                    style = expressive.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = groupedFormat.format(total),
                    style = expressive.statNumericMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))

                // Proportional segmented bar. Decorative for TalkBack — the legend
                // below carries the same numbers.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clearAndSetSemantics { },
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ordered.forEachIndexed { index, slice ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                // Floor keeps slivers tappable-visible without distorting the read.
                                .weight((slice.amount / total.toFloat()).coerceAtLeast(0.012f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette[index % palette.size])
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ordered.forEachIndexed { index, slice ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(palette[index % palette.size], CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = mediaStatusDisplayName(slice.status, isManga),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(slice.amount * 100f / total).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = groupedFormat.format(slice.amount),
                                style = expressive.numericMono,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun mediaStatusDisplayName(status: String, isManga: Boolean): String = when (status) {
    "CURRENT" -> stringResource(if (isManga) R.string.status_reading else R.string.status_watching)
    "COMPLETED" -> stringResource(R.string.status_completed)
    "PLANNING" -> stringResource(R.string.status_planning)
    "PAUSED" -> stringResource(R.string.status_paused)
    "DROPPED" -> stringResource(R.string.status_dropped)
    "REPEATING" -> stringResource(if (isManga) R.string.status_rereading else R.string.status_rewatching)
    else -> status.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
private fun rememberGroupedNumberFormat(): NumberFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { NumberFormat.getIntegerInstance(locale) }
}

// ---------------------------------------------------------------------------
// Score distribution
// ---------------------------------------------------------------------------

@Composable
private fun ScoreDistributionSection(slices: List<MediaScoreSlice>, meanScore: Int?) {
    val expressive = LocalExpressiveTypography.current
    // The API omits empty buckets; render the full 10..100 axis regardless.
    val buckets = remember(slices) {
        (10..100 step 10).map { score ->
            MediaScoreSlice(score = score, amount = slices.firstOrNull { it.score == score }?.amount ?: 0)
        }
    }
    val maxAmount = remember(buckets) { buckets.maxOf { it.amount }.coerceAtLeast(1) }
    val total = remember(buckets) { buckets.sumOf { it.amount }.coerceAtLeast(1) }
    // Tap a bar to inspect its bucket; the headline swaps from the mean score to it.
    var selectedIndex by remember(slices) { mutableStateOf<Int?>(null) }
    val selected = selectedIndex?.let(buckets::getOrNull)
    val groupedFormat = rememberGroupedNumberFormat()
    // TalkBack summary for the histogram; per-bucket labels stay visual-only.
    val topBucket = remember(buckets) { buckets.maxBy { it.amount } }
    val chartDescription = stringResource(
        R.string.media_stats_score_dist_a11y,
        topBucket.score,
        formatCompactNumber(topBucket.amount)
    )

    Column {
        SectionHeader(
            title = stringResource(R.string.media_stats_score_distribution),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val onContainer = MaterialTheme.colorScheme.onTertiaryContainer
            Column(Modifier.padding(24.dp)) {
                if (selected != null) {
                    Text(
                        text = stringResource(R.string.media_stats_score_bucket_eyebrow, selected.score).uppercase(),
                        style = expressive.statLabel,
                        color = onContainer.copy(alpha = 0.7f)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = groupedFormat.format(selected.amount),
                            style = expressive.statNumericMedium,
                            color = onContainer
                        )
                        Text(
                            text = stringResource(
                                R.string.media_stats_percent_of_ratings,
                                (selected.amount * 100f / total).toInt()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp, start = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                } else if (meanScore != null && meanScore > 0) {
                    Text(
                        text = stringResource(R.string.statistics_mean_score).uppercase(),
                        style = expressive.statLabel,
                        color = onContainer.copy(alpha = 0.7f)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = meanScore.toString(),
                            style = expressive.statNumericMedium,
                            color = onContainer
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.titleMedium,
                            color = onContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clearAndSetSemantics { contentDescription = chartDescription },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    buckets.forEachIndexed { index, bucket ->
                        val animatedHeight = remember { Animatable(0f) }
                        val targetFraction = bucket.amount / maxAmount.toFloat()
                        LaunchedEffect(targetFraction) {
                            animatedHeight.animateTo(
                                targetFraction,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                        // Dim everything but the inspected bucket while a selection is active.
                        val baseColor = when {
                            bucket.amount == 0 -> onContainer.copy(alpha = 0.2f)
                            bucket.score >= 80 -> onContainer
                            bucket.score >= 50 -> onContainer.copy(alpha = 0.65f)
                            else -> onContainer.copy(alpha = 0.4f)
                        }
                        val barColor = if (selectedIndex != null && selectedIndex != index) {
                            baseColor.copy(alpha = baseColor.alpha * 0.35f)
                        } else baseColor
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedIndex = if (selectedIndex == index) null else index
                                },
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (bucket.amount > 0) {
                                Text(
                                    text = formatCompactNumber(bucket.amount),
                                    style = expressive.numericMono.copy(fontSize = 9.sp),
                                    color = onContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .weight(1f, fill = false)
                                    .fillMaxHeight(
                                        if (bucket.amount > 0) animatedHeight.value.coerceAtLeast(0.02f) else 0.02f
                                    )
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(barColor)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = bucket.score.toString(),
                                style = expressive.numericMono,
                                color = if (bucket.amount > 0) onContainer else onContainer.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }
        }
    }
}

// region Previews

private val previewMediaStats = MediaStats(
    rankings = listOf(
        MediaRanking(1, MediaRankingType.RATED, null, null, allTime = true, context = "highest rated all time"),
        MediaRanking(3, MediaRankingType.POPULAR, null, null, allTime = true, context = "most popular all time"),
        MediaRanking(1, MediaRankingType.RATED, 2024, "SPRING", allTime = false, context = "highest rated"),
        MediaRanking(2, MediaRankingType.POPULAR, 2024, null, allTime = false, context = "most popular")
    ),
    recentActivity = (0 until 25).map { day ->
        MediaTrendPoint(
            dateSeconds = 1_700_000_000L + day * 86_400L,
            activity = 120 + ((day * 37) % 240)
        )
    },
    airingProgression = (1..13).map { ep ->
        MediaAiringTrend(
            episode = ep,
            dateSeconds = 1_700_000_000L + ep * 7 * 86_400L,
            averageScore = 78 + (ep * 7) % 12,
            watching = 9_000 + ep * 1_450
        )
    },
    scoreDistribution = listOf(
        MediaScoreSlice(10, 1_200), MediaScoreSlice(20, 900), MediaScoreSlice(30, 1_400),
        MediaScoreSlice(40, 2_300), MediaScoreSlice(50, 6_100), MediaScoreSlice(60, 14_000),
        MediaScoreSlice(70, 46_000), MediaScoreSlice(80, 92_000), MediaScoreSlice(90, 88_000),
        MediaScoreSlice(100, 41_000)
    ),
    statusDistribution = listOf(
        MediaStatusSlice("CURRENT", 244_000), MediaStatusSlice("PLANNING", 320_000),
        MediaStatusSlice("COMPLETED", 1_150_000), MediaStatusSlice("DROPPED", 31_000),
        MediaStatusSlice("PAUSED", 58_000)
    )
)

@Preview(showBackground = true, name = "Rankings — mixed scopes", widthDp = 380)
@Composable
private fun RankingsPreview() {
    StatPreviewSurface(isDark = false) {
        RankingsSection(rankings = previewMediaStats.rankings, onRankingClick = {})
    }
}

@Preview(showBackground = true, name = "Activity per day", widthDp = 380)
@Composable
private fun ActivityPreview() {
    StatPreviewSurface(isDark = false) {
        ActivityPerDaySection(points = previewMediaStats.recentActivity)
    }
}

@Preview(showBackground = true, name = "Score progression", widthDp = 380)
@Composable
private fun ScoreProgressionPreview() {
    StatPreviewSurface(isDark = false) {
        ScoreProgressionSection(points = previewMediaStats.airingProgression)
    }
}

@Preview(showBackground = true, name = "Watchers progression — dark", widthDp = 380)
@Composable
private fun WatchersProgressionDarkPreview() {
    StatPreviewSurface(isDark = true) {
        WatchersProgressionSection(points = previewMediaStats.airingProgression)
    }
}

@Preview(showBackground = true, name = "Status distribution", widthDp = 380)
@Composable
private fun StatusDistributionPreview() {
    StatPreviewSurface(isDark = false) {
        StatusDistributionSection(slices = previewMediaStats.statusDistribution, isManga = false)
    }
}

@Preview(showBackground = true, name = "Score distribution", widthDp = 380)
@Composable
private fun ScoreDistributionPreview() {
    StatPreviewSurface(isDark = false) {
        ScoreDistributionSection(slices = previewMediaStats.scoreDistribution, meanScore = 82)
    }
}

// endregion
