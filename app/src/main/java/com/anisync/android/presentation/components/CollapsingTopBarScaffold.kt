package com.anisync.android.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/**
 * App-wide collapsing hero top bar.
 *
 * The bar floats above [content]; the content scrolls underneath it and the bar shrinks from a
 * tall "hero" height down to a pinned minimum as the user scrolls up, then snaps fully open or
 * closed once scrolling stops.
 *
 * The bar height is updated *synchronously* inside the nested-scroll callbacks, so the amount of
 * scroll the bar reports as consumed always matches the amount it actually moved — no frame-lag
 * desync between the bar and the content padding that tracks it.
 *
 * PullToRefresh interaction: the bar collapses on upward scroll and expands on downward scroll
 * only while the content is already at the top. Once the bar is fully expanded it stops consuming
 * the downward gesture, so an inner `PullToRefreshBox` connection still gets to trigger a refresh.
 *
 * @param scrollableState the scroll state the [content] scrollable uses (`LazyListState`,
 *   `LazyGridState`, `ScrollState`, ...). Used both to detect "at the top" and to snap the bar
 *   open/closed when scrolling stops.
 * @param topBarModifier applied to the bar surface itself — e.g. a shared-transition overlay
 *   modifier.
 * @param actions trailing icons shown in the top-end corner of the bar.
 * @param belowBar optional fixed strip pinned directly under the bar (e.g. filter chips). It does
 *   not collapse, but tracks the bar's bottom edge as the bar collapses.
 * @param content receives the top inset the scrollable must leave clear for the bar (plus
 *   [belowBar], if any). Apply it as the scrollable's top `contentPadding`.
 */
@Composable
fun CollapsingTopBarScaffold(
    title: String,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    scrollableState: ScrollableState? = null,
    topBarModifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    scrolledContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    navigationIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack,
    actions: @Composable RowScope.() -> Unit = {},
    belowBar: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    enableEnterAnimation: Boolean = false,
    content: @Composable (topContentPadding: Dp) -> Unit,
) {
    val density = LocalDensity.current

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = if (title.length > 18) 200.dp else 170.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    // Synchronous height state — written directly inside the nested-scroll callbacks.
    var topBarHeightPx by remember(minTopBarHeightPx, maxTopBarHeightPx) {
        mutableFloatStateOf(maxTopBarHeightPx)
    }
    val collapseFraction by remember {
        derivedStateOf {
            1f - ((topBarHeightPx - minTopBarHeightPx) /
                (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
        }
    }

    // Snap the bar fully open/closed once the content stops scrolling. collectLatest cancels an
    // in-flight snap animation the moment the user grabs the list again.
    LaunchedEffect(scrollableState, minTopBarHeightPx, maxTopBarHeightPx) {
        if (scrollableState == null) return@LaunchedEffect
        snapshotFlow { scrollableState.isScrollInProgress }.collectLatest { scrolling ->
            if (scrolling) return@collectLatest
            val atTop = !scrollableState.canScrollBackward
            val shouldExpand = topBarHeightPx > (minTopBarHeightPx + maxTopBarHeightPx) / 2f
            val target = if (shouldExpand && atTop) maxTopBarHeightPx else minTopBarHeightPx
            if (topBarHeightPx != target) {
                animate(
                    initialValue = topBarHeightPx,
                    targetValue = target,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) { value, _ -> topBarHeightPx = value }
            }
        }
    }

    val nestedScrollConnection = remember(minTopBarHeightPx, maxTopBarHeightPx, scrollableState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                when {
                    // Scrolling content up — collapse the bar first.
                    delta < 0f -> {
                        val next = (topBarHeightPx + delta)
                            .coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                        val consumed = next - topBarHeightPx
                        if (consumed == 0f) return Offset.Zero
                        topBarHeightPx = next
                        return Offset(0f, consumed)
                    }
                    // Scrolling content down while it is already at the top — re-expand the bar
                    // before an inner PullToRefresh connection can claim the gesture.
                    delta > 0f && scrollableState?.canScrollBackward == false -> {
                        val next = (topBarHeightPx + delta)
                            .coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                        val consumed = next - topBarHeightPx
                        if (consumed == 0f) return Offset.Zero
                        topBarHeightPx = next
                        return Offset(0f, consumed)
                    }
                    else -> return Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Expand from any downward scroll left over after the content reached its top
                // (covers flings and the case where no scrollableState was supplied).
                val delta = available.y
                if (delta <= 0f) return Offset.Zero
                val next = (topBarHeightPx + delta)
                    .coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val grew = next - topBarHeightPx
                if (grew == 0f) return Offset.Zero
                topBarHeightPx = next
                return Offset(0f, grew)
            }
        }
    }

    val transitionState = remember { MutableTransitionState(!enableEnterAnimation) }
    LaunchedEffect(Unit) { transitionState.targetState = true }
    val transition = rememberTransition(transitionState, label = "CollapsingTopBarAppear")
    val contentAlpha by transition.animateFloat(
        label = "ContentAlpha",
        transitionSpec = { tween(durationMillis = 500) }
    ) { if (it) 1f else 0f }
    val contentOffset by transition.animateDp(
        label = "ContentOffset",
        transitionSpec = { tween(durationMillis = 400, easing = FastOutSlowInEasing) }
    ) { if (it) 0.dp else 40.dp }

    var belowBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }
    val belowBarHeightDp = with(density) { belowBarHeightPx.toDp() }

    // Same lerp the collapsing top bar uses, hoisted so the below-bar strip can
    // share it — `background` while expanded, `surfaceContainer` while collapsed.
    val barBackgroundColor = androidx.compose.ui.graphics.lerp(
        containerColor,
        scrolledContainerColor,
        collapseFraction
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
            .graphicsLayer {
                if (enableEnterAnimation) {
                    alpha = contentAlpha
                    translationY = contentOffset.toPx()
                }
            }
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                content(topBarHeightDp + belowBarHeightDp)
            }

            if (belowBar != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .offset { IntOffset(0, topBarHeightPx.roundToInt()) }
                        .background(barBackgroundColor)
                        .onSizeChanged { belowBarHeightPx = it.height }
                ) {
                    belowBar()
                }
            }

            CollapsibleCommonTopBar(
                title = title,
                collapseFraction = collapseFraction,
                headerHeight = topBarHeightDp,
                onBackClick = onBackClick,
                modifier = topBarModifier,
                containerColor = containerColor,
                scrolledContainerColor = scrolledContainerColor,
                navigationIcon = navigationIcon,
                actions = actions,
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                floatingActionButton()
            }
        }
        bottomBar()
    }
}

