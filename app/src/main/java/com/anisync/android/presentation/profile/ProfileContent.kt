package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.presentation.components.ShareFab
import com.anisync.android.presentation.components.TabRequestEffect
import com.anisync.android.presentation.components.shareFabBottomInset
import com.anisync.android.presentation.components.shareFabClearance
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.LocalWhatsNew
import com.anisync.android.presentation.components.WhatsNewDot
import com.anisync.android.presentation.components.WhatsNewPill
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.components.rememberActiveWhatsNew
import com.anisync.android.presentation.details.components.ReviewDetailsSheet
import com.anisync.android.presentation.profile.components.DirectMessageInputSheet
import com.anisync.android.presentation.profile.components.ProfileBioSheet
import com.anisync.android.presentation.profile.components.ProfileTopSection
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.dashboardColumns
import com.anisync.android.presentation.util.profileGridColumns
import com.anisync.android.presentation.profile.sections.ProfileOverviewSection
import com.anisync.android.presentation.profile.sections.ActivityFilterRail
import com.anisync.android.presentation.profile.sections.FavoritesFilterRail
import com.anisync.android.presentation.profile.sections.MediaStatusRail
import com.anisync.android.presentation.profile.sections.SocialTabRail
import com.anisync.android.presentation.profile.sections.profileActivityTab
import com.anisync.android.presentation.profile.sections.profileFavoritesTab
import com.anisync.android.presentation.profile.sections.profileMediaTab
import com.anisync.android.presentation.profile.sections.profileReviewsTab
import com.anisync.android.presentation.profile.sections.profileSocialTab
import com.anisync.android.presentation.profile.sections.profileStatsTab
import com.anisync.android.presentation.statistics.StatsRail
import com.anisync.android.presentation.navigation.ShareStudio
import com.anisync.android.presentation.share.studio.ShareStudioSubject
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.anisync.android.util.ShareUtils
import androidx.compose.foundation.lazy.rememberLazyListState
import com.anisync.android.presentation.components.ScrollToTopOnRequest

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun ProfileContent(
    profile: UserProfile,
    uiState: ProfileUiState,
    /** See ProfileViewModel.scrollToTopRequest: the tab was reselected. */
    scrollToTopRequest: Long = 0L,
    /** See ProfileViewModel.dockRequest: a recap opened Statistics. */
    dockRequest: Long = 0L,
    isOwnProfile: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onAction: (ProfileAction) -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    onMediaClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onVoiceActorClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit = { _, _ -> },
    onCommentClick: (threadId: Int, commentId: Int, threadTitle: String) -> Unit = { _, _, _ -> },
    onActivityClick: (Int) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    showAccountSwitcher: Boolean = false,
    onAccountSwitchClick: () -> Unit = {},
    onShareCard: (ShareStudio) -> Unit = {},
    onOpenRecap: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()
    val statsColumns = dashboardColumns()

    // Someone else's profile keeps its colours in the studio, the viewer's own follows the app.
    val shareProfileColor = if (isOwnProfile) null else profile.profileColor
    val shareStats = {
        onShareCard(
            ShareStudio(
                subject = ShareStudioSubject.STATS.name,
                id = profile.id,
                userName = profile.name,
                mediaType = if (uiState.selectedStatsType == ProfileStatsType.MANGA) MediaType.MANGA.rawValue else MediaType.ANIME.rawValue,
                profileColor = shareProfileColor,
            )
        )
    }
    val shareFavourites = {
        onShareCard(
            ShareStudio(
                subject = ShareStudioSubject.FAVOURITES.name,
                id = profile.id,
                userName = profile.name,
                mediaType = if (uiState.selectedFavoritesFilter == ProfileFavoritesFilter.MANGA) MediaType.MANGA.rawValue else MediaType.ANIME.rawValue,
                profileColor = shareProfileColor,
            )
        )
    }

    if (LocalAdaptiveInfo.current.supportsTwoPane) {
        ProfileWideLayout(
            profile = profile,
            uiState = uiState,
            isOwnProfile = isOwnProfile,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onAction = onAction,
            onSettingsClick = onSettingsClick,
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
            onShareStats = shareStats,
            onShareFavourites = shareFavourites,
            statsColumns = statsColumns,
            onOpenRecap = onOpenRecap,
            modifier = modifier
        )
    } else {
    // Portrait grids (favorite characters/staff, social following/followers) keep their 3-up phone
    // density and gain columns on wider windows; studio chips are wider so they start 2-up.
    val portraitColumns = profileGridColumns(baseMinSize = 150.dp)
    val studioColumns = profileGridColumns(baseMinSize = 240.dp, compactColumns = 2)

    // The banner runs under the status bar, so the tab strip can't be a stickyHeader: that docks at
    // the list's top edge, which is behind the clock. Instead the strip is a real in-list item and a
    // pinned copy takes over once it reaches the bar, the same swap the media-detail tabs use.
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val dockPx = with(LocalDensity.current) { statusBarInset.roundToPx() }.toFloat()
    var contentTopWindow by remember { mutableFloatStateOf(0f) }
    var inlineTabsTopWindow by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val listState = rememberLazyListState()
    ScrollToTopOnRequest(scrollToTopRequest, listState)
    // Coming back to the page below the tabs never lays the in-list strip out, so its top is never
    // measured and the pinned copy goes by the list's position instead.
    val tabsDocked by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1 || inlineTabsTopWindow <= contentTopWindow + dockPx
        }
    }

    // Picking a tab or a Statistics view docks the strip, so the page starts at the top of the
    // screen. Each view comes back where it was left.
    val scope = rememberCoroutineScope()
    val dockStrip: suspend () -> Unit = {
        listState.scrollToItem(PROFILE_TABS_INDEX)
        listState.scrollBy(-dockPx)
    }
    TabRequestEffect(dockRequest) {
        // The list ignores the scroll back by the status bar until it has been laid out once.
        snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > PROFILE_TABS_INDEX }
        dockStrip()
    }
    val statsScroll = remember { mutableMapOf<StatsView, Pair<Int, Int>>() }
    var restoreStatsView by remember { mutableStateOf<StatsView?>(null) }
    LaunchedEffect(uiState.selectedStatsView, restoreStatsView) {
        val view = restoreStatsView
        if (view == null || view != uiState.selectedStatsView) return@LaunchedEffect
        restoreStatsView = null
        val saved = statsScroll[view]
        if (saved != null) listState.scrollToItem(saved.first, saved.second) else dockStrip()
    }
    val onTabSelected: (ProfileTab) -> Unit = { tab ->
        onAction(ProfileAction.SelectTab(tab))
        scope.launch { dockStrip() }
    }
    val onStatsViewSelected: (StatsView) -> Unit = { view ->
        if (view != uiState.selectedStatsView) {
            if (tabsDocked) {
                statsScroll[uiState.selectedStatsView] =
                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            }
            restoreStatsView = view
            onAction(ProfileAction.SelectStatsView(view))
        }
    }
    val onFilterAction: (ProfileAction) -> Unit = { action ->
        onAction(action)
        scope.launch { dockStrip() }
    }
    val tabStrip: @Composable () -> Unit = {
        ProfileTabStrip(
            uiState = uiState,
            onTabSelected = onTabSelected,
            onStatsTypeSelected = { onAction(ProfileAction.SelectStatsType(it)) },
            onStatsViewSelected = onStatsViewSelected,
            onFilterAction = onFilterAction
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = rememberRateLimitedRefresh { onAction(ProfileAction.Refresh()) },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { contentTopWindow = it.boundsInWindow().top },
        indicator = {
            CustomPullToRefreshIndicator(
                isRefreshing = uiState.isRefreshing,
                state = pullToRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    ) {
    val showShareFab = uiState.showsShareFab(profile)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = if (showShareFab) shareFabClearance() else LocalMainNavBarInset.current + 48.dp
        )
    ) {
        item(key = "profile_header", contentType = "header") {
            ProfileTopSection(
                profile = profile,
                isOwnProfile = isOwnProfile,
                onSettingsClick = onSettingsClick,
                onEditProfileClick = {
                    onAction(ProfileAction.SetEditProfileDialogVisible(true))
                },
                onShowBiography = {
                    onAction(ProfileAction.SetBiographySheetVisible(true))
                },
                isFollowing = uiState.isFollowingUser,
                isFollowerOfViewer = uiState.isFollowerOfViewer,
                isFollowLoading = uiState.isFollowLoading,
                onFollowClick = { onAction(ProfileAction.ToggleFollow) },
                onMessageClick = { onAction(ProfileAction.ShowMessageComposer) },
                onNotificationsClick = onNotificationsClick,
                unreadNotificationCount = unreadNotificationCount,
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
                showAccountSwitcher = showAccountSwitcher,
                onAccountSwitchClick = onAccountSwitchClick
            )
        }

        item(key = "profile_tabs", contentType = "tabs") {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { inlineTabsTopWindow = it.boundsInWindow().top }
            ) {
                tabStrip()
            }
        }

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
            statsColumns = statsColumns,
            onOpenRecap = onOpenRecap,
            onStatsViewSelected = onStatsViewSelected
        )
    }

    if (tabsDocked) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                tabStrip()
            }
        }
    }

    if (showShareFab) ProfileShareFab(uiState, onShareStats = shareStats, onShareFavourites = shareFavourites)
    }
    }

    if (uiState.isBiographySheetVisible) {
        ProfileBioSheet(
            about = profile.about.orEmpty(),
            onDismissRequest = {
                onAction(ProfileAction.SetBiographySheetVisible(false))
            }
        )
    }

    uiState.selectedReview?.let { review ->
        ReviewDetailsSheet(
            review = review,
            onRateReview = { id, r -> onAction(ProfileAction.RateReview(id, r)) },
            onUserClick = onUserClick,
            onMediaClick = { mediaId ->
                // Close the sheet first so the back gesture returns to the profile, not a
                // lingering sheet, once the media-details screen is popped.
                onAction(ProfileAction.SelectReview(null))
                onMediaClick(mediaId)
            },
            onDismiss = { onAction(ProfileAction.SelectReview(null)) }
        )
    }

    if (uiState.isMessageComposerVisible) {
        DirectMessageInputSheet(
            recipientName = profile.name,
            isSending = uiState.isSendingMessage,
            errorMessage = uiState.messageSendError,
            onDismissRequest = { onAction(ProfileAction.HideMessageComposer) },
            onSend = { text, isPrivate ->
                onAction(ProfileAction.SendMessage(text, isPrivate))
            }
        )
    }

    val editing = uiState.editingActivity
    if (editing != null) {
        val isMessage = editing.type == com.anisync.android.domain.ActivityType.MESSAGE
        val bounds = if (isMessage) {
            com.anisync.android.domain.ContentLimits.MessageActivity
        } else {
            com.anisync.android.domain.ContentLimits.TextActivity
        }
        com.anisync.android.presentation.components.richtext.RichTextInputSheet(
            title = stringResource(R.string.activity_edit_status_title),
            placeholder = stringResource(R.string.feed_compose_placeholder),
            submitLabel = stringResource(R.string.activity_edit_save),
            isSubmitting = uiState.isSavingActivityEdit,
            prefillBody = editing.bodyMarkdown ?: editing.text,
            minLength = bounds.min,
            maxLength = bounds.max,
            onSubmit = { body -> onAction(ProfileAction.SubmitActivityEdit(body)) },
            onDismiss = { onAction(ProfileAction.DismissActivityEdit) }
        )
    }
}

