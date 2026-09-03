package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.profile.components.ProfileActionButtons
import com.anisync.android.presentation.profile.components.ProfileAvatarHalfSize
import com.anisync.android.presentation.profile.components.ProfileAvatarSize
import com.anisync.android.presentation.profile.components.ProfileBannerSurface
import com.anisync.android.presentation.profile.components.ProfileDisplayName
import com.anisync.android.presentation.profile.components.ProfileIdentityDetails
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.PaneDragHandle
import com.anisync.android.presentation.util.profileGridColumns
import com.anisync.android.presentation.util.TwoPaneDefaults
import com.anisync.android.presentation.util.TwoPaneRow
import com.anisync.android.util.ShareUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// The wide profile reads as a dashboard: a shorter banner pinned on top (inset to the panes' width
// and rounded to match them — a floating card), the avatar straddling its lower edge, and below it
// two resizable panes — identity + bio on the left, the tab-switched content on the right.
private val WideBannerHeight = 200.dp
private val WideBannerTopMargin = 12.dp

// Horizontal offset of the overlay avatar = the two-pane gutter start + the identity pane's own
// horizontal padding, so the avatar's left edge lines up with the name below it.
private val IdentityPanePadding = 20.dp
private val WideGutterStart = 16.dp
private val WideAvatarStartInset = WideGutterStart + IdentityPanePadding

// EXPERIMENTAL: let the shared banner shrink along with the identity header, which grows *both*
// panes rather than only the bio's. Flip to false to drop that last stage and leave the banner the
// fixed strip it used to be — nothing else needs touching.
private const val COLLAPSE_BANNER_WITH_BIO = true
private val WideBannerCollapsedHeight = 96.dp

/** Gap between the banner's bottom edge and the display name once it has handed off to the banner. */
private val BannerNameBottomInset = 16.dp

/** Room kept clear on the banner's end side for the settings/share button the name must not reach. */
private val BannerNameEndInset = 88.dp

/**
 * Point within the name stage where the pane copy has finished fading and the banner copy takes
 * over. The two fades and the accessibility handoff all derive from this, so they cannot drift apart
 * and leave the name shown twice or not at all.
 */
private const val NameHandoffPoint = 0.5f

/**
 * Point within the identity stage where the avatar and the badge/meta block have finished fading.
 * They are invisible from here on, so this is also where they leave the accessibility tree — a
 * separate "fully collapsed" threshold would leave a window where TalkBack still reached them.
 */
private const val IdentityFadeOutPoint = 0.62f

private val PROFILE_FRACTION_ANCHORS = listOf(0.26f, 0.32f, 0.40f)
private const val PROFILE_MIN_FRACTION = 0.24f
private const val PROFILE_MAX_FRACTION = 0.46f

private fun nextProfileAnchor(fraction: Float): Float =
    PROFILE_FRACTION_ANCHORS.firstOrNull { it > fraction + 0.01f } ?: PROFILE_FRACTION_ANCHORS.first()

/**
 * How far the profile header has collapsed as the inline bio scrolls, in pixels.
 *
 * The identity pane's fixed chrome costs ~260dp of a pane only ~370dp tall on a short tablet,
 * leaving the bio a ~110dp slot it can never grow out of. Scrolling the bio spends that chrome in
 * three stages, each of which hands its height to the bio:
 *
 *  1. the clearance the overlay avatar needs, plus the badge/meta block — the avatar fades out with it
 *  2. the display name's row, which hands off to a copy drawn over the banner where the avatar was
 *  3. the banner itself, down to [WideBannerCollapsedHeight] — experimental, see
 *     [COLLAPSE_BANNER_WITH_BIO], and the only stage that grows the *right* pane too
 *
 * The action buttons never move, so following or messaging is always one tap away, and the name is
 * never absent — it is either in the pane or on the banner.
 *
 * [collapsedPx] is written *synchronously* inside the nested-scroll callbacks, so the scroll this
 * reports as consumed always matches how far the header actually moved, exactly as
 * [com.anisync.android.presentation.components.CollapsingTopBarScaffold] does it.
 */
@Stable
private class BioCollapseState(density: Density) {
    /** Height of the avatar-clearance spacer, the first thing to give way. */
    val avatarClearancePx = with(density) { ProfileAvatarHalfSize.toPx() }

