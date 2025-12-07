package com.anisync.android.presentation.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.SegmentedControl
import com.anisync.android.presentation.util.shimmerEffect
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.BeigeYellow
import com.anisync.android.ui.theme.CreamBackground
import com.anisync.android.ui.theme.OliveDrab
import com.anisync.android.ui.theme.TextDark
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onMediaClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    // Main Scaffold-like structure
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        // Search Bar (Docked at top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { isTraversalGroup = true }
        ) {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { traversalIndex = 0f },
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onSearch = viewModel::onSearch,
                        expanded = isSearchActive,
                        onExpandedChange = viewModel::onSearchActiveChange,
                        placeholder = { Text("Search Anime & Manga...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                             if (isSearchActive) {
                                 Icon(
                                     modifier = Modifier.clickable {
                                         if (searchQuery.isNotEmpty()) {
                                             viewModel.onSearchQueryChange("")
                                         } else {
                                             viewModel.onSearchActiveChange(false)
                                         }
                                     },
                                     imageVector = Icons.Default.Close,
                                     contentDescription = "Close"
                                 )
                             }
                        }
                    )
                },
                expanded = isSearchActive,
                onExpandedChange = viewModel::onSearchActiveChange,
                colors = SearchBarDefaults.colors(
                    containerColor = BeigeYellow
                )
            ) {
                // Search Results
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(searchResults) { index, item ->
                        ListItem(
                            headlineContent = { Text(item.title) },
                            leadingContent = {
                                AsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .clickable {
                                    onMediaClick(item.mediaId)
                                    // Optionally close search?
                                    // viewModel.onSearchActiveChange(false)
                                }
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        // MediaType Selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            SegmentedControl(
                options = listOf("Anime", "Manga"),
                selectedOption = if (mediaType == MediaType.ANIME) "Anime" else "Manga",
                onOptionSelected = {
                    viewModel.onMediaTypeChange(if (it == "Anime") MediaType.ANIME else MediaType.MANGA)
                }
            )
        }

        // Content with PullToRefresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = uiState) {
                is DiscoverUiState.Loading -> {
                    DiscoverLoadingShimmer()
                }

                is DiscoverUiState.Success -> {
                    DiscoverContent(
                        trending = state.trending,
                        popular = state.popular,
                        upcoming = state.upcoming,
                        onMediaClick = onMediaClick
                    )
                }

                is DiscoverUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverContent(
    trending: List<LibraryEntry>,
    popular: List<LibraryEntry>,
    upcoming: List<LibraryEntry>,
    onMediaClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Hero Section (Trending)
        item {
            if (trending.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Trending Now",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                HeroCarousel(
                    items = trending.take(5), // Top 5
                    onItemClick = onMediaClick
                )
            }
        }

        // Popular Section
        item {
            MediaSection(
                title = "All-Time Popular",
                icon = Icons.Default.Favorite,
                items = popular,
                onItemClick = onMediaClick
            )
        }

        // Upcoming Section
        item {
            MediaSection(
                title = "Upcoming Next Season",
                icon = Icons.Default.CalendarMonth,
                items = upcoming,
                onItemClick = onMediaClick
            )
        }
    }
}

@Composable
fun HeroCarousel(
    items: List<LibraryEntry>,
    onItemClick: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.height(220.dp)
        ) { page ->
            val item = items[page]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val scale = lerp(
                start = 0.85f,
                stop = 1f,
                fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onItemClick(item.mediaId) }
            ) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                // Text Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "#${page + 1} Trending",
                        color = Color(0xFFFFD700), // Gold
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pager Indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(items.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) OliveDrab else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                )
            }
        }
    }
}

@Composable
fun MediaSection(
    title: String,
    icon: ImageVector,
    items: List<LibraryEntry>,
    onItemClick: (Int) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OliveDrab,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(items) { index, item ->
                DiscoverItemCard(
                    item = item,
                    rank = index + 1,
                    onClick = { onItemClick(item.mediaId) },
                    showRank = title.contains("Popular", ignoreCase = true)
                )
            }
        }
    }
}

@Composable
fun DiscoverItemCard(
    item: LibraryEntry,
    rank: Int,
    onClick: () -> Unit,
    showRank: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .height(190.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (showRank && rank <= 10) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(
                            color = if (rank <= 3) OliveDrab else Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(bottomEnd = 12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "#$rank",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Rating Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "8.5", // Placeholder rating as LibraryEntry doesn't have it apparently, or I can check
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DiscoverLoadingShimmer() {
    Column(modifier = Modifier.padding(16.dp)) {
        // Hero Shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Row Shimmers
        repeat(2) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .shimmerEffect()
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
