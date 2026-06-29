package com.anisync.android.presentation.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.anisync.android.R

/**
 * Shared low-level chrome for AniSync's two-pane surfaces (Material 3 panes): two rounded panes
 * floating side by side on a tinted gutter. Used by the on-demand list-detail scaffold
 * (`TwoPaneListDetailScaffold`) and the rich-text editor (`RichTextScaffold`) so both read as the
 * same "cards on a gutter" frame without duplicating the chrome.
 *
 * The primitive owns **layout + chrome only** — gutter background, rounded surfaces, pane gap, and
 * the placement of an optional drag handle. It holds **no** selection / split / persistence state;
 * the caller owns that and supplies the split via [leadingWeight] (and, when resizable, a [handle]
 * built from [PaneDragHandle]).
 */
/**
 * True when a detail screen is the **root** of a two-pane detail pane (the item selected from the
 * list), as opposed to a full-screen route or an entry the user drilled INTO within the pane.
 *
 * Per the Material 3 list-detail guidance a two-pane detail ROOT has **no back button** — selecting
 * another list item swaps it. So at the root each detail screen hides its leading back arrow and
 * instead shows a close (✕) action on the **trailing (right) edge** of its top bar, within easy
 * right-thumb reach. A DRILLED entry keeps its normal leading back arrow (to pop the pane's own
 * stack); a full-screen route is unaffected (default `false`).
 *
 * A drilling pane host (media browse/search, settings) provides `true` only while its nested nav is
 * at its start destination, `false` once something is pushed on top; a single-screen pane (feed /
 * forum / notifications) provides `true` around its always-root detail. One CompositionLocal drives
 * the close placement everywhere without threading a parameter through every detail screen + the 15
 * settings subscreens.
 */
val LocalPaneIsRoot = staticCompositionLocalOf { false }

/**
 * Window-relative bounds of the pane the reading composable lives in, or `null` outside any pane
 * (compact widths, full-screen routes, a full-width list). Published by [PaneSheetHost] inside each
 * pane of the two-pane surfaces and read by `AppModalBottomSheet`: a sheet whose actions affect only
 * one pane opens **inside that pane** instead of centring over the whole window, which would read as
 * acting on both panes. The state's value updates live as the pane is resized via its drag handle.
 *
 * Deliberately a [State] holder (not a plain Rect) so only readers of `.value` recompose on resize.
 */
val LocalPaneSheetBounds = staticCompositionLocalOf<State<Rect?>?> { null }

/**
 * Forces any `AppModalBottomSheet` composed within [content] onto the window-modal path by clearing
 * [LocalPaneSheetBounds], regardless of the pane the caller lives in. For sheets whose scope is the
 * whole window despite a pane call site: focused writing flows (the comment composer and its attach
 * sheet) and the search overlay's filters (the overlay covers both panes).
 */
@Composable
fun WindowModalSheetScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPaneSheetBounds provides null) { content() }
}

/**
 * Marks [content] as living inside a pane: measures the pane's window bounds and publishes them via
 * [LocalPaneSheetBounds] so pane-aware overlays (`AppModalBottomSheet`) can anchor to this pane.
 * Insert as the direct child of a pane surface — it fills the pane and adds no visuals.
 */
@Composable
fun PaneSheetHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val bounds = remember { mutableStateOf<Rect?>(null) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { bounds.value = it.boundsInWindow() },
    ) {
        CompositionLocalProvider(LocalPaneSheetBounds provides bounds) { content() }
    }
}

object TwoPaneDefaults {
    /** Rounded corners shared by both panes (all four corners). */
    val PaneShape = RoundedCornerShape(24.dp)

    /**
     * Gutter inset around the panes. Start is 0 so the leading pane can sit flush against an
     * adjacent navigation rail; top/end/bottom keep the margin so the panes read as floating cards.
     */
    val GutterPadding = PaddingValues(start = 0.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)

    /** Width of the gutter gap / drag handle column between the two panes. */
    val PaneGap = 24.dp

    /** Gutter tone — shares the navigation rail's and status-bar protection's surfaceContainer. */
    val gutterColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    /** Pane (card) tone — a step darker than the gutter so the cards read as floating within it. */
    val paneColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
}

