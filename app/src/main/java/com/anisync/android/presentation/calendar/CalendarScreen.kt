package com.anisync.android.presentation.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.presentation.calendar.components.AiringEpisodeCard
import com.anisync.android.presentation.calendar.components.CalendarDaySkeleton
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateWithAction
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.PaneDragHandle
import com.anisync.android.presentation.util.TwoPaneRow
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.ui.theme.ExpressiveShapes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

private const val DAYS_IN_WEEK = 7
private const val GRID_WEEKS = 6

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
    // On expanded widths the calendar becomes a two-pane month grid + selected-day list instead of
    // the one-day pager; the week selector / day strip (both week-based) give way to the month nav.
    val isWide = LocalAdaptiveInfo.current.isExpandedOrWider

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
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
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

                    // Week navigation lives only in the compact pager view; the month grid carries its own.
                    if (!isWide) {
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
                            WeekDayStrip(
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
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWide) {
                // Lazily fetch the current month the first time the wide layout mounts.
                LaunchedEffect(Unit) { viewModel.onAction(CalendarAction.EnsureMonthLoaded) }
                val month = uiState.month
                when {
                    month == null ->
                        CalendarDaySkeleton(modifier = Modifier.padding(top = 12.dp))

                    month.error != null && !month.isLoading && month.days.all { it.episodes.isEmpty() } ->
                        ErrorState(
                            message = month.error,
                            onRetry = { viewModel.onAction(CalendarAction.RetryMonth) }
                        )

                    else -> CalendarMonthTwoPane(
                        month = month,
                        followingOnly = uiState.followingOnly,
                        titleLanguage = titleLanguage,
                        nowEpochSec = nowEpochSec,
                        today = today,
                        onAction = viewModel::onAction,
                        onMediaClick = onMediaClick
                    )
                }
            } else {
                val showError = uiState.error != null && !uiState.isLoading &&
                    uiState.days.all { it.episodes.isEmpty() }

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
}

@Composable
private fun DayEpisodeList(
    day: CalendarDay,
    titleLanguage: TitleLanguage,
    nowEpochSec: Long,
    onMediaClick: (Int) -> Unit,
    cardColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
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
                onClick = { onMediaClick(episode.mediaId) },
                containerColor = cardColor
            )
        }
    }
}

// --- expanded-width month grid (two-pane) ---

// Drag release snaps the month-grid pane to the nearest of these width fractions; a tap cycles
// through them. The grid + day panes are both permanent, so the split never collapses to zero.
private val CAL_FRACTION_ANCHORS = listOf(0.34f, 0.42f, 0.5f, 0.6f)
private const val CAL_MIN_FRACTION = 0.28f
private const val CAL_MAX_FRACTION = 0.62f

private fun nextCalAnchor(fraction: Float): Float =
    CAL_FRACTION_ANCHORS.firstOrNull { it > fraction + 0.01f } ?: CAL_FRACTION_ANCHORS.first()

/**
 * Expanded-width calendar: a resizable [TwoPaneRow] with a month grid on the leading pane and the
 * selected day's airings on the trailing pane (defaulting to today). Mirrors the app's other
 * two-pane surfaces — rounded cards on a tinted gutter, drag handle to resize (snap on release / tap
 * to cycle), split persisted to [com.anisync.android.data.AppSettings.paneCalendarFraction].
 */
