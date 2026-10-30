package com.anisync.android.presentation.share.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.data.CardSize
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.formatCommunityCount
import com.anisync.android.domain.formatExactCount
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.lineBox

object MediaPosterTemplate : ShareTemplate<ShareSubject.Media> {
    override val label: Int = R.string.share_template_hero
    override val defaultBackground: CardBackground = CardBackground.ARTWORK_BLUR

    @Composable
    override fun Card(subject: ShareSubject.Media, spec: CardSpec) {
        ShareCardScaffold(spec, subject.coverUrl) {
            when (spec.size) {
                CardSize.CHAT -> PosterChat(subject, spec)
                CardSize.SQUARE -> PosterBeside(subject, spec, PosterSquare)
                CardSize.PORTRAIT -> PosterBeside(subject, spec, PosterPortrait)
                CardSize.STORY -> PosterStory(subject, spec)
            }
        }
    }
}

object MediaBentoTemplate : ShareTemplate<ShareSubject.Media> {
    override val label: Int = R.string.share_template_bento
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Media, spec: CardSpec) {
        ShareCardScaffold(spec, subject.coverUrl) {
            MediaBento(subject, spec)
        }
    }
}

object MediaCardTemplate : ShareTemplate<ShareSubject.Media> {
    override val label: Int = R.string.share_template_standard
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Media, spec: CardSpec) {
        ShareCardScaffold(
            spec = spec,
            artworkUrl = subject.coverUrl,
            solid = MaterialTheme.colorScheme.surfaceContainer,
            corner = ChatCorner,
        ) {
            MediaCard(subject, spec)
        }
    }
}


// --- Poster ---

