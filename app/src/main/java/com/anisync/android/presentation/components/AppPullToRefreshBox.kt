package com.anisync.android.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.GestureThresholdDeactivate
import com.anisync.android.presentation.util.rememberHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Pull to refresh with the Material 3 Expressive loading indicator.
 *
 * A pull only starts when the finger lands with the content already at its top, so a swipe that
 * scrolls back up stops there instead of running on into a refresh. The content follows the
 * finger and the indicator grows in the gap it opens. It is armed once it reaches full size, and
 * letting go short of that, or pushing back above it, refreshes nothing.
 *
 * @param topInset where the content's visible top sits inside the box: the height of a bar drawn
 *   over the list, or the status bar on a page that draws under it. The indicator opens below it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    enabled: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val thresholdPx = with(LocalDensity.current) { PullThreshold.toPx() }
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticFeedback()
    val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val currentRefreshing by rememberUpdatedState(isRefreshing)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentHaptics by rememberUpdatedState(haptics)
    val currentFadeSpec by rememberUpdatedState(fadeSpec)

    val state = remember(scope, thresholdPx) {
        PullRefreshState(
            scope = scope,
            thresholdPx = thresholdPx,
            initiallyRefreshing = isRefreshing,
            isRefreshing = { currentRefreshing },
            enabled = { currentEnabled },
            onRefresh = { currentOnRefresh() },
            onArmedChange = { armed ->
                currentHaptics.performHapticFeedback(
                    if (armed) HapticFeedbackType.GestureThresholdActivate else GestureThresholdDeactivate
                )
            },
            fadeSpec = { currentFadeSpec },
        )
    }

    LaunchedEffect(state, isRefreshing) {
        if (isRefreshing) state.showRefreshing() else state.hideRefreshing()
    }

    Box(
        modifier = modifier.nestedScroll(state),
        contentAlignment = contentAlignment,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = state.contentFraction * thresholdPx },
            contentAlignment = contentAlignment,
            content = content,
        )
        PullIndicator(
            state = state,
            isRefreshing = isRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    val fraction = state.indicatorFraction
                    val scale = fraction.coerceAtMost(1f)
                    translationY = topInset.toPx() + fraction * thresholdPx / 2f - size.height / 2f
                    scaleX = scale
                    scaleY = scale
                    alpha = state.indicatorAlpha
                },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PullIndicator(
    state: PullRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulled by remember(state) { derivedStateOf { state.indicatorFraction > 0f } }
    if (!pulled && !isRefreshing) return

    Crossfade(
        targetState = isRefreshing,
        modifier = modifier.size(
            width = LoadingIndicatorDefaults.ContainerWidth,
            height = LoadingIndicatorDefaults.ContainerHeight,
        ),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "PullIndicator",
    ) { refreshing ->
        if (refreshing) {
            ContainedLoadingIndicator()
        } else {
            ContainedLoadingIndicator(
                progress = { state.indicatorFraction.coerceIn(0f, 1f) },
                modifier = Modifier.drawWithContent {
                    // The morph stops turning at the threshold, so a harder pull turns the whole shape.
                    val overshoot = state.indicatorFraction - 1f
                    if (overshoot > 0f) {
                        rotate(-overshoot * 180f) { this@drawWithContent.drawContent() }
                    } else {
                        drawContent()
                    }
                },
            )
        }
    }
}

/**
 * Pull distances are fractions of the threshold: 0 at rest, 1 at the threshold, up to 2 at the
 * hardest pull. The content and the indicator are tracked apart because the content springs back
 * as soon as a refresh starts while the indicator stays in view until it ends.
 */
