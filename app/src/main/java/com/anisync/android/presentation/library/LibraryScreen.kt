package com.anisync.android.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.shimmerEffect
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LibraryScreen(
    onMediaClick: (Int) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    
    var isGridView by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(LibraryStatus.CURRENT) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LibraryEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        containerColor = CreamBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CreamBackground)
        ) {
            // Header Section
            LibraryHeader(
                selectedMediaType = mediaType,
                onMediaTypeChange = viewModel::onMediaTypeChange,
                selectedStatus = selectedStatus,
                onStatusChange = { selectedStatus = it },
                isGridView = isGridView,
                onViewToggle = { isGridView = !isGridView }
            )

            // Content Section
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                   LibraryLoadingShimmer(isGridView)
                }
                is LibraryUiState.Success -> {
                    // Filter Logic
                    val filteredEntries = state.entries.filter { entry ->
                        entry.status == selectedStatus
                    }

                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredEntries, key = { it.id }) { entry ->
                                LibraryGridCard(
                                    entry = entry,
                                    onClick = { onMediaClick(entry.mediaId) },
                                    onIncrement = { viewModel.incrementProgress(entry.mediaId) },
                                    onDecrement = { viewModel.decrementProgress(entry.mediaId) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredEntries, key = { it.id }) { entry ->
                                LibraryListCard(
                                    entry = entry,
                                    onClick = { onMediaClick(entry.mediaId) },
                                    onIncrement = { viewModel.incrementProgress(entry.mediaId) },
                                    onDecrement = { viewModel.decrementProgress(entry.mediaId) }
                                )
                            }
                        }
                    }
                }
                is LibraryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryHeader(
    selectedMediaType: MediaType,
    onMediaTypeChange: (MediaType) -> Unit,
    selectedStatus: LibraryStatus,
    onStatusChange: (LibraryStatus) -> Unit,
    isGridView: Boolean,
    onViewToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Title
        Text(
            text = "My Library",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDark,
                fontSize = 28.sp
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Segmented Control (Anime/Manga)
        com.anisync.android.presentation.components.SegmentedControl(
            options = listOf("Anime", "Manga"),
            selectedOption = if (selectedMediaType == MediaType.ANIME) "Anime" else "Manga",
            onOptionSelected = { 
                onMediaTypeChange(if (it == "Anime") MediaType.ANIME else MediaType.MANGA) 
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Chips & View Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statuses = listOf(
                    "Watching" to LibraryStatus.CURRENT,
                    "Paused" to LibraryStatus.PAUSED,
                    "Completed" to LibraryStatus.COMPLETED,
                    "Planning" to LibraryStatus.PLANNING,
                    "Dropped" to LibraryStatus.DROPPED
                )
                
                items(statuses) { (label, status) ->
                    val isSelected = selectedStatus == status
                    val displayLabel = if (selectedMediaType == MediaType.MANGA && label == "Watching") "Reading" else label
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) BeigeYellow else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) OliveDrab else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable { onStatusChange(status) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = displayLabel,
                            color = TextDark,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // View Toggle
            IconButton(
                onClick = onViewToggle,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .background(BeigeYellow, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Filled.Menu else Icons.Filled.MoreVert,
                    contentDescription = "Toggle View",
                    tint = TextDark
                )
            }
        }
    }
}

@Composable
fun LibraryLoadingShimmer(isGridView: Boolean) {
    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(6) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
             items(6) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
             }
        }
    }
}

@Composable
fun LibraryListCard(
    entry: LibraryEntry,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isManga = entry.type == MediaType.MANGA
    val total = if (isManga) entry.totalChapters else entry.totalEpisodes
    val unitLabel = if (isManga) "Ch." else "Ep."
    val progressLabel = if (isManga) "READING" else "AIRING" // Simplified, ideally check status

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePinkWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Cover Image
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = entry.coverUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    if (entry.status == LibraryStatus.CURRENT) {
                         Text(
                            text = progressLabel, 
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDark,
                            modifier = Modifier
                                .background(AiringYellow, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = "$unitLabel ${entry.progress + 1}", 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Progress Bar and Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Progress", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(
                                "${entry.progress} / ${total ?: "?"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        LinearProgressIndicator(
                            progress = { 
                                if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = OliveDrab,
                            trackColor = Color.LightGray.copy(alpha = 0.3f),
                        )
                    }
                }
            }

            // Actions Strip
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(PastelPink)
                        .clickable { onDecrement() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextDark)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(PastelGreen)
                        .clickable { onIncrement() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextDark)
                }
            }
        }
    }
}

@Composable
fun LibraryGridCard(
    entry: LibraryEntry,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isManga = entry.type == MediaType.MANGA
    val total = if (isManga) entry.totalChapters else entry.totalEpisodes
    val progressLabel = if (isManga) "READING" else "AIRING"

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePinkWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Image
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = entry.coverUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .padding(8.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                 Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDark,
                    modifier = Modifier
                        .background(AiringYellow, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                LinearProgressIndicator(
                    progress = { 
                        if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = OliveDrab,
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                )
            }
            
            // Action Row
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(PastelPink)
                        .clickable { onDecrement() },
                    contentAlignment = Alignment.Center
                ) {
                     Text("-", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextDark)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(PastelGreen)
                        .clickable { onIncrement() },
                    contentAlignment = Alignment.Center
                ) {
                     Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextDark)
                }
            }
        }
    }
}
