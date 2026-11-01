package com.anisync.android.presentation.share.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
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
import com.anisync.android.domain.formatOneDecimal
import com.anisync.android.domain.splitMinutes
import com.anisync.android.domain.stats.tasteSpread
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.statistics.fittedStyle
import com.anisync.android.presentation.statistics.labelRes
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.genreColor
import com.anisync.android.ui.theme.lineBox

object StatsBigNumberTemplate : ShareTemplate<ShareSubject.Stats> {
    override val label: Int = R.string.share_template_big_number
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Stats, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl) { StatsBigNumber(subject, spec) }
    }
}

object StatsBentoTemplate : ShareTemplate<ShareSubject.Stats> {
    override val label: Int = R.string.share_template_bento
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Stats, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl) { StatsBento(subject, spec) }
    }
}

object StatsGenresTemplate : ShareTemplate<ShareSubject.Stats> {
    override val label: Int = R.string.share_template_genres
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Stats, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl) { StatsGenres(subject, spec) }
    }
}


// --- Big number ---

private data class BigNumberLayout(
    val banner: Dp,
    val avatar: Dp,
    val avatarStart: Dp,
    val avatarTop: Dp,
    val nameGap: Dp,
    val heroSize: Int,
    val heroTop: Dp,
    val sentenceSize: Int,
    val factsGap: Dp,
)

private fun bigNumberLayout(size: CardSize) = when (size) {
    CardSize.CHAT -> BigNumberLayout(88.dp, 52.dp, 20.dp, 44.dp, 12.dp, 64, 104.dp, 15, 14.dp)
    CardSize.SQUARE -> BigNumberLayout(96.dp, 56.dp, 20.dp, 48.dp, 12.dp, 72, 112.dp, 15, 14.dp)
    CardSize.PORTRAIT -> BigNumberLayout(104.dp, 60.dp, 20.dp, 52.dp, 12.dp, 80, 120.dp, 16, 14.dp)
    CardSize.STORY -> BigNumberLayout(170.dp, 69.dp, 21.dp, 121.dp, 10.dp, 100, 206.dp, 18, 16.dp)
}

@Composable
private fun StatsBigNumber(subject: ShareSubject.Stats, spec: CardSpec) {
    val layout = bigNumberLayout(spec.size)
    val reserve = captionReserve(spec)
    val heroWide = LocalExpressiveTypography.current.heroWide
    val locale = LocalConfiguration.current.locales[0]
    val fixed = spec.size != CardSize.CHAT
    Box(Modifier.fillMaxWidth().then(if (fixed) Modifier.fillMaxSize() else Modifier)) {
        CardPicture(
            url = subject.bannerUrl,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth().height(layout.banner - reserve + 2.dp).fadeToBottom(topAlpha = 0.9f),
        )
        StatsIdentity(
            subject = subject,
            avatar = layout.avatar,
            nameGap = layout.nameGap,
            nameTop = if (spec.size == CardSize.STORY) 15.dp else 10.dp,
            eyebrow = stringResource(if (subject.isManga) R.string.share_stats_all_time_manga else R.string.share_stats_all_time_anime),
            nameSize = 22,
            nameLineHeight = 28,
            modifier = Modifier.padding(start = layout.avatarStart, top = layout.avatarTop - reserve),
        )
        Column(Modifier.fillMaxWidth().then(if (fixed) Modifier.fillMaxSize() else Modifier).padding(top = layout.heroTop - reserve)) {
            val heroText = formatExactCount(subject.count, locale)
            val heroStyle = fittedStyle(heroText, heroWide.style(layout.heroSize.sp, layout.heroSize.sp, (-3).sp), 320.dp)
            Text(
                text = heroText,
                style = heroStyle,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(start = 20.dp).lineBox(heroStyle),
            )
            Text(
                text = statsSentence(subject, spec),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = layout.sentenceSize.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp),
            )
            StatsFacts(subject, spec, Modifier.padding(start = 24.dp, end = 24.dp, top = layout.factsGap))
            CardCaption(
                spec = spec,
                style = cardText(15, 20, FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
            )
            if (spec.size == CardSize.STORY && spec.shows(CardDetail.FAVOURITES) && subject.favourites.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.share_fav_eyebrow).cardUpper(),
                    style = cardEyebrow(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp),
                )
                CardCollage(
                    covers = subject.favourites,
                    slots = StripCollage,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp).size(312.dp, 92.dp),
                )
            }
            if (spec.size == CardSize.PORTRAIT || spec.size == CardSize.STORY) {
                StatsTopTiles(
                    subject = subject,
                    spec = spec,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = if (spec.size == CardSize.STORY) 0.dp else 10.dp,
                    ),
                )
            }
            if (fixed) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(16.dp))
            CardBrand(
                handle = subject.handle,
                spec = spec,
                container = Color.Transparent,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(if (spec.size == CardSize.CHAT) 20.dp else if (spec.size == CardSize.STORY) 0.dp else 16.dp))
        }
    }
}