/**
 * Emits the currently-selected profile tab's content into a [LazyColumn]. Shared by the compact
 * single-column profile and the expanded layout's right pane (`ProfileTabPane`) so the per-tab
 * sections are defined once. The adaptive column counts are computed by the caller and threaded in.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun LazyListScope.profileSelectedTabContent(
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
    onOpenRecap: (String) -> Unit = {},
    onStatsViewSelected: (StatsView) -> Unit = { onAction(ProfileAction.SelectStatsView(it)) }
) {
    when (uiState.selectedTab) {
        ProfileTab.OVERVIEW -> {
            item(key = "tab_overview", contentType = "overview") {
                ProfileOverviewSection(
                    profile = profile,
                    activityHistory = uiState.statsData?.activityHistory.orEmpty(),
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onNavigateToTab = { onAction(ProfileAction.SelectTab(it)) },
                    onMediaClick = onMediaClick,
                    onCharacterClick = onCharacterClick,
                    onStaffClick = onStaffClick,
                    onUserClick = onUserClick,
                    onActivityClick = onActivityClick,
                    onLastReplyClick = onLastReplyClick,
                    onSubscribeClick = { onAction(ProfileAction.ToggleActivitySubscription(it)) },
                    onLikeActivity = { onAction(ProfileAction.ToggleActivityLike(it)) },
                    onDeleteActivity = { onAction(ProfileAction.DeleteActivity(it)) },
                    onEditActivity = { onAction(ProfileAction.EditActivity(it)) },
                    viewerId = uiState.viewerId
                )
            }
        }

        ProfileTab.ACTIVITY -> {
            profileActivityTab(
                profile = profile,
                selectedFilter = uiState.selectedActivityFilter,
                onUserClick = onUserClick,
                onActivityClick = onActivityClick,
                onMediaClick = onMediaClick,
                onLastReplyClick = onLastReplyClick,
                onSubscribeClick = { onAction(ProfileAction.ToggleActivitySubscription(it)) },
                onLikeActivity = { onAction(ProfileAction.ToggleActivityLike(it)) },
                onDeleteActivity = { onAction(ProfileAction.DeleteActivity(it)) },
                onEditActivity = { onAction(ProfileAction.EditActivity(it)) },
                viewerId = uiState.viewerId,
                activitiesHasNextPage = uiState.activitiesHasNextPage,
                isActivitiesPaginating = uiState.isActivitiesPaginating,
                onLoadMore = { onAction(ProfileAction.LoadMoreActivities) }
            )
        }

        ProfileTab.SOCIAL -> {
            profileSocialTab(
                uiState = uiState,
                onUserClick = onUserClick,
                onThreadClick = onThreadClick,
                onCommentClick = onCommentClick,
                onLoadMore = { onAction(ProfileAction.LoadMoreSocial) },
                onRemoveFollower = if (uiState.viewerId == profile.id) {
                    { onAction(ProfileAction.AskToRemoveFollower(it)) }
                } else {
                    null
                },
                userColumns = portraitColumns
            )
        }

        ProfileTab.ANIME -> {
            profileMediaTab(
                itemsByStatus = uiState.userAnimeListByStatus,
                selectedStatus = uiState.selectedAnimeStatus,
                isLoading = uiState.isUserAnimeListLoading,
                mediaType = MediaType.ANIME,
                emptyMessageRes = R.string.profile_placeholder_anime,
                onMediaClick = onMediaClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                transitionPrefix = "profile_anime",
                // List rows are wide; 2-up is the most that stays legible (incl. the right pane).
                listColumns = statsColumns.coerceAtMost(2)
            )
        }

        ProfileTab.MANGA -> {
            profileMediaTab(
                itemsByStatus = uiState.userMangaListByStatus,
                selectedStatus = uiState.selectedMangaStatus,
                isLoading = uiState.isUserMangaListLoading,
                mediaType = MediaType.MANGA,
                emptyMessageRes = R.string.profile_placeholder_manga,
                onMediaClick = onMediaClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                transitionPrefix = "profile_manga",
                listColumns = statsColumns.coerceAtMost(2)
            )
        }

        ProfileTab.FAVORITES -> {
            profileFavoritesTab(
                profile = profile,
                selectedFilter = uiState.selectedFavoritesFilter,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onMediaClick = onMediaClick,
                onCharacterClick = onCharacterClick,
                onStaffClick = onStaffClick,
                onStudioClick = onStudioClick,
                // List rows are wide; 2-up is the most that stays legible (incl. the right pane).
                listColumns = statsColumns.coerceAtMost(2),
                studioColumns = studioColumns
            )
        }

        ProfileTab.REVIEWS -> {
            profileReviewsTab(
                uiState = uiState,
                onUserClick = onUserClick,
                onReviewClick = { onAction(ProfileAction.SelectReview(it)) },
                onLoadMore = { onAction(ProfileAction.LoadMoreReviews) }
            )
        }

        ProfileTab.STATS -> {
            item(key = "stats_top_gap") { Spacer(Modifier.height(8.dp)) }
            profileStatsTab(
                uiState = uiState,
                onOpenView = onStatsViewSelected,
                onGenreSortSelected = { onAction(ProfileAction.SelectGenreSort(it)) },
                onPeopleSortSelected = { list, sort -> onAction(ProfileAction.SelectPeopleSort(list, sort)) },
                onPeopleExpandToggled = { onAction(ProfileAction.TogglePeopleExpanded(it)) },
                onVoiceActorClick = onVoiceActorClick,
                onStaffClick = onStaffClick,
                onStudioClick = onStudioClick,
                onMediaClick = onMediaClick,
                onActivityClick = onActivityClick,
                statsColumns = statsColumns,
                onOpenRecap = onOpenRecap,
                onRetryStats = { onAction(ProfileAction.RetryStats) }
            )
        }
    }
}

/** Statistics once it has loaded and Favorites on a non-empty anime or manga list float the share button. */
internal fun ProfileUiState.showsShareFab(profile: UserProfile): Boolean = when (selectedTab) {
    ProfileTab.STATS -> statsData != null
    ProfileTab.FAVORITES -> when (selectedFavoritesFilter) {
        ProfileFavoritesFilter.ANIME -> profile.favoriteAnime.isNotEmpty()
        ProfileFavoritesFilter.MANGA -> profile.favoriteMangaOverview.isNotEmpty()
        else -> false
    }
    else -> false
}

