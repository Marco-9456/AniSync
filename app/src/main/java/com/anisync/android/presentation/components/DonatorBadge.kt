package com.anisync.android.presentation.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.em
import com.anisync.android.R
import com.anisync.android.ui.theme.emphasis

/** Where a donator badge sits, which on AniList's website decides what it says and whether it shimmers. */
enum class DonatorBadgePlacement {
    /** The profile header, where every donator's badge shows their own text over the rainbow. */
    ProfileHeader,

    /** Next to a name: an activity, a forum post, a user in search. */
    NextToName
}

/**
 * AniList's donator badge, drawn by the website's rules.
 *
 * Next to a name, tiers one to three read "Donator", four and up show the donator's own text, and
 * five and up cycle through the rainbow. On the profile header every donator gets their own text
 * over the rainbow. The rainbow is the website's: thirteen stops over twenty seconds, eased between
 * each, at 71% opacity under white text.
 *
 * A quiet badge takes [containerColor]. The website always sets it on a card in the page colour, so
 * that is the default, and a badge sitting on the page itself passes a container colour instead.
 * Next to a name the badge stops at [maxWidth], so a long text cannot squeeze the name out of a
 * crowded row. Where the badge has a line of its own, pass [Dp.Unspecified].
 */
@Composable
fun DonatorBadge(
    tier: Int,
    badgeText: String?,
    placement: DonatorBadgePlacement,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    maxWidth: Dp = 112.dp
) {
    if (tier <= 0) return
    val text = donatorBadgeText(tier, badgeText, placement) ?: stringResource(R.string.donator_badge_default)
    val rainbow = isRainbowDonator(tier, placement)

    // The header badge sits among the profile's chips, so it takes their shape and type.
    val onHeader = placement == DonatorBadgePlacement.ProfileHeader
    val shape: Shape = if (onHeader) CircleShape else RoundedCornerShape(4.dp)
    val style = if (onHeader) {
        MaterialTheme.typography.labelLarge.emphasis()
    } else {
        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.03.em)
    }
    val padding = if (onHeader) {
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    } else {
        Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    }
    val bounded = if (onHeader || !maxWidth.isSpecified) modifier else modifier.widthIn(max = maxWidth)

    if (rainbow) {
        val progress = rememberRainbowProgress()
        Text(
            text = text,
            style = style,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = bounded
                .clip(shape)
                // Read in the draw phase, so the cycle repaints the badge without recomposing it.
                .drawBehind { drawRect(rainbowAt(progress.value)) }
                .then(padding)
        )
    } else {
        Text(
            text = text,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = bounded
                .clip(shape)
                .background(containerColor)
                .then(padding)
        )
    }
}

private const val CUSTOM_TEXT_TIER = 4
private const val RAINBOW_TIER = 5
private const val RAINBOW_CYCLE_MILLIS = 20_000

/** The donator's own text where the website prints it, or null for the plain "Donator". */
internal fun donatorBadgeText(tier: Int, badgeText: String?, placement: DonatorBadgePlacement): String? {
    val custom = badgeText?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when (placement) {
        DonatorBadgePlacement.ProfileHeader -> custom
        DonatorBadgePlacement.NextToName -> custom.takeIf { tier >= CUSTOM_TEXT_TIER }
    }
}

internal fun isRainbowDonator(tier: Int, placement: DonatorBadgePlacement): Boolean =
    tier > 0 && (placement == DonatorBadgePlacement.ProfileHeader || tier >= RAINBOW_TIER)

private fun rgba(red: Int, green: Int, blue: Int) = Color(red, green, blue, alpha = (0.71f * 255).toInt())

// AniList's `@keyframes rainbow`, as offsets into the cycle.
private val RainbowStops = listOf(
    0.00f to rgba(0, 105, 255),
    0.10f to rgba(100, 0, 255),
    0.20f to rgba(255, 0, 139),
    0.30f to rgba(255, 0, 0),
    0.40f to rgba(255, 96, 0),
    0.50f to rgba(202, 255, 0),
    0.60f to rgba(0, 255, 139),
    0.70f to rgba(202, 255, 0),
    0.80f to rgba(255, 96, 0),
    0.85f to rgba(255, 0, 0),
    0.90f to rgba(255, 0, 139),
    0.95f to rgba(100, 0, 255),
    1.00f to rgba(0, 105, 255)
)

@Composable
private fun rememberRainbowProgress(): State<Float> =
    rememberInfiniteTransition(label = "donator_rainbow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RAINBOW_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "donator_rainbow_progress"
    )

/** The colour at [progress] through the cycle. CSS eases each keyframe gap on its own, so this does too. */
internal fun rainbowAt(progress: Float): Color {
    val t = progress.coerceIn(0f, 1f)
    val next = RainbowStops.indexOfFirst { it.first >= t }.coerceAtLeast(1)
    val (startAt, startColor) = RainbowStops[next - 1]
    val (endAt, endColor) = RainbowStops[next]
    val span = endAt - startAt
    val fraction = if (span <= 0f) 1f else (t - startAt) / span
    return lerp(startColor, endColor, EaseInOut.transform(fraction))
}
