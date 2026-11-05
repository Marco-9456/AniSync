package com.anisync.android.presentation.share.cards

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.data.CardSize
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.splitMinutes
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.genreColor
import com.anisync.android.ui.theme.lineBox

object RecapMonthTemplate : ShareTemplate<ShareSubject.Recap> {
    override val label: Int = R.string.share_template_summary
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Recap, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl) { RecapMonth(subject, spec) }
    }
}

object RecapYearTemplate : ShareTemplate<ShareSubject.Recap> {
    override val label: Int = R.string.share_template_summary
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Recap, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl) { RecapYear(subject, spec) }
    }
}

@Composable
private fun RecapMonth(subject: ShareSubject.Recap, spec: CardSpec) {
    val card = subject.card
    val heroWide = LocalExpressiveTypography.current.heroWide
    val locale = LocalConfiguration.current.locales[0]
    val story = spec.size == CardSize.STORY
    val fixed = spec.size != CardSize.CHAT
    val tight = captionReserve(spec) > 0.dp
    Box(Modifier.fillMaxWidth().then(if (fixed) Modifier.fillMaxSize() else Modifier)) {
        CardCollage(card.covers.take(3), if (story) MonthCoversStory else MonthCovers, Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (fixed) Modifier.fillMaxSize() else Modifier)
                .padding(start = 20.dp, end = 20.dp, top = if (story) 40.dp else 24.dp)
        ) {
            Text(
                text = recapOwner(spec, R.string.share_recap_owner, R.string.recap_monthly, card.userName).cardUpper(),
                style = cardEyebrow(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp),
            )
            val monthStyle = heroWide.style(29.5.sp, 44.sp, (-1).sp)
            Text(
                text = card.monthName.orEmpty(),
                style = monthStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp).lineBox(monthStyle),
            )
            Text(
                text = card.year.toString(),
                style = heroWide.style(29.sp, 44.sp, (-1).sp),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(top = 0.dp).height(38.dp),
            )
            Spacer(Modifier.height(if (story) 100.dp - captionReserve(spec) else if (tight) 4.dp else 16.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val hero = if (tight && !story) heroWide.style(80.sp, 80.sp, (-3).sp) else heroWide.style(96.sp, 96.sp, (-3).sp)
                Text(
                    text = formatExactCount(card.episodes, locale),
                    style = hero,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.lineBox(hero),
                )
                Column(Modifier.padding(bottom = 14.dp)) {
                    Text(
                        text = pluralStringResourceOrDefault(R.plurals.share_recap_episodes_word, card.episodes),
                        style = cardText(18, 24, FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = recapDuration(card.minutes),
                        style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (spec.size != CardSize.PORTRAIT) {
                Spacer(Modifier.height(12.dp))
                RecapCounts(card)
            }
            if ((spec.size == CardSize.PORTRAIT || story) && spec.shows(CardDetail.TOP_TITLES) && card.top.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.share_recap_most_watched).cardUpper(),
                    style = cardEyebrow(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                RecapTop(card.top.take(3), rowHeight = 40.dp, gap = 8.dp, rankSize = 18, cover = 28.dp to 40.dp, coverCorner = 8.dp)
            }
            CardCaption(spec, cardText(15, 20, FontWeight.SemiBold), MaterialTheme.colorScheme.onSurface, Modifier.padding(top = 10.dp))
            if (fixed) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (spec.shows(CardDetail.GENRES)) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        card.genres.take(3).forEach { genre -> RecapGenreChip(genre) }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                CardMark()
            }
            Spacer(Modifier.height(if (spec.size == CardSize.CHAT) 20.dp else if (story) 24.dp else 16.dp))
        }
    }
}

private val MonthCovers = listOf(
    CollageSlot(CollageShape.COOKIE, 271.5.dp, 18.dp, 62.7.dp, 61.9.dp),
    CollageSlot(CollageShape.PILL, 238.dp, 71.9.dp, 34.1.dp, 59.4.dp),
    CollageSlot(CollageShape.CIRCLE, 285.3.dp, 88.4.dp, 41.8.dp, 41.8.dp),
)

private val MonthCoversStory = listOf(
    CollageSlot(CollageShape.COOKIE, 227.dp, 30.dp, 114.dp, 112.6.dp),
    CollageSlot(CollageShape.PILL, 166.dp, 128.dp, 62.dp, 108.dp),
    CollageSlot(CollageShape.CIRCLE, 252.dp, 158.dp, 76.dp, 76.dp),
)

@Composable
private fun RecapCounts(card: RecapCard) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    val locale = LocalConfiguration.current.locales[0]
    val counts = listOf(
        card.titles to pluralStringResourceOrDefault(R.plurals.share_recap_titles_word, card.titles),
        card.finished to pluralStringResourceOrDefault(R.plurals.share_recap_finished_word, card.finished),
        card.activeDays to pluralStringResourceOrDefault(R.plurals.stats_active_days, card.activeDays),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        counts.forEach { (value, label) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(formatExactCount(value, locale), style = titleWide.style(22.sp, 26.sp), color = MaterialTheme.colorScheme.onSurface)
                Text(label, style = cardText(11, 16, FontWeight.Medium), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RecapTop(
    titles: List<RecapTitle>,
    rowHeight: Dp,
    gap: Dp,
    rankSize: Int,
    cover: Pair<Dp, Dp>,
    coverCorner: Dp,
) {
    val heroWide = LocalExpressiveTypography.current.heroWide
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        titles.forEachIndexed { index, title ->
            Row(Modifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (index + 1).toString(),
                    style = heroWide.style(rankSize.sp, (rankSize + 4).sp),
                    color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(if (rankSize > 16) 16.dp else 14.dp),
                )
                Spacer(Modifier.width(10.dp))
                CardPicture(title.coverUrl, RoundedCornerShape(coverCorner), Modifier.size(cover.first, cover.second))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title.title,
                        style = cardText(13, 18, FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = recapDuration(title.minutes),
                        style = cardText(11, 14, FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecapGenreChip(genre: String) {
    val colors = genreColor(genre)
    Text(
        text = genre,
        style = cardText(12, 16, FontWeight.Bold),
        color = colors?.content ?: MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors?.container ?: MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun RecapYear(subject: ShareSubject.Recap, spec: CardSpec) {
    val card = subject.card
    val heroWide = LocalExpressiveTypography.current.heroWide
    val locale = LocalConfiguration.current.locales[0]
    val size = spec.size
    val story = size == CardSize.STORY
    val fixed = size != CardSize.CHAT
    val yearSize = when (size) {
        CardSize.CHAT -> 64
        CardSize.SQUARE -> 72
        CardSize.PORTRAIT -> 80
        CardSize.STORY -> 96
    }
    Box(Modifier.fillMaxWidth().then(if (fixed) Modifier.fillMaxSize() else Modifier)) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (fixed) Modifier.fillMaxSize() else Modifier)
                .padding(start = 20.dp, end = 20.dp, top = if (story) 28.dp else if (size == CardSize.CHAT) 20.dp else 24.dp)
        ) {
            Text(
                text = recapOwner(spec, R.string.share_recap_year_owner, R.string.recap_yearly, card.userName).cardUpper(),
                style = cardEyebrow(),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(end = 40.dp),
            )
            val yearStyle = heroWide.style(yearSize.sp, (if (story) 100 else yearSize).sp, (-4).sp)
            Text(
                text = card.year.toString(),
                style = yearStyle,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(start = 0.dp).lineBox(yearStyle),
            )
            Text(
                text = stringResource(
                    R.string.share_duration_list,
                    stringResource(
                        R.string.share_duration_list,
                        pluralStringResourceOrDefault(R.plurals.share_recap_episode_count, card.episodes, formatExactCount(card.episodes, locale)),
                        pluralStringResourceOrDefault(R.plurals.share_recap_hour_count, card.minutes / 60, formatExactCount(card.minutes / 60, locale)),
                    ),
                    pluralStringResourceOrDefault(R.plurals.share_stats_title_count, card.titles, formatExactCount(card.titles, locale)),
                ),
                style = cardText(16, 22, FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(if (captionReserve(spec) > 0.dp) 4.dp else 12.dp))
            MonthBars(card, tall = captionReserve(spec) == 0.dp)
            if (size != CardSize.CHAT && card.busiestMonthName != null) {
                val busiest = card.months.maxOrNull() ?: 0
                Text(
                    text = pluralStringResourceOrDefault(
                        R.plurals.share_recap_busiest_month,
                        busiest,
                        card.busiestMonthName,
                        formatExactCount(busiest, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if ((size == CardSize.PORTRAIT || story) && spec.shows(CardDetail.TOP_TITLES) && card.top.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.share_recap_top_five).cardUpper(),
                    style = cardEyebrow(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                RecapTop(card.top.take(if (story) 5 else 3), rowHeight = 36.dp, gap = 6.dp, rankSize = 16, cover = 26.dp to 36.dp, coverCorner = 7.dp)
            }
            CardCaption(spec, cardText(15, 20, FontWeight.SemiBold), MaterialTheme.colorScheme.onSurface, Modifier.padding(top = 10.dp))
            if (fixed) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(16.dp))
            if (size != CardSize.PORTRAIT) YearTiles(card, spec)
            Spacer(Modifier.height(if (story) 32.dp else 20.dp))
        }
        CardMark(Modifier.align(Alignment.TopEnd).padding(top = if (story) 28.dp else if (size == CardSize.CHAT) 20.dp else 24.dp, end = 20.dp))
    }
}

@Composable
private fun MonthBars(card: RecapCard, tall: Boolean) {
    val max = card.months.maxOrNull()?.coerceAtLeast(1) ?: 1
    val busiest = card.months.indexOf(card.months.maxOrNull())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        card.months.forEachIndexed { index, episodes ->
            val lit = index == busiest
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val barMax = if (tall) 64f else 40f
                Box(Modifier.size(20.dp, (barMax + 2f).dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .size(20.dp, (barMax * episodes / max).coerceAtLeast(4f).dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = card.monthInitials.getOrElse(index) { "" },
                    style = cardText(10, 14, FontWeight.SemiBold),
                    color = if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun YearTiles(card: RecapCard, spec: CardSpec) {
    val locale = LocalConfiguration.current.locales[0]
    val tiles = buildList {
        if (spec.shows(CardDetail.GENRES)) {
            card.topGenre?.let { genre ->
                add(Triple(genre, stringResource(R.string.share_stats_top_genre), genreColor(genre)?.let { it.container to it.content }))
            }
        }
        card.topStudio?.let {
            add(
                Triple(
                    it,
                    stringResource(R.string.share_stats_top_studio),
                    MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
        card.mangaChapters?.let {
            add(
                Triple(
                    formatExactCount(it, locale),
                    pluralStringResourceOrDefault(R.plurals.share_recap_manga_chapters, it),
                    MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    }
    if (tiles.isEmpty()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.forEach { (value, label, colors) ->
            val container = colors?.first ?: MaterialTheme.colorScheme.secondaryContainer
            val content = colors?.second ?: MaterialTheme.colorScheme.onSecondaryContainer
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(container)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(value, style = cardText(14, 18, FontWeight.ExtraBold), color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, style = cardText(10, 14, FontWeight.Medium), color = content.copy(alpha = 0.8f), maxLines = 1)
            }
        }
    }
}

/** Whose recap it is while the Handle detail is on, otherwise only which kind of recap. */
@Composable
private fun recapOwner(spec: CardSpec, @StringRes owned: Int, @StringRes plain: Int, userName: String): String =
    if (spec.shows(CardDetail.HANDLE)) stringResource(owned, userName) else stringResource(plain)

@Composable
private fun recapDuration(minutes: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    val parts = splitMinutes(minutes)
    val hours = parts.days * 24 + parts.hours
    return if (hours > 0) {
        stringResource(R.string.share_recap_hours_minutes, formatExactCount(hours, locale), formatExactCount(parts.minutes, locale))
    } else {
        stringResource(R.string.share_recap_minutes, formatExactCount(parts.minutes, locale))
    }
}
