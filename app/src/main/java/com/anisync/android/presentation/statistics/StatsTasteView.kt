package com.anisync.android.presentation.statistics

import android.icu.text.MessageFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.TagStat
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.formatOneDecimal
import com.anisync.android.domain.stats.ScoreSummary
import com.anisync.android.domain.stats.TasteSpread
import com.anisync.android.domain.stats.tasteSpread
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.presentation.profile.GenreSort
import com.anisync.android.presentation.profile.ScoreUiModel
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.genreColor
import com.anisync.android.ui.theme.lineBox
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** The Scores card: twenty steps of the spread with the mean marked, then mean, median and most given. */
@Composable
fun TasteScoresCard(
    scores: List<ScoreUiModel>,
    summary: ScoreSummary,
    meanScore: Double,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.stats_scores_title),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(
                    R.string.stats_scores_scored,
                    formatExactCount(summary.scored, locale),
                    formatExactCount(totalCount, locale)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        }
        ScoreHistogram(scores, mostGiven = summary.mostGiven, meanScore = meanScore)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoreCapsule(
                value = formatOneDecimal(meanScore, locale),
                label = stringResource(R.string.stats_score_mean),
                container = scheme.surfaceContainerHigh,
                content = scheme.onSurface,
                labelColor = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            ScoreCapsule(
                value = summary.median?.toString().orEmpty(),
                label = stringResource(R.string.stats_score_median),
                container = scheme.surfaceContainerHigh,
                content = scheme.onSurface,
                labelColor = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            ScoreCapsule(
                value = summary.mostGiven?.toString().orEmpty(),
                label = stringResource(R.string.stats_score_most_given),
                container = scheme.primaryContainer,
                content = scheme.onPrimaryContainer,
                labelColor = scheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * A bar for each step the list was scored in on a 128dp baseline, the most given one lit, a line
 * and a pill at the mean, and the first, middle, most given and last steps labelled underneath.
 * Bars keep the frame's 14dp when a list has fewer steps than twenty.
 */
@Composable
private fun ScoreHistogram(scores: List<ScoreUiModel>, mostGiven: Int?, meanScore: Double) {
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(HistogramHeight)
    ) {
        val steps = scores.size.coerceAtLeast(1)
        val slot = (maxWidth - BarsStart - BarsEnd - BarGap * (steps - 1)) / steps
        val barWidth = slot.coerceAtMost(BarMaxWidth)
        val stride = slot + BarGap
        fun centre(index: Float): Dp = BarsStart + stride * index + slot / 2

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarsHeight)
                .padding(start = BarsStart, end = BarsEnd),
            horizontalArrangement = Arrangement.spacedBy(BarGap),
            verticalAlignment = Alignment.Bottom
        ) {
            scores.forEach { bucket ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .width(barWidth)
                            .height((BarsMax * bucket.heightFraction).roundToInt().coerceAtLeast(BarsMin).dp)
                            .clip(RoundedCornerShape(barWidth / 2))
                            .background(scheme.primary.copy(alpha = if (bucket.score == mostGiven) 1f else 0.38f))
                    )
                }
            }
        }

        if (scores.any { it.count > 0 }) {
            val first = scores.first().score
            val span = (scores.last().score - first).coerceAtLeast(1)
            val meanIndex = ((meanScore - first) / span * scores.lastIndex).toFloat()
            val meanX = centre(meanIndex.coerceIn(0f, scores.lastIndex.toFloat()))
            Box(
                Modifier
                    .padding(top = MeanLineTop)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(constraints.maxWidth, placeable.height) {
                            placeable.place((meanX - 1.dp).roundToPx(), 0)
                        }
                    }
                    .width(2.dp)
                    .height(BarsHeight - MeanLineTop)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.6f))
            )
            Text(
                text = stringResource(R.string.stats_scores_mean_pill, formatOneDecimal(meanScore, locale)),
                style = labelStyle,
                color = scheme.inverseOnSurface,
                maxLines = 1,
                modifier = Modifier
                    .centredAt(meanX)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.inverseSurface)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        val lastIndex = scores.lastIndex
        val modeIndex = scores.indexOfFirst { it.score == mostGiven }
        val labelled = listOf(0, lastIndex / 2, lastIndex)
            .filter { index -> modeIndex < 0 || index == modeIndex || abs(index - modeIndex) > 1 }
            .plus(modeIndex)
            .filter { it in scores.indices }
            .distinct()
        labelled.forEach { index ->
            Text(
                text = scores[index].label,
                style = labelStyle,
                color = if (index == modeIndex) scheme.primary else scheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = LabelsTop)
                    .centredAt(centre(index.toFloat()))
            )
        }
    }
}

/** Places a node so its centre sits at [x], kept inside the parent's width. */
private fun Modifier.centredAt(x: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val left = (x.roundToPx() - placeable.width / 2).coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
    layout(constraints.maxWidth, placeable.height) { placeable.place(left, 0) }
}

