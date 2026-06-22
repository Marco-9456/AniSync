package com.anisync.android.presentation.discover

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.anisync.android.presentation.navigation.CharacterDetails
import com.anisync.android.presentation.navigation.MediaListDetailScaffold
import com.anisync.android.presentation.navigation.RecentReviews
import com.anisync.android.presentation.navigation.ReviewDetail
import com.anisync.android.presentation.navigation.SectionGrid
import com.anisync.android.presentation.navigation.StaffDetails
import com.anisync.android.presentation.navigation.StudioDetails
import com.anisync.android.presentation.navigation.UserProfile
import com.anisync.android.presentation.navigation.navigateSafely
import com.anisync.android.presentation.util.LocalAdaptiveInfo

/**
 * The Discover tab. Compact/medium widths show the plain [DiscoverScreen] and push the full-screen
 * detail. Expanded widths use the shared two-pane [MediaListDetailScaffold] — the discover feed as
 * the permanent list pane, the selected media's detail in the on-demand resizable pane.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverListDetail(
    navController: NavHostController,
    onMediaClickFullScreen: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val feed: @Composable (onMediaClick: (Int) -> Unit) -> Unit = { onMediaClick ->
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
            onReviewClick = { navController.navigate(ReviewDetail(it)) },
            onRecentReviewsSeeAllClick = { mediaType ->
                navController.navigate(RecentReviews(mediaType.name))
            },
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }

    if (!LocalAdaptiveInfo.current.supportsTwoPane) {
        feed(onMediaClickFullScreen)
        return
    }

    // Discover section items don't show a selected state, so the pane's selected id is ignored here.
    MediaListDetailScaffold(navController = navController) { _, onMediaClick ->
        feed(onMediaClick)
    }
}