/**
 * Lays out [leading] and [trailing] as two rounded [TwoPaneDefaults.paneColor] panes on a
 * [TwoPaneDefaults.gutterColor] gutter, split by [leadingWeight] (the leading pane's flex weight;
 * the trailing pane gets the remainder). Both [leadingWeight] and `1 - leadingWeight` must be > 0.
 *
 * Between the panes sits [handle] (a [PaneDragHandle] for resizable splits) or, when null, a fixed
 * [TwoPaneDefaults.PaneGap] spacer for a non-resizable split. The caller measures the row width (for
 * any drag math) by attaching `Modifier.onSizeChanged { … }` to [modifier].
 *
 * The chrome tones default to [TwoPaneDefaults] (the list-detail look), but [gutterColor],
 * [leadingColor], [trailingColor], [gutterPadding] and [shape] are overridable so a caller can give
 * its panes a distinct look — e.g. the editor uses bright `surface` document cards on a symmetric
 * gutter (no flush-against-rail inset, since no navigation rail is shown there).
 */
@Composable
fun TwoPaneRow(
    leadingWeight: Float,
    modifier: Modifier = Modifier,
    gutterColor: Color = TwoPaneDefaults.gutterColor,
    gutterPadding: PaddingValues = TwoPaneDefaults.GutterPadding,
    leadingColor: Color = TwoPaneDefaults.paneColor,
    trailingColor: Color = TwoPaneDefaults.paneColor,
    shape: Shape = TwoPaneDefaults.PaneShape,
    handle: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .background(gutterColor)
            .padding(gutterPadding),
    ) {
        Surface(
            modifier = Modifier.weight(leadingWeight).fillMaxHeight(),
            shape = shape,
            color = leadingColor,
        ) { PaneSheetHost { leading() } }

        if (handle != null) handle() else Spacer(Modifier.width(TwoPaneDefaults.PaneGap))

        Surface(
            modifier = Modifier.weight(1f - leadingWeight).fillMaxHeight(),
            shape = shape,
            color = trailingColor,
        ) { PaneSheetHost { trailing() } }
    }
}

/**
 * The shared resize affordance for [TwoPaneRow]: an M3 [VerticalDragHandle] in a
 * [TwoPaneDefaults.PaneGap]-wide touch column. [modifier] sets its placement (e.g.
 * `Modifier.fillMaxHeight()` for the gutter column, or `Modifier.align(…).height(…)` overlaid on a
 * pane). Horizontal drag reports its raw pixel [onDelta]; the caller converts that to a fraction.
 *
 * [onClick] / [onLongClick] are optional — supply them for tap/long-press affordances (the
 * list-detail scaffold cycles / collapses the split); omit them for a drag-only handle (the editor).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun PaneDragHandle(
    onDelta: (Float) -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    longClickLabel: String? = null,
    resizeLabel: String = stringResource(R.string.pane_resize_handle),
) {
    Box(
        modifier = modifier
            .width(TwoPaneDefaults.PaneGap)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> onDelta(delta) },
                interactionSource = interactionSource,
                onDragStarted = { onDragStarted() },
                onDragStopped = { onDragStopped() },
            )
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClickLabel = clickLabel,
                        onLongClickLabel = longClickLabel,
                        onLongClick = onLongClick,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            // Keep the drag from colliding with the system back gesture at the edge.
            .systemGestureExclusion(),
        contentAlignment = Alignment.Center,
    ) {
        VerticalDragHandle(
            interactionSource = interactionSource,
            modifier = Modifier.semantics { contentDescription = resizeLabel },
        )
    }
}

/**
 * Selection treatment for a list item in a two-pane list-detail layout: an outline ring in the
 * primary color tracing the item's [shape]. Per the Material 3 list-detail guidance the selected
 * state is shown **only** in the list pane of a two-pane layout — callers thread the open item's id
 * down from `TwoPaneListDetailScaffold` and pass `selected = (id == selectedId)`. On compact widths
 * the scaffold isn't composed, so the list renders with no selection and this is a no-op; likewise
 * non-pane callers (default `selected = false`) are unaffected.
 *
 * Applied via each card's existing `modifier`, so the ring traces the card's outer bounds without
 * touching its internals or its shared-element treatment.
 */
@Composable
fun Modifier.selectedPaneItem(
    selected: Boolean,
    shape: Shape = RoundedCornerShape(16.dp),
): Modifier = if (!selected) {
    this
} else {
    this.border(
        width = 2.dp,
        color = MaterialTheme.colorScheme.primary,
        shape = shape,
    )
}
