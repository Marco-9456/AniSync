package com.anisync.android.presentation.details

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.MediaFollowingEntry
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.components.ConnectedToggleButtonGroup
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.ImageViewerDialog
import com.anisync.android.presentation.components.ReviewCard
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.details.components.ContentMetadataSection
import com.anisync.android.presentation.details.components.DetailsSkeletonContent
import com.anisync.android.presentation.details.components.ExpandableSynopsis
import com.anisync.android.presentation.details.components.ExternalLinksSection
import com.anisync.android.presentation.details.components.FollowingItem
import com.anisync.android.presentation.details.components.FollowingListSheet
import com.anisync.android.presentation.details.components.HorizontalInfoCards
import com.anisync.android.presentation.details.components.RecommendMediaSheet
import com.anisync.android.presentation.details.components.RecommendationItem
import com.anisync.android.presentation.details.components.RelationItem
import com.anisync.android.presentation.details.components.ReviewDetailsSheet
import com.anisync.android.presentation.details.components.ReviewsListSheet
import com.anisync.android.presentation.details.components.StaffItem
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.presentation.util.toIcon
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.util.getTitle


@OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun MediaDetailsScreen(
    mediaId: Int,
    sourceScreen: String = "unknown",
    onBackClick: () -> Unit,
    onRelationClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    onCastSeeAllClick: (Int, String) -> Unit = { _, _ -> },
    onStaffSeeAllClick: (Int, String) -> Unit = { _, _ -> },
    onRelatedSeeAllClick: (Int, String) -> Unit = { _, _ -> },
    onRecommendationsSeeAllClick: (Int, String) -> Unit = { _, _ -> },
    onWriteReviewClick: (Int, String) -> Unit = { _, _ -> },
    onDiscussionClick: (threadId: Int, threadTitle: String) -> Unit = { _, _ -> },
    onViewAllDiscussions: (mediaId: Int, mediaTitle: String) -> Unit = { _, _ -> },
    onStartDiscussion: (mediaId: Int, title: String, coverUrl: String?) -> Unit = { _, _, _ -> },
    onUserClick: (String) -> Unit = {},
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    viewModel: MediaDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()
    val showEditSheet by viewModel.showEditSheet.collectAsStateWithLifecycle()
    val draftEntry by viewModel.draftEntry.collectAsStateWithLifecycle()
    val userScoreFormat by viewModel.userScoreFormat.collectAsStateWithLifecycle()
    val animeCustomLists by viewModel.animeCustomLists.collectAsStateWithLifecycle()
    val mangaCustomLists by viewModel.mangaCustomLists.collectAsStateWithLifecycle()
    val following by viewModel.following.collectAsStateWithLifecycle()
    val hasMoreFollowing by viewModel.hasMoreFollowing.collectAsStateWithLifecycle()
    val discussions by viewModel.discussions.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    val spatialSpec = AppMotion.rememberSpatialSpec()
    val containerKey = TransitionKeys.container(sourceScreen, mediaId)
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    var shouldKeepChromeOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedDetailsReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToRelationDetails: (Int) -> Unit = remember(onRelationClick) {
        { relationMediaId ->
            shouldKeepChromeOverlayForReturn = true
            hasObservedDetailsReEnter = false
            onRelationClick(relationMediaId)
        }
    }

    val navigateToCharacterDetails: (Int) -> Unit = remember(onCharacterClick) {
        { characterId ->
            shouldKeepChromeOverlayForReturn = true
            hasObservedDetailsReEnter = false
            onCharacterClick(characterId)
        }
    }

    val navigateToStaffDetails: (Int) -> Unit = remember(onStaffClick) {
        { staffId ->
            shouldKeepChromeOverlayForReturn = true
            hasObservedDetailsReEnter = false
            onStaffClick(staffId)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // How far the content has scrolled under the app bar (0 = top, 1 = app bar fully opaque).
    // Drives both the status-bar scrim fade and the top-app-bar title cross-fade.
    val overlappedFraction by remember {
        derivedStateOf { scrollBehavior.state.overlappedFraction.coerceIn(0f, 1f) }
    }
    val isScrolled by remember { derivedStateOf { overlappedFraction > 0.01f } }

    // While the banner is showing behind a transparent app bar (Loading/Success, unscrolled), the
    // status-bar icons must stay white over the dark scrim regardless of theme — like the Play
    // Store. Once the app bar fades in (scrolled) or on an error page, revert to the theme default.
    val bannerVisible = uiState !is DetailsUiState.Error
    // Status-bar icon appearance follows the app theme (set globally): the banner no longer draws
    // under the status bar — a root status-bar scrim sits above it — so no per-screen override.

    val isDetailsEnteringFromBackStack by remember {
        derivedStateOf {
            animatedVisibilityScope.transition.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isDetailsTargetingVisible by remember {
        derivedStateOf {
            animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isDetailsFullyVisible by remember {
        derivedStateOf {
            animatedVisibilityScope.transition.currentState == EnterExitState.Visible &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isSharedTransitionRunning by remember {
        derivedStateOf { sharedTransitionScope.isTransitionActive }
    }
    val shouldRenderChromeInOverlay by remember {
        derivedStateOf {
            shouldKeepChromeOverlayForReturn &&
                isDetailsTargetingVisible &&
                (
                    isDetailsEnteringFromBackStack ||
                        (hasObservedDetailsReEnter && isSharedTransitionRunning)
                    )
        }
    }
    val chromeOverlayAlpha by animatedVisibilityScope.transition.animateFloat(label = "DetailsChromeOverlayAlpha") { state ->
        if (state == EnterExitState.Visible) 1f else 0f
    }

    LaunchedEffect(shouldKeepChromeOverlayForReturn, isDetailsEnteringFromBackStack) {
        if (shouldKeepChromeOverlayForReturn && isDetailsEnteringFromBackStack) {
            hasObservedDetailsReEnter = true
        }
    }

    LaunchedEffect(
        shouldKeepChromeOverlayForReturn,
        hasObservedDetailsReEnter,
        isDetailsFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepChromeOverlayForReturn &&
            hasObservedDetailsReEnter &&
            isDetailsFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepChromeOverlayForReturn = false
            hasObservedDetailsReEnter = false
        }
    }

    val statuses = remember {
        listOf(
            LibraryStatus.CURRENT,
            LibraryStatus.PLANNING,
            LibraryStatus.COMPLETED,
            LibraryStatus.PAUSED,
            LibraryStatus.DROPPED
        )
    }

    with(sharedTransitionScope) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
                topBar = {
                    val state = uiState

                    val appBarTitle = remember(state, titleLanguage) {
                        (state as? DetailsUiState.Success)?.details?.getTitle(titleLanguage) ?: ""
                    }

                    with(sharedTransitionScope) {
                        TopAppBar(
                            modifier = Modifier
                                .renderInSharedTransitionScopeOverlay(
                                    zIndexInOverlay = 1f,
                                    renderInOverlay = { shouldRenderChromeInOverlay }
                                )
                                .graphicsLayer {
                                    alpha =
                                        if (shouldRenderChromeInOverlay) chromeOverlayAlpha else 1f
                                },
                            title = {
                                AnimatedVisibility(
                                    visible = isScrolled,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Text(
                                        text = appBarTitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = navigationIcon,
                                        contentDescription = stringResource(R.string.back),
                                        tint = animateColorAsState(
                                            if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
                                            label = "navIconTint"
                                        ).value
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            scrollBehavior = scrollBehavior,
                            // Root already insets below the status bar; don't add it again here.
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        )
                    }
                },
                floatingActionButton = {
                    val state = uiState
                    if (state is DetailsUiState.Success) {
                        val details = state.details
                        val haptic = rememberHapticFeedback()
                        // The whole details content Box uses sharedBounds, so during a
                        // transition it renders in the shared-element overlay (on top).
                        // The FAB must be lifted into that same overlay at a higher
                        // zIndex while THIS screen is animating in, otherwise it's drawn
                        // behind the page until the transition ends — a square stub
                        // poking out from under the content (forward enter included).
                        //
                        // Gate on the screen *targeting visible* so the FAB only lifts
                        // while entering/returning, never while exiting. isTransitionActive
                        // is scope-global: without the targeting-visible guard, the FAB of
                        // the screen being navigated AWAY from also lifts into the overlay
                        // and floats over the destination (another detail page, a cast/
                        // staff grid, ...) as a stray FAB.
                        val fabOverlayModifier = with(sharedTransitionScope) {
                            Modifier
                                .renderInSharedTransitionScopeOverlay(
                                    zIndexInOverlay = 1f,
                                    renderInOverlay = {
                                        (isSharedTransitionRunning && isDetailsTargetingVisible) ||
                                                shouldRenderChromeInOverlay
                                    }
                                )
                                .graphicsLayer {
                                    alpha =
                                        if (shouldRenderChromeInOverlay) chromeOverlayAlpha else 1f
                                }
                        }

                        BackHandler(enabled = fabMenuExpanded) {
                            fabMenuExpanded = false
                        }

                        // Library Management FAB
                        FloatingActionButtonMenu(
                            expanded = fabMenuExpanded,
                            modifier = fabOverlayModifier
                                .padding(dimensionResource(R.dimen.fab_menu_padding)),
                            button = {
                                ToggleFloatingActionButton(
                                    checked = fabMenuExpanded,
                                    onCheckedChange = { fabMenuExpanded = !fabMenuExpanded }
                                ) {
                                    val imageVector = if (checkedProgress > 0.5f) Icons.Filled.Close
                                        else if (details.listEntryId != null) Icons.Filled.Edit
                                        else Icons.Filled.Add
                                    Icon(
                                        painter = rememberVectorPainter(imageVector),
                                        contentDescription = if (fabMenuExpanded) stringResource(R.string.fab_close_menu) else stringResource(
                                            R.string.fab_open_menu_library
                                        ),
                                        modifier = Modifier.animateIcon({ checkedProgress })
                                    )
                                }
                            }
                        ) {
                            statuses.forEach { status ->
                                val isSelected = status == details.listStatus
                                val statusLabel = status.toLabel(details.type)
                                FloatingActionButtonMenuItem(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        viewModel.saveMediaListEntry(
                                            status,
                                            details.listProgress ?: 0
                                        )
                                        fabMenuExpanded = false
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = status.toIcon(details.type),
                                            contentDescription = statusLabel,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = statusLabel,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            }
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    viewModel.openEditSheet()
                                    fabMenuExpanded = false
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.label_custom),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = {
                                    Text(
                                        text = stringResource(R.string.label_custom),
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            if (details.listEntryId != null) {
                                FloatingActionButtonMenuItem(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        viewModel.deleteMediaListEntry()
                                        fabMenuExpanded = false
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            stringResource(R.string.a11y_remove_from_library),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    text = {
                                        Text(
                                            stringResource(R.string.action_remove),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    state = pullToRefreshState,
                    onRefresh = rememberRateLimitedRefresh { viewModel.refresh() },
                    indicator = {
                        CustomPullToRefreshIndicator(
                            isRefreshing = isRefreshing,
                            state = pullToRefreshState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .windowInsetsPadding(WindowInsets.statusBars)
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Note: We intentionally IGNORE top padding here to let the content
                            // (specifically the header image) draw behind the transparent status bar.
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = containerKey),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> spatialSpec },
                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp))
                            )
                    ) {
                        when (val state = uiState) {
                            is DetailsUiState.Loading -> DetailsSkeletonContent(onBackClick = onBackClick)
                        is DetailsUiState.Success -> {
                            val context = LocalContext.current
                            DetailsPageContent(
                                details = state.details,
                                sourceScreen = sourceScreen,
                                listState = listState,
                                following = following,
                                hasMoreFollowing = hasMoreFollowing,
                                onRelationClick = navigateToRelationDetails,
                                onCharacterClick = navigateToCharacterDetails,
                                onStaffClick = navigateToStaffDetails,
                                onStudioClick = onStudioClick,
                                onCastSeeAllClick = {
                                    shouldKeepChromeOverlayForReturn = true
                                    hasObservedDetailsReEnter = false
                                    onCastSeeAllClick(
                                        state.details.id,
                                        state.details.getTitle(titleLanguage)
                                    )
                                },
                                onStaffSeeAllClick = {
                                    shouldKeepChromeOverlayForReturn = true
                                    hasObservedDetailsReEnter = false
                                    onStaffSeeAllClick(
                                        state.details.id,
                                        state.details.getTitle(titleLanguage)
                                    )
                                },
                                onRelatedSeeAllClick = {
                                    shouldKeepChromeOverlayForReturn = true
                                    hasObservedDetailsReEnter = false
                                    onRelatedSeeAllClick(
                                        state.details.id,
                                        state.details.getTitle(titleLanguage)
                                    )
                                },
                                onRecommendationsSeeAllClick = {
                                    shouldKeepChromeOverlayForReturn = true
                                    hasObservedDetailsReEnter = false
                                    onRecommendationsSeeAllClick(
                                        state.details.id,
                                        state.details.getTitle(titleLanguage)
                                    )
                                },
                                onWriteReviewClick = {
                                    onWriteReviewClick(
                                        state.details.id,
                                        state.details.getTitle(titleLanguage)
                                    )
                                },
                                discussions = discussions,
                                onDiscussionClick = { threadId, threadTitle ->
                                    shouldKeepChromeOverlayForReturn = true
                                    hasObservedDetailsReEnter = false
                                    onDiscussionClick(threadId, threadTitle)
                                },
                                onViewAllDiscussions = {
                                    shouldKeepChromeOverlayForReturn = true
                                    hasObservedDetailsReEnter = false
                                    onViewAllDiscussions(
                                        state.details.id,
                                        state.details.getTitle(titleLanguage)
                                    )
                                },
                                onStartDiscussion = {
                                    onStartDiscussion(
                                        state.details.id,
                                        state.details.getTitle(titleLanguage),
                                        state.details.coverUrl
                                    )
                                },
                                onRecommendMedia = viewModel::recommendMedia,
                                onUserClick = onUserClick,
                                onFavouriteClick = viewModel::toggleFavourite,
                                onShareClick = { viewModel.shareMedia(context) },
                                onRateReview = viewModel::rateReview,
                                onRateRecommendation = viewModel::rateRecommendation,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                titleLanguage = titleLanguage
                            )
                        }

                        is DetailsUiState.Error -> ErrorStateContent(
                            message = state.message,
                            onBackClick = onBackClick
                        )
                        }
                    }
                }
            }

            // Play-Store-style scrim behind the transparent status bar so the system clock /
            // battery / back arrow stay legible over a bright banner. Fades out as the opaque
            // app bar scrolls in, and is suppressed on the error page (no banner there).
            if (bannerVisible) {
                StatusBarScrim(
                    alpha = 1f - overlappedFraction,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        if (showEditSheet) {
            draftEntry?.let { entry ->
                val details = (uiState as? DetailsUiState.Success)?.details
                val mediaType = details?.type ?: entry.type
                val availableLists = when (mediaType) {
                    com.anisync.android.type.MediaType.ANIME -> animeCustomLists
                    com.anisync.android.type.MediaType.MANGA -> mangaCustomLists
                    else -> emptyList()
                }
                
                com.anisync.android.presentation.library.components.EditLibraryEntrySheet(
                    entry = entry,
                    titleLanguage = titleLanguage,
                    scoreFormat = userScoreFormat,
                    availableCustomLists = availableLists,
                    onDismiss = viewModel::closeEditSheet,
                    onSave = viewModel::saveLibraryEntry,
                    onDelete = {
                        viewModel.deleteMediaListEntry()
                        viewModel.closeEditSheet()
                    }
                )
            }
        }
    }
}

@Composable
fun ErrorStateContent(message: String, onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large))
        ) {
            Icon(
                Icons.Default.Delete,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.error_something_went_wrong),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onBackClick) { Text(stringResource(R.string.action_go_back)) }
        }
    }
}

/**
 * Compact, read-only preview row for a discussion thread in the media-detail
 * Discussions section. Full thread actions live on the dedicated media-threads
 * screen (reached via "View all").
 */
@Composable
private fun DiscussionPreviewRow(
    thread: ForumThread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = thread.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    UserAvatar(
                        url = thread.authorAvatarUrl,
                        contentDescription = thread.authorName,
                        size = 28.dp
                    )
                    Text(
                        text = thread.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DiscussionStat(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        value = thread.replyCount
                    )
                    DiscussionStat(icon = Icons.Outlined.FavoriteBorder, value = thread.likeCount)
                }
            }
        }
    }
}

@Composable
private fun DiscussionStat(icon: ImageVector, value: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Top-level grouping of the media-detail page. The header (cover/banner/title) and the
 * favorite/share row sit above the tab bar; everything else lives under one of these tabs so the
 * page reads as a few focused screens instead of one long scroll.
 */
enum class DetailsTab(@StringRes val titleRes: Int) {
    OVERVIEW(R.string.details_tab_overview),
    CHARACTERS(R.string.details_tab_characters),
    SOCIAL(R.string.details_tab_social)
}

private fun detailsTabIcon(tab: DetailsTab): ImageVector = when (tab) {
    DetailsTab.OVERVIEW -> Icons.Outlined.Info
    DetailsTab.CHARACTERS -> Icons.Default.Group
    DetailsTab.SOCIAL -> Icons.Default.Forum
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailsTabsButtonGroup(
    tabs: List<DetailsTab>,
    selectedTab: DetailsTab,
    onTabSelected: (DetailsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    ConnectedToggleButtonGroup(
        options = tabs,
        selected = selectedTab,
        onSelect = onTabSelected,
        label = { stringResource(it.titleRes) },
        modifier = modifier.padding(vertical = 8.dp),
        icon = ::detailsTabIcon
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailsPageContent(
    details: MediaDetails,
    sourceScreen: String,
    listState: LazyListState,
    following: List<MediaFollowingEntry>,
    hasMoreFollowing: Boolean,
    onRelationClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onCastSeeAllClick: () -> Unit,
    onStaffSeeAllClick: () -> Unit,
    onRelatedSeeAllClick: () -> Unit,
    onRecommendationsSeeAllClick: () -> Unit,
    onWriteReviewClick: () -> Unit,
    discussions: List<ForumThread>,
    onDiscussionClick: (Int, String) -> Unit,
    onViewAllDiscussions: () -> Unit,
    onStartDiscussion: () -> Unit,
    onRecommendMedia: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    onFavouriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onRateReview: (Int, com.anisync.android.type.ReviewRating) -> Unit,
    onRateRecommendation: (Int, com.anisync.android.type.RecommendationRating) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    titleLanguage: TitleLanguage
) {
    val displayCharacters = remember(details.characters, titleLanguage) {
        details.characters.distinctBy { it.id }.take(10).map { character ->
            character.copy(
                nameUserPreferred = when (titleLanguage) {
                    TitleLanguage.ROMAJI -> character.nameUserPreferred
                    TitleLanguage.ENGLISH -> character.nameUserPreferred
                    TitleLanguage.NATIVE -> character.nameNative ?: character.nameUserPreferred
                }
            )
        }
    }

    val displayRelations = remember(details.relations, titleLanguage) {
        details.relations.take(10)
            .distinctBy { "${it.id}_${it.relationType}" }
            .map { relation -> relation.copy(titleUserPreferred = relation.getTitle(titleLanguage)) }
    }

    val displayRecommendations = remember(details.recommendations) {
        details.recommendations.filter { it.rating > 0 }.distinctBy { it.id }.take(10)
    }

    val displayReviews = remember(details.reviews) {
        details.reviews.distinctBy { it.id }.take(5)
    }

    // ImageViewerDialog state for cover/banner image
    var showImageViewer by rememberSaveable { mutableStateOf(false) }
    val viewerImages = remember(details.cover.url() ?: details.coverUrl, details.bannerUrl) {
        listOfNotNull(details.coverUrl, details.bannerUrl)
    }

    var selectedReview by remember { mutableStateOf<MediaReview?>(null) }
    var showAllReviewsSheet by remember { mutableStateOf(false) }
    var showAllFollowingSheet by remember { mutableStateOf(false) }
    var showRecommendSheet by remember { mutableStateOf(false) }

    val displayStaff = remember(details.staff) {
        details.staff.distinctBy { it.id }.take(10)
    }

    var selectedTab by rememberSaveable { mutableStateOf(DetailsTab.OVERVIEW) }

    // Only surface tabs that have something to show. Overview is always present; Social always
    // offers the "start discussion" affordance, so neither ever collapses.
    val availableTabs = remember(displayCharacters, displayStaff) {
        buildList {
            add(DetailsTab.OVERVIEW)
            if (displayCharacters.isNotEmpty() || displayStaff.isNotEmpty()) {
                add(DetailsTab.CHARACTERS)
            }
            add(DetailsTab.SOCIAL)
        }
    }

    // If the active tab disappears (data changed underneath), fall back to Overview.
    LaunchedEffect(availableTabs) {
        if (selectedTab !in availableTabs) selectedTab = DetailsTab.OVERVIEW
    }

    // Sticky tabs — a single interactive tab strip that tracks the scroll continuously: it rides up
    // with its in-list slot, then clamps under the app bar (status bar inset + 64dp). The strip is an
    // overlay (below); the in-list item just reserves its slot. The background is the opaque page
    // colour (like the Profile tab bar) so it blends in inline at rest and cleanly covers content once
    // docked — no fade, so a fast fling can't catch a half-transparent strip with content showing
    // through.
    val density = LocalDensity.current
    // Just the app bar height — the root insets content below the status bar, so the sticky tab
    // strip docks under the 64dp bar without adding the status-bar inset.
    val appBarHeightPx = with(density) { 64.dp.roundToPx() }
    val spacerLargePx = with(density) { dimensionResource(R.dimen.spacing_large).roundToPx() }
    var tabStripHeightPx by remember { mutableIntStateOf(0) }
    val tabStripTranslationY by remember {
        derivedStateOf {
            val info = listState.layoutInfo.visibleItemsInfo.find { it.key == "tabs" }
            if (info == null) {
                // Slot scrolled above the viewport top → fully docked under the app bar.
                appBarHeightPx.toFloat()
            } else {
                maxOf(appBarHeightPx, info.offset + spacerLargePx).toFloat()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Sufficient padding for FAB
        ) {
            // Header (Cover, Banner, Title) — always visible above the tabs
            item(key = "header") {
                PageHeaderSection(
                    details = details,
                    sourceScreen = sourceScreen,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    titleLanguage = titleLanguage,
                    onCoverClick = { showImageViewer = true }
                )
            }

            // Action Row (Favorite / Share) — always visible above the tabs
            item(key = "action_buttons") {
                Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                    SmartActionButtonsRow(
                        isFavorite = details.isFavourite,
                        onFavoriteClick = onFavouriteClick,
                        onShareClick = onShareClick
                    )
                }
            }

            // Section selector slot — reserves the tab strip's footprint in the scroll. The strip
            // itself is the pinned overlay below, positioned to track this slot then dock.
            item(key = "tabs") {
                Spacer(
                    modifier = Modifier.height(
                        dimensionResource(R.dimen.spacing_large) +
                            with(density) { tabStripHeightPx.toDp() }
                    )
                )
            }

            when (selectedTab) {
                DetailsTab.OVERVIEW -> {
                    // Information (Info Cards)
                    item(key = "info_cards") {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                            HorizontalInfoCards(details = details, onStudioClick = onStudioClick)
                        }
                    }

                    // Synopsis
                    item(key = "synopsis") {
                        Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))) {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                            ExpandableSynopsis(details.description)
                        }
                    }

                    // Categories (Genres & Tags)
                    item(key = "metadata") {
                        if (details.tags.isNotEmpty() || details.genres.isNotEmpty()) {
                            Column {
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                                ContentMetadataSection(genres = details.genres, tags = details.tags)
                            }
                        }
                    }

                    // External & Streaming Links
                    item(key = "external_links") {
                        if (details.externalLinks.isNotEmpty()) {
                            Column {
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                                ExternalLinksSection(
                                    externalLinks = details.externalLinks,
                                    mediaType = details.type
                                )
                            }
                        }
                    }

                    // Related (Relations)
                    if (displayRelations.isNotEmpty()) {
                        item(key = "relations") {
                            Column {
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                                SectionHeader(
                                    title = stringResource(R.string.section_related),
                                    level = HeaderLevel.Section,
                                    onActionClick = if (details.relations.size > 10) onRelatedSeeAllClick else null
                                )
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_normal)),
                                    modifier = Modifier.height(dimensionResource(R.dimen.character_item_height))
                                ) {
                                    items(
                                        items = displayRelations,
                                        key = { "${it.id}_${it.relationType}" }) { relation ->
                                        RelationItem(
                                            relation = relation,
                                            onClick = { onRelationClick(relation.id) },
                                            transitionPrefix = TransitionKeys.MEDIA_DETAILS,
                                            modifier = Modifier.animateItem(), // Expressive motion
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recommendations
                    item(key = "recommendations") {
                        val canRecommend = details.isRecommendationBlocked != true
                        val hasMoreRecommendations = remember(details.recommendations) {
                            details.recommendations.distinctBy { it.id }.size > 10
                        }
                        if (displayRecommendations.isNotEmpty() || canRecommend) {
                            Column {
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                                SectionHeader(
                                    title = stringResource(R.string.section_recommendations),
                                    level = HeaderLevel.Section,
                                    onActionClick = if (hasMoreRecommendations) onRecommendationsSeeAllClick else null,
                                    trailingContent = if (canRecommend) {
                                        {
                                            IconButton(onClick = { showRecommendSheet = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = stringResource(R.string.cd_add_recommendation),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    } else null
                                )
                                if (displayRecommendations.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_normal)),
                                        modifier = Modifier.height(240.dp)
                                    ) {
                                        items(
                                            items = displayRecommendations,
                                            key = { "rec_${it.id}" }
                                        ) { recommendation ->
                                            RecommendationItem(
                                                recommendation = recommendation,
                                                onClick = { onRelationClick(recommendation.id) },
                                                onRate = { isUpvote ->
                                                    val rating = when {
                                                        isUpvote && recommendation.userRating == "RATE_UP" ->
                                                            com.anisync.android.type.RecommendationRating.NO_RATING
                                                        isUpvote ->
                                                            com.anisync.android.type.RecommendationRating.RATE_UP
                                                        !isUpvote && recommendation.userRating == "RATE_DOWN" ->
                                                            com.anisync.android.type.RecommendationRating.NO_RATING
                                                        else ->
                                                            com.anisync.android.type.RecommendationRating.RATE_DOWN
                                                    }
                                                    onRateRecommendation(recommendation.id, rating)
                                                },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                DetailsTab.CHARACTERS -> {
                    // Cast
                    if (displayCharacters.isNotEmpty()) {
                        item(key = "cast") {
                            Column {
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                                SectionHeader(
                                    title = stringResource(R.string.section_cast),
                                    level = HeaderLevel.Section,
                                    onActionClick = if (details.characters.size > 10) onCastSeeAllClick else null
                                )
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
                                    modifier = Modifier.height(dimensionResource(R.dimen.character_item_height))
                                ) {
                                    items(items = displayCharacters, key = { "character_${it.id}" }) { character ->
                                        CharacterItem(
                                            character = character,
                                            onClick = { onCharacterClick(character.id) },
                                            modifier = Modifier.animateItem(), // Expressive motion
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Staff
                    if (displayStaff.isNotEmpty()) {
                        item(key = "staff") {
                            Column {
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                                SectionHeader(
                                    title = stringResource(R.string.section_staff),
                                    level = HeaderLevel.Section,
                                    onActionClick = if (details.staff.size > 10) onStaffSeeAllClick else null
                                )
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
                                    modifier = Modifier.height(dimensionResource(R.dimen.character_item_height))
                                ) {
                                    items(items = displayStaff, key = { "staff_${it.id}" }) { staff ->
                                        StaffItem(
                                            staff = staff,
                                            onClick = { onStaffClick(staff.id) },
                                            modifier = Modifier.animateItem(),
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DetailsTab.SOCIAL -> {
                    // Following — list of followed users' status for this media
                    if (following.isNotEmpty()) {
                        item(key = "following") {
                            Column {
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                                SectionHeader(
                                    title = stringResource(R.string.section_following),
                                    level = HeaderLevel.Section,
                                    onActionClick = if (hasMoreFollowing) { { showAllFollowingSheet = true } } else null
                                )
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
                                ) {
                                    items(items = following, key = { "follow_${it.userId}" }) { entry ->
                                        FollowingItem(
                                            entry = entry,
                                            mediaType = details.type,
                                            onClick = { onUserClick(entry.userName) },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reviews
                    val canReview = details.isReviewBlocked != true
                    if (displayReviews.isNotEmpty() || canReview) {
                        item(key = "reviews_header") {
                            Column {
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                                SectionHeader(
                                    title = stringResource(R.string.section_reviews),
                                    level = HeaderLevel.Section,
                                    onActionClick = if (displayReviews.size >= 5) { { showAllReviewsSheet = true } } else null,
                                    trailingContent = if (canReview) {
                                        {
                                            IconButton(onClick = onWriteReviewClick) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = stringResource(R.string.cd_write_review),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    } else null
                                )
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            }
                        }
                        items(
                            items = displayReviews,
                            key = { "review_${it.id}" }
                        ) { review ->
                            ReviewCard(
                                review = review,
                                onClick = { selectedReview = review },
                                onUserClick = onUserClick,
                                showBanner = false,
                                modifier = Modifier
                                    .padding(horizontal = dimensionResource(R.dimen.spacing_large))
                                    .padding(bottom = dimensionResource(R.dimen.spacing_normal))
                                    .animateItem()
                            )
                        }
                    }

                    // Discussions — forum threads with this media as a mediaCategory
                    item(key = "discussions_header") {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                            SectionHeader(
                                title = stringResource(R.string.forum_discussions_title),
                                level = HeaderLevel.Section,
                                onActionClick = if (discussions.isNotEmpty()) onViewAllDiscussions else null,
                                trailingContent = {
                                    IconButton(onClick = onStartDiscussion) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.forum_discussions_start),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            if (discussions.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.forum_discussions_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
                                )
                                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            }
                        }
                    }
                    items(items = discussions, key = { "discussion_${it.id}" }) { thread ->
                        DiscussionPreviewRow(
                            thread = thread,
                            onClick = { onDiscussionClick(thread.id, thread.title) },
                            modifier = Modifier
                                .padding(horizontal = dimensionResource(R.dimen.spacing_large))
                                .padding(bottom = dimensionResource(R.dimen.spacing_normal))
                                .animateItem()
                        )
                    }
                }
            }
        }

        // The one interactive tab strip, lifted into an overlay so it can ride the scroll and dock
        // under the app bar. translationY tracks the in-list slot then clamps under the app bar. The
        // background is the opaque page colour so content scrolls cleanly underneath while docked.
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = tabStripTranslationY }
                .onSizeChanged { tabStripHeightPx = it.height }
        ) {
            DetailsTabsButtonGroup(
                tabs = availableTabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }

    // Sync selectedReview with the latest data from details.reviews when API refreshed
    LaunchedEffect(details.reviews) {
        selectedReview?.let { current ->
            val updated = details.reviews.find { it.id == current.id }
            if (updated != null && updated != current) {
                selectedReview = updated
            }
        }
    }

    selectedReview?.let { review ->
        ReviewDetailsSheet(
            review = review,
            onRateReview = { reviewId, rating ->
                onRateReview(reviewId, rating)
                // Optimistic UI update
                val oldRating = review.userRating
                val newRatingStr = if (rating.name == "NO_VOTE") null else rating.name

                var updatedRating = review.rating
                var updatedAmount = review.ratingAmount

                // Remove old vote effect
                when (oldRating) {
                    "UP_VOTE" -> updatedRating -= 1
                    "DOWN_VOTE" -> updatedRating += 1
                }
                // Remove old vote from total if transitioning to NO_VOTE
                if (oldRating != null && newRatingStr == null) updatedAmount -= 1
                // Add new vote to total if transitioning from NO_VOTE
                if (oldRating == null && newRatingStr != null) updatedAmount += 1

                // Add new vote effect
                when (newRatingStr) {
                    "UP_VOTE" -> updatedRating += 1
                    "DOWN_VOTE" -> updatedRating -= 1
                }

                selectedReview = review.copy(
                    userRating = newRatingStr,
                    rating = updatedRating,
                    ratingAmount = updatedAmount
                )
            },
            onUserClick = onUserClick,
            onDismiss = { selectedReview = null }
        )
    }

    if (showAllReviewsSheet) {
        ReviewsListSheet(
            mediaId = details.id,
            onDismiss = { showAllReviewsSheet = false },
            onReviewClick = { selectedReview = it },
            onUserClick = onUserClick
        )
    }

    if (showAllFollowingSheet) {
        FollowingListSheet(
            mediaId = details.id,
            mediaType = details.type,
            onDismiss = { showAllFollowingSheet = false },
            onUserClick = { username ->
                showAllFollowingSheet = false
                onUserClick(username)
            }
        )
    }

    if (showImageViewer) {
        ImageViewerDialog(
            imageUrls = viewerImages,
            initialIndex = 0,
            onDismiss = { showImageViewer = false }
        )
    }

    if (showRecommendSheet) {
        details.type?.let { mediaType ->
            RecommendMediaSheet(
                mediaType = mediaType,
                sourceMediaId = details.id,
                onRecommend = onRecommendMedia,
                onDismiss = { showRecommendSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageHeaderSection(
    details: MediaDetails,
    sourceScreen: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    titleLanguage: TitleLanguage,
    onCoverClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // --- Transition Keys ---
    val coverKey = remember(details.id) { TransitionKeys.cover(sourceScreen, details.id) }
    val titleKey = remember(details.id) { TransitionKeys.title(sourceScreen, details.id) }
    val coverQuality = com.anisync.android.domain.LocalCoverQuality.current
    val cacheKey = remember(details.id, coverQuality) {
        TransitionKeys.imageCacheKey(sourceScreen, details.id) + "-" + coverQuality.name
    }

    // --- UI Constants ---
    val spatialSpec = AppMotion.rememberSpatialSpec()
    val coverShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    val themeBackground = MaterialTheme.colorScheme.background

    // --- Data Preparation ---
    val displayTitle = remember(details, titleLanguage) { details.getTitle(titleLanguage) }
    val formattedScore =
        remember(details.score) { details.score?.let { String.format("%.1f", it / 10f) } }

    // Resolve the best image to show in the banner
    // Priority: Trailer Thumbnail -> Banner -> Cover
    val bannerModel = remember(details) {
        details.trailer?.thumbnail ?: details.bannerUrl ?: details.coverUrl
    }

    // Calculate if we need a custom crop ratio
    // Standard YouTube thumbnails (hqdefault) usually have black letterbox bars for 16:9 content.
    val needsZoom = remember(details, bannerModel) {
        bannerModel == details.trailer?.thumbnail
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        // 1. Banner Image Layer
        BannerImage(
            model = bannerModel,
            needsZoom = needsZoom,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        // 2. Trailer Button Overlay
        // Only show if we have a valid YouTube trailer
        if (details.trailer?.site == "youtube" && details.trailer.id != null) {
            TrailerPlayButton(
                onClick = {
                    try {
                        uriHandler.openUri("https://www.youtube.com/watch?v=${details.trailer.id}")
                    } catch (_: Exception) {
                        // Handle no browser installed if necessary
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    // adjust alignment slightly up since the banner is only top 240dp
                    .padding(bottom = 100.dp)
            )
        }

        // 3. Gradient Overlays (Visual integration)
        BannerGradients(themeBackground = themeBackground)

        // 4. Content Row (Cover + Title + Metadata)
        ContentRow(
            details = details,
            displayTitle = displayTitle,
            formattedScore = formattedScore,
            coverKey = coverKey,
            titleKey = titleKey,
            cacheKey = cacheKey,
            coverShape = coverShape,
            spatialSpec = spatialSpec,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onCoverClick = onCoverClick
        )
    }
}

@Composable
private fun BannerImage(
    model: Any?,
    needsZoom: Boolean,
    modifier: Modifier = Modifier
) {
    val contentScale = remember(needsZoom) {
        if (needsZoom) {
            object : ContentScale {
                override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
                    if (srcSize.width == 0f || srcSize.height == 0f) return ScaleFactor(1f, 1f)
                    // YouTube standard thumbnails (hqdefault.jpg) are 4:3 with 16:9 content letterboxed (black bars).
                    // We calculate the exact mathematical scale needed to fit the 16:9 content cleanly
                    // into the destination bounds, completely avoiding arbitrary zooming or stretching.
                    val contentHeight = srcSize.width * (9f / 16f)
                    val widthScale = dstSize.width / srcSize.width
                    val heightScale = dstSize.height / contentHeight

                    // Use maxOf to ensure it fills the space completely, exactly like ContentScale.Crop
                    val scale = maxOf(widthScale, heightScale)
                    return ScaleFactor(scale, scale)
                }
            }
        } else {
            ContentScale.Crop
        }
    }

    Box(modifier = modifier.clip(RectangleShape)) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = contentScale,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
private fun BannerGradients(themeBackground: Color) {
    val bottomGradient = remember(themeBackground) {
        Brush.verticalGradient(colors = listOf(Color.Transparent, themeBackground))
    }

    Box(
        modifier = Modifier
            .fillMaxSize() // Fill the 340dp container to align relative to it
    ) {
        // Gradient transitioning from image to solid background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp) // Starts fading before the solid block
                .background(bottomGradient)
        )
        // Solid background block for the text area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(themeBackground)
        )
    }
}

/**
 * Dark top-to-transparent gradient pinned behind the status bar, mirroring the Google Play Store
 * detail page. Keeps the white system icons (and back arrow) readable over a bright banner while
 * the app bar is transparent; [alpha] is driven to 0 as the opaque app bar scrolls in.
 */
@Composable
private fun StatusBarScrim(alpha: Float, modifier: Modifier = Modifier) {
    // Root insets below the status bar, so this banner scrim only needs its own short height.
    val scrimHeight = 28.dp
    val scrimBrush = remember {
        Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(scrimHeight)
            .graphicsLayer { this.alpha = alpha }
            .background(scrimBrush)
    )
}

@Composable
private fun TrailerPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = R.string.action_play_trailer.toString(),
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ContentRow(
    details: MediaDetails,
    displayTitle: String?,
    formattedScore: String?,
    coverKey: Any,
    titleKey: Any,
    cacheKey: Any,
    coverShape: RoundedCornerShape,
    spatialSpec: FiniteAnimationSpec<Rect>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCoverClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coverData = details.cover.url() ?: details.coverUrl
    val coverImageRequest = remember(coverData, cacheKey) {
        ImageRequest.Builder(context)
            .data(coverData)
            .crossfade(true)
            .placeholderMemoryCacheKey(cacheKey.toString()) // Ensure key is string if needed
            .memoryCacheKey(cacheKey.toString())
            .build()
    }
    val copyToClipboard = rememberCopyToClipboard()
    val copyLongClickLabel = stringResource(R.string.a11y_action_copy)
    val titleClipLabel = stringResource(R.string.clip_label_title)
    val copiedTitleMessage = stringResource(R.string.copied_title)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
        verticalAlignment = Alignment.Bottom
    ) {
        // Cover Image with Shared Transition
        with(sharedTransitionScope) {
            Card(
                modifier = Modifier
                    .width(115.dp)
                    .height(165.dp)
                    // sharedBounds (not sharedElement) to match the library card's cover, which
                    // also uses sharedBounds for this key — mixing the two APIs on one key made the
                    // cover mis-size mid-flight.
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = coverKey),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        clipInOverlayDuringTransition = OverlayClip(coverShape)
                    ),
                shape = coverShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = coverImageRequest,
                    contentDescription = stringResource(R.string.content_description_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onCoverClick() }
                )
            }
        }

        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))

        // Title and Metadata
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 6.dp)
        ) {
            with(sharedTransitionScope) {
                Text(
                    text = displayTitle.orEmpty(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = titleKey),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                displayTitle?.let {
                                    copyToClipboard(titleClipLabel, it, copiedTitleMessage)
                                }
                            },
                            onLongClickLabel = copyLongClickLabel
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Format · Year
            MetadataTags(details)

            if (details.score != null || details.popularity != null || details.favourites != null) {
                Spacer(modifier = Modifier.height(4.dp))
                // Row 2: ★ Score · 👥 Popularity · ♥ Favorites
                StatsStrip(details = details, formattedScore = formattedScore)
            }
        }
    }
}

@Composable
private fun MetadataTags(details: MediaDetails) {
    val formattedFormat = remember(details.format) { details.format?.formatAsTitle() }
    val formattedYear = remember(details.seasonYear) { details.seasonYear?.toString() }

    if (formattedFormat == null && formattedYear == null) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (formattedFormat != null) {
            MetadataText(formattedFormat)
        }

        if (formattedFormat != null && formattedYear != null) {
            MetadataSeparator()
        }

        if (formattedYear != null) {
            MetadataText(formattedYear)
        }
    }
}

@Composable
private fun StatsStrip(details: MediaDetails, formattedScore: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var hasPrevious = false

        if (formattedScore != null) {
            StatItem(
                icon = Icons.Filled.Star,
                iconTint = Color(0xFFFFC107),
                text = formattedScore
            )
            hasPrevious = true
        }

        if (details.popularity != null && details.popularity > 0) {
            if (hasPrevious) MetadataSeparator()
            StatItem(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = MaterialTheme.colorScheme.primary,
                text = com.anisync.android.presentation.util.formatCompactNumber(details.popularity)
            )
            hasPrevious = true
        }

        if (details.favourites != null && details.favourites > 0) {
            if (hasPrevious) MetadataSeparator()
            StatItem(
                icon = Icons.Filled.Favorite,
                iconTint = MaterialTheme.colorScheme.error,
                text = com.anisync.android.presentation.util.formatCompactNumber(details.favourites)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MetadataText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun MetadataSeparator() {
    Text(
        text = stringResource(R.string.separator_bullet),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun SmartActionButtonsRow(
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- SOCIAL ACTIONS ---
        FilledIconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(56.dp), // Larger touch target
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isFavorite)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isFavorite)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.primary
            )
        ) {
            AnimatedFavoriteButton(
                isFavorite = isFavorite,
                onClick = onFavoriteClick,
                iconSize = 28.dp,
                activeColor = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
        }

        // Share
        OutlinedButton(
            onClick = onShareClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp), // Match height of fav button
            shape = CircleShape, // Pill shape
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                containerColor = Color.Transparent
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.action_share),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
