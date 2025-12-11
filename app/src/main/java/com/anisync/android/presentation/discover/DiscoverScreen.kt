package com.anisync.android.presentation.discover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.AVAILABLE_GENRES
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchFilters
import com.anisync.android.presentation.util.formatChaptersCount
import com.anisync.android.presentation.util.formatEpisodesCount
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.presentation.util.shimmerEffect
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.geometry.Rect
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.type.MediaType
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaStatus
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreen(
    onMediaClick: (Int) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchFilters by viewModel.searchFilters.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    
    // Filter sheet state
    var showFilterSheet by remember { mutableStateOf(false) }

    // Sync textFieldState changes with ViewModel
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { viewModel.onSearchQueryChange(it) }
    }

    // Clear keyboard and focus when search bar is collapsing to prevent InputConnection warnings
    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Collapsed) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    // Handle back press to close search
    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) {
        keyboardController?.hide()
        coroutineScope.launch { searchBarState.animateToCollapsed() }
    }

    // ExpandedFullScreenSearchBar displays the search results in a full-screen overlay
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = {
            SearchBarDefaults.InputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = {
                    viewModel.onSearch(textFieldState.text.toString())
                    keyboardController?.hide()
                },
                placeholder = { Text(if (mediaType == MediaType.ANIME) stringResource(R.string.search_anime_placeholder) else stringResource(R.string.search_manga_placeholder)) },
                leadingIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        coroutineScope.launch { searchBarState.animateToCollapsed() }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                textFieldState.edit { replace(0, length, "") }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }
                        // Filter button with badge when filters are active
                        IconButton(onClick = { showFilterSheet = true }) {
                            if (searchFilters.hasActiveFilters) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(searchFilters.activeFilterCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
                                }
                            } else {
                                Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
                            }
                        }
                    }
                }
            )
        }
    ) {
        // Search Results Content
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { item ->
                    SearchResultItem(
                        item = item,
                        onClick = {
                            keyboardController?.hide()
                            onMediaClick(item.mediaId)
                        }
                    )
                }
            }
        }
    }

    // Search Filter Bottom Sheet
    if (showFilterSheet) {
        SearchFilterSheet(
            filters = searchFilters,
            mediaType = mediaType,
            onFiltersChanged = { viewModel.updateFilters(it) },
            onDismiss = { showFilterSheet = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // AppBarWithSearch integrates with Scaffold's topBar for proper window insets handling
            AppBarWithSearch(
                state = searchBarState,
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = searchBarState,
                        textFieldState = textFieldState,
                        onSearch = {
                            viewModel.onSearch(textFieldState.text.toString())
                            keyboardController?.hide()
                        },
                        placeholder = { Text(if (mediaType == MediaType.ANIME) stringResource(R.string.search_anime_placeholder) else stringResource(R.string.search_manga_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        },
                        trailingIcon = null
                    )
                }
            )
        }
    ) { paddingValues ->
        val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
        
        // Main Content
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            indicator = {
                // Custom polygons for shape morphing during pull-to-refresh
                val customPolygons = listOf(
                    androidx.compose.material3.MaterialShapes.Circle,
                    androidx.compose.material3.MaterialShapes.Flower,
                    androidx.compose.material3.MaterialShapes.Diamond,
                    androidx.compose.material3.MaterialShapes.Heart,
                    androidx.compose.material3.MaterialShapes.Clover4Leaf
                )
                
                // Show LoadingIndicator when refreshing or pulling
                if (isRefreshing || pullToRefreshState.distanceFraction > 0f) {
                    androidx.compose.material3.ContainedLoadingIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        polygons = customPolygons
                    )
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                when (val state = uiState) {
                    is DiscoverUiState.Loading -> {
                        item { DiscoverShimmer() }
                    }
                    is DiscoverUiState.Error -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = stringResource(R.string.error_failed_to_load), color = MaterialTheme.colorScheme.error)
                                    Text(text = state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    is DiscoverUiState.Success -> {
                        item {
                            MediaTypeSelector(
                                selected = mediaType,
                                onSelect = viewModel::onMediaTypeChange,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            SectionHeader(
                                title = stringResource(R.string.section_trending_now),
                                icon = Icons.Default.LocalFireDepartment,
                                color = Color(0xFFFF5722)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CinematicHeroCarousel(
                                items = state.trending.take(10),
                                onItemClick = onMediaClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(48.dp))
                            SectionHeader(
                                title = stringResource(R.string.section_all_time_popular),
                                icon = Icons.Default.Star,
                                color = Color(0xFFFFC107)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalMediaList(
                                items = state.popular,
                                onItemClick = onMediaClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(48.dp))
                            SectionHeader(
                                title = stringResource(R.string.section_upcoming_season),
                                icon = Icons.Default.CalendarMonth,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalMediaList(
                                items = state.upcoming,
                                onItemClick = onMediaClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// UI COMPONENTS
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MediaTypeSelector(
    selected: MediaType,
    onSelect: (MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        ToggleButton(
            checked = selected == MediaType.ANIME,
            onCheckedChange = { 
                haptic.click()
                onSelect(MediaType.ANIME) 
            },
            modifier = Modifier.weight(1f),
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
        ) {
            val scale by animateFloatAsState(targetValue = if (selected == MediaType.ANIME) 1.1f else 1f, animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(), label = "AnimeScale")
            Text(
                text = stringResource(R.string.media_type_anime),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(scale)
            )
        }
        ToggleButton(
            checked = selected == MediaType.MANGA,
            onCheckedChange = { 
                haptic.click()
                onSelect(MediaType.MANGA) 
            },
            modifier = Modifier.weight(1f),
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
        ) {
            val scale by animateFloatAsState(targetValue = if (selected == MediaType.MANGA) 1.1f else 1f, animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(), label = "MangaScale")
            Text(
                text = stringResource(R.string.media_type_manga),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(scale)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.more),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// --- CAROUSEL (UNTOUCHED) ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun CinematicHeroCarousel(
    items: List<LibraryEntry>,
    onItemClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val carouselState = rememberCarouselState { items.size }

    HorizontalCenteredHeroCarousel(
        state = carouselState,
        modifier = Modifier
            .height(380.dp)
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 48.dp),
        itemSpacing = 24.dp,
        flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state = carouselState)
    ) { itemIndex ->
        val item = items[itemIndex]
        HeroCard(
            item = item,
            onClick = { onItemClick(item.mediaId) },
            modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroCard(
    item: LibraryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()

    Card(
        modifier = modifier
            .height(380.dp)
            .bouncyClickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            with(sharedTransitionScope) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "discover_media_cover_${item.mediaId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            clipInOverlayDuringTransition = OverlayClip(MaterialTheme.shapes.extraLarge)
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.type?.name ?: stringResource(R.string.media_type_anime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                with(sharedTransitionScope) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "discover_media_title_${item.mediaId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec }
                        )
                    )
                }

                val statusText = formatStatus(item.mediaStatus)
                val countsText = if (item.totalEpisodes != null) formatEpisodesCount(item.totalEpisodes) else if (item.totalChapters != null) formatChaptersCount(item.totalChapters) else null

                if (statusText != null || countsText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = listOfNotNull(statusText, countsText).joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// --- REDESIGNED MEDIA CARD ---

/**
 * A specialized Media Card strictly following the provided redesign image.
 * Structure:
 * - Top: Image Thumbnail
 * - Bottom: Content Area (Title + Type + Rating Pill)
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MediaCard(
    item: LibraryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer, // Matches the white/light background of the design
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .width(150.dp)
            .bouncyClickable(onClick = onClick)
    ) {
        Column {
            // Image Container
            with(sharedTransitionScope) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f) // Standard poster ratio
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "discover_media_cover_${item.mediaId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(12.dp))
                        )
                )
            }

            // Content Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Title (Bold, Black)
                with(sharedTransitionScope) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "discover_media_title_${item.mediaId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec }
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Row: Type and Rating Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Type (e.g., "TV") - Secondary Color
                    Text(
                        text = item.type?.name ?: "TV",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Rating Pill (e.g., "8.8" with Star) - Only show if score is available
                    item.averageScore?.let { score ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest, // Light grey/purple tint
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107), // Amber/Gold Star
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                // Convert from 0-100 scale to 0-10 scale for display
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", score / 10.0),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalMediaList(
    items: List<LibraryEntry>,
    onItemClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(items) { _, item ->
            MediaCard(
                item = item,
                onClick = { onItemClick(item.mediaId) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchResultItem(
    item: LibraryEntry,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            val type = item.type?.name ?: stringResource(R.string.media_type_media)
            val status = formatStatus(item.mediaStatus) ?: stringResource(R.string.unknown)
            Text(
                text = "$type • $status",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.bouncyClickable(onClick = onClick)
    )
}

@Composable
private fun DiscoverShimmer() {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(28.dp)).shimmerEffect())
        Spacer(Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(150.dp, 24.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) {
                Box(Modifier.width(140.dp).height(200.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
            }
        }
    }
}

private fun formatStatus(status: String?): String? {
    if (status == null) return null
    return status.replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

/**
 * Bottom sheet for search filters including Genres, Year, Season, Format, and Airing Status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFilterSheet(
    filters: SearchFilters,
    mediaType: MediaType,
    onFiltersChanged: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with title and clear button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (filters.hasActiveFilters) {
                    TextButton(onClick = { onFiltersChanged(SearchFilters()) }) {
                        Text(stringResource(R.string.filter_clear_all))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Genres Section
            FilterSectionHeader(title = stringResource(R.string.filter_genres))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(AVAILABLE_GENRES) { genre ->
                    FilterChip(
                        selected = genre in filters.genres,
                        onClick = {
                            val newGenres = if (genre in filters.genres) 
                                filters.genres - genre 
                            else 
                                filters.genres + genre
                            onFiltersChanged(filters.copy(genres = newGenres))
                        },
                        label = { Text(genre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Year Section
            FilterSectionHeader(title = stringResource(R.string.filter_year))
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val years = (currentYear downTo currentYear - 20).toList()
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(years) { year ->
                    FilterChip(
                        selected = filters.year == year,
                        onClick = {
                            val newYear = if (filters.year == year) null else year
                            onFiltersChanged(filters.copy(year = newYear))
                        },
                        label = { Text(year.toString()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Season Section
            FilterSectionHeader(title = stringResource(R.string.filter_season))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                MediaSeason.entries.forEach { season ->
                    FilterChip(
                        selected = filters.season == season,
                        onClick = {
                            val newSeason = if (filters.season == season) null else season
                            onFiltersChanged(filters.copy(season = newSeason))
                        },
                        label = { Text(getSeasonLabel(season)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Format Section
            FilterSectionHeader(title = stringResource(R.string.filter_format))
            val availableFormats = if (mediaType == MediaType.ANIME) {
                listOf(MediaFormat.TV, MediaFormat.TV_SHORT, MediaFormat.MOVIE, MediaFormat.SPECIAL, MediaFormat.OVA, MediaFormat.ONA, MediaFormat.MUSIC)
            } else {
                listOf(MediaFormat.MANGA, MediaFormat.NOVEL, MediaFormat.ONE_SHOT)
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(availableFormats) { format ->
                    FilterChip(
                        selected = format in filters.formats,
                        onClick = {
                            val newFormats = if (format in filters.formats) 
                                filters.formats - format 
                            else 
                                filters.formats + format
                            onFiltersChanged(filters.copy(formats = newFormats))
                        },
                        label = { Text(getFormatLabel(format)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Airing Status Section
            FilterSectionHeader(title = stringResource(R.string.filter_status))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(MediaStatus.entries.filter { it != MediaStatus.UNKNOWN__ }) { status ->
                    FilterChip(
                        selected = filters.status == status,
                        onClick = {
                            val newStatus = if (filters.status == status) null else status
                            onFiltersChanged(filters.copy(status = newStatus))
                        },
                        label = { Text(getStatusLabel(status)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun getSeasonLabel(season: MediaSeason): String {
    return when (season) {
        MediaSeason.WINTER -> stringResource(R.string.season_winter)
        MediaSeason.SPRING -> stringResource(R.string.season_spring)
        MediaSeason.SUMMER -> stringResource(R.string.season_summer)
        MediaSeason.FALL -> stringResource(R.string.season_fall)
        MediaSeason.UNKNOWN__ -> ""
    }
}

@Composable
private fun getFormatLabel(format: MediaFormat): String {
    return when (format) {
        MediaFormat.TV -> stringResource(R.string.format_tv)
        MediaFormat.TV_SHORT -> stringResource(R.string.format_tv_short)
        MediaFormat.MOVIE -> stringResource(R.string.format_movie)
        MediaFormat.SPECIAL -> stringResource(R.string.format_special)
        MediaFormat.OVA -> stringResource(R.string.format_ova)
        MediaFormat.ONA -> stringResource(R.string.format_ona)
        MediaFormat.MUSIC -> stringResource(R.string.format_music)
        MediaFormat.MANGA -> stringResource(R.string.media_type_manga)
        MediaFormat.NOVEL -> stringResource(R.string.format_novel)
        MediaFormat.ONE_SHOT -> stringResource(R.string.format_one_shot)
        MediaFormat.UNKNOWN__ -> ""
    }
}

@Composable
private fun getStatusLabel(status: MediaStatus): String {
    return when (status) {
        MediaStatus.RELEASING -> stringResource(R.string.media_status_airing)
        MediaStatus.FINISHED -> stringResource(R.string.media_status_finished)
        MediaStatus.NOT_YET_RELEASED -> stringResource(R.string.media_status_not_yet_released)
        MediaStatus.CANCELLED -> stringResource(R.string.media_status_cancelled)
        MediaStatus.HIATUS -> stringResource(R.string.media_status_hiatus)
        MediaStatus.UNKNOWN__ -> ""
    }
}