package com.anisync.android.presentation.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.SegmentedControl
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

    // UI View State
    var isGridView by remember { mutableStateOf(true) }
    var selectedStatus by remember { mutableStateOf(LibraryStatus.CURRENT) }

    // Scroll States
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Scroll to top when filter or view mode changes
    LaunchedEffect(selectedStatus, mediaType, isGridView) {
        if (isGridView) {
            gridState.scrollToItem(0)
        } else {
            listState.scrollToItem(0)
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LibraryHeader(
                selectedMediaType = mediaType,
                onMediaTypeChange = viewModel::onMediaTypeChange,
                selectedStatus = selectedStatus,
                onStatusChange = { selectedStatus = it },
                isGridView = isGridView,
                onViewToggle = { isGridView = !isGridView }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CreamBackground)
        ) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    LibraryLoadingShimmer(isGridView)
                }
                is LibraryUiState.Success -> {
                    val filteredEntries = remember(state.entries, selectedStatus) {
                        state.entries.filter { it.status == selectedStatus }
                    }

                    if (filteredEntries.isEmpty()) {
                        LibraryEmptyState(
                            message = "No ${mediaType.name.lowercase()} found in ${getStatusLabel(selectedStatus, mediaType)}",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        AnimatedContent(
                            targetState = isGridView,
                            label = "ViewTransition",
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                        fadeOut(animationSpec = tween(300))
                            }
                        ) { grid ->
                            if (grid) {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
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
                                    state = listState,
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
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
                    }
                }
                is LibraryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadLibrary() }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Retry")
                            }
                        }
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
            .background(CreamBackground)
            .padding(bottom = 8.dp)
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                spotColor = Color.LightGray.copy(0.2f)
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Title & View Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "My Library",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    )
                )

                IconButton(
                    onClick = onViewToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = BeigeYellow,
                        contentColor = OliveDrab
                    )
                ) {
                    AnimatedContent(
                        targetState = isGridView,
                        label = "IconAnim",
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                        }
                    ) { isGrid ->
                        Icon(
                            imageVector = if (isGrid) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Control (Anime/Manga)
            SegmentedControl(
                options = listOf("Anime", "Manga"),
                selectedOption = if (selectedMediaType == MediaType.ANIME) "Anime" else "Manga",
                onOptionSelected = {
                    onMediaTypeChange(if (it == "Anime") MediaType.ANIME else MediaType.MANGA)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
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

                FilterChip(
                    selected = isSelected,
                    onClick = { onStatusChange(status) },
                    label = {
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OliveDrab,
                        selectedLabelColor = Color.White,
                        containerColor = SurfacePinkWhite,
                        labelColor = TextDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(50)
                )
            }
        }
    }
}

@Composable
fun LibraryEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LibraryLoadingShimmer(isGridView: Boolean) {
    val shimmerMod = Modifier
        .clip(RoundedCornerShape(12.dp))
        .shimmerEffect()

    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(6) {
                Box(modifier = Modifier.fillMaxWidth().height(320.dp).then(shimmerMod))
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(6) {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp).then(shimmerMod))
            }
        }
    }
}

