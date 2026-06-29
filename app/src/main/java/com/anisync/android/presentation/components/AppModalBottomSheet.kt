package com.anisync.android.presentation.components

import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.anisync.android.R
import com.anisync.android.presentation.util.LocalPaneSheetBounds
import com.anisync.android.presentation.util.TwoPaneDefaults
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Breathing room kept above a pane-anchored sheet so it never fully covers its pane, mirroring the
// modal bottom sheet's status-bar clearance on phones.
private val PaneSheetTopClearance = 48.dp

// Flinging down faster than this dismisses the sheet regardless of how far it has been dragged.
private val DismissVelocityThreshold = 250.dp

/**
 * AniSync's modal bottom sheet. On phones (and anywhere outside a pane) it is exactly a Material 3
 * [ModalBottomSheet]. Inside a pane of a two-pane layout ([LocalPaneSheetBounds] published by
 * `PaneSheetHost`) it instead anchors to the **owning pane**: the sheet and its scrim cover only
 * that pane, so an edit affecting one pane no longer reads as acting on the whole window. Taps
 * outside the pane, the scrim, the back gesture and a downward drag all dismiss it, matching the
 * modal sheet's behaviour.
 *
 * Drop-in for the [ModalBottomSheet] call sites: same parameter names/defaults. [sheetState] and
 * [properties]-style knobs only apply to the window-modal fallback; the pane-anchored variant is
 * always skip-partially-expanded.
 *
 * [confirmDismiss] is the pane-anchored counterpart of a [SheetState]'s `confirmValueChange` gate:
 * it runs before a user-initiated dismissal (scrim tap, back, drag past the threshold) and vetoes it
 * by returning false — use it to interpose a "discard draft?" dialog. The window-modal fallback
 * ignores it (those callers already gate through their [sheetState]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    confirmDismiss: () -> Boolean = { true },
    content: @Composable ColumnScope.() -> Unit,
) {
    val paneBounds = LocalPaneSheetBounds.current
    if (paneBounds?.value == null) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            sheetMaxWidth = sheetMaxWidth,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            scrimColor = scrimColor,
            dragHandle = dragHandle,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    } else {
        PaneAnchoredSheet(
            bounds = paneBounds,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetMaxWidth = sheetMaxWidth,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            scrimColor = scrimColor,
            dragHandle = dragHandle,
            confirmDismiss = confirmDismiss,
            content = content,
        )
    }
}

/**
 * The pane-anchored variant: a focusable popup laid over exactly the pane's window bounds (tracking
 * live pane resizes), holding a pane-wide scrim and the sheet surface at the pane's bottom edge.
 * Content insets are irrelevant here — the pane already floats inside the window's safe area — but
 * the sheet still lifts above the IME (computed against the app window, so it works whether or not
 * the popup's own window receives IME insets).
 */
