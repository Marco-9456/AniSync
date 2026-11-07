package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.CountryStat
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.formatOneDecimal
import com.anisync.android.domain.formatPercent
import com.anisync.android.presentation.components.toIndicatorKind
import com.anisync.android.presentation.profile.LengthUiModel
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.lineBox
import com.anisync.android.ui.theme.listIndicatorColor
import kotlin.math.roundToInt

/** One list's tile: its count over its name, in the list's own colours. */
@Immutable
data class ListTile(val status: LibraryStatus, val count: Int)

/** The lists as tiles, largest first, then how much of what was started got finished. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListTilesCard(
    tiles: List<ListTile>,
    completed: Int,
    total: Int,
    mediaType: MediaType,
    ownProfile: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val valueStyle = LocalExpressiveTypography.current.titleWide.style(24.sp, 30.sp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tiles.sortedByDescending { it.count }.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { tile ->
                        val colors = listIndicatorColor(tile.status.toIndicatorKind())
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.container)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = formatExactCount(tile.count, locale),
                                style = valueStyle,
                                color = colors.content,
                                maxLines = 1,
                                modifier = Modifier.lineBox(valueStyle)
                            )
                            Text(
                                text = tile.status.toLabel(mediaType),
                                style = tileLabelStyle(),
                                color = colors.content.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
        if (completed > 0 && total > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = listIndicatorColor(ListIndicatorKind.COMPLETED).content,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(
                        if (ownProfile) R.string.stats_finish_line else R.string.stats_finish_line_other,
                        formatPercent(completed, total, locale),
                        formatExactCount(completed, locale),
                        formatExactCount(total, locale)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun tileLabelStyle() = MaterialTheme.typography.labelLarge.copy(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.sp
)

/** Formats side by side by share, then one row each with its titles, share, time and mean. */
@Composable
fun FormatsCard(formats: List<FormatStat>, isManga: Boolean, isOwnProfile: Boolean, modifier: Modifier = Modifier) {
    if (formats.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val pairs = formatColorPairs()
    val sorted = formats.sortedByDescending { it.count }.take(pairs.size)
    val total = sorted.sumOf { it.count }.coerceAtLeast(1)
    // A format needs a fair share of the list before its mean can speak for it.
    val bestFloor = maxOf(BEST_FORMAT_MIN_TITLES, (total * BEST_FORMAT_MIN_SHARE).roundToInt())
    val best = sorted.filter { it.meanScore > 0f && it.count >= bestFloor }.maxByOrNull { it.meanScore }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val widths = segmentWidths(sorted.map { it.count }, maxWidth - 3.dp * (sorted.size - 1).coerceAtLeast(0))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                sorted.forEachIndexed { index, _ ->
                    Box(
                        Modifier
                            .width(widths[index])
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pairs[index].first)
                    )
                }
            }
        }
        sorted.forEachIndexed { index, format ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pairs[index].first),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getFormatIcon(format.format), contentDescription = null, tint = pairs[index].second, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(text = shortFormatName(format.format), style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, maxLines = 1)
                    val titles = pluralStringResourceOrDefault(R.plurals.stats_titles, format.count, formatExactCount(format.count, locale))
                    val share = formatPercent(format.count, total, locale)
                    val amount = if (isManga) {
                        pluralStringResourceOrDefault(
                            R.plurals.stats_chapters_short,
                            format.hoursWatched.roundToInt(),
                            formatExactCount(format.hoursWatched.roundToInt(), locale)
                        )
                    } else {
                        stringResource(R.string.stats_genre_hours, formatExactCount(format.hoursWatched.roundToInt(), locale))
                    }
                    Text(
                        text = stringResource(R.string.stats_format_line, titles, share, amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (format.meanScore > 0f) {
                    MeanChip(formatOneDecimal(format.meanScore.toDouble(), locale), highlighted = format == best)
                }
            }
        }
        if (best != null && sorted.size > 1) {
            Text(
                text = stringResource(if (isOwnProfile) bestFormatRes(best.format) else theirBestFormatRes(best.format)),
                style = statsBodySmall(),
                color = scheme.onSurfaceVariant
            )
        }
    }
}

private const val BEST_FORMAT_MIN_TITLES = 3
private const val BEST_FORMAT_MIN_SHARE = 0.05

@Composable
private fun MeanChip(value: String, highlighted: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val content = if (highlighted) scheme.onPrimaryContainer else scheme.onSurface
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) scheme.primaryContainer else scheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(text = value, style = tileLabelStyle().copy(fontWeight = FontWeight.Bold), color = content, maxLines = 1)
    }
}

/** Each format's colour and the colour drawn on it, in the order the share bar uses them. */
@Composable
private fun formatColorPairs(): List<Pair<Color, Color>> {
    val scheme = MaterialTheme.colorScheme
    val colors = formatColors()
    val on = listOf(
        scheme.onPrimary,
        scheme.onTertiary,
        scheme.onSecondary,
        listIndicatorColor(ListIndicatorKind.PLANNING).container,
        listIndicatorColor(ListIndicatorKind.PAUSED).container,
        listIndicatorColor(ListIndicatorKind.DROPPED).container,
        scheme.surface,
    )
    return colors.zip(on)
}

