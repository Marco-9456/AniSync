package com.anisync.android.presentation.navigation

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

@Composable
fun AniSyncNavHost(
    navController: NavHostController,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Library,
        modifier = modifier
    ) {
        composable<Login>(
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            LoginScreen()
        }
        composable<Library>(
            enterTransition = { fadeIn(tween(DurationMedium2, easing = EmphasizedDecelerateEasing)) },
            exitTransition = { fadeOut(tween(DurationMedium2, easing = EmphasizedAccelerateEasing)) },
            popEnterTransition = { fadeIn(tween(DurationMedium2, easing = EmphasizedDecelerateEasing)) },
            popExitTransition = { fadeOut(tween(DurationMedium2, easing = EmphasizedAccelerateEasing)) }
        ) {
            LibraryScreen(
                onMediaClick = onMediaClick
            )
        }
        composable<Discover>(
            enterTransition = { fadeIn(tween(DurationMedium2, easing = EmphasizedDecelerateEasing)) },
            exitTransition = { fadeOut(tween(DurationMedium2, easing = EmphasizedAccelerateEasing)) },
            popEnterTransition = { fadeIn(tween(DurationMedium2, easing = EmphasizedDecelerateEasing)) },
            popExitTransition = { fadeOut(tween(DurationMedium2, easing = EmphasizedAccelerateEasing)) }
        ) {
            DiscoverScreen(
                onMediaClick = onMediaClick
            )
        }
        composable<Profile>(
            enterTransition = { fadeIn(tween(DurationMedium2, easing = EmphasizedDecelerateEasing)) },
            exitTransition = { fadeOut(tween(DurationMedium2, easing = EmphasizedAccelerateEasing)) },
            popEnterTransition = { fadeIn(tween(DurationMedium2, easing = EmphasizedDecelerateEasing)) },
            popExitTransition = { fadeOut(tween(DurationMedium2, easing = EmphasizedAccelerateEasing)) }
        ) {
            ProfileScreen(
                onMediaClick = onMediaClick,
                onLogoutClick = {
                    navController.navigate(Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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
                onBackClick = { navController.popBackStack() },
                onRelationClick = { relationMediaId ->
                    navController.navigate(Details(relationMediaId))
                }
            )
        }
    }
}