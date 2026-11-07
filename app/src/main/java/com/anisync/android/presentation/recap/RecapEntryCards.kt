package com.anisync.android.presentation.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.presentation.share.cards.CardPicture
import com.anisync.android.presentation.share.cards.cardText
import com.anisync.android.presentation.share.cards.cardUpper
import com.anisync.android.presentation.share.cards.fitted
import com.anisync.android.presentation.statistics.fittedStyle
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.lineBox
import com.anisync.android.ui.theme.rememberSeededColorScheme
import com.anisync.android.ui.theme.shareCardColorScheme
import com.anisync.android.ui.theme.withBlackBackground
import java.time.YearMonth

/** An entry's colours: its top cover's scheme in the app's light or dark and palette style, or the app's own. */
@Composable
internal fun entryScheme(seed: Color?): ColorScheme =
    recapScheme(seed, MaterialTheme.colorScheme, dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f)

/**
 * A recap's colours, [seed]'s scheme or [appScheme] without one. Pure black keeps the regular dark
 * containers under a black background, so the pages' boxes and rows keep their edges.
 */
@Composable
internal fun recapScheme(seed: Color?, appScheme: ColorScheme, dark: Boolean): ColorScheme {
    val appSettings = LocalAppSettings.current
    val paletteStyle by appSettings.paletteStyle.collectAsStateWithLifecycle()
    val amoled by appSettings.amoledEnabled.collectAsStateWithLifecycle()
    if (!(amoled && dark)) {
        return if (seed == null) appScheme else rememberSeededColorScheme(seed, darkTheme = dark, amoled = false, paletteStyle = paletteStyle)
    }
    val base = if (seed == null) shareCardColorScheme(dark = true) else rememberSeededColorScheme(seed, darkTheme = true, amoled = false, paletteStyle = paletteStyle)
    return remember(base) { base.withBlackBackground() }
}

