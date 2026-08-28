package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.PaneDragHandle
import com.anisync.android.presentation.util.TwoPaneDefaults
import com.anisync.android.presentation.util.TwoPaneRow
import com.anisync.android.ui.theme.emphasis
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// The wide person screen is built like the wide profile: a shorter banner pinned on top, inset to
// the panes' width and rounded so it reads as a floating card, the portrait straddling its lower
// edge, and two resizable panes underneath — identity on the left, the tab-switched list on the
// right.
private val WideBannerHeight = 200.dp
private val WideBannerCollapsedHeight = 112.dp
private val WideBannerTopMargin = 12.dp

private val IdentityPanePadding = 20.dp
private val WideGutterStart = 16.dp

/** Portrait straddles the banner/pane seam exactly as the profile avatar does. */
private val WidePortraitWidth = 112.dp
private val WidePortraitHeight = WidePortraitWidth * 7 / 5
private val WidePortraitHalfHeight = WidePortraitHeight / 2
private val WidePortraitStartInset = WideGutterStart + IdentityPanePadding

/** Squarer than the panes below it: the banner is art, and a heavy radius eats into the picture. */
private val BannerShape = RoundedCornerShape(16.dp)

/** Gap between the banner's bottom edge and the name once it has handed off to the banner. */
private val BannerNameBottomInset = 16.dp

/** Room kept clear on the banner's end side for the app bar actions the name must not reach. */
private val BannerNameEndInset = 148.dp

/** Where in the name stage the pane copy has finished fading and the banner copy takes over. */
private const val NameHandoffPoint = 0.5f

/** Where in the identity stage the portrait and the meta block have finished fading. */
private const val IdentityFadeOutPoint = 0.62f

private val PERSON_FRACTION_ANCHORS = listOf(0.28f, 0.34f, 0.42f)
private const val PERSON_MIN_FRACTION = 0.24f
private const val PERSON_MAX_FRACTION = 0.48f

private fun nextPersonAnchor(fraction: Float): Float =
    PERSON_FRACTION_ANCHORS.firstOrNull { it > fraction + 0.01f } ?: PERSON_FRACTION_ANCHORS.first()

/**
 * How far the person header has collapsed as the identity pane scrolls, in pixels.
 *
 * The header costs a third of a 800dp window, and the identity pane below it is where the facts,
 * the aliases row and the biography live — on a short tablet they get a slot they can never grow
 * out of. Scrolling that pane spends the header in three stages, each handing its height down:
 *
 *  1. the clearance the straddling portrait needs, plus the native name / meta / favourites block —
 *     the portrait fades and recedes with it
 *  2. the name's row, which hands off to a copy drawn over the banner where the portrait was
 *  3. the banner itself, down to [WideBannerCollapsedHeight] — the only stage that also grows the
 *     right pane
 *
 * [collapsedPx] is written synchronously inside the nested-scroll callbacks, so the scroll reported
 * as consumed always matches how far the header actually moved.
 */
@Stable
private class PersonCollapseState(density: Density) {
    val portraitClearancePx = with(density) { WidePortraitHalfHeight.toPx() }

    private val bannerTopMarginPx = with(density) { WideBannerTopMargin.toPx() }
    private val bannerHeightPx = with(density) { WideBannerHeight.toPx() }
    private val bannerNameBottomInsetPx = with(density) { BannerNameBottomInset.toPx() }
    private val bannerStagePx =
        with(density) { (WideBannerHeight - WideBannerCollapsedHeight).toPx() }

    /** Natural heights, measured rather than assumed — the meta block and the name both wrap. */
    var detailsHeightPx by mutableIntStateOf(0)
    var nameHeightPx by mutableIntStateOf(0)
    var bannerNameHeightPx by mutableIntStateOf(0)

    var collapsedPx by mutableFloatStateOf(0f)
        private set

    private val identityStagePx: Float get() = portraitClearancePx + detailsHeightPx
    private val nameStagePx: Float get() = nameHeightPx.toFloat()

