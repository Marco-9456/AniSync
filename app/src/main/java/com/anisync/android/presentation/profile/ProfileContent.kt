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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.anisync.android.presentation.components.AppPullToRefreshBox
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
import com.anisync.android.presentation.profile.lists.ProfileListFilterSheet
import com.anisync.android.presentation.profile.lists.ProfileListSheet
import com.anisync.android.presentation.profile.lists.ProfileListSortSheet
import com.anisync.android.presentation.profile.lists.ProfileListToolbar
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.statistics.StatsRail
import com.anisync.android.presentation.navigation.ShareStudio
import com.anisync.android.presentation.share.studio.ShareStudioSubject
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import com.anisync.android.presentation.components.ScrollToTopOnRequest
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

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
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
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

    BackHandler(enabled = uiState.selectedTab.isPage) { onAction(ProfileAction.ClosePage) }

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
            modifier = modifier,
            onBackClick = onBackClick
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
    // Docking while typing would swap the field for its pinned copy and drop focus mid word.
    val onListQueryChange: (String) -> Unit = { query ->
        onAction(ProfileAction.SetListQuery(query))
        if (tabsDocked) scope.launch { dockStrip() }
    }
    val tabStrip: @Composable () -> Unit = {
        ProfileTabStrip(
            uiState = uiState,
            onTabSelected = onTabSelected,
            onStatsTypeSelected = { onAction(ProfileAction.SelectStatsType(it)) },
            onStatsViewSelected = onStatsViewSelected,
            onFilterAction = if (uiState.selectedTab.isPage) onAction else onFilterAction,
            onListQueryChange = onListQueryChange,
            onClosePage = { onAction(ProfileAction.ClosePage) }
        )
    }
    val tabContent: LazyListScope.() -> Unit = {
        profileSelectedTabContent(
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
            studioColumns = studioColumns,
            statsColumns = statsColumns,
            onOpenRecap = onOpenRecap,
            onStatsViewSelected = onStatsViewSelected
        )
    }

    val showShareFab = uiState.showsShareFab(profile)
    val bottomPadding = if (showShareFab) shareFabClearance() else LocalMainNavBarInset.current + 48.dp
    if (uiState.selectedTab.isPage) {
        val pageListState = rememberSaveable(uiState.pageListKey(), saver = LazyListState.Saver) { LazyListState() }
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                        tabStrip()
                    }
                }
                AppPullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = rememberRateLimitedRefresh { onAction(ProfileAction.Refresh()) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = pageListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomPadding)
                    ) {
                        tabContent()
                    }
                }
            }
            if (showShareFab) ProfileShareFab(uiState, onShareStats = shareStats, onShareFavourites = shareFavourites)
        }
    } else {
    Box(modifier = modifier.fillMaxSize()) {
    AppPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = rememberRateLimitedRefresh { onAction(ProfileAction.Refresh()) },
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { contentTopWindow = it.boundsInWindow().top },
        topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    ) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding)
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
                topActionIcon = Icons.Default.Settings,
                onTopActionClick = onSettingsClick,
                showAccountSwitcher = showAccountSwitcher,
                onAccountSwitchClick = onAccountSwitchClick,
                onBackClick = onBackClick,
                followingTotal = uiState.followingTotal,
                followersTotal = uiState.followersTotal,
                onPeopleClick = { onAction(ProfileAction.SelectTab(ProfileTab.SOCIAL)) }
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

        tabContent()
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

    }
    // Kept out of the pull box, which carries everything inside it down with the finger.
    if (showShareFab) ProfileShareFab(uiState, onShareStats = shareStats, onShareFavourites = shareFavourites)
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

    val listEntries = if (uiState.selectedTab == ProfileTab.MANGA) uiState.userMangaList else uiState.userAnimeList
    val distinctListEntries = remember(listEntries) { listEntries.distinctBy { it.id } }
    when (uiState.listSheet) {
        ProfileListSheet.SORT -> ProfileListSortSheet(
            tools = uiState.listTools,
            isOwnProfile = isOwnProfile,
            onSortChange = { sort, ascending -> onAction(ProfileAction.SetListSort(sort, ascending)) },
            onDismiss = { onAction(ProfileAction.OpenListSheet(ProfileListSheet.NONE)) }
        )
        ProfileListSheet.FILTERS -> ProfileListFilterSheet(
            entries = distinctListEntries,
            status = if (uiState.selectedTab == ProfileTab.MANGA) uiState.selectedMangaStatus else uiState.selectedAnimeStatus,
            tools = uiState.listTools,
            viewer = uiState.viewerEntries,
            titleLanguage = uiState.titleLanguage,
            ownerScoreFormat = distinctListEntries.firstNotNullOfOrNull { it.scoreFormat } ?: ScoreFormat.POINT_100,
            isOwnProfile = isOwnProfile,
            onFiltersChange = { onAction(ProfileAction.SetListFilters(it)) },
            onDismiss = { onAction(ProfileAction.OpenListSheet(ProfileListSheet.NONE)) }
        )
        ProfileListSheet.NONE -> Unit
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

        ProfileTab.ACTIVITY -> when (uiState.selectedActivityFilter) {
            ProfileActivityFilter.REVIEWS -> profileReviewsTab(
                uiState = uiState,
                onUserClick = onUserClick,
                onReviewClick = { onAction(ProfileAction.SelectReview(it)) },
                onLoadMore = { onAction(ProfileAction.LoadMoreReviews) }
            )
            ProfileActivityFilter.THREADS, ProfileActivityFilter.COMMENTS -> profileSocialTab(
                uiState = uiState,
                selectedTab = if (uiState.selectedActivityFilter == ProfileActivityFilter.THREADS) {
                    ProfileSocialTab.FORUM_THREADS
                } else {
                    ProfileSocialTab.FORUM_COMMENTS
                },
                onUserClick = onUserClick,
                onThreadClick = onThreadClick,
                onCommentClick = onCommentClick,
                onLoadMore = { onAction(ProfileAction.LoadMoreSocial) }
            )
            else -> profileActivityTab(
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

        ProfileTab.ANIME, ProfileTab.MANGA -> {
            val manga = uiState.selectedTab == ProfileTab.MANGA
            profileMediaTab(
                results = uiState.listResults,
                tools = uiState.listTools,
                selectedStatus = if (manga) uiState.selectedMangaStatus else uiState.selectedAnimeStatus,
                hasEntries = (if (manga) uiState.userMangaList else uiState.userAnimeList).isNotEmpty(),
                isLoading = if (manga) uiState.isUserMangaListLoading else uiState.isUserAnimeListLoading,
                loadFailed = if (manga) uiState.isMangaListFailed else uiState.isAnimeListFailed,
                mediaType = if (manga) MediaType.MANGA else MediaType.ANIME,
                ownerName = profile.name,
                isOwnProfile = isOwnProfile,
                viewer = uiState.viewerEntries,
                yourScoreFormat = uiState.viewerScoreFormat,
                titleLanguage = uiState.titleLanguage,
                onMediaClick = onMediaClick,
                onOpenSort = { onAction(ProfileAction.OpenListSheet(ProfileListSheet.SORT)) },
                onClearFilters = { onAction(ProfileAction.ClearListFilters) },
                onRetry = { onAction(ProfileAction.RetryList) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                transitionPrefix = if (manga) "profile_manga" else "profile_anime",
                // List rows are wide; 2-up is the most that stays legible (incl. the right pane).
                listColumns = statsColumns.coerceAtMost(2),
                gridColumns = studioColumns.coerceAtLeast(2),
                onAddToPlanning = if (isOwnProfile) null else { mediaId -> onAction(ProfileAction.AddToPlanning(mediaId)) }
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
                onRetryStats = { onAction(ProfileAction.RetryStats) },
                onRetryRecaps = { onAction(ProfileAction.RetryRecaps) }
            )
        }
    }
}

/** Statistics once it has loaded, outside Recaps, and Favorites on a non-empty anime or manga list float the share button. */
internal fun ProfileUiState.showsShareFab(profile: UserProfile): Boolean = when (selectedTab) {
    ProfileTab.STATS -> statsData != null && selectedStatsView != StatsView.RECAPS
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

private enum class StripTab(@StringRes val titleRes: Int) {
    OVERVIEW(R.string.profile_tab_overview),
    LISTS(R.string.profile_tab_lists),
    STATS(R.string.profile_tab_stats),
    ACTIVITY(R.string.profile_tab_activity)
}

private fun ProfileTab.stripTab(): StripTab = when (this) {
    ProfileTab.OVERVIEW -> StripTab.OVERVIEW
    ProfileTab.ANIME, ProfileTab.MANGA -> StripTab.LISTS
    ProfileTab.STATS -> StripTab.STATS
    else -> StripTab.ACTIVITY
}

internal fun ProfileUiState.pageListKey(): String = when (selectedTab) {
    ProfileTab.FAVORITES -> "FAVORITES_${selectedFavoritesFilter.name}"
    ProfileTab.SOCIAL -> "SOCIAL_${selectedSocialTab.name}"
    else -> selectedTab.name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePageBar(title: String, subtitle: String?, onBack: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

/** The profile tabs, and under them the open tab's rail, so the two dock together. */
@Composable
internal fun ProfileTabStrip(
    uiState: ProfileUiState,
    onTabSelected: (ProfileTab) -> Unit,
    onStatsTypeSelected: (ProfileStatsType) -> Unit,
    onStatsViewSelected: (StatsView) -> Unit,
    onFilterAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
    onListQueryChange: (String) -> Unit = { onFilterAction(ProfileAction.SetListQuery(it)) },
    onClosePage: () -> Unit = { onFilterAction(ProfileAction.ClosePage) }
) {
    val selectListTab: (MediaType) -> Unit = {
        onTabSelected(if (it == MediaType.MANGA) ProfileTab.MANGA else ProfileTab.ANIME)
    }
    Column(modifier = modifier) {
        if (uiState.selectedTab.isPage) {
            ProfilePageBar(
                title = stringResource(uiState.selectedTab.titleRes),
                subtitle = uiState.profile?.name,
                onBack = onClosePage
            )
        } else {
            ProfileTabsButtonGroup(
                selectedTab = uiState.selectedTab,
                lastListTab = uiState.lastListTab,
                onTabSelected = onTabSelected
            )
        }
        when (uiState.selectedTab) {
            ProfileTab.STATS -> {
                val own = uiState.viewerId != null && uiState.viewerId == uiState.profile?.id
                StatsRail(
                    statsType = uiState.selectedStatsType,
                    views = if (own) StatsView.entries else StatsView.entries.filter { it != StatsView.RECAPS },
                    selectedView = uiState.selectedStatsView,
                    onTypeSelected = onStatsTypeSelected,
                    onViewSelected = onStatsViewSelected,
                    isOwnProfile = own,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
            ProfileTab.ACTIVITY -> ActivityFilterRail(uiState.selectedActivityFilter) {
                onFilterAction(ProfileAction.SelectActivityFilter(it))
            }
            ProfileTab.ANIME, ProfileTab.MANGA -> {
                val manga = uiState.selectedTab == ProfileTab.MANGA
                MediaStatusRail(
                    itemsByStatus = if (manga) uiState.userMangaListByStatus else uiState.userAnimeListByStatus,
                    selectedStatus = if (manga) uiState.selectedMangaStatus else uiState.selectedAnimeStatus,
                    mediaType = if (manga) MediaType.MANGA else MediaType.ANIME,
                    onStatusSelected = {
                        onFilterAction(if (manga) ProfileAction.SelectMangaStatus(it) else ProfileAction.SelectAnimeStatus(it))
                    },
                    onMediaTypeSelected = selectListTab
                )
                ProfileListToolbar(
                    query = uiState.listTools.query,
                    onQueryChange = onListQueryChange,
                    view = uiState.listTools.view,
                    onToggleView = { onFilterAction(ProfileAction.ToggleListView) },
                    activeFilters = uiState.listTools.filters.activeCount,
                    onOpenFilters = { onFilterAction(ProfileAction.OpenListSheet(ProfileListSheet.FILTERS)) }
                )
            }
            ProfileTab.FAVORITES -> FavoritesFilterRail(uiState.selectedFavoritesFilter) {
                onFilterAction(ProfileAction.SelectFavoritesFilter(it))
            }
            ProfileTab.SOCIAL -> SocialTabRail(uiState) { onFilterAction(ProfileAction.SelectSocialTab(it)) }
            ProfileTab.OVERVIEW -> Unit
        }
    }
}

/** The in-list strip sits right after the profile header. */
private const val PROFILE_TABS_INDEX = 1

@Composable
internal fun ProfileTabsButtonGroup(
    selectedTab: ProfileTab,
    lastListTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val whatsNew = rememberActiveWhatsNew()
    val whatsNewStore = LocalWhatsNew.current
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            ProfileTab.STATS -> whatsNewStore.markSeen(WhatsNewItem.PROFILE_STATS_MOVED)
            ProfileTab.ANIME, ProfileTab.MANGA -> whatsNewStore.markSeen(WhatsNewItem.PROFILE_LISTS)
            else -> Unit
        }
    }
    SegmentedTabGroup(
        options = StripTab.entries,
        selected = selectedTab.stripTab(),
        onSelect = { tab ->
            onTabSelected(
                when (tab) {
                    StripTab.OVERVIEW -> ProfileTab.OVERVIEW
                    StripTab.LISTS -> lastListTab
                    StripTab.STATS -> ProfileTab.STATS
                    StripTab.ACTIVITY -> ProfileTab.ACTIVITY
                }
            )
        },
        label = { stringResource(it.titleRes) },
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        fillEqually = true,
        contentPadding = PaddingValues(horizontal = 8.dp),
        badge = { tab ->
            when {
                tab == StripTab.STATS && WhatsNewItem.PROFILE_STATS_MOVED in whatsNew -> ({ WhatsNewPill(WhatsNewItem.Kind.MOVED) })
                tab == StripTab.LISTS && WhatsNewItem.PROFILE_LISTS in whatsNew -> ({ WhatsNewPill(WhatsNewItem.Kind.NEW) })
                tab == StripTab.ACTIVITY && WhatsNewItem.PROFILE_POSTS_MOVED in whatsNew -> ({ WhatsNewDot() })
                else -> null
            }
        }
    )
}
