package com.anisync.android.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.anisync.android.R
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.LocalPaneIsRoot
import com.anisync.android.presentation.util.PaneDragHandle
import com.anisync.android.presentation.util.PaneSheetHost
import com.anisync.android.presentation.util.TwoPaneDefaults
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anisync.android.presentation.details.CharacterDetailsScreen
import com.anisync.android.presentation.details.CharacterMediaGridScreen
import com.anisync.android.presentation.details.MediaDetailsScreen
import com.anisync.android.presentation.details.MediaRecommendationsGridScreen
import com.anisync.android.presentation.details.MediaRelationsGridScreen
import com.anisync.android.presentation.details.StaffDetailsScreen
import com.anisync.android.presentation.details.StaffMediaGridScreen
import com.anisync.android.presentation.details.StaffProductionMediaGridScreen
import com.anisync.android.presentation.details.StudioDetailsScreen
import com.anisync.android.presentation.details.StudioMediaGridScreen

// Shared-element source tag for detail screens hosted in a two-pane detail slot. No cross-pane
// morph partner exists under this tag, so pane-hosted details fade in instead of morphing.
// Internal so sibling list-detail hosts (e.g. Discover's review target) reuse the same tag.
internal const val LIST_DETAIL_PANE_SOURCE = "list_detail_pane"

// Width split between the two panes while the detail is open, as the list pane's fraction. The list
// stays the smaller "index" pane; the detail gets the majority of the space. User-resizable from
// fully collapsed up to [MAX_LIST_FRACTION] via the drag handle (drag to resize, release to snap to
// the nearest anchor; tap to cycle the split; long-press to collapse/expand the list pane).
private const val DEFAULT_LIST_FRACTION = 1f / 3f
private const val MAX_LIST_FRACTION = 0.7f

// Below this the list pane is treated as fully collapsed (single-pane: detail fills the width).
private const val COLLAPSE_THRESHOLD = 0.04f

// The list pane's content is laid out at no less than this fraction of the row width and clipped as
// the pane shrinks past it, so the list slides/clips out while collapsing instead of reflowing its
// search bar, tab switcher and grid into a squashed mess. Kept just below the smallest open split
// (1/3) so an open pane never clips.
private const val LIST_MIN_LAYOUT_FRACTION = 0.30f

// Drag release snaps to the nearest of these list fractions: collapsed, then the canonical 1/3, 1/2,
// 2/3 splits (M3 panes guidance). A single tap cycles through the non-collapsed splits.
private val LIST_FRACTION_ANCHORS = listOf(0f, 1f / 3f, 1f / 2f, 2f / 3f)
private val LIST_SPLIT_ANCHORS = listOf(1f / 3f, 1f / 2f, 2f / 3f)

private fun nearestAnchor(fraction: Float): Float =
    LIST_FRACTION_ANCHORS.minBy { abs(it - fraction) }

// Nearest of the *open* splits only (never collapsed) — used when reopening a collapsed list so a
// small drag-right settles to a real split instead of snapping back shut.
private fun nearestSplitAnchor(fraction: Float): Float =
    LIST_SPLIT_ANCHORS.minBy { abs(it - fraction) }

private fun nextSplitAnchor(fraction: Float): Float =
    LIST_SPLIT_ANCHORS.firstOrNull { it > fraction + 0.01f } ?: LIST_SPLIT_ANCHORS.first()

/**
 * Reusable Material 3 two-pane container for any feed→detail surface (Library, Discover, Feed,
 * Forum, Notifications). The caller supplies its list/feed via [listPane] (receiving an
 * `onItemClick`) and the detail content via [detailPane] (receiving the selected item and an
 * `onClose`). The selection is generic over [T] — usually the item's `Int` id, but any type works
 * (Notifications selects a heterogeneous `NotificationTarget`); pass a [selectionSaver] when [T] is
 * not auto-saveable so the open detail survives configuration changes.
 *
 * Behaviour (matching the M3 panes guidance):
 *  - The **list pane is permanent** and fills the full width while nothing is selected.
 *  - The **detail pane is temporary / on demand**: it opens when an item is tapped and is dismissed
 *    with the detail's close affordance or the system back gesture.
 *  - Both panes are **flexible** (weight-based) and **user-resizable** via a [VerticalDragHandle]:
 *    drag to resize (release snaps to the nearest 1/3 · 1/2 · 2/3 split or fully collapsed), **tap**
 *    to cycle the split, and **long-press** to collapse/expand the list pane (single ↔ two-pane).
 *    The chosen split is remembered across configuration changes.
 *
 * Intended to be rendered only on expanded widths; compact/medium use the plain full-screen screen.
 */
