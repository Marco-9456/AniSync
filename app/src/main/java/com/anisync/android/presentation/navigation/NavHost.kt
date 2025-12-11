package com.anisync.android.presentation.navigation

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.anisync.android.presentation.details.DetailsScreen
import com.anisync.android.presentation.discover.DiscoverScreen
import com.anisync.android.presentation.library.LibraryScreen
import com.anisync.android.presentation.login.LoginScreen
import com.anisync.android.presentation.profile.ProfileScreen

// Lateral slide offset for tab transitions (10% of width)
private const val TabSlideOffset = 10

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AniSyncNavHost(
    navController: NavHostController,
    onMediaClick: (mediaId: Int, sourceScreen: String) -> Unit,
    modifier: Modifier = Modifier
) {
    SharedTransitionLayout(modifier = modifier) {
        // We access the motion scheme here to ensure all transitions use the same physics
        val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

        // For Scale transitions (Float)
        val scaleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

        NavHost(
            navController = navController,
            startDestination = Library,
            modifier = Modifier
        ) {
            composable<Login>(
                enterTransition = { slideInHorizontally(spatialSpec) { it } + fadeIn(effectsSpec) },
                exitTransition = { slideOutHorizontally(spatialSpec) { -it } + fadeOut(effectsSpec) },
                popEnterTransition = { slideInHorizontally(spatialSpec) { -it } + fadeIn(effectsSpec) },
                popExitTransition = { slideOutHorizontally(spatialSpec) { it } + fadeOut(effectsSpec) }
            ) {
                LoginScreen()
            }

            // --- MAIN TABS ---
            // Note: We use springs (spatialSpec) here instead of tweens.
            // This ensures the slide velocity matches the shared element velocity.

            composable<Library>(
                enterTransition = {
                    fadeIn(effectsSpec) + slideInHorizontally(spatialSpec) { -it / TabSlideOffset }
                },
                exitTransition = {
                    fadeOut(effectsSpec) + slideOutHorizontally(spatialSpec) { -it / TabSlideOffset }
                },
                popEnterTransition = {
                    fadeIn(effectsSpec) + slideInHorizontally(spatialSpec) { -it / TabSlideOffset }
                },
                popExitTransition = {
                    fadeOut(effectsSpec) + slideOutHorizontally(spatialSpec) { -it / TabSlideOffset }
                }
            ) {
                LibraryScreen(
                    onMediaClick = { mediaId -> onMediaClick(mediaId, "library") },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Discover>(
                enterTransition = {
                    val offset = if (initialState.destination.route == Library::class.qualifiedName) 1 else -1
                    fadeIn(effectsSpec) + slideInHorizontally(spatialSpec) { offset * (it / TabSlideOffset) }
                },
                exitTransition = {
                    val offset = if (targetState.destination.route == Library::class.qualifiedName) 1 else -1
                    fadeOut(effectsSpec) + slideOutHorizontally(spatialSpec) { offset * (it / TabSlideOffset) }
                },
                popEnterTransition = { fadeIn(effectsSpec) },
                popExitTransition = { fadeOut(effectsSpec) }
            ) {
                DiscoverScreen(
                    onMediaClick = { mediaId -> onMediaClick(mediaId, "discover") },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Profile>(
                enterTransition = {
                    fadeIn(effectsSpec) + slideInHorizontally(spatialSpec) { it / TabSlideOffset }
                },
                exitTransition = {
                    fadeOut(effectsSpec) + slideOutHorizontally(spatialSpec) { it / TabSlideOffset }
                },
                popEnterTransition = {
                    fadeIn(effectsSpec) + slideInHorizontally(spatialSpec) { it / TabSlideOffset }
                },
                popExitTransition = {
                    fadeOut(effectsSpec) + slideOutHorizontally(spatialSpec) { it / TabSlideOffset }
                }
            ) {
                ProfileScreen(
                    onMediaClick = { mediaId -> onMediaClick(mediaId, "profile") },
                    onLogoutClick = {
                        navController.navigate(Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // --- DETAILS ---
            // Using "Fade Through" pattern with Springs
            composable<Details>(
                deepLinks = listOf(
                    navDeepLink<Details>(basePath = "anisync://details")
                ),
                enterTransition = {
                    fadeIn(effectsSpec) + scaleIn(initialScale = 0.92f, animationSpec = scaleSpec)
                },
                exitTransition = {
                    fadeOut(effectsSpec) + scaleOut(targetScale = 1.04f, animationSpec = scaleSpec)
                },
                popEnterTransition = {
                    fadeIn(effectsSpec) + scaleIn(initialScale = 0.92f, animationSpec = scaleSpec)
                },
                popExitTransition = {
                    fadeOut(effectsSpec) + scaleOut(targetScale = 0.92f, animationSpec = scaleSpec)
                }
            ) { backStackEntry ->
                val details: Details = backStackEntry.toRoute()

                DetailsScreen(
                    mediaId = details.mediaId,
                    sourceScreen = details.sourceScreen,
                    onBackClick = { navController.popBackStack() },
                    onRelationClick = { relationMediaId ->
                        navController.navigate(Details(relationMediaId, "details"))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
        }
    }
}