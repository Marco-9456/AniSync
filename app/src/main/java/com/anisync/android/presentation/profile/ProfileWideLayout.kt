package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import com.anisync.android.presentation.profile.lists.ProfileListSheet
import com.anisync.android.presentation.profile.lists.ProfileListFilterPanel
import com.anisync.android.presentation.profile.components.ProfileRails
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.width
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.navigation.BackTrailButton
import com.anisync.android.presentation.profile.components.ProfileAccounts
import com.anisync.android.presentation.profile.components.AvatarPlaceholder
import com.anisync.android.presentation.profile.components.IdentityPlaceholder
import com.anisync.android.presentation.profile.components.ProfileHeaderAvatar
import com.anisync.android.presentation.profile.components.bandChromeColors
import com.anisync.android.presentation.profile.components.ProfileActionButtons
import com.anisync.android.presentation.profile.components.ProfileAvatarHalfSize
import com.anisync.android.presentation.profile.components.ProfileBannerSurface
import com.anisync.android.presentation.profile.components.ProfileCardSettingsButton
import com.anisync.android.presentation.profile.components.bannerChromeColors
import com.anisync.android.presentation.profile.components.ProfileIdentityInfo
import com.anisync.android.presentation.profile.components.ProfileMenuButton
import com.anisync.android.presentation.profile.components.ProfileSettingsButton
import com.anisync.android.presentation.profile.sections.ProfileAboutExtras
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.PaneDragHandle
import com.anisync.android.presentation.util.TwoPaneDefaults
import com.anisync.android.presentation.util.TwoPaneRow
import com.anisync.android.presentation.util.profileGridColumns
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private val WideBannerHeight = 168.dp

/** T4's card is 140dp on a 312dp column, and T5's short window takes 120dp. */
private const val WideBannerAspect = 0.43f
private val WideBannerMinHeight = 120.dp

/** The least room between the avatar and the actions for the two to share a row. */
private val WideActionsGap = 12.dp

/** T4's identity pane, the narrowest that holds the banner card, the avatar and the actions. */
private val IdentityPaneMinWidth = 344.dp

/** Without a banner the column keeps only the controls that sat on it, in a row of this height. */
private val WideChromeRowHeight = 48.dp

private val WideBannerInset = 8.dp
private val WideContentInset = 4.dp
private val WideGutterStart = 16.dp

/** The tabs sit this far into their strip, and the banner card's top lines up with them. */
private val TabsTopInset = 6.dp

/** T2's filter panel, and the pane width from which it sits beside the rows. */
private val ListFilterPanelWidth = 336.dp
private val ListFilterPanelMinWidth = 720.dp

/** T1 lays the Overview out in two 388dp columns, which needs about this much pane. */
private val OverviewColumnsMinWidth = 720.dp

private val PROFILE_FRACTION_ANCHORS = listOf(0.26f, 0.32f, 0.40f)
private const val PROFILE_MIN_FRACTION = 0.24f
private const val PROFILE_MAX_FRACTION = 0.46f

private fun nextProfileAnchor(fraction: Float): Float =
    PROFILE_FRACTION_ANCHORS.firstOrNull { it > fraction + 0.01f } ?: PROFILE_FRACTION_ANCHORS.first()