@Composable
fun <T : Any> TwoPaneListDetailScaffold(
    modifier: Modifier = Modifier,
    selectionSaver: Saver<T?, out Any> = autoSaver(),
    // Gutter inset around the panes while two are shown. Defaults to the rail-flush [TwoPaneDefaults]
    // padding (start = 0) for the tab surfaces that sit beside the navigation rail; standalone routes
    // with no rail (e.g. Notifications) should pass a symmetric inset so the list pane isn't flush to
    // the screen edge.
    gutterPadding: PaddingValues = TwoPaneDefaults.GutterPadding,
    // Optional Material 3 list-detail placeholder: when provided and nothing is selected, the detail
    // slot shows this empty state beside the list (at the persisted split) instead of letting the list
    // fill the full width. Pass it for list-style surfaces (Forum / Feed / Notifications); leave null
    // for grid feeds (Library / Discover) that should browse full-width until a selection.
    placeholderPane: (@Composable () -> Unit)? = null,
    // Receives the currently-open item (or null) so the list pane can show the Material 3 selected
    // state on it (two-pane only), plus the click callback that opens an item in the detail pane.
    listPane: @Composable (selected: T?, onItemClick: (T) -> Unit) -> Unit,
    detailPane: @Composable (selected: T, onClose: () -> Unit) -> Unit,
) {
    var selected by rememberSaveable(stateSaver = selectionSaver) { mutableStateOf<T?>(null) }
    val detail = selected

    // List pane fraction. rememberSaveable survives config change; the initial value is seeded from
    // (and resized splits are written back to) AppSettings so the chosen split also survives app close.
    val appSettings = LocalAppSettings.current
    var listFraction by rememberSaveable { mutableFloatStateOf(appSettings.paneListFraction.value) }
    var rowWidthPx by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    // The split at the moment a drag began, so release can tell "reopening from collapsed" apart from
    // a normal resize and avoid snapping a re-open attempt back shut.
    var dragStartFraction by remember { mutableFloatStateOf(0f) }
    // Smoothly animate the split to [target] (snap / tap / long-press); cancels any running settle.
    // Real split widths are persisted; the fully-collapsed state stays a transient per-session toggle.
    fun settleTo(target: Float) {
        if (target > COLLAPSE_THRESHOLD) appSettings.setPaneListFraction(target)
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(initialValue = listFraction, targetValue = target) { value, _ ->
                listFraction = value
            }
        }
    }

    // Closing the detail returns to the list. If the list was collapsed for detail viewing, restore
    // the persisted split so it reappears — and so its modifier never falls to weight(0) once the
    // placeholder/list takes the detail's place (RowScope.weight(0f) throws "invalid weight").
    fun closeDetail() {
        if (listFraction <= COLLAPSE_THRESHOLD) {
            settleJob?.cancel()
            val restored = appSettings.paneListFraction.value
            listFraction = if (restored > COLLAPSE_THRESHOLD) restored else DEFAULT_LIST_FRACTION
        }
        selected = null
    }

    BackHandler(enabled = detail != null) { closeDetail() }

    val resizeHandleLabel = stringResource(R.string.pane_resize_handle)
    val cycleLabel = stringResource(R.string.pane_resize_cycle)
    val toggleLabel = stringResource(R.string.pane_resize_toggle)

    val hasDetail = detail != null
    // On expanded widths a list-style caller shows an empty detail pane before a selection; grid
    // feeds (no placeholder) keep the list full-width instead.
    val showPlaceholder = !hasDetail && placeholderPane != null
    val twoPane = hasDetail || showPlaceholder
    val listMinWidth = with(LocalDensity.current) { (rowWidthPx * LIST_MIN_LAYOUT_FRACTION).toDp() }

    // The two-pane chrome (rounded panes on a surfaceContainer gutter) is the shared [TwoPaneDefaults]
    // primitive, but this scaffold keeps its own Row rather than [TwoPaneRow]: the list pane is held
    // in the layout at zero width while collapsed so its scroll state survives, which a generic
    // weight-based two-pane row can't express. Applied only while two panes show; a lone list keeps
    // its normal full-bleed look.
    Row(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (twoPane) {
                    Modifier
                        .background(TwoPaneDefaults.gutterColor)
                        .padding(gutterPadding)
                } else {
                    Modifier
                }
            )
            .onSizeChanged { rowWidthPx = it.width },
    ) {
        // Collapsed by fraction alone (a just-closed detail can momentarily leave it at ~0): the list
        // renders at width(0), never weight(0) — RowScope rejects a zero/negative weight.
        val isListCollapsed = listFraction <= COLLAPSE_THRESHOLD

        // List pane — fills the full width on its own, shrinks to [listFraction] when the detail
        // opens, and is kept in the layout at zero width when collapsed so its scroll state survives.
        val listModifier = when {
            !twoPane -> Modifier.weight(1f)
            isListCollapsed -> Modifier.width(0.dp)
            else -> Modifier.weight(listFraction)
        }.fillMaxHeight()
        if (twoPane) {
            Surface(modifier = listModifier, shape = TwoPaneDefaults.PaneShape, color = TwoPaneDefaults.paneColor) {
                // Hold the content at its open layout width and let the Surface clip it while the pane
                // shrinks past [LIST_MIN_LAYOUT_FRACTION], so collapsing slides/clips the list out
                // instead of reflowing its search bar + tab switcher into a squashed column.
                PaneSheetHost {
                    Box(modifier = Modifier.requiredWidthIn(min = listMinWidth).fillMaxSize()) {
                        listPane(selected) { item -> selected = item }
                    }
                }
            }
        } else {
            Box(modifier = listModifier.clipToBounds()) {
                listPane(selected) { item -> selected = item }
            }
        }

        if (detail != null) {
            // Drag handle — drag to resize, tap to cycle the split, long-press to collapse/expand.
            // [handleModifier] sets its placement: a full-height column between the cards while both
            // panes show, or a short centered pill while the list is collapsed (see below).
            val dragHandle: @Composable (Modifier) -> Unit = { handleModifier ->
                PaneDragHandle(
                    modifier = handleModifier,
                    onDelta = { delta ->
                        if (rowWidthPx > 0) {
                            listFraction = (listFraction + delta / rowWidthPx)
                                .coerceIn(0f, MAX_LIST_FRACTION)
                        }
                    },
                    onDragStarted = {
                        settleJob?.cancel()
                        dragStartFraction = listFraction
                    },
                    onDragStopped = {
                        // Reopening from (near) collapsed snaps to the nearest open split so a small
                        // drag-right reliably reveals the list; a normal resize may still settle to 0.
                        val reopening = dragStartFraction <= COLLAPSE_THRESHOLD &&
                            listFraction > COLLAPSE_THRESHOLD
                        settleTo(
                            if (reopening) nearestSplitAnchor(listFraction)
                            else nearestAnchor(listFraction)
                        )
                    },
                    onClick = {
                        settleTo(if (isListCollapsed) DEFAULT_LIST_FRACTION else nextSplitAnchor(listFraction))
                    },
                    onLongClick = {
                        settleTo(if (isListCollapsed) DEFAULT_LIST_FRACTION else 0f)
                    },
                    clickLabel = cycleLabel,
                    longClickLabel = toggleLabel,
                    resizeLabel = resizeHandleLabel,
                )
            }

            // One drag handle, always a full-height column in the gutter at the detail's leading edge.
            // Keeping it at a single stable position (rather than swapping between an overlaid pill when
            // collapsed and a gutter column when open) means an in-progress resize is never interrupted
            // as the list crosses the collapse boundary — the swap used to dispose the handle mid-drag
            // and drop the gesture, so re-opening felt stuck. While collapsed the list holds at zero
            // width, so the detail still fills the row (minus the thin handle column).
            dragHandle(Modifier.fillMaxHeight())

            Surface(
                modifier = Modifier
                    .weight(1f - listFraction)
                    .fillMaxHeight(),
                shape = TwoPaneDefaults.PaneShape,
                color = TwoPaneDefaults.paneColor,
            ) {
                PaneSheetHost {
                    detailPane(detail) { closeDetail() }
                }
            }
        } else if (showPlaceholder) {
            // Material 3 list-detail placeholder: list at the persisted split + an empty detail pane.
            // No drag handle here — resizing is a detail-mode affordance; the split returns on select.
            Spacer(Modifier.width(TwoPaneDefaults.PaneGap))
            Surface(
                modifier = Modifier
                    .weight(1f - listFraction)
                    .fillMaxHeight(),
                shape = TwoPaneDefaults.PaneShape,
                color = TwoPaneDefaults.paneColor,
            ) {
                placeholderPane!!()
            }
        }
    }
}