/**
 * The collapsing hero bar itself: a back button pinned top-start, optional [actions] top-end, and
 * a title that slides and shrinks from a large hero style to a compact app-bar style as
 * [collapseFraction] goes 0f -> 1f.
 *
 * When collapsed the title leaves exactly enough room on the end for whatever [actions] measured,
 * so a long title never runs underneath the action icons.
 */
@Composable
fun CollapsibleCommonTopBar(
    title: String,
    collapseFraction: Float,
    headerHeight: Dp,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    scrolledContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    navigationIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack,
    actions: @Composable RowScope.() -> Unit = {},
    maxLines: Int = 2,
    expandedTitleStartPadding: Dp = 16.dp,
    collapsedTitleStartPadding: Dp = 68.dp,
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val backgroundColor = androidx.compose.ui.graphics.lerp(
        containerColor,
        scrolledContainerColor,
        collapseFraction
    )

    var actionsWidthPx by remember { mutableIntStateOf(0) }
    val actionsWidthDp = with(density) { actionsWidthPx.toDp() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight),
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = statusBarHeight + 4.dp, start = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = stringResource(R.string.navigate_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = statusBarHeight + 4.dp, end = 4.dp)
                    .align(Alignment.TopEnd)
                    .onSizeChanged { actionsWidthPx = it.width },
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )

            // When there is no back button the title keeps its expanded inset even once collapsed.
            val collapsedStart =
                if (onBackClick != null) collapsedTitleStartPadding else expandedTitleStartPadding
            val titleStartPadding = androidx.compose.ui.unit.lerp(
                expandedTitleStartPadding,
                collapsedStart,
                collapseFraction
            )
            // Once collapsed, leave exactly enough room on the end for the measured actions.
            val titleEndPadding = androidx.compose.ui.unit.lerp(
                16.dp,
                actionsWidthDp + 12.dp,
                collapseFraction
            )
            val titleFontSize = androidx.compose.ui.unit.lerp(32.sp, 20.sp, collapseFraction)
            // Expanded: hero title sits near the bottom edge. Collapsed: the title's vertical
            // centre lines up with the back button / action icons (their centre is at
            // statusBar + 28dp; with a ~24dp tall collapsed title in a 64dp+statusBar bar that
            // means a 24dp bottom inset).
            val titleBottomPadding =
                androidx.compose.ui.unit.lerp(16.dp, 24.dp, collapseFraction)

            Text(
                text = title,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (collapseFraction > 0.8f) 1 else maxLines,
                overflow = TextOverflow.Ellipsis,
                lineHeight = titleFontSize * 1.2f,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = titleStartPadding,
                        end = titleEndPadding,
                        bottom = titleBottomPadding
                    )
            )
        }
    }
}
