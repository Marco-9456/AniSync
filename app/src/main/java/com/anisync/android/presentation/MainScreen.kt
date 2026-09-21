package com.anisync.android.presentation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.anisync.android.presentation.components.SupportPromptCard
import com.anisync.android.presentation.components.alert.LocalRateLimitMonitor
import com.anisync.android.presentation.components.alert.ProvideToastManager
import com.anisync.android.presentation.components.alert.RateLimitNotice
import com.anisync.android.presentation.components.alert.TopToastHost
import com.anisync.android.presentation.components.navigation.CompactNavBar
import com.anisync.android.presentation.components.navigation.CompactNavBarItem
import com.anisync.android.presentation.navigation.AniSyncNavHost
import com.anisync.android.presentation.navigation.Discover
import com.anisync.android.presentation.navigation.navigateSafely
import com.anisync.android.presentation.navigation.Feed
import com.anisync.android.presentation.navigation.Forum
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.anisync.android.domain.MainTab
import com.anisync.android.presentation.navigation.Library
import com.anisync.android.presentation.navigation.MediaDetails
import com.anisync.android.presentation.navigation.Profile
import com.anisync.android.presentation.navigation.SettingsSponsors
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.LocalMainNavBarSuppressor
import com.anisync.android.presentation.util.LocalRailFabState
import com.anisync.android.presentation.util.MainNavBarSuppressor
import com.anisync.android.presentation.util.RailFab
import com.anisync.android.presentation.util.RailFabState
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