/**
 * Media specialization of [TwoPaneListDetailScaffold]: the detail pane is a [MediaDetailsScreen]
 * hosted in its own NavHost + [SharedTransitionLayout] so each gets a route-scoped ViewModel (its id
 * is read from the route's SavedStateHandle). Media→media (relations) navigates inside the pane;
 * everything else escalates to the app [navController]. Used by Library and Discover.
 */
@Composable
fun MediaListDetailScaffold(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    listPane: @Composable (selectedMediaId: Int?, onMediaClick: (Int) -> Unit) -> Unit,
) {
    TwoPaneListDetailScaffold(
        modifier = modifier,
        listPane = listPane,
        detailPane = { id, onClose ->
            PaneDetailHost(
                startRoute = MediaDetails(id, LIST_DETAIL_PANE_SOURCE),
                navController = navController,
                onClose = onClose,
            )
        },
    )
}

/**
 * Hosts a detail [startRoute] inside a two-pane detail slot as a self-contained mini-app: media →
 * character/staff/studio and every "see all" grid navigate WITHIN the pane ([mediaPaneGraph] on a
 * pane-scoped [NavHost]), so only the list pane stays put; cross-feature destinations escalate to the
 * app [navController] and open full screen. Selecting a new item (a new [startRoute]) navigates with a
 * cleared back stack so a FRESH entry + route-scoped ViewModel is created — reusing the entry would
 * keep the ViewModel bound to the previous selection. System back steps through the pane's own stack
 * first, then closes the pane.
 *
 * [extraGraph] registers destinations beyond the shared media graph (the Discover search pane adds a
 * user-profile root), receiving the pane [NavHostController] and the [SharedTransitionScope].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PaneDetailHost(
    startRoute: Any,
    navController: NavHostController,
    onClose: () -> Unit,
    extraGraph: NavGraphBuilder.(paneNav: NavHostController, sharedScope: SharedTransitionScope) -> Unit = { _, _ -> },
) {
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val paneNav = rememberNavController()

        // Material 3: a two-pane detail's ROOT shows a trailing close (✕), not a leading back arrow;
        // only an entry drilled into within the pane keeps its back arrow. The pane is at its root
        // while the nested nav sits at its start destination (nothing pushed on top).
        val currentPaneEntry by paneNav.currentBackStackEntryAsState()
        val paneIsRoot = currentPaneEntry == null || paneNav.previousBackStackEntry == null

        BackHandler(enabled = true) {
            if (!paneNav.popBackStack()) onClose()
        }

        var isFirstSelection by remember { mutableStateOf(true) }
        LaunchedEffect(startRoute) {
            if (isFirstSelection) {
                isFirstSelection = false
            } else {
                paneNav.navigate(startRoute) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        CompositionLocalProvider(LocalPaneIsRoot provides paneIsRoot) {
        NavHost(
            navController = paneNav,
            startDestination = remember { startRoute },
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeIn()
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeOut()
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeIn()
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeOut()
            },
        ) {
            mediaPaneGraph(
                paneNav = paneNav,
                navController = navController,
                sharedScope = this@SharedTransitionLayout,
                onClose = onClose,
            )
            extraGraph(paneNav, this@SharedTransitionLayout)
        }
        }
    }
}

/**
 * The media → character/staff/studio detail graph shared by every [PaneDetailHost] (the Library/
 * Discover browse pane and the Discover search pane). Drilling (relations, "see all" grids) stays in
 * [paneNav]; cross-feature destinations (reviews, threads, user profiles) escalate to the app
 * [navController]. [sharedScope] threads the shared-element scope down to each screen.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
internal fun NavGraphBuilder.mediaPaneGraph(
    paneNav: NavHostController,
    navController: NavHostController,
    sharedScope: SharedTransitionScope,
    onClose: () -> Unit,
) {
    composable<MediaDetails> { backStackEntry ->
        val route: MediaDetails = backStackEntry.toRoute()
        MediaDetailsScreen(
            mediaId = route.mediaId,
            sourceScreen = route.sourceScreen,
            // Back at the root closes the pane; deeper destinations pop within the pane. The nav icon
            // (✕ at the pane root, ← when drilled) comes from LocalPaneNavIcon, provided by the host.
            onBackClick = { if (!paneNav.popBackStack()) onClose() },
            onRelationClick = { relId -> paneNav.navigate(MediaDetails(relId, LIST_DETAIL_PANE_SOURCE)) },
            onCharacterClick = { paneNav.navigate(CharacterDetails(it)) },
            onStaffClick = { paneNav.navigate(StaffDetails(it)) },
            onStudioClick = { paneNav.navigate(StudioDetails(it)) },
            onRelatedSeeAllClick = { mId, t -> paneNav.navigate(MediaRelationsGrid(mId, t)) },
            onRecommendationsSeeAllClick = { mId, t ->
                paneNav.navigate(MediaRecommendationsGrid(mId, t))
            },
            onWriteReviewClick = { mId, t -> navController.navigate(WriteReview(mId, t)) },
            onDiscussionClick = { tId, tt -> navController.navigate(ForumThreadDetail(tId, tt)) },
            onViewAllDiscussions = { mId, t -> navController.navigate(ForumMediaThreads(mId, t)) },
            onStartDiscussion = { mId, t, cover ->
                navController.navigate(CreateThread(mId, t, cover.orEmpty()))
            },
            onUserClick = { navController.navigateSafely(UserProfile(it)) },
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = this,
        )
    }

    composable<CharacterDetails> { backStackEntry ->
        val route: CharacterDetails = backStackEntry.toRoute()
        CharacterDetailsScreen(
            characterId = route.characterId,
            onBackClick = { if (!paneNav.popBackStack()) onClose() },
            onMediaClick = { paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE)) },
            onMediaSeeAllClick = { cId, cName -> paneNav.navigate(CharacterMediaGrid(cId, cName)) },
            onStaffClick = { paneNav.navigate(StaffDetails(it)) },
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = this,
        )
    }

    composable<StaffDetails> { backStackEntry ->
        val route: StaffDetails = backStackEntry.toRoute()
        StaffDetailsScreen(
            staffId = route.staffId,
            onBackClick = { if (!paneNav.popBackStack()) onClose() },
            onMediaClick = { paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE)) },
            onCharacterClick = { paneNav.navigate(CharacterDetails(it)) },
            onMediaSeeAllClick = { sId, sName -> paneNav.navigate(StaffMediaGrid(sId, sName)) },
            onProductionSeeAllClick = { sId, sName ->
                paneNav.navigate(StaffProductionMediaGrid(sId, sName))
            },
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = this,
        )
    }

    composable<StudioDetails> { backStackEntry ->
        val route: StudioDetails = backStackEntry.toRoute()
        StudioDetailsScreen(
            studioId = route.studioId,
            onBackClick = { if (!paneNav.popBackStack()) onClose() },
            onMediaClick = { paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE)) },
            onMediaSeeAllClick = { sId, sName -> paneNav.navigate(StudioMediaGrid(sId, sName)) },
        )
    }

    composable<MediaRelationsGrid> { backStackEntry ->
        val route: MediaRelationsGrid = backStackEntry.toRoute()
        MediaRelationsGridScreen(
            mediaId = route.mediaId,
            mediaTitle = route.mediaTitle,
            onBackClick = { paneNav.popBackStack() },
            onRelationClick = { paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE)) },
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = this,
        )
    }

    composable<MediaRecommendationsGrid> { backStackEntry ->
        val route: MediaRecommendationsGrid = backStackEntry.toRoute()
        MediaRecommendationsGridScreen(
            mediaId = route.mediaId,
            mediaTitle = route.mediaTitle,
            onBackClick = { paneNav.popBackStack() },
            onRecommendationClick = { paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE)) },
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = this,
        )
    }

    composable<CharacterMediaGrid> { backStackEntry ->
        val route: CharacterMediaGrid = backStackEntry.toRoute()
        CharacterMediaGridScreen(
            characterId = route.characterId,
            characterName = route.characterName,
            onBackClick = { paneNav.popBackStack() },
            onMediaClick = { paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE)) },
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = this,
        )
    }

    composable<StaffMediaGrid> { backStackEntry ->
        val route: StaffMediaGrid = backStackEntry.toRoute()
        StaffMediaGridScreen(
            staffId = route.staffId,
            staffName = route.staffName,
            onBackClick = { paneNav.popBackStack() },
            onMediaClick = { paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE)) },
            onCharacterClick = { paneNav.navigate(CharacterDetails(it)) },
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = this,
        )
    }

    composable<StaffProductionMediaGrid> { backStackEntry ->
        val route: StaffProductionMediaGrid = backStackEntry.toRoute()
        StaffProductionMediaGridScreen(
            staffId = route.staffId,
            staffName = route.staffName,
            onBackClick = { paneNav.popBackStack() },
            onMediaClick = { paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE)) },
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = this,
        )
    }

    composable<StudioMediaGrid> { backStackEntry ->
        val route: StudioMediaGrid = backStackEntry.toRoute()
        StudioMediaGridScreen(
            studioId = route.studioId,
            studioName = route.studioName,
            onBackClick = { paneNav.popBackStack() },
            onMediaClick = { paneNav.navigate(MediaDetails(it, LIST_DETAIL_PANE_SOURCE)) },
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = this,
        )
    }
}

/**
 * The empty state shown in a two-pane detail slot before anything is selected (Material 3 list-detail
 * placeholder). Supplied by list-style callers via [TwoPaneListDetailScaffold]'s `placeholderPane`.
 */
@Composable
fun DetailPanePlaceholder(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
