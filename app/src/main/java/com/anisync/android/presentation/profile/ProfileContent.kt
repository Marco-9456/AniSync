package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.MediaTypeToggle
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.presentation.components.ShareFab
import com.anisync.android.presentation.components.TabRequestEffect
import com.anisync.android.presentation.components.shareFabBottomInset
import com.anisync.android.presentation.components.shareFabClearance
import com.anisync.android.presentation.components.AppPullToRefreshBox
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.LocalPullRefreshOffset
import com.anisync.android.presentation.components.LocalWhatsNew
import com.anisync.android.presentation.components.WhatsNewDot
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.components.rememberActiveWhatsNew
import com.anisync.android.presentation.details.components.ReviewDetailsSheet
import com.anisync.android.presentation.profile.components.DirectMessageInputSheet
import com.anisync.android.presentation.profile.components.ProfileAboutSheet
import com.anisync.android.presentation.profile.components.ProfileBackdrop
import com.anisync.android.presentation.profile.components.ProfileAccounts
import com.anisync.android.presentation.profile.components.ProfileOwnDockedBar
import com.anisync.android.presentation.profile.components.ProfileRails
import com.anisync.android.presentation.profile.components.ProfileTopSection
import com.anisync.android.presentation.profile.components.ProfileVisitBar
import com.anisync.android.presentation.profile.lists.ProfileListTopBar
import com.anisync.android.presentation.profile.sections.OverviewCompareCard
import com.anisync.android.presentation.profile.sections.OverviewCompareCardLoading
import com.anisync.android.presentation.profile.sections.PeopleSwitcher
import com.anisync.android.presentation.profile.sections.StatusComposerCard
import com.anisync.android.presentation.profile.sections.profileComparePage
import com.anisync.android.presentation.share.cards.fitted
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.dashboardColumns
import com.anisync.android.presentation.util.openUrl
import com.anisync.android.presentation.util.profileGridColumns
import com.anisync.android.presentation.profile.sections.OverviewBlocks
import com.anisync.android.presentation.profile.sections.OverviewColumnA
import com.anisync.android.presentation.profile.sections.OverviewColumnB
import com.anisync.android.presentation.profile.sections.profileOverviewBlocks
import com.anisync.android.presentation.profile.sections.ActivityFilterRail
import com.anisync.android.presentation.profile.sections.FavoritesFilterRail
import com.anisync.android.presentation.profile.sections.MediaStatusRail
import com.anisync.android.presentation.profile.sections.profileActivityTab
import com.anisync.android.presentation.profile.sections.profileFavoritesTab
import com.anisync.android.presentation.profile.sections.profileMediaTab
import com.anisync.android.presentation.profile.sections.profileReviewsTab
import com.anisync.android.presentation.profile.sections.profileSocialTab
import com.anisync.android.presentation.profile.sections.profileStatsTab
import com.anisync.android.presentation.profile.lists.ProfileListFilterSheet
import com.anisync.android.presentation.profile.lists.ProfileListSheet
import com.anisync.android.presentation.profile.lists.ProfileListSortSheet
import com.anisync.android.presentation.statistics.StatsRail
import com.anisync.android.presentation.navigation.ShareStudio
import com.anisync.android.presentation.share.studio.ShareStudioSubject
import com.anisync.android.type.MediaType
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.key
import com.anisync.android.presentation.profile.components.profileBackdropHeight
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

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
    accounts: ProfileAccounts? = null,
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
            accounts = accounts,
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

    // Every tab docks under a bar of the same height below the status bar, the list's search on
    // Lists and whose profile this is on the others.
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val dockTarget = with(density) { (statusBarInset + ProfileDockBarHeight).roundToPx() }
    val header = rememberProfileHeaderState()
    val listStates = rememberProfileListStates()
    val contentKey = uiState.contentKey
    val listState = listStates.of(contentKey)
    val rails = remember { ProfileRails() }

    TabRequestEffect(scrollToTopRequest) {
        coroutineScope {
            launch {
                if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
                    listState.animateScrollToItem(0)
                }
            }
            header.animateExpand()
        }
    }
    TabRequestEffect(dockRequest) { header.dock() }
    // Picking a tab or a chip never moves the page, so only a docked strip keeps each one's place.
    ProfileListPlacement(uiState, listState, keepsPlace = { header.docked })

    val onTabSelected: (ProfileTab) -> Unit = { onAction(ProfileAction.SelectTab(it)) }
    val onStatsViewSelected: (StatsView) -> Unit = { view ->
        if (view != uiState.selectedStatsView) onAction(ProfileAction.SelectStatsView(view))
    }
    val onListQueryChange: (String) -> Unit = { onAction(ProfileAction.SetListQuery(it)) }
    val overviewBlocks = rememberOverviewBlocks(
        profile, uiState, onAction, onMediaClick, onCharacterClick, onStaffClick, onUserClick, onActivityClick, onLastReplyClick
    )
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
            onStatsViewSelected = onStatsViewSelected,
            overviewBlocks = overviewBlocks
        )
    }

    val showShareFab = uiState.showsShareFab(profile)
    val bottomPadding = profileBottomPadding(showShareFab)
    if (uiState.selectedTab.isPage) {
        val whatsNewStore = LocalWhatsNew.current
        LaunchedEffect(uiState.selectedTab) {
            if (uiState.selectedTab == ProfileTab.COMPARE) whatsNewStore.markSeen(WhatsNewItem.PROFILE_COMPARE)
        }
        Box(modifier = modifier.fillMaxSize()) {
            ProfilePage(
                uiState = uiState,
                listState = listState,
                bottomPadding = bottomPadding,
                onAction = onAction,
                content = tabContent
            )
            if (showShareFab) ProfileShareFab(uiState, onShareStats = shareStats, onShareFavourites = shareFavourites)
        }
    } else {
    Box(modifier = modifier.fillMaxSize()) {
    AppPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = rememberRateLimitedRefresh { onAction(ProfileAction.Refresh()) },
        modifier = Modifier.fillMaxSize(),
        topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
        // The page only pulls from its top, where the header's banner sits exactly over this one.
        backdrop = {
            val pullOffset = LocalPullRefreshOffset.current
            ProfileBackdrop(
                profile = profile,
                isOwnProfile = isOwnProfile,
                onSettingsClick = onSettingsClick,
                onBackClick = onBackClick,
                modifier = Modifier.graphicsLayer { alpha = if (pullOffset() > 0f) 1f else 0f }
            )
        }
    ) {
    ProfileCollapsingLayout(
        state = header,
        listState = listState,
        dockTargetPx = dockTarget,
        modifier = Modifier.fillMaxSize(),
        header = {
            Column {
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
                    accounts = accounts,
                    onBackClick = onBackClick,
                    followingTotal = uiState.followingTotal,
                    followersTotal = uiState.followersTotal,
                    onPeopleClick = { onAction(ProfileAction.SelectTab(ProfileTab.SOCIAL)) }
                )
                if (uiState.isShowingSavedCopy) SavedCopyNotice(onRetry = { onAction(ProfileAction.Refresh()) })
            }
        },
        strip = {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                ProfileTabStrip(
                    uiState = uiState,
                    onTabSelected = onTabSelected,
                    onStatsTypeSelected = { onAction(ProfileAction.SelectStatsType(it)) },
                    onStatsViewSelected = onStatsViewSelected,
                    onFilterAction = onAction,
                    onListQueryChange = onListQueryChange,
                    showListSearch = false,
                    rails = rails
                )
            }
        },
        content = {
            key(contentKey) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomPadding),
                    content = tabContent
                )
            }
        }
    )

    val docked by remember(header) { derivedStateOf { header.docked } }
    val statusScrimFrom = with(density) { (profileBackdropHeight(profile.bannerUrl == null) - statusBarInset - 8.dp).toPx() }
    val showStatusScrim by remember(header, statusScrimFrom) { derivedStateOf { header.collapsed > statusScrimFrom } }
    if (showStatusScrim && !docked) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(statusBarInset)
                .background(MaterialTheme.colorScheme.background)
        )
    }

    if (docked) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                when {
                    uiState.selectedTab.isList ->
                        ProfileListSearchBar(
                            uiState = uiState,
                            onQueryChange = onListQueryChange,
                            onFilterAction = onAction,
                            onBack = onBackClick
                        )
                    isOwnProfile -> ProfileOwnDockedBar(
                        name = profile.name,
                        avatarUrl = profile.avatarUrl,
                        accounts = accounts,
                        onSettingsClick = onSettingsClick
                    )
                    else -> ProfileVisitBar(
                        name = profile.name,
                        avatarUrl = profile.avatarUrl,
                        onBackClick = onBackClick,
                        isFollowing = uiState.isFollowingUser,
                        isFollowLoading = uiState.isFollowLoading,
                        onFollowClick = { onAction(ProfileAction.ToggleFollow) }
                    )
                }
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
        ProfileAboutSheet(
            profile = profile,
            isOwnProfile = isOwnProfile,
            onEditAbout = {
                onAction(ProfileAction.SetBiographySheetVisible(false))
                onAction(ProfileAction.SetEditProfileDialogVisible(true))
            },
            onDismissRequest = { onAction(ProfileAction.SetBiographySheetVisible(false)) }
        )
    }

    when (uiState.listSheet) {
        ProfileListSheet.SORT -> ProfileListSortSheet(
            tools = uiState.listTools,
            isOwnProfile = isOwnProfile,
            onSortChange = { sort, ascending -> onAction(ProfileAction.SetListSort(sort, ascending)) },
            onDismiss = { onAction(ProfileAction.OpenListSheet(ProfileListSheet.NONE)) }
        )
        ProfileListSheet.FILTERS -> ProfileListFilterSheet(
            entries = uiState.selectedListEntries,
            tools = uiState.listTools,
            results = uiState.listResults,
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

    if (uiState.isStatusComposerVisible) {
        com.anisync.android.presentation.components.richtext.RichTextInputSheet(
            title = stringResource(R.string.profile_post_status),
            placeholder = stringResource(R.string.feed_compose_placeholder),
            submitLabel = stringResource(R.string.post),
            isSubmitting = uiState.isPostingStatus,
            minLength = com.anisync.android.domain.ContentLimits.TextActivity.min,
            maxLength = com.anisync.android.domain.ContentLimits.TextActivity.max,
            onSubmit = { body -> onAction(ProfileAction.PostStatus(body)) },
            onDismiss = { onAction(ProfileAction.SetStatusComposerVisible(false)) }
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
    onStatsViewSelected: (StatsView) -> Unit = { onAction(ProfileAction.SelectStatsView(it)) },
    overviewBlocks: OverviewBlocks,
    /** A pane wide enough for T1's two Overview columns. */
    overviewColumns: Boolean = false
) {
    // A list keeps its first visible item where it is, and an empty first block is skipped over, so
    // one that fills in later, the comparison once both lists are in, would open above the screen.
    item(key = "tab_top", contentType = "tab_top") { TabTopAnchor() }
    when (uiState.selectedTab) {
        ProfileTab.OVERVIEW -> {
            val blocks = overviewBlocks
            // The composer and the picture prompt on your own profile, how your lists meet theirs on
            // someone else's.
            val lead: @Composable () -> Unit = {
                OverviewLead(profile = profile, uiState = uiState, isOwnProfile = isOwnProfile, onAction = onAction)
            }
            if (overviewColumns && !blocks.isEmpty) {
                item(key = "overview_columns", contentType = "overview_columns") {
                    // Each block keeps its own 16dp margins, so the columns overlap by one margin
                    // and the cards meet across a 16dp gap.
                    Row(horizontalArrangement = Arrangement.spacedBy((-16).dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            lead()
                            OverviewColumnA(blocks)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            OverviewColumnB(blocks)
                        }
                    }
                }
                item(key = "overview_end", contentType = "overview_end") { Spacer(modifier = Modifier.height(16.dp)) }
            } else {
                item(key = "overview_lead", contentType = "overview_lead") { lead() }
                profileOverviewBlocks(blocks)
            }
        }

        ProfileTab.ACTIVITY -> when (uiState.selectedActivityFilter) {
            ProfileActivityFilter.REVIEWS -> profileReviewsTab(
                uiState = uiState,
                onUserClick = onUserClick,
                onReviewClick = { onAction(ProfileAction.SelectReview(it)) },
                onLoadMore = { onAction(ProfileAction.LoadMoreReviews) },
                onRetry = { onAction(ProfileAction.RetryReviews) }
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
                onLoadMore = { onAction(ProfileAction.LoadMoreSocial) },
                onRetry = { onAction(ProfileAction.RetrySocial) }
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
                onLoadMore = { onAction(ProfileAction.LoadMoreActivities) },
                onCompose = if (isOwnProfile) {
                    { onAction(ProfileAction.SetStatusComposerVisible(true)) }
                } else {
                    null
                },
                loadFailed = uiState.isActivityFailed,
                onRetry = { onAction(ProfileAction.Refresh()) }
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
                userColumns = portraitColumns,
                onToggleFollow = { onAction(ProfileAction.TogglePersonFollow(it)) },
                onRetry = { onAction(ProfileAction.RetrySocial) }
            )
        }

        ProfileTab.ANIME, ProfileTab.MANGA -> {
            val manga = uiState.selectedTab == ProfileTab.MANGA
            val entries = uiState.selectedListEntries
            // Switching between Anime and Manga waits for the rows rather than showing the other type's.
            val settling = uiState.listSettling
            profileMediaTab(
                results = uiState.listResults,
                tools = uiState.listTools,
                selectedStatus = uiState.selectedListStatus,
                hasEntries = entries.isNotEmpty() && !settling,
                isLoading = settling || if (manga) uiState.isUserMangaListLoading else uiState.isUserAnimeListLoading,
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
                transitionPrefix = if (manga) "profile_manga" else "profile_anime",
                // List rows are wide; 2-up is the most that stays legible (incl. the right pane).
                listColumns = statsColumns.coerceAtMost(2),
                gridColumns = studioColumns.coerceAtLeast(2),
                onAddToPlanning = if (isOwnProfile) null else { mediaId -> onAction(ProfileAction.AddToPlanning(mediaId)) },
                ownerAvatar = profile.avatarUrl,
                yourAvatar = uiState.viewerAvatarUrl,
                onRemoveFilter = { onAction(ProfileAction.SetListFilters(it)) }
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
                studioColumns = studioColumns,
                gridColumns = portraitColumns,
                titleLanguage = uiState.titleLanguage
            )
        }

        ProfileTab.COMPARE -> profileComparePage(
            comparison = uiState.comparison,
            isLoading = if (uiState.compareType == MediaType.MANGA) uiState.isUserMangaListLoading else uiState.isUserAnimeListLoading,
            type = uiState.compareType,
            ownerName = profile.name,
            yourAvatar = uiState.viewerAvatarUrl,
            theirAvatar = profile.avatarUrl,
            yourScoreFormat = uiState.viewerScoreFormat,
            titleLanguage = uiState.titleLanguage,
            onMediaClick = onMediaClick,
            expanded = uiState.compareExpanded,
            onExpand = { onAction(ProfileAction.ExpandCompareSection(it)) }
        )

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

/** The Overview's blocks, built again only when what they show or open changes. */
@Composable
internal fun rememberOverviewBlocks(
    profile: UserProfile,
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    onActivityClick: (Int) -> Unit,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit
): OverviewBlocks {
    val activityHistory = uiState.statsData?.activityHistory.orEmpty()
    val latestReview = uiState.reviews.firstOrNull()
    return remember(
        profile, activityHistory, uiState.watching, latestReview, uiState.titleLanguage, uiState.viewerId,
        onAction, onMediaClick, onCharacterClick, onStaffClick, onUserClick, onActivityClick, onLastReplyClick
    ) {
        OverviewBlocks(
            profile = profile,
            activityHistory = activityHistory,
            watching = uiState.watching,
            latestReview = latestReview,
            titleLanguage = uiState.titleLanguage,
            viewerId = uiState.viewerId,
            onOpenActivity = { onAction(ProfileAction.SelectTab(ProfileTab.ACTIVITY)) },
            onOpenFavorites = {
                onAction(ProfileAction.SelectFavoritesFilter(it))
                onAction(ProfileAction.SelectTab(ProfileTab.FAVORITES))
            },
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
            onOpenWatching = {
                onAction(ProfileAction.SelectAnimeStatus(LibraryStatus.CURRENT))
                onAction(ProfileAction.SelectTab(ProfileTab.ANIME))
            },
            onOpenReviews = {
                onAction(ProfileAction.SelectTab(ProfileTab.ACTIVITY))
                onAction(ProfileAction.SelectActivityFilter(ProfileActivityFilter.REVIEWS))
            },
            onReviewClick = { onAction(ProfileAction.SelectReview(it)) }
        )
    }
}

@Composable
private fun OverviewLead(
    profile: UserProfile,
    uiState: ProfileUiState,
    isOwnProfile: Boolean,
    onAction: (ProfileAction) -> Unit
) {
    val comparison = uiState.comparison
    Column {
        if (isOwnProfile) {
            StatusComposerCard(
                avatarUrl = profile.avatarUrl,
                onClick = { onAction(ProfileAction.SetStatusComposerVisible(true)) }
            )
            if (profile.bannerUrl == null) PicturePrompt()
        }
        if (!isOwnProfile && comparison == null && uiState.isUserAnimeListLoading && uiState.viewerEntries.isNotEmpty()) {
            OverviewCompareCardLoading(
                ownerName = profile.name,
                yourAvatar = uiState.viewerAvatarUrl,
                theirAvatar = profile.avatarUrl,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        if (comparison != null && comparison.sharedCount > 0) {
            OverviewCompareCard(
                comparison = comparison,
                ownerName = profile.name,
                yourAvatar = uiState.viewerAvatarUrl,
                theirAvatar = profile.avatarUrl,
                onCompare = { onAction(ProfileAction.SelectTab(ProfileTab.COMPARE)) },
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

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
    LISTS(R.string.profile_activity_lists),
    STATS(R.string.details_tab_stats),
    ACTIVITY(R.string.profile_tab_activity)
}

private fun ProfileTab.stripTab(): StripTab = when (this) {
    ProfileTab.OVERVIEW -> StripTab.OVERVIEW
    ProfileTab.ANIME, ProfileTab.MANGA -> StripTab.LISTS
    ProfileTab.STATS -> StripTab.STATS
    else -> StripTab.ACTIVITY
}

@Composable
private fun TabTopAnchor() {
    Spacer(modifier = Modifier.fillMaxWidth().height(with(LocalDensity.current) { 1.toDp() }))
}

/** Avatar and banner only change on the website, so the prompt hands off to AniList's settings. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PicturePrompt() {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialShapes.Cookie9Sided.fitted())
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.profile_picture_prompt_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.profile_picture_prompt_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                Button(onClick = { openUrl(context, ANILIST_SETTINGS_URL) }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_picture_prompt_action), maxLines = 1)
                }
            }
        }
    }
}

private const val ANILIST_SETTINGS_URL = "https://anilist.co/settings"

@Composable
private fun SavedCopyNotice(onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.profile_offline_saved_copy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

/** Only the pages have a title: People, Favourites, and Compare in the last branch. */
@Composable
private fun pageTitle(uiState: ProfileUiState): String {
    val name = uiState.profile?.name.orEmpty()
    val own = uiState.viewerOwnsProfile
    return when (uiState.selectedTab) {
        ProfileTab.SOCIAL ->
            if (own) stringResource(R.string.profile_people_title_own) else stringResource(R.string.profile_people_title, name)
        ProfileTab.FAVORITES ->
            if (own) stringResource(R.string.profile_favorites_title_own) else stringResource(R.string.profile_favorites_title, name)
        else -> stringResource(R.string.profile_compare_title, name)
    }
}

/**
 * People, Favourites and Compare open over the profile on the app's collapsing bar, the one the
 * Discover sections and the media grids use, with the page's rail pinned under it.
 */
@Composable
internal fun ProfilePage(
    uiState: ProfileUiState,
    listState: LazyListState,
    bottomPadding: Dp,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
    /** Zero in the wide pane, which already sits below the status bar. */
    topInset: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
    content: LazyListScope.() -> Unit
) {
    CollapsingTopBarScaffold(
        title = pageTitle(uiState),
        onBackClick = { onAction(ProfileAction.ClosePage) },
        modifier = modifier,
        scrollableState = listState,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
        topInset = topInset,
        belowBar = { ProfilePageRail(uiState = uiState, onFilterAction = onAction) }
    ) { topContentPadding ->
        ProfileRefreshList(uiState, listState, bottomPadding, onAction, topPadding = topContentPadding, content = content)
    }
}

/** A tab's or a page's list, which a pull refreshes the whole profile from. */
@Composable
internal fun ProfileRefreshList(
    uiState: ProfileUiState,
    listState: LazyListState,
    bottomPadding: Dp,
    onAction: (ProfileAction) -> Unit,
    topPadding: Dp = 0.dp,
    content: LazyListScope.() -> Unit
) {
    AppPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = rememberRateLimitedRefresh { onAction(ProfileAction.Refresh()) },
        modifier = Modifier.fillMaxSize(),
        topInset = topPadding
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)
        ) {
            content()
        }
    }
}

/** Clears the share button when one floats, otherwise the bottom navigation. */
@Composable
internal fun profileBottomPadding(showShareFab: Boolean): Dp =
    if (showShareFab) shareFabClearance() else LocalMainNavBarInset.current + 48.dp

/** A page's own rail: which favourites, which people, or which list is compared. */
@Composable
private fun ProfilePageRail(uiState: ProfileUiState, onFilterAction: (ProfileAction) -> Unit) {
    when (uiState.selectedTab) {
        ProfileTab.FAVORITES -> {
            val profile = uiState.profile
            val own = uiState.viewerOwnsProfile
            FavoritesFilterRail(
                selected = uiState.selectedFavoritesFilter,
                count = when (uiState.selectedFavoritesFilter) {
                    ProfileFavoritesFilter.ANIME -> profile?.favoriteAnime?.size
                    ProfileFavoritesFilter.MANGA -> profile?.favoriteMangaOverview?.size
                    ProfileFavoritesFilter.CHARACTERS -> profile?.favoriteCharactersOverview?.size
                    ProfileFavoritesFilter.STAFF -> profile?.favoriteStaffOverview?.size
                    ProfileFavoritesFilter.STUDIOS -> profile?.favoriteStudiosOverview?.size
                } ?: 0,
                ownerName = if (own) null else profile?.name,
                onSelect = { onFilterAction(ProfileAction.SelectFavoritesFilter(it)) }
            )
        }
        ProfileTab.SOCIAL -> PeopleSwitcher(
            uiState = uiState,
            onSelect = { onFilterAction(ProfileAction.SelectSocialTab(it)) },
            onOrderSelected = { onFilterAction(ProfileAction.SelectPeopleOrder(it)) }
        )
        ProfileTab.COMPARE -> MediaTypeToggle(
            selected = uiState.compareType,
            onSelect = { onFilterAction(ProfileAction.SelectCompareType(it)) },
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        )
        else -> Unit
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
    modifier: Modifier = Modifier,
    onListQueryChange: (String) -> Unit = { onFilterAction(ProfileAction.SetListQuery(it)) },
    /** The phone draws the list's search in the docked bar instead, above this strip. */
    showListSearch: Boolean = true,
    rails: ProfileRails? = null,
    /** T2 draws the list's rail over the rows instead, beside the filter panel. */
    showListRail: Boolean = true
) {
    val railScrolls = rails ?: remember { ProfileRails() }
    Column(modifier = modifier) {
        val onLists = uiState.selectedTab.isList
        if (onLists && showListSearch) {
            ProfileListSearchBar(
                uiState = uiState,
                onQueryChange = onListQueryChange,
                onFilterAction = onFilterAction,
                onBack = null
            )
        }
        ProfileTabsButtonGroup(
            selectedTab = uiState.selectedTab,
            lastListTab = uiState.lastListTab,
            onTabSelected = onTabSelected
        )
        when (uiState.selectedTab) {
            ProfileTab.STATS -> {
                val own = uiState.viewerOwnsProfile
                StatsRail(
                    statsType = uiState.selectedStatsType,
                    views = if (own) StatsView.entries else StatsView.entries.filter { it != StatsView.RECAPS },
                    selectedView = uiState.selectedStatsView,
                    onTypeSelected = onStatsTypeSelected,
                    onViewSelected = onStatsViewSelected,
                    isOwnProfile = own,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    scroll = railScrolls.scroll("stats")
                )
            }
            ProfileTab.ACTIVITY -> ActivityFilterRail(uiState.selectedActivityFilter, railScrolls.scroll("activity")) {
                onFilterAction(ProfileAction.SelectActivityFilter(it))
            }
            ProfileTab.ANIME, ProfileTab.MANGA -> if (showListRail) {
                ProfileListRail(
                    uiState = uiState,
                    onFilterAction = onFilterAction,
                    onTabSelected = onTabSelected,
                    rails = railScrolls
                )
            }
            else -> Unit
        }
    }
}

/** The list's status chips, with the anime and manga toggle pinned at their start. */
@Composable
internal fun ProfileListRail(
    uiState: ProfileUiState,
    onFilterAction: (ProfileAction) -> Unit,
    onTabSelected: (ProfileTab) -> Unit,
    rails: ProfileRails
) {
    val manga = uiState.selectedTab == ProfileTab.MANGA
    val searching = uiState.listTools.query.isNotBlank()
    MediaStatusRail(
        statusCounts = if (manga) uiState.mangaStatusCounts else uiState.animeStatusCounts,
        selectedStatus = if (searching) uiState.listTools.searchStatus else uiState.selectedListStatus,
        mediaType = if (manga) MediaType.MANGA else MediaType.ANIME,
        onStatusSelected = {
            onFilterAction(
                when {
                    searching -> ProfileAction.SetListSearchStatus(it)
                    manga -> ProfileAction.SelectMangaStatus(it)
                    else -> ProfileAction.SelectAnimeStatus(it)
                }
            )
        },
        onMediaTypeSelected = { onTabSelected(if (it == MediaType.MANGA) ProfileTab.MANGA else ProfileTab.ANIME) },
        searchCounts = if (searching) uiState.listResults.searchCounts.orEmpty() else null,
        scroll = rails.scroll(if (manga) "lists_manga" else "lists_anime")
    )
}

/** The list's search, sort, filters and view, which docks above the tabs on Lists. */
@Composable
internal fun ProfileListSearchBar(
    uiState: ProfileUiState,
    onQueryChange: (String) -> Unit,
    onFilterAction: (ProfileAction) -> Unit,
    onBack: (() -> Unit)?
) {
    val manga = uiState.selectedTab == ProfileTab.MANGA
    val own = uiState.viewerOwnsProfile
    val name = uiState.profile?.name.orEmpty()
    ProfileListTopBar(
        query = uiState.listTools.query,
        onQueryChange = onQueryChange,
        placeholder = when {
            own && manga -> stringResource(R.string.profile_list_search_own_manga)
            own -> stringResource(R.string.profile_list_search_own_anime)
            manga -> stringResource(R.string.profile_list_search_manga, name)
            else -> stringResource(R.string.profile_list_search_anime, name)
        },
        view = uiState.listTools.view,
        onToggleView = { onFilterAction(ProfileAction.ToggleListView) },
        onOpenSort = { onFilterAction(ProfileAction.OpenListSheet(ProfileListSheet.SORT)) },
        filtersActive = uiState.listTools.filters.isActive,
        onOpenFilters = { onFilterAction(ProfileAction.OpenListSheet(ProfileListSheet.FILTERS)) },
        onBack = onBack
    )
}

/** The bar the strip docks under, the identity bar or the list's search. */
private val ProfileDockBarHeight = 64.dp

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
                tab == StripTab.STATS && WhatsNewItem.PROFILE_STATS_MOVED in whatsNew -> ({ WhatsNewDot() })
                tab == StripTab.LISTS && WhatsNewItem.PROFILE_LISTS in whatsNew -> ({ WhatsNewDot() })
                tab == StripTab.ACTIVITY && WhatsNewItem.PROFILE_POSTS_MOVED in whatsNew -> ({ WhatsNewDot() })
                else -> null
            }
        }
    )
}
