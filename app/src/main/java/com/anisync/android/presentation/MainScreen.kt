package com.anisync.android.presentation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anisync.android.R
import com.anisync.android.presentation.navigation.AniSyncNavHost
import com.anisync.android.presentation.navigation.Discover
import com.anisync.android.presentation.navigation.Library
import com.anisync.android.presentation.navigation.MediaDetails
import com.anisync.android.presentation.navigation.Profile
import kotlin.reflect.KClass

private data class BottomNavItem<T : Any>(
    val titleResId: Int,
    val route: T,
    val routeClass: KClass<T>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainBottomBar(navController = navController)
        }
    ) { _ ->
        // Scaffold padding is intentionally ignored to prevent the NavHost from
        // remeasuring during bottom bar animations. AniSyncNavHost handles its own insets.
        AniSyncNavHost(
            navController = navController,
            onMediaClick = { mediaId, sourceScreen ->
                navController.navigate(MediaDetails(mediaId, sourceScreen))
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainBottomBar(navController: NavHostController) {
    val navItems = remember {
        listOf(
            BottomNavItem(
                R.string.nav_library,
                Library,
                Library::class,
                Icons.Filled.VideoLibrary,
                Icons.Outlined.VideoLibrary
            ),
            BottomNavItem(
                R.string.nav_discover,
                Discover,
                Discover::class,
                Icons.Filled.Explore,
                Icons.Outlined.Explore
            ),
            BottomNavItem(
                R.string.nav_profile,
                Profile,
                Profile::class,
                Icons.Filled.Person,
                Icons.Outlined.Person
            )
        )
    }

    val navBackStackEntryState = navController.currentBackStackEntryAsState()

    val isBottomBarVisible by remember {
        derivedStateOf {
            val dest = navBackStackEntryState.value?.destination
            dest?.hasRoute<Library>() == true ||
                    dest?.hasRoute<Discover>() == true ||
                    dest?.hasRoute<Profile>() == true
        }
    }

    val motionScheme = MaterialTheme.motionScheme

    val enterAnim = remember(motionScheme) {
        slideInVertically(
            initialOffsetY = { it },
            animationSpec = motionScheme.defaultSpatialSpec()
        ) + expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = motionScheme.defaultSpatialSpec()
        )
    }

    val exitAnim = remember(motionScheme) {
        slideOutVertically(
            targetOffsetY = { it },
            animationSpec = motionScheme.fastSpatialSpec()
        ) + shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = motionScheme.fastSpatialSpec()
        )
    }

    AnimatedVisibility(
        visible = isBottomBarVisible,
        enter = enterAnim,
        exit = exitAnim
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            val currentDestination = navBackStackEntryState.value?.destination

            navItems.forEach { item ->
                val isSelected = currentDestination?.hasRoute(item.routeClass) == true

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.titleResId)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(item.titleResId),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}