    val maxCollapsePx: Float get() = identityStagePx + nameStagePx + bannerStagePx

    /** Stage boundaries, which double as the only positions the header may rest at. */
    val stageAnchors: List<Float>
        get() = listOf(0f, identityStagePx, identityStagePx + nameStagePx, maxCollapsePx)

    private fun stageFraction(start: Float, span: Float): Float =
        if (span <= 0f) 0f else ((collapsedPx - start) / span).coerceIn(0f, 1f)

    val identityFraction: Float get() = stageFraction(0f, identityStagePx)
    val nameFraction: Float get() = stageFraction(identityStagePx, nameStagePx)
    val bannerFraction: Float get() = stageFraction(identityStagePx + nameStagePx, bannerStagePx)

    /** Pixels to trim off the banner's bottom edge right now. */
    val bannerTrimPx: Float get() = bannerFraction * bannerStagePx

    /** Top of the banner-overlay name, tracking the banner's bottom edge as the banner shrinks. */
    val bannerNameTopPx: Float
        get() = bannerTopMarginPx + bannerHeightPx - bannerTrimPx -
            bannerNameBottomInsetPx - bannerNameHeightPx

    fun snapTo(value: Float) {
        collapsedPx = value.coerceIn(0f, maxCollapsePx)
    }

    /**
     * Applies a scroll [delta] and returns how much of it the header absorbed, in the caller's sign
     * convention. Zero once fully open or fully closed, which hands the gesture to the pane.
     *
     * [paneOverflows] gates *collapsing* only: an identity pane that already fits has nothing to
     * gain from the space. Re-expanding is never gated, or a collapsed header could not be reopened.
     */
    fun consume(delta: Float, paneOverflows: Boolean): Float {
        val max = maxCollapsePx
        if (max <= 0f) return 0f
        if (delta < 0f && !paneOverflows) return 0f
        val next = (collapsedPx - delta).coerceIn(0f, max)
        val consumed = collapsedPx - next
        if (consumed != 0f) collapsedPx = next
        return consumed
    }
}

/**
 * Shrinks the node's reported height toward zero as [fraction] goes 0f..1f, sliding its content up
 * behind whatever sits above it and clipping the overflow. Read in the layout pass, so a scrolling
 * pane re-measures without recomposing.
 */
private fun Modifier.collapseVertically(fraction: () -> Float) = this
    .clipToBounds()
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val visible = (placeable.height * (1f - fraction().coerceIn(0f, 1f))).roundToInt()
        layout(placeable.width, visible) {
            placeable.place(0, visible - placeable.height)
        }
    }

/**
 * Trims [amountPx] off the node's bottom edge, top anchored — the banner keeps showing the top of
 * the image, which is where its subject usually is, rather than sliding out of frame.
 */
private fun Modifier.trimBottom(amountPx: () -> Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = (placeable.height - amountPx()).roundToInt().coerceIn(0, placeable.height)
    layout(placeable.width, height) { placeable.place(0, 0) }
}