@Composable
private fun StatsFacts(subject: ShareSubject.Stats, spec: CardSpec, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val taste = tasteSpread(subject.standardDeviation.toDouble())
    val facts = buildList {
        add(
            formatExactCount(subject.units, locale) to
                stringResource(if (subject.isManga) R.string.share_media_chapters else R.string.share_media_episodes)
        )
        if (spec.shows(CardDetail.SCORE) && subject.meanScore > 0f) {
            add(formatOneDecimal(subject.meanScore.toDouble(), locale) to stringResource(R.string.share_stats_mean_score))
        }
        if (spec.shows(CardDetail.TASTE) && taste != null) {
            add(stringResource(taste.labelRes()) to stringResource(R.string.share_stats_taste))
        }
    }
    val titleWide = LocalExpressiveTypography.current.titleWide
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        facts.forEach { (value, label) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(10.dp)
            ) {
                BoxWithConstraints {
                    Text(value, style = fittedStyle(value, titleWide.style(18.sp, 24.sp), maxWidth), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                }
                Text(
                    text = label,
                    style = cardText(11, 16, FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The top genre in its genre colours beside the top studio. */
@Composable
private fun StatsTopTiles(subject: ShareSubject.Stats, spec: CardSpec, modifier: Modifier = Modifier) {
    val genre = subject.genres.firstOrNull()?.takeIf { spec.shows(CardDetail.GENRES) }
    val studio = subject.topStudio?.takeIf { spec.shows(CardDetail.STUDIO) }
    if (genre == null && studio == null) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (genre != null) {
            val colors = genreColor(genre.genre)
            SmallTile(
                value = genre.genre,
                label = stringResource(R.string.share_stats_top_genre),
                container = colors?.container ?: MaterialTheme.colorScheme.secondaryContainer,
                content = colors?.content ?: MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
        if (studio != null) {
            SmallTile(
                value = studio,
                label = stringResource(R.string.share_stats_top_studio),
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SmallTile(value: String, label: String, container: Color, content: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, style = cardText(15, 20, FontWeight.ExtraBold), color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = label,
            style = cardText(11, 14, FontWeight.Medium),
            color = content.copy(alpha = 0.8f),
            maxLines = 1,
        )
    }
}


// --- Bento ---

@Composable
private fun StatsBento(subject: ShareSubject.Stats, spec: CardSpec) {
    val size = spec.size
    val story = size == CardSize.STORY
    val locale = LocalConfiguration.current.locales[0]
    Box(Modifier.fillMaxWidth().padding(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsIdentity(
                subject = subject,
                avatar = 53.dp,
                nameGap = 10.dp,
                nameTop = 5.dp,
                eyebrow = stringResource(if (subject.isManga) R.string.share_stats_eyebrow_manga else R.string.share_stats_eyebrow_anime),
                nameSize = 18,
                nameLineHeight = 24,
                modifier = Modifier.padding(start = 1.dp, top = 1.dp).height(55.dp),
            )
            if (story) {
                BentoTotal(subject, spec, Modifier.fillMaxWidth().height(130.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BentoFigure(formatExactCount(subject.units, locale), unitsLabel(subject), Modifier.weight(1f).height(80.dp), 52.dp)
                    if (spec.shows(CardDetail.SCORE)) {
                        BentoFigure(
                            formatOneDecimal(subject.meanScore.toDouble(), locale),
                            stringResource(R.string.share_stats_mean_score),
                            Modifier.weight(1f).height(80.dp),
                            52.dp,
                        )
                    }
                }
            } else {
                Row(Modifier.height(144.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BentoTotal(subject, spec, Modifier.width(200.dp).fillMaxSize())
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoFigure(formatExactCount(subject.units, locale), unitsLabel(subject), Modifier.fillMaxWidth().height(68.dp), 40.dp)
                        if (spec.shows(CardDetail.SCORE)) {
                            BentoFigure(
                                formatOneDecimal(subject.meanScore.toDouble(), locale),
                                stringResource(R.string.share_stats_mean_score),
                                Modifier.fillMaxWidth().height(68.dp),
                                40.dp,
                            )
                        }
                    }
                }
            }
            if (size != CardSize.CHAT) {
                Row(Modifier.height(120.dp - captionReserve(spec)), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val genre = subject.genres.firstOrNull()?.takeIf { spec.shows(CardDetail.GENRES) }
                    if (genre != null) BentoGenreTile(genre, Modifier.weight(if (story) 1f else 160f).fillMaxSize())
                    if (spec.shows(CardDetail.STUDIO) && subject.topStudio != null) {
                        BentoStudioTile(subject, compact = captionReserve(spec) > 0.dp, modifier = Modifier.weight(if (story) 1f else 168f).fillMaxSize())
                    }
                }
            }
            if (size == CardSize.PORTRAIT || story) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val taste = tasteSpread(subject.standardDeviation.toDouble())
                    if (spec.shows(CardDetail.TASTE) && taste != null) {
                        BentoFigure(
                            value = stringResource(taste.labelRes()),
                            label = stringResource(
                                R.string.stats_tile_taste,
                                stringResource(R.string.stats_tile_taste_word).cardUpper(),
                                stringResource(R.string.stats_sigma),
                                formatOneDecimal(subject.standardDeviation.toDouble(), locale),
                            ),
                            modifier = Modifier.weight(if (story) 1f else 160f).height(if (story) 80.dp else 82.dp),
                            labelTop = if (story) 52.dp else 54.dp,
                            upper = false,
                        )
                    }
                    BentoFigure(
                        value = formatExactCount(subject.completed, locale),
                        label = stringResource(R.string.share_recap_completed),
                        modifier = Modifier.weight(if (story) 1f else 168f).height(if (story) 80.dp else 82.dp),
                        labelTop = if (story) 52.dp else 54.dp,
                    )
                }
            }
            if (story && spec.shows(CardDetail.FAVOURITES) && subject.favourites.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(start = 12.dp, top = 7.dp)
                ) {
                    CardCollage(subject.favourites, StripCollage, Modifier.size(312.dp, 96.dp))
                }
            }
            CardCaption(spec, cardText(15, 20, FontWeight.SemiBold), MaterialTheme.colorScheme.onSurface)
        }
        CardMark(Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 8.dp))
    }
}

@Composable
private fun BentoTotal(subject: ShareSubject.Stats, spec: CardSpec, modifier: Modifier) {
    val heroWide = LocalExpressiveTypography.current.heroWide
    val locale = LocalConfiguration.current.locales[0]
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        val total = formatExactCount(subject.count, locale)
        val style = fittedStyle(total, heroWide.style(52.sp, 56.sp, (-1.5).sp), 168.dp)
        Text(
            text = total,
            style = style,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp).lineBox(style),
        )
        Text(
            text = stringResource(if (subject.isManga) R.string.share_stats_unit_manga else R.string.share_stats_unit_anime).cardUpper(),
            style = cardEyebrow(),
            color = content.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 18.dp, top = 84.dp),
        )
        val second = if (subject.isManga) {
            pluralStringResourceOrDefault(R.plurals.share_media_chapter_count, subject.units, formatExactCount(subject.units, locale))
        } else if (spec.shows(CardDetail.TIME)) {
            subject.minutes?.let { statsDuration(it, R.string.share_duration_list) }
        } else {
            null
        }
        if (second != null) {
            Text(
                text = second,
                style = cardText(13, 18, FontWeight.Medium),
                color = content.copy(alpha = 0.9f),
                maxLines = 1,
                modifier = Modifier.padding(start = 18.dp, top = 104.dp),
            )
        }
    }
}

@Composable
private fun BentoFigure(value: String, label: String, modifier: Modifier, labelTop: Dp, upper: Boolean = true) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = value,
            style = fittedStyle(value, titleWide.style(22.sp, 26.sp), maxWidth),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = if (upper) label.cardUpper() else label,
            style = cardEyebrow(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = labelTop),
        )
    }
}

