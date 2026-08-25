package com.anisync.android.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.AppTheme
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.listIndicatorArtColor
import com.anisync.android.ui.theme.listIndicatorColor
import com.anisync.android.ui.theme.listIndicatorNeedsOutline

/**
 * How the indicator is drawn. Covers take [Corner], rows and search results take [Chip], and narrow
 * surfaces take the free-standing [Overlay]. Never two forms on one card.
 */
enum class ListIndicatorStyle { Corner, Overlay, Chip }

/**
 * Which corner of the cover the [ListIndicatorStyle.Corner] tab grows out of.
 *
 * [BottomEnd] is the design's default. [TopStart] is for covers that already spend their bottom
 * edge on a title, such as the trending card, and is the same tab turned around.
 */
enum class ListIndicatorCorner { BottomEnd, TopStart }

/**
 * Says that a title already sits on one of the viewer's lists, and which one.
 *
 * Colour names the list, the icon names it again for anyone who cannot use the colour, and the
 * shape names the family: circle while a title is in motion, rounded square while it is parked,
 * square once it is finished with.
 */
@Composable
fun ListIndicator(
    status: LibraryStatus,
    type: MediaType?,
    style: ListIndicatorStyle,
    modifier: Modifier = Modifier,
    corner: ListIndicatorCorner = ListIndicatorCorner.BottomEnd
) {
    val kind = status.toIndicatorKind()
    val colors = listIndicatorColor(kind)
    val outline = if (listIndicatorNeedsOutline()) {
        Modifier.border(1.dp, colors.content.copy(alpha = 0.4f), kind.overlayShape())
    } else {
        Modifier
    }
    val label = kind.label(status, type)
    val spoken = stringResource(R.string.a11y_in_your_list, label)

    when (style) {
        // Welded into the bottom-right of a cover: the tab shares the cover's 18dp radius on the
        // outside and meets the art with concave fillets, so it grows out of the poster rather
        // than sitting on top of it.
        ListIndicatorStyle.Corner -> {
            val art = listIndicatorArtColor(kind)
            val flipped = corner == ListIndicatorCorner.TopStart
            Box(
                modifier = modifier
                    .size(width = 64.dp, height = 56.dp)
                    .semanticsLabel(spoken)
            ) {
                // The same tab turned around for the opposite corner. The host cover clips its own
                // outer radius, so the tab fuses into whatever radius that cover uses.
                Icon(
                    painter = painterResource(R.drawable.ic_list_corner_tab),
                    contentDescription = null,
                    tint = art.container,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = if (flipped) 180f else 0f }
                )
                Icon(
                    painter = painterResource(kind.iconRes()),
                    contentDescription = null,
                    tint = art.content,
                    modifier = Modifier
                        .offset(
                            x = if (flipped) 17.dp else 29.dp,
                            y = if (flipped) 13.dp else 25.dp
                        )
                        .size(18.dp)
                )
            }
        }

        ListIndicatorStyle.Overlay -> Surface(
            color = colors.container,
            contentColor = colors.content,
            shape = kind.overlayShape(),
            shadowElevation = 4.dp,
            modifier = modifier
                .size(28.dp)
                .alpha(0.94f)
                .then(outline)
                .semanticsLabel(spoken)
        ) {
            Box(contentAlignment = Alignment.Center) {
                IndicatorIcon(kind)
            }
        }

        ListIndicatorStyle.Chip -> Surface(
            color = colors.container,
            contentColor = colors.content,
            shape = kind.chipShape(),
            modifier = modifier
                .height(26.dp)
                .then(if (kind == ListIndicatorKind.CUSTOM) Modifier.dashedOutline(colors.content, kind.chipShape()) else Modifier)
                .semanticsLabel(spoken)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp, end = 10.dp)
            ) {
                IndicatorIcon(kind)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun IndicatorIcon(kind: ListIndicatorKind) {
    Icon(
        painter = painterResource(kind.iconRes()),
        contentDescription = null,
        modifier = Modifier.size(16.dp)
    )
}

/**
 * Reads as one phrase inside the card it marks, rather than as a control of its own. The card is
 * the tap target, so the indicator only contributes wording.
 */
private fun Modifier.semanticsLabel(label: String) = clearAndSetSemantics {
    contentDescription = label
}

private fun Modifier.dashedOutline(color: androidx.compose.ui.graphics.Color, shape: Shape) = drawBehind {
    val radius = if (shape is RoundedCornerShape) 8.dp.toPx() else 0f
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
        )
    )
}