/**
 * Expanded-width character / staff screen (M3 supporting pane), built to the same rules as
 * [com.anisync.android.presentation.profile.ProfileWideLayout]: banner card on top, portrait
 * straddling its edge, a resizable [TwoPaneRow] underneath carrying identity on the left and the
 * tabbed list on the right, and a header that collapses in stages as the identity pane scrolls.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PersonWideLayout(
    backdropUrl: String?,
    backdropCredit: String?,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    portraitUrl: String?,
    portraitTransitionKey: String,
    onPortraitClick: (() -> Unit)?,
    /** Drawn in the portrait slot instead of [portraitUrl]. The studio hands in its cover mark. */
    portraitContent: (@Composable () -> Unit)? = null,
    name: String,
    nativeName: String?,
    metaLine: String?,
    favourites: Int?,
    identityKey: Any,
    aliasLine: (@Composable () -> Unit)?,
    identityContent: @Composable ColumnScope.() -> Unit,
    tabs: @Composable () -> Unit,
    listContent: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val density = LocalDensity.current
    val collapse = remember(density, identityKey) { PersonCollapseState(density) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PersonBannerCard(
                backdropUrl = backdropUrl,
                backdropCredit = backdropCredit,
                onBackClick = onBackClick,
                actions = actions,
                modifier = Modifier
                    .padding(start = WideGutterStart, top = WideBannerTopMargin, end = 16.dp)
                    // Rounding sits outside the trim so the corners re-round against the collapsed
                    // height instead of being cut square.
                    .clip(BannerShape)
                    .trimBottom { collapse.bannerTrimPx }
            )

            PersonTwoPane(
                collapse = collapse,
                name = name,
                nativeName = nativeName,
                metaLine = metaLine,
                favourites = favourites,
                aliasLine = aliasLine,
                identityContent = identityContent,
                tabs = tabs,
                listContent = listContent,
                modifier = Modifier.weight(1f)
            )
        }

        // Faded-out overlays leave the accessibility tree too, and with the name drawn twice during
        // the handoff only one copy may ever be readable. Thresholded so crossing one costs a single
        // recomposition rather than one per frame.
        val portraitHidden by remember(collapse) {
            derivedStateOf { collapse.identityFraction >= IdentityFadeOutPoint }
        }
        val nameOnBanner by remember(collapse) {
            derivedStateOf { collapse.nameFraction >= NameHandoffPoint }
        }

        val portraitShape = RoundedCornerShape(16.dp)
        val portraitModifier = if (
            sharedTransitionScope != null && animatedVisibilityScope != null
        ) {
            val spatialSpec = AppMotion.rememberSpatialSpec()
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = portraitTransitionKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(portraitShape)
                )
            }
        } else {
            Modifier
        }

        // Portrait overlay: drawn last so it can straddle the banner / identity-pane seam without
        // the pane's rounded Surface clipping it. Aligned with the identity content's left edge.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = WidePortraitStartInset,
                    y = WideBannerTopMargin + WideBannerHeight - WidePortraitHalfHeight
                )
                .size(WidePortraitWidth, WidePortraitHeight)
                // Recedes into the banner as the header collapses: the pane's clearance vanishes
                // underneath it at the same rate, so it rides up and fades. All draw-phase, so a
                // scrolling identity pane never recomposes the portrait.
                .graphicsLayer {
                    val collapsed = collapse.identityFraction
                    val shrink = 1f - 0.3f * collapsed
                    translationY = -collapse.portraitClearancePx * collapsed
                    scaleX = shrink
                    scaleY = shrink
                    alpha = (1f - collapsed / IdentityFadeOutPoint).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0f, 1f)
                }
                .then(if (portraitHidden) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            val portraitSlot = Modifier
                .fillMaxSize()
                .then(portraitModifier)
                .clip(portraitShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (onPortraitClick != null) {
                        Modifier.clickable(onClick = onPortraitClick)
                    } else {
                        Modifier
                    }
                )

            if (portraitContent != null) {
                Box(modifier = portraitSlot) { portraitContent() }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(portraitUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = portraitSlot
                )
            }
        }

        // The name's destination: it leaves the pane in stage 2 and lands here, in the slot the
        // portrait vacated, riding the banner's bottom edge down as the banner shrinks.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .offset { IntOffset(0, collapse.bannerNameTopPx.roundToInt()) }
                .padding(start = WidePortraitStartInset, end = BannerNameEndInset)
                .graphicsLayer {
                    alpha = ((collapse.nameFraction - NameHandoffPoint) / (1f - NameHandoffPoint))
                        .coerceIn(0f, 1f)
                }
                .onSizeChanged { collapse.bannerNameHeightPx = it.height }
                .then(if (nameOnBanner) Modifier else Modifier.clearAndSetSemantics {})
        ) {
            BannerOverlayName(name = name)
        }
    }
}