    private val bannerTopMarginPx = with(density) { WideBannerTopMargin.toPx() }
    private val bannerHeightPx = with(density) { WideBannerHeight.toPx() }
    private val bannerNameBottomInsetPx = with(density) { BannerNameBottomInset.toPx() }
    private val bannerStagePx = with(density) {
        if (COLLAPSE_BANNER_WITH_BIO) (WideBannerHeight - WideBannerCollapsedHeight).toPx() else 0f
    }

    /** Natural heights, measured rather than assumed — the badge row wraps and the name can too. */
    var detailsHeightPx by mutableIntStateOf(0)
    var nameHeightPx by mutableIntStateOf(0)
    var bannerNameHeightPx by mutableIntStateOf(0)

    var collapsedPx by mutableFloatStateOf(0f)
        private set

    private val identityStagePx: Float get() = avatarClearancePx + detailsHeightPx
    private val nameStagePx: Float get() = nameHeightPx.toFloat()

    val maxCollapsePx: Float get() = identityStagePx + nameStagePx + bannerStagePx

    /** Stage boundaries, which double as the only positions the header is allowed to rest at. */
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
     * Applies a scroll [delta] to the header and returns how much of it the header absorbed, in the
     * same sign convention the caller uses. Zero once the header is fully open or fully closed, which
     * is what lets the bio take over the gesture.
     *
     * [bioOverflows] gates *collapsing* only: a bio that already fits has nothing to gain from the
     * space, so a short one leaves the header alone entirely instead of stranding it above whitespace.
     * Re-expanding is never gated, or a collapsed header could not be reopened once the bio fits.
     */
    fun consume(delta: Float, bioOverflows: Boolean): Float {
        val max = maxCollapsePx
        if (max <= 0f) return 0f
        if (delta < 0f && !bioOverflows) return 0f
        val next = (collapsedPx - delta).coerceIn(0f, max)
        val consumed = collapsedPx - next
        if (consumed != 0f) collapsedPx = next
        return consumed
    }
}

/**
 * Shrinks the node's reported height toward zero as [fraction] goes 0f..1f, sliding its content up
 * behind whatever sits above it and clipping the overflow.
 *
 * [fraction] is a lambda so the read lands in the layout pass instead of composition — the header
 * re-measures as the bio scrolls, but nothing recomposes.
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
 * Trims [amountPx] off the node's bottom edge with its top left anchored — the banner keeps showing
 * the top of the image, which is where its subject usually is, instead of sliding out of frame.
 *
 * Deliberately does not clip: the caller's rounded [TwoPaneDefaults.PaneShape] clip sits outside
 * this, so the corners re-round against the trimmed height rather than the natural one.
 */
private fun Modifier.trimBottom(amountPx: () -> Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = (placeable.height - amountPx()).roundToInt().coerceIn(0, placeable.height)
    layout(placeable.width, height) { placeable.place(0, 0) }
}

