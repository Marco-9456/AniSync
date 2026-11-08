package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.formatOneDecimal
import com.anisync.android.domain.stats.Eras
import com.anisync.android.presentation.recap.spelled
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.lineBox
import java.time.Year
import kotlin.math.roundToInt

/** The span of years a list covers, on the wide axis, with a sentence on its ends. */
@Composable
fun ErasSpanHero(eras: Eras, isManga: Boolean, isOwnProfile: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val content = scheme.onTertiaryContainer
    val rangeStyle = LocalExpressiveTypography.current.heroWide.style(42.sp, 52.sp, (-1.5).sp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.tertiaryContainer)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(if (isOwnProfile) R.string.stats_eras_eyebrow_own else R.string.stats_eras_eyebrow_other).uppercase(),
            style = LocalExpressiveTypography.current.statLabel,
            color = content.copy(alpha = 0.75f)
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val text = stringResource(R.string.stats_year_range, eras.oldestYear, eras.newestYear)
            val style = fittedStyle(text, rangeStyle, maxWidth)
            Text(text = text, style = style, color = content, maxLines = 1, modifier = Modifier.lineBox(style))
        }
        Text(
            text = spanSentence(eras, isManga, isOwnProfile),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
            color = content.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun spanSentence(eras: Eras, isManga: Boolean, isOwnProfile: Boolean): String {
    val span = eras.newestYear - eras.oldestYear
    val decades = (span / 10.0).roundToInt()
    val thisYear = Year.now().value
    val ends = when {
        eras.newestYear >= thisYear -> stringResource(
            if (isOwnProfile) R.string.stats_eras_ends_this_year_own else R.string.stats_eras_ends_this_year_other,
            eras.oldestYear
        )
        else -> stringResource(
            if (isOwnProfile) R.string.stats_eras_ends_own else R.string.stats_eras_ends_other,
            eras.oldestYear,
            eras.newestYear
        )
    }
    val lead = when {
        span >= CENTURY -> stringResource(if (isManga) R.string.stats_eras_century_manga else R.string.stats_eras_century_anime)
        decades >= 2 -> pluralStringResourceOrDefault(
            if (isManga) R.plurals.stats_eras_decades_manga else R.plurals.stats_eras_decades_anime,
            decades,
            spelled(decades).replaceFirstChar { it.titlecase() }
        )
        else -> null
    }
    return if (lead == null) ends else stringResource(R.string.stats_two_sentences, lead, ends)
}

private const val CENTURY = 100

/** A row per decade from the oldest to the newest, the empty ones saying so, the peak lit. */
@Composable
fun DecadesCard(eras: Eras, isManga: Boolean, isOwnProfile: Boolean, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val max = eras.decades.maxOf { it.count }.coerceAtLeast(1)
    val countStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        eras.decades.forEach { decade ->
            val isPeak = decade == eras.peakDecade
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.stats_decade, decade.decade),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isPeak) scheme.primary else scheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.width(52.dp)
                )
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .height(22.dp)
                ) {
                    if (decade.count > 0) {
                        Box(
                            Modifier
                                .width(((maxWidth - DecadeSlack) * (decade.count.toFloat() / max)).coerceAtLeast(6.dp))
                                .height(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isPeak) scheme.primary else scheme.secondaryContainer)
                        )
                    }
                }
                Text(
                    text = if (decade.count > 0) formatExactCount(decade.count, locale) else stringResource(R.string.stats_decade_none),
                    style = countStyle,
                    color = if (decade.count > 0) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        NoteChip(
            icon = Icons.Default.AutoAwesomeMotion,
            text = decadeShareSentence(eras, isManga, isOwnProfile),
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurface
        )
    }
}

/** The longest bar leaves this much of its row free, as the frame draws it. */
private val DecadeSlack = 24.dp

@Composable
private fun decadeShareSentence(eras: Eras, isManga: Boolean, isOwnProfile: Boolean): String {
    val decade = stringResource(R.string.stats_decade, eras.peakDecade.decade)
    val share = eras.peakDecadeShare
    val shares = when {
        !isOwnProfile -> DecadeShares.List
        isManga -> DecadeShares.Manga
        else -> DecadeShares.Anime
    }
    val res = when {
        share >= 2.0 / 3 -> shares.most
        share >= 0.5 -> shares.half
        share >= 1.0 / 3 -> shares.third
        share >= 0.25 -> shares.quarter
        else -> shares.peak
    }
    return stringResource(res, decade)
}