/** Custom lists are the fallback: an entry with no standard status still belongs somewhere. */
fun LibraryStatus.toIndicatorKind(): ListIndicatorKind = when (this) {
    LibraryStatus.CURRENT -> ListIndicatorKind.WATCHING
    LibraryStatus.REPEATING -> ListIndicatorKind.REPEATING
    LibraryStatus.PLANNING -> ListIndicatorKind.PLANNING
    LibraryStatus.PAUSED -> ListIndicatorKind.PAUSED
    LibraryStatus.COMPLETED -> ListIndicatorKind.COMPLETED
    LibraryStatus.DROPPED -> ListIndicatorKind.DROPPED
    LibraryStatus.UNKNOWN -> ListIndicatorKind.CUSTOM
}

/** The list's own glyph, shared with any surface that names a list (the details status menu). */
fun ListIndicatorKind.iconRes(): Int = when (this) {
    ListIndicatorKind.WATCHING -> R.drawable.ic_list_watching
    ListIndicatorKind.REPEATING -> R.drawable.ic_list_repeating
    ListIndicatorKind.PLANNING -> R.drawable.ic_list_planning
    ListIndicatorKind.PAUSED -> R.drawable.ic_list_paused
    ListIndicatorKind.COMPLETED -> R.drawable.ic_list_completed
    ListIndicatorKind.DROPPED -> R.drawable.ic_list_dropped
    ListIndicatorKind.CUSTOM -> R.drawable.ic_list_custom
}

/** Circle while in motion, rounded square while parked, square once finished with. */
private fun ListIndicatorKind.overlayShape(): Shape = when (this) {
    ListIndicatorKind.WATCHING, ListIndicatorKind.REPEATING -> CircleShape
    ListIndicatorKind.PLANNING, ListIndicatorKind.PAUSED, ListIndicatorKind.CUSTOM -> RoundedCornerShape(8.dp)
    ListIndicatorKind.COMPLETED, ListIndicatorKind.DROPPED -> RoundedCornerShape(2.dp)
}

private fun ListIndicatorKind.chipShape(): Shape = when (this) {
    ListIndicatorKind.WATCHING, ListIndicatorKind.REPEATING -> RoundedCornerShape(13.dp)
    ListIndicatorKind.PLANNING, ListIndicatorKind.PAUSED, ListIndicatorKind.CUSTOM -> RoundedCornerShape(8.dp)
    ListIndicatorKind.COMPLETED, ListIndicatorKind.DROPPED -> RoundedCornerShape(3.dp)
}

@Composable
private fun ListIndicatorKind.label(status: LibraryStatus, type: MediaType?): String =
    if (this == ListIndicatorKind.CUSTOM) stringResource(R.string.list_indicator_custom)
    else status.toLabel(type)

@Preview
@Composable
private fun ListIndicatorPreview() {
    AppTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            ListIndicator(LibraryStatus.CURRENT, MediaType.ANIME, ListIndicatorStyle.Corner)
            ListIndicator(LibraryStatus.COMPLETED, MediaType.ANIME, ListIndicatorStyle.Overlay)
            ListIndicator(LibraryStatus.PLANNING, MediaType.ANIME, ListIndicatorStyle.Chip)
            ListIndicator(LibraryStatus.UNKNOWN, MediaType.ANIME, ListIndicatorStyle.Chip)
        }
    }
}
