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
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.get
import kotlinx.coroutines.flow.StateFlow

/**
 * The navigation host a back arrow's trail reads. A two-pane detail passes [list], the row that ends
 * its trail and closes the pane, since its own stack holds no tab.
 */
@Stable
class BackTrailHost(
    val navController: NavController,
    val list: TrailListRow? = null,
) {
    // Every page the trail lists is a composable destination. NavController's own combined stack is
    // internal to the navigation library. Read on use, because the navigator only has a stack once
    // the NavHost below the host has set its graph.
    internal val backStack: StateFlow<List<NavBackStackEntry>>
        get() = navController.navigatorProvider[ComposeNavigator::class].backStack

    /** The entry two steps back in quick succession landed on. Its arrow names the tab to jump to. */
    var streakEntryId: String? by mutableStateOf(null)
        private set

    internal suspend fun watchBackStreaks() {
        val streak = BackStreak()
        var previous = backStack.value.map { it.id }
        backStack.collect { stack ->
            val current = stack.map { it.id }
            if (wentBack(previous, current)) {
                streakEntryId = current.last().takeIf { streak.next(SystemClock.elapsedRealtime()) }
            } else if (current != previous) {
                streakEntryId = null
            }
            previous = current
        }
    }

    /**
     * Pops pages off the top until the entry with [entryId] is current. All of it happens before the
     * next frame, so one transition plays and the pages in between never compose.
     */
    internal fun popTo(entryId: String): Boolean {
        if (backStack.value.none { it.id == entryId }) return false
        while (navController.currentBackStackEntry?.id != entryId) {
            if (!navController.popBackStack()) return false
        }
        return true
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

/** A trail with the back stack entries its rows point at, read from the same stack. */
class TrailSnapshot internal constructor(
    val trail: BackTrail,
    private val entries: Map<String, NavBackStackEntry>,
) {
    fun entry(node: TrailNode): NavBackStackEntry? = entries[node.entryId]
}

/**
 * The trail behind [entry], or null when it has fewer than two rows or [entry] is not on this
 * host's stack, as with a page inside a nested host that brings no trail of its own.
 */
@Composable
internal fun rememberBackTrail(host: BackTrailHost?, entry: NavBackStackEntry?): TrailSnapshot? {
    if (host == null || entry == null) return null
    val stack by host.backStack.collectAsState()
    return remember(stack, entry, host) {
        val index = stack.indexOfFirst { it.id == entry.id }
        if (index < 0) return@remember null
        val below = stack.subList(0, index + 1)
        backTrailOf(below.map { it.toTrailNode() }, endsOnList = host.list != null)
            ?.let { TrailSnapshot(it, below.associateBy { page -> page.id }) }
    }
}
