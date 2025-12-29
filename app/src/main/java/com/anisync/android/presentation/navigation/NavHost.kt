package com.anisync.android.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.anisync.android.presentation.details.MediaDetailsScreen
import com.anisync.android.presentation.details.MediaCharactersGridScreen
import com.anisync.android.presentation.details.MediaRelationsGridScreen
import com.anisync.android.presentation.details.CharacterMediaGridScreen
import com.anisync.android.presentation.details.CharacterDetailsScreen
import com.anisync.android.presentation.discover.DiscoverScreen
import com.anisync.android.presentation.discover.SectionGridScreen
import com.anisync.android.presentation.library.LibraryScreen
import com.anisync.android.presentation.login.LoginScreen
import com.anisync.android.presentation.profile.ProfileScreen

// =============================================================================
// MATERIAL 3 MOTION CONSTANTS
// =============================================================================
// Duration and easing values based on Material 3 Motion guidelines
// See: https://m3.material.io/styles/motion/easing-and-duration/applying-easing-and-duration

/** Standard duration for navigation transitions (300ms as per M3 guidelines) */
private const val TRANSITION_DURATION = 300

/** Slide offset fraction for Shared Axis X transitions (30% of screen width) */
private const val SHARED_AXIS_OFFSET_FRACTION = 0.30f

/** Scale factor for Shared Axis Z forward entry (slightly zoomed out) */
private const val SHARED_AXIS_Z_INITIAL_SCALE = 0.92f

/** Scale factor for Shared Axis Z forward exit (slightly zoomed in) */
private const val SHARED_AXIS_Z_TARGET_SCALE = 1.10f

// =============================================================================
// ANIMATION SPECS
// =============================================================================

private val SpatialSpec = tween<IntOffset>(
    durationMillis = TRANSITION_DURATION,
    easing = FastOutSlowInEasing
)

private val ScaleSpec = tween<Float>(
    durationMillis = TRANSITION_DURATION,
    easing = FastOutSlowInEasing
)

private val FadeSpec = tween<Float>(
    durationMillis = TRANSITION_DURATION,
    easing = LinearOutSlowInEasing
)

// =============================================================================
// SHARED AXIS X TRANSITIONS (Horizontal - for Tab Navigation)
// =============================================================================
// Material 3 Shared Axis X: Used for switching between peer destinations
// at the same hierarchy level (e.g., tabs in a bottom navigation bar).
// Combines a horizontal slide with a fade for smooth lateral movement.

/**
 * Shared Axis X enter transition - slides in from the specified direction with fade.
 * @param forward If true, slides in from the right (forward navigation).
 *                If false, slides in from the left (backward navigation).
 */
private fun sharedAxisXEnter(forward: Boolean): EnterTransition {
    val offsetMultiplier = if (forward) 1 else -1
    return slideInHorizontally(
        animationSpec = SpatialSpec,
        initialOffsetX = { (it * SHARED_AXIS_OFFSET_FRACTION * offsetMultiplier).toInt() }
    ) + fadeIn(
        animationSpec = FadeSpec
    )
}

/**
 * Shared Axis X exit transition - slides out in the specified direction with fade.
 * @param forward If true, slides out to the left (forward navigation).
 *                If false, slides out to the right (backward navigation).
 */
private fun sharedAxisXExit(forward: Boolean): ExitTransition {
    val offsetMultiplier = if (forward) -1 else 1
    return slideOutHorizontally(
        animationSpec = SpatialSpec,
        targetOffsetX = { (it * SHARED_AXIS_OFFSET_FRACTION * offsetMultiplier).toInt() }
    ) + fadeOut(
        animationSpec = FadeSpec
    )
}

// =============================================================================
// SHARED AXIS Z TRANSITIONS (Depth - for Detail/Hierarchy Navigation)
// =============================================================================
// Material 3 Shared Axis Z: Used for navigation between a parent and child,
// or when moving deeper into a hierarchy (e.g., list to detail screen).
// Combines a scale transformation with fade for depth perception.

/** Shared Axis Z forward enter - scales up from a smaller size with fade in */
private fun sharedAxisZEnter(): EnterTransition {
    return scaleIn(
        animationSpec = ScaleSpec,
        initialScale = SHARED_AXIS_Z_INITIAL_SCALE
    ) + fadeIn(
        animationSpec = FadeSpec
    )
}

