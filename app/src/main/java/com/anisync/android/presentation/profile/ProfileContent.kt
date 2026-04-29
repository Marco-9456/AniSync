package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.details.components.ReviewDetailsSheet
import com.anisync.android.presentation.profile.components.MessageComposerSheet
import com.anisync.android.presentation.profile.components.ProfileBioSheet
import com.anisync.android.presentation.profile.components.ProfileTopSection
import com.anisync.android.presentation.profile.sections.ProfileOverviewSection
import com.anisync.android.presentation.profile.sections.profileActivityTab
import com.anisync.android.presentation.profile.sections.profileFavoritesTab
import com.anisync.android.presentation.profile.sections.profileMediaTab
import com.anisync.android.presentation.profile.sections.profileReviewsTab
import com.anisync.android.presentation.profile.sections.profileSocialTab
import com.anisync.android.presentation.profile.sections.profileStatsTab
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType
import com.anisync.android.util.ShareUtils

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
    isOwnProfile: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onAction: (ProfileAction) -> Unit,
    onSettingsClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit = { _, _ -> },
    onCommentClick: (threadId: Int, commentId: Int, threadTitle: String) -> Unit = { _, _, _ -> },
    onActivityClick: (Int) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { onAction(ProfileAction.Refresh) },
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize(),
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
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
                isFollowLoading = uiState.isFollowLoading,
                onFollowClick = { onAction(ProfileAction.ToggleFollow) },
                onMessageClick = { onAction(ProfileAction.ShowMessageComposer) },
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
                }
            )
        }

        stickyHeader {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileTabsButtonGroup(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { onAction(ProfileAction.SelectTab(it)) }
                )
            }
        }

        when (uiState.selectedTab) {
            ProfileTab.OVERVIEW -> {
                item(key = "tab_overview", contentType = "overview") {
                    ProfileOverviewSection(
                        profile = profile,
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
                        onLikeActivity = if (isOwnProfile) {
                            { onAction(ProfileAction.ToggleActivityLike(it)) }
                        } else null,
                        onDeleteActivity = if (isOwnProfile) {
                            { onAction(ProfileAction.DeleteActivity(it)) }
                        } else null,
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
                    onLikeActivity = if (isOwnProfile) {
                        { onAction(ProfileAction.ToggleActivityLike(it)) }
                    } else null,
                    onDeleteActivity = if (isOwnProfile) {
                        { onAction(ProfileAction.DeleteActivity(it)) }
                    } else null,
                    viewerId = uiState.viewerId
                )
            }

            ProfileTab.SOCIAL -> {
                profileSocialTab(
                    uiState = uiState,
                    onTabSelected = { onAction(ProfileAction.SelectSocialTab(it)) },
                    onUserClick = onUserClick,
                    onThreadClick = onThreadClick,
                    onCommentClick = onCommentClick,
                    onLoadMore = { onAction(ProfileAction.LoadMoreSocial) }
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
                    transitionPrefix = "profile_anime"
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
                    transitionPrefix = "profile_manga"
                )
            }

            ProfileTab.FAVORITES -> {
                profileFavoritesTab(
                    profile = profile,
                    selectedFilter = uiState.selectedFavoritesFilter,
                    onFilterSelected = { onAction(ProfileAction.SelectFavoritesFilter(it)) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onMediaClick = onMediaClick,
                    onCharacterClick = onCharacterClick,
                    onStaffClick = onStaffClick
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
                profileStatsTab(
                    uiState = uiState,
                    onStatsTypeSelected = { onAction(ProfileAction.SelectStatsType(it)) }
                )
            }
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

    uiState.selectedReview?.let { review ->
        ReviewDetailsSheet(
            review = review,
            onUserClick = onUserClick,
            onDismiss = { onAction(ProfileAction.SelectReview(null)) }
        )
    }

    if (uiState.isMessageComposerVisible) {
        MessageComposerSheet(
            recipientName = profile.name,
            isSending = uiState.isSendingMessage,
            errorMessage = uiState.messageSendError,
            onDismissRequest = { onAction(ProfileAction.HideMessageComposer) },
            onSend = { text, isPrivate ->
                onAction(ProfileAction.SendMessage(text, isPrivate))
            }
        )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileTabsButtonGroup(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
            ButtonGroupDefaults.ConnectedSpaceBetween
        )
    ) {
        ProfileTab.entries.forEachIndexed { index, tab ->
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                ProfileTab.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            ToggleButton(
                checked = selectedTab == tab,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(tab)
                },
                shapes = shapes
            ) {
                Icon(
                    imageVector = profileTabIcon(tab),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(tab.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