@Composable
private fun PaneAnchoredSheet(
    bounds: State<Rect?>,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetMaxWidth: Dp,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    scrimColor: Color,
    dragHandle: @Composable (() -> Unit)?,
    confirmDismiss: () -> Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val rect = bounds.value ?: return
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // IME overlap with the pane, measured in the APP window (this composable's window). Combined
    // inside the popup with the popup window's own IME insets — whichever window the system actually
    // reports them to (the popup is the IME target, so usually that one).
    val containerHeight = LocalWindowInfo.current.containerSize.height
    val outerImeBottom = WindowInsets.ime.getBottom(density)
    val outerOverlapPx =
        (outerImeBottom - (containerHeight - rect.bottom)).coerceAtLeast(0f).roundToInt()

    // Enter animates on first composition; dismissal animates out, THEN reports to the caller.
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    var dismissing by remember { mutableStateOf(false) }
    fun performDismiss() {
        dismissing = true
        visibleState.targetState = false
    }
    fun startDismiss() {
        if (confirmDismiss()) performDismiss()
    }
    LaunchedEffect(visibleState.currentState, visibleState.isIdle) {
        if (dismissing && !visibleState.currentState && visibleState.isIdle) onDismissRequest()
    }

    val positionProvider = remember(rect) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt())
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = ::startDismiss,
        // clippingEnabled=false: clipping clamps the popup inside the cutout-safe display frame,
        // shifting it off the pane on devices whose camera cutout borders the pane's edge — the app
        // window (and the pane) already draw edge-to-edge, so position exactly on the pane instead.
        properties = PopupProperties(focusable = true, clippingEnabled = false),
    ) {
        val paneWidth = with(density) { rect.width.toDp() }
        val paneHeight = with(density) { rect.height.toDp() }
        // Read inside the popup composition: resolves against the popup window's own insets. The
        // popup is sized to the pane, so its IME inset IS the pane overlap.
        val innerImeBottom = WindowInsets.ime.getBottom(density)
        val imeOverlap = with(density) { maxOf(outerOverlapPx, innerImeBottom).toDp() }

        // Compose popups are created with SOFT_INPUT_ADJUST_PAN and this popup is the IME input
        // target for any text field in the sheet, so without intervention the keyboard just covers
        // it. Switch the popup window to ADJUST_RESIZE: the window shrinks above the IME and the
        // bottom-aligned sheet stays visible. (PopupProperties exposes no soft-input knob.)
        val composeView = LocalView.current
        SideEffect {
            val popupWindowView = composeView.parent as? View ?: return@SideEffect
            val params = popupWindowView.layoutParams as? WindowManager.LayoutParams
                ?: return@SideEffect
            val adjustResize = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            if (params.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST != adjustResize) {
                params.softInputMode = (
                    params.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()
                    ) or adjustResize
                (popupWindowView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .updateViewLayout(popupWindowView, params)
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                // size() coerces into the window's constraints: when the IME resizes the popup, the
                // box shrinks with it and the bottom-aligned sheet stays above the keyboard.
                .size(paneWidth, paneHeight)
                // The pane's own rounded corners, so the sheet + scrim never poke past them.
                .clip(TwoPaneDefaults.PaneShape),
        ) {
            // The height actually available after any IME-driven window resize.
            val availableHeight = maxHeight
            AnimatedVisibility(visibleState, enter = fadeIn(), exit = fadeOut()) {
                val closeLabel = stringResource(R.string.pane_close)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrimColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = closeLabel,
                        ) { startDismiss() },
                )
            }

            AnimatedVisibility(
                visibleState,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                var sheetHeightPx by remember { mutableIntStateOf(0) }
                val dragOffset = remember { Animatable(0f) }
                val dismissVelocityPx = with(density) { DismissVelocityThreshold.toPx() }

                fun settle(velocity: Float) {
                    scope.launch {
                        val wantsDismiss = sheetHeightPx > 0 &&
                            (velocity > dismissVelocityPx ||
                                (dragOffset.value > sheetHeightPx / 2f && velocity > -dismissVelocityPx))
                        if (wantsDismiss && confirmDismiss()) {
                            dragOffset.animateTo(sheetHeightPx.toFloat(), initialVelocity = velocity)
                            performDismiss()
                        } else {
                            // Vetoed or below the threshold — spring the sheet back into place.
                            dragOffset.animateTo(0f, initialVelocity = velocity)
                        }
                    }
                }

                // Standard sheet nested-scroll contract: dragging the content down past its top pulls
                // the sheet with it; scrolling back up retracts the sheet before the content scrolls.
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            val delta = available.y
                            if (source == NestedScrollSource.UserInput && delta < 0 && dragOffset.value > 0f) {
                                val consumed = delta.coerceAtLeast(-dragOffset.value)
                                scope.launch { dragOffset.snapTo(dragOffset.value + consumed) }
                                return Offset(0f, consumed)
                            }
                            return Offset.Zero
                        }

                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource,
                        ): Offset {
                            val delta = available.y
                            if (source == NestedScrollSource.UserInput && delta > 0) {
                                scope.launch { dragOffset.snapTo(dragOffset.value + delta) }
                                return Offset(0f, delta)
                            }
                            return Offset.Zero
                        }

                        override suspend fun onPreFling(available: Velocity): Velocity {
                            if (dragOffset.value > 0f) {
                                settle(available.y)
                                return available
                            }
                            return Velocity.Zero
                        }
                    }
                }

                Surface(
                    modifier = modifier
                        .widthIn(max = sheetMaxWidth)
                        .fillMaxWidth()
                        // Cap to the pane minus top clearance only: the IME overlap is handled by the
                        // bottom padding below, which both lifts the sheet's visible body above the
                        // keyboard AND shrinks the content area — subtracting it here too would
                        // double-count and squeeze the content to nothing.
                        .heightIn(max = (availableHeight - PaneSheetTopClearance).coerceAtLeast(0.dp))
                        .onSizeChanged { sheetHeightPx = it.height }
                        .offset { IntOffset(0, dragOffset.value.roundToInt().coerceAtLeast(0)) }
                        .nestedScroll(nestedScrollConnection)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                scope.launch {
                                    dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f))
                                }
                            },
                            onDragStopped = { velocity -> settle(velocity) },
                        )
                        .padding(bottom = imeOverlap),
                    shape = shape,
                    color = containerColor,
                    contentColor = contentColor,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (dragHandle != null) {
                            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) { dragHandle() }
                        }
                        content()
                    }
                }
            }
        }
    }
}
