package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.ActivityHistoryDay
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.formatOneDecimal
import com.anisync.android.domain.stats.ActivityInsights
import com.anisync.android.domain.stats.Eras
import com.anisync.android.domain.stats.ScoreSummary
import com.anisync.android.domain.stats.TasteSpread
import com.anisync.android.domain.stats.tasteSpread
import com.anisync.android.presentation.profile.ScoreUiModel
import com.anisync.android.presentation.profile.StatsView
import com.anisync.android.presentation.recap.spelled
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.LocalAvatarShape
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.genreColor
import com.anisync.android.ui.theme.lineBox
import com.anisync.android.ui.theme.listIndicatorColor
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * One of the three tiles under the hero. Words get a smaller value size than numbers. The label is
 * drawn as given, already in capitals, so a σ inside it stays a σ.
 */
@Immutable
data class StatTileModel(val icon: ImageVector, val value: String, val label: String, val isWord: Boolean = false)

@Composable
fun StatsTiles(tiles: List<StatTileModel>, modifier: Modifier = Modifier) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.forEach { tile ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(tile.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(6.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val size = if (tile.isWord) 22f else 26f
                    val style = fittedStyle(tile.value, titleWide.style(size.sp, 32.sp), maxWidth)
                    Text(
                        text = tile.value,
                        style = style,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.lineBox(style)
                    )
                }
                Text(
                    text = tile.label,
                    style = LocalExpressiveTypography.current.statLabel.copy(letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * [base] shrunk by whole points until [text] fits [maxWidth] on one line, never below 14. The size
 * is found by halving the range, so a long way down costs a handful of layouts, not one per point.
 */
@Composable
internal fun fittedStyle(text: String, base: TextStyle, maxWidth: Dp): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, base, maxWidth, density, measurer) {
        val largest = base.fontSize.value
        val points = pointsToShrink(ceil(largest - MIN_FITTED_SP).toInt()) { points ->
            with(density) { measurer.measure(text, base.copy(fontSize = (largest - points).sp)).size.width.toDp() } <= maxWidth
        }
        if (points == 0) base else base.copy(fontSize = (largest - points).sp)
    }
}

private const val MIN_FITTED_SP = 14f

/**
 * The fewest whole points to take off for [fits] to hold, never more than [floor], which counts as
 * fitting whatever the text does. Halves the range each try, so [fits] runs a handful of times.
 */
internal fun pointsToShrink(floor: Int, fits: (points: Int) -> Boolean): Int {
    if (floor <= 0 || fits(0)) return 0
    var low = 1
    var high = floor
    while (low < high) {
        val mid = (low + high) / 2
        if (fits(mid)) high = mid else low = mid + 1
    }
    return low
}

/** The view a teaser opens, with everything it shows already worked out. */
@Immutable
data class AtAGlanceModel(
    val activity: ActivityTeaser?,
    val taste: TasteTeaser?,
    val format: FormatTeaser?,
    val eras: ErasTeaser?,
    val people: PeopleTeaser,
)

@Immutable
data class ActivityTeaser(val insights: ActivityInsights, val history: List<ActivityHistoryDay>)

@Immutable
data class TasteTeaser(
    val genre: String,
    val genreTitles: Int,
    val nextGenres: List<Pair<String, Int>>,
    val scores: List<ScoreUiModel>,
    val summary: ScoreSummary,
    val meanScore: Double,
)

@Immutable
data class FormatTeaser(
    val shares: List<Pair<String, Int>>,
    val completedShare: Int?,
    val onlyCountry: String?,
    val isManga: Boolean = false,
)

@Immutable
data class ErasTeaser(val eras: Eras)

@Immutable
data class PeopleTeaser(
    val personName: String?,
    val personImage: String?,
    val personCount: Int,
    /** Manga names its staff member, counted in works, where anime names a voice actor's roles. */
    val personIsStaff: Boolean = false,
    val studioName: String?,
    val studioTitles: Int,
)

/** "At a glance": one teaser per view, each opening its view. */
@Composable
fun AtAGlance(model: AtAGlanceModel, isOwnProfile: Boolean, onOpen: (StatsView) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.activity?.let { ActivityTeaserCard(it, onClick = { onOpen(StatsView.ACTIVITY) }) }
        // A narrow pane or a larger display size can push a teaser past 212dp, so each pair grows
        // together rather than clipping its last line.
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cell = Modifier
                .weight(1f)
                .fillMaxHeight()
            Box(cell) { model.taste?.let { TasteTeaserCard(it, isOwnProfile, onClick = { onOpen(StatsView.TASTE) }) } }
            Box(cell) { model.format?.let { FormatTeaserCard(it, isOwnProfile, onClick = { onOpen(StatsView.HOW_YOU_WATCH) }) } }
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cell = Modifier
                .weight(1f)
                .fillMaxHeight()
            Box(cell) { model.eras?.let { ErasTeaserCard(it, onClick = { onOpen(StatsView.ERAS) }) } }
            Box(cell) { PeopleTeaserCard(model.people, isOwnProfile, onClick = { onOpen(StatsView.PEOPLE) }) }
        }
    }
}

