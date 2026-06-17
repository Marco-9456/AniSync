package com.anisync.android.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.anisync.android.presentation.details.CharacterDetailsScreen
import com.anisync.android.presentation.details.CharacterMediaGridScreen
import com.anisync.android.presentation.details.MediaCharactersGridScreen
import com.anisync.android.presentation.details.MediaDetailsScreen
import com.anisync.android.presentation.details.MediaRecommendationsGridScreen
import com.anisync.android.presentation.details.MediaRelationsGridScreen
import com.anisync.android.presentation.details.MediaStaffGridScreen
import com.anisync.android.presentation.details.StaffDetailsScreen
import com.anisync.android.presentation.details.StaffMediaGridScreen
import com.anisync.android.presentation.details.StaffProductionMediaGridScreen
import com.anisync.android.presentation.details.StudioDetailsScreen
import com.anisync.android.presentation.details.StudioMediaGridScreen

private const val PANE_SOURCE = "list_detail_pane"

// Width split between the two panes while the detail is open, as the list pane's fraction. The list
// stays the smaller "index" pane; the detail gets the majority of the space. User-resizable within
// [MIN_LIST_FRACTION]..[MAX_LIST_FRACTION] via the drag handle.
private const val DEFAULT_LIST_FRACTION = 0.4f
private const val MIN_LIST_FRACTION = 0.28f
private const val MAX_LIST_FRACTION = 0.6f

/**
 * Reusable Material 3 two-pane container for any feed→detail surface (Library, Discover, Feed,
 * Forum). The caller supplies its list/feed via [listPane] (receiving an `onItemClick`) and the
 * detail content via [detailPane] (receiving the selected id and an `onClose`).
 *
 * Behaviour (matching the M3 panes guidance):
 *  - The **list pane is permanent** and fills the full width while nothing is selected.
 *  - The **detail pane is temporary / on demand**: it opens when an item is tapped and is dismissed
 *    with the detail's close affordance or the system back gesture.
 *  - Both panes are **flexible** (weight-based) and **user-resizable** via a [VerticalDragHandle];
 *    the chosen split is remembered across configuration changes.
 *
 * Intended to be rendered only on expanded widths; compact/medium use the plain full-screen screen.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TwoPaneListDetailScaffold(
    modifier: Modifier = Modifier,
    listPane: @Composable (onItemClick: (Int) -> Unit) -> Unit,
    detailPane: @Composable (id: Int, onClose: () -> Unit) -> Unit,
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
                detailPane(detailId) { selectedId = null }
            }
        }
    }
}

/**
 * Media specialization of [TwoPaneListDetailScaffold]: the detail pane is a [MediaDetailsScreen]
 * hosted in its own NavHost + [SharedTransitionLayout] so each gets a route-scoped ViewModel (its id
 * is read from the route's SavedStateHandle). Media→media (relations) navigates inside the pane;
 * everything else escalates to the app [navController]. Used by Library and Discover.
 */
