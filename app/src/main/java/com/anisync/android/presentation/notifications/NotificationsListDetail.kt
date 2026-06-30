package com.anisync.android.presentation.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anisync.android.R
import com.anisync.android.presentation.activity.ActivityDetailScreen
import com.anisync.android.presentation.details.MediaDetailsScreen
import com.anisync.android.presentation.forum.ThreadDetailScreen
import com.anisync.android.presentation.navigation.ActivityDetail
import com.anisync.android.presentation.navigation.CharacterDetails
import com.anisync.android.presentation.navigation.CreateThread
import com.anisync.android.presentation.navigation.DetailPanePlaceholder
import com.anisync.android.presentation.navigation.EditActivity
import com.anisync.android.presentation.navigation.EditThreadBody
import com.anisync.android.presentation.navigation.ForumMediaThreads
import com.anisync.android.presentation.navigation.ForumThreadDetail
import com.anisync.android.presentation.navigation.MediaDetails
import com.anisync.android.presentation.navigation.MediaRecommendationsGrid
import com.anisync.android.presentation.navigation.MediaRelationsGrid
import com.anisync.android.presentation.navigation.StaffDetails
import com.anisync.android.presentation.navigation.StudioDetails
import com.anisync.android.presentation.navigation.TwoPaneListDetailScaffold
import com.anisync.android.presentation.navigation.UserProfile
import com.anisync.android.presentation.navigation.WriteReview
import com.anisync.android.presentation.profile.ProfileScreen
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalPaneIsRoot

private const val SOURCE = "notifications"

/**
 * The Notifications inbox. Compact/medium widths show the plain [NotificationsScreen] and push the
 * tapped notification's target full screen (unchanged behaviour). Expanded widths use the shared
 * two-pane [TwoPaneListDetailScaffold] — the inbox as the permanent list pane, the tapped
 * notification's target in the on-demand resizable detail pane (closable ✕).
 *
 * Unlike the four tab surfaces, a notification has no detail of its own: it deep-links to one of four
 * heterogeneous targets (media · activity · thread · user). The detail pane is therefore a
 * self-contained [NavHost] keyed by a [NotificationTarget] that can host any of those screens;
 * drilling deeper from a target escalates full screen to the app [navController].
 *
 * Notifications is a pushed route with no navigation rail, so the two-pane gutter is symmetric
 * (not the rail-flush default).
 */
@Composable
fun NotificationsListDetail(
    navController: NavHostController,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigateUser: (String) -> Unit = { username ->
        username.trim().takeIf { it.isNotEmpty() }?.let { navController.navigate(UserProfile(it)) }
    }

    if (!LocalAdaptiveInfo.current.supportsTwoPane) {
        NotificationsScreen(
            onBackClick = onBackClick,
            onMediaClick = { navController.navigate(MediaDetails(it, SOURCE)) },
            onUserClick = navigateUser,
            onActivityClick = { navController.navigate(ActivityDetail(it)) },
            onThreadClick = { threadId, commentId ->
                navController.navigate(ForumThreadDetail(threadId, "", commentId ?: 0))
            },
            onSettingsClick = onSettingsClick,
            modifier = modifier,
        )
        return
    }

    TwoPaneListDetailScaffold(
        modifier = modifier,
        selectionSaver = NotificationTargetSaver,
        gutterPadding = PaddingValues(16.dp),
        placeholderPane = {
            DetailPanePlaceholder(
                icon = Icons.Outlined.Notifications,
                text = stringResource(R.string.pane_placeholder_notification),
            )
        },
        listPane = { selectedTarget, onSelect ->
            NotificationsScreen(
                onBackClick = onBackClick,
                onMediaClick = { onSelect(NotificationTarget.Media(it)) },
                onUserClick = { onSelect(NotificationTarget.Profile(it)) },
                onActivityClick = { onSelect(NotificationTarget.Activity(it)) },
                onThreadClick = { threadId, commentId ->
                    onSelect(NotificationTarget.Thread(threadId, commentId))
                },
                onSettingsClick = onSettingsClick,
                selectedTarget = selectedTarget,
            )
        },
        detailPane = { target, onClose ->
            NotificationDetailPane(target = target, navController = navController, onClose = onClose)
        },
    )
}

/** The destination a tapped notification opens in the detail pane. */
sealed interface NotificationTarget {
    data class Media(val mediaId: Int) : NotificationTarget
    data class Activity(val activityId: Int) : NotificationTarget
    data class Thread(val threadId: Int, val commentId: Int?) : NotificationTarget
    data class Profile(val username: String) : NotificationTarget
}

private fun NotificationTarget.toPaneRoute(): Any = when (this) {
    is NotificationTarget.Media -> MediaDetails(mediaId, SOURCE)
    is NotificationTarget.Activity -> ActivityDetail(activityId)
    is NotificationTarget.Thread -> ForumThreadDetail(threadId, "", commentId ?: 0)
    is NotificationTarget.Profile -> UserProfile(username)
}

// Persists the open target across configuration changes. A -1 comment id encodes "no comment".
private val NotificationTargetSaver = listSaver<NotificationTarget?, Any>(
    save = { target ->
        when (target) {
            is NotificationTarget.Media -> listOf("media", target.mediaId)
            is NotificationTarget.Activity -> listOf("activity", target.activityId)
            is NotificationTarget.Thread -> listOf("thread", target.threadId, target.commentId ?: -1)
            is NotificationTarget.Profile -> listOf("profile", target.username)
            null -> emptyList()
        }
    },
    restore = { saved ->
        when (saved.getOrNull(0)) {
            "media" -> NotificationTarget.Media(saved[1] as Int)
            "activity" -> NotificationTarget.Activity(saved[1] as Int)
            "thread" -> NotificationTarget.Thread(saved[1] as Int, (saved[2] as Int).takeIf { it >= 0 })
            "profile" -> NotificationTarget.Profile(saved[1] as String)
            else -> null
        }
    },
)

