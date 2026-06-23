package com.anisync.android.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A contextual primary action a top-level tab publishes to the navigation rail's header.
 *
 * On rail layouts (medium / expanded) Material 3 hosts a screen's primary FAB in the rail's `header`
 * slot rather than floating it bottom-end. A tab declares its action with [SetRailFab]; the rail
 * reads [RailFabState.fab] and renders it. On compact widths there is no rail, so the tab keeps its
 * own floating action button instead (see [LocalRailFabState] being null there).
 */
data class RailFab(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

/** Mutable bridge between the active tab (which sets [fab]) and the rail (which renders it). */
class RailFabState {
    var fab by mutableStateOf<RailFab?>(null)
        internal set
}

/**
 * Provided by `MainScreen` above both the rail and the NavHost on rail layouts; **null on compact**,
 * so a tab can tell whether a rail exists to host its FAB.
 */
val LocalRailFabState = compositionLocalOf<RailFabState?> { null }

/**
 * Publishes a contextual FAB to the navigation rail header for as long as the caller is in
 * composition, clearing it on leave. No-op when [LocalRailFabState] is null (compact widths, where
 * the caller shows its own floating action button keyed on `LocalRailFabState.current == null`).
 *
 * [onClick] is captured through [rememberUpdatedState] so the rail always invokes the latest lambda
 * without the [DisposableEffect] churning every recomposition.
 */
@Composable
fun SetRailFab(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val holder = LocalRailFabState.current ?: return
    val currentOnClick by rememberUpdatedState(onClick)
    DisposableEffect(holder, icon, contentDescription) {
        val fab = RailFab(icon, contentDescription) { currentOnClick() }
        holder.fab = fab
        // Clear only if we still own the slot. When switching tabs, the incoming screen's effect can
        // publish its FAB before the outgoing screen's onDispose runs; an unconditional null would
        // then clobber the new tab's FAB. Identity check makes the handoff order-independent.
        onDispose { if (holder.fab === fab) holder.fab = null }
    }
}
