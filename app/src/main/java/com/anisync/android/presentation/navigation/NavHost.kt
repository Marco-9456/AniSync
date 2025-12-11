package com.anisync.android.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

// Spring specs that match MaterialTheme.motionScheme defaults
// These can be called from non-composable contexts (transition lambdas)
private fun <T> spatialSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private fun <T> effectsSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

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
            composable<Login>(
                enterTransition = {
                    slideInHorizontally(spatialSpring()) { it } + fadeIn(effectsSpring())
                },
                exitTransition = {
                    slideOutHorizontally(spatialSpring()) { -it } + fadeOut(effectsSpring())
                },
                popEnterTransition = {
                    slideInHorizontally(spatialSpring()) { -it } + fadeIn(effectsSpring())
                },
                popExitTransition = {
                    slideOutHorizontally(spatialSpring()) { it } + fadeOut(effectsSpring())
                }
            ) {
                LoginScreen()
            }

            // --- MAIN TABS ---
            // Using spring physics for smooth, natural animations

            composable<Library>(
                enterTransition = {
                    fadeIn(effectsSpring()) + slideInHorizontally(spatialSpring()) { -it / TabSlideOffset }
                },
                exitTransition = {
                    fadeOut(effectsSpring()) + slideOutHorizontally(spatialSpring()) { -it / TabSlideOffset }
                },
                popEnterTransition = {
                    fadeIn(effectsSpring()) + slideInHorizontally(spatialSpring()) { -it / TabSlideOffset }
                },
                popExitTransition = {
                    fadeOut(effectsSpring()) + slideOutHorizontally(spatialSpring()) { -it / TabSlideOffset }
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
                    fadeIn(effectsSpring()) + slideInHorizontally(spatialSpring()) { offset * (it / TabSlideOffset) }
                },
                exitTransition = {
                    val offset = if (targetState.destination.route == Library::class.qualifiedName) 1 else -1
                    fadeOut(effectsSpring()) + slideOutHorizontally(spatialSpring()) { offset * (it / TabSlideOffset) }
                },
                popEnterTransition = {
                    fadeIn(effectsSpring())
                },
                popExitTransition = {
                    fadeOut(effectsSpring())
                }
            ) {
                DiscoverScreen(
                    onMediaClick = { mediaId -> onMediaClick(mediaId, "discover") },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Profile>(
                enterTransition = {
                    fadeIn(effectsSpring()) + slideInHorizontally(spatialSpring()) { it / TabSlideOffset }
                },
                exitTransition = {
                    fadeOut(effectsSpring()) + slideOutHorizontally(spatialSpring()) { it / TabSlideOffset }
                },
                popEnterTransition = {
                    fadeIn(effectsSpring()) + slideInHorizontally(spatialSpring()) { it / TabSlideOffset }
                },
                popExitTransition = {
                    fadeOut(effectsSpring()) + slideOutHorizontally(spatialSpring()) { it / TabSlideOffset }
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
                    fadeIn(effectsSpring()) + scaleIn(initialScale = 0.92f, animationSpec = spatialSpring())
                },
                exitTransition = {
                    fadeOut(effectsSpring()) + scaleOut(targetScale = 1.04f, animationSpec = spatialSpring())
                },
                popEnterTransition = {
                    fadeIn(effectsSpring()) + scaleIn(initialScale = 0.92f, animationSpec = spatialSpring())
                },
                popExitTransition = {
                    fadeOut(effectsSpring()) + scaleOut(targetScale = 0.92f, animationSpec = spatialSpring())
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