@Stable
internal class PullRefreshState(
    private val scope: CoroutineScope,
    private val thresholdPx: Float,
    initiallyRefreshing: Boolean,
    private val isRefreshing: () -> Boolean,
    private val enabled: () -> Boolean,
    private val onRefresh: () -> Unit,
    private val onArmedChange: (armed: Boolean) -> Unit,
    private val fadeSpec: () -> AnimationSpec<Float>,
) : NestedScrollConnection {

    var contentFraction by mutableFloatStateOf(0f)
        private set
    var indicatorFraction by mutableFloatStateOf(if (initiallyRefreshing) 1f else 0f)
        private set
    var indicatorAlpha by mutableFloatStateOf(1f)
        private set

    private val mutex = MutatorMutex()
    private var settling = false
    private var showingRefresh = initiallyRefreshing
    private var distancePulled = 0f
    private var inGesture = false
    private var startedMidList = false

    // Same feel as Material's own: half the finger travel counts, and past the threshold the pull
    // stiffens until it tops out at twice the threshold.
    private val pullFraction: Float
        get() {
            val linear = distancePulled * DRAG_MULTIPLIER / thresholdPx
            if (linear <= 1f) return linear
            val overshoot = (linear - 1f).coerceAtMost(2f)
            return 1f + overshoot - overshoot * overshoot / 4f
        }

    private fun canPull() = enabled() && !isRefreshing() && !settling

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        beginGesture()
        // Pushing back up closes the pull before the list scrolls again.
        return if (available.y < 0f && distancePulled > 0f) {
            Offset(0f, pullBy(available.y))
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        beginGesture()
        if (consumed.y > 0f) startedMidList = true
        if (available.y <= 0f || startedMidList || !canPull()) return Offset.Zero
        return Offset(0f, pullBy(available.y))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        inGesture = false
        if (distancePulled == 0f) return Velocity.Zero
        val armed = pullFraction >= 1f
        distancePulled = 0f
        if (armed && canPull()) {
            onRefresh()
            scope.launch {
                park()
                // The screen turned the refresh down, rate limited for one.
                if (!isRefreshing()) fadeOut()
            }
        } else if (!isRefreshing()) {
            scope.launch { settleBack() }
        }
        return if (available.y > 0f) available else Velocity.Zero
    }

    suspend fun showRefreshing() {
        distancePulled = 0f
        park()
    }

    suspend fun hideRefreshing() {
        if (showingRefresh) fadeOut()
    }

    private fun beginGesture() {
        if (inGesture) return
        inGesture = true
        startedMidList = false
    }

    private fun pullBy(delta: Float): Float {
        val wasArmed = pullFraction >= 1f
        val pulled = (distancePulled + delta).coerceAtLeast(0f)
        val consumed = pulled - distancePulled
        distancePulled = pulled
        val fraction = pullFraction
        contentFraction = fraction
        indicatorFraction = fraction
        indicatorAlpha = 1f
        val armed = fraction >= 1f
        if (armed != wasArmed) onArmedChange(armed)
        return consumed
    }

    private suspend fun park() = settle {
        showingRefresh = true
        launch { animate(contentFraction, 0f, animationSpec = SettleSpec) { v, _ -> contentFraction = v } }
        launch { animate(indicatorFraction, 1f, animationSpec = SettleSpec) { v, _ -> indicatorFraction = v } }
        animate(indicatorAlpha, 1f, animationSpec = fadeSpec()) { v, _ -> indicatorAlpha = v }
    }

    private suspend fun fadeOut() = settle {
        launch { animate(contentFraction, 0f, animationSpec = SettleSpec) { v, _ -> contentFraction = v } }
        animate(indicatorAlpha, 0f, animationSpec = fadeSpec()) { v, _ -> indicatorAlpha = v }
        indicatorFraction = 0f
        indicatorAlpha = 1f
        showingRefresh = false
    }

    private suspend fun settleBack() = settle {
        launch { animate(contentFraction, 0f, animationSpec = SettleSpec) { v, _ -> contentFraction = v } }
        animate(indicatorFraction, 0f, animationSpec = SettleSpec) { v, _ -> indicatorFraction = v }
    }

    private suspend fun settle(block: suspend CoroutineScope.() -> Unit) = mutex.mutate {
        settling = true
        try {
            coroutineScope(block)
        } finally {
            settling = false
        }
    }
}

private val PullThreshold = 80.dp
private const val DRAG_MULTIPLIER = 0.5f
private val SettleSpec = spring<Float>(stiffness = Spring.StiffnessMedium)
