package com.anisync.android.presentation.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import com.anisync.android.presentation.components.MediaTypeSelector
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.anisync.android.presentation.components.EditLibraryEntryDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.RoundedIconButton
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.LibraryMediaCard
import com.anisync.android.presentation.components.StatusBadge
import com.anisync.android.presentation.components.WatchingCardConfig
import com.anisync.android.presentation.components.CompletedCardConfig
import com.anisync.android.presentation.components.SkeletonGrid
import com.anisync.android.presentation.components.SkeletonList
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.util.formatTimeUntilAiring
import com.anisync.android.presentation.util.formatEpisodesBehind
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryScreen(
    onMediaClick: (Int) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = rememberHapticFeedback()
    var isGridView by rememberSaveable { mutableStateOf(true) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var selectedStatus by rememberSaveable { mutableStateOf(LibraryStatus.CURRENT) }
    var editingEntry by remember { mutableStateOf<LibraryEntry?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LibraryEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Use rememberSaveable to persist scroll position across navigation
    // This ensures returning from Details screen restores the scroll position
    val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // Only scroll to top when filter/sort changes, not on initial composition
    var previousMediaType by rememberSaveable { mutableStateOf(mediaType) }
    var previousSelectedStatus by rememberSaveable { mutableStateOf(selectedStatus) }
    var previousSortOption by rememberSaveable { mutableStateOf(sortOption) }

    LaunchedEffect(sortOption, selectedStatus, mediaType) {
        // Only scroll to top if the user actually changed a filter, not on navigation return
        val filterChanged = mediaType != previousMediaType ||
                            selectedStatus != previousSelectedStatus ||
                            sortOption != previousSortOption

        if (filterChanged) {
            if (isGridView) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0)
            previousMediaType = mediaType
            previousSelectedStatus = selectedStatus
            previousSortOption = sortOption
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = stringResource(R.string.library_title),
                        level = HeaderLevel.Screen,
                        modifier = Modifier.weight(1f),
                        padding = PaddingValues(0.dp) // Reset padding as parent handles it
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            RoundedIconButton(
                                icon = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.sort),
                                onClick = {
                                    haptic.click()
                                    showSortMenu = true
                                }
                            )

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                shape = MenuDefaults.shape,
                                containerColor = MenuDefaults.containerColor
                            ) {
                                LibrarySort.entries.forEach { sort ->
                                    val isSelected = sortOption == sort
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = when (sort) {
                                                    LibrarySort.TITLE -> stringResource(R.string.sort_title_az)
                                                    LibrarySort.PROGRESS -> stringResource(R.string.sort_progress)
                                                    LibrarySort.AIRING_SOON -> stringResource(R.string.sort_airing_soon)
                                                    LibrarySort.SCORE -> stringResource(R.string.sort_score)
                                                    LibrarySort.LAST_UPDATED -> stringResource(R.string.sort_last_updated)
                                                    LibrarySort.LAST_ADDED -> stringResource(R.string.sort_last_added)
                                                    LibrarySort.START_DATE -> stringResource(R.string.sort_start_date)
                                                    LibrarySort.RELEASE_DATE -> stringResource(R.string.sort_release_date)
                                                },
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            haptic.click()
                                            viewModel.onSortChange(sort)
                                            showSortMenu = false
                                        },
                                        trailingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                            }
                        }

                        RoundedIconButton(
                            icon = if (isGridView) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = stringResource(R.string.toggle_view),
                            onClick = {
                                haptic.click()
                                isGridView = !isGridView
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                MediaTypeSelector(
                    selected = mediaType,
                    onSelect = viewModel::onMediaTypeChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                val statuses = listOf(LibraryStatus.CURRENT, LibraryStatus.PAUSED, LibraryStatus.COMPLETED, LibraryStatus.PLANNING, LibraryStatus.DROPPED)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(statuses) { status ->
                        val isSelected = selectedStatus == status
                        val statusIcon = when (status) {
                            LibraryStatus.CURRENT -> if (mediaType == MediaType.ANIME) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook
                            LibraryStatus.PAUSED -> Icons.Default.Pause
                            LibraryStatus.COMPLETED -> Icons.Default.Done
                            LibraryStatus.PLANNING -> Icons.Default.CalendarMonth
                            LibraryStatus.DROPPED -> Icons.Default.Close
                            else -> Icons.Default.Inbox
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.click()
                                selectedStatus = status
                            },
                            label = { Text(status.toLabel(mediaType)) },
                            leadingIcon = { Icon(imageVector = statusIcon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    if (isGridView) SkeletonGrid(itemCount = 6) else SkeletonList(itemCount = 6)
                }
                is LibraryUiState.Error -> ErrorState(message = state.message, onRetry = { viewModel.refresh() })
                is LibraryUiState.Success -> {
                    val entries by remember(state.entries, selectedStatus) {
                        derivedStateOf { state.entries.filter { it.status == selectedStatus } }
                    }

                    if (entries.isEmpty()) {
                        EmptyLibraryTabState(selectedStatus, mediaType)
                    } else {
                        // Using MotionScheme specs for the transition
                        val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                        val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

                        AnimatedContent(
                            targetState = isGridView,
                            transitionSpec = {
                                (slideInVertically(spatialSpec) { if (targetState) -it / 8 else it / 8 } + fadeIn(effectsSpec)) togetherWith
                                        (slideOutVertically(spatialSpec) { if (targetState) it / 8 else -it / 8 } + fadeOut(effectsSpec))
                            },
                            label = "ViewMode"
                        ) { isGrid ->
                            if (isGrid) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    state = gridState,
                                contentPadding = PaddingValues(24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(entries, key = { it.id }) { entry ->
                                        val cardConfig = if (selectedStatus == LibraryStatus.CURRENT) WatchingCardConfig else CompletedCardConfig
                                        LibraryMediaCard(
                                            entry = entry,
                                            mediaType = mediaType,
                                            onClick = { onMediaClick(entry.mediaId) },
                                            onIncrement = if (selectedStatus == LibraryStatus.CURRENT) { { viewModel.incrementProgress(entry.mediaId) } } else null,
                                            onDecrement = if (selectedStatus == LibraryStatus.CURRENT) { { viewModel.decrementProgress(entry.mediaId) } } else null,
                                            onEdit = { editingEntry = entry },
                                            config = cardConfig,
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = effectsSpec,
                                                fadeOutSpec = effectsSpec,
                                                placementSpec = spatialSpec
                                            )
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(entries, key = { it.id }) { entry ->
                                        NewListCard(
                                            entry = entry,
                                            mediaType = mediaType,
                                            onClick = { onMediaClick(entry.mediaId) },
                                            onIncrement = { viewModel.incrementProgress(entry.mediaId) },
                                            onDecrement = { viewModel.decrementProgress(entry.mediaId) },
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = effectsSpec,
                                                fadeOutSpec = effectsSpec,
                                                placementSpec = spatialSpec
                                            )
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

    if (editingEntry != null) {
        EditLibraryEntryDialog(
            entry = editingEntry!!,
            onDismiss = { editingEntry = null },
            onSave = {
                viewModel.updateEntry(it)
                editingEntry = null
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewListCard(
    entry: LibraryEntry,
    mediaType: MediaType,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val haptic = rememberHapticFeedback()
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val progressPercent = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "Progress"
    )

    with(sharedTransitionScope) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(110.dp)
                .bouncyClickable(onClick = onClick)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "library_container_${entry.mediaId}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                )
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val cacheKey = "library_cover_${entry.mediaId}"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(entry.coverUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(80.dp).fillMaxHeight().padding(8.dp).clip(RoundedCornerShape(12.dp))
                )

                Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp).padding(end = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "library_media_title_${entry.mediaId}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> spatialSpec },
                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val nextAiring = entry.nextAiringEpisode
                            val latest = if (nextAiring != null) nextAiring - 1 else total
                            if (entry.status == LibraryStatus.CURRENT) {
                                if (latest != null && entry.progress < latest) {
                                    StatusBadge(formatEpisodesBehind(latest - entry.progress), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                                } else {
                                    StatusBadge(stringResource(R.string.badge_up_to_date), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            if (entry.timeUntilAiring != null && entry.nextAiringEpisode != null) {
                                Text(
                                    text = stringResource(R.string.airing_episode_in, entry.nextAiringEpisode, formatTimeUntilAiring(entry.timeUntilAiring)),
                                    style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.progress_format, entry.progress, total?.toString() ?: stringResource(R.string.progress_unknown)),
                            style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontSize = 11.sp
                        )
                    }
                }

                Column(modifier = Modifier.width(48.dp).fillMaxHeight().padding(vertical = 12.dp, horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxWidth().bouncyClickable { haptic.click(); onIncrement() },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxWidth().bouncyClickable { haptic.click(); onDecrement() },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryTabState(status: LibraryStatus, type: MediaType) {
    val icon = when(status) {
        LibraryStatus.CURRENT -> if (type == MediaType.ANIME) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook
        LibraryStatus.PLANNING -> Icons.Default.Check
        LibraryStatus.COMPLETED -> Icons.Default.Check
        else -> Icons.Default.Inbox
    }
    val message = when(status) {
        LibraryStatus.CURRENT -> if(type == MediaType.ANIME) stringResource(R.string.empty_watching) else stringResource(R.string.empty_reading)
        LibraryStatus.PLANNING -> stringResource(R.string.empty_planning)
        LibraryStatus.COMPLETED -> stringResource(R.string.empty_completed)
        else -> stringResource(R.string.empty_default)
    }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(80.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

