package com.anisync.android.presentation.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.components.CompletedCardConfig
import com.anisync.android.presentation.components.EditLibraryEntrySheet
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.LibraryMediaCard
import com.anisync.android.presentation.components.MediaTypeSelector
import com.anisync.android.presentation.components.SkeletonGrid
import com.anisync.android.presentation.components.SkeletonList
import com.anisync.android.presentation.components.SortBottomSheet
import com.anisync.android.presentation.components.SortIcon
import com.anisync.android.presentation.components.WatchingCardConfig
import com.anisync.android.presentation.library.components.EmptyLibraryTabState
import com.anisync.android.presentation.library.components.LibraryListCard
import com.anisync.android.presentation.library.components.LibrarySearchResultCard
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    kotlinx.coroutines.FlowPreview::class
)
@Composable
fun LibraryScreen(
    onMediaClick: (Int) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaType by viewModel.mediaType.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isAscending by viewModel.isAscending.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = rememberHapticFeedback()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isGridView by rememberSaveable { mutableStateOf(true) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }

    val statuses = remember {
        listOf(
            LibraryStatus.CURRENT,
            LibraryStatus.PAUSED,
            LibraryStatus.COMPLETED,
            LibraryStatus.PLANNING,
            LibraryStatus.DROPPED
        )
    }

    val pagerState = rememberPagerState(pageCount = { statuses.size })
    var editingEntry by remember { mutableStateOf<LibraryEntry?>(null) }

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    val isSearchQueryEmpty by remember {
        derivedStateOf { textFieldState.text.isEmpty() }
    }

    val handleIncrement =
        remember(viewModel) { { mediaId: Int -> viewModel.incrementProgress(mediaId) } }
    val handleDecrement =
        remember(viewModel) { { mediaId: Int -> viewModel.decrementProgress(mediaId) } }
    val handleEdit = remember { { entry: LibraryEntry -> editingEntry = entry } }

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LibraryEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .debounce(300.milliseconds)
            .collect { viewModel.onSearchQueryChange(it) }
    }

    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Collapsed) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) {
        keyboardController?.hide()
        coroutineScope.launch { searchBarState.animateToCollapsed() }
    }

    val onSearchResultClick: (Int) -> Unit = remember(onMediaClick) {
        { id ->
            keyboardController?.hide()
            onMediaClick(id)
        }
    }

    val gridScrollStates = statuses.associateWith { status ->
        rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    }
    val listScrollStates = statuses.associateWith { status ->
        rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    }

    val inputField = remember {
        @Composable {
            val currentSearchBarValue = searchBarState.currentValue
            val currentIsSearchQueryEmpty = isSearchQueryEmpty
            val currentIsGridView = isGridView
            val currentIsAscending = isAscending

            SearchBarDefaults.InputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = { keyboardController?.hide() },
                placeholder = {
                    if (currentSearchBarValue == SearchBarValue.Collapsed) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.search_library_placeholder),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(stringResource(R.string.search_library_placeholder))
                    }
                },
                leadingIcon = if (currentSearchBarValue == SearchBarValue.Expanded) {
                    {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            coroutineScope.launch { searchBarState.animateToCollapsed() }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                } else null,
                trailingIcon = {
                    if (currentSearchBarValue == SearchBarValue.Expanded && !currentIsSearchQueryEmpty) {
                        IconButton(onClick = {
                            textFieldState.edit { replace(0, length, "") }
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                    } else if (currentSearchBarValue == SearchBarValue.Collapsed) {
                        Row {
                            IconButton(onClick = {
                                haptic.click()
                                isGridView = !currentIsGridView
                            }) {
                                Icon(
                                    imageVector = if (currentIsGridView) Icons.Outlined.GridView else Icons.Outlined.ViewAgenda,
                                    contentDescription = stringResource(R.string.toggle_view)
                                )
                            }

                            IconButton(onClick = {
                                haptic.click()
                                showSortMenu = true
                            }) {
                                SortIcon(isAscending = currentIsAscending)
                            }
                        }
                    }
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier.statusBarsPadding()
            ) {
                AppBarWithSearch(
                    scrollBehavior = scrollBehavior,
                    state = searchBarState,
                    inputField = inputField,
                    colors = SearchBarDefaults.appBarWithSearchColors(
                        appBarContainerColor = Color.Transparent,
                        scrolledAppBarContainerColor = Color.Transparent
                    ),
                )

                MediaTypeSelector(
                    selected = mediaType,
                    onSelect = viewModel::onMediaTypeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )

                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    edgePadding = 16.dp,
                    indicator = {},
                    divider = {}
                ) {
                    statuses.forEachIndexed { index, status ->
                        val statusIcon = when (status) {
                            LibraryStatus.CURRENT -> if (mediaType == MediaType.ANIME) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook
                            LibraryStatus.PAUSED -> Icons.Default.Pause
                            LibraryStatus.COMPLETED -> Icons.Default.Done
                            LibraryStatus.PLANNING -> Icons.Default.CalendarMonth
                            LibraryStatus.DROPPED -> Icons.Default.Close
                            else -> Icons.Default.Inbox
                        }

                        AnimatedTab(
                            index = index,
                            selectedIndex = pagerState.currentPage,
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = statusIcon,
                            label = status.toLabel(mediaType)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 80.dp)
        ) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    if (isGridView) SkeletonGrid(itemCount = 6) else SkeletonList(itemCount = 6)
                }

                is LibraryUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.refresh() })

                is LibraryUiState.Success -> {
                    val motionScheme = MaterialTheme.motionScheme
                    val spatialSpec =
                        remember(motionScheme) { motionScheme.defaultSpatialSpec<IntOffset>() }
                    val effectsSpec =
                        remember(motionScheme) { motionScheme.defaultEffectsSpec<Float>() }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val pageStatus = statuses[pageIndex]
                        val entries = state.groupedEntries[pageStatus] ?: emptyList()

                        val gridState = gridScrollStates[pageStatus]!!
                        val listState = listScrollStates[pageStatus]!!

                        val cardConfig =
                            if (pageStatus == LibraryStatus.CURRENT) WatchingCardConfig else CompletedCardConfig

                        if (entries.isEmpty()) {
                            EmptyLibraryTabState(pageStatus, mediaType)
                        } else {
                            AnimatedContent(
                                targetState = isGridView,
                                transitionSpec = {
                                    (slideInVertically(spatialSpec) { if (targetState) -it / 8 else it / 8 } + fadeIn(
                                        effectsSpec
                                    )) togetherWith
                                            (slideOutVertically(spatialSpec) { if (targetState) it / 8 else -it / 8 } + fadeOut(
                                                effectsSpec
                                            ))
                                },
                                label = "ViewMode"
                            ) { isGrid ->
                                if (isGrid) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        state = gridState,
                                        contentPadding = PaddingValues(24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = entries,
                                            key = { it.id },
                                            contentType = { "LibraryEntry" }
                                        ) { entry ->
                                            LibraryMediaCard(
                                                entry = entry,
                                                mediaType = mediaType,
                                                onClick = { onMediaClick(entry.mediaId) },
                                                onIncrement = if (pageStatus == LibraryStatus.CURRENT) {
                                                    { handleIncrement(entry.mediaId) }
                                                } else null,
                                                onDecrement = if (pageStatus == LibraryStatus.CURRENT) {
                                                    { handleDecrement(entry.mediaId) }
                                                } else null,
                                                onEdit = { handleEdit(entry) },
                                                config = cardConfig,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                titleLanguage = titleLanguage,
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
                                        items(
                                            items = entries,
                                            key = { it.id },
                                            contentType = { "LibraryEntry" }
                                        ) { entry ->
                                            LibraryListCard(
                                                entry = entry,
                                                mediaType = mediaType,
                                                onClick = { onMediaClick(entry.mediaId) },
                                                onIncrement = if (pageStatus == LibraryStatus.CURRENT) {
                                                    { handleIncrement(entry.mediaId) }
                                                } else null,
                                                onDecrement = if (pageStatus == LibraryStatus.CURRENT) {
                                                    { handleDecrement(entry.mediaId) }
                                                } else null,
                                                onEdit = { handleEdit(entry) },
                                                config = cardConfig,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                titleLanguage = titleLanguage,
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

    if (editingEntry == null) {
        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = inputField
        ) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.loading),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is LibraryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is LibraryUiState.Success -> {
                    if (state.entries.isEmpty() && !isSearchQueryEmpty) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.search_no_results),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.entries, key = { it.id }) { entry ->
                                LibrarySearchResultCard(
                                    entry = entry,
                                    mediaType = mediaType,
                                    onClick = { onSearchResultClick(entry.mediaId) },
                                    titleLanguage = titleLanguage
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    SortBottomSheet(
        visible = showSortMenu,
        onDismiss = { showSortMenu = false },
        options = LibrarySort.entries.toList(),
        selectedOption = sortOption,
        isAscending = isAscending,
        onOptionSelected = { sort, ascending ->
            haptic.click()
            viewModel.onSortOptionChange(sort, ascending)
        }
    )

    editingEntry?.let { entry ->
        LaunchedEffect(Unit) {
            if (searchBarState.currentValue == SearchBarValue.Expanded) {
                searchBarState.animateToCollapsed()
            }
        }

        EditLibraryEntrySheet(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { updatedEntry ->
                viewModel.updateEntry(updatedEntry)
                editingEntry = null
            },
            onDelete = {
                viewModel.deleteEntry(entry.id, entry.mediaId)
                editingEntry = null
            }
        )
    }
}