/**
 * The profile on a wide window: the identity column on the left, which scrolls on its own, and the
 * tabs with the open tab's content on the right. The two are resizable, the split kept in settings.
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
    accounts: ProfileAccounts?,
    onShareStats: () -> Unit,
    onShareFavourites: () -> Unit,
    statsColumns: Int,
    onOpenRecap: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val appSettings = LocalAppSettings.current
    var leadingFraction by rememberSaveable { mutableFloatStateOf(appSettings.paneProfileFraction.value) }
    val density = LocalDensity.current
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

    ProfileTwoPaneRow(
        leadingWeight = leadingFraction,
        modifier = modifier.onSizeChanged { rowWidthPx = it.width },
        handle = {
            PaneDragHandle(
                modifier = Modifier.fillMaxHeight(),
                onDelta = { delta ->
                    if (rowWidthPx > 0) {
                        val narrowest = identityMinFraction(with(density) { rowWidthPx.toDp() })
                        leadingFraction = (leadingFraction + delta / rowWidthPx)
                            .coerceIn(maxOf(PROFILE_MIN_FRACTION, narrowest), PROFILE_MAX_FRACTION)
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
            ProfileIdentityColumn(
                profile = profile,
                uiState = uiState,
                isOwnProfile = isOwnProfile,
                onAction = onAction,
                onNotificationsClick = onNotificationsClick,
                unreadNotificationCount = unreadNotificationCount,
                accounts = accounts,
                onSettingsClick = onSettingsClick,
                onBackClick = onBackClick
            )
        },
        trailing = {
            ProfileTabPane(
                profile = profile,
                uiState = uiState,
                isOwnProfile = isOwnProfile,
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
                onShareStats = onShareStats,
                onShareFavourites = onShareFavourites,
                statsColumns = statsColumns,
                onOpenRecap = onOpenRecap
            )
        }
    )
}

/** The profile's panes sit flat on the page background, the identity column and the tabs without cards around them. */
@Composable
private fun ProfileTwoPaneRow(
    leadingWeight: Float,
    modifier: Modifier = Modifier,
    handle: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        TwoPaneRow(
            leadingWeight = leadingWeight.coerceAtLeast(identityMinFraction(maxWidth)),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            gutterColor = MaterialTheme.colorScheme.background,
            leadingColor = MaterialTheme.colorScheme.background,
            trailingColor = MaterialTheme.colorScheme.background,
            gutterPadding = PaddingValues(start = WideGutterStart, top = 12.dp, end = 16.dp, bottom = 16.dp),
            handle = handle,
            leading = leading,
            trailing = trailing
        )
    }
}

/** On an upright tablet or a phone on its side the saved split would squeeze the identity column. */
private fun identityMinFraction(rowWidth: Dp): Float =
    (IdentityPaneMinWidth / (rowWidth - WideGutterStart - 32.dp)).coerceIn(0f, PROFILE_MAX_FRACTION)

/**
 * T1's identity column: the banner card, the avatar on its lower edge with the actions beside it,
 * then the name, the details and the whole bio. Your own profile without a banner (T3) has no
 * band, only Settings above the avatar.
 */
