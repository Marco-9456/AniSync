package com.anisync.android.presentation.share.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.data.CardSize
import com.anisync.android.ui.theme.LocalExpressiveTypography

object FavouritesShelfTemplate : ShareTemplate<ShareSubject.Favourites> {
    override val label: Int = R.string.share_template_shelf
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Favourites, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl) { FavouritesShelf(subject, spec) }
    }
}

object FavouritesCollageTemplate : ShareTemplate<ShareSubject.Favourites> {
    override val label: Int = R.string.share_template_collage
    override val defaultBackground: CardBackground = CardBackground.SOLID

    @Composable
    override fun Card(subject: ShareSubject.Favourites, spec: CardSpec) {
        ShareCardScaffold(spec, subject.artworkUrl, solid = MaterialTheme.colorScheme.primaryContainer) {
            FavouritesCollage(subject, spec)
        }
    }
}

private data class ShelfLayout(
    val padding: Dp,
    val headerTop: Dp,
    val first: Pair<Dp, Dp>,
    val other: Pair<Dp, Dp>,
    /** The four after the first sit in a row under it rather than in a grid beside it. */
    val rowBelow: Boolean,
    val rowBeside: Boolean,
    val list: Boolean,
    val bottom: Dp,
)

private fun shelfLayout(size: CardSize) = when (size) {
    CardSize.CHAT -> ShelfLayout(20.dp, 20.dp, 92.dp to 131.dp, 49.dp to 70.dp, rowBelow = false, rowBeside = true, list = true, bottom = 20.dp)
    CardSize.SQUARE -> ShelfLayout(20.dp, 20.dp, 140.dp to 198.dp, 82.dp to 95.dp, rowBelow = false, rowBeside = false, list = false, bottom = 20.dp)
    CardSize.PORTRAIT -> ShelfLayout(20.dp, 22.dp, 156.dp to 222.dp, 76.dp to 106.dp, rowBelow = false, rowBeside = false, list = true, bottom = 14.dp)
    CardSize.STORY -> ShelfLayout(24.dp, 32.dp, 204.dp to 290.dp, 72.dp to 102.dp, rowBelow = true, rowBeside = false, list = true, bottom = 16.dp)
}

