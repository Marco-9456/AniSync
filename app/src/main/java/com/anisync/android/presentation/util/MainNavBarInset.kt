package com.anisync.android.presentation.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bottom inset that scrollable tab-screen content should reserve so the last items
 * can be fully scrolled into view above the main bottom navigation bar.
 *
 * Provided by `MainScreen`. Defaults to a conservative value when not provided
 * (e.g. on a detail screen with its own bottom bar) so consumers can use it
 * unconditionally without null checks.
 *
 * Apply via `PaddingValues(bottom = LocalMainNavBarInset.current, ...)` on
 * LazyColumn/LazyGrid contentPadding, or as `Modifier.padding(bottom = ...)` on
 * non-scrollable bottom-aligned UI (FABs use a higher value if they also need to
 * clear the bar visually).
 */
val LocalMainNavBarInset = compositionLocalOf<Dp> { 0.dp }