@Composable
private fun ProfileIdentityColumn(
    profile: UserProfile,
    uiState: ProfileUiState,
    isOwnProfile: Boolean,
    onAction: (ProfileAction) -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationCount: Int,
    accounts: ProfileAccounts?,
    onSettingsClick: () -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val hasBanner = profile.bannerUrl != null
    val compactHeight = LocalAdaptiveInfo.current.isCompactHeight
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // T1's card shrinks with a narrower column as T4's does, and a short window takes T5's.
        val bannerHeight = if (compactHeight) {
            WideBannerMinHeight
        } else {
            ((maxWidth - WideBannerInset * 2) * WideBannerAspect).coerceIn(WideBannerMinHeight, WideBannerHeight)
        }
        Layout(
            contents = listOf(
                {
                    if (hasBanner) {
                        ProfileBannerSurface(
                            profile = profile,
                            height = bannerHeight,
                            onBackClick = onBackClick,
                            shape = TwoPaneDefaults.PaneShape,
                            statusScrim = false,
                            endControl = {
                                if (isOwnProfile) {
                                    ProfileCardSettingsButton(onClick = onSettingsClick)
                                } else {
                                    ProfileMenuButton(name = profile.name, colors = bannerChromeColors())
                                }
                            },
                            modifier = Modifier.padding(horizontal = WideBannerInset)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(WideChromeRowHeight)
                        ) {
                            if (!isOwnProfile && onBackClick != null) BackTrailButton(onClick = onBackClick)
                            Spacer(modifier = Modifier.weight(1f))
                            if (isOwnProfile) {
                                ProfileSettingsButton(onBanner = false, onClick = onSettingsClick)
                            } else {
                                ProfileMenuButton(
                                    name = profile.name,
                                    colors = bandChromeColors()
                                )
                            }
                        }
                    }
                },
                { ProfileHeaderAvatar(url = profile.avatarUrl) },
                {
                    ProfileActionButtons(
                        isOwnProfile = isOwnProfile,
                        isFollowing = uiState.isFollowingUser,
                        isFollowLoading = uiState.isFollowLoading,
                        onFollowClick = { onAction(ProfileAction.ToggleFollow) },
                        onMessageClick = { onAction(ProfileAction.ShowMessageComposer) },
                        onEditProfileClick = { onAction(ProfileAction.SetEditProfileDialogVisible(true)) },
                        onNotificationsClick = onNotificationsClick,
                        unreadNotificationCount = unreadNotificationCount
                    )
                },
                {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WideContentInset)
                            .padding(bottom = 24.dp)
                    ) {
                        ProfileIdentityInfo(
                            profile = profile,
                            isOwnProfile = isOwnProfile,
                            viewerFollows = uiState.isFollowingUser,
                            followsViewer = uiState.isFollowerOfViewer,
                            followingTotal = uiState.followingTotal,
                            followersTotal = uiState.followersTotal,
                            onPeopleClick = { onAction(ProfileAction.SelectTab(ProfileTab.SOCIAL)) },
                            accounts = accounts,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // The column scrolls on its own, so the bio is there in full rather than cut
                        // short, with what the phone's About sheet keeps under it.
                        val about = profile.about
                        if (!about.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AsyncRichTextRenderer(html = about, modifier = Modifier.fillMaxWidth())
                        }
                        ProfileAboutExtras(profile = profile, horizontalPadding = 0.dp)
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = TabsTopInset)
        ) { (top, avatar, actions, body), constraints ->
            val width = constraints.maxWidth
            val loose = Constraints(maxWidth = width)
            val topPlaceable = top.first().measure(Constraints.fixedWidth(width))
            val avatarPlaceable = avatar.first().measure(loose)
            val actionsPlaceable = actions.first().measure(loose)
            val bodyPlaceable = body.first().measure(Constraints.fixedWidth(width))

            val header = topPlaceable.height
            val avatarStart = (WideBannerInset + 16.dp).roundToPx()
            val actionsEnd = WideContentInset.roundToPx()
            val avatarTop = if (hasBanner) header - ProfileAvatarHalfSize.roundToPx() else header + 8.dp.roundToPx()
            val avatarBottom = avatarTop + avatarPlaceable.height
            // T1 sets the actions beside the avatar, T4 and T5 under it once the column is too narrow for both.
            val beside = avatarStart + avatarPlaceable.width + WideActionsGap.roundToPx() + actionsPlaceable.width + actionsEnd <= width
            val actionsTop = when {
                !beside -> avatarBottom + 12.dp.roundToPx()
                hasBanner -> header + 8.dp.roundToPx()
                else -> avatarTop + 56.dp.roundToPx()
            }
            val identityTop = when {
                !beside -> actionsTop + actionsPlaceable.height + 12.dp.roundToPx()
                hasBanner -> actionsTop + 52.dp.roundToPx()
                else -> avatarBottom + 10.dp.roundToPx()
            }
            layout(width, identityTop + bodyPlaceable.height) {
                topPlaceable.place(0, 0)
                bodyPlaceable.place(0, identityTop)
                avatarPlaceable.place(avatarStart, avatarTop)
                actionsPlaceable.place(width - actionsEnd - actionsPlaceable.width, actionsTop)
            }
        }
    }
}

