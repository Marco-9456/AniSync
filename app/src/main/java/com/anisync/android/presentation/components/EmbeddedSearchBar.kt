package com.anisync.android.presentation.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalMainNavBarSuppressor
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Where the collapsed search bar sits, so [EmbeddedFullScreenSearchBar] grows out of it and lands
 * back on it. [Modifier.searchBarAnchor] goes on a box around the collapsed field rather than on
 * the field itself, whose modifiers change as the search opens and would leave the anchor pointing
 * at a detached node for that frame.
 */
@Stable
class SearchBarAnchor {
    internal var coordinates: LayoutCoordinates? by mutableStateOf(null)
}

fun Modifier.searchBarAnchor(anchor: SearchBarAnchor): Modifier =
    onGloballyPositioned { anchor.coordinates = it }

/** Keeps what the expanded bar covers away from screen readers, as the dialog window used to. */
@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.coveredBySearch(state: SearchBarState): Modifier =
    if (state.currentValue == SearchBarValue.Expanded) clearAndSetSemantics {} else this

/**
 * Keeps back presses away from what the expanded bar covers, as the dialog window used to.
 *
 * Back goes to the handler registered last, and covered content can register after the bar: a
 * detail pane composes inside its pane's subcomposition, so when the screen comes back, its handler
 * lands after the bar's and back closes the hidden pane instead of the search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepBackFromCovered(state: SearchBarState, content: @Composable () -> Unit) {
    val owner = rememberNavigationEventDispatcherOwner(
        enabled = state.currentValue != SearchBarValue.Expanded
    )
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner, content = content)
}

/**
 * Material's `ExpandedFullScreenSearchBar`, drawn in the screen's own composition instead of a
 * dialog window.
 *
 * The dialog sat above the navigation host, so a result could only open once the search had closed,
 * and back from it landed on the screen behind the search. Drawn here, the search is part of the
 * screen: a result covers it, and back uncovers it with its query, results and scroll position as
 * they were. The field takes focus when the user opens the search, not when the search comes back.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EmbeddedFullScreenSearchBar(
    state: SearchBarState,
    anchor: SearchBarAnchor,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    windowInsets: WindowInsets = SearchBarDefaults.fullScreenWindowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Outlives the expanded bar, so the collapse a back gesture starts runs to the end.
    val scope = rememberCoroutineScope()
    if (state.currentValue != SearchBarValue.Expanded) return

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val opening = remember { state.isAnimating }
    var fieldFocused by rememberSaveable { mutableStateOf(false) }
    var firstBackEvent by remember { mutableStateOf<BackEventCompat?>(null) }
    var lastBackEvent by remember { mutableStateOf<BackEventCompat?>(null) }

    PredictiveBackHandler(enabled = state.targetValue == SearchBarValue.Expanded) { events ->
        try {
            events.collect { event ->
                if (firstBackEvent == null) firstBackEvent = event
                lastBackEvent = event
            }
        } catch (e: CancellationException) {
            firstBackEvent = null
            lastBackEvent = null
            throw e
        }
        focusManager.clearFocus()
        keyboardController?.hide()
        scope.launch { state.animateToCollapsed() }
    }

    LaunchedEffect(Unit) {
        if (opening || fieldFocused) focusRequester.requestFocus()
        // A configuration change mid animation restores the bar part way open, with nothing left
        // to finish the move.
        if (!state.isAnimating && state.progress < 1f) state.animateToExpanded()
    }
    LaunchedEffect(state.targetValue) {
        if (state.targetValue == SearchBarValue.Collapsed) keyboardController?.hide()
    }

    // The bottom bar would sit over the results. A rail stays beside them. The bar stays away
    // while a result covers the search too, or it would flash in on the way back.
    val navBarSuppressor = LocalMainNavBarSuppressor.current
    val entryId = (LocalLifecycleOwner.current as? NavBackStackEntry)?.id
    val adaptive = LocalAdaptiveInfo.current
    val hidesBottomBar = adaptive.isCompact || adaptive.isCompactHeight
    DisposableEffect(navBarSuppressor, entryId, hidesBottomBar) {
        if (hidesBottomBar && entryId != null) navBarSuppressor?.hideOn(entryId)
        onDispose {
            if (entryId != null && state.currentValue == SearchBarValue.Collapsed) {
                navBarSuppressor?.showOn(entryId)
            }
        }
    }

    val density = LocalDensity.current
    val surfaceShape = remember(density) {
        GenericShape { size, _ ->
            val fraction = max(1 - state.progress, lastBackEvent.easedProgress())
            val radius = with(density) {
                (SearchBarDefaults.InputFieldHeight / 2 * fraction).toPx()
            }
            if (radius < 1e-3f) {
                addRect(size.toRect())
            } else {
                addRoundRect(RoundRect(size.toRect(), CornerRadius(radius)))
            }
        }
    }
    val inputFieldShape = SearchBarDefaults.inputFieldShape
    val unconsumedInsets = remember { MutableWindowInsets() }
    val nonTopInsets =
        unconsumedInsets.insets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)

    Layout(
        modifier = modifier
            // Takes the touches the screen beneath would get while the bar grows and shrinks.
            .pointerInput(Unit) {}
            .onConsumedWindowInsetsChanged { consumed ->
                unconsumedInsets.insets = windowInsets.exclude(consumed)
            }
            .consumeWindowInsets(windowInsets),
        content = {
            Box(
                modifier = Modifier
                    .layoutId(INPUT_FIELD_ID)
                    .padding(nonTopInsets.only(WindowInsetsSides.Horizontal).asPaddingValues())
                    .clip(inputFieldShape),
                propagateMinConstraints = true
            ) {
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { fieldFocused = it.hasFocus },
                    propagateMinConstraints = true
                ) {
                    inputField()
                }
            }
            Surface(
                modifier = Modifier.layoutId(SURFACE_ID),
                shape = surfaceShape,
                color = colors.containerColor,
                contentColor = contentColorFor(colors.containerColor),
                content = {}
            )
            Column(
                modifier = Modifier.layoutId(CONTENT_ID).padding(nonTopInsets.asPaddingValues())
            ) {
                HorizontalDivider(color = colors.dividerColor)
                content()
            }
        }
    ) { measurables, constraints ->
        val progress = state.progress
        val backProgress = lastBackEvent.easedProgress()
        val collapsedSize = anchor.coordinates?.takeIf { it.isAttached }?.size
        val collapsedWidth = collapsedSize?.width?.takeIf { it != 0 }
            ?: CollapsedMinWidth.roundToPx()
        val collapsedHeight = collapsedSize?.height?.takeIf { it != 0 }
            ?: SearchBarDefaults.InputFieldHeight.roundToPx()

        val backEndWidth = (constraints.maxWidth * BACK_MIN_SCALE).roundToInt()
            .coerceAtLeast(collapsedWidth)
        val backEndHeight = (constraints.maxHeight * BACK_MIN_SCALE).roundToInt()
            .coerceAtLeast(collapsedHeight)
        val endWidth = lerp(constraints.maxWidth, backEndWidth, backProgress)
        val endHeight = lerp(constraints.maxHeight, backEndHeight, backProgress)
        val width = constraints.constrainWidth(lerp(collapsedWidth, endWidth, progress))
        val height = constraints.constrainHeight(lerp(collapsedHeight, endHeight, progress))

        val surfacePlaceable = measurables.fastFirst { it.layoutId == SURFACE_ID }
            .measure(Constraints.fixed(width, height))
        val inputFieldPlaceable = measurables.fastFirst { it.layoutId == INPUT_FIELD_ID }
            .measure(Constraints.fixed(lerp(collapsedWidth, width, progress), collapsedHeight))

        val topPadding = unconsumedInsets.getTop(this) + VerticalPadding.roundToPx()
        val animatedTopPadding = lerp(0, topPadding, min(progress, 1 - backProgress))
        val bottomPadding = lerp(0, VerticalPadding.roundToPx(), progress)
        val fieldHeight = inputFieldPlaceable.height + animatedTopPadding + bottomPadding
        val contentPlaceable = measurables.fastFirst { it.layoutId == CONTENT_ID }.measure(
            Constraints(
                minWidth = width,
                maxWidth = width,
                minHeight = 0,
                maxHeight = (height - fieldHeight).coerceAtLeast(0)
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            val collapsed = anchor.coordinates?.takeIf { it.isAttached }?.let { source ->
                coordinates?.localBoundingBoxOf(source, clipBounds = false)?.roundToIntRect()
            } ?: IntRect.Zero
            val margin = BackMinMargin.roundToPx()

            // The back gesture shrinks the bar towards the side away from the swipe and lets it
            // follow the finger a little vertically, as Material's dialog does.
            val backOffsetX = lastBackEvent?.let { event ->
                val target = if (event.swipeEdge == BackEventCompat.EDGE_LEFT) {
                    constraints.maxWidth - margin - backEndWidth
                } else {
                    margin
                }
                target.coerceAtLeast(collapsed.right - backEndWidth).coerceAtMost(collapsed.left)
            } ?: 0
            val backOffsetY = lastBackEvent?.let { event ->
                val startY = firstBackEvent?.touchY ?: return@let 0
                val deltaY = event.touchY - startY
                val room = ((constraints.maxHeight - backEndHeight) / 2 - margin).coerceAtLeast(0)
                val reach = min(room, BackMaxOffsetY.roundToPx())
                val travel = lerp(0, reach, abs(deltaY) / constraints.maxHeight)
                (travel * sign(deltaY).toInt() + topPadding).coerceAtMost(collapsed.top)
            } ?: 0

            val offsetX = lerp(collapsed.left, lerp(0, backOffsetX, backProgress), progress)
            val offsetY = lerp(collapsed.top, lerp(0, backOffsetY, backProgress), progress)
            val centerX = lerp(collapsed.center.x.toFloat(), offsetX + width / 2f, progress)

            surfacePlaceable.place(offsetX, offsetY)
            inputFieldPlaceable.place(
                x = (centerX - inputFieldPlaceable.width / 2f).roundToInt(),
                y = offsetY + animatedTopPadding
            )
            contentPlaceable.placeWithLayer(
                x = offsetX,
                y = offsetY + animatedTopPadding + inputFieldPlaceable.height + bottomPadding
            ) {
                alpha = state.progress
            }
        }
    }
}

private fun BackEventCompat?.easedProgress(): Float =
    if (this == null) 0f else BackEasing.transform(progress)

private const val INPUT_FIELD_ID = "InputField"
private const val SURFACE_ID = "Surface"
private const val CONTENT_ID = "Content"

// Material's own values for the full screen bar, which it keeps internal.
private val CollapsedMinWidth = 360.dp
private val VerticalPadding = 8.dp
private const val BACK_MIN_SCALE = 9f / 10f
private val BackMinMargin = 8.dp
private val BackMaxOffsetY = 24.dp
private val BackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
