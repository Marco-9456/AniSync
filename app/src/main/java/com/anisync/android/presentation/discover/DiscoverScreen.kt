package com.anisync.android.presentation.discover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.util.shimmerEffect
import com.anisync.android.type.MediaType
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onMediaClick: (Int) -> Unit,
    onSearchClick: () -> Unit, // Kept for interface compatibility
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    // Handle back press to close search
    BackHandler(enabled = isSearchActive) {
        viewModel.onSearchActiveChange(false)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // ---------------------------------------------------------------------
            // Search Bar Layer
            // ---------------------------------------------------------------------
            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            Box(
                modifier = Modifier
                    .zIndex(10f) // Keep SearchBar on top
                    .fillMaxWidth()
                    .padding(horizontal = if (isSearchActive) 0.dp else 16.dp)
                    .padding(top = if (isSearchActive) 0.dp else statusBarHeight + 8.dp)
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onSearch = {
                                viewModel.onSearch(it)
                                keyboardController?.hide()
                            },
                            expanded = isSearchActive,
                            onExpandedChange = viewModel::onSearchActiveChange,
                            placeholder = { Text("Search ${mediaType.name.lowercase().replaceFirstChar { it.uppercase() }}...") },
                            leadingIcon = {
                                if (isSearchActive) {
                                    IconButton(onClick = { viewModel.onSearchActiveChange(false) }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    },
                    expanded = isSearchActive,
                    onExpandedChange = viewModel::onSearchActiveChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SearchBarDefaults.colors(
                        containerColor = if (isSearchActive) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainerHigh,
                        dividerColor = if (isSearchActive) MaterialTheme.colorScheme.outlineVariant else Color.Transparent
                    ),
                    tonalElevation = if (isSearchActive) 0.dp else 6.dp,
                    shape = if (isSearchActive) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
                    windowInsets = if (isSearchActive) WindowInsets.statusBars else WindowInsets(0.dp)
                ) {
                    // Search Results
                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No results found.",
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
            }

            // ---------------------------------------------------------------------
            // Main Content
            // ---------------------------------------------------------------------
            AnimatedVisibility(
                visible = !isSearchActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // Spacer for floating Search Bar
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }

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
                                            Text(text = "Failed to load content", color = MaterialTheme.colorScheme.error)
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
                                        title = "Trending Now",
                                        icon = Icons.Default.LocalFireDepartment,
                                        color = Color(0xFFFF5722)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CinematicHeroCarousel(
                                        items = state.trending.take(10),
                                        onItemClick = onMediaClick
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(48.dp))
                                    SectionHeader(
                                        title = "All Time Popular",
                                        icon = Icons.Default.Star,
                                        color = Color(0xFFFFC107)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalMediaList(
                                        items = state.popular,
                                        onItemClick = onMediaClick,
                                        isRanked = true
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(48.dp))
                                    SectionHeader(
                                        title = "Upcoming Season",
                                        icon = Icons.Default.CalendarMonth,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalMediaList(
                                        items = state.upcoming,
                                        onItemClick = onMediaClick,
                                        isRanked = false
                                    )
                                }
                            }
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

@Composable
private fun MediaTypeSelector(
    selected: MediaType,
    onSelect: (MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f), RoundedCornerShape(25.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MediaTypeTab("Anime", selected == MediaType.ANIME, { onSelect(MediaType.ANIME) }, Modifier.weight(1f))
        MediaTypeTab("Manga", selected == MediaType.MANGA, { onSelect(MediaType.MANGA) }, Modifier.weight(1f))
    }
}

@Composable
private fun MediaTypeTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "TabContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "TabContent"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(21.dp))
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
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
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun CinematicHeroCarousel(items: List<LibraryEntry>, onItemClick: (Int) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 48.dp),
        pageSpacing = 24.dp,
        modifier = Modifier.height(380.dp).fillMaxWidth()
    ) { page ->
        val item = items[page]
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val scale by animateFloatAsState(
            targetValue = if (pageOffset.absoluteValue < 0.5f) 1f else 0.92f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            label = "Scale"
        )

        HeroCard(
            item = item,
            onClick = { onItemClick(item.mediaId) },
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

@Composable
private fun HeroCard(item: LibraryEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
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
                        text = item.type?.name ?: "ANIME",
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
                    overflow = TextOverflow.Ellipsis
                )

                val statusText = formatStatus(item.mediaStatus)
                val countsText = if (item.totalEpisodes != null) "${item.totalEpisodes} Eps" else if (item.totalChapters != null) "${item.totalChapters} Ch" else null

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

@Composable
private fun HorizontalMediaList(items: List<LibraryEntry>, onItemClick: (Int) -> Unit, isRanked: Boolean) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(items) { index, item ->
            Column(modifier = Modifier.width(140.dp).clickable { onItemClick(item.mediaId) }) {
                Box(
                    modifier = Modifier.aspectRatio(0.7f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isRanked) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(
                                    if (index < 3) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(bottomEnd = 12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "#${index + 1}", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val status = formatStatus(item.mediaStatus)
                if (status != null) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(item: LibraryEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            val type = item.type?.name ?: "MEDIA"
            val status = formatStatus(item.mediaStatus) ?: "Unknown"
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
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
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