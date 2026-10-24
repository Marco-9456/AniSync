package com.anisync.android.presentation.navigation

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * The navigation host a back arrow's trail reads. A two-pane detail passes [list], the row that ends
 * its trail and closes the pane, since its own stack holds no tab.
 */
@Stable
class BackTrailHost(
    val navController: NavController,
    val list: TrailListRow? = null,
) {
    /** The entry two steps back in quick succession landed on. Its arrow names the tab to jump to. */
    var streakEntryId: String? by mutableStateOf(null)
        private set

    internal suspend fun watchBackStreaks() {
        val streak = BackStreak()
        var previous = navController.currentBackStack.value.map { it.id }
        navController.currentBackStack.collect { stack ->
            val current = stack.map { it.id }
            if (wentBack(previous, current)) {
                streakEntryId = current.last().takeIf { streak.next(SystemClock.elapsedRealtime()) }
            } else if (current != previous) {
                streakEntryId = null
            }
            previous = current
        }
    }
}

@Composable
fun rememberBackTrailHost(navController: NavController, list: TrailListRow? = null): BackTrailHost {
    val host = remember(navController, list) { BackTrailHost(navController, list) }
    LaunchedEffect(host) { host.watchBackStreaks() }
    return host
}

/** The list a two-pane detail was opened from, as the last row of the trail inside it. */
class TrailListRow(
    val icon: ImageVector,
    @param:StringRes val label: Int,
    val onSelect: () -> Unit,
)

val LocalBackTrailHost = staticCompositionLocalOf<BackTrailHost?> { null }

/**
 * The trail behind [entry], or null when it has fewer than two rows or [entry] is not on this
 * host's stack, as with a page inside a nested host that brings no trail of its own.
 */
@Composable
internal fun rememberBackTrail(host: BackTrailHost?, entry: NavBackStackEntry?): BackTrail? {
    if (host == null || entry == null) return null
    val stack by host.navController.currentBackStack.collectAsState()
    return remember(stack, entry, host) {
        val index = stack.indexOfFirst { it.id == entry.id }
        if (index < 0) {
            null
        } else {
            backTrailOf(
                stack = stack.subList(0, index + 1).map { it.toTrailNode() },
                endsOnList = host.list != null,
            )
        }
    }
}

/**
 * Pops pages off the top until the entry with [entryId] is current. All of it happens before the
 * next frame, so one transition plays and the pages in between never compose.
 */
internal fun NavController.popBackStackTo(entryId: String): Boolean {
    if (currentBackStack.value.none { it.id == entryId }) return false
    while (currentBackStackEntry?.id != entryId) {
        if (!popBackStack()) return false
    }
    return true
}
