package com.anisync.android.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Library : Screen("library", "Library", Icons.Default.Home)
    data object Discover : Screen("discover", "Discover", Icons.Default.Search)
}
