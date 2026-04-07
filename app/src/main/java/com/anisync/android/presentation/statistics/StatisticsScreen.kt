package com.anisync.android.presentation.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.StudioStat
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.util.formatDecimal
import kotlinx.coroutines.launch

enum class StatisticsType {
    ANIME, MANGA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by remember(uiState) {
        derivedStateOf {
            (uiState as? StatisticsUiState.Success)?.isRefreshing == true
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    val animeListState = rememberLazyListState()
    val mangaListState = rememberLazyListState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = StatisticsType.entries

    val currentListState = if (selectedTabIndex == 0) animeListState else mangaListState
    val canRefresh by remember(selectedTabIndex) {
        derivedStateOf {
            scrollBehavior.state.collapsedFraction == 0f &&
                    currentListState.firstVisibleItemIndex == 0 &&
                    currentListState.firstVisibleItemScrollOffset == 0
        }
    }

    val spacingMedium = dimensionResource(R.dimen.spacing_medium)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.statistics_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )

                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    edgePadding = spacingMedium,
                    indicator = {},
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, type ->
                        val icon = when (type) {
                            StatisticsType.ANIME -> Icons.Default.Tv
                            StatisticsType.MANGA -> Icons.AutoMirrored.Filled.MenuBook
                        }
                        val label = when (type) {
                            StatisticsType.ANIME -> stringResource(R.string.statistics_anime)
                            StatisticsType.MANGA -> stringResource(R.string.statistics_manga)
                        }

                        AnimatedTab(
                            index = index,
                            selectedIndex = selectedTabIndex,
                            selected = selectedTabIndex == index,
                            onClick = { coroutineScope.launch { selectedTabIndex = index } },
                            icon = icon,
                            label = label
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { if (canRefresh) viewModel.onAction(StatisticsAction.Refresh) },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            indicator = {
                if (canRefresh || isRefreshing) {
                    CustomPullToRefreshIndicator(
                        isRefreshing = isRefreshing,
                        state = pullToRefreshState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                }
            }
        ) {
            when (val state = uiState) {
                is StatisticsUiState.Loading -> {
                    StatisticsSkeleton(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                    )
                }

                is StatisticsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.onAction(StatisticsAction.Retry) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is StatisticsUiState.Success -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (tabs[selectedTabIndex] == StatisticsType.ANIME) {
                            AnimeStatisticsContent(
                                stats = state.data.animeStats,
                                listState = animeListState
                            )
                        } else {
                            MangaStatisticsContent(
                                stats = state.data.mangaStats,
                                listState = mangaListState
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeStatisticsContent(
    stats: AnimeStatisticsUi,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Dashboard
        item(key = "hero_dashboard") {
            HeroDashboard(
                count = stats.totalCount.toString(),
                countLabel = stringResource(R.string.statistics_total_anime),
                subStat1Value = formatDecimal(stats.daysWatched),
                subStat1Label = stringResource(R.string.statistics_days_watched),
                subStat1Icon = Icons.Default.Schedule,
                subStat2Value = formatDecimal(stats.meanScore),
                subStat2Label = stringResource(R.string.statistics_mean_score),
                subStat2Icon = Icons.Rounded.Star,
                episodes = stats.episodesWatched
            )
        }

        // Score Histogram
        if (stats.scoreDistribution.isNotEmpty()) {
            item(key = "score_histogram") {
                ScoreHistogramSection(stats.scoreDistribution)
            }
        }

        // Top Genres
        if (stats.genreDistribution.isNotEmpty()) {
            item(key = "top_genres") {
                HorizontalStatsSection(
                    title = stringResource(R.string.statistics_top_genres),
                    items = stats.genreDistribution,
                    key = { it.genre }
                ) {
                    GenreCardModern(it)
                }
            }
        }

        // Formats Breakdown
        if (stats.formatDistribution.isNotEmpty()) {
            item(key = "formats") {
                FormatsSection(stats.formatDistribution)
            }
        }

        // Release Years Histogram
        if (stats.releaseYearDistribution.isNotEmpty()) {
            item(key = "years_histogram") {
                ReleaseYearsHistogramSection(stats.releaseYearDistribution)
            }
        }

        // Studios
        if (stats.studioDistribution.isNotEmpty()) {
            item(key = "top_studios") {
                HorizontalStatsSection(
                    title = stringResource(R.string.statistics_top_studios),
                    items = stats.studioDistribution,
                    key = { it.studioName }
                ) {
                    StudioCardModern(it)
                }
            }
        }
    }
}

@Composable
private fun MangaStatisticsContent(
    stats: MangaStatisticsUi?,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    if (stats == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.statistics_no_manga_stats),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "manga_dashboard") {
            HeroDashboard(
                count = stats.totalCount.toString(),
                countLabel = stringResource(R.string.statistics_total_manga),
                subStat1Value = stats.chaptersRead.toString(),
                subStat1Label = stringResource(R.string.statistics_chapters),
                subStat1Icon = Icons.AutoMirrored.Filled.MenuBook,
                subStat2Value = formatDecimal(stats.meanScore),
                subStat2Label = stringResource(R.string.statistics_mean_score),
                subStat2Icon = Icons.Rounded.Star,
                episodes = null
            )
        }

        item(key = "manga_placeholder") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "More manga statistics coming soon",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// MARK: - Components extracted to StatisticsComponents.kt
