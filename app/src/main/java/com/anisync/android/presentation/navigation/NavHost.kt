package com.anisync.android.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
        startDestination = Screen.Library.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.Login.route,
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            LoginScreen()
        }
        composable(
            route = Screen.Library.route,
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            LibraryScreen(
                onMediaClick = onMediaClick
            )
        }
        composable(
            route = Screen.Discover.route,
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            DiscoverScreen(
                onMediaClick = onMediaClick,
                onSearchClick = { /* No-op: Search is handled internally now */ }
            )
        }
        composable(
            route = Screen.Profile.route,
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            ProfileScreen(
                onMediaClick = onMediaClick,
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("mediaId") { type = NavType.IntType }),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: return@composable

            DetailsScreen(
                mediaId = mediaId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}