/** The banner card itself: art, a scrim under the app bar icons, and the "borrowed from" credit. */
@Composable
private fun PersonBannerCard(
    backdropUrl: String?,
    backdropCredit: String?,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WideBannerHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (backdropUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.35f)
                    )
                )
        )
        // Chrome lives on the banner, the way the profile puts its settings/share action there.
        // A top app bar above the card would leave an empty band over it and, once the card has
        // collapsed to a strip, cover the name that just landed on it.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BannerIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBackClick
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = actions
        )

        if (backdropCredit != null) {
            Text(
                text = backdropCredit,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 12.dp)
            )
        }
    }
}

/** A chrome button on the banner: white on a soft scrim, so it survives any artwork under it. */
@Composable
fun BannerIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * The name once it has moved onto the banner: white with a shadow so it survives a bright banner,
 * one ellipsised line so a long name cannot run under the app bar actions in the opposite corner.
 */
@Composable
private fun BannerOverlayName(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 12f)
        ),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * The resizable body: rounded cards on the tinted gutter with a drag handle between them, the same
 * shape the profile and the calendar use. The split is session state rather than a stored setting —
 * a person screen is a visit, not a place the reader lives in.
 */
@Composable
private fun PersonTwoPane(
    collapse: PersonCollapseState,
    name: String,
    nativeName: String?,
    metaLine: String?,
    favourites: Int?,
    aliasLine: (@Composable () -> Unit)?,
    identityContent: @Composable ColumnScope.() -> Unit,
    tabs: @Composable () -> Unit,
    listContent: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    var leadingFraction by rememberSaveable { mutableFloatStateOf(PERSON_FRACTION_ANCHORS[1]) }
    var rowWidthPx by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    fun settleTo(target: Float) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(initialValue = leadingFraction, targetValue = target) { value, _ ->
                leadingFraction = value
            }
        }
    }

    val cycleLabel = stringResource(R.string.pane_resize_cycle)
    val resizeLabel = stringResource(R.string.pane_resize_handle)

    TwoPaneRow(
        leadingWeight = leadingFraction,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { rowWidthPx = it.width },
        gutterColor = MaterialTheme.colorScheme.surfaceContainer,
        gutterPadding = PaddingValues(
            start = WideGutterStart,
            top = 12.dp,
            end = 16.dp,
            bottom = 16.dp
        ),
        handle = {
            PaneDragHandle(
                modifier = Modifier.fillMaxHeight(),
                onDelta = { delta ->
                    if (rowWidthPx > 0) {
                        leadingFraction = (leadingFraction + delta / rowWidthPx)
                            .coerceIn(PERSON_MIN_FRACTION, PERSON_MAX_FRACTION)
                    }
                },
                onDragStarted = { settleJob?.cancel() },
                onDragStopped = {
                    settleTo(PERSON_FRACTION_ANCHORS.minBy { abs(it - leadingFraction) })
                },
                onClick = { settleTo(nextPersonAnchor(leadingFraction)) },
                clickLabel = cycleLabel,
                resizeLabel = resizeLabel
            )
        },
        leading = {
            PersonIdentityPane(
                collapse = collapse,
                name = name,
                nativeName = nativeName,
                metaLine = metaLine,
                favourites = favourites,
                aliasLine = aliasLine,
                identityContent = identityContent
            )
        },
        trailing = {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) { tabs() }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp + LocalMainNavBarInset.current),
                    content = listContent
                )
            }
        }
    )
}

/**
 * Left pane: the identity header over the scrolling facts / aliases / biography column.
 *
 * The header is only half fixed. Scrolling the column first folds the portrait clearance and the
 * meta block into nothing, then the name — see [PersonCollapseState] — which roughly doubles the
 * column's viewport on a short tablet. The name is never absent: it is either here or on the banner.
 */