@Composable
private fun CalendarMonthTwoPane(
    month: CalendarMonthState,
    followingOnly: Boolean,
    titleLanguage: TitleLanguage,
    nowEpochSec: Long,
    today: LocalDate,
    onAction: (CalendarAction) -> Unit,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val appSettings = LocalAppSettings.current
    val haptic = rememberHapticFeedback()
    var leadingFraction by rememberSaveable { mutableFloatStateOf(appSettings.paneCalendarFraction.value) }
    var rowWidthPx by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    fun settleTo(target: Float) {
        appSettings.setPaneCalendarFraction(target)
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(initialValue = leadingFraction, targetValue = target) { value, _ -> leadingFraction = value }
        }
    }

    val cycleLabel = stringResource(R.string.pane_resize_cycle)
    val resizeLabel = stringResource(R.string.pane_resize_handle)
    val onRefresh = rememberRateLimitedRefresh { onAction(CalendarAction.RefreshMonth) }

    TwoPaneRow(
        leadingWeight = leadingFraction,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { rowWidthPx = it.width },
        handle = {
            PaneDragHandle(
                modifier = Modifier.fillMaxHeight(),
                onDelta = { delta ->
                    if (rowWidthPx > 0) {
                        leadingFraction = (leadingFraction + delta / rowWidthPx)
                            .coerceIn(CAL_MIN_FRACTION, CAL_MAX_FRACTION)
                    }
                },
                onDragStarted = { settleJob?.cancel() },
                onDragStopped = { settleTo(CAL_FRACTION_ANCHORS.minBy { abs(it - leadingFraction) }) },
                onClick = { settleTo(nextCalAnchor(leadingFraction)) },
                clickLabel = cycleLabel,
                resizeLabel = resizeLabel
            )
        },
        leading = {
            MonthGridPane(
                month = month,
                today = today,
                onSelectDay = { date ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAction(CalendarAction.SelectDay(date))
                },
                onAction = onAction
            )
        },
        trailing = {
            DayDetailPane(
                month = month,
                followingOnly = followingOnly,
                titleLanguage = titleLanguage,
                nowEpochSec = nowEpochSec,
                today = today,
                onMediaClick = onMediaClick,
                onRefresh = onRefresh,
                // Lift the day cards a tone above the pane so they read as floating cards
                // (the pane is surfaceContainerLow; matching cards would disappear into it).
                cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    )
}

@Composable
private fun MonthGridPane(
    month: CalendarMonthState,
    today: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        MonthHeader(
            monthAnchor = month.monthAnchor,
            isCurrentMonth = month.monthAnchor == today.withDayOfMonth(1),
            onPrev = { onAction(CalendarAction.PrevMonth) },
            onNext = { onAction(CalendarAction.NextMonth) },
            onThisMonth = { onAction(CalendarAction.ThisMonth) }
        )
        Spacer(Modifier.height(4.dp))
        WeekdayLabelsRow()
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(2.dp))

        // While a month loads its grid is empty; synthesize the 6×7 dates from gridStart so the
        // calendar structure (and the today / selected highlights) are visible immediately.
        val weeks = month.days.takeIf { it.size == GRID_WEEKS * DAYS_IN_WEEK }?.chunked(DAYS_IN_WEEK)
            ?: (0 until GRID_WEEKS).map { w ->
                (0 until DAYS_IN_WEEK).map { d ->
                    CalendarDay(month.gridStart.plusDays((w * DAYS_IN_WEEK + d).toLong()), emptyList())
                }
            }
        val anchorMonth = YearMonth.from(month.monthAnchor)

        Column(modifier = Modifier.fillMaxSize()) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    week.forEach { day ->
                        MonthDayCell(
                            day = day,
                            inMonth = YearMonth.from(day.date) == anchorMonth,
                            isToday = day.date == today,
                            selected = day.date == month.selectedDate,
                            onClick = { onSelectDay(day.date) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    monthAnchor: LocalDate,
    isCurrentMonth: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onThisMonth: () -> Unit
) {
    val locale = Locale.getDefault()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.calendar_previous_month)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = monthAnchor.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isCurrentMonth) {
                AssistChip(
                    onClick = onThisMonth,
                    label = { Text(stringResource(R.string.calendar_jump_to_this_month)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Today,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.calendar_next_month)
            )
        }
    }
}

@Composable
private fun WeekdayLabelsRow(modifier: Modifier = Modifier) {
    val locale = Locale.getDefault()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        for (i in 0 until DAYS_IN_WEEK) {
            val dow = DayOfWeek.MONDAY.plus(i.toLong())
            Text(
                text = dow.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthDayCell(
    day: CalendarDay,
    inMonth: Boolean,
    isToday: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate the highlight so day selection feels continuous.
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        label = "monthDayContainer"
    )
    val numberColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val hasEpisodes = day.episodes.isNotEmpty()
    val dotColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        !inMonth -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.primary
    }

    val locale = Locale.getDefault()
    val dateLabel = remember(day.date) {
        day.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))
    }
    val countText = if (hasEpisodes) {
        pluralStringResource(R.plurals.calendar_airing_count, day.episodes.size, day.episodes.size)
    } else {
        stringResource(R.string.calendar_empty_title)
    }
    val cellDescription = "$dateLabel. $countText"

    Box(modifier = modifier.padding(2.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(containerColor)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = cellDescription }
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Normal,
                color = numberColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            // Airing marker — always laid out (transparent when none) so every cell keeps one height.
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (hasEpisodes) dotColor else Color.Transparent)
            )
        }
    }
}

