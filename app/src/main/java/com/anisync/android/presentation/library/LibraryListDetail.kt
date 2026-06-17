package com.anisync.android.presentation.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anisync.android.R
import com.anisync.android.presentation.details.MediaDetailsScreen
import com.anisync.android.presentation.navigation.CharacterDetails
import com.anisync.android.presentation.navigation.CreateThread
import com.anisync.android.presentation.navigation.ForumMediaThreads
import com.anisync.android.presentation.navigation.ForumThreadDetail
import com.anisync.android.presentation.navigation.MediaCharactersGrid
import com.anisync.android.presentation.navigation.MediaDetails
import com.anisync.android.presentation.navigation.MediaRecommendationsGrid
import com.anisync.android.presentation.navigation.MediaRelationsGrid
import com.anisync.android.presentation.navigation.MediaStaffGrid
import com.anisync.android.presentation.navigation.StaffDetails
import com.anisync.android.presentation.navigation.StudioDetails
import com.anisync.android.presentation.navigation.UserProfile
import com.anisync.android.presentation.navigation.WriteReview
import com.anisync.android.presentation.navigation.navigateSafely
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import kotlinx.coroutines.launch

private const val PANE_SOURCE = "library_pane"

/**
 * The Library tab as a Material 3 **list-detail** canonical layout.
 *
 * Compact/medium: just [LibraryScreen]; tapping an item pushes the full-screen detail via
 * [onMediaClickFullScreen] (keeping the card→page shared-element morph).
 *
 * Expanded: a two-pane [ListDetailPaneScaffold] — the library list on the left, the selected media's
 * detail filling a real pane on the right (using the width, not a stretched/centered column). The
 * detail pane hosts its own tiny NavHost so each [MediaDetailsScreen] gets a correctly-scoped
 * ViewModel (its id comes from the route's SavedStateHandle). Media→media (relations) stays in the
 * pane; other destinations push full-screen via [navController].
 */
@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun LibraryListDetail(
    navController: NavHostController,
    onMediaClickFullScreen: (Int) -> Unit,
    onNavigateToCalendar: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    if (!LocalAdaptiveInfo.current.isExpandedOrWider) {
        LibraryScreen(
            onMediaClick = onMediaClickFullScreen,
            onNavigateToCalendar = onNavigateToCalendar,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
        return
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<Int>()
    val scope = rememberCoroutineScope()
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }

    BackHandler(navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                LibraryScreen(
                    onMediaClick = { id ->
                        selectedId = id
                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) }
                    },
                    onNavigateToCalendar = onNavigateToCalendar,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val id = selectedId
                if (id == null) {
                    DetailPanePlaceholder()
                } else {
                    DetailPane(
                        mediaId = id,
                        navController = navController,
                        onExhausted = {
                            selectedId = null
                            scope.launch { navigator.navigateBack() }
                        },
                    )
                }
            }
        },
    )
}

/**
 * Detail pane content: a self-contained NavHost (with its own SharedTransitionLayout) so each
 * [MediaDetailsScreen] gets a route-scoped ViewModel. [mediaId] selects what to show; relation taps
 * navigate within this pane, everything else escalates to the app's [navController].
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailPane(
    mediaId: Int,
    navController: NavHostController,
    onExhausted: () -> Unit,
) {
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val paneNav = rememberNavController()

        // Skip the first id (the NavHost startDestination already shows it with a correctly-scoped
        // ViewModel). On later list selections, navigate with a cleared stack so a FRESH entry +
        // ViewModel is created — launchSingleTop would reuse the entry whose ViewModel is bound to
        // the previous id (its mediaId comes from SavedStateHandle at creation).
        var isFirstSelection by remember { mutableStateOf(true) }
        LaunchedEffect(mediaId) {
            if (isFirstSelection) {
                isFirstSelection = false
            } else {
                paneNav.navigate(MediaDetails(mediaId, PANE_SOURCE)) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        NavHost(
            navController = paneNav,
            startDestination = MediaDetails(mediaId, PANE_SOURCE),
        ) {
            composable<MediaDetails> { backStackEntry ->
                val route: MediaDetails = backStackEntry.toRoute()
                MediaDetailsScreen(
                    mediaId = route.mediaId,
                    sourceScreen = route.sourceScreen,
                    onBackClick = { if (!paneNav.popBackStack()) onExhausted() },
                    onRelationClick = { relId -> paneNav.navigate(MediaDetails(relId, PANE_SOURCE)) },
                    onCharacterClick = { navController.navigate(CharacterDetails(it)) },
                    onStaffClick = { navController.navigate(StaffDetails(it)) },
                    onStudioClick = { navController.navigate(StudioDetails(it)) },
                    onCastSeeAllClick = { mId, t -> navController.navigate(MediaCharactersGrid(mId, t)) },
                    onStaffSeeAllClick = { mId, t -> navController.navigate(MediaStaffGrid(mId, t)) },
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
                    onUserClick = { navController.navigateSafely(UserProfile(it)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }
        }
    }
}

/** Empty-state shown in the detail pane until a library item is selected. */
@Composable
private fun DetailPanePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.TheaterComedy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.list_detail_select_prompt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
