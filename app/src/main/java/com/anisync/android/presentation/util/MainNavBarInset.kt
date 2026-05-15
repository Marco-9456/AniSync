package com.anisync.android.presentation.util

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
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

/**
 * Reference-counted suppressor for the main bottom navigation bar.
 *
 * Full-screen overlays rendered inside a whitelisted tab route (e.g. the Edit
 * Profile rich-text editor on the Profile tab) can call [acquire] on enter and
 * [release] on dispose to hide the main nav bar while they are visible, without
 * having to remove their parent route from the visibility whitelist.
 */
@Stable
class MainNavBarSuppressor {
    var count by mutableIntStateOf(0)
        private set
    val isSuppressed: Boolean get() = count > 0
    fun acquire() { count++ }
    fun release() { if (count > 0) count-- }
}

val LocalMainNavBarSuppressor = compositionLocalOf<MainNavBarSuppressor?> { null }
