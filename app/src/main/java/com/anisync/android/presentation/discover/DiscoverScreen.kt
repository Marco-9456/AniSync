package com.anisync.android.presentation.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.SegmentedControl
import com.anisync.android.presentation.util.shimmerEffect
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onMediaClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    var searchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Discover",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // M3 SearchBar - triggers navigation on expand
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { isTraversalGroup = true }
        ) {
            SearchBar(
                modifier = Modifier.fillMaxWidth(),
                query = "",
                onQueryChange = {},
                onSearch = {},
                active = false,
                onActiveChange = { 
                    onSearchClick()
                },
                placeholder = { Text("Search Anime & Manga...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                colors = SearchBarDefaults.colors(
                    containerColor = BeigeYellow
                )
            ) {
                // Empty content - we navigate away instead
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Control
        SegmentedControl(
            options = listOf("Anime", "Manga"),
            selectedOption = if (mediaType == MediaType.ANIME) "Anime" else "Manga",
            onOptionSelected = { 
                viewModel.onMediaTypeChange(if (it == "Anime") MediaType.ANIME else MediaType.MANGA)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                    Text(text = "Error: ${state.message}", color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun DiscoverLoadingShimmer() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Mock sections
        items(3) {
            Column {
                // Header shimmer
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Row shimmer
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(4) {
                        Column(modifier = Modifier.width(110.dp)) {
                            Box(
                                modifier = Modifier
                                    .height(160.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .shimmerEffect()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                        }
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
        verticalArrangement = Arrangement.spacedBy(32.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Trending Section
        item {
            SectionHeader(
                title = "Trending Now",
                icon = Icons.AutoMirrored.Filled.TrendingUp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(trending) { index, item ->
                    DiscoverItemCard(
                        item = item,
                        rank = index + 1,
                        onClick = { onMediaClick(item.mediaId) }
                    )
                }
            }
        }

        // Popular Section
        item {
            SectionHeader(
                title = "All-Time Popular",
                icon = Icons.Default.FavoriteBorder
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(popular) { index, item ->
                    DiscoverItemCard(
                        item = item,
                        rank = index + 1,
                        onClick = { onMediaClick(item.mediaId) }
                    )
                }
            }
        }

        // Upcoming Section
        item {
            SectionHeader(
                title = "Upcoming",
                icon = Icons.Default.CalendarMonth
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(upcoming) { index, item ->
                    DiscoverItemCard(
                        item = item,
                        rank = index + 1,
                        onClick = { onMediaClick(item.mediaId) }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
}

@Composable
fun DiscoverItemCard(
    item: LibraryEntry,
    rank: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Rank Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(OliveDrab, RoundedCornerShape(bottomEnd = 8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "#$rank",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            
            // Rating Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "★ 8.5",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}