@Composable
private fun BentoGenreTile(genre: GenreCount, modifier: Modifier) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    val locale = LocalConfiguration.current.locales[0]
    val colors = genreColor(genre.genre)
    val content = colors?.content ?: MaterialTheme.colorScheme.onSecondaryContainer
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors?.container ?: MaterialTheme.colorScheme.secondaryContainer)
            .padding(start = 16.dp, end = 12.dp, top = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.share_stats_top_genre).cardUpper(),
            style = cardEyebrow(),
            color = content.copy(alpha = 0.8f),
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        Text(genre.genre, style = titleWide.style(26.sp, 30.sp), color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(
            text = pluralStringResourceOrDefault(R.plurals.share_stats_title_count, genre.count, formatExactCount(genre.count, locale)),
            style = cardText(12, 16, FontWeight.Medium),
            color = content.copy(alpha = 0.85f),
            maxLines = 1,
        )
    }
}

@Composable
private fun BentoStudioTile(subject: ShareSubject.Stats, compact: Boolean, modifier: Modifier) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 12.dp, top = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.share_stats_top_studio).cardUpper(),
            style = cardEyebrow(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subject.topStudio.orEmpty(),
            style = titleWide.style(20.sp, 24.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(if (compact) 24.dp else 48.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.share_stats_studio_line,
                pluralStringResourceOrDefault(R.plurals.share_stats_title_count, subject.topStudioCount, formatExactCount(subject.topStudioCount, locale)),
                formatOneDecimal(subject.topStudioMean.toDouble(), locale),
            ),
            style = cardText(12, 16, FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}


// --- Genres ---

private data class GenresLayout(
    val avatar: Dp,
    val avatarStart: Dp,
    val avatarTop: Dp,
    val nameTop: Dp,
    val titleSize: Int,
    val barsTop: Dp,
    val barGap: Dp,
    val count: Int,
)

private fun genresLayout(size: CardSize) = when (size) {
    CardSize.CHAT -> GenresLayout(48.dp, 20.dp, 20.dp, 4.dp, 20, 88.dp, 6.dp, 3)
    CardSize.SQUARE -> GenresLayout(57.dp, 21.dp, 21.dp, 9.dp, 20, 96.dp, 6.dp, 5)
    CardSize.PORTRAIT -> GenresLayout(57.dp, 21.dp, 21.dp, 9.dp, 20, 100.dp, 10.dp, 6)
    CardSize.STORY -> GenresLayout(64.dp, 24.dp, 32.dp, 8.dp, 22, 120.dp, 12.dp, 9)
}

@Composable
private fun StatsGenres(subject: ShareSubject.Stats, spec: CardSpec) {
    val layout = genresLayout(spec.size)
    val fixed = spec.size != CardSize.CHAT
    val genres = subject.genres.take(layout.count - if (captionReserve(spec) > 0.dp) 1 else 0)
    val max = genres.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Box(Modifier.fillMaxWidth().then(if (fixed) Modifier.fillMaxSize() else Modifier)) {
        Column(Modifier.fillMaxWidth().then(if (fixed) Modifier.fillMaxSize() else Modifier)) {
            StatsIdentity(
                subject = subject,
                avatar = layout.avatar,
                nameGap = if (spec.size == CardSize.STORY) 12.dp else 10.dp,
                nameTop = layout.nameTop,
                title = stringResource(R.string.share_stats_genres_title, subject.name),
                eyebrow = stringResource(if (subject.isManga) R.string.share_stats_by_titles_read else R.string.share_stats_by_titles_watched),
                nameSize = layout.titleSize,
                nameLineHeight = 26,
                modifier = Modifier.padding(start = layout.avatarStart, top = layout.avatarTop),
            )
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = layout.barsTop - layout.avatarTop - layout.avatar),
                verticalArrangement = Arrangement.spacedBy(layout.barGap),
            ) {
                genres.forEachIndexed { index, genre ->
                    GenreBar(rank = index + 1, genre = genre, fraction = genre.count.toFloat() / max)
                }
            }
            CardCaption(
                spec = spec,
                style = cardText(15, 20, FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
            )
            if (spec.size != CardSize.SQUARE) {
                if (fixed) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(16.dp))
                CardBrand(
                    handle = subject.handle,
                    spec = spec,
                    container = Color.Transparent,
                    modifier = Modifier.padding(start = if (spec.size == CardSize.CHAT) 20.dp else 24.dp),
                )
                Spacer(Modifier.height(if (spec.size == CardSize.PORTRAIT) 6.dp else if (spec.size == CardSize.CHAT) 20.dp else 16.dp))
            }
        }
        if (spec.size == CardSize.SQUARE) CardMark(Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 20.dp))
    }
}

