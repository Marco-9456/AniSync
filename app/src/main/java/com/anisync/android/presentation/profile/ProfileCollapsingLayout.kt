package com.anisync.android.presentation.profile

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * How far the phone profile's header has scrolled away, from 0 with all of it in view to [range]
 * with the tabs docked under the bar. A docked header stays docked when its height changes, so a
 * bio measuring itself never pulls the tabs back down.
 */
@Stable
internal class ProfileHeaderState(collapsed: Float = 0f, dockOnMeasure: Boolean = false) {
    var collapsed by mutableFloatStateOf(collapsed)
        private set

    var range by mutableFloatStateOf(0f)
        private set

    private var dockOnMeasure = dockOnMeasure

    val docked: Boolean get() = range > 0f && collapsed >= range - 0.5f

    internal fun onMeasured(newRange: Float) {
        if (newRange == range && !dockOnMeasure) return
        val stayDocked = dockOnMeasure || docked
        dockOnMeasure = false
        range = newRange
        collapsed = if (stayDocked) newRange else collapsed.coerceIn(0f, newRange)
    }

    /** Moves by up to [distance], positive towards docked, and returns how far it went. */
    fun collapseBy(distance: Float): Float {
        val next = (collapsed + distance).coerceIn(0f, range)
        val moved = next - collapsed
        collapsed = next
        return moved
    }

    fun dock() {
        if (range > 0f) collapsed = range else dockOnMeasure = true
    }

    suspend fun animateExpand() {
        animate(collapsed, 0f) { value, _ -> collapsed = value }
    }

    companion object {
        val Saver: Saver<ProfileHeaderState, Any> = listSaver(
            save = { listOf(it.collapsed, it.docked) },
            restore = { ProfileHeaderState(it[0] as Float, dockOnMeasure = it[1] as Boolean) }
        )
    }
}

@Composable
internal fun rememberProfileHeaderState(): ProfileHeaderState =
    rememberSaveable(saver = ProfileHeaderState.Saver) { ProfileHeaderState() }

/**
 * The phone profile: the header, the tabs under it and the open tab's own list. Scrolling up folds
 * the header away before the list moves, scrolling down brings the list back to its top before the
 * header returns, and a drag that starts on the header carries on into the list, so the three move
 * as one page while each tab keeps a list, and a place, of its own.
 *
 * The list is as tall as the space under the docked tabs, so folding the header only moves it.
 */
@Composable
internal fun ProfileCollapsingLayout(
    state: ProfileHeaderState,
    listState: LazyListState,
    dockTargetPx: Int,
    header: @Composable () -> Unit,
    strip: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val list by rememberUpdatedState(listState)
    val foldFirst = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (available.y < 0f) Offset(0f, -state.collapseBy(-available.y)) else Offset.Zero
        }
    }
    val page = rememberScrollableState { delta ->
        if (delta < 0f) {
            val folded = state.collapseBy(-delta)
            -(folded + list.dispatchRawDelta(-delta - folded))
        } else {
            val listMoved = -list.dispatchRawDelta(-delta)
            listMoved - state.collapseBy(-(delta - listMoved))
        }
    }
    Layout(
        contents = listOf(header, strip, content),
        modifier = modifier
            .clipToBounds()
            .nestedScroll(foldFirst)
            .scrollable(page, Orientation.Vertical, overscrollEffect = null)
    ) { (headerMeasurables, stripMeasurables, contentMeasurables), constraints ->
        val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val headerPlaceable = headerMeasurables.first().measure(loose)
        val stripPlaceable = stripMeasurables.first().measure(loose)
        val range = (headerPlaceable.height - dockTargetPx).coerceAtLeast(0)
        state.onMeasured(range.toFloat())
        val contentHeight = (constraints.maxHeight - dockTargetPx - stripPlaceable.height).coerceAtLeast(0)
        val contentPlaceable = contentMeasurables.first().measure(Constraints.fixed(constraints.maxWidth, contentHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val top = headerPlaceable.height - state.collapsed.coerceIn(0f, range.toFloat()).roundToInt()
            headerPlaceable.place(0, top - headerPlaceable.height)
            stripPlaceable.place(0, top)
            contentPlaceable.place(0, top + stripPlaceable.height)
        }
    }
}