/** Shared Axis Z forward exit - scales up slightly with fade out (behind the entering screen) */
private fun sharedAxisZExit(): ExitTransition {
    return scaleOut(
        animationSpec = ScaleSpec,
        targetScale = SHARED_AXIS_Z_TARGET_SCALE
    ) + fadeOut(
        animationSpec = FadeSpec
    )
}

/** Shared Axis Z pop enter - scales down from larger size with fade in (returning from detail) */
private fun sharedAxisZPopEnter(): EnterTransition {
    return scaleIn(
        animationSpec = ScaleSpec,
        initialScale = SHARED_AXIS_Z_TARGET_SCALE
    ) + fadeIn(
        animationSpec = FadeSpec
    )
}

/** Shared Axis Z pop exit - scales down with fade out (detail screen exiting) */
private fun sharedAxisZPopExit(): ExitTransition {
    return scaleOut(
        animationSpec = ScaleSpec,
        targetScale = SHARED_AXIS_Z_INITIAL_SCALE
    ) + fadeOut(
        animationSpec = FadeSpec
    )
}

// =============================================================================
// TAB ORDER HELPER
// =============================================================================
// Defines the order of tabs for determining slide direction during navigation.
// Lower order = further left in the navigation hierarchy.

private val tabOrder = mapOf(
    Library::class.qualifiedName to 0,
    Discover::class.qualifiedName to 1,
    Profile::class.qualifiedName to 2
)

/**
 * Determines slide direction for tab transitions based on relative position.
 * @return true if navigating forward (left to right), false if backward (right to left)
 */
private fun isForwardNavigation(fromRoute: String?, toRoute: String?): Boolean {
    val fromOrder = tabOrder[fromRoute] ?: 0
    val toOrder = tabOrder[toRoute] ?: 0
    return toOrder > fromOrder
}