private data class BottomNavItem<T : Any>(
    val titleResId: Int,
    val route: T,
    val routeClass: KClass<T>,
    /** How the reselect gestures address this destination. */
    val tab: MainTab,
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
            MainTab.LIBRARY,
            Icons.Filled.VideoLibrary,
            Icons.Outlined.VideoLibrary,
            persistKey = "library"
        ),
        BottomNavItem(
            R.string.nav_discover,
            Discover,
            Discover::class,
            MainTab.DISCOVER,
            Icons.Filled.Explore,
            Icons.Outlined.Explore,
            persistKey = "discover"
        ),
        BottomNavItem(
            R.string.nav_feed,
            Feed,
            Feed::class,
            MainTab.FEED,
            Icons.Filled.DynamicFeed,
            Icons.Outlined.DynamicFeed,
            persistKey = "feed"
        ),
        BottomNavItem(
            R.string.nav_forum,
            Forum,
            Forum::class,
            MainTab.FORUM,
            Icons.Filled.Forum,
            Icons.Outlined.Forum,
            persistKey = "forum"
        ),
        BottomNavItem(
            R.string.nav_profile,
            Profile,
            Profile::class,
            MainTab.PROFILE,
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
    // "Open Discover search with preset filters" (ranking cards, genre/tag chips on
    // media details): switch to the Discover tab; DiscoverViewModel picks the request
    // up from the same launcher, applies the filters and expands the search overlay.
    LaunchedEffect(navController) {
        // Routes the launcher pop below stops at — the five main tab roots.
        val mainTabClasses = listOf(
            Library::class, Discover::class, Feed::class, Forum::class, Profile::class
        )
        viewModel.discoverSearchNavigations.collect {
            // This tab switch is app-initiated (a ranking/genre/tag tap), not a
            // deliberate "leave and come back later": first pop the detail chain
            // that launched the search back to the source tab's root WITHOUT
            // saving it, so returning to that tab later lands on the tab itself
            // (with its own state intact), not on the media page again. Manual
            // tab switches keep the normal save/restore behaviour.
            while (
                navController.currentBackStackEntry?.destination
                    ?.let { dest -> mainTabClasses.any { dest.hasRoute(it) } } == false
            ) {
                if (!navController.popBackStack()) break
            }
            navController.navigateToMainTab(Discover, "discover", viewModel::onMainTabSelected)
            // The restored Discover tab stack may itself have a details screen on
            // top (search → details → chip tap). Pop back to the Discover root,
            // otherwise the "switch" lands on the same details screen and the
            // search overlay never shows.
            navController.popBackStack(route = Discover, inclusive = false)
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

    // Navigation container per Material 3: a navigation bar on compact width (phone portrait) AND on
    // compact height (< 480dp, e.g. a phone in landscape) where a vertical rail's destinations can't
    // fit and would overflow; the navigation rail only when the window has room in both axes (tablets
    // / foldables). M3: "use a Navigation Bar for compact screen dimensions, especially in landscape,
    // as the rail may result in a compact height." Rail destinations must stay fixed/visible (no
    // scroll), so a too-short window must fall back to the bar rather than clip items off-screen.
    val adaptive = LocalAdaptiveInfo.current

    RateLimitNotice(monitor = viewModel.rateLimitMonitor, toastManager = viewModel.toastManager)

    // The tip card after an update. Hosted here rather than on a screen so it floats over whichever
    // tab the launch opened on, centred in the window at every size.
    val supportPromptVisible by viewModel.supportPromptVisible.collectAsStateWithLifecycle()
    val supportPrompt: @Composable () -> Unit = {
        SupportPromptCard(
            visible = supportPromptVisible,
            onDonate = {
                viewModel.onSupportPromptDismissed()
                navController.navigateSafely(SettingsSponsors)
            },
            onDismiss = viewModel::onSupportPromptDismissed
        )
    }

    ProvideToastManager(toastManager = viewModel.toastManager) {
        CompositionLocalProvider(
            LocalMainNavBarSuppressor provides navBarSuppressor,
            LocalRateLimitMonitor provides viewModel.rateLimitMonitor,
        ) {
            if (adaptive.isCompact || adaptive.isCompactHeight) {
                CompactNavLayout(
                    navController = navController,
                    startDestination = startDestination,
                    unreadNotificationCount = unreadNotificationCount,
                    navBarStyle = navBarStyle,
                    navBarShowLabels = navBarShowLabels,
                    navBarCornerRadius = navBarCornerRadius,
                    onTabSelected = viewModel::onMainTabSelected,
                    onTabTapped = viewModel::onTabTapped,
                    onTabSearch = viewModel::onTabSearchRequested,
                    supportPrompt = supportPrompt,
                    toastHost = { TopToastHost(toastManager = viewModel.toastManager) }
                )
            } else {
                RailNavLayout(
                    navController = navController,
                    startDestination = startDestination,
                    unreadNotificationCount = unreadNotificationCount,
                    onTabSelected = viewModel::onMainTabSelected,
                    onTabTapped = viewModel::onTabTapped,
                    onTabSearch = viewModel::onTabSearchRequested,
                    supportPrompt = supportPrompt,
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
    onTabTapped: (MainTab, Boolean) -> Unit,
    onTabSearch: (MainTab) -> Unit,
    supportPrompt: @Composable () -> Unit,
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
                    onTabSelected = onTabSelected,
                    onTabTapped = onTabTapped,
                    onTabSearch = onTabSearch
                )
            }
        }
    ) { _ ->
        // Bar's occupied bottom space — used by scrollable tab content as bottom contentPadding so
        // the last item is reachable above the bar.
        //
        // Measured rather than assumed. The two constants this replaces were tuned against gesture
        // navigation, where the system inset is small; under three-button navigation the bar sits
        // roughly 24dp lower and content ran right into it (issue #127). Measuring also keeps the
        // reserve honest as the label setting, corner radius and font scale change.
        val density = LocalDensity.current
        var barHeightPx by remember { mutableIntStateOf(0) }
        val fallbackInset = if (navBarShowLabels) 96.dp else 76.dp
        val barInset = if (barHeightPx > 0) {
            with(density) { barHeightPx.toDp() }
        } else {
            fallbackInset + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        }

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
                        .onSizeChanged { barHeightPx = it.height }
                ) {
                    MainBottomBar(
                        navController = navController,
                        unreadNotificationCount = unreadNotificationCount,
                        style = NavBarStyle.FLOATING,
                        showLabels = navBarShowLabels,
                        cornerRadius = navBarCornerRadius,
                        onTabSelected = onTabSelected,
                        onTabTapped = onTabTapped,
                        onTabSearch = onTabSearch
                    )
                }
            }

            supportPrompt()

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
    onTabSelected: (String) -> Unit,
    onTabTapped: (MainTab, Boolean) -> Unit,
    onTabSearch: (MainTab) -> Unit,
    supportPrompt: @Composable () -> Unit,
    toastHost: @Composable () -> Unit
) {
    // Bridges a tab's contextual primary action into the rail header (Material 3 hosts the FAB in the
    // rail rather than floating it bottom-end). Provided above both the rail and the NavHost so the
    // active tab can publish into it and the rail can render it.
    val railFabState = remember { RailFabState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompositionLocalProvider(LocalRailFabState provides railFabState) {
            Row(modifier = Modifier.fillMaxSize()) {
                MainWideNavigationRail(
                    navController = navController,
                    unreadNotificationCount = unreadNotificationCount,
                    onTabSelected = onTabSelected,
                    onTabTapped = onTabTapped,
                    onTabSearch = onTabSearch
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
        }
        supportPrompt()
        toastHost()
    }
}

/**
 * Gives a navigation item an explicit "search this tab" accessibility action.
 *
 * The gesture itself is unreachable under a screen reader, where a double tap already means
 * activate, so the shortcut needs a real action rather than only a hidden gesture. Tabs without a
 * search get no action.
 */
@Composable
private fun tabSearchActionModifier(
    tab: MainTab,
    tabTitle: String,
    onTabSearch: (MainTab) -> Unit
): Modifier {
    if (!tab.hasSearch) return Modifier
    val label = stringResource(R.string.a11y_nav_search_tab, tabTitle)
    return remember(tab, label, onTabSearch) {
        Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(label) { onTabSearch(tab); true })
        }
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
    onTabSelected: (String) -> Unit,
    onTabTapped: (MainTab, Boolean) -> Unit,
    onTabSearch: (MainTab) -> Unit
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
                        // Every tap reaches the handler, since both taps of the double-tap
                        // shortcut count whether or not they land on the open tab.
                        if (!isSelected) {
                            navController.navigateToMainTab(item.route, item.persistKey, onTabSelected)
                        }
                        onTabTapped(item.tab, isSelected)
                    },
                    modifier = tabSearchActionModifier(item.tab, itemTitle, onTabSearch),
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

/** Height of the rail header's action slot, reserved whether or not a tab fills it. */
private val RailFabSlotHeight = 56.dp

/**
 * The contextual primary action in a rail's header (Material 3). Like the destination items, it
 * follows the rail's expansion: an icon-only FAB when collapsed, animating to an **extended FAB** with
 * a text label (the action's description) when the rail is [expanded].
 */
@Composable
private fun RailHeaderFab(fab: RailFab, expanded: Boolean, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = fab.onClick,
        expanded = expanded,
        icon = {
            Icon(
                imageVector = fab.icon,
                // When expanded the visible text carries the label; null here avoids a double read.
                contentDescription = if (expanded) null else fab.contentDescription
            )
        },
        text = { Text(fab.contentDescription) },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
    )
}

/**
 * The collapsible Material 3 wide navigation rail used on all rail widths (medium / expanded).
 *
 * **Starts collapsed** (icon + label below, ~96dp) so it never eats horizontal space by default; the
 * header menu button expands it to labels-beside-icons on demand (a default-expanded rail read as too
 * wide). Mirrors [MainNavigationRail]'s destinations, selection, badge and route-suppression logic.
 *
 * Header alignment follows the M3 `WideNavigationRail` sample: the content is start-indented rather
 * than width-filled (the rail measures its header with an unbounded width, so a `fillMaxWidth` child
 * collapses to nothing). A 24dp start inset centers the 48dp menu button in the 96dp collapsed rail —
 * aligning it with the centered destination items — and matches the start inset of the expanded
 * items; the 56dp FAB uses a 20dp inset so its center lands on the same axis.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainWideNavigationRail(
    navController: NavHostController,
    unreadNotificationCount: Int,
    onTabSelected: (String) -> Unit,
    onTabTapped: (MainTab, Boolean) -> Unit,
    onTabSearch: (MainTab) -> Unit
) {
    val navItems = rememberMainNavItems()
    val navBackStackEntryState = navController.currentBackStackEntryAsState()
    val navBarSuppressor = LocalMainNavBarSuppressor.current
    val openTab = navItems.firstOrNull {
        navBackStackEntryState.value?.destination?.hasRoute(it.routeClass) == true
    }?.tab
    // The open tab's action, not simply the last one published. A screen keeps its FAB until it
    // leaves composition at the end of the tab transition, so reading the slot alone left the
    // outgoing tab's action sitting in the header well after the new tab had taken over.
    val railFab = LocalRailFabState.current?.fab?.takeIf { it.tab == openTab }

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

    val railState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    val expanded = railState.targetValue == WideNavigationRailValue.Expanded

    val expandLabel = stringResource(R.string.rail_expand)
    val collapseLabel = stringResource(R.string.rail_collapse)
    val expandedStateDesc = stringResource(R.string.rail_expanded)
    val collapsedStateDesc = stringResource(R.string.rail_collapsed)

    AnimatedVisibility(
        visible = isRailVisible,
        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
    ) {
        WideNavigationRail(
            state = railState,
            // Match the status-bar protection / two-pane gutter tone for a consistent frame.
            colors = WideNavigationRailDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            header = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (expanded) railState.collapse() else railState.expand()
                            }
                        },
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .semantics {
                                stateDescription =
                                    if (expanded) expandedStateDesc else collapsedStateDesc
                            }
                    ) {
                        Icon(
                            imageVector = if (expanded) {
                                Icons.AutoMirrored.Filled.MenuOpen
                            } else {
                                Icons.Filled.Menu
                            },
                            contentDescription = if (expanded) collapseLabel else expandLabel
                        )
                    }
                    // The slot keeps its height whether or not the open tab publishes an action.
                    // A screen publishes and disposes its FAB around the tab transition rather
                    // than on the tap, so a collapsing slot slid every destination up or down a
                    // FAB height a moment after a tab switch, under a finger that was on its way
                    // down for the second tap of the double-tap shortcut.
                    Box(modifier = Modifier.height(RailFabSlotHeight)) {
                        Crossfade(targetState = railFab, label = "RailHeaderFab") { fab ->
                            if (fab != null) {
                                RailHeaderFab(fab, expanded, Modifier.padding(start = 20.dp))
                            }
                        }
                    }
                }
            }
        ) {
            val currentDestination = navBackStackEntryState.value?.destination

            navItems.forEach { item ->
                val isSelected = currentDestination?.hasRoute(item.routeClass) == true
                val isProfile = item.routeClass == Profile::class
                val showBadge = isProfile && unreadNotificationCount > 0
                val iconVector = if (isSelected) item.selectedIcon else item.unselectedIcon
                val itemTitle = stringResource(item.titleResId)

                WideNavigationRailItem(
                    railExpanded = expanded,
                    selected = isSelected,
                    onClick = {
                        // Every tap reaches the handler, since both taps of the double-tap
                        // shortcut count whether or not they land on the open tab.
                        if (!isSelected) {
                            navController.navigateToMainTab(item.route, item.persistKey, onTabSelected)
                        }
                        onTabTapped(item.tab, isSelected)
                    },
                    modifier = tabSearchActionModifier(item.tab, itemTitle, onTabSearch),
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
                    label = {
                        Text(
                            text = itemTitle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
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