@Composable
private fun Teaser(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fixedHeight: Boolean = true,
    spacing: Dp = 6.dp,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fixedHeight) Modifier.fillMaxHeight().heightIn(min = TeaserHeight) else Modifier)
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title.uppercase(),
                style = LocalExpressiveTypography.current.statLabel,
                color = content,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }
        body()
    }
}

private val TeaserHeight = 212.dp

@Composable
private fun ActivityTeaserCard(teaser: ActivityTeaser, onClick: () -> Unit) {
    val insights = teaser.insights
    val locale = LocalConfiguration.current.locales[0]
    val showStreak = insights.longestStreak >= STREAK_WORTH_SHOWING
    Teaser(title = stringResource(R.string.profile_tab_activity), onClick = onClick, fixedHeight = false, spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            MiniHeatmap(teaser.history)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val value = when {
                    !showStreak -> activeDays(teaser.history).toString()
                    insights.streakIsOpenEnded -> stringResource(R.string.stats_count_open_ended, insights.longestStreak)
                    else -> insights.longestStreak.toString()
                }
                val valueStyle = LocalExpressiveTypography.current.heroWide.style(44.sp, 48.sp, (-1.5).sp)
                Text(
                    text = value,
                    style = valueStyle,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.lineBox(valueStyle)
                )
                Text(
                    text = if (showStreak) {
                        pluralStringResourceOrDefault(R.plurals.stats_day_streak, insights.longestStreak)
                    } else {
                        pluralStringResourceOrDefault(R.plurals.stats_active_days, activeDays(teaser.history))
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val weekday = stringResource(weekdayPluralRes(insights.busiestWeekday))
                Text(
                    text = if (insights.perDay >= 1.0) {
                        stringResource(R.string.stats_busiest_on_rate, weekday, formatOneDecimal(insights.perDay, locale))
                    } else {
                        pluralStringResourceOrDefault(R.plurals.stats_busiest_on_count, insights.busiestWeekdayTotal, weekday, insights.busiestWeekdayTotal)
                    },
                    style = statsBodySmall(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A streak shorter than a week says less than the count of active days does. */
private const val STREAK_WORTH_SHOWING = 7

private fun activeDays(history: List<ActivityHistoryDay>): Int = history.count { it.amount > 0 }

/** The last ten weeks of activity, a column per week. */
@Composable
private fun MiniHeatmap(history: List<ActivityHistoryDay>) {
    val byDate = remember(history) { bucketActivityDays(history) }
    val last = byDate.keys.maxOrNull() ?: LocalDate.now()
    val firstColumnStart = last.minusWeeks(MINI_WEEKS - 1L).minusDays(last.dayOfWeek.value % 7L)
    val active = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(MINI_WEEKS) { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(7) { day ->
                    val date = firstColumnStart.plusDays(week * 7L + day)
                    val level = byDate[date]?.level ?: 0
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(
                                if (level <= 0 || date.isAfter(last)) empty
                                else active.copy(alpha = 0.36f + 0.16f * ((level.coerceIn(1, 10) - 1) * 4 / 10))
                            )
                    )
                }
            }
        }
    }
}

private const val MINI_WEEKS = 10

@Composable
private fun TasteTeaserCard(teaser: TasteTeaser, isOwnProfile: Boolean, onClick: () -> Unit) {
    val colors = genreColor(teaser.genre)
    val container = colors?.container ?: MaterialTheme.colorScheme.secondaryContainer
    val content = colors?.content ?: MaterialTheme.colorScheme.onSecondaryContainer
    Teaser(title = stringResource(R.string.stats_view_taste), onClick = onClick, container = container, content = content) {
        val genreStyle = LocalExpressiveTypography.current.titleWide.style(28.sp, 34.sp, (-0.5).sp)
        Text(
            text = teaser.genre,
            style = genreStyle,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.lineBox(genreStyle)
        )
        Text(
            text = pluralStringResourceOrDefault(
                if (isOwnProfile) R.plurals.stats_teaser_top_genre else R.plurals.stats_teaser_top_genre_other,
                teaser.genreTitles,
                teaser.genreTitles
            ),
            style = statsBodySmall(),
            color = content.copy(alpha = 0.85f)
        )
        if (teaser.summary.hasSpread) {
            ScoreMiniBars(teaser.scores, teaser.summary, content)
            Text(
                text = stringResource(
                    R.string.stats_teaser_mean_mode,
                    formatScoreNumber(teaser.meanScore),
                    teaser.summary.mostGiven.toString()
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
                color = content.copy(alpha = 0.85f)
            )
        } else {
            if (teaser.nextGenres.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.stats_teaser_then, joinWithAnd(teaser.nextGenres.map { "${it.first} ${it.second}" })),
                    style = statsBodySmall(),
                    color = content.copy(alpha = 0.8f)
                )
            }
            Text(
                text = stringResource(if (isOwnProfile) R.string.stats_teaser_score_prompt else R.string.stats_teaser_score_prompt_other),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
                color = content.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Up to ten steps of the score spread, the most given one lit. Ten bars sit 5.5dp apart as the
 * frame draws them, and a list scored in fewer steps spreads its bars over the same width.
 */
@Composable
private fun ScoreMiniBars(scores: List<ScoreUiModel>, summary: ScoreSummary, color: Color) {
    val lit = scores.indexOfFirst { bucket -> summary.mostGiven != null && bucket.score >= summary.mostGiven }
    Row(
        modifier = Modifier
            .widthIn(max = MiniBarsWidth)
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        scores.forEachIndexed { index, bucket ->
            Box(
                Modifier
                    .width(12.dp)
                    .height((48f * bucket.heightFraction).coerceAtLeast(6f).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (index == lit) color else color.copy(alpha = 0.45f))
            )
        }
    }
}

/** Ten 12dp bars 5.5dp apart. */
private val MiniBarsWidth = 169.5.dp

@Composable
private fun FormatTeaserCard(teaser: FormatTeaser, isOwnProfile: Boolean, onClick: () -> Unit) {
    val total = teaser.shares.sumOf { it.second }.coerceAtLeast(1)
    val first = teaser.shares.firstOrNull()
    val title = if (teaser.isManga) R.string.stats_view_how_you_read else R.string.stats_view_how_you_watch
    Teaser(title = stringResource(title), onClick = onClick) {
        if (first != null) {
            // TV, OVA and ONA take the frame's 40sp. A longer name steps down, and the share moves
            // under it when the two no longer fit side by side.
            val name = shortFormatName(first.first)
            FlowRow(itemVerticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val formatStyle = if (name.length <= SHORT_FORMAT) {
                    LocalExpressiveTypography.current.heroWide.style(40.sp, 44.sp)
                } else {
                    LocalExpressiveTypography.current.heroWide.style(28.sp, 32.sp)
                }
                Text(
                    text = name,
                    style = formatStyle,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.lineBox(formatStyle)
                )
                Text(
                    text = stringResource(R.string.stats_teaser_format_share, percent(first.second, total)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            FormatShareBar(teaser.shares)
            Spacer(Modifier.height(2.dp))
        }
        val line = when {
            teaser.completedShare != null -> stringResource(
                if (isOwnProfile) R.string.stats_teaser_finish_rate else R.string.stats_teaser_finish_rate_other,
                "${teaser.completedShare}%"
            )
            teaser.onlyCountry != null -> stringResource(R.string.stats_teaser_one_country, countryName(teaser.onlyCountry))
            else -> null
        }
        if (line != null) {
            Text(text = line, style = statsBodySmall(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The formats side by side by share, in a fixed order of colours. A plain layout rather than a
 * BoxWithConstraints, so the teaser rows can ask for its intrinsic height.
 */
@Composable
private fun FormatShareBar(shares: List<Pair<String, Int>>) {
    val colors = formatColors()
    val visible = shares.take(colors.size)
    Layout(
        content = {
            visible.forEachIndexed { index, _ ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors[index])
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
    ) { measurables, constraints ->
        if (!constraints.hasBoundedWidth) return@Layout layout(0, constraints.minHeight) {}
        val gap = ShareGap.roundToPx()
        val available = (constraints.maxWidth - gap * (measurables.size - 1).coerceAtLeast(0)).toDp()
        val widths = segmentWidths(visible.map { it.second }, available)
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints.fixed(widths[index].roundToPx().coerceAtLeast(0), constraints.maxHeight))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.place(x, 0)
                x += placeable.width + gap
            }
        }
    }
}

private val ShareGap = 2.dp

@Composable
internal fun formatColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        scheme.primary,
        scheme.tertiary,
        scheme.secondary,
        listIndicatorColor(ListIndicatorKind.PLANNING).content,
        listIndicatorColor(ListIndicatorKind.PAUSED).content,
        listIndicatorColor(ListIndicatorKind.DROPPED).content,
        scheme.outline,
    )
}

@Composable
private fun ErasTeaserCard(teaser: ErasTeaser, onClick: () -> Unit) {
    val eras = teaser.eras
    val span = eras.newestYear - eras.oldestYear
    val decades = (span / 10.0).roundToInt()
    Teaser(title = stringResource(R.string.stats_view_eras), onClick = onClick) {
        val rangeStyle = LocalExpressiveTypography.current.heroWide.style(21.sp, 30.sp)
        Text(
            text = stringResource(R.string.stats_year_range, eras.oldestYear, eras.newestYear),
            style = rangeStyle,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = rangeStyle.fontSize),
            modifier = Modifier.lineBox(rangeStyle)
        )
        Text(
            text = if (span >= CENTURY) {
                pluralStringResourceOrDefault(R.plurals.stats_teaser_century, decades, spelled(decades))
            } else {
                pluralStringResourceOrDefault(
                    R.plurals.stats_teaser_decades_peak,
                    decades,
                    spelled(decades),
                    eras.peakReleaseYear.year.toString()
                )
            },
            style = statsBodySmall(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (eras.decades.size >= DECADES_FOR_DECADE_BARS) {
            DecadeMiniBars(eras)
        } else {
            YearMiniBars(eras)
        }
    }
}

private const val CENTURY = 100
private const val DECADES_FOR_DECADE_BARS = 5

@Composable
private fun DecadeMiniBars(eras: Eras) {
    val max = eras.decades.maxOf { it.count }.coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        eras.decades.forEach { decade ->
            Box(
                Modifier
                    .width(11.dp)
                    .height((56f * decade.count / max).coerceAtLeast(4f).dp)
                    .clip(RoundedCornerShape(5.5.dp))
                    .background(
                        if (decade == eras.peakDecade) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer
                    )
            )
        }
    }
}

/** Few release years: a bar per year with its short label, the peak lit. */
@Composable
private fun YearMiniBars(eras: Eras) {
    val years = eras.releaseYears.entries.sortedBy { it.key }
    val max = years.maxOf { it.value }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        years.forEach { (year, count) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(44.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((44f * count / max).coerceAtLeast(8f).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (year == eras.peakReleaseYear.year) 1f else 0.36f)
                            )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.stats_short_year, year % 100),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PeopleTeaserCard(teaser: PeopleTeaser, isOwnProfile: Boolean, onClick: () -> Unit) {
    Teaser(title = stringResource(R.string.stats_view_people), onClick = onClick, spacing = 8.dp) {
        if (teaser.personName == null && teaser.studioName == null) {
            Text(
                text = stringResource(if (isOwnProfile) R.string.stats_teaser_people_locked else R.string.stats_teaser_people_locked_other),
                style = statsBodySmall(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Teaser
        }
        if (teaser.personName != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AsyncImage(
                    model = teaser.personImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(LocalAvatarShape.current)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                PersonLines(teaser.personName, if (teaser.personIsStaff) worksLabel(teaser.personCount) else rolesLabel(teaser.personCount))
            }
        }
        if (teaser.studioName != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioMonogram(teaser.studioName, size = 40.dp, textSize = 13f)
                PersonLines(teaser.studioName, pluralStringResourceOrDefault(R.plurals.studio_titles_count, teaser.studioTitles, teaser.studioTitles))
            }
        }
    }
}

@Composable
private fun PersonLines(name: String, detail: String) {
    Column {
        Text(text = name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = detail, style = statsBodySmall(), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** A studio's initials in a 9-sided cookie, as the People view and its teaser draw them. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StudioMonogram(name: String, size: Dp, textSize: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val style = LocalExpressiveTypography.current.titleWide.style(textSize.sp, (textSize * 18 / 13).sp)
        Text(
            text = monogram(name),
            style = style,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            modifier = Modifier.lineBox(style)
        )
    }
}

/** The first letters of a studio's first two words, "Kyoto Animation" reading KA. */
internal fun monogram(name: String): String {
    val words = name.split(' ', '-').filter { it.isNotBlank() }
    val capitals = name.filter { it.isUpperCase() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        capitals.length >= 2 && capitals.length < name.length -> capitals.take(2)
        else -> name.take(2).uppercase()
    }
}

@Composable
internal fun shortFormatName(format: String): String = when (format) {
    "TV" -> "TV"
    "OVA", "ONA" -> format
    else -> formatDisplayName(format)
}

@Composable
private fun formatScoreNumber(value: Double): String {
    val locale = LocalConfiguration.current.locales[0]
    return NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(value)
}

private fun percent(part: Int, total: Int): String = "${(part * 100.0 / total).roundToInt()}%"

@Composable
internal fun countryName(code: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return Locale("", code).getDisplayCountry(locale).ifBlank { code }
}

@Composable
internal fun joinWithAnd(items: List<String>): String = when (items.size) {
    0 -> ""
    1 -> items.first()
    else -> stringResource(R.string.notification_actors_two, items.dropLast(1).joinToString(", "), items.last())
}

internal fun weekdayPluralRes(day: DayOfWeek): Int = when (day) {
    DayOfWeek.MONDAY -> R.string.stats_weekdays_monday
    DayOfWeek.TUESDAY -> R.string.stats_weekdays_tuesday
    DayOfWeek.WEDNESDAY -> R.string.stats_weekdays_wednesday
    DayOfWeek.THURSDAY -> R.string.stats_weekdays_thursday
    DayOfWeek.FRIDAY -> R.string.stats_weekdays_friday
    DayOfWeek.SATURDAY -> R.string.stats_weekdays_saturday
    DayOfWeek.SUNDAY -> R.string.stats_weekdays_sunday
}

/** Tiles for the hero's row: the count, the mean, and the spread or the top genre. */
@Composable
fun overviewTiles(
    count: Int,
    countLabel: String,
    isManga: Boolean,
    meanScore: Double,
    standardDeviation: Double,
    summary: ScoreSummary,
    topGenre: String?,
): List<StatTileModel> {
    val locale = LocalConfiguration.current.locales[0]
    val spread = tasteSpread(standardDeviation)
    val third = if (summary.hasSpread && spread != null) {
        StatTileModel(
            icon = Icons.Default.Equalizer,
            value = stringResource(spread.labelRes()),
            label = stringResource(
                R.string.stats_tile_taste,
                stringResource(R.string.stats_view_taste).uppercase(locale),
                stringResource(R.string.stats_sigma),
                formatScoreNumber(standardDeviation)
            ),
            isWord = true
        )
    } else {
        StatTileModel(
            icon = Icons.Default.LocalOffer,
            value = topGenre ?: "",
            label = stringResource(R.string.stats_tile_top_genre).uppercase(locale),
            isWord = topGenre != null && topGenre.length > SHORT_WORD
        )
    }
    return listOf(
        StatTileModel(
            icon = if (isManga) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PlayArrow,
            value = formatExactCount(count, locale),
            label = countLabel.uppercase(locale)
        ),
        StatTileModel(Icons.Default.Star, formatScoreNumber(meanScore), stringResource(R.string.statistics_mean_score).uppercase(locale)),
        third,
    )
}

private const val SHORT_WORD = 6

/** Format names up to this long fit the teaser at the frame's size. */
private const val SHORT_FORMAT = 3

internal fun TasteSpread.labelRes(): Int = when (this) {
    TasteSpread.FOCUSED -> R.string.stats_taste_focused
    TasteSpread.BALANCED -> R.string.stats_taste_balanced
    TasteSpread.ECLECTIC -> R.string.stats_taste_eclectic
}