@Composable
private fun PersonIdentityPane(
    collapse: PersonCollapseState,
    name: String,
    nativeName: String?,
    metaLine: String?,
    favourites: Int?,
    aliasLine: (@Composable () -> Unit)?,
    identityContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val detailsHidden by remember(collapse) {
        derivedStateOf { collapse.identityFraction >= IdentityFadeOutPoint }
    }
    val nameOnBanner by remember(collapse) {
        derivedStateOf { collapse.nameFraction >= NameHandoffPoint }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Clears the lower half of the portrait straddling the banner/pane seam. Collapses first,
        // in step with the portrait fading out above it.
        Spacer(
            modifier = Modifier
                .collapseVertically { collapse.identityFraction }
                .height(WidePortraitHalfHeight)
        )

        // Stage 2: the name folds up behind the pane's top edge while its banner counterpart fades
        // in, so it reads as the name flying up into the slot the portrait left.
        Box(
            modifier = Modifier
                .collapseVertically { collapse.nameFraction }
                .graphicsLayer {
                    alpha = (1f - collapse.nameFraction / NameHandoffPoint).coerceIn(0f, 1f)
                }
                .then(if (nameOnBanner) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = IdentityPanePadding, vertical = 4.dp)
                    .onSizeChanged { collapse.nameHeightPx = it.height }
            )
        }

        Box(
            modifier = Modifier
                .collapseVertically { collapse.identityFraction }
                // Faded out ahead of the height reaching zero so the text is gone before the clip
                // would slice it. Draw-phase read, so scrolling costs no recomposition.
                .graphicsLayer {
                    alpha = (1f - collapse.identityFraction / IdentityFadeOutPoint).coerceIn(0f, 1f)
                }
                .then(if (detailsHidden) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = IdentityPanePadding)
                    .onSizeChanged { collapse.detailsHeightPx = it.height }
            ) {
                if (nativeName != null) {
                    Text(
                        text = nativeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!metaLine.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.labelLarge.emphasis(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (aliasLine != null) {
                    Spacer(Modifier.height(6.dp))
                    aliasLine()
                }
                if (favourites != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = NumberFormat.getNumberInstance(Locale.getDefault())
                                .format(favourites),
                            style = MaterialTheme.typography.titleSmall.emphasis(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.person_favourites_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.nestedScroll(
                rememberPersonCollapseConnection(collapse, scrollState)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = IdentityPanePadding)
            ) {
                identityContent()
                Spacer(Modifier.height(24.dp))
            }
        }
        PersonCollapseSettler(collapse = collapse, scrollState = scrollState)
    }
}

/**
 * Nested-scroll wiring for the collapsing header: upward scroll folds the header away before the
 * pane moves at all, downward scroll re-opens it once the pane is back at its own top. Returning
 * zero at either end hands the gesture over to the pane.
 */
@Composable
private fun rememberPersonCollapseConnection(
    collapse: PersonCollapseState,
    scrollState: ScrollState
): NestedScrollConnection = remember(collapse, scrollState) {
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            // Let the pane scroll back to its own top before the header starts re-expanding,
            // otherwise the header pops open over content the reader is still in the middle of.
            if (delta > 0f && scrollState.canScrollBackward) return Offset.Zero
            val consumed = collapse.consume(delta, scrollState.maxValue > 0)
            return if (consumed == 0f) Offset.Zero else Offset(0f, consumed)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (available.y <= 0f) return Offset.Zero
            val absorbed = collapse.consume(available.y, scrollState.maxValue > 0)
            return if (absorbed == 0f) Offset.Zero else Offset(0f, absorbed)
        }
    }
}

/**
 * Settles the header onto the nearest stage boundary once the pane stops moving. Resting mid-stage
 * would strand the name half faded between the pane and the banner, so the stage edges are the only
 * valid rest positions.
 */
@Composable
private fun PersonCollapseSettler(collapse: PersonCollapseState, scrollState: ScrollState) {
    LaunchedEffect(collapse, scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }.collectLatest { scrolling ->
            if (scrolling) return@collectLatest
            if (collapse.maxCollapsePx <= 0f) return@collectLatest
            val target = collapse.stageAnchors.minBy { abs(it - collapse.collapsedPx) }
            if (collapse.collapsedPx != target) {
                animate(
                    initialValue = collapse.collapsedPx,
                    targetValue = target,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) { value, _ -> collapse.snapTo(value) }
            }
        }
    }
}
