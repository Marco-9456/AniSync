package com.anisync.android.presentation.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollState
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.offset
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt

/**
 * Lets an expanded full-screen search bar's field leave as its results scroll and come back on the
 * first scroll up, the way the bar over Discover home does.
 *
 * The expanded bar lays its field out in a slot of fixed height, so the slot itself cannot shrink.
 * The content grows upward over it instead: [content] measures taller by however far it has
 * travelled and slides up on an opaque background, and [field] rides up with it and fades.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
class SearchFieldCollapse internal constructor(
    private val scrollState: SearchBarScrollState,
    /** Attach to the results alone. A detail pane scrolling beside them should leave the field be. */
    val nestedScrollConnection: NestedScrollConnection
) {
    internal val isCollapsing: Boolean
        get() = scrollState.scrollOffset < 0f

    private val fraction: Float
        get() = scrollState.scrollOffsetLimit.let { limit ->
            if (limit < 0f) (scrollState.scrollOffset / limit).coerceIn(0f, 1f) else 0f
        }

    /** For the input field. */
    val field: Modifier = Modifier.graphicsLayer {
        translationY = scrollState.scrollOffset
        alpha = 1f - fraction
    }

    /**
     * For the root of the bar's content. [topInset] is the window inset the bar keeps clear, which
     * is as far up as the content may travel.
     */
    fun content(topInset: Int, background: Color): Modifier = Modifier
        // Ahead of the shift, so this reads where the content sits with the field in place.
        .onPlaced { coordinates ->
            scrollState.scrollOffsetLimit = -(coordinates.positionInRoot().y - topInset).coerceAtLeast(0f)
        }
        .layout { measurable, constraints ->
            val offset = scrollState.scrollOffset.roundToInt()
            val placeable = measurable.measure(constraints.offset(vertical = -offset))
            layout(placeable.width, (placeable.height + offset).coerceAtLeast(0)) {
                placeable.place(0, offset)
            }
        }
        .background(background)

    /** Brings the field back, for content that has nothing left to scroll it back with. */
    suspend fun expand() {
        if (!isCollapsing) return
        animate(scrollState.scrollOffset, 0f) { value, _ -> scrollState.scrollOffset = value }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSearchFieldCollapse(searchBarState: SearchBarState): SearchFieldCollapse {
    // Fresh on every opening, so the bar never reopens with its field away.
    val expanded = searchBarState.currentValue == SearchBarValue.Expanded
    val scrollState = remember(expanded) { SearchBarScrollState(0f, 0f, 0f) }
    val behavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(scrollState = scrollState)
    val collapse = remember(behavior) { SearchFieldCollapse(scrollState, behavior.nestedScrollConnection) }
    // The bar reports Expanded until it has finished shrinking, so follow its target instead and
    // bring the field back while it shrinks.
    LaunchedEffect(collapse, searchBarState.targetValue) {
        if (searchBarState.targetValue == SearchBarValue.Collapsed) collapse.expand()
    }
    return collapse
}

/**
 * Closes the keyboard once the field starts to leave, since typing into a field off screen shows
 * nothing. Call it inside the bar's content: the expanded bar is its own window, and only that
 * window's controller reaches the keyboard it opened.
 */
@Composable
fun HideKeyboardOnCollapse(collapse: SearchFieldCollapse) {
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(collapse, keyboardController) {
        snapshotFlow { collapse.isCollapsing }
            .filter { it }
            .collect { keyboardController?.hide() }
    }
}