/** The share button of whichever tab is open, in the corner of the profile on phones and tablets alike. */
@Composable
internal fun BoxScope.ProfileShareFab(uiState: ProfileUiState, onShareStats: () -> Unit, onShareFavourites: () -> Unit) {
    val stats = uiState.selectedTab == ProfileTab.STATS
    ShareFab(
        description = stringResource(if (stats) R.string.share_stats_card else R.string.share_favourites_card),
        onClick = if (stats) onShareStats else onShareFavourites,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp + shareFabBottomInset())
    )
}

private fun profileTabIcon(tab: ProfileTab): ImageVector {
    return when (tab) {
        ProfileTab.OVERVIEW -> Icons.Default.Person
        ProfileTab.ACTIVITY -> Icons.Default.Schedule
        ProfileTab.ANIME -> Icons.Default.Tv
        ProfileTab.MANGA -> Icons.AutoMirrored.Filled.MenuBook
        ProfileTab.FAVORITES -> Icons.Default.Group
        ProfileTab.SOCIAL -> Icons.Default.Forum
        ProfileTab.REVIEWS -> Icons.Default.RateReview
        ProfileTab.STATS -> Icons.Default.BarChart
    }
}

/** The profile tabs, and under them the open tab's rail, so the two dock together. */
@Composable
internal fun ProfileTabStrip(
    uiState: ProfileUiState,
    onTabSelected: (ProfileTab) -> Unit,
    onStatsTypeSelected: (ProfileStatsType) -> Unit,
    onStatsViewSelected: (StatsView) -> Unit,
    onFilterAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectListTab: (MediaType) -> Unit = {
        onTabSelected(if (it == MediaType.MANGA) ProfileTab.MANGA else ProfileTab.ANIME)
    }
    Column(modifier = modifier) {
        ProfileTabsButtonGroup(selectedTab = uiState.selectedTab, onTabSelected = onTabSelected)
        when (uiState.selectedTab) {
            ProfileTab.STATS -> StatsRail(
                statsType = uiState.selectedStatsType,
                views = if (uiState.viewerId != null && uiState.viewerId == uiState.profile?.id) {
                    StatsView.entries
                } else {
                    StatsView.entries.filter { it != StatsView.RECAPS }
                },
                selectedView = uiState.selectedStatsView,
                onTypeSelected = onStatsTypeSelected,
                onViewSelected = onStatsViewSelected,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            ProfileTab.ACTIVITY -> ActivityFilterRail(uiState.selectedActivityFilter) {
                onFilterAction(ProfileAction.SelectActivityFilter(it))
            }
            ProfileTab.ANIME -> MediaStatusRail(
                itemsByStatus = uiState.userAnimeListByStatus,
                selectedStatus = uiState.selectedAnimeStatus,
                mediaType = MediaType.ANIME,
                onStatusSelected = { onFilterAction(ProfileAction.SelectAnimeStatus(it)) },
                onMediaTypeSelected = selectListTab
            )
            ProfileTab.MANGA -> MediaStatusRail(
                itemsByStatus = uiState.userMangaListByStatus,
                selectedStatus = uiState.selectedMangaStatus,
                mediaType = MediaType.MANGA,
                onStatusSelected = { onFilterAction(ProfileAction.SelectMangaStatus(it)) },
                onMediaTypeSelected = selectListTab
            )
            ProfileTab.FAVORITES -> FavoritesFilterRail(uiState.selectedFavoritesFilter) {
                onFilterAction(ProfileAction.SelectFavoritesFilter(it))
            }
            ProfileTab.SOCIAL -> SocialTabRail(uiState) { onFilterAction(ProfileAction.SelectSocialTab(it)) }
            ProfileTab.OVERVIEW, ProfileTab.REVIEWS -> Unit
        }
    }
}

/** The in-list strip sits right after the profile header. */
private const val PROFILE_TABS_INDEX = 1

@Composable
internal fun ProfileTabsButtonGroup(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val whatsNew = rememberActiveWhatsNew()
    val mutualsNew = WhatsNewItem.PROFILE_MUTUALS in whatsNew
    val statsMoved = WhatsNewItem.PROFILE_STATS_MOVED in whatsNew
    val whatsNewStore = LocalWhatsNew.current
    LaunchedEffect(selectedTab) {
        if (selectedTab == ProfileTab.STATS) whatsNewStore.markSeen(WhatsNewItem.PROFILE_STATS_MOVED)
    }
    SegmentedTabGroup(
        options = ProfileTab.entries,
        selected = selectedTab,
        onSelect = onTabSelected,
        label = { stringResource(it.titleRes) },
        modifier = modifier.padding(vertical = 6.dp),
        icon = ::profileTabIcon,
        badge = { tab ->
            when {
                tab == ProfileTab.SOCIAL && mutualsNew -> ({ WhatsNewDot() })
                tab == ProfileTab.STATS && statsMoved -> ({ WhatsNewPill(WhatsNewItem.Kind.MOVED) })
                else -> null
            }
        }
    )
}