@Composable
private fun DayDetailPane(
    month: CalendarMonthState,
    followingOnly: Boolean,
    titleLanguage: TitleLanguage,
    nowEpochSec: Long,
    today: LocalDate,
    onMediaClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        DayDetailHeader(
            date = month.selectedDate,
            count = month.selectedDay.episodes.size,
            isToday = month.selectedDate == today
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = month.isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = month.isRefreshing,
                    state = pullState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        ) {
            // Cross-fade the day's list as the selection changes; keying on the date also gives each
            // day a fresh LazyColumn (so the scroll position resets to the top per day).
            AnimatedContent(
                targetState = month.selectedDate,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 14 }) togetherWith
                        fadeOut(tween(120))
                },
                label = "calendarDaySwitch"
            ) { date ->
                val day = month.days.firstOrNull { it.date == date } ?: CalendarDay(date, emptyList())
                when {
                    month.isLoading -> CalendarDaySkeleton(modifier = Modifier.padding(top = 12.dp))
                    day.episodes.isEmpty() -> EmptyDay(followingOnly = followingOnly)
                    else -> DayEpisodeList(
                        day = day,
                        titleLanguage = titleLanguage,
                        nowEpochSec = nowEpochSec,
                        onMediaClick = onMediaClick,
                        cardColor = cardColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DayDetailHeader(date: LocalDate, count: Int, isToday: Boolean) {
    val locale = Locale.getDefault()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
    ) {
        Text(
            text = if (isToday) {
                stringResource(R.string.calendar_today)
            } else {
                date.format(DateTimeFormatter.ofPattern("EEEE", locale))
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = date.format(DateTimeFormatter.ofPattern("MMMM d", locale)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (count == 0) {
                stringResource(R.string.calendar_empty_title)
            } else {
                pluralStringResource(R.plurals.calendar_airing_count, count, count)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                AssistChip(
                    onClick = onThisWeek,
                    label = { Text(stringResource(R.string.calendar_jump_to_today)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Today,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.padding(top = 4.dp)
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
private fun WeekDayStrip(
    days: List<CalendarDay>,
    selectedIndex: Int,
    today: LocalDate,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEachIndexed { index, day ->
            DayCell(
                day = day,
                selected = index == selectedIndex,
                isToday = day.date == today,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate the pill background so selection feels continuous as the pager swipes.
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "dayCellContainer"
    )

    val weekdayColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val numberColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val isEmpty = day.episodes.isEmpty()
    val countColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        isEmpty -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val locale = Locale.getDefault()
    // Merge the three texts into one TalkBack node; selection state comes from selectable().
    val cellDescription = remember(day.date) {
        val weekdayFull = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val dateLabel = day.date.format(DateTimeFormatter.ofPattern("MMMM d", locale))
        "$weekdayFull, $dateLabel"
    }

    Column(
        modifier = modifier
            .clip(ExpressiveShapes.pill)
            .background(containerColor)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) { contentDescription = cellDescription }
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
            style = MaterialTheme.typography.labelMedium,
            color = weekdayColor
        )
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = numberColor
        )
        Text(
            text = if (isEmpty) "–" else day.episodes.size.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = countColor
        )
        // Today marker — drawn for every cell (transparent unless today) so all pills keep
        // the same height; visible only when today isn't the currently selected day.
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    if (isToday && !selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                )
        )
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