private fun bestFormatRes(format: String): Int = when (format) {
    "TV" -> R.string.stats_best_format_tv
    "TV_SHORT" -> R.string.stats_best_format_tv_short
    "MOVIE" -> R.string.stats_best_format_movie
    "SPECIAL" -> R.string.stats_best_format_special
    "OVA" -> R.string.stats_best_format_ova
    "ONA" -> R.string.stats_best_format_ona
    "MUSIC" -> R.string.stats_best_format_music
    "MANGA" -> R.string.stats_best_format_manga
    "NOVEL" -> R.string.stats_best_format_novel
    "ONE_SHOT" -> R.string.stats_best_format_one_shot
    else -> R.string.stats_best_format_other
}

private fun theirBestFormatRes(format: String): Int = when (format) {
    "TV" -> R.string.stats_best_format_tv_theirs
    "TV_SHORT" -> R.string.stats_best_format_tv_short_theirs
    "MOVIE" -> R.string.stats_best_format_movie_theirs
    "SPECIAL" -> R.string.stats_best_format_special_theirs
    "OVA" -> R.string.stats_best_format_ova_theirs
    "ONA" -> R.string.stats_best_format_ona_theirs
    "MUSIC" -> R.string.stats_best_format_music_theirs
    "MANGA" -> R.string.stats_best_format_manga_theirs
    "NOVEL" -> R.string.stats_best_format_novel_theirs
    "ONE_SHOT" -> R.string.stats_best_format_one_shot_theirs
    else -> R.string.stats_best_format_other_theirs
}

/** Titles by how long they run, shortest first, with the titles AniList has no count for noted. */
@Composable
fun LengthsCard(lengths: List<LengthUiModel>, isManga: Boolean, modifier: Modifier = Modifier) {
    val known = lengths.filter { it.label.isNotEmpty() }.sortedBy { lengthStart(it.label) }
    if (known.isEmpty()) return
    val unknown = lengths.filter { it.label.isEmpty() }.sumOf { it.count }
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val max = known.maxOf { it.count }.coerceAtLeast(1)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        known.forEach { length ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lengthLabel(length.label, isManga),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.width(92.dp)
                )
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .height(20.dp)
                ) {
                    Box(
                        Modifier
                            .width((maxWidth * (length.count.toFloat() / max)).coerceAtLeast(6.dp))
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.tertiary)
                    )
                }
                Text(
                    text = formatExactCount(length.count, locale),
                    style = tileLabelStyle().copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = if (length.meanScore > 0f) {
                        stringResource(R.string.stats_genre_mean, formatOneDecimal(length.meanScore.toDouble(), locale))
                    } else {
                        stringResource(R.string.stats_not_scored)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        if (unknown > 0) {
            Text(
                text = pluralStringResourceOrDefault(
                    if (isManga) R.plurals.stats_lengths_unknown_manga else R.plurals.stats_lengths_unknown,
                    unknown,
                    unknown
                ),
                style = statsBodySmall(),
                color = scheme.onSurfaceVariant
            )
        }
    }
}

/** AniList names a length "1", "2-6" or "101+". */
private fun lengthStart(label: String): Int = label.takeWhile { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE

@Composable
private fun lengthLabel(label: String, isManga: Boolean): String {
    val parts = label.trimEnd('+').split('-')
    return when {
        label.endsWith("+") -> stringResource(R.string.stats_length_or_more, parts.first())
        parts.size == 2 -> stringResource(R.string.stats_length_range, parts[0], parts[1])
        else -> {
            val count = parts.first().toIntOrNull() ?: 1
            pluralStringResourceOrDefault(if (isManga) R.plurals.stats_length_chapters else R.plurals.stats_length_episodes, count, count)
        }
    }
}

/** Where the titles come from, the largest first and lit, each with its mean. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountriesCard(countries: List<CountryStat>, modifier: Modifier = Modifier) {
    if (countries.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val sorted = countries.sortedByDescending { it.count }
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sorted.forEachIndexed { index, country ->
            val lead = index == 0
            val container = if (lead) scheme.primaryContainer else scheme.surfaceContainerHigh
            val content = if (lead) scheme.onPrimaryContainer else scheme.onSurface
            val muted = if (lead) scheme.onPrimaryContainer else scheme.onSurfaceVariant
            val countStyle = if (lead) {
                LocalExpressiveTypography.current.titleWide.style(24.sp, 30.sp)
            } else {
                LocalExpressiveTypography.current.titleWide.style(18.sp, 24.sp)
            }
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(container)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(text = countryName(country.countryCode), style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatExactCount(country.count, locale),
                        style = countStyle,
                        color = content,
                        maxLines = 1,
                        modifier = Modifier.lineBox(countStyle)
                    )
                    Text(
                        text = if (country.meanScore > 0f) {
                            stringResource(R.string.stats_genre_mean, formatOneDecimal(country.meanScore.toDouble(), locale))
                        } else {
                            stringResource(R.string.stats_not_scored)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                        color = muted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
