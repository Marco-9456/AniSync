package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.anisync.android.presentation.profile.components.ProfileIdentityInfo
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.PaneDragHandle
import com.anisync.android.presentation.util.TwoPaneDefaults
import com.anisync.android.presentation.util.TwoPaneRow
import com.anisync.android.util.ShareUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

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

private val PROFILE_FRACTION_ANCHORS = listOf(0.26f, 0.32f, 0.40f)
private const val PROFILE_MIN_FRACTION = 0.24f
private const val PROFILE_MAX_FRACTION = 0.46f

private fun nextProfileAnchor(fraction: Float): Float =
    PROFILE_FRACTION_ANCHORS.firstOrNull { it > fraction + 0.01f } ?: PROFILE_FRACTION_ANCHORS.first()

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
    portraitColumns: Int,
    studioColumns: Int,
    statsColumns: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                modifier = Modifier.padding(
                    start = WideGutterStart,
                    top = WideBannerTopMargin,
                    end = 16.dp
                ),
                shape = TwoPaneDefaults.PaneShape
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
                portraitColumns = portraitColumns,
                studioColumns = studioColumns,
                statsColumns = statsColumns,
                modifier = Modifier.weight(1f)
            )
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
                .height(ProfileAvatarSize),
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
    }
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
    portraitColumns: Int,
    studioColumns: Int,
    statsColumns: Int,
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
                onAccountSwitchClick = onAccountSwitchClick
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
                portraitColumns = portraitColumns,
                studioColumns = studioColumns,
                statsColumns = statsColumns
            )
        }
    )
}

/**
 * Left pane: a fixed identity header (name/badges/meta + action buttons; the avatar is drawn as an
 * overlay by [ProfileWideLayout] so it can overlap the banner) over the inline biography, which
 * scrolls on its own when long. The leading [Spacer] reserves room for the overlay avatar.
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Clears the lower half of the overlay avatar that straddles the banner/pane seam.
        Spacer(modifier = Modifier.height(ProfileAvatarHalfSize))

        ProfileIdentityInfo(
            profile = profile,
            isOwnProfile = isOwnProfile,
            viewerFollows = isFollowing,
            followsViewer = isFollowerOfViewer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = IdentityPanePadding)
        )

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = IdentityPanePadding)
            ) {
                AsyncRichTextRenderer(
                    html = about,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
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
    portraitColumns: Int,
    studioColumns: Int,
    statsColumns: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
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
