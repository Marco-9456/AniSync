package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.ScoreBadge
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.StaggeredAnimatedVisibility
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.details.components.DetailsSkeletonContent
import com.anisync.android.presentation.details.components.ExpandableSynopsis
import com.anisync.android.presentation.details.components.GenreFlow
import com.anisync.android.presentation.details.components.RelationItem
import com.anisync.android.presentation.details.components.StatsCard
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.toIcon
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType

// Custom stagger delay for media details (faster reveal)
private const val MediaStaggerDelay = 10

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaDetailsScreen(
    mediaId: Int,
    sourceScreen: String = "unknown",
    onBackClick: () -> Unit,
    onRelationClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    viewModel: MediaDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(mediaId) {
        viewModel.loadMedia(mediaId)
    }

    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    with(sharedTransitionScope) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                val state = uiState
                if (state is DetailsUiState.Success) {
                    val details = state.details
                    val isManga = details.type == MediaType.MANGA
                    val statuses = listOf(
                        LibraryStatus.CURRENT,
                        LibraryStatus.PLANNING,
                        LibraryStatus.COMPLETED,
                        LibraryStatus.PAUSED,
                        LibraryStatus.DROPPED
                    )

                    FloatingActionButtonMenu(
                        expanded = fabMenuExpanded,
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.fab_menu_padding))
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                        button = {
                            ToggleFloatingActionButton(
                                checked = fabMenuExpanded,
                                onCheckedChange = { fabMenuExpanded = !fabMenuExpanded }
                            ) {
                                val imageVector by remember {
                                    derivedStateOf {
                                        if (checkedProgress > 0.5f) Icons.Filled.Close
                                        else if (details.listEntryId != null) Icons.Filled.Edit
                                        else Icons.Filled.Add
                                    }
                                }
                                Icon(
                                    painter = rememberVectorPainter(imageVector),
                                    contentDescription = if (fabMenuExpanded) stringResource(R.string.fab_close_menu) else stringResource(R.string.fab_open_menu),
                                    modifier = Modifier.animateIcon({ checkedProgress })
                                )
                            }
                        }
                    ) {
                        // Status options
                        statuses.forEach { status ->
                            val isSelected = status == details.listStatus
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    val progress = details.listProgress ?: 0
                                    viewModel.saveMediaListEntry(status, progress)
                                    fabMenuExpanded = false
                                },
                                icon = {
                                    Icon(
                                        imageVector = status.toIcon(details.type),
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                text = {
                                    Text(
                                        text = status.toLabel(details.type),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            )
                        }

                        // Remove from Library option
                        if (details.listEntryId != null) {
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    viewModel.deleteMediaListEntry()
                                    fabMenuExpanded = false
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = {
                                    Text(
                                        text = stringResource(R.string.action_remove),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "${sourceScreen}_container_${mediaId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp))
                    )
            ) {
                when (val state = uiState) {
                    is DetailsUiState.Loading -> {
                        // Skeleton loading state for premium feel
                        DetailsSkeletonContent(onBackClick = onBackClick)
                    }
                    is DetailsUiState.Success -> {
                        DetailsPageContent(
                            details = state.details,
                            sourceScreen = sourceScreen,
                            onBackClick = onBackClick,
                            onRelationClick = onRelationClick,
                            onCharacterClick = onCharacterClick,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                    is DetailsUiState.Error -> {
                        ErrorStateContent(message = state.message, onBackClick = onBackClick)
                    }
                }
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
                imageVector = Icons.Default.Delete, // Or an error icon
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_large))
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            Text(
                text = stringResource(R.string.error_something_went_wrong),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onBackClick) {
                Text(stringResource(R.string.action_go_back))
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailsPageContent(
    details: MediaDetails,
    sourceScreen: String,
    onBackClick: () -> Unit,
    onRelationClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.list_bottom_padding_fab)) // Space for FAB
        ) {
            item {
                PageHeaderSection(details, sourceScreen, onBackClick, sharedTransitionScope, animatedVisibilityScope)
            }

            item {
                Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))) {
                    // Title Group (stagger index 0)
                    StaggeredAnimatedVisibility(index = 0, delayPerItem = MediaStaggerDelay) {
                        TitleSection(
                            details = details,
                            sourceScreen = sourceScreen,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                    // Stats (stagger index 1)
                    StaggeredAnimatedVisibility(index = 1, delayPerItem = MediaStaggerDelay) {
                        StatsCard(details)
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                    // Genres (stagger index 2)
                    StaggeredAnimatedVisibility(index = 2, delayPerItem = MediaStaggerDelay) {
                        GenreFlow(details.genres)
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                    // Synopsis (stagger index 3)
                    StaggeredAnimatedVisibility(index = 3, delayPerItem = MediaStaggerDelay) {
                        ExpandableSynopsis(details.description)
                    }
                }
            }

            item {
                if (details.characters.isNotEmpty()) {
                    // Cast section (stagger index 4)
                    StaggeredAnimatedVisibility(index = 4, delayPerItem = MediaStaggerDelay) {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                            SectionHeader(stringResource(R.string.section_cast), level = HeaderLevel.Section)
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
                            ) {
                                items(
                                    items = details.characters,
                                    key = { it.id }
                                ) { CharacterItem(it, onClick = { onCharacterClick(it.id) }, modifier = Modifier.animateItem()) }
                            }
                        }
                    }
                }
            }

            item {
                if (details.relations.isNotEmpty()) {
                    // Relations section (stagger index 5)
                    StaggeredAnimatedVisibility(index = 5, delayPerItem = MediaStaggerDelay) {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                            SectionHeader(stringResource(R.string.section_related), level = HeaderLevel.Section)
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_normal))
                            ) {
                                items(
                                    items = details.relations,
                                    key = { it.id }
                                ) { relation ->
                                    RelationItem(
                                        relation = relation,
                                        onClick = { onRelationClick(relation.id) },
                                        modifier = Modifier.animateItem()
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

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TitleSection(
    details: MediaDetails,
    sourceScreen: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    Column {
        with(sharedTransitionScope) {
            Text(
                text = details.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "${sourceScreen}_media_title_${details.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                )
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_normal)))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
            modifier = Modifier.fillMaxWidth()
        ) {
            details.score?.let { ScoreBadge(it) }

            details.format?.let { format ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium))
                ) {
                    Text(
                        text = if (format == "TV") stringResource(R.string.format_tv_series) else format.formatAsTitle() ?: format,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_small), vertical = dimensionResource(R.dimen.spacing_tiny)),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = details.year?.toString() ?: "",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (details.studio != null) {
                Text(
                    stringResource(R.string.separator_bullet),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_tiny))
                )
                Text(
                    text = details.studio,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = dimensionResource(R.dimen.studio_name_max_width))
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageHeaderSection(
    details: MediaDetails,
    sourceScreen: String,
    onBackClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.details_header_height)) // Slightly taller for better proportions
    ) {
        // Banner Image
        AsyncImage(
            model = details.bannerUrl ?: details.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.details_banner_height))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // Gradient Scrim (Top for status bar/back button visibility)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.details_scrim_height_top))
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Gradient Scrim (Bottom to blend into background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.details_scrim_height_bottom)) // Taller fade for smoother transition
                .align(Alignment.TopCenter) // Align to top box but offset
                .offset(y = dimensionResource(R.dimen.details_scrim_offset_y)) // Start where the banner ends
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        // Hard cut gradient overlay at the bottom of the banner image itself
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.details_scrim_height_top))
                .align(Alignment.TopCenter)
                .offset(y = dimensionResource(R.dimen.details_scrim_hard_cut_offset_y))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )


        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + dimensionResource(R.dimen.spacing_small), start = dimensionResource(R.dimen.spacing_small))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium)) // Slightly larger touch target visual
            )
        }

        // Cover Image (Poster)
        with(sharedTransitionScope) {
            val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = dimensionResource(R.dimen.spacing_large))
                    .width(dimensionResource(R.dimen.details_cover_width))
                    .height(dimensionResource(R.dimen.details_cover_height))
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "${sourceScreen}_media_cover_${details.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)))
                    ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)),
                elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation_large)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val cacheKey = "${sourceScreen}_cover_${details.id}"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(details.coverUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = stringResource(R.string.content_description_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}