/**
 * T2: Lists on a wide pane. The tabs span the pane, and under them the rows keep the search and the
 * status chips while the filters sit in a panel beside them. The search bar's filter button folds
 * the panel away and back.
 */
@Composable
private fun ProfileListsWithPanel(
    uiState: ProfileUiState,
    isOwnProfile: Boolean,
    listState: LazyListState,
    bottomPadding: Dp,
    onAction: (ProfileAction) -> Unit,
    rowsContent: LazyListScope.(gridColumns: Int) -> Unit
) {
    var panelShown by rememberSaveable { mutableStateOf(true) }
    val rails = remember { ProfileRails() }
    val onTabSelected: (ProfileTab) -> Unit = { onAction(ProfileAction.SelectTab(it)) }
    Column(modifier = Modifier.fillMaxSize()) {
        ProfileTabStrip(
            uiState = uiState,
            onTabSelected = onTabSelected,
            onStatsTypeSelected = { onAction(ProfileAction.SelectStatsType(it)) },
            onStatsViewSelected = { onAction(ProfileAction.SelectStatsView(it)) },
            onFilterAction = onAction,
            showListSearch = false,
            showListRail = false,
            rails = rails,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val gridColumns = profileGridColumns(baseMinSize = 240.dp, compactColumns = 2, availableWidth = maxWidth)
                Column(modifier = Modifier.fillMaxSize()) {
                    ProfileListSearchBar(
                        uiState = uiState,
                        onQueryChange = { onAction(ProfileAction.SetListQuery(it)) },
                        onFilterAction = { action ->
                            val opensFilters = action is ProfileAction.OpenListSheet && action.sheet == ProfileListSheet.FILTERS
                            if (opensFilters) panelShown = !panelShown else onAction(action)
                        },
                        onBack = null
                    )
                    ProfileListRail(uiState = uiState, onFilterAction = onAction, onTabSelected = onTabSelected, rails = rails)
                    ProfileRefreshList(uiState, listState, bottomPadding, onAction) { rowsContent(gridColumns) }
                }
            }
            if (panelShown) {
                ProfileListFilterPanel(
                    entries = uiState.selectedListEntries,
                    tools = uiState.listTools,
                    results = uiState.listResults,
                    isOwnProfile = isOwnProfile,
                    onFiltersChange = { onAction(ProfileAction.SetListFilters(it)) },
                    modifier = Modifier
                        .width(ListFilterPanelWidth)
                        .fillMaxHeight()
                        .padding(end = 16.dp, top = 4.dp, bottom = 16.dp)
                )
            }
        }
    }
}

/** The first load on a wide window, in the same places as the identity column and the tab pane. */
@Composable
internal fun ProfileWideLoadingSkeleton(name: String?, onBackClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    val quiet = MaterialTheme.colorScheme.surfaceContainerLow
    val leadingFraction by LocalAppSettings.current.paneProfileFraction.collectAsStateWithLifecycle()
    ProfileTwoPaneRow(
        leadingWeight = leadingFraction,
        modifier = modifier,
        leading = {
            Box(modifier = Modifier.fillMaxSize().padding(top = TabsTopInset)) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = WideBannerInset)
                        .fillMaxWidth()
                        .height(WideBannerHeight)
                        .clip(TwoPaneDefaults.PaneShape)
                        .background(block)
                )
                IdentityPlaceholder(
                    name = name,
                    lineFractions = listOf(0.6f, 0.85f, 0.7f),
                    modifier = Modifier
                        .padding(top = WideBannerHeight + 60.dp)
                        .padding(horizontal = WideContentInset)
                )
                AvatarPlaceholder(
                    modifier = Modifier
                        .padding(start = WideBannerInset + 16.dp)
                        .offset(y = WideBannerHeight - ProfileAvatarHalfSize)
                )
                if (onBackClick != null) {
                    BackTrailButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = WideBannerInset + 12.dp, top = 12.dp)
                    )
                }
            }
        },
        trailing = {
            Column(modifier = Modifier.fillMaxSize()) {
                ProfileTabsButtonGroup(selectedTab = ProfileTab.OVERVIEW, lastListTab = ProfileTab.ANIME, onTabSelected = {})
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(240.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(quiet)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(320.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(quiet)
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    AppCircularProgressIndicator()
                }
            }
        }
    )
}