private val HistogramHeight = 150.dp
private val BarsHeight = 128.dp
private const val BarsMax = 100f
private const val BarsMin = 6
private val BarsStart = 2.dp
private val BarsEnd = 7.dp
private val BarMaxWidth = 14.dp
private val BarGap = 5.dp
private val MeanLineTop = 8.dp
private val LabelsTop = 134.dp

@Composable
private fun ScoreCapsule(
    value: String,
    label: String,
    container: Color,
    content: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val style = LocalExpressiveTypography.current.titleWide.style(22.sp, 28.sp)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = value, style = style, color = content, maxLines = 1, modifier = Modifier.lineBox(style))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = labelColor, maxLines = 1)
    }
}

/** The spread in a word, a sentence on what it means for these scores, and σ itself. */
@Composable
fun TasteSpreadCard(
    standardDeviation: Double,
    scores: List<ScoreUiModel>,
    meanScore: Double,
    modifier: Modifier = Modifier,
) {
    val spread = tasteSpread(standardDeviation) ?: return
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val wordStyle = LocalExpressiveTypography.current.heroWide.style(28.sp, 36.sp, (-0.5).sp)
    val sigmaStyle = LocalExpressiveTypography.current.titleWide.style(22.sp, 28.sp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(spread.labelRes()),
                style = wordStyle,
                color = scheme.tertiary,
                maxLines = 1,
                modifier = Modifier.lineBox(wordStyle)
            )
            Text(
                text = spreadSentence(spread, standardDeviation, scores, meanScore, locale),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.tertiaryContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.stats_sigma),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = scheme.onTertiaryContainer
            )
            Text(
                text = formatOneDecimal(standardDeviation, locale),
                style = sigmaStyle,
                color = scheme.onTertiaryContainer,
                maxLines = 1,
                modifier = Modifier.lineBox(sigmaStyle)
            )
        }
    }
}

@Composable
private fun spreadSentence(
    spread: TasteSpread,
    standardDeviation: Double,
    scores: List<ScoreUiModel>,
    meanScore: Double,
    locale: Locale,
): String {
    val points = standardDeviation.roundToInt()
    val mean = formatOneDecimal(meanScore, locale)
    return when (spread) {
        TasteSpread.FOCUSED -> pluralStringResourceOrDefault(R.plurals.stats_spread_focused_body, points, points, mean)
        TasteSpread.BALANCED -> pluralStringResourceOrDefault(R.plurals.stats_spread_balanced_body, points, points, mean)
        TasteSpread.ECLECTIC -> {
            val low = scores.firstOrNull { it.count > 0 }
            val high = scores.lastOrNull { it.count > 0 }
            if (low == null || high == null || low.score == high.score) {
                pluralStringResourceOrDefault(R.plurals.stats_spread_balanced_body, points, points, mean)
            } else {
                val spellOut = remember(locale) { MessageFormat("{0,spellout}", locale) }
                fun spelled(count: Int): String = spellOut.format(arrayOf(count))
                val lowPart = pluralStringResourceOrDefault(R.plurals.stats_spread_count, low.count, spelled(low.count), low.label)
                val highPart = if (high.score == PERFECT) {
                    pluralStringResourceOrDefault(R.plurals.stats_spread_perfect, high.count, spelled(high.count))
                } else {
                    pluralStringResourceOrDefault(R.plurals.stats_spread_count, high.count, spelled(high.count), high.label)
                }
                stringResource(R.string.stats_spread_eclectic_body, lowPart, highPart)
            }
        }
    }
}

private const val PERFECT = 100

/**
 * Genres by titles, hours (chapters for manga) or score, each bar in its genre's colours, then the
 * three best rated genres among those with enough titles to mean something.
 */
@Composable
fun GenreBoard(
    genres: List<GenreStat>,
    sort: GenreSort,
    onSortChange: (GenreSort) -> Unit,
    isManga: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val metric: (GenreStat) -> Float = when (sort) {
        GenreSort.TITLES -> { genre -> genre.count.toFloat() }
        GenreSort.HOURS -> { genre -> genre.hoursWatched }
        GenreSort.SCORE -> { genre -> genre.meanScore }
    }
    val rows = genres.sortedByDescending(metric).take(GENRE_ROWS)
    val max = rows.maxOfOrNull(metric)?.takeIf { it > 0f } ?: 1f

    // The sort buttons draw 44dp inside a 48dp touch row, so the padding above and the gap below
    // are 2dp short of the 16 and 12 the frame draws.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 16.dp)
    ) {
        SegmentedTabGroup(
            options = GenreSort.entries,
            selected = sort,
            onSelect = onSortChange,
            label = { option ->
                stringResource(
                    when (option) {
                        GenreSort.TITLES -> R.string.stats_genre_sort_titles
                        GenreSort.HOURS -> if (isManga) R.string.stats_genre_sort_chapters else R.string.stats_genre_sort_hours
                        GenreSort.SCORE -> R.string.stats_genre_sort_score
                    }
                )
            },
            icon = { option ->
                when (option) {
                    GenreSort.TITLES -> Icons.Default.Tv
                    GenreSort.HOURS -> if (isManga) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Schedule
                    GenreSort.SCORE -> Icons.Default.Star
                }
            },
            fillEqually = true
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEachIndexed { index, genre ->
                val value = when (sort) {
                    GenreSort.HOURS -> if (isManga) {
                        formatExactCount(genre.hoursWatched.roundToInt(), locale)
                    } else {
                        stringResource(R.string.stats_genre_hours, formatExactCount(genre.hoursWatched.roundToInt(), locale))
                    }
                    GenreSort.TITLES, GenreSort.SCORE -> formatExactCount(genre.count, locale)
                }
                GenreRow(
                    rank = index + 1,
                    genre = genre,
                    value = value,
                    fraction = metric(genre) / max,
                    mean = stringResource(R.string.stats_genre_mean, formatOneDecimal(genre.meanScore.toDouble(), locale))
                )
            }
            BestRated(genres, locale)
        }
    }
}

