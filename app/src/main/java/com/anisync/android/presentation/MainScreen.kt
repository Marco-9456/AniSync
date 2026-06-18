package com.anisync.android.presentation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.flow.filterNotNull
import com.anisync.android.R
import com.anisync.android.data.NavBarStyle
import com.anisync.android.presentation.components.alert.ProvideToastManager
import com.anisync.android.presentation.components.alert.TopToastHost
import com.anisync.android.presentation.components.navigation.CompactNavBar
import com.anisync.android.presentation.components.navigation.CompactNavBarItem
import com.anisync.android.presentation.navigation.AniSyncNavHost
import com.anisync.android.presentation.navigation.Discover
import com.anisync.android.presentation.navigation.navigateSafely
import com.anisync.android.presentation.navigation.Feed
import com.anisync.android.presentation.navigation.Forum
import com.anisync.android.presentation.navigation.Library
import com.anisync.android.presentation.navigation.MediaDetails
import com.anisync.android.presentation.navigation.Profile
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.LocalMainNavBarSuppressor
import com.anisync.android.presentation.util.MainNavBarSuppressor
import kotlin.reflect.KClass

private data class BottomNavItem<T : Any>(
    val titleResId: Int,
    val route: T,
    val routeClass: KClass<T>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    /**
     * Stable key persisted as the "last visited tab" for cold-launch restore.
     * Null for tabs that should never become the launch destination (Profile).
     */
    val persistKey: String? = null
)

/**
 * The five top-level destinations, shared by every navigation container (bottom bar, rail) so the
 * destination set, ordering, and badge logic never diverge across form factors.
 */