private enum class DecadeShares(val most: Int, val half: Int, val third: Int, val quarter: Int, val peak: Int) {
    Anime(
        R.string.stats_decade_share_most_anime,
        R.string.stats_decade_share_half_anime,
        R.string.stats_decade_share_third_anime,
        R.string.stats_decade_share_quarter_anime,
        R.string.stats_decade_share_peak_anime,
    ),
    Manga(
        R.string.stats_decade_share_most_manga,
        R.string.stats_decade_share_half_manga,
        R.string.stats_decade_share_third_manga,
        R.string.stats_decade_share_quarter_manga,
        R.string.stats_decade_share_peak_manga,
    ),
    List(
        R.string.stats_decade_share_most_list,
        R.string.stats_decade_share_half_list,
        R.string.stats_decade_share_third_list,
        R.string.stats_decade_share_quarter_list,
        R.string.stats_decade_share_peak_list,
    ),
}

/** One bar a year for the most recent fifteen, the peak lit, every other year named. */
@Composable
fun YearsTimelineCard(
    title: String,
    years: Map<Int, Int>,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit,
) {
    if (years.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val newest = years.keys.max()
    val oldest = maxOf(years.keys.min(), newest - TIMELINE_YEARS + 1)
    val shown = (oldest..newest).map { it to (years[it] ?: 0) }
    val peak = shown.maxWith(compareBy({ it.second }, { it.first })).first
    val max = shown.maxOf { it.second }.coerceAtLeast(1)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.stats_year_range, oldest, newest),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        }
        val peakIndex = shown.indexOfFirst { it.first == peak }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TimelineHeight)
                .padding(start = 2.dp, end = 5.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            shown.forEachIndexed { index, (year, count) ->
                val isPeak = year == peak
                val named = isPeak || (index % 2 == 0 && kotlin.math.abs(index - peakIndex) != 1)
                val description = pluralStringResourceOrDefault(R.plurals.a11y_timeline_bar, count, year, count)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = description }
                ) {
                    Box(
                        Modifier
                            .width(TimelineBar)
                            .height((TimelineMax * count / max).roundToInt().coerceAtLeast(if (count > 0) 6 else 0).dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isPeak) scheme.primary else scheme.secondaryContainer)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (named) stringResource(R.string.stats_short_year, year % 100) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPeak) scheme.primary else scheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.height(16.dp)
                    )
                }
            }
        }
        footer()
    }
}

private const val TIMELINE_YEARS = 15
private const val TimelineMax = 110f
private val TimelineBar = 18.dp
private val TimelineHeight = 150.dp

/** Two capsules under the release timeline: the peak year and the best scored one. */
@Composable
fun ReleaseCapsules(eras: Eras, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        YearCapsule(
            year = eras.peakReleaseYear.year,
            label = pluralStringResourceOrDefault(
                R.plurals.stats_eras_peak_titles,
                eras.peakReleaseYear.count,
                formatExactCount(eras.peakReleaseYear.count, locale)
            ),
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
            labelColor = scheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        val best = eras.bestScoredYear
        if (best != null) {
            YearCapsule(
                year = best.year,
                label = stringResource(R.string.stats_eras_best_scored, formatOneDecimal(best.mean, locale)),
                container = scheme.surfaceContainerHigh,
                content = scheme.onSurface,
                labelColor = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(Modifier.weight(1f))
        }
    }
}

@Composable
private fun YearCapsule(year: Int, label: String, container: Color, content: Color, labelColor: Color, modifier: Modifier = Modifier) {
    val style = LocalExpressiveTypography.current.titleWide.style(22.sp, 28.sp)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = year.toString(), style = style, color = content, maxLines = 1, modifier = Modifier.lineBox(style))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = labelColor, maxLines = 1)
    }
}

/** Under the start timeline: the year the user started the most, and whether it was also the peak release year. */
@Composable
fun StartedNote(eras: Eras, isManga: Boolean, isOwnProfile: Boolean) {
    val start = eras.peakStartYear ?: return
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val text = if (start.year == eras.peakReleaseYear.year) {
        stringResource(
            when {
                !isOwnProfile -> R.string.stats_eras_year_twice_other
                isManga -> R.string.stats_eras_year_twice_own_manga
                else -> R.string.stats_eras_year_twice_own
            },
            start.year.toString(),
            formatExactCount(eras.peakReleaseYear.count, locale),
            formatExactCount(start.count, locale)
        )
    } else {
        stringResource(
            if (isOwnProfile) R.string.stats_eras_started_most_own else R.string.stats_eras_started_most_other,
            start.year,
            formatExactCount(start.count, locale)
        )
    }
    NoteChip(Icons.Default.Event, text, scheme.tertiaryContainer, scheme.onTertiaryContainer)
}

@Composable
private fun NoteChip(icon: ImageVector, text: String, container: Color, content: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}
