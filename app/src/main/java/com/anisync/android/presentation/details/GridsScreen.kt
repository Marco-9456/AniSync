package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.details.components.FeaturedMediaItem
import com.anisync.android.presentation.details.components.MediaSort
import com.anisync.android.presentation.details.components.MediaSortBottomSheet
import com.anisync.android.presentation.details.components.RelationItem
import com.anisync.android.presentation.details.components.VoicedCharacterItem
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.util.getTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterMediaGridScreen(
    characterId: Int,
    characterName: String,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    viewModel: CharacterDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf(MediaSort.POPULARITY) }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }
    var onlyOnList by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyGridState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && uiState is CharacterDetailsUiState.Success) {
                    val details = (uiState as CharacterDetailsUiState.Success).details
                    val totalItems = listState.layoutInfo.totalItemsCount
                    if (lastIndex >= totalItems - 4 && details.hasNextPage) {
                        viewModel.loadMoreMedia()
                    }
                }
            }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Featured Media",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Sort"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is CharacterDetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is CharacterDetailsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is CharacterDetailsUiState.Success -> {
                    val sortedMedia =
                        remember(state.details.media, selectedSort, isSortAscending, onlyOnList) {
                            val filtered = if (onlyOnList) {
                                state.details.media.filter { it.isOnList }
                            } else {
                                state.details.media
                            }
                            sortCharacterMedia(filtered, selectedSort, isSortAscending)
                        }

                    if (sortedMedia.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (onlyOnList) "No media on your list" else "No media appearances",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            state = listState,
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 8.dp,
                                end = 16.dp,
                                bottom = 96.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    FilterChip(
                                        selected = onlyOnList,
                                        onClick = { onlyOnList = !onlyOnList },
                                        label = { Text("On My List") }
                                    )
                                }
                            }

                            items(sortedMedia, key = { it.id }) { mediaItem ->
                                FeaturedMediaItem(
                                    coverUrl = mediaItem.coverUrl,
                                    title = mediaItem.getTitle(titleLanguage),
                                    type = mediaItem.type?.name,
                                    role = mediaItem.characterRole,
                                    year = mediaItem.startYear,
                                    onClick = { onMediaClick(mediaItem.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    MediaSortBottomSheet(
        visible = showSortSheet,
        onDismiss = { showSortSheet = false },
        selectedSort = selectedSort,
        isAscending = isSortAscending,
        onSortSelected = { sort, ascending ->
            selectedSort = sort
            isSortAscending = ascending
        }
    )
}

private fun sortCharacterMedia(
    media: List<CharacterMedia>,
    sort: MediaSort,
    ascending: Boolean
): List<CharacterMedia> {
    val sorted = media.sortedWith(compareBy {
        when (sort) {
            MediaSort.POPULARITY -> it.popularity ?: 0
            MediaSort.AVERAGE_SCORE -> it.averageScore ?: 0
            MediaSort.FAVORITES -> it.favourites ?: 0
            MediaSort.NEWEST -> it.startYear ?: 0
            MediaSort.OLDEST -> it.startYear ?: Int.MAX_VALUE
            MediaSort.TITLE -> it.titleUserPreferred.lowercase()
        }
    })
    return if (ascending) sorted else sorted.reversed()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffMediaGridScreen(
    staffId: Int,
    staffName: String,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    viewModel: StaffDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf(MediaSort.NEWEST) }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }
    var onlyOnList by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && uiState is StaffDetailsUiState.Success) {
                    val details = (uiState as StaffDetailsUiState.Success).details
                    val totalItems = listState.layoutInfo.totalItemsCount
                    if (lastIndex >= totalItems - 3 &&
                        details.hasNextPage) {
                        viewModel.loadMoreMedia()
                    }
                }
            }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Voiced Characters",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Sort"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is StaffDetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is StaffDetailsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is StaffDetailsUiState.Success -> {
                    val allAppearances = state.details.voicedCharacters

                    val sortedAppearances =
                        remember(allAppearances, selectedSort, isSortAscending, onlyOnList) {
                            val filtered = if (onlyOnList) {
                                allAppearances.mapNotNull { vc ->
                                    val filteredApps = vc.mediaAppearances.filter { it.isOnList }
                                    if (filteredApps.isEmpty()) null else vc.copy(mediaAppearances = filteredApps)
                                }
                            } else {
                                allAppearances
                            }
                            sortVoicedCharacters(filtered, selectedSort, isSortAscending)
                        }

                    if (sortedAppearances.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (onlyOnList) "No media on your list" else "No voiced characters",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    FilterChip(
                                        selected = onlyOnList,
                                        onClick = { onlyOnList = !onlyOnList },
                                        label = { Text("On My List") }
                                    )
                                }
                            }

                            items(sortedAppearances, key = { it.characterId }) { vc ->
                                VoicedCharacterItem(
                                    voicedCharacter = vc,
                                    titleLanguage = titleLanguage,
                                    onCharacterClick = { onCharacterClick(vc.characterId) },
                                    onMediaClick = onMediaClick,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    MediaSortBottomSheet(
        visible = showSortSheet,
        onDismiss = { showSortSheet = false },
        selectedSort = selectedSort,
        isAscending = isSortAscending,
        onSortSelected = { sort, ascending ->
            selectedSort = sort
            isSortAscending = ascending
        }
    )
}

private fun sortVoicedCharacters(
    characters: List<com.anisync.android.domain.VoicedCharacter>,
    sort: MediaSort,
    ascending: Boolean
): List<com.anisync.android.domain.VoicedCharacter> {
    val sorted = characters.sortedWith(compareBy { vc ->
        when (sort) {
            MediaSort.POPULARITY -> vc.mediaAppearances.maxOfOrNull { it.popularity ?: 0 } ?: 0
            MediaSort.AVERAGE_SCORE -> vc.mediaAppearances.maxOfOrNull { it.averageScore ?: 0 } ?: 0
            MediaSort.FAVORITES -> vc.mediaAppearances.maxOfOrNull { it.favourites ?: 0 } ?: 0
            MediaSort.NEWEST -> vc.mediaAppearances.maxOfOrNull { it.startYear ?: 0 } ?: 0
            MediaSort.OLDEST -> vc.mediaAppearances.minOfOrNull { it.startYear ?: Int.MAX_VALUE }
                ?: Int.MAX_VALUE

            MediaSort.TITLE -> vc.characterName.lowercase()
        }
    })
    return if (ascending) sorted else sorted.reversed()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MediaCharactersGridScreen(
    mediaId: Int,
    mediaTitle: String,
    onBackClick: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    viewModel: MediaDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.section_cast),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when (val state = uiState) {
                is DetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is DetailsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is DetailsUiState.Success -> {
                    val characters = state.details.characters
                    if (characters.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_no_characters),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val placementSpec = AppMotion.rememberOffsetSpatialSpec()
                        val fadeSpec = AppMotion.rememberEffectsSpec()

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = 96.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(characters, key = { it.id }) { character ->
                                CharacterItem(
                                    character = character,
                                    onClick = { onCharacterClick(character.id) },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = fadeSpec,
                                        fadeOutSpec = fadeSpec,
                                        placementSpec = placementSpec
                                    ),
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MediaRelationsGridScreen(
    mediaId: Int,
    mediaTitle: String,
    onBackClick: () -> Unit,
    onRelationClick: (Int) -> Unit,
    viewModel: MediaDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.section_related),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is DetailsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is DetailsUiState.Success -> {
                    val relations = state.details.relations
                    if (relations.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_no_related),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val placementSpec = AppMotion.rememberOffsetSpatialSpec()
                        val fadeSpec = AppMotion.rememberEffectsSpec()

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(relations, key = { "${it.id}_${it.relationType}" }) { relation ->
                                RelationItem(
                                    relation = relation,
                                    onClick = { onRelationClick(relation.id) },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = fadeSpec,
                                        fadeOutSpec = fadeSpec,
                                        placementSpec = placementSpec
                                    ),
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
}