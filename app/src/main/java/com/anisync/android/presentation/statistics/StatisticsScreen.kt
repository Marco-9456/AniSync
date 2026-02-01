package com.anisync.android.presentation.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.anisync.android.domain.AnimeStatistics
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.MangaStatistics
import com.anisync.android.domain.ReleaseYearStat
import com.anisync.android.domain.ScoreStat
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

    // Restored LargeTopAppBar scroll behavior
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = StatisticsType.entries

    // Spacing resources
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
                    windowInsets = WindowInsets(0),
                    scrollBehavior = scrollBehavior
                )

                // Restored AnimatedTab style tabs
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    edgePadding = spacingMedium,
                    indicator = {}, // No indicator - pill background shows selection
                    divider = {} // Remove default divider
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
                            onClick = {
                                coroutineScope.launch {
                                    selectedTabIndex = index
                                }
                            },
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
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = isRefreshing,
                    state = pullToRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
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
                    // Content Wrapper to handle crossfade or layout changes
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (tabs[selectedTabIndex] == StatisticsType.ANIME) {
                            AnimeStatisticsContent(state.statistics.animeStats)
                        } else {
                            MangaStatisticsContent(state.statistics.mangaStats)
                        }
                    }
                }
            }
        }
    }
}

// Simple clickable wrapper
@Composable
private fun Modifier.clickableWithRipple(onClick: () -> Unit) = this.clickable(onClick = onClick)

@Composable
private fun AnimeStatisticsContent(stats: AnimeStatistics) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Hero Dashboard Section
        item {
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

        // 2. Score Histogram
        if (stats.scoreDistribution.isNotEmpty()) {
            item {
                ScoreHistogramSection(stats.scoreDistribution)
            }
        }

        // 3. Top Genres
        if (stats.genreDistribution.isNotEmpty()) {
            item {
                HorizontalStatsSection(
                    title = stringResource(R.string.statistics_top_genres),
                    items = stats.genreDistribution
                ) {
                    GenreCardModern(it)
                }
            }
        }

        // 4. Formats Breakdown
        if (stats.formatDistribution.isNotEmpty()) {
            item {
                FormatsGridSection(stats.formatDistribution)
            }
        }

        // 5. Release Years Histogram
        if (stats.releaseYearDistribution.isNotEmpty()) {
            item {
                ReleaseYearsHistogramSection(stats.releaseYearDistribution)
            }
        }

        // 6. Studios
        if (stats.studioDistribution.isNotEmpty()) {
            item {
                HorizontalStatsSection(
                    title = stringResource(R.string.statistics_top_studios),
                    items = stats.studioDistribution
                ) {
                    StudioCardModern(it)
                }
            }
        }
    }
}

@Composable
private fun MangaStatisticsContent(stats: MangaStatistics?) {
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
        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            HeroDashboard(
                count = stats.totalCount.toString(),
                countLabel = stringResource(R.string.statistics_total_manga),
                subStat1Value = stats.chaptersRead.toString(),
                subStat1Label = stringResource(R.string.statistics_chapters),
                subStat1Icon = Icons.AutoMirrored.Filled.MenuBook,
                subStat2Value = formatDecimal(stats.meanScore),
                subStat2Label = stringResource(R.string.statistics_mean_score),
                subStat2Icon = Icons.Rounded.Star,
                episodes = null // No episodes for manga
            )
        }

        item {
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
        // Main Dashboard Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
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
                    // Sub Stat 1 (Days / Chapters)
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

                    // Sub Stat 2 (Score)
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
private fun ScoreHistogramSection(scores: List<ScoreStat>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(
            title = stringResource(R.string.statistics_score_distribution),
            level = HeaderLevel.Subsection
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val maxCount = remember(scores) { scores.maxOfOrNull { it.count } ?: 1 }

            // Generate full 1..10 range to fill gaps
            val fullRange = remember(scores) {
                (1..10).map { score ->
                    scores.find { it.score == score }?.count ?: 0
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                fullRange.forEachIndexed { index, count ->
                    val score = index + 1
                    val heightFraction = count.toFloat() / maxCount.coerceAtLeast(1)
                    val animatedHeight = remember { Animatable(0f) }

                    LaunchedEffect(heightFraction) {
                        animatedHeight.animateTo(
                            heightFraction,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // The Bar
                        Box(
                            modifier = Modifier
                                .width(8.dp) // Slim bars
                                .weight(1f, fill = false) // Allow flexible height logic
                                .fillMaxHeight(if (count > 0) animatedHeight.value else 0.02f) // Min height for empty
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    when {
                                        count == 0 -> MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.2f
                                        )

                                        score >= 8 -> MaterialTheme.colorScheme.primary
                                        score >= 5 -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseYearsHistogramSection(years: List<ReleaseYearStat>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(
            title = stringResource(R.string.statistics_by_year),
            level = HeaderLevel.Subsection
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val sortedYears = remember(years) {
                years.sortedBy { it.year }.takeLast(10)
            } // Show last 10 relevant years
            val maxCount = remember(sortedYears) { sortedYears.maxOfOrNull { it.count } ?: 1 }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                sortedYears.forEach { stat ->
                    val heightFraction = stat.count.toFloat() / maxCount.coerceAtLeast(1)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Count Bubble
                        if (stat.count > 0) {
                            Text(
                                text = stat.count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(100.dp) // Max bar height
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .align(Alignment.BottomCenter)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Year Label (vertical if tight)
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
private fun FormatsGridSection(formats: List<FormatStat>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(
            title = stringResource(R.string.statistics_formats),
            level = HeaderLevel.Subsection
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                formats.chunked(2).forEachIndexed { index, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        pair.forEach { format ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            CircleShape
                                        ),
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
                                Column {
                                    Text(
                                        text = formatDisplayName(format.format),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = format.count.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        // Fill empty space if odd number
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    if (index < formats.chunked(2).lastIndex) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> HorizontalStatsSection(
    title: String,
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    Column {
        SectionHeader(
            title = title,
            level = HeaderLevel.Subsection,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                itemContent(item)
            }
        }
    }
}

@Composable
private fun GenreCardModern(genre: GenreStat) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )

    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // Manual background
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
            modifier = Modifier.padding(16.dp),
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
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${studio.count} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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