/**
 * Right pane: the profile tab group on top (controls the pane), then the selected tab's content in
 * its own pull-to-refreshable [LazyColumn], reusing [profileSelectedTabContent], the same per-tab
 * sections the compact profile uses. Pages open on the app's collapsing bar instead.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfileTabPane(
    profile: UserProfile,
    uiState: ProfileUiState,
    isOwnProfile: Boolean,
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
    onShareStats: () -> Unit,
    onShareFavourites: () -> Unit,
    statsColumns: Int,
    onOpenRecap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Column counts come from the pane, not the window: this pane is a fraction of the window wide,
    // and a window-derived count budgets for space it does not have, squeezing every cell.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // The tabs never scroll here, so every tab and chip keeps its own place.
        val listStates = rememberProfileListStates()
        val listState = listStates.of(uiState.contentKey)
        ProfileListPlacement(uiState, listState, keepsPlace = { true })
        val portraitColumns = profileGridColumns(baseMinSize = 150.dp, availableWidth = maxWidth)
        val studioColumns = profileGridColumns(
            baseMinSize = 240.dp,
            compactColumns = 2,
            availableWidth = maxWidth
        )

        val showShareFab = uiState.showsShareFab(profile)
        val overviewColumns = maxWidth >= OverviewColumnsMinWidth
        val bottomPadding = profileBottomPadding(showShareFab)
        val overviewBlocks = rememberOverviewBlocks(
            profile, uiState, onAction, onMediaClick, onCharacterClick, onStaffClick, onUserClick, onActivityClick, onLastReplyClick
        )
        fun LazyListScope.tabContent(
            gridColumns: Int = studioColumns,
            rowColumns: Int = statsColumns,
            twoColumnOverview: Boolean = overviewColumns
        ) = profileSelectedTabContent(
            profile = profile,
            uiState = uiState,
            isOwnProfile = isOwnProfile,
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
            studioColumns = gridColumns,
            statsColumns = rowColumns,
            onOpenRecap = onOpenRecap,
            overviewBlocks = overviewBlocks,
            overviewColumns = twoColumnOverview
        )
        val content: LazyListScope.() -> Unit = { tabContent() }
        if (uiState.selectedTab.isPage) {
            ProfilePage(
                uiState = uiState,
                listState = listState,
                bottomPadding = bottomPadding,
                onAction = onAction,
                topInset = 0.dp,
                content = content
            )
        } else if (uiState.selectedTab.isList && maxWidth >= ListFilterPanelMinWidth) {
            ProfileListsWithPanel(
                uiState = uiState,
                isOwnProfile = isOwnProfile,
                listState = listState,
                bottomPadding = bottomPadding,
                onAction = onAction,
                // One row of titles beside the panel, as in T2.
                rowsContent = { gridColumns -> tabContent(gridColumns, rowColumns = 1, twoColumnOverview = false) }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ProfileTabStrip(
                    uiState = uiState,
                    onTabSelected = { onAction(ProfileAction.SelectTab(it)) },
                    onStatsTypeSelected = { onAction(ProfileAction.SelectStatsType(it)) },
                    onStatsViewSelected = { onAction(ProfileAction.SelectStatsView(it)) },
                    onFilterAction = onAction,
                    modifier = Modifier.fillMaxWidth()
                )
                ProfileRefreshList(uiState, listState, bottomPadding, onAction, content = content)
            }
        }
        // Kept out of the pull box, which carries everything inside it down with the finger.
        if (showShareFab) ProfileShareFab(uiState, onShareStats = onShareStats, onShareFavourites = onShareFavourites)
    }
}
