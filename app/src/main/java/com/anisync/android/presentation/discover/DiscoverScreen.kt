package com.anisync.android.presentation.discover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.AVAILABLE_GENRES
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchFilters
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.MediaCard
import com.anisync.android.presentation.components.MediaTypeSelector
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatChaptersCount
import com.anisync.android.presentation.util.formatEpisodesCount
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.shimmerEffect
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.StarGold
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreen(
    onMediaClick: (Int) -> Unit,
    onSectionSeeAllClick: (title: String, sectionType: String) -> Unit,
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
    // Search Bar State with persistence
    val isSearchExpanded = rememberSaveable { mutableStateOf(false) }
    val searchBarState = rememberSearchBarState(
        initialValue = if (isSearchExpanded.value) SearchBarValue.Expanded else SearchBarValue.Collapsed
    )
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) { androidx.compose.foundation.lazy.LazyListState() }

    // Filter dialog state
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }

    // Sync textFieldState changes with ViewModel
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { viewModel.onSearchQueryChange(it) }
    }

    // Sync searchBarState changes back to isSearchExpanded to persist state
    LaunchedEffect(searchBarState.currentValue) {
        isSearchExpanded.value = searchBarState.currentValue == SearchBarValue.Expanded
        
        // Clear keyboard and focus when search bar is collapsing
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
                        IconButton(onClick = { showFilterDialog = true }) {
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
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                items(searchResults, key = { it.mediaId }) { item ->
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

    // Search Filter Dialog
    if (showFilterDialog) {
        SearchFilterDialog(
            filters = searchFilters,
            mediaType = mediaType,
            onFiltersChanged = { viewModel.updateFilters(it) },
            onDismiss = { showFilterDialog = false }
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
                            val trendingTitle = stringResource(R.string.section_trending_now)
                            SectionHeader(
                                title = trendingTitle,
                                iconColor = Color(0xFFFF5722),
                                onActionClick = { onSectionSeeAllClick(trendingTitle, "trending") },
                                level = HeaderLevel.Section
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
                            val popularTitle = stringResource(R.string.section_all_time_popular)
                            SectionHeader(
                                title = popularTitle,
                                iconColor = StarGold,
                                onActionClick = { onSectionSeeAllClick(popularTitle, "popular") },
                                level = HeaderLevel.Section
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
                            val upcomingTitle = stringResource(R.string.section_upcoming_season)
                            SectionHeader(
                                title = upcomingTitle,
                                iconColor = MaterialTheme.colorScheme.primary,
                                onActionClick = { onSectionSeeAllClick(upcomingTitle, "upcoming") },
                                level = HeaderLevel.Section
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
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    with(sharedTransitionScope) {
        Card(
            modifier = modifier
                .height(380.dp)
                .bouncyClickable(onClick = onClick)
                .sharedElement(
                    sharedContentState = rememberSharedContentState(key = "discover_media_cover_${item.mediaId}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(MaterialTheme.shapes.extraLarge)
                ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val cacheKey = "discover_cover_${item.mediaId}"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.coverUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "discover_gradient_${item.mediaId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            enter = fadeIn(effectsSpec),
                            exit = fadeOut(effectsSpec)
                        )
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
                        text = item.format?.toLabel() ?: stringResource(R.string.media_type_anime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                        boundsTransform = { _, _ -> spatialSpec },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                )

                val statusText = item.mediaStatus.formatAsTitle()
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
        items(
            items = items,
            key = { it.mediaId }
        ) { item ->
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
            val formatLabel = item.format?.toLabel() ?: stringResource(R.string.media_type_media)
            val status = item.mediaStatus.formatAsTitle() ?: stringResource(R.string.unknown)
            Text(
                text = "$formatLabel • $status",
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



/**
 * Search Filter Dialog
 * Replaces the Bottom Sheet with a centered, floating dialog.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterDialog(
    filters: SearchFilters,
    mediaType: MediaType,
    onFiltersChanged: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Slightly narrower than screen
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh, // Slightly higher elevation color
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // We keep the button in the layout (invisible) to prevent height jumps
                    val hasFilters = filters.hasActiveFilters
                    TextButton(
                        onClick = { onFiltersChanged(SearchFilters()) },
                        enabled = hasFilters,
                        modifier = Modifier.alpha(if (hasFilters) 1f else 0f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.filter_clear_all))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f) // Takes remaining space
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {

                    // Genres Section (FlowRow)
                    FilterSection(title = stringResource(R.string.filter_genres)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AVAILABLE_GENRES.forEach { genre ->
                                val selected = genre in filters.genres
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val newGenres = if (selected) filters.genres - genre else filters.genres + genre
                                        onFiltersChanged(filters.copy(genres = newGenres))
                                    },
                                    label = { Text(genre) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Season Section
                    FilterSection(title = stringResource(R.string.filter_season)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaSeason.entries.filter { it != MediaSeason.UNKNOWN__ }.forEach { season ->
                                val selected = filters.season == season
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onFiltersChanged(filters.copy(season = if (selected) null else season))
                                    },
                                    label = { Text(getSeasonLabel(season)) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Year Section (Horizontal Scroll is better for timeline/numbers)
                    FilterSection(title = stringResource(R.string.filter_year)) {
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val years = (currentYear downTo 1970).toList()
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            items(years) { year ->
                                val selected = filters.year == year
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onFiltersChanged(filters.copy(year = if (selected) null else year))
                                    },
                                    label = { Text(year.toString()) },
                                    border = if (selected) null else FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    // Format Section
                    FilterSection(title = stringResource(R.string.filter_format)) {
                        val availableFormats = if (mediaType == MediaType.ANIME) {
                            listOf(MediaFormat.TV, MediaFormat.TV_SHORT, MediaFormat.MOVIE, MediaFormat.SPECIAL, MediaFormat.OVA, MediaFormat.ONA, MediaFormat.MUSIC)
                        } else {
                            listOf(MediaFormat.MANGA, MediaFormat.NOVEL, MediaFormat.ONE_SHOT)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableFormats.forEach { format ->
                                val selected = format in filters.formats
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val newFormats = if (selected) filters.formats - format else filters.formats + format
                                        onFiltersChanged(filters.copy(formats = newFormats))
                                    },
                                    label = { Text(format.toLabel()) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Status Section
                    FilterSection(title = stringResource(R.string.filter_status)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaStatus.entries.filter { it != MediaStatus.UNKNOWN__ }.forEach { status ->
                                val selected = filters.status == status
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onFiltersChanged(filters.copy(status = if (selected) null else status))
                                    },
                                    label = { Text(status.toLabel()) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Show Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        content()
    }
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