/**
 * Hosts the selected [target]'s screen inside the detail pane. A nested [NavHost] gives each target a
 * route-scoped entry (so Media/Profile get their id from a fresh SavedStateHandle); selecting another
 * notification navigates with a cleared back stack to force a fresh entry + ViewModel. System back
 * steps through the pane's own stack first, then closes the pane.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NotificationDetailPane(
    target: NotificationTarget,
    navController: NavHostController,
    onClose: () -> Unit,
) {
    val navigateUser: (String) -> Unit = { username ->
        username.trim().takeIf { it.isNotEmpty() }?.let { navController.navigate(UserProfile(it)) }
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val paneNav = rememberNavController()

        BackHandler(enabled = true) { if (!paneNav.popBackStack()) onClose() }

        var isFirstSelection by remember { mutableStateOf(true) }
        LaunchedEffect(target) {
            if (isFirstSelection) {
                isFirstSelection = false
            } else {
                paneNav.navigate(target.toPaneRoute()) { popUpTo(0) { inclusive = true } }
            }
        }

        // A notification target never drills WITHIN the pane (onward taps escalate full-screen), so the
        // detail is always the root → trailing close (✕) via LocalPaneIsRoot.
        CompositionLocalProvider(LocalPaneIsRoot provides true) {
        NavHost(
            navController = paneNav,
            startDestination = remember { target.toPaneRoute() },
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeIn()
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeOut()
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeIn()
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeOut()
            },
        ) {
            composable<MediaDetails> { backStackEntry ->
                val route: MediaDetails = backStackEntry.toRoute()
                MediaDetailsScreen(
                    mediaId = route.mediaId,
                    sourceScreen = route.sourceScreen,
                    onBackClick = { if (!paneNav.popBackStack()) onClose() },                    onRelationClick = { navController.navigate(MediaDetails(it, SOURCE)) },
                    onCharacterClick = { navController.navigate(CharacterDetails(it)) },
                    onStaffClick = { navController.navigate(StaffDetails(it)) },
                    onStudioClick = { navController.navigate(StudioDetails(it)) },
                    onRelatedSeeAllClick = { mId, t -> navController.navigate(MediaRelationsGrid(mId, t)) },
                    onRecommendationsSeeAllClick = { mId, t ->
                        navController.navigate(MediaRecommendationsGrid(mId, t))
                    },
                    onWriteReviewClick = { mId, t -> navController.navigate(WriteReview(mId, t)) },
                    onDiscussionClick = { tId, tt -> navController.navigate(ForumThreadDetail(tId, tt)) },
                    onViewAllDiscussions = { mId, t -> navController.navigate(ForumMediaThreads(mId, t)) },
                    onStartDiscussion = { mId, t, cover ->
                        navController.navigate(CreateThread(mId, t, cover.orEmpty()))
                    },
                    onUserClick = navigateUser,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<ActivityDetail> { backStackEntry ->
                val route: ActivityDetail = backStackEntry.toRoute()
                ActivityDetailScreen(
                    activityId = route.activityId,
                    onBackClick = { if (!paneNav.popBackStack()) onClose() },
                    onUserClick = navigateUser,
                    targetReplyId = route.targetReplyId.takeIf { it != 0 },                    onEditActivity = { navController.navigate(EditActivity(it)) },
                )
            }

            composable<ForumThreadDetail> { backStackEntry ->
                val route: ForumThreadDetail = backStackEntry.toRoute()
                ThreadDetailScreen(
                    threadId = route.threadId,
                    threadTitle = route.threadTitle,
                    onBackClick = { if (!paneNav.popBackStack()) onClose() },
                    onUserClick = navigateUser,
                    targetCommentId = route.commentId.takeIf { it != 0 },                    onEditThread = { navController.navigate(EditThreadBody(it)) },
                    capContentWidth = false,
                )
            }

            composable<UserProfile> { backStackEntry ->
                // ProfileScreen has no built-in back affordance for other users (full screen relies on
                // the system back gesture), so overlay a pane-level Close to match the other targets.
                val avScope = this
                Box(modifier = Modifier.fillMaxSize()) {
                    ProfileScreen(
                        onMediaClick = { navController.navigate(MediaDetails(it, SOURCE)) },
                        onCharacterClick = { navController.navigate(CharacterDetails(it)) },
                        onStaffClick = { navController.navigate(StaffDetails(it)) },
                        onVoiceActorClick = { navController.navigate(StaffDetails(it)) },
                        onStudioClick = { navController.navigate(StudioDetails(it)) },
                        onUserClick = navigateUser,
                        onThreadClick = { tId, tt -> navController.navigate(ForumThreadDetail(tId, tt)) },
                        onCommentClick = { tId, cId, tt ->
                            navController.navigate(ForumThreadDetail(tId, tt, cId))
                        },
                        onActivityClick = { navController.navigate(ActivityDetail(it)) },
                        onLastReplyClick = { aId, rId ->
                            navController.navigate(ActivityDetail(aId, rId))
                        },
                        onLogoutClick = { },
                        isOwnProfile = false,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = avScope,
                    )
                    FilledTonalIconButton(
                        onClick = { if (!paneNav.popBackStack()) onClose() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.pane_close),
                        )
                    }
                }
            }
        }
        }
    }
}