@Composable
fun MediaListDetailScaffold(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    listPane: @Composable (onMediaClick: (Int) -> Unit) -> Unit,
) {
    TwoPaneListDetailScaffold(
        modifier = modifier,
        listPane = listPane,
        detailPane = { id, onClose ->
            MediaDetailPane(mediaId = id, navController = navController, onClose = onClose)
        },
    )
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

        // System back inside the pane steps back through the pane's OWN stack first, only closing
        // the pane once we're back at the root media. Registered here (inside the detail pane) so it
        // shadows the scaffold's close-the-pane BackHandler while a deeper destination is showing.
        BackHandler(enabled = true) {
            if (!paneNav.popBackStack()) onClose()
        }

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

        // The detail pane is a self-contained mini-app: media → character/staff/studio and every
        // "see all" grid navigate WITHIN the pane (paneNav), so only the list pane stays put. Cross-
        // feature destinations (forum, review, user profile, the review editor) still escalate to the
        // app [navController] and open full screen.
        NavHost(
            navController = paneNav,
            startDestination = MediaDetails(mediaId, PANE_SOURCE),
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
                    // Back at the root closes the pane; deeper destinations pop within the pane.
                    onBackClick = { if (!paneNav.popBackStack()) onClose() },
                    navigationIcon = Icons.Default.Close,
                    onRelationClick = { relId -> paneNav.navigate(MediaDetails(relId, PANE_SOURCE)) },
                    onCharacterClick = { paneNav.navigate(CharacterDetails(it)) },
                    onStaffClick = { paneNav.navigate(StaffDetails(it)) },
                    onStudioClick = { paneNav.navigate(StudioDetails(it)) },
                    onCastSeeAllClick = { mId, t -> paneNav.navigate(MediaCharactersGrid(mId, t)) },
                    onStaffSeeAllClick = { mId, t -> paneNav.navigate(MediaStaffGrid(mId, t)) },
                    onRelatedSeeAllClick = { mId, t -> paneNav.navigate(MediaRelationsGrid(mId, t)) },
                    onRecommendationsSeeAllClick = { mId, t ->
                        paneNav.navigate(MediaRecommendationsGrid(mId, t))
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

            composable<CharacterDetails> { backStackEntry ->
                val route: CharacterDetails = backStackEntry.toRoute()
                CharacterDetailsScreen(
                    characterId = route.characterId,
                    onBackClick = { paneNav.popBackStack() },
                    onMediaClick = { paneNav.navigate(MediaDetails(it, PANE_SOURCE)) },
                    onMediaSeeAllClick = { cId, cName -> paneNav.navigate(CharacterMediaGrid(cId, cName)) },
                    onStaffClick = { paneNav.navigate(StaffDetails(it)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<StaffDetails> { backStackEntry ->
                val route: StaffDetails = backStackEntry.toRoute()
                StaffDetailsScreen(
                    staffId = route.staffId,
                    onBackClick = { paneNav.popBackStack() },
                    onMediaClick = { paneNav.navigate(MediaDetails(it, PANE_SOURCE)) },
                    onCharacterClick = { paneNav.navigate(CharacterDetails(it)) },
                    onMediaSeeAllClick = { sId, sName -> paneNav.navigate(StaffMediaGrid(sId, sName)) },
                    onProductionSeeAllClick = { sId, sName ->
                        paneNav.navigate(StaffProductionMediaGrid(sId, sName))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<StudioDetails> { backStackEntry ->
                val route: StudioDetails = backStackEntry.toRoute()
                StudioDetailsScreen(
                    studioId = route.studioId,
                    onBackClick = { paneNav.popBackStack() },
                    onMediaClick = { paneNav.navigate(MediaDetails(it, PANE_SOURCE)) },
                    onMediaSeeAllClick = { sId, sName -> paneNav.navigate(StudioMediaGrid(sId, sName)) },
                )
            }

            composable<MediaCharactersGrid> { backStackEntry ->
                val route: MediaCharactersGrid = backStackEntry.toRoute()
                MediaCharactersGridScreen(
                    mediaId = route.mediaId,
                    mediaTitle = route.mediaTitle,
                    onBackClick = { paneNav.popBackStack() },
                    onCharacterClick = { paneNav.navigate(CharacterDetails(it)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<MediaStaffGrid> { backStackEntry ->
                val route: MediaStaffGrid = backStackEntry.toRoute()
                MediaStaffGridScreen(
                    mediaId = route.mediaId,
                    mediaTitle = route.mediaTitle,
                    onBackClick = { paneNav.popBackStack() },
                    onStaffClick = { paneNav.navigate(StaffDetails(it)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<MediaRelationsGrid> { backStackEntry ->
                val route: MediaRelationsGrid = backStackEntry.toRoute()
                MediaRelationsGridScreen(
                    mediaId = route.mediaId,
                    mediaTitle = route.mediaTitle,
                    onBackClick = { paneNav.popBackStack() },
                    onRelationClick = { paneNav.navigate(MediaDetails(it, PANE_SOURCE)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<MediaRecommendationsGrid> { backStackEntry ->
                val route: MediaRecommendationsGrid = backStackEntry.toRoute()
                MediaRecommendationsGridScreen(
                    mediaId = route.mediaId,
                    mediaTitle = route.mediaTitle,
                    onBackClick = { paneNav.popBackStack() },
                    onRecommendationClick = { paneNav.navigate(MediaDetails(it, PANE_SOURCE)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<CharacterMediaGrid> { backStackEntry ->
                val route: CharacterMediaGrid = backStackEntry.toRoute()
                CharacterMediaGridScreen(
                    characterId = route.characterId,
                    characterName = route.characterName,
                    onBackClick = { paneNav.popBackStack() },
                    onMediaClick = { paneNav.navigate(MediaDetails(it, PANE_SOURCE)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<StaffMediaGrid> { backStackEntry ->
                val route: StaffMediaGrid = backStackEntry.toRoute()
                StaffMediaGridScreen(
                    staffId = route.staffId,
                    staffName = route.staffName,
                    onBackClick = { paneNav.popBackStack() },
                    onMediaClick = { paneNav.navigate(MediaDetails(it, PANE_SOURCE)) },
                    onCharacterClick = { paneNav.navigate(CharacterDetails(it)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<StaffProductionMediaGrid> { backStackEntry ->
                val route: StaffProductionMediaGrid = backStackEntry.toRoute()
                StaffProductionMediaGridScreen(
                    staffId = route.staffId,
                    staffName = route.staffName,
                    onBackClick = { paneNav.popBackStack() },
                    onMediaClick = { paneNav.navigate(MediaDetails(it, PANE_SOURCE)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable<StudioMediaGrid> { backStackEntry ->
                val route: StudioMediaGrid = backStackEntry.toRoute()
                StudioMediaGridScreen(
                    studioId = route.studioId,
                    studioName = route.studioName,
                    onBackClick = { paneNav.popBackStack() },
                    onMediaClick = { paneNav.navigate(MediaDetails(it, PANE_SOURCE)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }
        }
    }
}
