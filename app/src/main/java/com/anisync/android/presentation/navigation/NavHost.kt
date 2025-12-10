package com.anisync.android.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            LibraryScreen(
                onMediaClick = onMediaClick
            )
        }
        composable<Discover>(
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            DiscoverScreen(
                onMediaClick = onMediaClick
            )
        }
        composable<Profile>(
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
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


        composable<Details>(
            deepLinks = listOf(
                navDeepLink<Details>(basePath = "anisync://details")
            ),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
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