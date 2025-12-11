package com.anisync.android.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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

/**
 * Material 3 Motion Easing tokens
 * Reference: https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
 */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
private val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

// M3 Duration tokens (medium durations for navigation transitions)
private const val DurationMedium2 = 300
private const val DurationMedium4 = 400

// Lateral slide offset for tab transitions (10% of width for subtle movement)
private const val TabSlideOffset = 10

/**
 * Creates enter transition with lateral slide for tab navigation.
 * @param towards 1 for slide from right, -1 for slide from left
 */
private fun tabEnterTransition(towards: Int) = fadeIn(
    tween(DurationMedium2, easing = EmphasizedDecelerateEasing)
) + slideInHorizontally(
    initialOffsetX = { towards * (it / TabSlideOffset) },
    animationSpec = tween(DurationMedium2, easing = EmphasizedDecelerateEasing)
)

/**
 * Creates exit transition with lateral slide for tab navigation.
 * @param towards 1 for slide to right, -1 for slide to left
 */
private fun tabExitTransition(towards: Int) = fadeOut(
    tween(DurationMedium2, easing = EmphasizedAccelerateEasing)
) + slideOutHorizontally(
    targetOffsetX = { towards * (it / TabSlideOffset) },
    animationSpec = tween(DurationMedium2, easing = EmphasizedAccelerateEasing)
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
                enterTransition = { slideInHorizontally { it } + fadeIn() },
                exitTransition = { slideOutHorizontally { -it } + fadeOut() },
                popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it } + fadeOut() }
            ) {
                LoginScreen()
            }
            
            // Library (index 0) - slides from/to left relative to Discover/Profile
            composable<Library>(
                enterTransition = { tabEnterTransition(-1) },  // Enter from left
                exitTransition = { tabExitTransition(-1) },     // Exit to left
                popEnterTransition = { tabEnterTransition(-1) },
                popExitTransition = { tabExitTransition(-1) }
            ) {
                LibraryScreen(
                    onMediaClick = { mediaId -> onMediaClick(mediaId, "library") },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
            
            // Discover (index 1) - center tab, uses simple fade for most cases
            // When navigating from Library: enters from right
            // When navigating from Profile: enters from left
            composable<Discover>(
                enterTransition = { 
                    when (initialState.destination?.route) {
                        Library::class.qualifiedName -> tabEnterTransition(1)  // From Library: enter from right
                        else -> tabEnterTransition(-1)  // From Profile: enter from left
                    }
                },
                exitTransition = { 
                    when (targetState.destination?.route) {
                        Library::class.qualifiedName -> tabExitTransition(1)  // To Library: exit to right
                        else -> tabExitTransition(-1)  // To Profile: exit to left
                    }
                },
                popEnterTransition = { tabEnterTransition(1) },
                popExitTransition = { tabExitTransition(1) }
            ) {
                DiscoverScreen(
                    onMediaClick = { mediaId -> onMediaClick(mediaId, "discover") },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
            
            // Profile (index 2) - slides from/to right relative to Library/Discover
            composable<Profile>(
                enterTransition = { tabEnterTransition(1) },   // Enter from right
                exitTransition = { tabExitTransition(1) },      // Exit to right
                popEnterTransition = { tabEnterTransition(1) },
                popExitTransition = { tabExitTransition(1) }
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


            // Details screen uses M3 fade-through transition pattern for entering detail views
            // Fade-through: outgoing fades out, then incoming fades in with slight scale
            composable<Details>(
                deepLinks = listOf(
                    navDeepLink<Details>(basePath = "anisync://details")
                ),
                enterTransition = {
                    fadeIn(tween(DurationMedium4, easing = EmphasizedDecelerateEasing)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(DurationMedium4, easing = EmphasizedDecelerateEasing)
                    )
                },
                exitTransition = {
                    fadeOut(tween(DurationMedium2, easing = EmphasizedAccelerateEasing)) +
                    scaleOut(
                        targetScale = 1.04f,
                        animationSpec = tween(DurationMedium2, easing = EmphasizedAccelerateEasing)
                    )
                },
                popEnterTransition = {
                    fadeIn(tween(DurationMedium4, easing = EmphasizedDecelerateEasing)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(DurationMedium4, easing = EmphasizedDecelerateEasing)
                    )
                },
                popExitTransition = {
                    fadeOut(tween(DurationMedium2, easing = EmphasizedAccelerateEasing)) +
                    scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(DurationMedium2, easing = EmphasizedAccelerateEasing)
                    )
                }
            ) { backStackEntry ->
                val details: Details = backStackEntry.toRoute()

                DetailsScreen(
                    mediaId = details.mediaId,
                    sourceScreen = details.sourceScreen,
                    onBackClick = { navController.popBackStack() },
                    onRelationClick = { relationMediaId ->
                        // Relations navigate from Details, so use "details" as source
                        navController.navigate(Details(relationMediaId, "details"))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
        }
    }
}