/** A genre as a pill whose length is its share of the top genre, never shorter than its own label. */
@Composable
private fun GenreBar(rank: Int, genre: GenreCount, fraction: Float) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    val locale = LocalConfiguration.current.locales[0]
    val colors = genreColor(genre.genre)
    val container = colors?.container ?: MaterialTheme.colorScheme.secondaryContainer
    val content = colors?.content ?: MaterialTheme.colorScheme.onSecondaryContainer
    Layout(
        content = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(content), contentAlignment = Alignment.Center) {
                    Text(rank.toString(), style = cardText(13, 18, FontWeight.ExtraBold), color = container)
                }
                Text(genre.genre, style = cardText(14, 20, FontWeight.Bold), color = content, maxLines = 1)
            }
            Text(formatExactCount(genre.count, locale), style = titleWide.style(14.sp, 20.sp), color = content, maxLines = 1)
        },
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(container),
    ) { measurables, constraints ->
        val start = 8.dp.roundToPx()
        val end = 14.dp.roundToPx()
        val gap = 10.dp.roundToPx()
        val height = 40.dp.roundToPx()
        val label = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val count = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val needed = start + label.width + gap + count.width + end
        val width = maxOf(needed, (constraints.maxWidth * fraction).toInt()).coerceAtMost(constraints.maxWidth)
        layout(width, height) {
            label.place(start, (height - label.height) / 2)
            count.place(width - end - count.width, (height - count.height) / 2)
        }
    }
}