@Composable
private fun FavouritesShelf(subject: ShareSubject.Favourites, spec: CardSpec) {
    val layout = shelfLayout(spec.size)
    val titleWide = LocalExpressiveTypography.current.titleWide
    val fixed = spec.size != CardSize.CHAT
    val entries = subject.entries.take(5)
    val reserve = captionReserve(spec)
    val shrink = (layout.first.second - reserve) / layout.first.second
    val firstSize = layout.first.first * shrink to layout.first.second * shrink
    val otherSize = if (layout.rowBelow) layout.other else layout.other.first * shrink to layout.other.second * shrink
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (fixed) Modifier.fillMaxSize() else Modifier)
            .padding(start = layout.padding, end = layout.padding, top = layout.headerTop)
    ) {
        Text(
            text = stringResource(if (subject.isManga) R.string.share_fav_heading_manga else R.string.share_fav_heading_anime).cardUpper(),
            style = cardEyebrow(),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subject.name,
            style = titleWide.style(30.sp, 36.sp, (-0.5).sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(if (spec.size == CardSize.STORY) 18.dp else if (spec.size == CardSize.PORTRAIT) 14.dp else 10.dp))
        val first = entries.firstOrNull()
        val rest = entries.drop(1)
        when {
            layout.rowBelow -> {
                if (first != null) {
                    RankedCover(first, 1, firstSize, Modifier.align(Alignment.CenterHorizontally))
                }
                if (rest.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rest.forEachIndexed { index, entry -> RankedCover(entry, index + 2, otherSize) }
                    }
                }
            }
            layout.rowBeside -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (first != null) RankedCover(first, 1, firstSize)
                rest.forEachIndexed { index, entry -> RankedCover(entry, index + 2, otherSize) }
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (first != null) RankedCover(first, 1, firstSize)
                Column(verticalArrangement = Arrangement.spacedBy(if (spec.size == CardSize.PORTRAIT) 6.dp else 8.dp)) {
                    rest.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEachIndexed { index, entry -> RankedCover(entry, row * 2 + index + 2, otherSize) }
                        }
                    }
                }
            }
        }
        if (layout.list && spec.shows(CardDetail.RANKING) && entries.isNotEmpty()) {
            Spacer(Modifier.height(if (spec.size == CardSize.STORY) 16.dp else 14.dp))
            RankedList(entries, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CardCaption(
            spec = spec,
            style = cardText(15, 20, FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (fixed) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(16.dp))
        CardBrand(
            handle = subject.handle,
            spec = spec,
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = if (spec.size == CardSize.STORY) Modifier.offset(x = (-4).dp) else Modifier,
        )
        Spacer(Modifier.height(layout.bottom))
    }
}

/** A favourite's cover with its rank on a cookie badge, larger and in primary for the first. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RankedCover(entry: CardImage, rank: Int, size: Pair<Dp, Dp>, modifier: Modifier = Modifier) {
    val first = rank == 1
    Box(modifier.size(size.first, size.second)) {
        CardPicture(entry.url, RoundedCornerShape(if (first) 24.dp else 16.dp), Modifier.fillMaxSize())
        val badge = if (first) 31.6.dp else 21.7.dp
        Box(
            modifier = Modifier
                .padding(if (first) 10.dp else if (size.first > 60.dp) 6.dp else 4.dp)
                .size(badge, badge * 0.9875f)
                .clip(MaterialShapes.Cookie9Sided.fitted())
                .background(if (first) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rank.toString(),
                style = if (first) cardText(15, 19, FontWeight.ExtraBold) else cardText(10, 13, FontWeight.ExtraBold),
                color = if (first) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/** "1. K-ON!   2. K-ON!!   3. K-ON! Movie" over the fourth and fifth. */
@Composable
private fun RankedList(entries: List<CardImage>, color: Color, modifier: Modifier = Modifier) {
    val items = entries.mapIndexed { index, entry -> stringResource(R.string.share_fav_rank_item, index + 1, entry.title) }
    val style = cardText(12, 18, FontWeight.Medium)
    Column(modifier) {
        items.chunked(3).take(2).forEach { line -> RankLine(line, style, color) }
    }
}

/** One line of ranks where a long title gives way first, so every rank on the line stays visible. */
@Composable
private fun RankLine(items: List<String>, style: TextStyle, color: Color) {
    Layout(
        content = {
            items.forEach { Text(it, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
    ) { measurables, constraints ->
        val gap = RankGap.roundToPx()
        val available = (constraints.maxWidth - gap * (measurables.size - 1)).coerceAtLeast(0)
        val natural = measurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }
        val cap = fairShareCap(natural, available)
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints(maxWidth = minOf(natural[index], cap)))
        }
        val width = (placeables.sumOf { it.width } + gap * (placeables.size - 1)).coerceIn(0, constraints.maxWidth)
        layout(width, placeables.maxOfOrNull { it.height } ?: 0) {
            var x = 0
            placeables.forEach {
                it.placeRelative(x, 0)
                x += it.width + gap
            }
        }
    }
}

/** The widest any item may be so that all of [widths] fit in [available], the narrow ones keeping their full width. */
internal fun fairShareCap(widths: List<Int>, available: Int): Int {
    if (widths.sum() <= available) return Int.MAX_VALUE
    var remaining = available
    widths.sorted().forEachIndexed { index, width ->
        val share = remaining / (widths.size - index)
        if (width > share) return share
        remaining -= width
    }
    return Int.MAX_VALUE
}

private val RankGap = 10.dp

/** How much the collage gives up so a caption fits above the brand. */
private const val CollageShrink = 0.9f

private fun collageSlots(size: CardSize): List<CollageSlot> = when (size) {
    CardSize.CHAT -> listOf(
        CollageSlot(CollageShape.COOKIE, 131.2.dp, 82.dp, 99.5.dp, 98.3.dp),
        CollageSlot(CollageShape.CIRCLE, 82.dp, 71.2.dp, 57.6.dp, 57.6.dp),
        CollageSlot(CollageShape.PILL, 234.4.dp, 58.dp, 43.2.dp, 74.4.dp),
        CollageSlot(CollageShape.ARCH, 215.2.dp, 139.6.dp, 62.4.dp, 79.2.dp, bottomCorner = 12.dp),
        CollageSlot(CollageShape.CLOVER, 85.6.dp, 137.2.dp, 54.3.dp, 54.3.dp),
        CollageSlot(CollageShape.SUNNY, 145.6.dp, 179.2.dp, 44.9.dp, 44.9.dp),
    )
    CardSize.SQUARE -> collageAt(92.dp, 74.dp, 52.dp, 188.dp, 184.dp, 254.dp)
    CardSize.PORTRAIT -> collageAt(97.4.dp, 81.5.dp, 62.dp, 182.3.dp, 178.8.dp, 240.7.dp)
    CardSize.STORY -> collageAt(162.dp, 134.1.dp, 100.dp, 310.8.dp, 304.6.dp, 413.1.dp)
}

/** The square's six shapes at their sizes, each moved to its row for the taller cards. */
private fun collageAt(cookie: Dp, circle: Dp, pill: Dp, arch: Dp, clover: Dp, sunny: Dp) = listOf(
    CollageSlot(CollageShape.COOKIE, 96.dp, cookie, 165.9.dp, 163.8.dp),
    CollageSlot(CollageShape.CIRCLE, 14.dp, circle, 96.dp, 96.dp),
    CollageSlot(CollageShape.PILL, 268.dp, pill, 72.dp, 124.dp),
    CollageSlot(CollageShape.ARCH, 236.dp, arch, 104.dp, 132.dp, bottomCorner = 20.dp),
    CollageSlot(CollageShape.CLOVER, 20.dp, clover, 90.4.dp, 90.4.dp),
    CollageSlot(CollageShape.SUNNY, 120.dp, sunny, 74.8.dp, 74.8.dp),
)

@Composable
private fun FavouritesCollage(subject: ShareSubject.Favourites, spec: CardSpec) {
    val heroWide = LocalExpressiveTypography.current.heroWide
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    val story = spec.size == CardSize.STORY
    Box(Modifier.fillMaxWidth().then(if (spec.size == CardSize.CHAT) Modifier.height(292.dp) else Modifier.fillMaxSize())) {
        val shrink = if (captionReserve(spec) > 0.dp) CollageShrink else 1f
        CardCollage(
            covers = subject.entries.take(6),
            slots = collageSlots(spec.size),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = shrink
                    scaleY = shrink
                    transformOrigin = TransformOrigin(0.5f, 0.15f)
                },
        )
        Text(
            text = stringResource(if (subject.isManga) R.string.share_fav_heading_manga else R.string.share_fav_heading_anime),
            style = heroWide.style(if (story) 28.sp else 25.sp, 30.sp, (-0.5).sp),
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = when (spec.size) {
                    CardSize.STORY -> 32.dp
                    CardSize.PORTRAIT -> 20.dp
                    else -> 16.dp
                },
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
            if (story && spec.shows(CardDetail.RANKING) && subject.entries.isNotEmpty()) {
                RankedList(subject.entries.take(5), content.copy(alpha = 0.85f))
                Spacer(Modifier.height(28.dp))
            }
            CardCaption(spec, cardText(15, 20, FontWeight.SemiBold), content, Modifier.padding(bottom = 10.dp))
            CardBrand(
                handle = subject.handle,
                spec = spec,
                container = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f),
                content = content,
            )
        }
    }
}
