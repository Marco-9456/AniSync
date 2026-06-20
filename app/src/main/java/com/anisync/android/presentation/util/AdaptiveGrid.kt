package com.anisync.android.presentation.util

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.data.AppSettings

/**
 * Whether poster grids size their columns automatically (adaptive to window width) or use a fixed
 * [LocalGridColumnCount]. Both are published app-wide from AppSettings (see MainActivity) and chosen
 * by the user from the Library view bottom sheet; grids read them through [posterGridColumns].
 */
val LocalGridColumnsAuto = compositionLocalOf { true }

/** The manual poster-grid column count used when [LocalGridColumnsAuto] is false. */
val LocalGridColumnCount = compositionLocalOf { AppSettings.DEFAULT_GRID_COLUMNS }

/**
 * Column strategy for poster grids, decided in exactly one place.
 *
 *  - Automatic: [GridCells.Adaptive] so the grid gains columns as the window widens (the Material 3
 *    adaptive default); [baseMinSize] is the natural minimum cell width (e.g. ~150dp for media
 *    posters, ~100dp for character/staff portraits).
 *  - Manual: [GridCells.Fixed] with the user's chosen [count] (2..8), the same on every width.
 */
@Composable
fun posterGridColumns(
    baseMinSize: Dp,
    auto: Boolean = LocalGridColumnsAuto.current,
    count: Int = LocalGridColumnCount.current,
): GridCells = if (auto) {
    GridCells.Adaptive(minSize = baseMinSize)
} else {
    GridCells.Fixed(count.coerceIn(AppSettings.MIN_GRID_COLUMNS, AppSettings.MAX_GRID_COLUMNS))
}

/**
 * Poster-column **count** (not [GridCells]) for surfaces that lay rows out manually because they
 * live inside another scrolling list and cannot nest a [androidx.compose.foundation.lazy.grid.LazyVerticalGrid]
 * — namely the Profile anime/manga tabs, which chunk their library entries into [Row]s.
 *
 * Phones keep their existing 3-up density unchanged; wider windows gain columns so posters stay
 * roughly [baseMinSize] wide (matching the [GridCells.Adaptive] grids elsewhere) instead of being
 * stretched three-across. Delegates to [profileGridColumns] with a 3-up compact floor.
 */
@Composable
fun profilePosterColumns(
    baseMinSize: Dp = 150.dp,
    horizontalPadding: Dp = 16.dp,
    itemSpacing: Dp = 12.dp,
    railWidth: Dp = 80.dp,
): Int = profileGridColumns(
    baseMinSize = baseMinSize,
    compactColumns = 3,
    horizontalPadding = horizontalPadding,
    itemSpacing = itemSpacing,
    railWidth = railWidth,
)

/**
 * Generalized column **count** for any profile grid that chunks its items into [Row]s because it
 * lives inside the profile [androidx.compose.foundation.lazy.LazyColumn] and cannot nest a grid —
 * the favorites (anime/manga/characters/staff/studios) and social (following/followers) tabs.
 *
 * Compact keeps the tab's existing [compactColumns] density unchanged (3 for portraits, 2 for the
 * wide studio chips); wider windows gain columns so each cell stays roughly [baseMinSize] wide,
 * matching the [GridCells.Adaptive] grids elsewhere instead of being stretched a fixed few across.
 * [railWidth] is subtracted because the navigation rail occupies the leading edge on medium+ widths;
 * the result is clamped to `[compactColumns, 8]` so it never drops below the compact density.
 */
@Composable
fun profileGridColumns(
    baseMinSize: Dp,
    compactColumns: Int = 3,
    horizontalPadding: Dp = 16.dp,
    itemSpacing: Dp = 12.dp,
    railWidth: Dp = 80.dp,
): Int {
    val info = LocalAdaptiveInfo.current
    if (info.isCompact) return compactColumns
    val density = LocalDensity.current
    val windowWidth = with(density) { currentWindowSize().width.toDp() }
    val content = windowWidth - railWidth - horizontalPadding * 2
    if (content <= 0.dp) return compactColumns
    val columns = ((content + itemSpacing) / (baseMinSize + itemSpacing)).toInt()
    return columns.coerceIn(compactColumns, 8)
}