/**
 * Expanded-width profile (M3 supporting-pane): a full-width banner over a resizable [TwoPaneRow] —
 * the **left** pane is the profile identity (avatar + name/badges/meta + action buttons + the bio
 * shown inline, replacing the compact "View Biography" sheet); the **right** pane carries the tab
 * group on top and shows the selected tab's content. Compact keeps the single-column profile.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileWideLayout(
    profile: UserProfile,
    uiState: ProfileUiState,
    isOwnProfile: Boolean,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    onAction: (ProfileAction) -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationCount: Int,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onVoiceActorClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit,
    onCommentClick: (threadId: Int, commentId: Int, threadTitle: String) -> Unit,
    onActivityClick: (Int) -> Unit,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    showAccountSwitcher: Boolean,
    onAccountSwitchClick: () -> Unit,
    statsColumns: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Owned here rather than in the identity pane because the overlay avatar below is drawn by this
    // Box, and has to recede in step with the clearance disappearing underneath it.
    //
    // Keyed on the bio's presence as well as the profile: only the bio's scroll can re-expand the
    // header, so a profile whose bio is edited away while collapsed would otherwise be stuck that way.
    // Rebuilding the state hands it back fully expanded.
    val density = LocalDensity.current
    val hasBio = !profile.about.isNullOrBlank()
    val collapse = remember(density, profile.id, hasBio) { BioCollapseState(density) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProfileBannerSurface(
                profile = profile,
                isOwnProfile = isOwnProfile,
                topActionIcon = if (isOwnProfile) Icons.Default.Settings else Icons.Default.Share,
                onTopActionClick = {
                    if (isOwnProfile) {
                        onSettingsClick()
                    } else {
                        ShareUtils.shareText(
                            context = context,
                            text = "${profile.name}\nhttps://anilist.co/user/${profile.name}"
                        )
                    }
                },
                height = WideBannerHeight,
                modifier = Modifier
                    .padding(
                        start = WideGutterStart,
                        top = WideBannerTopMargin,
                        end = 16.dp
                    )
                    // Rounding moved out of the banner and applied here, *outside* the trim, so the
                    // corners re-round against the collapsed height instead of being cut square.
                    .clip(TwoPaneDefaults.PaneShape)
                    .trimBottom { collapse.bannerTrimPx },
                shape = RectangleShape
            )

            ProfileTwoPane(
                profile = profile,
                uiState = uiState,
                isOwnProfile = isOwnProfile,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onAction = onAction,
                onNotificationsClick = onNotificationsClick,
                unreadNotificationCount = unreadNotificationCount,
                onMediaClick = onMediaClick,
                onCharacterClick = onCharacterClick,
                onStaffClick = onStaffClick,
                onVoiceActorClick = onVoiceActorClick,
                onStudioClick = onStudioClick,
                onUserClick = onUserClick,
                onThreadClick = onThreadClick,
                onCommentClick = onCommentClick,
                onActivityClick = onActivityClick,
                onLastReplyClick = onLastReplyClick,
                showAccountSwitcher = showAccountSwitcher,
                onAccountSwitchClick = onAccountSwitchClick,
                statsColumns = statsColumns,
                collapse = collapse,
                modifier = Modifier.weight(1f)
            )
        }

        // Faded-out overlays must leave the accessibility tree too, or TalkBack still stops on things
        // nobody can see — and with the name drawn twice during the handoff, only one copy may ever be
        // readable. Thresholded through derivedStateOf so crossing one costs a single recomposition
        // rather than one per frame.
        val avatarHidden by remember(collapse) {
            derivedStateOf { collapse.identityFraction >= IdentityFadeOutPoint }
        }
        val nameOnBanner by remember(collapse) {
            derivedStateOf { collapse.nameFraction >= NameHandoffPoint }
        }

        // Avatar overlay: drawn last (above the banner and panes) so it can straddle the banner /
        // identity-pane seam without being clipped by the pane's rounded Surface. Aligned with the
        // identity content's left edge.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = WideAvatarStartInset,
                    y = WideBannerTopMargin + WideBannerHeight - ProfileAvatarHalfSize
                )
                .height(ProfileAvatarSize)
                // Recedes into the banner as the identity header collapses: the pane's clearance is
                // vanishing underneath it at the same rate, so it rides up with it and fades. All of
                // it resolves in the draw pass, so a scrolling bio never recomposes the avatar.
                .graphicsLayer {
                    val collapsed = collapse.identityFraction
                    val shrink = 1f - 0.3f * collapsed
                    translationY = -collapse.avatarClearancePx * collapsed
                    scaleX = shrink
                    scaleY = shrink
                    // Gone partway through the stage, so it has vacated the slot before the name lands.
                    alpha = (1f - collapsed / IdentityFadeOutPoint).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0f, 1f)
                }
                .then(if (avatarHidden) Modifier.clearAndSetSemantics {} else Modifier),
            contentAlignment = Alignment.BottomStart
        ) {
            UserAvatar(
                url = profile.avatarUrl,
                contentDescription = stringResource(R.string.content_description_profile_avatar),
                size = ProfileAvatarSize,
                borderWidth = 2.dp,
                framePadding = 3.dp,
                isProfileHeader = true
            )
        }

        // The name's destination: it leaves the pane in stage 2 and lands here, in the slot the
        // avatar just vacated, riding the banner's bottom edge down as the banner itself shrinks.
        // Drawn as an overlay for the same reason the avatar is — the pane's Surface would clip it.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                // Width is settled by layout and only the moving y goes through offset: the name has
                // to ellipsise against the banner's end inset, which a content-sized box drawn at an
                // x offset would not do — a long name would run past the edge instead.
                .fillMaxWidth()
                .offset { IntOffset(0, collapse.bannerNameTopPx.roundToInt()) }
                .padding(start = WideAvatarStartInset, end = BannerNameEndInset)
                // Picks up exactly where the in-pane copy finishes fading, so the two are never both
                // visible and the name never blinks out between them.
                .graphicsLayer {
                    alpha = ((collapse.nameFraction - NameHandoffPoint) / (1f - NameHandoffPoint))
                        .coerceIn(0f, 1f)
                }
                .onSizeChanged { collapse.bannerNameHeightPx = it.height }
                .then(if (nameOnBanner) Modifier else Modifier.clearAndSetSemantics {})
        ) {
            BannerOverlayName(name = profile.name)
        }
    }
}

/**
 * The display name once it has moved onto the banner. Styled separately from [ProfileDisplayName]
 * rather than parameterised, because sitting on a photo changes every choice: white with a shadow so
 * it survives a light banner, and a single ellipsised line so a long name cannot run under the
 * share/settings button in the opposite corner.
 */
