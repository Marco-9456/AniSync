package com.anisync.android.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anisync.android.presentation.details.DetailsBottomSheet
import com.anisync.android.presentation.navigation.AniSyncNavHost
import com.anisync.android.presentation.navigation.Screen
import com.anisync.android.ui.theme.BeigeYellow
import com.anisync.android.ui.theme.CreamBackground
import com.anisync.android.ui.theme.OliveDrab
import com.anisync.android.ui.theme.SurfacePinkWhite
import com.anisync.android.ui.theme.TextDark

/**
 * Helper class to define navigation items with selected/unselected icon states
 */
private data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // Define items with specific visual assets for a polished look
    val navItems = remember {
        listOf(
            BottomNavItem(
                title = "Library",
                route = Screen.Library.route,
                selectedIcon = Icons.Filled.VideoLibrary,
                unselectedIcon = Icons.Outlined.VideoLibrary
            ),
            BottomNavItem(
                title = "Discover",
                route = Screen.Discover.route,
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore
            ),
            BottomNavItem(
                title = "Profile",
                route = Screen.Profile.route,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Bottom Sheet State
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedMediaId by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = CreamBackground, // Match app background theme
        bottomBar = {
            NavigationBar(
                containerColor = SurfacePinkWhite, // Warm card-like surface
                tonalElevation = 0.dp // Flat style, color provides separation
            ) {
                navItems.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                    // Animate icon color for polish
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) OliveDrab else TextDark.copy(alpha = 0.6f),
                        animationSpec = tween(300),
                        label = "IconColor"
                    )

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = iconColor
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = iconColor
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OliveDrab,
                            selectedTextColor = OliveDrab,
                            indicatorColor = BeigeYellow, // Pill shape color
                            unselectedIconColor = TextDark.copy(alpha = 0.6f),
                            unselectedTextColor = TextDark.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        AniSyncNavHost(
            navController = navController,
            onMediaClick = { mediaId ->
                selectedMediaId = mediaId
                showBottomSheet = true
            },
            modifier = Modifier.padding(innerPadding)
        )

        if (showBottomSheet && selectedMediaId != null) {
            DetailsBottomSheet(
                mediaId = selectedMediaId!!,
                sheetState = sheetState,
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}