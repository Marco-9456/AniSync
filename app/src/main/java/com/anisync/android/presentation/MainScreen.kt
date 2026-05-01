package com.anisync.android.presentation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anisync.android.R
import com.anisync.android.presentation.navigation.AniSyncNavHost
import com.anisync.android.presentation.navigation.Discover
import com.anisync.android.presentation.navigation.Feed
import com.anisync.android.presentation.navigation.Forum
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
fun MainScreen(viewModel: MainScreenViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshNotificationBadge()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainBottomBar(
                navController = navController,
                unreadNotificationCount = unreadNotificationCount
            )
        }
    ) { _ ->
        // Scaffold padding is intentionally ignored to prevent the NavHost from
        // remeasuring during bottom bar animations. AniSyncNavHost handles its own insets.
        AniSyncNavHost(
            navController = navController,
            onMediaClick = { mediaId, sourceScreen ->
                navController.navigate(MediaDetails(mediaId, sourceScreen)) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainBottomBar(
    navController: NavHostController,
    unreadNotificationCount: Int
) {
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
                R.string.nav_feed,
                Feed,
                Feed::class,
                Icons.Filled.DynamicFeed,
                Icons.Outlined.DynamicFeed
            ),
            BottomNavItem(
                R.string.nav_forum,
                Forum,
                Forum::class,
                Icons.Filled.Forum,
                Icons.Outlined.Forum
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
                    dest?.hasRoute<Feed>() == true ||
                    dest?.hasRoute<Forum>() == true ||
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
                val isProfile = item.routeClass == Profile::class
                val showBadge = isProfile && unreadNotificationCount > 0

                NavigationBarItem(
                    icon = {
                        val iconVector =
                            if (isSelected) item.selectedIcon else item.unselectedIcon
                        val itemTitle = stringResource(item.titleResId)
                        if (showBadge) {
                            ProfileNavBarIconWithBadge(
                                iconVector = iconVector,
                                title = itemTitle,
                                unreadCount = unreadNotificationCount,
                                isSelected = isSelected
                            )
                        } else {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = itemTitle
                            )
                        }
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

/**
 * Profile destination icon with the inbox unread badge.
 *
 * Per Material 3 badge guidelines:
 *  - Unselected destinations show the **large badge** (number) so the
 *    count is visible at a glance.
 *  - The selected destination collapses to the **small badge** (6.dp dot,
 *    no label) — the user is already in context, the count is redundant
 *    and the larger pill would compete with the destination's active
 *    indicator.
 *  - Counts >999 render as `999+`. TalkBack reads a pluralised label
 *    matching the displayed count, or "more than 999" on overflow.
 */
@Composable
private fun ProfileNavBarIconWithBadge(
    iconVector: ImageVector,
    title: String,
    unreadCount: Int,
    isSelected: Boolean
) {
    val badgeLabel = if (isSelected) {
        stringResource(R.string.notifications_unread_indicator_a11y)
    } else if (unreadCount > 999) {
        stringResource(R.string.notifications_unread_overflow_a11y)
    } else {
        pluralStringResource(
            R.plurals.notifications_unread_count_a11y,
            unreadCount,
            unreadCount
        )
    }
    val combinedDescription = "$title, $badgeLabel"

    // Empty contentDescription on the Badge keeps TalkBack from announcing
    // it twice — the icon-level description already carries both the
    // destination label and the unread state in a single utterance.
    val badgeModifier = Modifier.semantics { contentDescription = "" }

    BadgedBox(
        badge = {
            if (isSelected) {
                // Small badge (6.dp dot) — Material 3 omits content on selected
                // destinations because the count is redundant in-context.
                Badge(modifier = badgeModifier)
            } else {
                // Large badge — numeric label, capped at 999+ per M3.
                Badge(modifier = badgeModifier) {
                    Text(text = if (unreadCount > 999) "999+" else unreadCount.toString())
                }
            }
        }
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = combinedDescription
        )
    }
}