package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.rememberTopAppBarState
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
import com.anisync.android.presentation.components.InfoCard
import com.anisync.android.presentation.components.StaggeredAnimatedVisibility
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.details.components.DetailsSkeletonContent
import com.anisync.android.presentation.details.components.ExpandableSynopsis
import com.anisync.android.presentation.details.components.RelationItem
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.toIcon
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType

private const val MediaStaggerDelay = 10

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsScreen(
    mediaId: Int,
    sourceScreen: String = "unknown",
    onBackClick: () -> Unit,
    onRelationClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onCastSeeAllClick: (Int, String) -> Unit = { _, _ -> },
    onRelatedSeeAllClick: (Int, String) -> Unit = { _, _ -> },
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
    val listState = rememberLazyListState()
    
    // Setup ScrollBehavior for the TopAppBar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    
    with(sharedTransitionScope) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    val state = uiState
                        // Use standard overlappedFraction for state-based transitions
                        val isScrolled by remember { derivedStateOf { scrollBehavior.state.overlappedFraction > 0.01f } }

                        TopAppBar(
                            title = {
                                // Show title only when scrolled to avoid duplication with the header
                                AnimatedVisibility(
                                    visible = isScrolled,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    val title = (state as? DetailsUiState.Success)?.details?.title ?: ""
                                    Text(
                                        text = title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                        // Animate color between White (on image) and OnSurface (on background)
                                        tint = androidx.compose.animation.animateColorAsState(
                                            if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
                                            label = "navIconTint"
                                        ).value
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            windowInsets = WindowInsets(0),
                            scrollBehavior = scrollBehavior
                        )
                },
                floatingActionButton = {
                    val state = uiState
                    if (state is DetailsUiState.Success) {
                        val details = state.details
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
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = status.toLabel(details.type),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                )
                            }
                            if (details.listEntryId != null) {
                                FloatingActionButtonMenuItem(
                                    onClick = {
                                        viewModel.deleteMediaListEntry()
                                        fabMenuExpanded = false
                                    },
                                    icon = {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    text = {
                                        Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
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
                        // Apply only bottom padding to keep banner at top
                        .padding(bottom = paddingValues.calculateBottomPadding())
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "${sourceScreen}_container_${mediaId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp))
                        )
                ) {
                    when (val state = uiState) {
                        is DetailsUiState.Loading -> DetailsSkeletonContent(onBackClick = onBackClick)
                        is DetailsUiState.Success -> {
                            DetailsPageContent(
                                details = state.details,
                                sourceScreen = sourceScreen,
                                listState = listState,
                                onRelationClick = onRelationClick,
                                onCharacterClick = onCharacterClick,
                                onCastSeeAllClick = { onCastSeeAllClick(state.details.id, state.details.title) },
                                onRelatedSeeAllClick = { onRelatedSeeAllClick(state.details.id, state.details.title) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                        is DetailsUiState.Error -> ErrorStateContent(message = state.message, onBackClick = onBackClick)
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
                imageVector = Icons.Default.Delete,
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
    listState: LazyListState,
    onRelationClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onCastSeeAllClick: () -> Unit,
    onRelatedSeeAllClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.list_bottom_padding_fab))
        ) {
            item {
                PageHeaderSection(details, sourceScreen, sharedTransitionScope, animatedVisibilityScope)
            }

            item {
                Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                    // Info Cards (Format, Status, Date)
                    StaggeredAnimatedVisibility(key = "media_info_cards", index = 0, delayPerItem = MediaStaggerDelay) {
                        InfoCardsSection(details)
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                    // Genres (LazyRow)
                    StaggeredAnimatedVisibility(key = "media_genres", index = 1, delayPerItem = MediaStaggerDelay) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                        ) {
                            // Optimization: Add key for stable item identity
                            items(details.genres, key = { it }) { genre ->
                                SuggestionChip(
                                    onClick = { /* TODO: Filter by genre */ },
                                    label = { Text(genre) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    border = null,
                                    shape = CircleShape
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                    // Synopsis
                    StaggeredAnimatedVisibility(key = "media_synopsis", index = 2, delayPerItem = MediaStaggerDelay) {
                        ExpandableSynopsis(details.description)
                    }
                }
            }

            item {
                if (details.characters.isNotEmpty()) {
                    StaggeredAnimatedVisibility(key = "media_cast", index = 3, delayPerItem = MediaStaggerDelay) {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                            SectionHeader(
                                title = stringResource(R.string.section_cast),
                                level = HeaderLevel.Section,
                                onActionClick = if (details.characters.size > 10) onCastSeeAllClick else null
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
                                modifier = Modifier.height(dimensionResource(R.dimen.character_item_height))
                            ) {
                                items(
                                    items = details.characters.take(10),
                                    key = { it.id }
                                ) { character ->
                                    // Optimization: onCharacterClick is stable from parent
                                    CharacterItem(
                                        character,
                                        onClick = { onCharacterClick(character.id) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                if (details.relations.isNotEmpty()) {
                    StaggeredAnimatedVisibility(key = "media_related", index = 4, delayPerItem = MediaStaggerDelay) {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                            SectionHeader(
                                title = stringResource(R.string.section_related),
                                level = HeaderLevel.Section,
                                onActionClick = if (details.relations.size > 10) onRelatedSeeAllClick else null
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_normal)),
                                modifier = Modifier.height(dimensionResource(R.dimen.character_item_height))
                            ) {
                                items(
                                    items = details.relations.take(10),
                                    key = { it.id }
                                ) { relation ->
                                    // Optimization: onRelationClick is stable from parent
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
fun PageHeaderSection(
    details: MediaDetails,
    sourceScreen: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp) // Adjusted height
    ) {
        // Banner Image
        AsyncImage(
            model = details.bannerUrl ?: details.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Banner height
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

         // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Match banner height
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Poster and Title Row
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = dimensionResource(R.dimen.spacing_large))
        ) {
            // Cover Image (Poster)
            with(sharedTransitionScope) {
                 Card(
                    modifier = Modifier
                        .width(110.dp)
                        .height(160.dp)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "${sourceScreen}_media_cover_${details.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec }
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
            
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))
            
            // Title and Score
            Column(
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .padding(bottom = 8.dp) // Adjust to align with the bottom of the visible section text
            ) {
                 with(sharedTransitionScope) {
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "${sourceScreen}_media_title_${details.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                
                details.score?.let { ScoreBadge(it) }
            }
        }
    }
}

@Composable
fun InfoCardsSection(details: MediaDetails) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
        ) {
            // Format Card
            InfoCard(
                modifier = Modifier.weight(1f),
                icon = MediaDetailsIcons.getFormatIcon(details.format, details.type),
                label = stringResource(R.string.stat_format),
                value = details.format?.formatAsTitle() ?: stringResource(R.string.unknown),
                iconTint = MaterialTheme.colorScheme.tertiary // Pinkish/Red
            )
            // Status Card
            InfoCard(
                modifier = Modifier.weight(1f),
                icon = MediaDetailsIcons.getStatusIcon(details.status),
                label = stringResource(R.string.stat_status),
                value = details.status.formatAsTitle() ?: stringResource(R.string.unknown),
                iconTint = MediaDetailsIcons.getStatusColor(details.status),
                isStatus = true
            )
        }
        // Release Date Card
        val seasonText = if(details.season != null && details.seasonYear != null) {
            " • ${details.season.lowercase().replaceFirstChar { it.uppercase() }} ${details.seasonYear}"
        } else ""
        
        InfoCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.DateRange,
            label = stringResource(R.string.sort_release_date),
            value = "${details.startDate ?: stringResource(R.string.unknown)}$seasonText",
            iconTint = MaterialTheme.colorScheme.secondary
        )
    }
}

