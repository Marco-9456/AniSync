package com.anisync.android.presentation.util

import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
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
 */
@Immutable
data class AdaptiveInfo(
    val widthClass: WidthSizeClass,
    val paneCount: Int,
    val contentMaxWidth: Dp,
    val pagePadding: Dp,
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
    val widthDp = with(density) { currentWindowSize().width.toDp() }
    val widthClass = when {
        widthDp >= LARGE_LOWER_BOUND -> WidthSizeClass.LARGE
        widthDp >= EXPANDED_LOWER_BOUND -> WidthSizeClass.EXPANDED
        widthDp >= MEDIUM_LOWER_BOUND -> WidthSizeClass.MEDIUM
        else -> WidthSizeClass.COMPACT
    }
    return remember(widthClass) {
        AdaptiveInfo(
            widthClass = widthClass,
            paneCount = if (widthClass == WidthSizeClass.EXPANDED || widthClass == WidthSizeClass.LARGE) 2 else 1,
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