// =============================================================================
// MAIN NAVIGATION HOST
// =============================================================================

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AniSyncNavHost(
    navController: NavHostController,
    onMediaClick: (mediaId: Int, sourceScreen: String) -> Unit,
    modifier: Modifier = Modifier
) {
    SharedTransitionLayout(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = Library,
            modifier = Modifier
        ) {
            // =================================================================
            // LOGIN SCREEN
            // =================================================================
            // Full slide transitions for authentication flow
            composable<Login>(
                enterTransition = { sharedAxisXEnter(forward = true) },
                exitTransition = { sharedAxisXExit(forward = true) },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                LoginScreen()
            }

            // =================================================================
            // MAIN TABS - Shared Axis X (Horizontal)
            // =================================================================
            // Tab navigation uses directional awareness for natural feel

            composable<Library>(
                enterTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = initialState.destination.route,
                        toRoute = Library::class.qualifiedName
                    )
                    sharedAxisXEnter(forward = !forward)
                },
                exitTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = Library::class.qualifiedName,
                        toRoute = targetState.destination.route
                    )
                    sharedAxisXExit(forward = forward)
                },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                // Optimization: Memoize the callback to prevent unnecessary recompositions of LibraryScreen
                val onLibraryMediaClick = remember(onMediaClick) { 
                    { mediaId: Int -> onMediaClick(mediaId, "library") } 
                }
                
                LibraryScreen(
                    onMediaClick = onLibraryMediaClick,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Discover>(
                enterTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = initialState.destination.route,
                        toRoute = Discover::class.qualifiedName
                    )
                    sharedAxisXEnter(forward = forward)
                },
                exitTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = Discover::class.qualifiedName,
                        toRoute = targetState.destination.route
                    )
                    sharedAxisXExit(forward = forward)
                },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                // Optimization: Memoize callbacks
                val onDiscoverMediaClick = remember(onMediaClick) {
                    { mediaId: Int -> onMediaClick(mediaId, "discover") }
                }
                val onSectionClick = remember(navController) {
                    { title: String, sectionType: String, mediaType: com.anisync.android.type.MediaType ->
                        navController.navigate(SectionGrid(title, sectionType, mediaType.name))
                    }
                }

                DiscoverScreen(
                    onMediaClick = onDiscoverMediaClick,
                    onSectionSeeAllClick = onSectionClick,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Profile>(
                enterTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = initialState.destination.route,
                        toRoute = Profile::class.qualifiedName
                    )
                    sharedAxisXEnter(forward = forward)
                },
                exitTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = Profile::class.qualifiedName,
                        toRoute = targetState.destination.route
                    )
                    sharedAxisXExit(forward = forward)
                },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                // Optimization: Memoize callbacks
                val onProfileMediaClick = remember(onMediaClick) { 
                    { mediaId: Int -> onMediaClick(mediaId, "profile") } 
                }
                val onLogout = remember(navController) {
                    {
                        navController.navigate(Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                val onFavorites = remember(navController) {
                    { navController.navigate(SectionGrid("Favorites", "favorites")) }
                }

                ProfileScreen(
                    onMediaClick = onProfileMediaClick,
                    onLogoutClick = onLogout,
                    onFavoritesClick = onFavorites,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // DETAILS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            // Navigating to detail view uses scale+fade for depth perception
            // =================================================================
            // MEDIA DETAILS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            // Navigating to detail view uses scale+fade for depth perception
            composable<MediaDetails>(
                deepLinks = listOf(
                    navDeepLink<MediaDetails>(basePath = "anisync://details")
                ),
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val details: MediaDetails = backStackEntry.toRoute()

                MediaDetailsScreen(
                    mediaId = details.mediaId,
                    sourceScreen = details.sourceScreen,
                    onBackClick = { navController.popBackStack() },
                    onRelationClick = { relationMediaId ->
                        navController.navigate(MediaDetails(relationMediaId, "media_details"))
                    },
                    onCharacterClick = { characterId ->
                        navController.navigate(CharacterDetails(characterId))
                    },
                    onCastSeeAllClick = { mediaId, mediaTitle ->
                        navController.navigate(MediaCharactersGrid(mediaId, mediaTitle))
                    },
                    onRelatedSeeAllClick = { mediaId, mediaTitle ->
                        navController.navigate(MediaRelationsGrid(mediaId, mediaTitle))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // CHARACTER DETAILS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            composable<CharacterDetails>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val character: CharacterDetails = backStackEntry.toRoute()
                CharacterDetailsScreen(
                    characterId = character.characterId,
                    onBackClick = { navController.popBackStack() },
                    onMediaSeeAllClick = { characterId, characterName ->
                        navController.navigate(CharacterMediaGrid(characterId, characterName))
                    }
                )
            }

            // =================================================================
            // SECTION GRID SCREEN - Shared Axis Z (Depth)
            // =================================================================
            // Grid view for "See All" uses same depth pattern as Details
            composable<SectionGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val sectionGrid: SectionGrid = backStackEntry.toRoute()

                if (sectionGrid.sectionType == "favorites") {
                    com.anisync.android.presentation.discover.FavoritesGridScreen(
                        sectionTitle = sectionGrid.sectionTitle,
                        onBackClick = { navController.popBackStack() },
                        onMediaClick = { mediaId ->
                            navController.navigate(MediaDetails(mediaId, "sectiongrid"))
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                } else {
                    SectionGridScreen(
                        sectionTitle = sectionGrid.sectionTitle,
                        sectionType = sectionGrid.sectionType,
                        onBackClick = { navController.popBackStack() },
                        onMediaClick = { mediaId ->
                            navController.navigate(MediaDetails(mediaId, "sectiongrid"))
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
            }

            // =================================================================
            // MEDIA CHARACTERS GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<MediaCharactersGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: MediaCharactersGrid = backStackEntry.toRoute()
                MediaCharactersGridScreen(
                    mediaId = grid.mediaId,
                    mediaTitle = grid.mediaTitle,
                    onBackClick = { navController.popBackStack() },
                    onCharacterClick = { characterId ->
                        navController.navigate(CharacterDetails(characterId))
                    }
                )
            }

            // =================================================================
            // MEDIA RELATIONS GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<MediaRelationsGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: MediaRelationsGrid = backStackEntry.toRoute()
                MediaRelationsGridScreen(
                    mediaId = grid.mediaId,
                    mediaTitle = grid.mediaTitle,
                    onBackClick = { navController.popBackStack() },
                    onRelationClick = { relationMediaId ->
                        navController.navigate(MediaDetails(relationMediaId, "relations_grid"))
                    }
                )
            }

            // =================================================================
            // CHARACTER MEDIA GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<CharacterMediaGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: CharacterMediaGrid = backStackEntry.toRoute()
                CharacterMediaGridScreen(
                    characterId = grid.characterId,
                    characterName = grid.characterName,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}