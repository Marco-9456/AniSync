package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.anisync.android.presentation.util.posterGridColumns
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.domain.StaffProductionMedia
import com.anisync.android.domain.StudioMediaEntry
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.details.components.FeaturedMediaItem
import com.anisync.android.presentation.details.components.MediaSort
import com.anisync.android.presentation.details.components.MediaSortBottomSheet
import com.anisync.android.presentation.details.components.RelationItem
import com.anisync.android.presentation.details.components.VoicedCharacterItem
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.util.getTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CharacterMediaGridScreen(
    characterId: Int,
    characterName: String,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    viewModel: CharacterDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf(MediaSort.POPULARITY) }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }
    var onlyOnList by rememberSaveable { mutableStateOf(false) }

    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedGridReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToMediaDetails: (Int) -> Unit = remember(onMediaClick) {
        { mediaId ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedGridReEnter = false
            onMediaClick(mediaId)
        }
    }

    val listState = rememberLazyGridState()

    val isGridEnteringFromBackStack by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isGridTargetingVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.targetState == EnterExitState.Visible
        }
    }
    val isGridFullyVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.Visible &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isSharedTransitionRunning by remember(sharedTransitionScope) {
        derivedStateOf { sharedTransitionScope?.isTransitionActive == true }
    }
    val shouldRenderTopBarInOverlay by remember(
        sharedTransitionScope,
        animatedVisibilityScope
    ) {
        derivedStateOf {
            sharedTransitionScope != null &&
                animatedVisibilityScope != null &&
                shouldKeepTopBarOverlayForReturn &&
                isGridTargetingVisible &&
                (
                    isGridEnteringFromBackStack ||
                        (hasObservedGridReEnter && isSharedTransitionRunning)
                    )
        }
    }
    val topBarOverlayAlpha = if (animatedVisibilityScope != null) {
        val alpha by animatedVisibilityScope.transition.animateFloat(label = "CharacterMediaGridTopBarOverlayAlpha") { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }
        alpha
    } else {
        1f
    }

    LaunchedEffect(shouldKeepTopBarOverlayForReturn, isGridEnteringFromBackStack) {
        if (shouldKeepTopBarOverlayForReturn && isGridEnteringFromBackStack) {
            hasObservedGridReEnter = true
        }
    }

    LaunchedEffect(
        shouldKeepTopBarOverlayForReturn,
        hasObservedGridReEnter,
        isGridFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepTopBarOverlayForReturn &&
            hasObservedGridReEnter &&
            isGridFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepTopBarOverlayForReturn = false
            hasObservedGridReEnter = false
        }
    }

    val topBarOverlayModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = 1f,
                    renderInOverlay = { shouldRenderTopBarInOverlay }
                )
                .graphicsLayer {
                    alpha = if (shouldRenderTopBarInOverlay) topBarOverlayAlpha else 1f
                }
        }
    } else {
        Modifier
    }

    // Key on the raw loaded count so paging walks forward when the "On my list" filter yields a page
    // with no matching items: the visible grid doesn't grow, so a layout-only trigger would stall with
    // hasNextPage still true (#89). A failed fetch leaves the count unchanged, so it won't retry.
    val loadedMediaCount = (uiState as? CharacterDetailsUiState.Success)?.details?.media?.size ?: 0
    LaunchedEffect(listState, loadedMediaCount) {
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

    CollapsingTopBarScaffold(
        title = stringResource(R.string.featured_media),
        onBackClick = onBackClick,
        scrollableState = listState,
        topBarModifier = topBarOverlayModifier,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
        actions = {
            IconButton(onClick = { showSortSheet = true }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = stringResource(R.string.sort)
                )
            }
        }
    ) { topContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is CharacterDetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                is CharacterDetailsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topContentPadding),
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
                            columns = posterGridColumns(baseMinSize = 100.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = topContentPadding + 8.dp,
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
                                        label = { Text(stringResource(R.string.filter_on_my_list)) }
                                    )
                                }
                            }

                            itemsIndexed(
                                sortedMedia,
                                key = { index, mediaItem -> "char_${mediaItem.id}_$index" }
                            ) { _, mediaItem ->
                                FeaturedMediaItem(
                                    mediaId = mediaItem.id,
                                    coverUrl = mediaItem.coverUrl,
                                    title = mediaItem.getTitle(titleLanguage),
                                    type = mediaItem.type?.name,
                                    role = mediaItem.characterRole,
                                    year = mediaItem.startYear,
                                    onClick = { navigateToMediaDetails(mediaItem.id) },
                                    fillCell = true,
                                    transitionPrefix = com.anisync.android.presentation.util.TransitionKeys.CHARACTER_GRID,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun StaffMediaGridScreen(
    staffId: Int,
    staffName: String,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    viewModel: StaffDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf(MediaSort.NEWEST) }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }
    var onlyOnList by rememberSaveable { mutableStateOf(false) }

    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedGridReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToMediaDetails: (Int) -> Unit = remember(onMediaClick) {
        { mediaId ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedGridReEnter = false
            onMediaClick(mediaId)
        }
    }
    val navigateToCharacterDetails: (Int) -> Unit = remember(onCharacterClick) {
        { characterId ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedGridReEnter = false
            onCharacterClick(characterId)
        }
    }

    val listState = rememberLazyListState()

    val isGridEnteringFromBackStack by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isGridTargetingVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.targetState == EnterExitState.Visible
        }
    }
    val isGridFullyVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.Visible &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isSharedTransitionRunning by remember(sharedTransitionScope) {
        derivedStateOf { sharedTransitionScope?.isTransitionActive == true }
    }
    val shouldRenderTopBarInOverlay by remember(
        sharedTransitionScope,
        animatedVisibilityScope
    ) {
        derivedStateOf {
            sharedTransitionScope != null &&
                animatedVisibilityScope != null &&
                shouldKeepTopBarOverlayForReturn &&
                isGridTargetingVisible &&
                (
                    isGridEnteringFromBackStack ||
                        (hasObservedGridReEnter && isSharedTransitionRunning)
                    )
        }
    }
    val topBarOverlayAlpha = if (animatedVisibilityScope != null) {
        val alpha by animatedVisibilityScope.transition.animateFloat(label = "StaffMediaGridTopBarOverlayAlpha") { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }
        alpha
    } else {
        1f
    }

    LaunchedEffect(shouldKeepTopBarOverlayForReturn, isGridEnteringFromBackStack) {
        if (shouldKeepTopBarOverlayForReturn && isGridEnteringFromBackStack) {
            hasObservedGridReEnter = true
        }
    }

    LaunchedEffect(
        shouldKeepTopBarOverlayForReturn,
        hasObservedGridReEnter,
        isGridFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepTopBarOverlayForReturn &&
            hasObservedGridReEnter &&
            isGridFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepTopBarOverlayForReturn = false
            hasObservedGridReEnter = false
        }
    }

    val topBarOverlayModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = 1f,
                    renderInOverlay = { shouldRenderTopBarInOverlay }
                )
                .graphicsLayer {
                    alpha = if (shouldRenderTopBarInOverlay) topBarOverlayAlpha else 1f
                }
        }
    } else {
        Modifier
    }

    // Key on the raw loaded count so paging walks forward under the "On my list" filter (#89).
    val loadedMediaCount = (uiState as? StaffDetailsUiState.Success)?.details?.voicedCharacters?.size ?: 0
    LaunchedEffect(listState, loadedMediaCount) {
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

    CollapsingTopBarScaffold(
        title = stringResource(R.string.voiced_characters),
        onBackClick = onBackClick,
        scrollableState = listState,
        topBarModifier = topBarOverlayModifier,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
        actions = {
            IconButton(onClick = { showSortSheet = true }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = stringResource(R.string.sort)
                )
            }
        }
    ) { topContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is StaffDetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                is StaffDetailsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topContentPadding),
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
                            contentPadding = PaddingValues(
                                top = topContentPadding + 8.dp,
                                bottom = 96.dp
                            ),
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
                                        label = { Text(stringResource(R.string.filter_on_my_list)) }
                                    )
                                }
                            }

                            itemsIndexed(
                                sortedAppearances,
                                key = { index, vc -> "staff_media_${vc.characterId}_$index" }
                            ) { _, vc ->
                                VoicedCharacterItem(
                                    voicedCharacter = vc,
                                    titleLanguage = titleLanguage,
                                    onCharacterClick = { navigateToCharacterDetails(vc.characterId) },
                                    onMediaClick = navigateToMediaDetails,
                                    transitionPrefix = com.anisync.android.presentation.util.TransitionKeys.STAFF_GRID,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
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

    val listState = rememberLazyGridState()

    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedGridReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToRelationDetails: (Int) -> Unit = remember(onRelationClick) {
        { relationMediaId ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedGridReEnter = false
            onRelationClick(relationMediaId)
        }
    }

    val isGridEnteringFromBackStack by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isGridTargetingVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.targetState == EnterExitState.Visible
        }
    }
    val isGridFullyVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.Visible &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isSharedTransitionRunning by remember(sharedTransitionScope) {
        derivedStateOf { sharedTransitionScope?.isTransitionActive == true }
    }
    val shouldRenderTopBarInOverlay by remember(
        sharedTransitionScope,
        animatedVisibilityScope
    ) {
        derivedStateOf {
            sharedTransitionScope != null &&
                animatedVisibilityScope != null &&
                shouldKeepTopBarOverlayForReturn &&
                isGridTargetingVisible &&
                (
                    isGridEnteringFromBackStack ||
                        (hasObservedGridReEnter && isSharedTransitionRunning)
                    )
        }
    }
    val topBarOverlayAlpha = if (animatedVisibilityScope != null) {
        val alpha by animatedVisibilityScope.transition.animateFloat(label = "MediaRelationsGridTopBarOverlayAlpha") { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }
        alpha
    } else {
        1f
    }

    LaunchedEffect(shouldKeepTopBarOverlayForReturn, isGridEnteringFromBackStack) {
        if (shouldKeepTopBarOverlayForReturn && isGridEnteringFromBackStack) {
            hasObservedGridReEnter = true
        }
    }

    LaunchedEffect(
        shouldKeepTopBarOverlayForReturn,
        hasObservedGridReEnter,
        isGridFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepTopBarOverlayForReturn &&
            hasObservedGridReEnter &&
            isGridFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepTopBarOverlayForReturn = false
            hasObservedGridReEnter = false
        }
    }

    val topBarOverlayModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = 1f,
                    renderInOverlay = { shouldRenderTopBarInOverlay }
                )
                .graphicsLayer {
                    alpha = if (shouldRenderTopBarInOverlay) topBarOverlayAlpha else 1f
                }
        }
    } else {
        Modifier
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.section_related),
        onBackClick = onBackClick,
        scrollableState = listState,
        topBarModifier = topBarOverlayModifier,
        scrolledContainerColor = MaterialTheme.colorScheme.background
    ) { topContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is DetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                is DetailsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is DetailsUiState.Success -> {
                    val relations = state.details.relations
                    if (relations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topContentPadding),
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
                            state = listState,
                            columns = posterGridColumns(baseMinSize = 100.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = topContentPadding + 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(relations, key = { "${it.id}_${it.relationType}" }) { relation ->
                                RelationItem(
                                    relation = relation,
                                    onClick = { navigateToRelationDetails(relation.id) },
                                    fillCell = true,
                                    transitionPrefix = com.anisync.android.presentation.util.TransitionKeys.RELATIONS_GRID,
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
fun StudioMediaGridScreen(
    studioId: Int,
    studioName: String,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    viewModel: StudioDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf(MediaSort.NEWEST) }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }
    var onlyOnList by rememberSaveable { mutableStateOf(false) }
    var onlyMainStudio by rememberSaveable { mutableStateOf(false) }

    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedGridReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToMediaDetails: (Int) -> Unit = remember(onMediaClick) {
        { mediaId ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedGridReEnter = false
            onMediaClick(mediaId)
        }
    }

    val listState = rememberLazyGridState()

    val isGridEnteringFromBackStack by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isGridTargetingVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.targetState == EnterExitState.Visible
        }
    }
    val isGridFullyVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.Visible &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isSharedTransitionRunning by remember(sharedTransitionScope) {
        derivedStateOf { sharedTransitionScope?.isTransitionActive == true }
    }
    val shouldRenderTopBarInOverlay by remember(
        sharedTransitionScope,
        animatedVisibilityScope
    ) {
        derivedStateOf {
            sharedTransitionScope != null &&
                animatedVisibilityScope != null &&
                shouldKeepTopBarOverlayForReturn &&
                isGridTargetingVisible &&
                (
                    isGridEnteringFromBackStack ||
                        (hasObservedGridReEnter && isSharedTransitionRunning)
                    )
        }
    }
    val topBarOverlayAlpha = if (animatedVisibilityScope != null) {
        val alpha by animatedVisibilityScope.transition.animateFloat(label = "StudioMediaGridTopBarOverlayAlpha") { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }
        alpha
    } else {
        1f
    }

    LaunchedEffect(shouldKeepTopBarOverlayForReturn, isGridEnteringFromBackStack) {
        if (shouldKeepTopBarOverlayForReturn && isGridEnteringFromBackStack) {
            hasObservedGridReEnter = true
        }
    }

    LaunchedEffect(
        shouldKeepTopBarOverlayForReturn,
        hasObservedGridReEnter,
        isGridFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepTopBarOverlayForReturn &&
            hasObservedGridReEnter &&
            isGridFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepTopBarOverlayForReturn = false
            hasObservedGridReEnter = false
        }
    }

    val topBarOverlayModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = 1f,
                    renderInOverlay = { shouldRenderTopBarInOverlay }
                )
                .graphicsLayer {
                    alpha = if (shouldRenderTopBarInOverlay) topBarOverlayAlpha else 1f
                }
        }
    } else {
        Modifier
    }

    // Key on the raw loaded count so paging walks forward under the "On my list" / "Main studio" filters (#89).
    val loadedMediaCount = (uiState as? StudioDetailsUiState.Success)?.details?.media?.size ?: 0
    LaunchedEffect(listState, loadedMediaCount) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && uiState is StudioDetailsUiState.Success) {
                    val details = (uiState as StudioDetailsUiState.Success).details
                    val totalItems = listState.layoutInfo.totalItemsCount
                    if (lastIndex >= totalItems - 4 && details.hasNextPage) {
                        viewModel.loadMoreMedia()
                    }
                }
            }
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.studio_label_works),
        onBackClick = onBackClick,
        scrollableState = listState,
        topBarModifier = topBarOverlayModifier,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
        actions = {
            IconButton(onClick = { showSortSheet = true }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = stringResource(R.string.sort)
                )
            }
        }
    ) { topContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is StudioDetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                is StudioDetailsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is StudioDetailsUiState.Success -> {
                    val sortedMedia =
                        remember(state.details.media, selectedSort, isSortAscending, onlyOnList, onlyMainStudio) {
                            val filtered = state.details.media
                                .let { if (onlyOnList) it.filter { m -> m.isOnList } else it }
                                .let { if (onlyMainStudio) it.filter { m -> m.isMainStudio } else it }
                            sortStudioMedia(filtered, selectedSort, isSortAscending)
                        }

                    if (sortedMedia.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topContentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (onlyOnList) "No works on your list" else "No works",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val mainStudioChipLabel = stringResource(R.string.studio_main_studio_chip)
                        LazyVerticalGrid(
                            state = listState,
                            columns = posterGridColumns(baseMinSize = 100.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = topContentPadding + 8.dp,
                                end = 16.dp,
                                bottom = 96.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = onlyOnList,
                                        onClick = { onlyOnList = !onlyOnList },
                                        label = { Text(stringResource(R.string.filter_on_my_list)) }
                                    )
                                    FilterChip(
                                        selected = onlyMainStudio,
                                        onClick = { onlyMainStudio = !onlyMainStudio },
                                        label = { Text(mainStudioChipLabel) }
                                    )
                                }
                            }

                            itemsIndexed(
                                sortedMedia,
                                key = { index, media -> "studio_${media.mediaId}_$index" }
                            ) { _, media ->
                                FeaturedMediaItem(
                                    mediaId = media.mediaId,
                                    coverUrl = media.coverUrl,
                                    cover = media.cover,
                                    title = media.titleUserPreferred,
                                    type = media.type?.name,
                                    role = if (media.isMainStudio) mainStudioChipLabel else null,
                                    year = media.year,
                                    onClick = { navigateToMediaDetails(media.mediaId) },
                                    fillCell = true,
                                    transitionPrefix = com.anisync.android.presentation.util.TransitionKeys.STUDIO_GRID,
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

private fun sortStudioMedia(
    media: List<StudioMediaEntry>,
    sort: MediaSort,
    ascending: Boolean
): List<StudioMediaEntry> {
    val sorted = media.sortedWith(compareBy { m ->
        when (sort) {
            MediaSort.POPULARITY -> m.popularity ?: 0
            MediaSort.AVERAGE_SCORE -> m.averageScore ?: 0
            MediaSort.FAVORITES -> m.favourites ?: 0
            MediaSort.NEWEST -> m.year ?: 0
            MediaSort.OLDEST -> m.year ?: Int.MAX_VALUE
            MediaSort.TITLE -> m.titleUserPreferred.lowercase()
        }
    })
    return if (ascending) sorted else sorted.reversed()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun StaffProductionMediaGridScreen(
    staffId: Int,
    staffName: String,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    viewModel: StaffDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf(MediaSort.NEWEST) }
    var isSortAscending by rememberSaveable { mutableStateOf(false) }
    var onlyOnList by rememberSaveable { mutableStateOf(false) }

    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedGridReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToMediaDetails: (Int) -> Unit = remember(onMediaClick) {
        { mediaId ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedGridReEnter = false
            onMediaClick(mediaId)
        }
    }

    val listState = rememberLazyGridState()

    val isGridEnteringFromBackStack by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isGridTargetingVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.targetState == EnterExitState.Visible
        }
    }
    val isGridFullyVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.Visible &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isSharedTransitionRunning by remember(sharedTransitionScope) {
        derivedStateOf { sharedTransitionScope?.isTransitionActive == true }
    }
    val shouldRenderTopBarInOverlay by remember(
        sharedTransitionScope,
        animatedVisibilityScope
    ) {
        derivedStateOf {
            sharedTransitionScope != null &&
                animatedVisibilityScope != null &&
                shouldKeepTopBarOverlayForReturn &&
                isGridTargetingVisible &&
                (
                    isGridEnteringFromBackStack ||
                        (hasObservedGridReEnter && isSharedTransitionRunning)
                    )
        }
    }
    val topBarOverlayAlpha = if (animatedVisibilityScope != null) {
        val alpha by animatedVisibilityScope.transition.animateFloat(label = "StaffProductionGridTopBarOverlayAlpha") { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }
        alpha
    } else {
        1f
    }

    LaunchedEffect(shouldKeepTopBarOverlayForReturn, isGridEnteringFromBackStack) {
        if (shouldKeepTopBarOverlayForReturn && isGridEnteringFromBackStack) {
            hasObservedGridReEnter = true
        }
    }

    LaunchedEffect(
        shouldKeepTopBarOverlayForReturn,
        hasObservedGridReEnter,
        isGridFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepTopBarOverlayForReturn &&
            hasObservedGridReEnter &&
            isGridFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepTopBarOverlayForReturn = false
            hasObservedGridReEnter = false
        }
    }

    val topBarOverlayModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = 1f,
                    renderInOverlay = { shouldRenderTopBarInOverlay }
                )
                .graphicsLayer {
                    alpha = if (shouldRenderTopBarInOverlay) topBarOverlayAlpha else 1f
                }
        }
    } else {
        Modifier
    }

    // Key on the raw loaded count so paging walks forward under the "On my list" filter (#89).
    val loadedProductionCount = (uiState as? StaffDetailsUiState.Success)?.details?.productionMedia?.size ?: 0
    LaunchedEffect(listState, loadedProductionCount) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && uiState is StaffDetailsUiState.Success) {
                    val details = (uiState as StaffDetailsUiState.Success).details
                    val totalItems = listState.layoutInfo.totalItemsCount
                    if (lastIndex >= totalItems - 4 && details.productionMediaHasNextPage) {
                        viewModel.loadMoreProductionMedia()
                    }
                }
            }
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.production_roles),
        onBackClick = onBackClick,
        scrollableState = listState,
        topBarModifier = topBarOverlayModifier,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
        actions = {
            IconButton(onClick = { showSortSheet = true }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = stringResource(R.string.sort)
                )
            }
        }
    ) { topContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is StaffDetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                is StaffDetailsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is StaffDetailsUiState.Success -> {
                    val sortedMedia =
                        remember(state.details.productionMedia, selectedSort, isSortAscending, onlyOnList) {
                            val filtered = if (onlyOnList) {
                                state.details.productionMedia.filter { it.isOnList }
                            } else {
                                state.details.productionMedia
                            }
                            sortProductionMedia(filtered, selectedSort, isSortAscending)
                        }

                    if (sortedMedia.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topContentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (onlyOnList) "No works on your list" else "No production roles",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            state = listState,
                            columns = posterGridColumns(baseMinSize = 100.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = topContentPadding + 8.dp,
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
                                        label = { Text(stringResource(R.string.filter_on_my_list)) }
                                    )
                                }
                            }

                            itemsIndexed(
                                sortedMedia,
                                key = { index, media -> "staff_prod_grid_${media.mediaId}_${media.staffRole.orEmpty()}_$index" }
                            ) { _, media ->
                                FeaturedMediaItem(
                                    mediaId = media.mediaId,
                                    coverUrl = media.coverUrl,
                                    cover = media.cover,
                                    title = media.getTitle(titleLanguage),
                                    type = media.type?.name,
                                    role = media.staffRole,
                                    year = media.startYear,
                                    onClick = { navigateToMediaDetails(media.mediaId) },
                                    fillCell = true,
                                    transitionPrefix = com.anisync.android.presentation.util.TransitionKeys.STAFF_PRODUCTION_GRID,
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

private fun sortProductionMedia(
    media: List<StaffProductionMedia>,
    sort: MediaSort,
    ascending: Boolean
): List<StaffProductionMedia> {
    val sorted = media.sortedWith(compareBy { m ->
        when (sort) {
            MediaSort.POPULARITY -> m.popularity ?: 0
            MediaSort.AVERAGE_SCORE -> m.averageScore ?: 0
            MediaSort.FAVORITES -> m.favourites ?: 0
            MediaSort.NEWEST -> m.startYear ?: 0
            MediaSort.OLDEST -> m.startYear ?: Int.MAX_VALUE
            MediaSort.TITLE -> m.titleUserPreferred.lowercase()
        }
    })
    return if (ascending) sorted else sorted.reversed()
}
