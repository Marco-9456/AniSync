package com.anisync.android.presentation.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.anisync.android.domain.UserStatistics
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import kotlinx.coroutines.launch

enum class StatisticsType {
    ANIME, MANGA
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

    // Tab state
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = StatisticsType.entries

    // Spacing resources
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingLarge = dimensionResource(R.dimen.spacing_large)

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

                // AnimatedTab style tabs (matching Library screen)
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
                        .padding(top = spacingMedium)
                )
            }
        ) {
            when (val state = uiState) {
                is StatisticsUiState.Loading -> {
                    StatisticsSkeleton(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = spacingMedium)
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
                    StatisticsContent(
                        statistics = state.statistics,
                        type = tabs[selectedTabIndex]
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    statistics: UserStatistics,
    type: StatisticsType,
    modifier: Modifier = Modifier
) {
    val spacingLarge = dimensionResource(R.dimen.spacing_large)
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacingLarge, top = spacingMedium),
        verticalArrangement = Arrangement.spacedBy(spacingLarge)
    ) {
        if (type == StatisticsType.ANIME) {
            val animeStats = statistics.animeStats
            // Overview Cards
            item(key = "anime_overview", contentType = "overview") {
                OverviewSection(animeStats)
            }

            // Score Distribution
            if (animeStats.scoreDistribution.isNotEmpty()) {
                item(key = "anime_scores", contentType = "scores") {
                    ScoreDistributionSection(animeStats.scoreDistribution)
                }
            }

            // Top Genres
            if (animeStats.genreDistribution.isNotEmpty()) {
                item(key = "anime_genres", contentType = "genres") {
                    GenresSection(animeStats.genreDistribution)
                }
            }

            // Formats
            if (animeStats.formatDistribution.isNotEmpty()) {
                item(key = "anime_formats", contentType = "formats") {
                    FormatsSection(animeStats.formatDistribution)
                }
            }

            // Top Studios
            if (animeStats.studioDistribution.isNotEmpty()) {
                item(key = "anime_studios", contentType = "studios") {
                    StudiosSection(animeStats.studioDistribution)
                }
            }

            // Release Years
            if (animeStats.releaseYearDistribution.isNotEmpty()) {
                item(key = "anime_years", contentType = "years") {
                    ReleaseYearsSection(animeStats.releaseYearDistribution)
                }
            }
        } else {
            // Manga
            val mangaStats = statistics.mangaStats
            if (mangaStats != null) {
                item(key = "manga_overview", contentType = "overview") {
                    MangaOverviewSection(mangaStats)
                }

                // Add empty state for additional manga sections
                item(key = "manga_more_coming", contentType = "info") {
                    MangaComingSoonInfo()
                }
            } else {
                item(key = "manga_empty", contentType = "empty") {
                    MangaEmptyState()
                }
            }
        }
    }
}

@Composable
private fun MangaEmptyState() {
    val spacingLarge = dimensionResource(R.dimen.spacing_large)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacingLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(R.string.statistics_no_manga_stats),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MangaComingSoonInfo() {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacingMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "More manga statistics coming soon!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OverviewSection(stats: AnimeStatistics) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)

    Column(
        modifier = Modifier.padding(horizontal = spacingMedium),
        verticalArrangement = Arrangement.spacedBy(spacingNormal)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacingNormal)
        ) {
            StatCard(
                icon = Icons.Default.Movie,
                label = stringResource(R.string.statistics_total_anime),
                value = stats.totalCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.PlayArrow,
                label = stringResource(R.string.statistics_episodes),
                value = stats.episodesWatched.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacingNormal)
        ) {
            StatCard(
                icon = Icons.Default.Schedule,
                label = stringResource(R.string.statistics_days_watched),
                value = "%.1f".format(stats.daysWatched),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Star,
                label = stringResource(R.string.statistics_mean_score),
                value = "%.1f".format(stats.meanScore),
                modifier = Modifier.weight(1f),
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MangaOverviewSection(stats: MangaStatistics) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)

    Column(
        modifier = Modifier.padding(horizontal = spacingMedium),
        verticalArrangement = Arrangement.spacedBy(spacingNormal)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacingNormal)
        ) {
            StatCard(
                icon = Icons.Default.Book,
                label = stringResource(R.string.statistics_total_manga),
                value = stats.totalCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = stringResource(R.string.statistics_chapters),
                value = stats.chaptersRead.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacingNormal)
        ) {
            StatCard(
                icon = Icons.Default.Star,
                label = stringResource(R.string.statistics_mean_score),
                value = "%.1f".format(stats.meanScore),
                modifier = Modifier.weight(1f),
                valueColor = MaterialTheme.colorScheme.primary
            )
            // Empty spacer for layout balance
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)
    val a11yDescription = stringResource(R.string.a11y_stat_card, label, value)

    Card(
        modifier = modifier.semantics {
            contentDescription = a11yDescription
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(spacingNormal))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScoreDistributionSection(scores: List<ScoreStat>) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_score_distribution),
            level = HeaderLevel.Subsection,
            modifier = Modifier.padding(horizontal = spacingMedium)
        )

        Spacer(modifier = Modifier.height(spacingNormal))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacingMedium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(spacingMedium)
            ) {
                val maxCount = scores.maxOfOrNull { it.count } ?: 1
                val sortedScores = scores.sortedByDescending { it.score }

                sortedScores.forEach { scoreStat ->
                    ScoreBar(
                        score = scoreStat.score,
                        count = scoreStat.count,
                        maxCount = maxCount
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScoreBar(
    score: Int,
    count: Int,
    maxCount: Int
) {
    val spacingSmall = dimensionResource(R.dimen.spacing_small)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)

    // Use MotionScheme for animations
    val motionScheme = MaterialTheme.motionScheme
    val animationSpec = remember(motionScheme) { motionScheme.fastSpatialSpec<Float>() }

    val animatedProgress = remember { Animatable(0f) }
    val targetProgress = count.toFloat() / maxCount.coerceAtLeast(1)

    LaunchedEffect(score, count) {
        animatedProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = animationSpec
        )
    }

    val a11yDescription = stringResource(R.string.a11y_score_bar, score, count)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics { contentDescription = a11yDescription },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(spacingSmall))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.value)
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            score >= 8 -> MaterialTheme.colorScheme.primary
                            score >= 6 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.width(spacingNormal))

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun GenresSection(genres: List<GenreStat>) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)
    val spacingLarge = dimensionResource(R.dimen.spacing_large)

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_top_genres),
            level = HeaderLevel.Subsection,
            modifier = Modifier.padding(horizontal = spacingMedium)
        )

        Spacer(modifier = Modifier.height(spacingNormal))

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(spacingNormal)
        ) {
            items(genres, key = { it.genre }) { genre ->
                GenreCard(genre)
            }
        }
    }
}

