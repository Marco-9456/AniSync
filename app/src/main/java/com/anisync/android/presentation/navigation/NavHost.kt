package com.anisync.android.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.anisync.android.presentation.details.CharacterDetailsScreen
import com.anisync.android.presentation.details.CharacterMediaGridScreen
import com.anisync.android.presentation.details.MediaCharactersGridScreen
import com.anisync.android.presentation.details.MediaDetailsScreen
import com.anisync.android.presentation.details.MediaRelationsGridScreen
import com.anisync.android.presentation.discover.DiscoverScreen
import com.anisync.android.presentation.discover.SectionGridScreen
import com.anisync.android.presentation.library.LibraryScreen
import com.anisync.android.presentation.login.LoginScreen
import com.anisync.android.presentation.profile.ProfileScreen
import com.anisync.android.presentation.settings.AboutScreen
import com.anisync.android.presentation.settings.AccountScreen
import com.anisync.android.presentation.settings.AcknowledgmentsScreen
import com.anisync.android.presentation.settings.LookAndFeelScreen
import com.anisync.android.presentation.settings.NotificationsScreen
import com.anisync.android.presentation.settings.OpenSourceLicensesScreen
import com.anisync.android.presentation.settings.SettingsScreen
import com.anisync.android.presentation.settings.StorageScreen
import com.anisync.android.presentation.statistics.StatisticsScreen

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

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AniSyncNavHost(
    navController: NavHostController,
    onMediaClick: (mediaId: Int, sourceScreen: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // =============================================================================
    // MATERIAL 3 MOTION SPECS (Memoized)
    // =============================================================================
    val motionScheme = MaterialTheme.motionScheme
    
    // Memoize specs to avoid recreation on each recomposition
    val spatialSpec = remember(motionScheme) { motionScheme.defaultSpatialSpec<IntOffset>() }
    val scaleSpec = remember(motionScheme) { motionScheme.defaultSpatialSpec<Float>() }
    val effectsSpec = remember(motionScheme) { motionScheme.defaultEffectsSpec<Float>() }

    // Constants for transitions
    val sharedAxisOffsetFraction = 0.20f
    val sharedAxisZInitialScale = 0.96f
    val sharedAxisZTargetScale = 1.02f

    // =============================================================================
    // TRANSITION HELPERS
    // =============================================================================
    
    // Using local functions to capture the theme specs without passing them around
    
    fun sharedAxisXEnter(forward: Boolean): EnterTransition {
        val offsetMultiplier = if (forward) 1 else -1
        return slideInHorizontally(
            animationSpec = spatialSpec,
            initialOffsetX = { (it * sharedAxisOffsetFraction * offsetMultiplier).toInt() }
        ) + fadeIn(
            animationSpec = effectsSpec
        )
    }

    fun sharedAxisXExit(forward: Boolean): ExitTransition {
        val offsetMultiplier = if (forward) -1 else 1
        return slideOutHorizontally(
            animationSpec = spatialSpec,
            targetOffsetX = { (it * sharedAxisOffsetFraction * offsetMultiplier).toInt() }
        ) + fadeOut(
            animationSpec = effectsSpec
        )
    }

    fun sharedAxisZEnter(): EnterTransition {
        return scaleIn(
            animationSpec = scaleSpec,
            initialScale = sharedAxisZInitialScale
        ) + fadeIn(
            animationSpec = effectsSpec
        )
    }

    fun sharedAxisZExit(): ExitTransition {
        return scaleOut(
            animationSpec = scaleSpec,
            targetScale = sharedAxisZTargetScale
        ) + fadeOut(
            animationSpec = effectsSpec
        )
    }

    fun sharedAxisZPopEnter(): EnterTransition {
        return scaleIn(
            animationSpec = scaleSpec,
            initialScale = sharedAxisZTargetScale
        ) + fadeIn(
            animationSpec = effectsSpec
        )
    }

    fun sharedAxisZPopExit(): ExitTransition {
        return scaleOut(
            animationSpec = scaleSpec,
            targetScale = sharedAxisZInitialScale
        ) + fadeOut(
            animationSpec = effectsSpec
        )
    }

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
                val onStatistics = remember(navController) {
                    { userId: Int -> navController.navigate(Statistics(userId)) }
                }
                val onNavigateToSettings = remember(navController) {
                    { navController.navigate(Settings) }
                }

                ProfileScreen(
                    onMediaClick = onProfileMediaClick,
                    onLogoutClick = onLogout,
                    onFavoritesClick = onFavorites,
                    onStatisticsClick = onStatistics,
                    onNavigateToSettings = onNavigateToSettings,
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
                    // Custom app scheme (for widgets, internal links)
                    navDeepLink<MediaDetails>(basePath = "anisync://details"),
                    // AniList anime URLs (e.g., https://anilist.co/anime/16498)
                    navDeepLink { uriPattern = "https://anilist.co/anime/{mediaId}" },
                    // AniList anime URLs with slug (e.g., https://anilist.co/anime/16498/attack-on-titan)
                    navDeepLink { uriPattern = "https://anilist.co/anime/{mediaId}/{slug}" },
                    // AniList manga URLs (e.g., https://anilist.co/manga/30002)
                    navDeepLink { uriPattern = "https://anilist.co/manga/{mediaId}" },
                    // AniList manga URLs with slug (e.g., https://anilist.co/manga/30002/berserk)
                    navDeepLink { uriPattern = "https://anilist.co/manga/{mediaId}/{slug}" }
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
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
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
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
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
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
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

            // =================================================================
            // STATISTICS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            composable<Statistics>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                StatisticsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // =================================================================
            // SETTINGS SCREENS - Shared Axis Z (Depth)
            // =================================================================

            // Settings Hub
            composable<Settings>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                SettingsScreen(
                    onNavigateToLookAndFeel = { navController.navigate(SettingsLookAndFeel) },
                    onNavigateToNotifications = { navController.navigate(SettingsNotifications) },
                    onNavigateToStorage = { navController.navigate(SettingsStorage) },
                    onNavigateToAccount = { navController.navigate(SettingsAccount) },
                    onNavigateToAbout = { navController.navigate(SettingsAbout) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Look and Feel Settings
            composable<SettingsLookAndFeel>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                LookAndFeelScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Notifications Settings
            composable<SettingsNotifications>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                NotificationsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Storage Settings
            composable<SettingsStorage>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                StorageScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Account Settings
            composable<SettingsAccount>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                AccountScreen(
                    onLogout = {
                        navController.navigate(Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // About Settings
            composable<SettingsAbout>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                AboutScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToOpenSourceLicenses = { navController.navigate(SettingsOpenSourceLicenses) },
                    onNavigateToAcknowledgments = { navController.navigate(SettingsAcknowledgments) }
                )
            }

            // Open Source Licenses
            composable<SettingsOpenSourceLicenses>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                OpenSourceLicensesScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Acknowledgments
            composable<SettingsAcknowledgments>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                AcknowledgmentsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}