/** The name a card prints for its period, "September" or "2025". */
@Composable
internal fun periodName(period: RecapPeriod): String = when (period) {
    is RecapPeriod.Month -> monthName(period.month.month)
    is RecapPeriod.Year -> period.year.toString()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun cookie(): Shape = MaterialShapes.Cookie9Sided.fitted()

/** The shapes month tiles take in turn, so a grid of months never repeats one side by side. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun tileShape(period: RecapPeriod): Shape {
    val index = when (period) {
        is RecapPeriod.Month -> period.month.monthValue
        is RecapPeriod.Year -> period.year
    }
    return when (index % 6) {
        0 -> MaterialShapes.Cookie9Sided.fitted()
        1 -> MaterialShapes.Flower.fitted()
        2 -> MaterialShapes.Clover8Leaf.fitted()
        3 -> MaterialShapes.Cookie12Sided.fitted()
        4 -> MaterialShapes.Sunny.fitted()
        else -> MaterialShapes.SoftBurst.fitted()
    }
}

@Composable
private fun NewPill(modifier: Modifier = Modifier, weight: FontWeight = FontWeight.Medium) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.recap_new).cardUpper(),
        style = cardText(11, 16, weight, 1f),
        color = scheme.onPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun Eyebrow(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Medium,
    tracking: Float = 1.5f,
) {
    Text(text.cardUpper(), style = cardText(11, 16, weight, tracking), color = color, maxLines = 1, modifier = modifier)
}

@Composable
private fun PlayDisc(size: Dp, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.size(size).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(size / 2))
    }
}

/** "62 episodes · 22 h" under a month, "339 episodes · 68 titles" under a year. */
@Composable
private fun entrySummary(entry: RecapEntry): String = stringResource(
    R.string.filter_chip_value,
    episodesText(entry.episodes),
    if (entry.isYear) titlesText(entry.titles) else wholeHours(entry.minutes),
)

// --- WE1 ---

/** The Library banner: a new recap, a week long, until it is opened or closed. */
@Composable
fun RecapBanner(entry: RecapEntry, onWatch: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val closeLabel = stringResource(R.string.cd_recap_banner_close)
    MaterialTheme(colorScheme = entryScheme(entry.seed)) {
        val scheme = MaterialTheme.colorScheme
        val ink = scheme.onPrimaryContainer
        val heroWide = LocalExpressiveTypography.current.heroWide
        Box(
            modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.primaryContainer)
        ) {
            CardPicture(entry.coverUrl, cookie(), Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = 12.dp).size(112.dp, 110.6.dp))
            entry.secondCoverUrl?.let {
                CardPicture(it, CircleShape, Modifier.align(Alignment.TopEnd).offset(x = (-46).dp, y = 96.dp).size(52.dp))
            }
            Row(Modifier.padding(start = 20.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                NewPill(weight = FontWeight.SemiBold)
                Eyebrow(
                    stringResource(if (entry.isYear) R.string.recap_yearly else R.string.recap_monthly),
                    ink.copy(alpha = 0.8f),
                    Modifier.padding(start = 3.dp),
                    weight = FontWeight.SemiBold,
                    tracking = 1.4f,
                )
            }
            val title = heroWide.style(23.sp, 30.sp, (-0.5).sp)
            Text(
                text = stringResource(R.string.recap_banner_title, periodName(entry.period)),
                style = title,
                color = ink,
                maxLines = 2,
                modifier = Modifier.padding(start = 20.dp, top = 47.dp).width(268.dp).lineBox(title),
            )
            Row(Modifier.padding(start = 20.dp, top = 112.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.primary)
                        .clickable(role = Role.Button, onClick = onWatch)
                        .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.recap_watch), style = cardText(14, 20, FontWeight.Bold), color = scheme.onPrimary)
                }
                Text(
                    text = episodesText(entry.episodes),
                    style = cardText(13, 18, FontWeight.SemiBold),
                    color = ink.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClose)
                    .semantics { contentDescription = closeLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// --- ST1 and ST4 ---

/** An Overview shelf card. [wide] is the tablet pane's 412dp card, with the larger covers ST4 draws. */
@Composable
fun RecapShelfCard(entry: RecapEntry, onClick: () -> Unit, modifier: Modifier = Modifier, wide: Boolean = false) {
    val label = stringResource(R.string.cd_recap_open, periodName(entry.period))
    MaterialTheme(colorScheme = entryScheme(entry.seed)) {
        val scheme = MaterialTheme.colorScheme
        val ink = scheme.onPrimaryContainer
        val heroWide = LocalExpressiveTypography.current.heroWide
        Box(
            modifier
                .height(200.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.primaryContainer)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
        ) {
            if (wide) {
                CardPicture(entry.coverUrl, cookie(), Modifier.align(Alignment.TopEnd).offset(x = (-22).dp, y = 14.dp).size(130.4.dp, 128.7.dp))
                entry.secondCoverUrl?.let { CardPicture(it, CircleShape, Modifier.align(Alignment.TopEnd).offset(x = (-122).dp, y = 80.dp).size(64.dp)) }
            } else {
                CardPicture(entry.coverUrl, cookie(), Modifier.align(Alignment.TopEnd).offset(x = (-19.4).dp, y = 12.dp).size(110.6.dp, 109.2.dp))
                entry.secondCoverUrl?.let { CardPicture(it, CircleShape, Modifier.align(Alignment.TopEnd).offset(x = (-106).dp, y = 66.dp).size(56.dp)) }
            }
            Column(Modifier.padding(start = 20.dp, top = 22.dp)) {
                if (entry.isNew) NewPill(Modifier.padding(bottom = 4.dp))
                Eyebrow(stringResource(if (entry.isYear) R.string.recap_yearly else R.string.recap_monthly), ink.copy(alpha = 0.8f))
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 18.dp)) {
                val style = heroWide.style(28.sp, 40.sp, (-1).sp)
                FrameText(periodName(entry.period), style = style, color = ink, maxLines = 1)
                FrameText(
                    text = entrySummary(entry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.85f),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            PlayDisc(48.dp, Modifier.align(Alignment.BottomEnd).padding(end = if (wide) 20.dp else 16.dp, bottom = if (wide) 20.dp else 12.dp))
        }
    }
}

// --- ST3 ---

/** Overview's card for the month in progress, when no recap is waiting to be opened. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecapInProgressCard(progress: RecapProgress, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val ink = scheme.onSecondaryContainer
    val locale = recapLocale()
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.secondaryContainer)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        MonthRing(progress, ink, ink.copy(alpha = 0.18f), 96.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Eyebrow(stringResource(R.string.recap_in_progress_eyebrow, monthName(progress.month.month)), ink.copy(alpha = 0.75f))
            Text(stringResource(R.string.recap_your_month, monthName(progress.month.month)), style = MaterialTheme.typography.titleLarge, color = ink)
            val opens = stringResource(R.string.recap_opens_on, dayAndMonth(progress.month.plusMonths(1).atDay(1)))
            val finished = if (progress.finished > 0) {
                pluralStringResourceOrDefault(R.plurals.recap_finished_so_far, progress.finished, spelled(progress.finished)).capitalized(locale)
            } else {
                null
            }
            Text(
                text = finished?.let { stringResource(R.string.stats_two_sentences, opens, it) } ?: opens,
                style = MaterialTheme.typography.bodyMedium,
                color = ink.copy(alpha = 0.85f),
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    pluralStringResourceOrDefault(R.plurals.recap_finished_chip, progress.finished, count(progress.finished)),
                    pluralStringResourceOrDefault(R.plurals.recap_updates_chip, progress.updates, count(progress.updates)),
                ).forEach { chip ->
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surface)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** The month's days as a wavy ring, today's date over the month's length in the middle. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MonthRing(progress: RecapProgress, color: Color, track: Color, size: Dp) {
    val days = progress.month.lengthOfMonth()
    val heroWide = LocalExpressiveTypography.current.heroWide
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        CircularWavyProgressIndicator(
            progress = { progress.day.toFloat() / days },
            color = color,
            trackColor = track,
            modifier = Modifier.size(size),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count(progress.day), style = heroWide.style(29.sp, 34.sp), color = color)
            Text(stringResource(R.string.recap_of_days, count(days)), style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

// --- ST5 ---

/** The Recaps view's month in progress, wearing its top cover's colours. */
@Composable
fun RecapThisMonthCard(progress: RecapProgress, modifier: Modifier = Modifier) {
    MaterialTheme(colorScheme = entryScheme(progress.seed)) {
        val scheme = MaterialTheme.colorScheme
        val ink = scheme.onPrimaryContainer
        val heroWide = LocalExpressiveTypography.current.heroWide
        Box(
            modifier
                .fillMaxWidth()
                .height(192.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.primaryContainer)
        ) {
            Box(Modifier.offset(24.dp, 24.dp)) { MonthRing(progress, ink, ink.copy(alpha = 0.22f), 92.dp) }
            CardPicture(
                progress.coverUrl,
                RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = 21.dp).size(60.dp, 83.5.dp),
            )
            progress.secondCoverUrl?.let { CardPicture(it, CircleShape, Modifier.align(Alignment.TopEnd).offset(x = (-62).dp, y = 76.dp).size(44.dp)) }
            Eyebrow(stringResource(R.string.recap_in_progress), ink.copy(alpha = 0.8f), Modifier.padding(start = 140.dp, top = 22.dp).lineBox(cardText(11, 16, FontWeight.Medium, 1.5f)))
            BoxWithConstraints(Modifier.padding(start = 140.dp, top = 40.dp, end = 110.dp)) {
                val name = monthName(progress.month.month)
                FrameText(name, style = fittedStyle(name, heroWide.style(29.sp, 38.sp, (-0.5).sp), maxWidth), color = ink, maxLines = 1)
            }
            val body = MaterialTheme.typography.bodyMedium
            Text(
                text = pluralStringResourceOrDefault(R.plurals.recap_episodes_so_far, progress.episodes, count(progress.episodes)) + "\n" +
                    stringResource(R.string.recap_opens_on, dayAndMonth(progress.month.plusMonths(1).atDay(1))),
                style = body,
                color = ink.copy(alpha = 0.9f),
                // A sentence a line, both shrinking together rather than wrapping into the pill on a narrow card.
                maxLines = 2,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = body.fontSize),
                modifier = Modifier.padding(start = 140.dp, top = 72.dp, end = 110.dp).lineBox(body),
            )
            if (progress.topTitle != null) {
                Text(
                    text = stringResource(
                        R.string.recap_in_progress_pill,
                        titlesText(progress.titles),
                        pluralStringResourceOrDefault(R.plurals.recap_day_count, progress.activeDays, count(progress.activeDays)),
                        progress.topTitle,
                        shortDuration(progress.topMinutes),
                    ),
                    style = cardText(12, 16, FontWeight.SemiBold),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.onPrimary.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .lineBox(cardText(12, 16, FontWeight.SemiBold)),
                )
            }
        }
    }
}

/** A finished month in the Recaps grid. */
@Composable
fun RecapMonthTile(entry: RecapEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.cd_recap_open, periodName(entry.period))
    MaterialTheme(colorScheme = entryScheme(entry.seed)) {
        val scheme = MaterialTheme.colorScheme
        val ink = scheme.onPrimaryContainer
        val heroWide = LocalExpressiveTypography.current.heroWide
        Box(
            modifier
                .height(228.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.primaryContainer)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
        ) {
            CardPicture(entry.coverUrl, tileShape(entry.period), Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = 12.dp).size(96.dp, 112.dp))
            if (entry.isNew) NewPill(Modifier.padding(start = 16.dp, top = 16.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                BoxWithConstraints {
                    val name = periodName(entry.period)
                    val style = fittedStyle(name, heroWide.style(23.sp, 30.sp, (-0.5).sp), maxWidth)
                    FrameText(name, style = style, color = ink, maxLines = 1)
                }
                FrameText(episodesText(entry.episodes), style = cardText(14, 20, FontWeight.Bold), color = ink, modifier = Modifier.padding(top = 2.dp))
                entry.topTitle?.let {
                    FrameText(
                        it,
                        style = cardText(12, 16, FontWeight.Normal),
                        color = ink.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/** A finished year in the Recaps view. */
@Composable
fun RecapYearTile(entry: RecapEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.cd_recap_open, periodName(entry.period))
    MaterialTheme(colorScheme = entryScheme(entry.seed)) {
        val scheme = MaterialTheme.colorScheme
        val ink = scheme.onPrimaryContainer
        val heroWide = LocalExpressiveTypography.current.heroWide
        Box(
            modifier
                .height(200.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.primaryContainer)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
        ) {
            CardPicture(entry.coverUrl, tileShape(entry.period), Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = 14.dp).size(88.dp))
            Column(Modifier.padding(start = 16.dp, top = 18.dp)) {
                if (entry.isNew) NewPill(Modifier.padding(bottom = 4.dp))
                Eyebrow(stringResource(R.string.recap_yearly), ink.copy(alpha = 0.8f))
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 12.dp)) {
                val style = heroWide.style(44.sp, 48.sp, (-1.5).sp)
                FrameText(periodName(entry.period), style = style, color = ink, maxLines = 1)
                FrameText(
                    text = episodesText(entry.episodes) + "\n" + titlesText(entry.titles),
                    style = cardText(13, 18, FontWeight.SemiBold),
                    color = ink.copy(alpha = 0.9f),
                )
            }
            PlayDisc(40.dp, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp))
        }
    }
}

/** The year so far, which opens on 1 January. */
@Composable
fun RecapRunningYearTile(year: RecapRunningYear, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val heroWide = LocalExpressiveTypography.current.heroWide
    Box(
        modifier
            .height(200.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.surfaceContainerLow)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 20.dp)
    ) {
        Column {
            Eyebrow(
                stringResource(R.string.recap_opens_year, dayAndMonth(YearMonth.of(year.year + 1, 1).atDay(1))),
                scheme.onSurfaceVariant,
                Modifier.lineBox(cardText(11, 16, FontWeight.Medium, 1.5f)),
            )
            val style = heroWide.style(44.sp, 48.sp, (-1.5).sp)
            FrameText(year.year.toString(), style = style, color = scheme.primary, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            FrameText(
                pluralStringResourceOrDefault(R.plurals.recap_episodes_so_far_short, year.episodes, count(year.episodes)),
                style = cardText(14, 20, FontWeight.Bold),
                color = scheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        YearBars(year.months, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(56.dp))
    }
}

@Composable
private fun YearBars(months: List<Int>, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val max = months.maxOrNull()?.coerceAtLeast(1) ?: 1
    val busiest = months.indexOf(months.maxOrNull())
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.4.dp), verticalAlignment = Alignment.Bottom) {
        months.forEachIndexed { index, episodes ->
            Box(
                Modifier
                    .width(11.dp)
                    .height((52f * episodes / max).coerceAtLeast(5f).dp)
                    .clip(RoundedCornerShape(5.5.dp))
                    .background(if (index == busiest) scheme.primary else scheme.secondaryContainer)
            )
        }
    }
}
