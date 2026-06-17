package com.anisync.android.presentation.util

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
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
