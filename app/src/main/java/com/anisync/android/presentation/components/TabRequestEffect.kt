package com.anisync.android.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Runs [onRequest] whenever [requestId] moves past the last one this composition handled.
 *
 * The counter lives in the ViewModel and outlives the screen, while this effect is recreated on
 * every return to the tab, so the consumed id has to be saveable: without it a screen re-entering
 * composition replays the last request and, say, reopens a search overlay the user just left.
 * Ids start at 0 and only grow, so 0 means "nothing asked yet".
 */
@Composable
fun TabRequestEffect(requestId: Long, onRequest: suspend () -> Unit) {
    var consumed by rememberSaveable { mutableLongStateOf(0L) }
    val request by rememberUpdatedState(onRequest)
    LaunchedEffect(requestId) {
        if (requestId <= consumed) return@LaunchedEffect
        consumed = requestId
        request()
    }
}

/** [TabRequestEffect] wired to a list: the tab's reselect gesture scrolls it back to the top. */
@Composable
fun ScrollToTopOnRequest(requestId: Long, listState: LazyListState) {
    TabRequestEffect(requestId) {
        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.animateScrollToItem(0)
        }
    }
}

/** Grid flavour of [ScrollToTopOnRequest], for the tabs laid out as a LazyVerticalGrid. */
@Composable
fun ScrollToTopOnRequest(requestId: Long, gridState: LazyGridState) {
    TabRequestEffect(requestId) {
        if (gridState.firstVisibleItemIndex != 0 || gridState.firstVisibleItemScrollOffset != 0) {
            gridState.animateScrollToItem(0)
        }
    }
}

/**
 * Expands a tab's search bar when [requestId] advances, for the double-tap shortcut.
 *
 * The retry loop is what the Discover overlay handshake had to learn: `animateToExpanded` can be
 * called before the bar is attached and then quietly does nothing, so nudge it until it reports
 * back expanded. An existing query is left alone rather than cleared, since on Library and Forum
 * the query is the screen's live filter and throwing it away would change what is on screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandSearchOnRequest(requestId: Long, searchBarState: SearchBarState) {
    TabRequestEffect(requestId) {
        var attempts = 0
        while (searchBarState.currentValue != SearchBarValue.Expanded && attempts < 10) {
            runCatching { searchBarState.animateToExpanded() }
            attempts++
            if (searchBarState.currentValue != SearchBarValue.Expanded) delay(100)
        }
    }
}