@Composable
private fun GenreCard(genre: GenreStat) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)
    val spacingSmall = dimensionResource(R.dimen.spacing_small)
    val a11yDescription = stringResource(R.string.a11y_genre_stat, genre.genre, genre.count, genre.meanScore)

    Card(
        modifier = Modifier
            .width(150.dp)
            .semantics { contentDescription = a11yDescription },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = genre.genre.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(spacingNormal))

            Text(
                text = genre.genre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(spacingSmall))

            Text(
                text = stringResource(R.string.statistics_entries, genre.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacingSmall))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = spacingSmall, vertical = spacingSmall / 2)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(genre.meanScore),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FormatsSection(formats: List<FormatStat>) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_formats),
            level = HeaderLevel.Subsection,
            modifier = Modifier.padding(horizontal = spacingMedium)
        )

        Spacer(modifier = Modifier.height(spacingNormal))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacingMedium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(spacingMedium),
                verticalArrangement = Arrangement.spacedBy(spacingNormal)
            ) {
                formats.forEach { format ->
                    FormatRow(format)
                }
            }
        }
    }
}

@Composable
private fun FormatRow(format: FormatStat) {
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)
    val spacingSmall = dimensionResource(R.dimen.spacing_small)
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val a11yDescription = stringResource(R.string.a11y_format_stat, formatDisplayName(format.format), format.count, format.meanScore)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = a11yDescription },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
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
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(spacingNormal))

            Text(
                text = formatDisplayName(format.format),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${format.count}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(format.meanScore),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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

@Composable
private fun StudiosSection(studios: List<StudioStat>) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_top_studios),
            level = HeaderLevel.Subsection,
            modifier = Modifier.padding(horizontal = spacingMedium)
        )

        Spacer(modifier = Modifier.height(spacingNormal))

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(spacingNormal)
        ) {
            items(studios, key = { it.studioName }) { studio ->
                StudioCard(studio)
            }
        }
    }
}

@Composable
private fun StudioCard(studio: StudioStat) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)
    val spacingSmall = dimensionResource(R.dimen.spacing_small)
    val animeLabel = stringResource(R.string.statistics_anime_label)
    val a11yDescription = stringResource(R.string.a11y_studio_stat, studio.studioName, studio.count, studio.meanScore)

    Card(
        modifier = Modifier
            .width(160.dp)
            .semantics { contentDescription = a11yDescription },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = studio.studioName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacingNormal))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${studio.count}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = animeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacingSmall)
                )
            }

            Spacer(modifier = Modifier.height(spacingSmall))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = spacingSmall, vertical = spacingSmall / 2)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(studio.meanScore),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ReleaseYearsSection(years: List<ReleaseYearStat>) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)
    val spacingSmall = dimensionResource(R.dimen.spacing_small)

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_by_year),
            level = HeaderLevel.Subsection,
            modifier = Modifier.padding(horizontal = spacingMedium)
        )

        Spacer(modifier = Modifier.height(spacingNormal))

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(spacingSmall)
        ) {
            items(years.take(15), key = { it.year }) { year ->
                YearChip(year)
            }
        }
    }
}

@Composable
private fun YearChip(year: ReleaseYearStat) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)
    val spacingSmall = dimensionResource(R.dimen.spacing_small)
    val a11yDescription = stringResource(R.string.a11y_year_stat, year.year, year.count)

    Card(
        modifier = Modifier.semantics { contentDescription = a11yDescription },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacingMedium, vertical = spacingNormal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = year.year.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(spacingSmall))
            Text(
                text = "${year.count}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Converts format enum name to user-friendly display name.
 */
private fun formatDisplayName(format: String): String {
    return when (format) {
        "TV" -> "TV Series"
        "TV_SHORT" -> "TV Short"
        "MOVIE" -> "Movie"
        "SPECIAL" -> "Special"
        "OVA" -> "OVA"
        "ONA" -> "ONA"
        "MUSIC" -> "Music"
        else -> format.replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}
