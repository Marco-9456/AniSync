package com.anisync.android.presentation.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 */
@Composable
fun TwoPaneRow(
    leadingWeight: Float,
    modifier: Modifier = Modifier,
    gutterPadding: PaddingValues = TwoPaneDefaults.GutterPadding,
    handle: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .background(TwoPaneDefaults.gutterColor)
            .padding(gutterPadding),
    ) {
        Surface(
            modifier = Modifier.weight(leadingWeight).fillMaxHeight(),
            shape = TwoPaneDefaults.PaneShape,
            color = TwoPaneDefaults.paneColor,
        ) { leading() }

        if (handle != null) handle() else Spacer(Modifier.width(TwoPaneDefaults.PaneGap))

        Surface(
            modifier = Modifier.weight(1f - leadingWeight).fillMaxHeight(),
            shape = TwoPaneDefaults.PaneShape,
            color = TwoPaneDefaults.paneColor,
        ) { trailing() }
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