// --- SHARED COMPONENT ---
@Composable
fun VerticalControlStrip(
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight().width(48.dp)
    ) {
        // Increment
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(PastelGreen.copy(alpha = 0.4f))
                .clickable { onIncrement() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Inc", tint = TextDark.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        }

        // Divider
        HorizontalDivider(color = Color.White, thickness = 1.dp)

        // Decrement
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(PastelPink.copy(alpha = 0.4f))
                .clickable { onDecrement() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Dec", tint = TextDark.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun HorizontalControlStrip(
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(40.dp)
    ) {
        // Decrement
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(PastelPink.copy(alpha = 0.4f))
                .clickable { onDecrement() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Dec", tint = TextDark.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        }

        // Divider
        VerticalDivider(color = Color.White, thickness = 1.dp)

        // Increment
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(PastelGreen.copy(alpha = 0.4f))
                .clickable { onIncrement() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Inc", tint = TextDark.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
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

    // Animated Progress
    val animatedProgress by animateFloatAsState(
        targetValue = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f,
        label = "ProgressAnimation",
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )



    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(140.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left: Cover Image + Status Badge area (Updated)
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

            // Middle: Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = TextDark,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Text
                Text(
                    text = "$unitLabel ${entry.progress} / ${total ?: "?"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )

                // Labels (Behind / Next Airing) - Only for Current Status
                if (entry.status == LibraryStatus.CURRENT) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Behind Label
                         val nextAiring = entry.nextAiringEpisode
                        val latestAvailable = if (nextAiring != null) nextAiring - 1 else total

                        if (latestAvailable != null && latestAvailable > entry.progress) {
                            val diff = latestAvailable - entry.progress
                            if (diff > 0) {
                                BadgeLabel(
                                    text = "$diff ${if (isManga) "Ch" else "Ep"} Behind",
                                    containerColor = BehindRed,
                                    contentColor = Color.White
                                )
                            }
                        }

                        // Next Airing Label
                        if (entry.nextAiringEpisode != null && entry.timeUntilAiring != null) {
                            BadgeLabel(
                                text = "Ep ${entry.nextAiringEpisode} in ${formatTimeUntilAiring(entry.timeUntilAiring)}",
                                containerColor = OliveDrab,
                                contentColor = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f) // Slight inset
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = OliveDrab,
                    trackColor = BeigeYellow.copy(alpha = 0.5f),
                )
            }

            // Right: Control Strip
            VerticalControlStrip(
                onIncrement = onIncrement,
                onDecrement = onDecrement
            )
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

    // Animated Progress
    val animatedProgress by animateFloatAsState(
        targetValue = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f,
        label = "ProgressAnimation",
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val statusLabel = getMediaStatusLabel(entry, isManga)
    val badgeColor = getStatusBadgeColor(entry)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp), // Increased height for footer
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top: Image Area
            Box(
                modifier = Modifier
                    .weight(0.55f) // 55% height
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = entry.coverUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlays
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Behind Label
                    val nextAiring = entry.nextAiringEpisode
                    val latestAvailable = if (nextAiring != null) nextAiring - 1 else total

                    if (entry.status == LibraryStatus.CURRENT && latestAvailable != null && latestAvailable > entry.progress) {
                        val diff = latestAvailable - entry.progress
                        if (diff > 0) {
                            BadgeLabel(
                                text = "$diff ${if (isManga) "Ch" else "Ep"} Behind",
                                containerColor = BehindRed,
                                contentColor = Color.White
                            )
                        }
                    }

                    // Next Airing Label
                    if (entry.status == LibraryStatus.CURRENT && entry.nextAiringEpisode != null && entry.timeUntilAiring != null) {
                        BadgeLabel(
                            text = "Ep ${entry.nextAiringEpisode} in ${formatTimeUntilAiring(entry.timeUntilAiring)}",
                            containerColor = OliveDrab,
                            contentColor = Color.White
                        )
                    }
                }
            }

            // Middle: Info Area
            Column(
                modifier = Modifier
                    .weight(0.45f) // 45% height
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = TextDark,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Start // Left Aligned
                    )

                    // Progress Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${entry.progress} / ${total ?: "?"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = OliveDrab,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = OliveDrab,
                            trackColor = BeigeYellow.copy(alpha = 0.5f),
                        )
                    }
                }

                // Bottom: Control Strip
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                HorizontalControlStrip(
                    onIncrement = onIncrement,
                    onDecrement = onDecrement
                )
            }
        }
    }
}

@Composable
fun BadgeLabel(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

private fun formatTimeUntilAiring(seconds: Int): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60

    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

private fun getStatusLabel(status: LibraryStatus, type: MediaType): String {
    return when(status) {
        LibraryStatus.CURRENT -> if (type == MediaType.MANGA) "Reading" else "Watching"
        LibraryStatus.PLANNING -> "Planning"
        LibraryStatus.COMPLETED -> "Completed"
        LibraryStatus.DROPPED -> "Dropped"
        LibraryStatus.PAUSED -> "Paused"
        LibraryStatus.REPEATING -> "Repeating"
        LibraryStatus.UNKNOWN -> "Unknown"
    }
}

// Deprecated: Status badge logic moved to individual card overlays
private fun getMediaStatusLabel(entry: LibraryEntry, isManga: Boolean): String {
     return "" 
}

private fun getStatusBadgeColor(entry: LibraryEntry): Color {
    return Color.Transparent
}