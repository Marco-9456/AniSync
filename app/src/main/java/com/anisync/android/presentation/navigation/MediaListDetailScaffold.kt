package com.anisync.android.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anisync.android.presentation.details.MediaDetailsScreen

private const val PANE_SOURCE = "list_detail_pane"

// Width split between the two panes while the detail is open, as the list pane's fraction. The list
// stays the smaller "index" pane; the detail gets the majority of the space. User-resizable within
// [MIN_LIST_FRACTION]..[MAX_LIST_FRACTION] via the drag handle.
private const val DEFAULT_LIST_FRACTION = 0.4f
private const val MIN_LIST_FRACTION = 0.28f
private const val MAX_LIST_FRACTION = 0.6f

/**
 * Reusable Material 3 two-pane container for media feeds (Library, and later Discover / Feed /
 * Forum). The caller supplies its list/feed via [listPane], receiving an `onMediaClick`.
 *
 * Behaviour (matching the M3 panes guidance):
 *  - The **list pane is permanent** and fills the full width while nothing is selected.
 *  - The **detail pane is temporary / on demand**: it opens when an item is tapped and is dismissed
 *    with the detail's close affordance or the system back gesture.
 *  - Both panes are **flexible** (weight-based) and **user-resizable** via a [VerticalDragHandle];
 *    the chosen split is remembered across configuration changes.
 *
 * The detail pane hosts its own NavHost + [SharedTransitionLayout] so each [MediaDetailsScreen] gets
 * a route-scoped ViewModel (its id is read from the route's SavedStateHandle). Media→media
 * (relations) navigates inside the pane; everything else escalates to the app [navController].
 *
 * Intended to be rendered only on expanded widths; compact/medium use the plain full-screen screen.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaListDetailScaffold(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    listPane: @Composable (onMediaClick: (Int) -> Unit) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    val detailId = selectedId

    BackHandler(enabled = detailId != null) { selectedId = null }

    var listFraction by rememberSaveable { mutableStateOf(DEFAULT_LIST_FRACTION) }
    var rowWidthPx by remember { mutableIntStateOf(0) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { rowWidthPx = it.width },
    ) {
        // List pane — fills the full width on its own, shrinks to [listFraction] when the detail opens.
        Box(
            modifier = Modifier
                .weight(if (detailId != null) listFraction else 1f)
                .fillMaxHeight()
                .clipToBounds(),
        ) {
            listPane { id -> selectedId = id }
        }

        if (detailId != null) {
            // Drag handle to resize the two panes.
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                VerticalDragHandle(
                    modifier = Modifier
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                if (rowWidthPx > 0) {
                                    listFraction = (listFraction + delta / rowWidthPx)
                                        .coerceIn(MIN_LIST_FRACTION, MAX_LIST_FRACTION)
                                }
                            },
                        )
                        // Keep the drag from colliding with the system back gesture at the edge.
                        .systemGestureExclusion(),
                )
            }

            // Detail pane — temporary, opened on demand.
            Box(
                modifier = Modifier
                    .weight(1f - listFraction)
                    .fillMaxHeight()
                    .clipToBounds(),
            ) {
                MediaDetailPane(
                    mediaId = detailId,
                    navController = navController,
                    onClose = { selectedId = null },
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MediaDetailPane(
    mediaId: Int,
    navController: NavHostController,
    onClose: () -> Unit,
) {
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val paneNav = rememberNavController()

        // Skip the first id (the NavHost startDestination already shows it with a correctly-scoped
        // ViewModel). On later selections, navigate with a cleared stack so a FRESH entry + ViewModel
        // is created — reusing the entry would keep the ViewModel bound to the previous id.
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
                    // Back at the root closes the pane; deeper (relations) pops within the pane.
                    onBackClick = { if (!paneNav.popBackStack()) onClose() },
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
