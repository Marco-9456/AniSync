package com.anisync.android.presentation.util

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import com.anisync.android.data.GridDensity

/**
 * The user's poster-grid [GridDensity] preference, published app-wide. Provided once at the app
 * root (see MainActivity) from AppSettings; grids read it through [posterGridColumns], so a single
 * setting reflows every poster grid without each screen wiring the preference itself.
 */
val LocalGridDensity = compositionLocalOf { GridDensity.AUTO }

/**
 * Column strategy for poster grids. Always returns [GridCells.Adaptive] so a grid naturally gains
 * columns as the window widens (the Material 3 adaptive default); [density] then scales the
 * per-cell minimum to honor the user's preference.
 *
 * This replaces the scattered `GridCells.Adaptive(minSize = …)` / `GridCells.Fixed(2)` call sites
 * so density and large-screen behavior are decided in exactly one place.
 *
 * @param baseMinSize the grid's natural minimum cell width at [GridDensity.AUTO] (e.g. ~150dp for
 *   media posters, ~100dp for character/staff portraits). Each call site keeps its own base, so
 *   dense grids stay dense and roomy grids stay roomy while still honoring the preference.
 * @param density the active density; defaults to the app-wide [LocalGridDensity].
 */
@Composable
fun posterGridColumns(
    baseMinSize: Dp,
    density: GridDensity = LocalGridDensity.current,
): GridCells {
    val factor = when (density) {
        GridDensity.AUTO -> 1f
        GridDensity.COMFORTABLE -> 1.25f // larger cells -> fewer columns
        GridDensity.COMPACT -> 0.8f      // smaller cells -> more columns
    }
    return GridCells.Adaptive(minSize = baseMinSize * factor)
}
