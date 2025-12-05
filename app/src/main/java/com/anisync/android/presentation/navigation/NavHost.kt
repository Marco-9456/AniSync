package com.anisync.android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anisync.android.presentation.library.LibraryScreen

@Composable
fun AniSyncNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController, 
        startDestination = Screen.Library.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            com.anisync.android.presentation.login.LoginScreen()
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onMediaClick = { mediaId ->
                    navController.navigate(Screen.Details.createRoute(mediaId))
                }
            )
        }
        composable(Screen.Discover.route) {
            com.anisync.android.presentation.discover.DiscoverScreen(
                onMediaClick = { mediaId ->
                    navController.navigate(Screen.Details.createRoute(mediaId))
                }
            )
        }
        composable(Screen.Profile.route) {
            com.anisync.android.presentation.profile.ProfileScreen(
                onMediaClick = { mediaId ->
                    navController.navigate(Screen.Details.createRoute(mediaId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(
            route = Screen.Details.route,
            arguments = listOf(androidx.navigation.navArgument("mediaId") { type = androidx.navigation.NavType.IntType })
        ) {
            com.anisync.android.presentation.details.DetailsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            com.anisync.android.presentation.search.SearchScreen(
                onMediaClick = { mediaId ->
                    navController.navigate(Screen.Details.createRoute(mediaId))
                }
            )
        }
        composable(Screen.Settings.route) {
            com.anisync.android.presentation.settings.SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutComplete = {
                    // Clear back stack and navigate to Login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
