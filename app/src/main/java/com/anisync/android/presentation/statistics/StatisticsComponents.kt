package com.anisync.android.presentation.statistics

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.CountryStat
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.StaffStat
import com.anisync.android.domain.StudioStat
import com.anisync.android.domain.TagStat
import com.anisync.android.domain.VoiceActorStat
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.LengthUiModel
import com.anisync.android.presentation.profile.ScoreUiModel
import com.anisync.android.presentation.profile.StatusUiModel
import com.anisync.android.presentation.profile.YearUiModel
import com.anisync.android.presentation.util.formatDecimal
import com.anisync.android.ui.theme.LocalExpressiveTypography

data class EditorialStat(
    val value: String,
    val label: String,
    val icon: ImageVector? = null
)

/**
 * MD3 Expressive HeroDashboard.
 *
 * Editorial layout (per m3.material.io/styles/typography/editorial-treatments):
 *  - eyebrow label (caps, wide tracking, low size)
 *  - mixed-weight baseline-aligned row: huge W900 numeric + W400 unit
 *  - optional editorial-lead sentence ("≈ 42 days of your life")
 *  - divider
 *  - up to three sub-stats using tabular figures
 */
@Composable
fun HeroDashboard(
    primaryValue: String,
    primaryUnit: String,
    primaryLabel: String,
    secondaryRow: List<EditorialStat>,
    accentText: String? = null,
    modifier: Modifier = Modifier
) {
    val expressive = LocalExpressiveTypography.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = 28.dp,
                bottom = 24.dp
            )
        ) {
            Text(
                text = primaryLabel.uppercase(),
                style = expressive.statLabel,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = primaryValue,
                    style = expressive.heroNumeric,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = primaryUnit,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            accentText?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = expressive.editorialLead,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
            if (secondaryRow.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    secondaryRow.forEach { stat ->
                        EditorialStatBlock(stat, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialStatBlock(stat: EditorialStat, modifier: Modifier = Modifier) {
    val expressive = LocalExpressiveTypography.current
    Column(modifier) {
        if (stat.icon != null) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = stat.value,
            style = expressive.statNumericMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1
        )
        Text(
            text = stat.label.uppercase(),
            style = expressive.statLabel,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            maxLines = 2
        )
    }
}

// endregion

// region Score histogram

@Composable
fun ScoreHistogramSection(
    scores: List<ScoreUiModel>,
    meanScore: Double
) {
    val expressive = LocalExpressiveTypography.current
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_score_distribution),
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.statistics_mean_score).uppercase(),
                            style = expressive.statLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDecimal(meanScore),
                            style = expressive.statNumericMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    scores.forEach { item ->
                        val animatedHeight = remember { Animatable(0f) }

                        LaunchedEffect(item.heightFraction) {
                            animatedHeight.animateTo(
                                item.heightFraction,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .weight(1f, fill = false)
                                    .fillMaxHeight(if (item.count > 0) animatedHeight.value else 0.02f)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(
                                        when {
                                            item.count == 0 -> MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.25f
                                            )

                                            item.normalizedScore >= 0.8f -> MaterialTheme.colorScheme.primary
                                            item.normalizedScore >= 0.5f -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.tertiary
                                        }
                                    )
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = item.label,
                                style = expressive.numericMono,
                                color = if (item.count > 0)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.outline,
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

// endregion

// region Release-year histogram + peak callout

@Composable
fun ReleaseYearsHistogramSection(years: List<YearUiModel>) {
    val expressive = LocalExpressiveTypography.current
    val peakYear = years.maxByOrNull { it.count }
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_by_year),
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
                if (peakYear != null && peakYear.count > 0) {
                    Text(
                        text = stringResource(R.string.statistics_peak_year).uppercase(),
                        style = expressive.statLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = peakYear.year.toString(),
                            style = expressive.statNumericLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${peakYear.count} titles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    years.forEach { stat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (stat.count > 0) {
                                Text(
                                    text = stat.count.toString(),
                                    style = expressive.numericMono.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(80.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(stat.heightFraction.coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "'${stat.year.toString().takeLast(2)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// endregion

// region Start-year + Year comparison

@Composable
fun YearComparisonSection(
    release: List<YearUiModel>,
    start: List<YearUiModel>
) {
    val expressive = LocalExpressiveTypography.current
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_start_year),
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
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    LegendDot(MaterialTheme.colorScheme.primary, "STARTED")
                    LegendDot(MaterialTheme.colorScheme.tertiary, "AIRED")
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxLen = maxOf(release.size, start.size)
                    val releasePadded = release.padTo(maxLen)
                    val startPadded = start.padTo(maxLen)
                    for (i in 0 until maxLen) {
                        val s = startPadded.getOrNull(i)
                        val r = releasePadded.getOrNull(i)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.height(80.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .fillMaxHeight(
                                            (s?.heightFraction ?: 0f).coerceAtLeast(0.02f)
                                        )
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .fillMaxHeight(
                                            (r?.heightFraction ?: 0f).coerceAtLeast(0.02f)
                                        )
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "'${(s?.year ?: r?.year ?: 0).toString().takeLast(2)}",
                                style = expressive.numericMono.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun <T> List<T>.padTo(size: Int): List<T?> =
    (0 until size).map { i -> getOrNull(i) }

@Composable
private fun LegendDot(color: Color, label: String) {
    val expressive = LocalExpressiveTypography.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = expressive.statLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// endregion

// region Time-spent breakdown

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeSpentBreakdown(minutesWatched: Int) {
    val expressive = LocalExpressiveTypography.current
    val totalMinutes = minutesWatched.coerceAtLeast(0)
    val days = totalMinutes / 1440
    val hours = (totalMinutes % 1440) / 60
    val mins = totalMinutes % 60

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_time_breakdown),
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
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimeUnitBlock(days.toString(), "DAYS")
                TimeUnitBlock(hours.toString(), "HOURS")
                TimeUnitBlock(mins.toString(), "MIN")
            }
        }
    }
}

/**
 * Manga-side variant: shows chapter + volume breakdown rather than time.
 * Editorial mixed-weight FlowRow same as TimeSpentBreakdown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadVolumeBreakdown(chaptersRead: Int, volumesRead: Int) {
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_time_breakdown),
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
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimeUnitBlock(chaptersRead.toString(), "CHAPTERS")
                TimeUnitBlock(volumesRead.toString(), "VOLUMES")
            }
        }
    }
}

@Composable
private fun TimeUnitBlock(value: String, label: String) {
    val expressive = LocalExpressiveTypography.current
    Column {
        Text(
            text = value,
            style = expressive.statNumericLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = expressive.statLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// endregion

// region Status donut

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun StatusDistributionDonut(
    statuses: List<StatusUiModel>,
    isManga: Boolean = false
) {
    val expressive = LocalExpressiveTypography.current
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )
    val total = statuses.sumOf { it.count }

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_status_breakdown),
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        val stroke = 22.dp.toPx()
                        val pad = stroke / 2f
                        var sweepStart = -90f
                        statuses.forEachIndexed { idx, s ->
                            val sweep = s.fraction * 360f
                            val color = palette[idx % palette.size]
                            drawArc(
                                color = color,
                                startAngle = sweepStart,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(pad, pad),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width - stroke,
                                    size.height - stroke
                                ),
                                style = Stroke(width = stroke, cap = StrokeCap.Butt)
                            )
                            sweepStart += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = total.toString(),
                            style = expressive.statNumericLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ENTRIES",
                            style = expressive.statLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statuses.forEachIndexed { idx, s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(palette[idx % palette.size], CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = statusDisplayName(s.status, isManga),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = s.count.toString(),
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
private fun statusDisplayName(status: String, isManga: Boolean): String = when (status) {
    "CURRENT" -> stringResource(if (isManga) R.string.status_reading else R.string.status_watching)
    "COMPLETED" -> stringResource(R.string.status_completed)
    "PLANNING" -> stringResource(R.string.status_planning)
    "PAUSED" -> stringResource(R.string.status_paused)
    "DROPPED" -> stringResource(R.string.status_dropped)
    "REPEATING" -> stringResource(if (isManga) R.string.status_rereading else R.string.status_rewatching)
    else -> status.lowercase().replaceFirstChar { it.uppercase() }
}

// endregion

// region Tag cloud

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagCloudSection(tags: List<TagStat>) {
    if (tags.isEmpty()) return
    val expressive = LocalExpressiveTypography.current
    val maxCount = tags.maxOf { it.count }.coerceAtLeast(1)

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_top_tags),
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
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val frequency = tag.count.toFloat() / maxCount
                    val style = when {
                        frequency > 0.66f -> MaterialTheme.typography.headlineSmall
                        frequency > 0.33f -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.bodyMedium
                    }
                    val containerColor = when {
                        frequency > 0.66f -> MaterialTheme.colorScheme.primaryContainer
                        frequency > 0.33f -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val contentColor = when {
                        frequency > 0.66f -> MaterialTheme.colorScheme.onPrimaryContainer
                        frequency > 0.33f -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(containerColor)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag.name,
                            style = style,
                            color = contentColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tag.count.toString(),
                            style = expressive.numericMono,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// endregion

// region Standard deviation card

@Composable
fun StandardDeviationCard(standardDeviation: Double, meanScore: Double) {
    if (standardDeviation <= 0.0) return
    val expressive = LocalExpressiveTypography.current
    val interpretation = when {
        standardDeviation < 1.0 -> stringResource(R.string.statistics_taste_focused)
        standardDeviation < 2.0 -> stringResource(R.string.statistics_taste_balanced)
        else -> stringResource(R.string.statistics_taste_eclectic)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.statistics_std_dev).uppercase(),
                style = expressive.statLabel,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatDecimal(standardDeviation),
                    style = expressive.statNumericLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "vs mean ${formatDecimal(meanScore)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = interpretation,
                style = expressive.editorialLead,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// endregion

// region Formats list

@Composable
fun FormatsSection(formats: List<FormatStat>) {
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_formats),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                formats.forEachIndexed { index, format ->
                    FormatRow(format)
                    if (index < formats.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatRow(format: FormatStat) {
    val expressive = LocalExpressiveTypography.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFormatIcon(format.format),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatDisplayName(format.format),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = format.count.toString(),
                style = expressive.statNumericMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            RatingBadge(meanScore = format.meanScore, size = RatingBadgeSize.Small)
        }
    }
}

// endregion

// region Episode-length distribution

@Composable
fun EpisodeLengthDistributionSection(
    lengths: List<LengthUiModel>,
    title: String = stringResource(R.string.statistics_length_distribution)
) {
    if (lengths.isEmpty()) return
    val expressive = LocalExpressiveTypography.current
    Column {
        SectionHeader(
            title = title,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(24.dp),
                horizontalArrangement = if (lengths.size <= 2) Arrangement.spacedBy(
                    24.dp,
                    Alignment.CenterHorizontally
                ) else Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                lengths.forEach { len ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = if (lengths.size <= 2) Modifier.width(64.dp) else Modifier.weight(
                            1f
                        )
                    ) {
                        Text(
                            text = len.count.toString(),
                            style = expressive.numericMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .widthIn(max = 48.dp)
                                .fillMaxWidth(0.7f)
                                .height(90.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(len.heightFraction.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = len.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// endregion

// region Horizontal lists (genres / studios / voice actors / staff)

@Composable
fun <T> HorizontalStatsSection(
    title: String,
    items: List<T>,
    key: ((T) -> Any)? = null,
    onActionClick: (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    Column {
        SectionHeader(
            title = title,
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            onActionClick = onActionClick
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = key) { item ->
                itemContent(item)
            }
        }
    }
}

@Composable
fun GenreCardModern(genre: GenreStat) {
    val expressive = LocalExpressiveTypography.current
    val secondary = MaterialTheme.colorScheme.secondaryContainer
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val gradientBrush = remember(secondary, surfaceHigh) {
        Brush.linearGradient(colors = listOf(secondary, surfaceHigh))
    }

    Card(
        modifier = Modifier.width(176.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = genre.count.toString(),
                        style = expressive.statNumericMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (genre.meanScore > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = formatDecimal(genre.meanScore),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = genre.genre,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${genre.count} entries",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun StudioCardModern(studio: StudioStat) {
    val expressive = LocalExpressiveTypography.current
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(StatPersonCardHeight),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(StatPersonAvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = studio.studioName.take(1),
                    style = expressive.statNumericMedium.copy(fontSize = 32.sp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = studio.studioName,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${studio.count} items",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val StatPersonCardHeight = 196.dp
private val StatPersonAvatarSize = 96.dp

@Composable
fun VoiceActorCardModern(va: VoiceActorStat) {
    PersonCard(
        name = va.name,
        imageUrl = va.imageUrl,
        countLabel = "${va.count} roles"
    )
}

@Composable
fun StaffCardModern(staff: StaffStat) {
    PersonCard(
        name = staff.name,
        imageUrl = staff.imageUrl,
        countLabel = "${staff.count} works"
    )
}

@Composable
private fun PersonCard(name: String, imageUrl: String?, countLabel: String) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(StatPersonCardHeight),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(StatPersonAvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(StatPersonAvatarSize)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// endregion

// region Country distribution

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountryDistributionRow(countries: List<CountryStat>) {
    if (countries.isEmpty()) return
    val expressive = LocalExpressiveTypography.current
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_country_distribution),
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
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                countries.forEach { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = c.countryCode,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = c.count.toString(),
                            style = expressive.numericMono,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// endregion

// region Rating badge + helpers

enum class RatingBadgeSize { Default, Small }

@Composable
fun RatingBadge(meanScore: Float, size: RatingBadgeSize = RatingBadgeSize.Default) {
    val hasRating = meanScore > 0.0f
    val (iconSize, textStyle, verticalPadding) = when (size) {
        RatingBadgeSize.Default -> Triple(12.dp, MaterialTheme.typography.labelMedium, 4.dp)
        RatingBadgeSize.Small -> Triple(10.dp, MaterialTheme.typography.labelSmall, 2.dp)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = verticalPadding)
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = if (hasRating) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (hasRating) formatDecimal(meanScore) else stringResource(R.string.not_available),
            style = textStyle,
            color = if (hasRating) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

fun formatDisplayName(format: String): String = when (format) {
    "TV" -> "TV Series"
    "TV_SHORT" -> "Short"
    "MOVIE" -> "Movie"
    "SPECIAL" -> "Special"
    "OVA" -> "OVA"
    "ONA" -> "ONA"
    "MUSIC" -> "Music"
    "MANGA" -> "Manga"
    "NOVEL" -> "Novel"
    "ONE_SHOT" -> "One Shot"
    else -> format.replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}

fun getFormatIcon(format: String): ImageVector = when (format) {
    "TV", "TV_SHORT" -> Icons.Default.Tv
    "MOVIE" -> Icons.Default.Movie
    "SPECIAL", "OVA", "ONA" -> Icons.Default.Videocam
    "MUSIC" -> Icons.Default.MusicNote
    "MANGA", "NOVEL", "ONE_SHOT" -> Icons.Default.Book
    else -> Icons.Default.PlayArrow
}