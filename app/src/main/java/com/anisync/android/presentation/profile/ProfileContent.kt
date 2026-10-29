package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.WhatsNewDot
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
import com.anisync.android.presentation.profile.sections.profileActivityTab
import com.anisync.android.presentation.profile.sections.profileFavoritesTab
import com.anisync.android.presentation.profile.sections.profileMediaTab
import com.anisync.android.presentation.profile.sections.profileReviewsTab
import com.anisync.android.presentation.profile.sections.profileSocialTab
import com.anisync.android.presentation.profile.sections.profileStatsTab
import com.anisync.android.presentation.share.FavouritesShareCard
import com.anisync.android.presentation.share.ProfileStatsShareCard
import com.anisync.android.presentation.share.ShareCardTemplate
import com.anisync.android.presentation.share.ShareImageSheet
import com.anisync.android.presentation.statistics.StatsRail
import com.anisync.android.presentation.statistics.StatsShareFab
import com.anisync.android.presentation.statistics.statsFabClearance
import com.anisync.android.type.MediaType
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()
    val statsColumns = dashboardColumns()

    // Share-as-image sheets for the Stats and Favourites tabs, hosted at screen scope below.
    var statsShareVisible by remember { mutableStateOf(false) }
    var favouritesShareVisible by remember { mutableStateOf(false) }

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
            onShareStats = { statsShareVisible = true },
            statsColumns = statsColumns,
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
    val tabStrip: @Composable () -> Unit = {
        ProfileTabStrip(
            uiState = uiState,
            onTabSelected = onTabSelected,
            onStatsTypeSelected = { onAction(ProfileAction.SelectStatsType(it)) },
            onStatsViewSelected = onStatsViewSelected
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
    val showStatsFab = uiState.selectedTab == ProfileTab.STATS && uiState.statsData != null
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = LocalMainNavBarInset.current + if (showStatsFab) statsFabClearance() else 48.dp
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
            showShareActions = true,
            onShareFavourites = { favouritesShareVisible = true },
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

    if (showStatsFab) {
        StatsShareFab(
            onClick = { statsShareVisible = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp + LocalMainNavBarInset.current)
        )
    }
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

    val profileUrl = "https://anilist.co/user/${profile.name}"

    if (statsShareVisible) {
        uiState.statsData?.let { stats ->
            ShareImageSheet(
                onDismiss = { statsShareVisible = false },
                link = profileUrl,
                templates = listOf(ShareCardTemplate.STANDARD, ShareCardTemplate.HERO),
                templateLabel = { tmpl ->
                    stringResource(
                        if (tmpl == ShareCardTemplate.STANDARD) R.string.share_template_stats
                        else R.string.share_template_recap
                    )
                }
            ) {
                ProfileStatsShareCard(
                    profile = profile,
                    stats = stats,
                    type = uiState.selectedStatsType
                )
            }
        }
    }

    if (favouritesShareVisible) {
        val isAnime = uiState.selectedFavoritesFilter == ProfileFavoritesFilter.ANIME
        val entries = if (isAnime) profile.favoriteAnime else profile.favoriteMangaOverview
        if (entries.isNotEmpty()) {
            ShareImageSheet(
                onDismiss = { favouritesShareVisible = false },
                link = profileUrl,
                templates = listOf(ShareCardTemplate.STANDARD, ShareCardTemplate.HERO),
                templateLabel = { tmpl ->
                    stringResource(
                        if (tmpl == ShareCardTemplate.STANDARD) R.string.share_template_grid
                        else R.string.share_template_ranked
                    )
                }
            ) {
                FavouritesShareCard(
                    heading = stringResource(
                        if (isAnime) R.string.share_fav_heading_anime
                        else R.string.share_fav_heading_manga
                    ),
                    eyebrow = stringResource(R.string.share_fav_eyebrow),
                    entries = entries,
                    bannerUrl = profile.bannerUrl,
                    handle = profile.name
                )
            }
        }
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
    showShareActions: Boolean = false,
    onShareFavourites: () -> Unit = {},
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
                onFilterSelected = { onAction(ProfileAction.SelectActivityFilter(it)) },
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
                onTabSelected = { onAction(ProfileAction.SelectSocialTab(it)) },
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
                onStatusSelected = { onAction(ProfileAction.SelectAnimeStatus(it)) },
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
                onStatusSelected = { onAction(ProfileAction.SelectMangaStatus(it)) },
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
            val favShareable = when (uiState.selectedFavoritesFilter) {
                ProfileFavoritesFilter.ANIME -> profile.favoriteAnime.isNotEmpty()
                ProfileFavoritesFilter.MANGA -> profile.favoriteMangaOverview.isNotEmpty()
                else -> false
            }
            if (showShareActions && favShareable) {
                item(key = "favourites_share_action") {
                    ShareTabAction(
                        label = stringResource(R.string.share_favourites_card),
                        onClick = onShareFavourites
                    )
                }
            }
            profileFavoritesTab(
                profile = profile,
                selectedFilter = uiState.selectedFavoritesFilter,
                onFilterSelected = { onAction(ProfileAction.SelectFavoritesFilter(it)) },
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
                onVoiceActorClick = onVoiceActorClick,
                onStaffClick = onStaffClick,
                onStudioClick = onStudioClick,
                onMediaClick = onMediaClick,
                onActivityClick = onActivityClick,
                statsColumns = statsColumns
            )
        }
    }
}

/** Right-aligned share affordance rendered above a shareable profile tab's content. */
@Composable
private fun ShareTabAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        horizontalArrangement = Arrangement.End
    ) {
        FilledTonalButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
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

/** The profile tabs, and under them the Statistics rail while Statistics is open. */
@Composable
internal fun ProfileTabStrip(
    uiState: ProfileUiState,
    onTabSelected: (ProfileTab) -> Unit,
    onStatsTypeSelected: (ProfileStatsType) -> Unit,
    onStatsViewSelected: (StatsView) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ProfileTabsButtonGroup(selectedTab = uiState.selectedTab, onTabSelected = onTabSelected)
        if (uiState.selectedTab == ProfileTab.STATS) {
            StatsRail(
                statsType = uiState.selectedStatsType,
                views = StatsView.entries.filter { it != StatsView.RECAPS },
                selectedView = uiState.selectedStatsView,
                onTypeSelected = onStatsTypeSelected,
                onViewSelected = onStatsViewSelected,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
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
    val mutualsNew = WhatsNewItem.PROFILE_MUTUALS in rememberActiveWhatsNew()
    SegmentedTabGroup(
        options = ProfileTab.entries,
        selected = selectedTab,
        onSelect = onTabSelected,
        label = { stringResource(it.titleRes) },
        // 6 rather than 8: each 44dp tab sits in a 48dp touch box.
        modifier = modifier.padding(vertical = 6.dp),
        icon = ::profileTabIcon,
        badge = { tab -> if (tab == ProfileTab.SOCIAL && mutualsNew) ({ WhatsNewDot() }) else null }
    )
}
