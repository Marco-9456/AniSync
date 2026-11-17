package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.components.ConnectedToggleDefaults
import com.anisync.android.presentation.components.PinnedRail
import com.anisync.android.presentation.components.revealChip

/**
 * A rail's sideways scroll, held outside the rail. The profile draws its strip twice, in the page and
 * docked under the bar, and both copies have to stay where the reader left them.
 */
@Stable
class RailScroll internal constructor(val state: LazyListState) {
    internal var revealed: Any? = null
    internal val pinnedInset: MutableState<Dp?> = mutableStateOf(null)
}

@Composable
fun rememberRailScroll(): RailScroll = remember { RailScroll(LazyListState()) }

/** One [RailScroll] per rail, so each tab's chips come back where they were. */
@Stable
class ProfileRails {
    private val scrolls = mutableMapOf<String, RailScroll>()

    fun scroll(key: String): RailScroll = scrolls.getOrPut(key) { RailScroll(LazyListState()) }
}

/**
 * Brings the selected chip into view when the selection changes. A copy of the rail coming back
 * into the page keeps whatever the reader scrolled to.
 */
@Composable
internal fun RevealSelectedChip(scroll: RailScroll, selected: Any, index: Int) {
    LaunchedEffect(scroll, selected) {
        if (scroll.revealed == selected) return@LaunchedEffect
        if (index >= 0) scroll.state.revealChip(index)
        // Marked only once it has finished, so a copy swapped out mid way leaves it to the next.
        scroll.revealed = selected
    }
}

/** Every rail under the profile tabs is this tall, padding included. */
private val ProfileRailHeight = ConnectedToggleDefaults.Height

/** Holds a rail's place in the copy of the strip that is hidden under the docked one. */
@Composable
internal fun ProfileRailPlaceholder() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .height(ProfileRailHeight)
    )
}

/**
 * A profile tab's filters as a rail of chips, sitting under the tabs where the Statistics rail does.
 * The list tabs pin the anime and manga toggle at its start.
 */
@Composable
internal fun <T : Enum<T>> ProfileTabRail(
    options: List<T>,
    selected: T,
    modifier: Modifier = Modifier,
    pinned: (@Composable () -> Unit)? = null,
    scroll: RailScroll = rememberRailScroll(),
    chip: @Composable (option: T, selected: Boolean) -> Unit,
) {
    RevealSelectedChip(scroll, selected, options.indexOf(selected))
    PinnedRail(
        pinned = pinned,
        listState = scroll.state,
        pinnedInsetEstimate = 120.dp,
        measuredInset = scroll.pinnedInset,
        modifier = modifier.padding(top = 4.dp, bottom = 8.dp),
    ) {
        items(options, key = { it.name }) { option -> chip(option, option == selected) }
    }
}