// --- Shared ---

/** The avatar beside a name and an eyebrow, how every stats card opens. */
@Composable
private fun StatsIdentity(
    subject: ShareSubject.Stats,
    avatar: Dp,
    nameGap: Dp,
    nameTop: Dp,
    eyebrow: String,
    nameSize: Int,
    nameLineHeight: Int,
    modifier: Modifier = Modifier,
    title: String = subject.name,
) {
    Row(modifier) {
        UserAvatar(contentDescription = null, size = avatar, url = subject.avatarUrl, showFrame = true)
        Spacer(Modifier.width(nameGap))
        Column(Modifier.padding(top = nameTop)) {
            Text(
                text = title,
                style = cardText(nameSize, nameLineHeight, FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = eyebrow.cardUpper(),
                style = cardEyebrow(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun unitsLabel(subject: ShareSubject.Stats): String =
    stringResource(if (subject.isManga) R.string.share_media_chapters else R.string.share_media_episodes)

/** "anime, 168 days and 9 hours of them", or the chapters for manga. */
@Composable
private fun statsSentence(subject: ShareSubject.Stats, spec: CardSpec): String {
    val locale = LocalConfiguration.current.locales[0]
    if (subject.isManga) {
        return pluralStringResourceOrDefault(R.plurals.share_stats_sentence_manga, subject.units, formatExactCount(subject.units, locale))
    }
    val minutes = subject.minutes
    return if (spec.shows(CardDetail.TIME) && minutes != null && minutes > 0) {
        stringResource(R.string.share_stats_sentence_anime, statsDuration(minutes, R.string.share_duration_and))
    } else {
        pluralStringResourceOrDefault(R.plurals.stats_unit_anime, subject.count)
    }
}

/** Days and the hours left over, joined by [joiner], or hours alone under a day. */
@Composable
private fun statsDuration(minutes: Int, joiner: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    val parts = splitMinutes(minutes)
    val days = pluralStringResourceOrDefault(R.plurals.share_duration_days, parts.days, formatExactCount(parts.days, locale))
    val hours = pluralStringResourceOrDefault(R.plurals.share_duration_hours, parts.hours, formatExactCount(parts.hours, locale))
    return when {
        parts.days > 0 && parts.hours > 0 -> stringResource(joiner, days, hours)
        parts.days > 0 -> days
        else -> hours
    }
}