@Composable
private fun rememberMainNavItems(): List<BottomNavItem<*>> = remember {
    listOf(
        BottomNavItem(
            R.string.nav_library,
            Library,
            Library::class,
            Icons.Filled.VideoLibrary,
            Icons.Outlined.VideoLibrary,
            persistKey = "library"
        ),
        BottomNavItem(
            R.string.nav_discover,
            Discover,
            Discover::class,
            Icons.Filled.Explore,
            Icons.Outlined.Explore,
            persistKey = "discover"
        ),
        BottomNavItem(
            R.string.nav_feed,
            Feed,
            Feed::class,
            Icons.Filled.DynamicFeed,
            Icons.Outlined.DynamicFeed,
            persistKey = "feed"
        ),
        BottomNavItem(
            R.string.nav_forum,
            Forum,
            Forum::class,
            Icons.Filled.Forum,
            Icons.Outlined.Forum,
            persistKey = "forum"
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

/** Shared tab navigation: single-top, save/restore the tab back stack, then persist the tab. */
private fun NavHostController.navigateToMainTab(route: Any, persistKey: String?, onTabSelected: (String) -> Unit) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    persistKey?.let(onTabSelected)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    builtAtEpoch: Int = 0,
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    LaunchedEffect(navController) {
        val activity = context as? com.anisync.android.MainActivity ?: return@LaunchedEffect
        activity.newIntents.collect { intent ->
            navController.handleDeepLink(intent)
        }
    }
    // Cross-account notification deep links: delivered after the account switch settles, tagged with
    // the session epoch of the post-switch MainScreen. Only that instance (matching epoch) handles
    // it, so the pre-switch MainScreen can't consume it first.
    LaunchedEffect(navController) {
        val activity = context as? com.anisync.android.MainActivity ?: return@LaunchedEffect
        activity.pendingDeepLink.filterNotNull().collect { pending ->
            if (pending.epoch == builtAtEpoch) {
                navController.handleDeepLink(pending.intent)
                activity.consumePendingDeepLink()
            }
        }
    }
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val navBarStyle by viewModel.navBarStyle.collectAsStateWithLifecycle()
    val navBarShowLabels by viewModel.navBarShowLabels.collectAsStateWithLifecycle()
    val navBarCornerRadius by viewModel.navBarCornerRadius.collectAsStateWithLifecycle()
    val navBarSuppressor = remember { MainNavBarSuppressor() }

    // Cold-launch restore: open on the tab the user last visited (Library/Discover/
    // Feed/Forum). Compose Navigation restores its own back stack across process
    // death, so this only governs a genuinely fresh start.
    val startDestination: Any = remember(viewModel.startTabKey) {
        when (viewModel.startTabKey) {
            "discover" -> Discover
            "feed" -> Feed
            "forum" -> Forum
            else -> Library
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshNotificationBadge()
    }

    // The window width decides the navigation container: a bottom bar on compact (phones), a
    // navigation rail on medium/expanded (landscape phones, tablets, foldables) per Material 3.
    val adaptive = LocalAdaptiveInfo.current

    ProvideToastManager(toastManager = viewModel.toastManager) {
        CompositionLocalProvider(LocalMainNavBarSuppressor provides navBarSuppressor) {
            if (adaptive.isCompact) {
                CompactNavLayout(
                    navController = navController,
                    startDestination = startDestination,
                    unreadNotificationCount = unreadNotificationCount,
                    navBarStyle = navBarStyle,
                    navBarShowLabels = navBarShowLabels,
                    navBarCornerRadius = navBarCornerRadius,
                    onTabSelected = viewModel::onMainTabSelected,
                    toastHost = { TopToastHost(toastManager = viewModel.toastManager) }
                )
            } else {
                RailNavLayout(
                    navController = navController,
                    startDestination = startDestination,
                    unreadNotificationCount = unreadNotificationCount,
                    navBarShowLabels = navBarShowLabels,
                    onTabSelected = viewModel::onMainTabSelected,
                    toastHost = { TopToastHost(toastManager = viewModel.toastManager) }
                )
            }
        }
    }
}

/** Shared NavHost wiring used by both the compact and rail layouts. */
@Composable
private fun MainNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier
) {
    AniSyncNavHost(
        navController = navController,
        startDestination = startDestination,
        onMediaClick = { mediaId, sourceScreen ->
            navController.navigateSafely(MediaDetails(mediaId, sourceScreen))
        },
        modifier = modifier
    )
}

/**
 * Compact layout (phones): bottom navigation bar. Anchored bars get a real `bottomBar` slot so
 * content shrinks above them; floating bars overlay the content so scrollable regions pass through
 * the empty space beside the pill. Unchanged from the original mobile-first behavior.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompactNavLayout(
    navController: NavHostController,
    startDestination: Any,
    unreadNotificationCount: Int,
    navBarStyle: NavBarStyle,
    navBarShowLabels: Boolean,
    navBarCornerRadius: Float,
    onTabSelected: (String) -> Unit,
    toastHost: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (navBarStyle == NavBarStyle.ANCHORED) {
                MainBottomBar(
                    navController = navController,
                    unreadNotificationCount = unreadNotificationCount,
                    style = NavBarStyle.ANCHORED,
                    showLabels = navBarShowLabels,
                    cornerRadius = navBarCornerRadius,
                    onTabSelected = onTabSelected
                )
            }
        }
    ) { _ ->
        // Bar's occupied bottom space — used by scrollable tab content as bottom contentPadding so
        // the last item is reachable above the bar.
        val barInset = if (navBarShowLabels) 96.dp else 76.dp

        Box(modifier = Modifier.fillMaxSize()) {
            // Scaffold padding is intentionally ignored to prevent the NavHost from remeasuring
            // during bottom bar animations. AniSyncNavHost handles its own insets.
            CompositionLocalProvider(LocalMainNavBarInset provides barInset) {
                MainNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (navBarStyle == NavBarStyle.FLOATING) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    MainBottomBar(
                        navController = navController,
                        unreadNotificationCount = unreadNotificationCount,
                        style = NavBarStyle.FLOATING,
                        showLabels = navBarShowLabels,
                        cornerRadius = navBarCornerRadius,
                        onTabSelected = onTabSelected
                    )
                }
            }

            toastHost()
        }
    }
}

/**
 * Medium / expanded layout (landscape phones, tablets, foldables): a navigation rail pinned to the
 * start edge, content filling the rest. The rail hides on non-top-level routes (detail/grid pushes)
 * exactly like the bottom bar, so those screens render full width. There is no bottom bar here, so
 * tab content uses a zero bottom inset.
 */
@Composable
private fun RailNavLayout(
    navController: NavHostController,
    startDestination: Any,
    unreadNotificationCount: Int,
    navBarShowLabels: Boolean,
    onTabSelected: (String) -> Unit,
    toastHost: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            MainNavigationRail(
                navController = navController,
                unreadNotificationCount = unreadNotificationCount,
                showLabels = navBarShowLabels,
                onTabSelected = onTabSelected
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                CompositionLocalProvider(LocalMainNavBarInset provides 0.dp) {
                    MainNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        toastHost()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainBottomBar(
    navController: NavHostController,
    unreadNotificationCount: Int,
    style: NavBarStyle,
    showLabels: Boolean,
    cornerRadius: Float,
    onTabSelected: (String) -> Unit
) {
    val navItems = rememberMainNavItems()

    val navBackStackEntryState = navController.currentBackStackEntryAsState()
    val navBarSuppressor = LocalMainNavBarSuppressor.current

    val isBottomBarVisible by remember(navBarSuppressor) {
        derivedStateOf {
            val dest = navBackStackEntryState.value?.destination
            val onWhitelistedRoute = dest?.hasRoute<Library>() == true ||
                    dest?.hasRoute<Discover>() == true ||
                    dest?.hasRoute<Feed>() == true ||
                    dest?.hasRoute<Forum>() == true ||
                    dest?.hasRoute<Profile>() == true
            onWhitelistedRoute && navBarSuppressor?.isSuppressed != true
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
        CompactNavBar(style = style, cornerRadius = cornerRadius) {
            val currentDestination = navBackStackEntryState.value?.destination

            navItems.forEach { item ->
                val isSelected = currentDestination?.hasRoute(item.routeClass) == true
                val isProfile = item.routeClass == Profile::class
                val showBadge = isProfile && unreadNotificationCount > 0
                val iconVector =
                    if (isSelected) item.selectedIcon else item.unselectedIcon
                val itemTitle = stringResource(item.titleResId)

                CompactNavBarItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigateToMainTab(item.route, item.persistKey, onTabSelected)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = itemTitle
                        )
                    },
                    label = {
                        Text(
                            text = itemTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    showLabel = showLabels,
                    badge = if (showBadge) {
                        {
                            ProfileNavBarIconWithBadge(
                                iconVector = iconVector,
                                title = itemTitle,
                                unreadCount = unreadNotificationCount,
                                isSelected = isSelected
                            )
                        }
                    } else null
                )
            }
        }
    }
}

/**
 * The navigation rail shown on medium / expanded widths. Mirrors [MainBottomBar]: same destinations,
 * same selection and badge logic, same suppression on non-top-level routes — only the container and
 * its enter/exit (horizontal) differ. Labels follow the user's "show labels" preference.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainNavigationRail(
    navController: NavHostController,
    unreadNotificationCount: Int,
    showLabels: Boolean,
    onTabSelected: (String) -> Unit
) {
    val navItems = rememberMainNavItems()
    val navBackStackEntryState = navController.currentBackStackEntryAsState()
    val navBarSuppressor = LocalMainNavBarSuppressor.current

    val isRailVisible by remember(navBarSuppressor) {
        derivedStateOf {
            val dest = navBackStackEntryState.value?.destination
            val onWhitelistedRoute = dest?.hasRoute<Library>() == true ||
                    dest?.hasRoute<Discover>() == true ||
                    dest?.hasRoute<Feed>() == true ||
                    dest?.hasRoute<Forum>() == true ||
                    dest?.hasRoute<Profile>() == true
            onWhitelistedRoute && navBarSuppressor?.isSuppressed != true
        }
    }

    AnimatedVisibility(
        visible = isRailVisible,
        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
    ) {
        NavigationRail(
            // One step below the two-pane gutter (surfaceContainerHigh) so the rail keeps a defined
            // right edge instead of bleeding into the gutter and looking like it extends past its
            // items. Also matches the status-bar protection tone for a consistent frame.
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            val currentDestination = navBackStackEntryState.value?.destination

            // Center the destinations vertically — Material 3 rail with no header/FAB.
            Spacer(Modifier.weight(1f))

            navItems.forEach { item ->
                val isSelected = currentDestination?.hasRoute(item.routeClass) == true
                val isProfile = item.routeClass == Profile::class
                val showBadge = isProfile && unreadNotificationCount > 0
                val iconVector = if (isSelected) item.selectedIcon else item.unselectedIcon
                val itemTitle = stringResource(item.titleResId)

                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigateToMainTab(item.route, item.persistKey, onTabSelected)
                        }
                    },
                    icon = {
                        if (showBadge) {
                            ProfileNavBarIconWithBadge(
                                iconVector = iconVector,
                                title = itemTitle,
                                unreadCount = unreadNotificationCount,
                                isSelected = isSelected
                            )
                        } else {
                            Icon(imageVector = iconVector, contentDescription = itemTitle)
                        }
                    },
                    label = if (showLabels) {
                        {
                            Text(
                                text = itemTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else null,
                    alwaysShowLabel = showLabels,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(Modifier.weight(1f))
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