@Composable
private fun PosterChat(subject: ShareSubject.Media, spec: CardSpec) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Row {
            PosterCover(subject.coverUrl, Modifier.size(96.dp, 136.dp), RoundedCornerShape(16.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = titleWide.style(20.sp, 34.sp, (-0.5).sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                MediaMeta(subject, spec, allStudios = true, maxLines = 2)
                if (showsStatus(spec)) {
                    Spacer(Modifier.height(12.dp))
                    StatusPill(subject)
                }
            }
        }
        PosterFacts(subject, spec, Modifier.padding(top = 16.dp))
        CardCaption(spec, posterCaptionStyle(), MaterialTheme.colorScheme.onSurface, Modifier.padding(top = 12.dp))
        if (showsProgress(subject, spec)) {
            Spacer(Modifier.height(16.dp))
            PosterProgress(subject)
            Spacer(Modifier.height(10.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
        CardBrand(
            handle = subject.handle,
            spec = spec,
            container = Color.Black.copy(alpha = 0.28f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** The cover beside the title, as Square and Portrait lay it out. */
private data class PosterBesideSpec(
    val coverWidth: Dp,
    val coverHeight: Dp,
    val coverCorner: Dp,
    val titleTop: Dp,
    val metaGap: Dp,
    val progressBeside: Boolean,
    val factsTop: Dp,
)

private val PosterSquare = PosterBesideSpec(112.dp, 159.dp, 18.dp, (-2).dp, 6.dp, true, 30.dp)
private val PosterPortrait = PosterBesideSpec(140.dp, 199.dp, 20.dp, 2.dp, 8.dp, false, 33.dp)

@Composable
private fun PosterBeside(subject: ShareSubject.Media, spec: CardSpec, layout: PosterBesideSpec) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row {
            CoverWithStatus(
                subject = subject,
                spec = spec,
                width = layout.coverWidth,
                height = layout.coverHeight,
                corner = layout.coverCorner,
                pillInset = 16.dp,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).padding(top = layout.titleTop.coerceAtLeast(0.dp)).offset(y = layout.titleTop.coerceAtMost(0.dp))) {
                Text(
                    text = subject.name,
                    style = titleWide.style(22.sp, 34.sp, (-0.5).sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(layout.metaGap))
                MediaMeta(subject, spec, allStudios = true, maxLines = 2)
                if (layout.progressBeside && showsProgress(subject, spec)) {
                    Spacer(Modifier.height(14.dp))
                    PosterProgress(subject)
                }
            }
        }
        PosterFacts(subject, spec, Modifier.padding(top = layout.factsTop - 14.dp))
        CardCaption(spec, posterCaptionStyle(), MaterialTheme.colorScheme.onSurface, Modifier.padding(top = 12.dp))
        if (!layout.progressBeside && showsProgress(subject, spec)) {
            Spacer(Modifier.height(16.dp))
            PosterProgress(subject)
        }
        Spacer(Modifier.weight(1f))
        CardBrand(
            handle = subject.handle,
            spec = spec,
            container = Color.Black.copy(alpha = 0.28f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun PosterStory(subject: ShareSubject.Media, spec: CardSpec) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(44.dp))
        CoverWithStatus(subject, spec, width = 196.dp, height = 278.dp, corner = 24.dp, pillInset = 20.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = subject.name,
            style = titleWide.style(30.sp, 34.sp, (-0.5).sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        MediaMeta(subject, spec, allStudios = true, maxLines = 1, center = true)
        val hasCaption = spec.caption.isNotBlank()
        PosterFacts(subject, spec, Modifier.padding(top = if (hasCaption) 4.dp else 10.dp))
        CardCaption(
            spec = spec,
            style = posterCaptionStyle(),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        if (showsProgress(subject, spec)) {
            PosterProgress(subject)
            Spacer(Modifier.height(16.dp))
        }
        CardBrand(handle = subject.handle, spec = spec, container = Color.Black.copy(alpha = 0.28f))
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
@ReadOnlyComposable
private fun posterCaptionStyle() = cardText(15, 20, FontWeight.SemiBold).copy(textAlign = TextAlign.Center)

@Composable
private fun PosterCover(url: String?, modifier: Modifier, shape: RoundedCornerShape) {
    CardPicture(
        url = url,
        shape = shape,
        modifier = modifier.dropShadow(
            shape,
            Shadow(radius = 36.dp, color = Color.Black.copy(alpha = 0.5f), offset = DpOffset(0.dp, 18.dp)),
        ),
    )
}

/** The cover with the list status set over its bottom edge, centred. */
@Composable
private fun CoverWithStatus(
    subject: ShareSubject.Media,
    spec: CardSpec,
    width: Dp,
    height: Dp,
    corner: Dp,
    pillInset: Dp,
) {
    val showStatus = showsStatus(spec)
    Box(modifier = Modifier.width(width).height(if (showStatus) height - pillInset + StatusPillHeight else height)) {
        PosterCover(subject.coverUrl, Modifier.size(width, height), RoundedCornerShape(corner))
        if (showStatus) {
            StatusPill(
                subject = subject,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = height - pillInset),
            )
        }
    }
}

@Composable
private fun PosterFacts(subject: ShareSubject.Media, spec: CardSpec, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val facts = buildList {
        if (spec.shows(CardDetail.SCORE)) subject.score?.let { add(it to R.string.share_media_score) }
        if (spec.shows(CardDetail.POPULARITY)) {
            subject.popularity?.let { add(formatCommunityCount(it, locale) to R.string.share_media_popularity) }
        }
        if (spec.shows(CardDetail.FAVOURITES)) {
            subject.favourites?.let { add(formatCommunityCount(it, locale) to R.string.share_media_favourites) }
        }
    }
    if (facts.isEmpty()) return
    val titleWide = LocalExpressiveTypography.current.titleWide
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        facts.forEach { (value, label) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = value,
                    style = titleWide.style(22.sp, 28.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(label),
                    style = cardText(11, 16, FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PosterProgress(subject: ShareSubject.Media) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = progressLine(subject),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.4.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        val fraction = progressFraction(subject)
        if (fraction != null) {
            Spacer(Modifier.height(4.dp))
            CardProgressBar(fraction, Modifier.fillMaxWidth())
        }
    }
}


// --- Bento ---

@Composable
private fun MediaBento(subject: ShareSubject.Media, spec: CardSpec) {
    val size = spec.size
    val coverWidth = when (size) {
        CardSize.CHAT -> 120.dp
        CardSize.STORY -> 216.dp
        else -> 160.dp
    }
    val topHeight = when (size) {
        CardSize.CHAT -> 170.dp
        CardSize.STORY -> 306.dp
        else -> 232.dp
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(topHeight), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardPicture(subject.coverUrl, RoundedCornerShape(24.dp), Modifier.width(coverWidth).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val showScore = spec.shows(CardDetail.SCORE) && subject.score != null
                val showProgress = showsStatus(spec)
                if (showScore) BentoScoreTile(subject.score.orEmpty(), size, Modifier.weight(if (size == CardSize.CHAT) 80f else 1f))
                if (showProgress) BentoProgressTile(subject, size, Modifier.weight(if (size == CardSize.CHAT) 82f else 1f))
            }
        }
        BentoTitleTile(subject, spec, Modifier.height(if (size == CardSize.STORY) 122.dp else 96.dp))
        if (size == CardSize.PORTRAIT || size == CardSize.STORY) {
            BentoFactTiles(subject, spec, Modifier.height(if (size == CardSize.STORY) 84.dp else 82.dp))
        }
        if (size == CardSize.STORY) BentoAboutTile(subject, spec)
    }
}

@Composable
private fun BentoScoreTile(score: String, size: CardSize, modifier: Modifier) {
    val heroWide = LocalExpressiveTypography.current.heroWide
    val chat = size == CardSize.CHAT
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 16.dp, end = 12.dp),
    ) {
        val style = heroWide.style(if (chat) 32.sp else 48.sp, 52.sp, (-1).sp)
        Text(
            text = score,
            style = style,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            modifier = Modifier.padding(top = if (chat) 10.dp else 14.dp).lineBox(style),
        )
        Text(
            text = stringResource(R.string.share_media_community_score).cardUpper(),
            style = cardEyebrow(1f),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            maxLines = if (size == CardSize.STORY) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = if (chat) 54.dp else 74.dp),
        )
    }
}

@Composable
private fun BentoProgressTile(subject: ShareSubject.Media, size: CardSize, modifier: Modifier) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    val chat = size == CardSize.CHAT
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = statusLabel(subject).cardUpper(),
            style = cardEyebrow(1f),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = if (chat) 12.dp else 16.dp),
        )
        val value = progressFractionText(subject)
        if (value != null) {
            Text(
                text = value,
                style = titleWide.style(if (chat) 22.sp else 28.sp, 34.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.padding(top = if (chat) 30.dp else 36.dp),
            )
        }
        val fraction = progressFraction(subject)
        if (fraction != null) {
            CardProgressBar(
                progress = fraction,
                modifier = if (size == CardSize.STORY) {
                    Modifier.align(Alignment.BottomStart).padding(bottom = 16.dp).width(80.dp)
                } else {
                    Modifier.padding(top = if (chat) 60.dp else 80.dp).fillMaxWidth()
                },
            )
        }
    }
}

@Composable
private fun BentoTitleTile(subject: ShareSubject.Media, spec: CardSpec, modifier: Modifier) {
    val titleWide = LocalExpressiveTypography.current.titleWide
    val story = spec.size == CardSize.STORY
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 8.dp, top = if (story) 16.dp else 14.dp),
    ) {
        Text(
            text = subject.name,
            style = titleWide.style(if (story) 24.sp else 20.sp, 26.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (story) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (spec.shows(CardDetail.GENRES)) {
                    subject.genres.take(2).forEach { genre ->
                        GenreChip(genre, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            if (!story) CardMark()
        }
    }
}

@Composable
private fun BentoFactTiles(subject: ShareSubject.Media, spec: CardSpec, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val facts = buildList {
        if (spec.shows(CardDetail.POPULARITY)) {
            subject.popularity?.let { add(formatCommunityCount(it, locale) to R.string.share_media_popularity) }
        }
        if (spec.shows(CardDetail.FAVOURITES)) {
            subject.favourites?.let { add(formatCommunityCount(it, locale) to R.string.share_media_favourites) }
        }
    }
    if (facts.isEmpty()) return
    val heroWide = LocalExpressiveTypography.current.heroWide
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        facts.forEach { (value, label) ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp)
            ) {
                val style = heroWide.style(28.sp, 52.sp, (-1).sp)
                Text(
                    text = value,
                    style = style,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 14.dp).lineBox(style),
                )
                Text(
                    text = stringResource(label).cardUpper(),
                    style = cardEyebrow(1f),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    modifier = Modifier.padding(top = if (spec.size == CardSize.STORY) 54.dp else 52.dp),
                )
            }
        }
    }
}

@Composable
private fun BentoAboutTile(subject: ShareSubject.Media, spec: CardSpec) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 10.dp, end = 16.dp, top = 14.dp),
    ) {
        MediaMeta(subject, spec, allStudios = true, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
        Spacer(Modifier.height(4.dp))
        CardBrand(handle = subject.handle, spec = spec, container = Color.Black.copy(alpha = 0.28f))
    }
}


// --- Card ---

@Composable
private fun MediaCard(subject: ShareSubject.Media, spec: CardSpec) {
    val size = spec.size
    val story = size == CardSize.STORY
    Column(Modifier.fillMaxWidth().then(if (size == CardSize.CHAT) Modifier else Modifier.fillMaxHeight())) {
        CardHeader(subject, spec)
        val bodyModifier = if (size == CardSize.CHAT) Modifier else Modifier.weight(1f)
        Column(
            modifier = bodyModifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = if (story) 24.dp else 12.dp, bottom = 16.dp),
            verticalArrangement = when (size) {
                CardSize.SQUARE -> Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
                CardSize.PORTRAIT -> Arrangement.spacedBy(12.dp)
                CardSize.STORY -> Arrangement.spacedBy(18.dp)
                CardSize.CHAT -> Arrangement.spacedBy(14.dp)
            },
        ) {
            if (size != CardSize.SQUARE) {
                MediaMeta(
                    subject = subject,
                    spec = spec,
                    allStudios = false,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.3.sp),
                )
                CardFacts(subject)
            }
            CardCaption(spec, MaterialTheme.typography.bodyMedium, MaterialTheme.colorScheme.onSurface)
            if (spec.shows(CardDetail.PROGRESS)) CardStatus(subject)
            if (size == CardSize.CHAT || story) {
                if (spec.shows(CardDetail.GENRES) && subject.genres.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subject.genres.take(3).forEach { genre ->
                            GenreChip(genre, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CardBrand(handle = subject.handle, spec = spec, container = MaterialTheme.colorScheme.surfaceContainerHigh)
            Text(
                text = stringResource(R.string.share_card_source),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CardHeader(subject: ShareSubject.Media, spec: CardSpec) {
    val story = spec.size == CardSize.STORY
    val bannerHeight = if (story) 220.dp else 132.dp
    val coverWidth = if (story) 112.dp else 88.dp
    val coverHeight = if (story) 158.dp else 124.dp
    Box(Modifier.fillMaxWidth().height(if (story) 300.dp else 196.dp)) {
        CardPicture(
            url = subject.bannerUrl ?: subject.coverUrl,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth().height(bannerHeight).fadeToBottom(),
        )
        CardPicture(
            url = subject.coverUrl,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(start = 20.dp, top = bannerHeight - 60.dp)
                .size(coverWidth, coverHeight)
                .border(3.dp, MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
        )
        if (spec.shows(CardDetail.SCORE) && subject.score != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 34.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = subject.score,
                    style = cardText(13, 18, FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = subject.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp + coverWidth + 14.dp, end = 20.dp, top = bannerHeight + 8.dp),
        )
    }
}

@Composable
private fun CardFacts(subject: ShareSubject.Media) {
    val locale = LocalConfiguration.current.locales[0]
    val facts = buildList {
        subject.popularity?.let { add(formatCommunityCount(it, locale) to R.string.share_detail_popularity) }
        subject.favourites?.let { add(formatCommunityCount(it, locale) to R.string.share_detail_favourites) }
        subject.total?.takeIf { it > 0 }?.let {
            add(formatExactCount(it, locale) to if (subject.isManga) R.string.share_stats_chapters else R.string.share_stats_episodes)
        }
    }
    if (facts.isEmpty()) return
    val titleWide = LocalExpressiveTypography.current.titleWide
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        facts.forEach { (value, label) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(value, style = titleWide.style(20.sp, 26.sp), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(
                    text = stringResource(label),
                    style = cardText(11, 16, FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CardStatus(subject: ShareSubject.Media) {
    val content = MaterialTheme.colorScheme.onSecondaryContainer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(statusLabel(subject), style = cardText(14, 20, FontWeight.Bold), color = content, maxLines = 1)
            cardProgressCount(subject)?.let { count ->
                Text(
                    text = count,
                    style = cardText(14, 20, FontWeight.Medium, 0.4f),
                    color = content,
                    maxLines = 1,
                )
            }
        }
        if (subject.listStatus != null) progressFraction(subject)?.let { CardProgressBar(it, Modifier.fillMaxWidth()) }
    }
}


// --- Shared pieces ---

private val StatusPillHeight = 30.dp

@Composable
private fun StatusPill(subject: ShareSubject.Media, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(StatusPillHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = statusIcon(subject.listStatus),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = statusLabel(subject),
            style = cardText(13, 18, FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

@Composable
private fun GenreChip(genre: String, container: Color, content: Color) {
    Text(
        text = genre,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun MediaMeta(
    subject: ShareSubject.Media,
    spec: CardSpec,
    allStudios: Boolean,
    maxLines: Int,
    modifier: Modifier = Modifier,
    center: Boolean = false,
    style: TextStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, letterSpacing = 0.2.sp),
) {
    val parts = buildList {
        subject.format?.let { add(formatLabel(it)) }
        subject.year?.let { add(it.toString()) }
        if (spec.shows(CardDetail.STUDIO) && subject.studios.isNotEmpty()) {
            add(if (allStudios) subject.studios.joinToString(", ") else subject.studios.first())
        }
    }
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString(MetaSeparator),
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = if (center) modifier.fillMaxWidth() else modifier,
    )
}

private const val MetaSeparator = " · "

private fun showsStatus(spec: CardSpec): Boolean = spec.shows(CardDetail.PROGRESS)

private fun showsProgress(subject: ShareSubject.Media, spec: CardSpec): Boolean =
    spec.shows(CardDetail.PROGRESS) && subject.listStatus != null && (subject.progress ?: 0) > 0

private fun progressFraction(subject: ShareSubject.Media): Float? {
    val total = subject.total?.takeIf { it > 0 } ?: return null
    val progress = subject.progress ?: return null
    return progress.toFloat() / total
}

@Composable
private fun progressLine(subject: ShareSubject.Media): String {
    val locale = LocalConfiguration.current.locales[0]
    val progress = formatExactCount(subject.progress ?: 0, locale)
    val total = subject.total?.takeIf { it > 0 }?.let { formatExactCount(it, locale) }
    return when {
        subject.isManga && total != null -> stringResource(R.string.share_progress_chapter_of, progress, total)
        subject.isManga -> stringResource(R.string.share_progress_chapter, progress)
        total != null -> stringResource(R.string.share_progress_episode_of, progress, total)
        else -> stringResource(R.string.share_progress_episode, progress)
    }
}

@Composable
private fun progressFractionText(subject: ShareSubject.Media): String? {
    if (subject.listStatus == null) return null
    val locale = LocalConfiguration.current.locales[0]
    val progress = formatExactCount(subject.progress ?: 0, locale)
    val total = subject.total?.takeIf { it > 0 } ?: return progress
    return stringResource(R.string.share_progress_fraction, progress, formatExactCount(total, locale))
}

/** "3 of 24 episodes" on the list, "24 episodes" for a title the viewer has not added. */
@Composable
private fun cardProgressCount(subject: ShareSubject.Media): String? {
    val locale = LocalConfiguration.current.locales[0]
    val total = subject.total?.takeIf { it > 0 }
    if (subject.listStatus == null) {
        total ?: return null
        val res = if (subject.isManga) R.plurals.share_media_chapter_count else R.plurals.share_media_episode_count
        return pluralStringResourceOrDefault(res, total, formatExactCount(total, locale))
    }
    val progress = subject.progress ?: 0
    return if (total != null) {
        val res = if (subject.isManga) R.plurals.share_progress_chapters_of else R.plurals.share_progress_episodes_of
        pluralStringResourceOrDefault(res, total, formatExactCount(progress, locale), formatExactCount(total, locale))
    } else {
        val res = if (subject.isManga) R.plurals.share_media_chapter_count else R.plurals.share_media_episode_count
        pluralStringResourceOrDefault(res, progress, formatExactCount(progress, locale))
    }
}

/** The viewer's list status, or how the title is releasing when it is not on their list. */
@Composable
private fun statusLabel(subject: ShareSubject.Media): String {
    val status = subject.listStatus
    return if (status != null && status != LibraryStatus.UNKNOWN) {
        stringResource(statusLabelRes(status, subject.isManga))
    } else {
        stringResource(releaseStatusRes(subject.releaseStatus))
    }
}

private fun statusLabelRes(status: LibraryStatus, isManga: Boolean): Int = when (status) {
    LibraryStatus.CURRENT -> if (isManga) R.string.status_reading else R.string.status_watching
    LibraryStatus.REPEATING -> if (isManga) R.string.status_rereading else R.string.status_rewatching
    LibraryStatus.COMPLETED -> R.string.status_completed
    LibraryStatus.PLANNING -> R.string.status_planning
    LibraryStatus.PAUSED -> R.string.status_paused
    LibraryStatus.DROPPED -> R.string.status_dropped
    LibraryStatus.UNKNOWN -> R.string.status_planning
}

private fun releaseStatusRes(status: String): Int = when (status) {
    "RELEASING" -> R.string.share_media_status_airing
    "NOT_YET_RELEASED" -> R.string.share_media_status_upcoming
    "CANCELLED" -> R.string.share_media_status_cancelled
    "HIATUS" -> R.string.share_media_status_hiatus
    else -> R.string.share_media_status_finished
}

private fun statusIcon(status: LibraryStatus?): ImageVector = when (status) {
    LibraryStatus.CURRENT -> Icons.Filled.PlayArrow
    LibraryStatus.REPEATING -> Icons.Filled.Replay
    LibraryStatus.COMPLETED -> Icons.Filled.Check
    LibraryStatus.PAUSED -> Icons.Filled.Pause
    LibraryStatus.DROPPED -> Icons.Filled.Close
    LibraryStatus.PLANNING, LibraryStatus.UNKNOWN, null -> Icons.Filled.Schedule
}

/** AniList's format enum in the app's words, short enough for a meta line. */
@Composable
@ReadOnlyComposable
internal fun formatLabel(format: String): String = when (format) {
    "TV" -> stringResource(R.string.format_tv)
    "TV_SHORT" -> stringResource(R.string.format_tv_short)
    "MOVIE" -> stringResource(R.string.format_movie)
    "SPECIAL" -> stringResource(R.string.format_special)
    "OVA" -> stringResource(R.string.format_ova)
    "ONA" -> stringResource(R.string.format_ona)
    "MUSIC" -> stringResource(R.string.format_music)
    "MANGA" -> stringResource(R.string.media_type_manga)
    "NOVEL" -> stringResource(R.string.format_novel)
    "ONE_SHOT" -> stringResource(R.string.format_one_shot)
    else -> format.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}
