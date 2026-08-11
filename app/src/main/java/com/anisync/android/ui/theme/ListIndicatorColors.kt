package com.anisync.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

/**
 * Container and content colours for the personal list indicator, one pair per list.
 *
 * The dark values are the list and on-list tokens from the design file. Light theme swaps
 * the pair rather than inventing a second palette: the tint becomes the surface and the deep tone
 * becomes the icon, which keeps each list recognisable by the same hue in both themes.
 */
@Immutable
data class ListIndicatorColor(
    val container: Color,
    val content: Color
)

private val WatchingDark = ListIndicatorColor(Color(0xFF26463F), Color(0xFFA8F0D8))
private val RepeatingDark = ListIndicatorColor(Color(0xFF4A2C18), Color(0xFFFFC29A))
private val PlanningDark = ListIndicatorColor(Color(0xFF1F3A57), Color(0xFFB6D9FF))
private val PausedDark = ListIndicatorColor(Color(0xFF4A3B18), Color(0xFFFFD98A))
private val CompletedDark = ListIndicatorColor(Color(0xFF2A4A2C), Color(0xFFB6E9B5))
private val DroppedDark = ListIndicatorColor(Color(0xFF55231F), Color(0xFFFFB4AB))
private val CustomDark = ListIndicatorColor(Color(0xFF3A4A47), Color(0xFFD5E5E0))

private fun ListIndicatorColor.inverted() = ListIndicatorColor(container = content, content = container)

/**
 * True in light theme, where the swapped pair puts a pale container over cover art that is often
 * just as pale. The shadow alone does not hold the edge there, so the indicator draws a hairline.
 */
@Composable
fun listIndicatorNeedsOutline(): Boolean = MaterialTheme.colorScheme.surface.luminance() > 0.5f

/** The palette for the current theme. */
@Composable
fun listIndicatorColor(kind: ListIndicatorKind): ListIndicatorColor =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) kind.tokens().inverted() else kind.tokens()

/**
 * The pair for an indicator drawn on cover art, with the tonal roles swapped: pale container, deep
 * icon. Cover art does not change with the app theme, so this one never flips.
 */
fun listIndicatorArtColor(kind: ListIndicatorKind): ListIndicatorColor = kind.tokens().inverted()

private fun ListIndicatorKind.tokens(): ListIndicatorColor = when (this) {
    ListIndicatorKind.WATCHING -> WatchingDark
    ListIndicatorKind.REPEATING -> RepeatingDark
    ListIndicatorKind.PLANNING -> PlanningDark
    ListIndicatorKind.PAUSED -> PausedDark
    ListIndicatorKind.COMPLETED -> CompletedDark
    ListIndicatorKind.DROPPED -> DroppedDark
    ListIndicatorKind.CUSTOM -> CustomDark
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

/**
 * The seven lists an indicator can name. Separate from `LibraryStatus` because the indicator also
 * has to speak for an entry that only sits in custom lists.
 */
enum class ListIndicatorKind {
    WATCHING,
    REPEATING,
    PLANNING,
    PAUSED,
    COMPLETED,
    DROPPED,
    CUSTOM
}