@Composable
private fun BannerOverlayName(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.displaySmall.copy(
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
 * The resizable two-pane body — a near-clone of the calendar's `CalendarMonthTwoPane`: rounded cards
 * on a tinted gutter, a drag handle that resizes (snap on release / tap to cycle), the split
 * persisted to [com.anisync.android.data.AppSettings.paneProfileFraction].
 */
@Composable
private fun ProfileTwoPane(
    profile: UserProfile,
    uiState: ProfileUiState,
    isOwnProfile: Boolean,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    onAction: (ProfileAction) -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationCount: Int,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onVoiceActorClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit,
    onCommentClick: (threadId: Int, commentId: Int, threadTitle: String) -> Unit,
    onActivityClick: (Int) -> Unit,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    showAccountSwitcher: Boolean,
    onAccountSwitchClick: () -> Unit,
    statsColumns: Int,
    collapse: BioCollapseState,
    modifier: Modifier = Modifier
) {
    val appSettings = LocalAppSettings.current
    var leadingFraction by rememberSaveable { mutableFloatStateOf(appSettings.paneProfileFraction.value) }
    var rowWidthPx by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    fun settleTo(target: Float) {
        appSettings.setPaneProfileFraction(target)
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(initialValue = leadingFraction, targetValue = target) { value, _ -> leadingFraction = value }
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
        gutterPadding = PaddingValues(start = WideGutterStart, top = 12.dp, end = 16.dp, bottom = 16.dp),
        handle = {
            PaneDragHandle(
                modifier = Modifier.fillMaxHeight(),
                onDelta = { delta ->
                    if (rowWidthPx > 0) {
                        leadingFraction = (leadingFraction + delta / rowWidthPx)
                            .coerceIn(PROFILE_MIN_FRACTION, PROFILE_MAX_FRACTION)
                    }
                },
                onDragStarted = { settleJob?.cancel() },
                onDragStopped = { settleTo(PROFILE_FRACTION_ANCHORS.minBy { abs(it - leadingFraction) }) },
                onClick = { settleTo(nextProfileAnchor(leadingFraction)) },
                clickLabel = cycleLabel,
                resizeLabel = resizeLabel
            )
        },
        leading = {
            ProfileIdentityPane(
                profile = profile,
                isOwnProfile = isOwnProfile,
                isFollowing = uiState.isFollowingUser,
                isFollowerOfViewer = uiState.isFollowerOfViewer,
                isFollowLoading = uiState.isFollowLoading,
                onFollowClick = { onAction(ProfileAction.ToggleFollow) },
                onMessageClick = { onAction(ProfileAction.ShowMessageComposer) },
                onEditProfileClick = { onAction(ProfileAction.SetEditProfileDialogVisible(true)) },
                onNotificationsClick = onNotificationsClick,
                unreadNotificationCount = unreadNotificationCount,
                showAccountSwitcher = showAccountSwitcher,
                onAccountSwitchClick = onAccountSwitchClick,
                collapse = collapse
            )
        },
        trailing = {
            ProfileTabPane(
                profile = profile,
                uiState = uiState,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onAction = onAction,
                onMediaClick = onMediaClick,
                onCharacterClick = onCharacterClick,
                onStaffClick = onStaffClick,
                onVoiceActorClick = onVoiceActorClick,
                onStudioClick = onStudioClick,
                onUserClick = onUserClick,
                onThreadClick = onThreadClick,
                onCommentClick = onCommentClick,
                onActivityClick = onActivityClick,
                onLastReplyClick = onLastReplyClick,
                statsColumns = statsColumns
            )
        }
    )
}

/**
 * Left pane: the identity header (name/badges/meta + action buttons; the avatar is drawn as an
 * overlay by [ProfileWideLayout] so it can overlap the banner) over the inline biography.
 *
 * The header is only half fixed. Scrolling the bio first collapses the avatar clearance and the
 * badge/meta block into nothing — see [BioCollapseState] — which roughly triples the bio's viewport
 * on a short tablet. The display name and the action buttons never move.
 */
@Composable
private fun ProfileIdentityPane(
    profile: UserProfile,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isFollowerOfViewer: Boolean,
    isFollowLoading: Boolean,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationCount: Int,
    showAccountSwitcher: Boolean,
    onAccountSwitchClick: () -> Unit,
    collapse: BioCollapseState,
    modifier: Modifier = Modifier
) {
    val bioScrollState = rememberScrollState()
    val detailsHidden by remember(collapse) {
        derivedStateOf { collapse.identityFraction >= IdentityFadeOutPoint }
    }
    val nameOnBanner by remember(collapse) {
        derivedStateOf { collapse.nameFraction >= NameHandoffPoint }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Clears the lower half of the overlay avatar that straddles the banner/pane seam. Collapses
        // first, in step with the avatar fading out above it.
        Spacer(
            modifier = Modifier
                .collapseVertically { collapse.identityFraction }
                .height(ProfileAvatarHalfSize)
        )

        // Stage 2: the name folds up behind the pane's top edge while its banner counterpart fades
        // in, so it reads as the name flying up into the slot the avatar left.
        Box(
            modifier = Modifier
                .collapseVertically { collapse.nameFraction }
                .graphicsLayer {
                    alpha = (1f - collapse.nameFraction / NameHandoffPoint).coerceIn(0f, 1f)
                }
                .then(if (nameOnBanner) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            ProfileDisplayName(
                name = profile.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = IdentityPanePadding)
                    .onSizeChanged { collapse.nameHeightPx = it.height }
            )
        }

        Box(
            modifier = Modifier
                .collapseVertically { collapse.identityFraction }
                // Faded out ahead of the height reaching zero so the text is gone before it would be
                // sliced by the clip. Draw-phase read, so scrolling costs no recomposition.
                .graphicsLayer {
                    alpha = (1f - collapse.identityFraction / IdentityFadeOutPoint).coerceIn(0f, 1f)
                }
                .then(if (detailsHidden) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            ProfileIdentityDetails(
                profile = profile,
                isOwnProfile = isOwnProfile,
                viewerFollows = isFollowing,
                followsViewer = isFollowerOfViewer,
                // Measured inside the collapsing wrapper, where the block still lays out at its
                // natural height — the wrapper only changes what it reports to the Column.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = IdentityPanePadding)
                    .onSizeChanged { collapse.detailsHeightPx = it.height }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileActionButtons(
            isOwnProfile = isOwnProfile,
            isFollowing = isFollowing,
            isFollowLoading = isFollowLoading,
            onFollowClick = onFollowClick,
            onMessageClick = onMessageClick,
            onEditProfileClick = onEditProfileClick,
            onNotificationsClick = onNotificationsClick,
            unreadNotificationCount = unreadNotificationCount,
            showAccountSwitcher = showAccountSwitcher,
            onAccountSwitchClick = onAccountSwitchClick,
            modifier = Modifier.padding(horizontal = IdentityPanePadding)
        )

        val about = profile.about
        if (!about.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.nestedScroll(rememberBioCollapseConnection(collapse, bioScrollState))) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(bioScrollState)
                        .padding(horizontal = IdentityPanePadding)
                ) {
                    AsyncRichTextRenderer(
                        html = about,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            BioCollapseSettler(collapse = collapse, bioScrollState = bioScrollState)
        }
    }
}

/**
 * Nested-scroll wiring for the collapsing identity header: upward scroll folds the header away
 * before the bio moves at all, downward scroll re-opens it once the bio is back at its own top.
 * Returning zero at either end is what hands the gesture over to the bio.
 */
@Composable
private fun rememberBioCollapseConnection(
    collapse: BioCollapseState,
    bioScrollState: ScrollState
): NestedScrollConnection = remember(collapse, bioScrollState) {
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            // Let the bio scroll back to its own top before the header starts re-expanding,
            // otherwise the header pops open over content the reader is still in the middle of.
            if (delta > 0f && bioScrollState.canScrollBackward) return Offset.Zero
            val consumed = collapse.consume(delta, bioScrollState.maxValue > 0)
            return if (consumed == 0f) Offset.Zero else Offset(0f, consumed)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // Re-expand from whatever downward scroll is left once the bio has hit its top, which is
            // what carries a fling through into the header.
            if (available.y <= 0f) return Offset.Zero
            val absorbed = collapse.consume(available.y, bioScrollState.maxValue > 0)
            return if (absorbed == 0f) Offset.Zero else Offset(0f, absorbed)
        }
    }
}

/**
 * Settles the header onto the nearest stage boundary once the bio stops moving, the same
 * nearest-anchor rule the pane splitter uses. Resting mid-stage would strand the name half faded
 * between the pane and the banner, so the only valid rest positions are the stage edges.
 *
 * `collectLatest` cancels an in-flight settle the moment the reader grabs the bio again.
 */
@Composable
private fun BioCollapseSettler(collapse: BioCollapseState, bioScrollState: ScrollState) {
    LaunchedEffect(collapse, bioScrollState) {
        snapshotFlow { bioScrollState.isScrollInProgress }.collectLatest { scrolling ->
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

/**
 * Right pane: the profile tab group on top (controls the pane), then the selected tab's content in
 * its own pull-to-refreshable [LazyColumn] — reusing [profileSelectedTabContent], the same per-tab
 * sections the compact profile uses.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfileTabPane(
    profile: UserProfile,
    uiState: ProfileUiState,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    onAction: (ProfileAction) -> Unit,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onVoiceActorClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit,
    onCommentClick: (threadId: Int, commentId: Int, threadTitle: String) -> Unit,
    onActivityClick: (Int) -> Unit,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    statsColumns: Int,
    modifier: Modifier = Modifier
) {
    // Column counts come from the pane, not the window: this pane is a fraction of the window wide,
    // and a window-derived count budgets for space it does not have, squeezing every cell.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val portraitColumns = profileGridColumns(baseMinSize = 150.dp, availableWidth = maxWidth)
        val studioColumns = profileGridColumns(
            baseMinSize = 240.dp,
            compactColumns = 2,
            availableWidth = maxWidth
        )

        Column(modifier = Modifier.fillMaxSize()) {
            ProfileTabsButtonGroup(
                selectedTab = uiState.selectedTab,
                onTabSelected = { onAction(ProfileAction.SelectTab(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = rememberRateLimitedRefresh { onAction(ProfileAction.Refresh()) },
                state = pullState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    CustomPullToRefreshIndicator(
                        isRefreshing = uiState.isRefreshing,
                        state = pullState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 48.dp + LocalMainNavBarInset.current)
                ) {
                    profileSelectedTabContent(
                        profile = profile,
                        uiState = uiState,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onAction = onAction,
                        onMediaClick = onMediaClick,
                        onCharacterClick = onCharacterClick,
                        onStaffClick = onStaffClick,
                        onVoiceActorClick = onVoiceActorClick,
                        onStudioClick = onStudioClick,
                        onUserClick = onUserClick,
                        onThreadClick = onThreadClick,
                        onCommentClick = onCommentClick,
                        onActivityClick = onActivityClick,
                        onLastReplyClick = onLastReplyClick,
                        portraitColumns = portraitColumns,
                        studioColumns = studioColumns,
                        statsColumns = statsColumns
                    )
                }
            }
        }
    }
}
