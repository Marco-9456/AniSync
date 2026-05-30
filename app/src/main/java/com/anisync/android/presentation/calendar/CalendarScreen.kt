package com.anisync.android.presentation.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.calendar.components.AiringEpisodeCard
import com.anisync.android.presentation.calendar.components.CalendarDaySkeleton
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateWithAction
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.util.rememberHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private const val DAYS_IN_WEEK = 7

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()

    val haptic = rememberHapticFeedback()
    val coroutineScope = rememberCoroutineScope()
    val zoneId = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zoneId)

    // One minute ticker driving every card's live countdown.
    var nowEpochSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowEpochSec = System.currentTimeMillis() / 1000
        }
    }

    val pagerState = rememberPagerState(
        initialPage = todayIndex(uiState.weekStart, zoneId)
    ) { DAYS_IN_WEEK }

    // When the displayed week changes, jump to today (current week) or Monday (other weeks).
    LaunchedEffect(uiState.weekStart) {
        pagerState.scrollToPage(todayIndex(uiState.weekStart, zoneId))
    }

    val isCurrentWeek = uiState.weekStart == weekStartOf(today)

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.calendar_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }
                    },
                    actions = {
                        FilledIconToggleButton(
                            checked = uiState.followingOnly,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onAction(CalendarAction.ToggleFollowingOnly)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = stringResource(R.string.calendar_following_only)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                WeekSelector(
                    weekStart = uiState.weekStart,
                    isCurrentWeek = isCurrentWeek,
                    onPrev = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onAction(CalendarAction.PrevWeek)
                    },
                    onNext = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onAction(CalendarAction.NextWeek)
                    },
                    onThisWeek = { viewModel.onAction(CalendarAction.ThisWeek) }
                )

                if (uiState.days.size == DAYS_IN_WEEK) {
                    DayTabRow(
                        days = uiState.days,
                        selectedIndex = pagerState.currentPage,
                        today = today,
                        onSelect = { index ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        val showError = uiState.error != null && !uiState.isLoading &&
            uiState.days.all { it.episodes.isEmpty() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showError) {
                ErrorState(
                    message = uiState.error.orEmpty(),
                    onRetry = { viewModel.onAction(CalendarAction.Retry) }
                )
            } else {
                val pullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = rememberRateLimitedRefresh { viewModel.onAction(CalendarAction.Refresh) },
                    state = pullState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        CustomPullToRefreshIndicator(
                            isRefreshing = uiState.isRefreshing,
                            state = pullState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        )
                    }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val day = uiState.days.getOrNull(page)
                        when {
                            uiState.isLoading || day == null -> {
                                CalendarDaySkeleton(modifier = Modifier.padding(top = 12.dp))
                            }

                            day.episodes.isEmpty() -> {
                                EmptyDay(followingOnly = uiState.followingOnly)
                            }

                            else -> {
                                DayEpisodeList(
                                    day = day,
                                    titleLanguage = titleLanguage,
                                    nowEpochSec = nowEpochSec,
                                    onMediaClick = onMediaClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayEpisodeList(
    day: CalendarDay,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    nowEpochSec: Long,
    onMediaClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(day.episodes, key = { it.id }) { episode ->
            AiringEpisodeCard(
                episode = episode,
                titleLanguage = titleLanguage,
                nowEpochSec = nowEpochSec,
                onClick = { onMediaClick(episode.mediaId) }
            )
        }
    }
}

@Composable
private fun EmptyDay(followingOnly: Boolean) {
    // Wrap in a scrollable so pull-to-refresh still fires on an empty day.
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateWithAction(
                    icon = Icons.Filled.CalendarMonth,
                    title = stringResource(R.string.calendar_empty_title),
                    description = stringResource(
                        if (followingOnly) {
                            R.string.calendar_empty_following_desc
                        } else {
                            R.string.calendar_empty_desc
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun WeekSelector(
    weekStart: LocalDate,
    isCurrentWeek: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onThisWeek: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.calendar_previous_week)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weekRangeLabel(weekStart),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isCurrentWeek) {
                Text(
                    text = stringResource(R.string.calendar_jump_to_today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable(onClick = onThisWeek)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.calendar_next_week)
            )
        }
    }
}

@Composable
private fun DayTabRow(
    days: List<CalendarDay>,
    selectedIndex: Int,
    today: LocalDate,
    onSelect: (Int) -> Unit
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex.coerceIn(0, days.lastIndex.coerceAtLeast(0)),
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        days.forEachIndexed { index, day ->
            val isToday = day.date == today
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.date.dayOfWeek
                            .getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = if (day.episodes.isEmpty()) "–" else day.episodes.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.episodes.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

// --- pure date helpers ---

private fun weekStartOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun todayIndex(weekStart: LocalDate, zoneId: ZoneId): Int {
    val offset = ChronoUnit.DAYS.between(weekStart, LocalDate.now(zoneId)).toInt()
    return offset.coerceIn(0, DAYS_IN_WEEK - 1)
}

private fun weekRangeLabel(weekStart: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val end = weekStart.plusDays((DAYS_IN_WEEK - 1).toLong())
    return "${weekStart.format(formatter)} – ${end.format(formatter)}"
}
