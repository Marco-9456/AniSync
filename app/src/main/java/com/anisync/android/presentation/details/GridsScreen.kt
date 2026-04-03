package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.domain.CharacterMediaAppearance
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.details.components.FeaturedMediaItem
import com.anisync.android.presentation.details.components.MediaSort
import com.anisync.android.presentation.details.components.MediaSortBottomSheet
import com.anisync.android.presentation.details.components.MediaRoleItem
import com.anisync.android.presentation.details.components.RelationItem
import com.anisync.android.presentation.util.AppMotion

/**
 * Grid screen displaying all media a character appears in.
 * Includes sort bottom sheet and "On My List" filter.
 */
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

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf(MediaSort.POPULARITY) }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }
    var onlyOnList by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                    val sortedMedia = remember(state.details.media, selectedSort, isSortAscending, onlyOnList) {
                        val filtered = if (onlyOnList) {
                            state.details.media.filter { it.isOnList }
                        } else {
                            state.details.media
                        }
                        sortCharacterMedia(filtered, selectedSort, isSortAscending)
                    }

                    if (sortedMedia.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (onlyOnList) "No media on your list" else "No media appearances",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // On My List filter chip
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
                                    title = mediaItem.titleUserPreferred,
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
    val sorted = when (sort) {
        MediaSort.POPULARITY -> media.sortedBy { it.popularity ?: 0 }
        MediaSort.AVERAGE_SCORE -> media.sortedBy { it.averageScore ?: 0 }
        MediaSort.FAVORITES -> media.sortedBy { it.favourites ?: 0 }
        MediaSort.NEWEST -> media.sortedBy { it.startYear ?: 0 }
        MediaSort.OLDEST -> media.sortedBy { it.startYear ?: Int.MAX_VALUE }
        MediaSort.TITLE -> media.sortedBy { it.titleUserPreferred.lowercase() }
    }
    return if (ascending) sorted else sorted.reversed()
}

/**
 * Grid screen displaying all media a staff member voiced characters in.
 * Includes sort bottom sheet and "On My List" filter.
 */
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

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf(MediaSort.NEWEST) }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }
    var onlyOnList by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                    // Flatten voiced characters into media appearances
                    val allAppearances = remember(state.details.voicedCharacters) {
                        state.details.voicedCharacters.flatMap { vc ->
                            vc.mediaAppearances.map { ma -> vc to ma }
                        }.distinctBy { it.second.mediaId }
                    }

                    val sortedAppearances = remember(allAppearances, selectedSort, isSortAscending, onlyOnList) {
                        val filtered = if (onlyOnList) {
                            allAppearances.filter { it.second.isOnList }
                        } else {
                            allAppearances
                        }
                        sortStaffMedia(filtered, selectedSort, isSortAscending)
                    }

                    if (sortedAppearances.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (onlyOnList) "No media on your list" else "No voiced characters",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // On My List filter chip
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    FilterChip(
                                        selected = onlyOnList,
                                        onClick = { onlyOnList = !onlyOnList },
                                        label = { Text("On My List") }
                                    )
                                }
                            }

                            items(sortedAppearances, key = { it.second.mediaId }) { (vc, appearance) ->
                                FeaturedMediaItem(
                                    coverUrl = appearance.coverUrl,
                                    title = appearance.mediaTitle,
                                    role = appearance.characterRole,
                                    year = appearance.startYear,
                                    onClick = { onMediaClick(appearance.mediaId) }
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

private fun sortStaffMedia(
    media: List<Pair<com.anisync.android.domain.VoicedCharacter, CharacterMediaAppearance>>,
    sort: MediaSort,
    ascending: Boolean
): List<Pair<com.anisync.android.domain.VoicedCharacter, CharacterMediaAppearance>> {
    val sorted = when (sort) {
        MediaSort.POPULARITY -> media.sortedBy { it.second.popularity ?: 0 }
        MediaSort.AVERAGE_SCORE -> media.sortedBy { it.second.averageScore ?: 0 }
        MediaSort.FAVORITES -> media.sortedBy { it.second.favourites ?: 0 }
        MediaSort.NEWEST -> media.sortedBy { it.second.startYear ?: 0 }
        MediaSort.OLDEST -> media.sortedBy { it.second.startYear ?: Int.MAX_VALUE }
        MediaSort.TITLE -> media.sortedBy { it.second.mediaTitle.lowercase() }
    }
    return if (ascending) sorted else sorted.reversed()
}

/**
 * Grid screen displaying all characters from a media's cast.
 */
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
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

/**
 * Grid screen displaying all related media.
 */
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
