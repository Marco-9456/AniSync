package com.anisync.android.presentation.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AniSync's width buckets, derived from the Material 3 window size classes. These — not raw pixel
 * widths — drive every adaptive layout decision (navigation container, grid columns, pane count,
 * reading-width cap, page padding).
 *
 * Material 3 defines three width breakpoints; we add [LARGE] (>= 1200dp) so very wide tablets,
 * unfolded large foldables and desktop windows can opt into a permanent drawer / higher column
 * ceiling without affecting the standard expanded layout.
 *
 *  - [COMPACT]  `< 600dp`   — phone portrait. Bottom navigation, single pane.
 *  - [MEDIUM]   `600–839dp` — phone landscape / small tablet / unfolded foldable. Navigation rail.
 *  - [EXPANDED] `840–1199dp`— tablet / large foldable. Wide rail, two panes.
 *  - [LARGE]    `>= 1200dp` — very wide. Permanent drawer candidate.
 */
enum class WidthSizeClass { COMPACT, MEDIUM, EXPANDED, LARGE }

/**
 * The single source of truth for "how wide are we and what does that imply". Resolved once near the
 * top of the tree (see [rememberAdaptiveInfo]) and published through [LocalAdaptiveInfo]; screens
 * read the local rather than calling [currentWindowAdaptiveInfo] themselves, so every surface agrees
 * on the active layout.
 *
 * @param widthClass the active [WidthSizeClass].
 * @param paneCount how many panes a list-detail surface should show (1 or 2).
 * @param contentMaxWidth reading-width cap for single-column surfaces; [Dp.Unspecified] = fill width.
 * @param pagePadding the default horizontal page inset for this width.
 * @param isTabletDevice whether this is a tablet-class device (smallest width >= 600dp). Gates the
 *   two-pane list-detail, which needs a tablet-width device — a phone in a wide landscape reports an
 *   expanded width but its side-by-side panes would be too narrow.
 * @param isCompactHeight whether the window height is compact (< 480dp per M3), e.g. a phone in
 *   landscape. A navigation rail needs vertical room for its destinations; M3 says compact-height
 *   windows use the navigation bar instead (the rail "may result in a compact height").
 */
@Immutable
data class AdaptiveInfo(
    val widthClass: WidthSizeClass,
    val paneCount: Int,
    val contentMaxWidth: Dp,
    val pagePadding: Dp,
    val isTabletDevice: Boolean = false,
    val isCompactHeight: Boolean = false,
) {
    val isCompact: Boolean get() = widthClass == WidthSizeClass.COMPACT
    val isMediumOrWider: Boolean get() = widthClass != WidthSizeClass.COMPACT
    val isExpandedOrWider: Boolean
        get() = widthClass == WidthSizeClass.EXPANDED || widthClass == WidthSizeClass.LARGE
    val supportsTwoPane: Boolean get() = paneCount >= 2
}

// Material 3 width breakpoints (dp). LARGE is an AniSync addition above the expanded bound.
private val MEDIUM_LOWER_BOUND = 600.dp
private val EXPANDED_LOWER_BOUND = 840.dp
private val LARGE_LOWER_BOUND = 1200.dp

// Material 3 compact-height upper bound (dp): below 480dp the window is too short for a vertical
// navigation rail (its destinations overflow / can't stay fixed and visible), so the navigation bar is
// used instead — the phone-landscape case. Mirrors WindowHeightSizeClass.Compact.
private val HEIGHT_COMPACT_UPPER_BOUND = 480.dp

// Smallest-width (dp) at/above which a device is tablet-class. M3 defines the Expanded width class as
// "the majority of tablets in landscape" — NOT phones in landscape, which can still report an
// expanded *width* (~850dp) yet are physically too small/short for a side-by-side list-detail. This
// is the canonical Android sw600dp tablet qualifier; below it, surfaces stay single-pane even in a
// wide landscape, while genuine tablets and unfolded foldables (sw >= 600) get two panes.
private const val TABLET_SW_MIN_DP = 600

/**
 * Compact-by-default so previews and any composable read outside a provider behave like a phone.
 * [compositionLocalOf] (not static) so only the composables that actually read it recompose when the
 * window is resized.
 */
val LocalAdaptiveInfo = compositionLocalOf {
    AdaptiveInfo(
        widthClass = WidthSizeClass.COMPACT,
        paneCount = 1,
        contentMaxWidth = Dp.Unspecified,
        pagePadding = 16.dp,
    )
}

/**
 * Computes the current [AdaptiveInfo] from the window size class. Call once high in the tree (inside
 * the app theme) and feed the result into [LocalAdaptiveInfo]; everything else consumes the local.
 */
@Composable
fun rememberAdaptiveInfo(): AdaptiveInfo {
    // Raw window width in dp — robust across androidx.window versions (no dependency on the newer
    // WindowSizeClass.isWidthAtLeastBreakpoint API), and lets us add the custom LARGE bound.
    val density = LocalDensity.current
    val windowSize = currentWindowSize()
    val widthDp = with(density) { windowSize.width.toDp() }
    val heightDp = with(density) { windowSize.height.toDp() }
    val widthClass = when {
        widthDp >= LARGE_LOWER_BOUND -> WidthSizeClass.LARGE
        widthDp >= EXPANDED_LOWER_BOUND -> WidthSizeClass.EXPANDED
        widthDp >= MEDIUM_LOWER_BOUND -> WidthSizeClass.MEDIUM
        else -> WidthSizeClass.COMPACT
    }
    // Two panes only on tablet-class devices (see [TABLET_SW_MIN_DP]); a phone in landscape reports an
    // expanded width but must stay single-pane. smallestScreenWidthDp is orientation-independent, so
    // it identifies the device, not the current rotation.
    val isTabletClass = LocalConfiguration.current.smallestScreenWidthDp >= TABLET_SW_MIN_DP
    val isCompactHeight = heightDp < HEIGHT_COMPACT_UPPER_BOUND
    return remember(widthClass, isTabletClass, isCompactHeight) {
        val expandedWidth = widthClass == WidthSizeClass.EXPANDED || widthClass == WidthSizeClass.LARGE
        AdaptiveInfo(
            widthClass = widthClass,
            isTabletDevice = isTabletClass,
            isCompactHeight = isCompactHeight,
            paneCount = if (isTabletClass && expandedWidth) 2 else 1,
            contentMaxWidth = when (widthClass) {
                WidthSizeClass.COMPACT -> Dp.Unspecified
                WidthSizeClass.MEDIUM -> 600.dp
                WidthSizeClass.EXPANDED -> 840.dp
                WidthSizeClass.LARGE -> 960.dp
            },
            pagePadding = when (widthClass) {
                WidthSizeClass.COMPACT -> 16.dp
                else -> 24.dp
            },
        )
    }
}

/**
 * Caps a single-column reading surface to [AdaptiveInfo.contentMaxWidth] and centers it within the
 * available width, so prose and list rows don't stretch to unreadable line lengths on wide windows.
 *
 * No-op on compact (where [AdaptiveInfo.contentMaxWidth] is [Dp.Unspecified]) — phones keep full
 * bleed. Apply to the scrollable/content of a reading screen (details body, forum thread, settings,
 * activity, review), not to individual list items.
 */
fun Modifier.adaptiveReadingWidth(): Modifier = composed {
    val maxWidth = LocalAdaptiveInfo.current.contentMaxWidth
    if (maxWidth == Dp.Unspecified) {
        this
    } else {
        this
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = maxWidth)
    }
}
