package com.anisync.android.presentation.discover

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.anisync.android.presentation.navigation.CharacterDetails
import com.anisync.android.presentation.navigation.LIST_DETAIL_PANE_SOURCE
import com.anisync.android.presentation.navigation.MediaDetails
import com.anisync.android.presentation.navigation.PaneDetailHost
import com.anisync.android.presentation.navigation.RecentReviews
import com.anisync.android.presentation.navigation.ReviewDetail
import com.anisync.android.presentation.navigation.SectionGrid
import com.anisync.android.presentation.navigation.StaffDetails
import com.anisync.android.presentation.navigation.StudioDetails
import com.anisync.android.presentation.navigation.TwoPaneListDetailScaffold
import com.anisync.android.presentation.navigation.UserProfile
import com.anisync.android.presentation.navigation.navigateSafely
import com.anisync.android.presentation.review.ReviewDetailScreen
import com.anisync.android.presentation.util.LocalAdaptiveInfo

/**
 * What a tapped Discover feed item opens in the wide detail pane. The feed carries two selectable
 * item kinds: media cards (all carousels) and recent-review cards. Per the Material 3 list-detail
 * guidance both open in the detail pane — a review is a list item like any other, not a
 * full-screen escalation.
 */
sealed interface DiscoverTarget {
    data class Media(val id: Int) : DiscoverTarget
    data class Review(val id: Int) : DiscoverTarget
}

private fun DiscoverTarget.toPaneRoute(): Any = when (this) {
    is DiscoverTarget.Media -> MediaDetails(id, LIST_DETAIL_PANE_SOURCE)
    is DiscoverTarget.Review -> ReviewDetail(id, sourceScreen = LIST_DETAIL_PANE_SOURCE)
}

/** Persists the open target across configuration changes (for the scaffold's selection). */
private val DiscoverTargetSaver = listSaver<DiscoverTarget?, Any>(
    save = { target ->
        when (target) {
            is DiscoverTarget.Media -> listOf("media", target.id)
            is DiscoverTarget.Review -> listOf("review", target.id)
            null -> emptyList()
        }
    },
    restore = { saved ->
        when (saved.getOrNull(0)) {
            "media" -> DiscoverTarget.Media(saved[1] as Int)
            "review" -> DiscoverTarget.Review(saved[1] as Int)
            else -> null
        }
    },
)

/**
 * The Discover tab. Compact/medium widths show the plain [DiscoverScreen] and push the full-screen
 * detail. Expanded widths use the shared two-pane [TwoPaneListDetailScaffold] — the discover feed as
 * the permanent list pane, the selected media's or review's detail in the on-demand resizable pane.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverListDetail(
    navController: NavHostController,
    // sourceSection = the TransitionKeys.DISCOVER_* prefix of the tapped section; becomes
    // MediaDetails.sourceScreen so the return morph targets the exact card tapped.
    onMediaClickFullScreen: (mediaId: Int, sourceSection: String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val feed: @Composable (
        onMediaClick: (Int, String) -> Unit,
        onReviewClick: (Int) -> Unit,
    ) -> Unit = { onMediaClick, onReviewClick ->
        DiscoverScreen(
            navController = navController,
            onMediaClick = onMediaClick,
            onCharacterClick = { navController.navigate(CharacterDetails(it)) },
            onStaffClick = { navController.navigate(StaffDetails(it)) },
            onStudioClick = { navController.navigate(StudioDetails(it)) },
            onUserClick = { navController.navigateSafely(UserProfile(it)) },
            onSectionSeeAllClick = { title, sectionType, mediaType ->
                navController.navigate(SectionGrid(title, sectionType, mediaType.name))
            },
            onReviewClick = onReviewClick,
            onRecentReviewsSeeAllClick = { mediaType ->
                navController.navigate(RecentReviews(mediaType.name))
            },
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }

    if (!LocalAdaptiveInfo.current.supportsTwoPane) {
        feed(
            onMediaClickFullScreen,
            { navController.navigate(ReviewDetail(it, sourceScreen = "discover")) },
        )
        return
    }

    // Discover section items don't show a selected state, so the pane's selected id is ignored here.
    // The pane detail uses its own LIST_DETAIL_PANE_SOURCE prefix (no cross-pane morph), so the
    // tapped section is dropped.
    TwoPaneListDetailScaffold(
        selectionSaver = DiscoverTargetSaver,
        listPane = { _, onItemClick ->
            feed(
                { mediaId, _ -> onItemClick(DiscoverTarget.Media(mediaId)) },
                { reviewId -> onItemClick(DiscoverTarget.Review(reviewId)) },
            )
        },
        detailPane = { target, onClose ->
            PaneDetailHost(
                startRoute = target.toPaneRoute(),
                navController = navController,
                onClose = onClose,
                extraGraph = { paneNav, sharedScope ->
                    // Reviews are a Discover list item, so their detail lives in the pane too. The
                    // review's media banner drills to the media WITHIN the pane; users escalate.
                    composable<ReviewDetail> { backStackEntry ->
                        val route: ReviewDetail = backStackEntry.toRoute()
                        ReviewDetailScreen(
                            reviewId = route.reviewId,
                            sourceScreen = route.sourceScreen,
                            onBackClick = { if (!paneNav.popBackStack()) onClose() },
                            onUserClick = { navController.navigateSafely(UserProfile(it)) },
                            onMediaClick = {
                                paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE))
                            },
                            sharedTransitionScope = sharedScope,
                            animatedVisibilityScope = this,
                        )
                    }
                },
            )
        },
    )
}