private const val GENRE_ROWS = 13

@Composable
private fun GenreRow(rank: Int, genre: GenreStat, value: String, fraction: Float, mean: String) {
    val scheme = MaterialTheme.colorScheme
    val colors = genreColor(genre.genre)
    val container = colors?.container ?: scheme.secondaryContainer
    val content = colors?.content ?: scheme.onSecondaryContainer
    val nameStyle = MaterialTheme.typography.labelLarge
    val valueStyle = genreValueStyle()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = rank.toString(),
            style = valueStyle,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(18.dp)
        )
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainerHigh)
        ) {
            val needed = measuredWidth(genre.genre, nameStyle) + measuredWidth(value, valueStyle) + FillPadding * 2 + FillGap
            val width = maxOf((maxWidth - TrackSlack) * fraction, MinFill, needed).coerceAtMost(maxWidth)
            Row(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(container)
                    .padding(horizontal = FillPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = genre.genre,
                    style = nameStyle,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(text = value, style = valueStyle, color = content.copy(alpha = 0.85f), maxLines = 1)
            }
        }
        Text(text = mean, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun genreValueStyle(): TextStyle = MaterialTheme.typography.labelLarge.copy(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp
)

/** The top bar leaves this much of its track showing, as the frame draws it. */
private val TrackSlack = 32.dp

/** Short bars stay long enough to carry a genre's name and number. */
private val MinFill = 176.dp
private val FillPadding = 12.dp
private val FillGap = 8.dp

/** The three best rated genres among those with at least as many titles as the bar sets. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BestRated(genres: List<GenreStat>, locale: Locale) {
    val threshold = BestRatedThresholds.firstOrNull { bar -> genres.count { it.count >= bar } >= BEST_RATED } ?: return
    val best = genres.filter { it.count >= threshold }.sortedByDescending { it.meanScore }.take(BEST_RATED)
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = pluralStringResourceOrDefault(R.plurals.stats_best_rated, threshold, threshold),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurface
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            best.forEach { genre ->
                val colors = genreColor(genre.genre)
                val content = colors?.content ?: scheme.onSecondaryContainer
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors?.container ?: scheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = genre.genre, style = genreValueStyle(), color = content, maxLines = 1)
                    Text(
                        text = stringResource(R.string.stats_genre_mean, formatOneDecimal(genre.meanScore.toDouble(), locale)),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
                        color = content.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private const val BEST_RATED = 3
private val BestRatedThresholds = listOf(100, 50, 20, 10, 5)

/** The most common tags, twelve at first and the rest a tap away. */
@Composable
fun TagBoard(tags: List<TagStat>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shown = if (expanded) tags else tags.take(TAGS_FIRST)
    val max = tags.maxOf { it.count }.coerceAtLeast(1)
    val hasMore = tags.size > TAGS_FIRST
    // Show more draws its line in a 40dp touch row, so the card keeps less padding under it.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (hasMore) 6.dp else 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.stats_tags_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.stats_tags_shown, shown.size, tags.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
            shown.forEach { tag ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(
                                R.string.stats_tag_line,
                                formatExactCount(tag.count, locale),
                                formatOneDecimal(tag.meanScore.toDouble(), locale)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    LinearProgressIndicator(
                        progress = { tag.count.toFloat() / max },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = scheme.secondary,
                        trackColor = scheme.surfaceContainerHigh,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }
        }
        if (hasMore) {
            ShowMoreRow(
                text = if (expanded) {
                    stringResource(R.string.stats_tags_show_fewer)
                } else {
                    pluralStringResourceOrDefault(R.plurals.stats_tags_show_more, tags.size - TAGS_FIRST, tags.size - TAGS_FIRST)
                },
                expanded = expanded,
                onClick = { expanded = !expanded }
            )
        }
    }
}

private const val TAGS_FIRST = 12
