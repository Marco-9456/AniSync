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
    onMediaClick: (Int) -> Unit,
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
                onMediaClick = onMediaClick
            )
        }
        composable(Screen.Discover.route) {
            com.anisync.android.presentation.discover.DiscoverScreen(
                onMediaClick = onMediaClick,
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }
        composable(Screen.Profile.route) {
            com.anisync.android.presentation.profile.ProfileScreen(
                onMediaClick = onMediaClick,
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Search.route) {
            com.anisync.android.presentation.search.SearchScreen(
                onMediaClick = onMediaClick
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
