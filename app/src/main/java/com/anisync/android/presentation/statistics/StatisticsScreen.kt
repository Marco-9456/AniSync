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
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

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
            onRefresh = { if (canRefresh) viewModel.refresh() },
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
                        onRetry = { viewModel.retry() },
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

// MARK: - Components

@Composable
private fun HeroDashboard(
    count: String,
    countLabel: String,
    subStat1Value: String,
    subStat1Label: String,
    subStat1Icon: ImageVector,
    subStat2Value: String,
    subStat2Label: String,
    subStat2Icon: ImageVector,
    episodes: Int?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = count,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    if (episodes != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$episodes eps",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                subStat1Icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = subStat1Value,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = subStat1Label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                subStat2Icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = subStat2Value,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = subStat2Label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreHistogramSection(scores: List<ScoreUiModel>) {
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_score_distribution),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                scores.forEach { item ->
                    val animatedHeight = remember { Animatable(0f) }

                    LaunchedEffect(item.heightFraction) {
                        animatedHeight.animateTo(
                            item.heightFraction,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .weight(1f, fill = false)
                                .fillMaxHeight(if (item.count > 0) animatedHeight.value else 0.02f)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    when {
                                        item.count == 0 -> MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.2f
                                        )

                                        item.score >= 8 -> MaterialTheme.colorScheme.primary
                                        item.score >= 5 -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.score.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            fontWeight = if (item.count > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseYearsHistogramSection(years: List<YearUiModel>) {
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_by_year),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                years.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (stat.count > 0) {
                            Text(
                                text = stat.count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(100.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(stat.heightFraction.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "'${stat.year.toString().takeLast(2)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatsSection(formats: List<FormatStat>) {
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_formats),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                formats.forEachIndexed { index, format ->
                    FormatRow(format)

                    if (index < formats.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatRow(format: FormatStat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFormatIcon(format.format),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatDisplayName(format.format),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${format.count}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            RatingBadge(meanScore = format.meanScore, size = RatingBadgeSize.Small)
        }
    }
}

@Composable
private fun <T> HorizontalStatsSection(
    title: String,
    items: List<T>,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    Column {
        SectionHeader(
            title = title,
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = key) { item ->
                itemContent(item)
            }
        }
    }
}

@Composable
private fun GenreCardModern(genre: GenreStat) {
    // Memoize the gradient to prevent re-allocation on every recomposition
    val secondary = MaterialTheme.colorScheme.secondaryContainer
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val gradientBrush = remember(secondary, surfaceHigh) {
        Brush.linearGradient(colors = listOf(secondary, surfaceHigh))
    }

    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = genre.genre.first().toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (genre.meanScore > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatDecimal(genre.meanScore),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = genre.genre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${genre.count} entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StudioCardModern(studio: StudioStat) {
    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = studio.studioName.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = studio.studioName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${studio.count} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private enum class RatingBadgeSize {
    Default,
    Small
}

@Composable
private fun RatingBadge(
    meanScore: Float,
    size: RatingBadgeSize = RatingBadgeSize.Default
) {
    val hasRating = meanScore > 0.0f

    val (iconSize, textStyle, verticalPadding) = when (size) {
        RatingBadgeSize.Default -> Triple(12.dp, MaterialTheme.typography.labelMedium, 4.dp)
        RatingBadgeSize.Small -> Triple(10.dp, MaterialTheme.typography.labelSmall, 2.dp)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50)) // Pill shape
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = verticalPadding)
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = if (hasRating) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (hasRating) formatDecimal(meanScore) else stringResource(R.string.not_available),
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = if (hasRating) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            }
        )
    }
}

// Helpers
private fun formatDisplayName(format: String): String {
    return when (format) {
        "TV" -> "TV Series"
        "TV_SHORT" -> "Short"
        "MOVIE" -> "Movie"
        "SPECIAL" -> "Special"
        "OVA" -> "OVA"
        "ONA" -> "ONA"
        "MUSIC" -> "Music"
        "MANGA" -> "Manga"
        "NOVEL" -> "Novel"
        "ONE_SHOT" -> "One Shot"
        else -> format.replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}

private fun getFormatIcon(format: String): ImageVector {
    return when (format) {
        "TV", "TV_SHORT" -> Icons.Default.Tv
        "MOVIE" -> Icons.Default.Movie
        "SPECIAL", "OVA", "ONA" -> Icons.Default.Videocam
        "MUSIC" -> Icons.Default.MusicNote
        "MANGA", "NOVEL", "ONE_SHOT" -> Icons.Default.Book
        else -> Icons.Default.PlayArrow
    }
}