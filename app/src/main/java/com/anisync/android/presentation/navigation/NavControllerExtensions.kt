package com.anisync.android.presentation.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Lifecycle-guarded navigation helpers. Inspired by PixelPlayer's nav extensions,
 * adapted for type-safe (`@Serializable`) route objects.
 *
 * Rapid taps during a running transition can fire `navigate()` multiple times before
 * Compose Navigation finishes attaching the destination, producing duplicate back
 * stack entries or — on older devices — crashes. Gating on the current entry's
 * lifecycle being at least [Lifecycle.State.STARTED] short-circuits those repeats.
 */

private fun NavController.isReadyForNavigation(): Boolean = runCatching {
    currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
}.getOrDefault(false)

/**
 * Navigates to [route] only if the current back-stack entry is at least STARTED.
 * Sets `launchSingleTop = true` to coalesce duplicate destinations.
 *
 * @return true if the call dispatched, false if it was suppressed.
 */
fun NavController.navigateSafely(
    route: Any,
    builder: NavOptionsBuilder.() -> Unit = {}
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
        builder()
    }
    return true
}
