package com.anisync.android.presentation.share.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.transformations
import com.anisync.android.R
import com.anisync.android.data.CardSize
import java.util.Locale
import kotlin.math.pow

/** Every card is laid out 360dp wide and captured at 1080px. */
val CardWidth = 360.dp

/** Card for chats keeps these corners, so its PNG sits in a chat bubble with transparent corners. */
val ChatCorner = 28.dp

/** The fixed height of [this] size, or null for [CardSize.CHAT], which fits its content. */
val CardSize.fixedHeight: Dp?
    get() = when (this) {
        CardSize.CHAT -> null
        CardSize.SQUARE -> 360.dp
        CardSize.PORTRAIT -> 450.dp
        CardSize.STORY -> 640.dp
    }

/**
 * The frame every template draws inside: the size, the corners and the background the studio picked.
 * Templates only draw their content, so the Background panel works the same for all of them.
 *
 * [solid] is the template's own fill, the colour Solid shows and Gradient fades into.
 */
@Composable
fun ShareCardScaffold(
    spec: CardSpec,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    solid: Color = MaterialTheme.colorScheme.surface,
    corner: Dp = if (spec.size == CardSize.CHAT) ChatCorner else 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val height = spec.size.fixedHeight
    val shape: Shape = if (corner > 0.dp) RoundedCornerShape(corner) else RectangleShape
    Box(
        modifier = modifier
            .width(CardWidth)
            .then(if (height != null) Modifier.height(height) else Modifier)
            .clip(shape)
    ) {
        CardBackdrop(spec, artworkUrl, solid, Modifier.matchParentSize())
        content()
    }
}

@Composable
private fun CardBackdrop(spec: CardSpec, artworkUrl: String?, solid: Color, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val background = if (spec.background == CardBackground.CLEAR && spec.size != CardSize.CHAT) {
        CardBackground.SOLID
    } else {
        spec.background
    }
    when (background) {
        CardBackground.CLEAR -> Unit
        CardBackground.SOLID -> Box(modifier.background(solid))
        CardBackground.GRADIENT -> Box(
            modifier.background(
                Brush.verticalGradient(
                    0f to scheme.primaryContainer,
                    GradientMidStop to lerp(scheme.primaryContainer, solid, dimmed(GradientMidMix, spec.dim)),
                    1f to solid,
                )
            )
        )
        CardBackground.ARTWORK_BLUR -> Box(modifier.background(solid)) {
            if (artworkUrl != null) {
                val context = LocalPlatformContext.current
                val request = remember(artworkUrl, context) {
                    ImageRequest.Builder(context).data(artworkUrl).transformations(ArtworkBlur()).build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    alpha = ArtworkAlpha,
                    modifier = Modifier
                        .fillMaxSize()
                        .overscan(ArtworkOverscan)
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to solid.copy(alpha = dimmed(ScrimTop, spec.dim)),
                            0.5f to solid.copy(alpha = dimmed(ScrimMiddle, spec.dim)),
                            1f to solid.copy(alpha = dimmed(ScrimBottom, spec.dim)),
                        )
                    )
            )
        }
    }
}

/**
 * Scales a darkening drawn at the default dim to [dim]. Applying it twice is the same as dimming
 * the result again, so 40% gives the frames exactly and the slider moves smoothly either side.
 */
internal fun dimmed(atDefault: Float, dim: Float): Float =
    (1f - (1f - atDefault).pow(dim / CardSpec.DEFAULT_DIM)).coerceIn(0f, 1f)

/** Draws past the bounds on every side, so the artwork's blurred edges fall outside the card. */
private fun Modifier.overscan(by: Dp): Modifier = layout { measurable, constraints ->
    val extra = by.roundToPx()
    val placeable = measurable.measure(
        Constraints.fixed(constraints.maxWidth + extra * 2, constraints.maxHeight + extra * 2)
    )
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-extra, -extra) }
}

/** Fades content out toward its bottom edge, so a banner melts into whatever background sits under it. */
fun Modifier.fadeToBottom(topAlpha: Float = 1f): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(0f to Color.Black.copy(alpha = topAlpha), 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
    }

/** The AniSync mark: the brand glyph on a primary circle. */
@Composable
fun CardMark(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_brand_glyph),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size),
        )
    }
}

/** The mark, AniSync and the handle, the attribution every card carries. */
@Composable
fun CardBrand(
    handle: String?,
    spec: CardSpec,
    container: Color,
    modifier: Modifier = Modifier,
    content: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .padding(start = 6.dp, top = 6.dp, end = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardMark()
        Text(
            text = stringResource(R.string.share_card_brand),
            style = cardText(13, 18, FontWeight.Bold),
            color = content,
            maxLines = 1,
        )
        if (handle != null && spec.shows(CardDetail.HANDLE)) {
            Text(
                text = stringResource(R.string.share_card_handle, handle),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
                color = content.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The flat linear indicator the frames detach from the M3 kit: 4dp, round caps, stop dot. */
@Composable
fun CardProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier.height(12.dp), contentAlignment = Alignment.Center) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = scheme.primary,
            trackColor = scheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 4.dp,
        )
    }
}

/** A cover or portrait, cropped into [shape], with a quiet fill while it loads. */
@Composable
fun CardPicture(url: String?, shape: Shape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** The caption the viewer typed, set into the card. */
@Composable
fun CardCaption(spec: CardSpec, style: TextStyle, color: Color, modifier: Modifier = Modifier, maxLines: Int = 2) {
    val caption = spec.caption.trim()
    if (caption.isEmpty()) return
    Text(
        text = caption,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Google Sans Flex at the given size, line height and weight, the label family the cards draw small text in. */
@Composable
@ReadOnlyComposable
fun cardText(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Float = 0f): TextStyle =
    MaterialTheme.typography.labelLarge.copy(
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = letterSpacing.sp,
    )

/** The small capitals eyebrow most cards open with. */
@Composable
@ReadOnlyComposable
fun cardEyebrow(letterSpacing: Float = 1.4f): TextStyle = cardText(11, 16, FontWeight.Medium, letterSpacing)

/** Upper case in the app's language, as the frames' eyebrows are set. */
@Composable
@ReadOnlyComposable
fun String.cardUpper(): String = uppercase(LocalConfiguration.current.locales[0] ?: Locale.getDefault())

/** True while the card's scheme is a dark one. */
@Composable
@ReadOnlyComposable
fun cardIsDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

private const val ArtworkAlpha = 0.85f
private val ArtworkOverscan = 40.dp
private const val ScrimTop = 0.25f
private const val ScrimMiddle = 0.55f
private const val ScrimBottom = 0.96f
private const val GradientMidStop = 0.55f
private const val GradientMidMix = 0.62f
