package com.anisync.android.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Login : Screen("login", "Login", Icons.Default.Home)
    data object Library : Screen("library", "Library", Icons.Default.Home)
    data object Discover : Screen("discover", "Discover", Icons.Default.Search)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
    data object Details : Screen("details/{mediaId}", "Details", Icons.Default.Person) {
        fun createRoute(mediaId: Int) = "details/$mediaId"
    }
}