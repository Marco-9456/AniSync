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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.SkeletonGrid
import com.anisync.android.presentation.components.SkeletonList
import com.anisync.android.presentation.util.formatTimeUntilAiring
import com.anisync.android.presentation.util.formatEpisodesBehind
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.presentation.util.bouncyClickable
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
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedStatus by rememberSaveable { mutableStateOf(LibraryStatus.CURRENT) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LibraryEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    LaunchedEffect(sortOption, selectedStatus, mediaType) {
        if (isGridView) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0)
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
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.library_title),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .bouncyClickable(pressedScale = 0.9f) {
                                        haptic.click()
                                        showSortMenu = true
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = stringResource(R.string.sort),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

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

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(40.dp)
                                .bouncyClickable(pressedScale = 0.9f) {
                                    haptic.click()
                                    isGridView = !isGridView
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                    contentDescription = stringResource(R.string.toggle_view),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    ToggleButton(
                        checked = mediaType == MediaType.ANIME,
                        onCheckedChange = {
                            haptic.click()
                            viewModel.onMediaTypeChange(MediaType.ANIME)
                        },
                        modifier = Modifier.weight(1f),
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text(text = stringResource(R.string.media_type_anime), fontWeight = FontWeight.SemiBold)
                    }
                    ToggleButton(
                        checked = mediaType == MediaType.MANGA,
                        onCheckedChange = {
                            haptic.click()
                            viewModel.onMediaTypeChange(MediaType.MANGA)
                        },
                        modifier = Modifier.weight(1f),
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text(text = stringResource(R.string.media_type_manga), fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val statuses = listOf(LibraryStatus.CURRENT, LibraryStatus.PAUSED, LibraryStatus.COMPLETED, LibraryStatus.PLANNING, LibraryStatus.DROPPED)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
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
                            label = { Text(getStatusLabel(status, mediaType)) },
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
                is LibraryUiState.Error -> ErrorState(state.message) { viewModel.refresh() }
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
                                    contentPadding = PaddingValues(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(entries, key = { it.id }) { entry ->
                                        NewGridCard(
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
                            } else {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(20.dp),
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
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewGridCard(
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
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val haptic = rememberHapticFeedback()
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val progressPercent = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f

    // Animate progress using MotionScheme
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "Progress"
    )

    with(sharedTransitionScope) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(340.dp)
                .bouncyClickable(onClick = onClick)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "library_container_${entry.mediaId}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                ) // Using unified interaction
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AsyncImage(
                        model = entry.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "library_gradient_${entry.mediaId}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> spatialSpec },
                                enter = fadeIn(effectsSpec),
                                exit = fadeOut(effectsSpec)
                            )
                            .background(
                                Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 200f)
                            )
                    )
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "library_media_title_${entry.mediaId}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> spatialSpec }
                            )
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val nextAiring = entry.nextAiringEpisode
                        val latest = if (nextAiring != null) nextAiring - 1 else total

                        if (entry.status == LibraryStatus.CURRENT) {
                            if (latest != null && entry.progress < latest) {
                                StatusBadge(formatEpisodesBehind(latest - entry.progress), Color(0xFFB3261E), Color.White)
                            } else {
                                StatusBadge(stringResource(R.string.badge_up_to_date), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        if (entry.timeUntilAiring != null && entry.nextAiringEpisode != null) {
                            Text(
                                text = stringResource(R.string.airing_episode_in, entry.nextAiringEpisode, formatTimeUntilAiring(entry.timeUntilAiring)),
                                style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        )
                        Text(
                            text = stringResource(R.string.progress_format, entry.progress, total?.toString() ?: stringResource(R.string.progress_unknown)),
                            style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontSize = 10.sp
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(36.dp).bouncyClickable { haptic.click(); onDecrement() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp)) }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).height(36.dp).bouncyClickable { haptic.click(); onIncrement() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
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
                AsyncImage(
                    model = entry.coverUrl,
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
                                boundsTransform = { _, _ -> spatialSpec }
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val nextAiring = entry.nextAiringEpisode
                            val latest = if (nextAiring != null) nextAiring - 1 else total
                            if (entry.status == LibraryStatus.CURRENT) {
                                if (latest != null && entry.progress < latest) {
                                    StatusBadge(formatEpisodesBehind(latest - entry.progress), Color(0xFFF2B8B5), Color(0xFF601410))
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
fun StatusBadge(text: String, containerColor: Color, contentColor: Color) {
    Surface(color = containerColor, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(18.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = contentColor)
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

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = stringResource(R.string.error_oops), style = MaterialTheme.typography.headlineMedium)
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        IconButton(onClick = onRetry) { Icon(Icons.Default.Add, null) }
    }
}

@Composable
fun getStatusLabel(status: LibraryStatus, type: MediaType): String {
    return when(status) {
        LibraryStatus.CURRENT -> if (type == MediaType.MANGA) stringResource(R.string.status_reading) else stringResource(R.string.status_watching)
        LibraryStatus.PLANNING -> stringResource(R.string.status_planning)
        LibraryStatus.COMPLETED -> stringResource(R.string.status_completed)
        LibraryStatus.PAUSED -> stringResource(R.string.status_paused)
        LibraryStatus.DROPPED -> stringResource(R.string.status_dropped)
        LibraryStatus.REPEATING -> stringResource(R.string.status_repeating)
        LibraryStatus.UNKNOWN -> stringResource(R.string.unknown)
    }
}