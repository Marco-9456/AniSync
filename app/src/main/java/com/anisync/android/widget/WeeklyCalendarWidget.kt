package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.datastore.preferences.core.Preferences
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.anisync.android.MainActivity
import com.anisync.android.R
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.widget.actions.ToggleCalendarFilterAction
import com.anisync.android.widget.actions.ToggleExpandTodayAction
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WeeklyCalendarWidgetEntryPoint {
    fun airingScheduleDao(): AiringScheduleDao
}

/**
 * Weekly Calendar Widget - Shows 7-day anime schedule overview
 *
 * Visualizes upcoming episodes across the week with day-by-day breakdown.
 * Complements "Airing Today" by providing weekly context.
 */
class WeeklyCalendarWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 100.dp),  // Compact: Today's count + mini week strip
            DpSize(250.dp, 100.dp),  // Medium: Full week strip with indicators
            DpSize(250.dp, 300.dp)   // Expanded: Full week with episode lists per day
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WeeklyCalendarWidgetEntryPoint::class.java
        )
        val dao = entryPoint.airingScheduleDao()

        // Calculate week range (next 7 days)
        val calendar = Calendar.getInstance()
        val startOfToday = getStartOfDayMillis(calendar) / 1000
        val endOfWeek = startOfToday + (7 * 24 * 60 * 60)

        val weeklySchedules = withContext(Dispatchers.IO) {
            try {
                dao.getAiringBetween(startOfToday, endOfWeek)
                    .sortedBy { it.airingAt }
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Group by day of week
        val groupedByDay = weeklySchedules.groupBy { schedule ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = schedule.airingAt * 1000
            }
            cal.get(Calendar.DAY_OF_WEEK)
        }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                val todayDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

                // READ FILTER STATE
                val prefs = currentState<Preferences>()
                val filterMyList = prefs[ToggleCalendarFilterAction.CalendarFilterKey] ?: false
                val expandToday = prefs[ToggleExpandTodayAction.ExpandTodayKey] ?: false

                // FILTER DATA based on mode
                val filteredGroupedByDay = if (filterMyList) {
                    groupedByDay.mapValues { (_, episodes) ->
                        episodes.filter { it.isWatching }
                    }.filterValues { it.isNotEmpty() } // Remove empty days
                } else {
                    groupedByDay
                }

                when {
                    size.height <= 110.dp -> CalendarCompact(filteredGroupedByDay, todayDayOfWeek, filterMyList)
                    size.height <= 120.dp -> CalendarMedium(filteredGroupedByDay, todayDayOfWeek, filterMyList)
                    else -> CalendarExpanded(filteredGroupedByDay, todayDayOfWeek, filterMyList, expandToday)
                }
            }
        }
    }

    private fun getStartOfDayMillis(calendar: Calendar): Long {
        return calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

// -------------------------------------------------------------------------
// LAYOUTS
// -------------------------------------------------------------------------

/**
 * COMPACT: Shows today's count prominently + 3-day mini strip
 */
@Composable
private fun CalendarCompact(
    groupedByDay: Map<Int, List<AiringScheduleEntity>>,
    todayDayOfWeek: Int,
    isMyList: Boolean = false
) {
    val todayList = groupedByDay[todayDayOfWeek] ?: emptyList()
    val count = todayList.size

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Today highlight block
        Box(
            modifier = GlanceModifier
                .width(64.dp)
                .fillMaxSize()
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = getDayAbbreviation(todayDayOfWeek),
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "$count",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (count == 1) "ep" else "eps",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Mini 3-day strip
        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = "This Week",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))

            // Day strip
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                val daysToShow = listOf(
                    getRelativeDay(todayDayOfWeek, -1),
                    todayDayOfWeek,
                    getRelativeDay(todayDayOfWeek, 1)
                )

                daysToShow.forEach { dayOfWeek ->
                    val isToday = dayOfWeek == todayDayOfWeek
                    val dayCount = groupedByDay[dayOfWeek]?.size ?: 0

                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(48.dp)
                            .cornerRadius(8.dp)
                            .background(
                                if (isToday) GlanceTheme.colors.primary
                                else GlanceTheme.colors.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = getDayAbbreviation(dayOfWeek),
                                style = TextStyle(
                                    color = if (isToday) GlanceTheme.colors.onPrimary
                                    else GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            if (dayCount > 0) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .size(16.dp)
                                        .cornerRadius(8.dp)
                                        .background(
                                            if (isToday) GlanceTheme.colors.onPrimary
                                            else GlanceTheme.colors.primary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayCount",
                                        style = TextStyle(
                                            color = if (isToday) GlanceTheme.colors.primary
                                            else GlanceTheme.colors.onPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (dayOfWeek != daysToShow.last()) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                    }
                }
            }
        }
    }
}

/**
 * MEDIUM: Full Mon-Sun week strip with indicators
 */
@Composable
private fun CalendarMedium(
    groupedByDay: Map<Int, List<AiringScheduleEntity>>,
    todayDayOfWeek: Int,
    isMyList: Boolean = false
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val days = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        days.forEach { dayOfWeek ->
            val isToday = dayOfWeek == todayDayOfWeek
            val episodes = groupedByDay[dayOfWeek] ?: emptyList()

            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Day letter
                Text(
                    text = getDayLetter(dayOfWeek),
                    style = TextStyle(
                        color = if (isToday) GlanceTheme.colors.primary
                        else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                    )
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Indicator dot(s) or count pill
                if (episodes.isNotEmpty()) {
                    if (episodes.size == 1) {
                        // Single dot
                        Box(
                            modifier = GlanceModifier
                                .size(8.dp)
                                .cornerRadius(4.dp)
                                .background(
                                    if (isToday) GlanceTheme.colors.primary
                                    else GlanceTheme.colors.tertiary
                                ),
                            contentAlignment = Alignment.Center
                        ) {}
                    } else {
                        // Count pill
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(6.dp)
                                .background(
                                    if (isToday) GlanceTheme.colors.primary
                                    else GlanceTheme.colors.tertiaryContainer
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${episodes.size}",
                                style = TextStyle(
                                    color = if (isToday) GlanceTheme.colors.onPrimary
                                    else GlanceTheme.colors.onTertiaryContainer,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    // Empty placeholder for alignment
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }

                // Today indicator line
                if (isToday) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(16.dp)
                            .height(2.dp)
                            .cornerRadius(1.dp)
                            .background(GlanceTheme.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }
        }
    }
}
/**
 * EXPANDED: Full week view with episode lists per day
 */
@Composable
private fun CalendarExpanded(
    groupedByDay: Map<Int, List<AiringScheduleEntity>>,
    todayDayOfWeek: Int,
    isMyList: Boolean = false,
    expandToday: Boolean = false
) {
    // Only show today and future days (filter out past days)
    val allDays = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
    
    // Reorder days starting from today through the week
    val todayIndex = allDays.indexOf(todayDayOfWeek)
    val days = if (todayIndex >= 0) {
        allDays.subList(todayIndex, allDays.size) + allDays.subList(0, todayIndex)
    } else {
        allDays
    }.take(7) // Show next 7 days starting from today

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
    ) {
        // Header with week info
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.calendar_view_week_24px),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(24.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "This Week",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            // Filter Toggle Pill (All / My List)
            Box(
                modifier = GlanceModifier
                    .cornerRadius(16.dp)
                    .background(
                        if (isMyList) GlanceTheme.colors.primary
                        else GlanceTheme.colors.surfaceVariant
                    )
                    .clickable(actionRunCallback<ToggleCalendarFilterAction>())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMyList) "My List" else "All",
                    style = TextStyle(
                        color = if (isMyList) GlanceTheme.colors.onPrimary
                               else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Days list
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(days) { dayOfWeek ->
                // Wrap each day in Column with padding like UpNextWidget
                Column(
                    modifier = GlanceModifier.fillMaxWidth().padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (dayOfWeek != Calendar.SUNDAY) 12.dp else 16.dp
                    )
                ) {
                    val isToday = dayOfWeek == todayDayOfWeek
                    DayRowExpanded(
                        dayOfWeek = dayOfWeek,
                        isToday = isToday,
                        episodes = groupedByDay[dayOfWeek] ?: emptyList(),
                        isMyList = isMyList,
                        isExpanded = if (isToday) expandToday else true // Only today can be collapsed
                    )
                }
            }
        }
    }
}


// -------------------------------------------------------------------------
// COMPONENTS
// -------------------------------------------------------------------------

@Composable
private fun DayRowExpanded(
    dayOfWeek: Int,
    isToday: Boolean,
    episodes: List<AiringScheduleEntity>,
    isMyList: Boolean = false,
    isExpanded: Boolean = true
) {
    val context = LocalContext.current
    val maxCollapsedItems = 4
    val hasMoreItems = episodes.size > maxCollapsedItems
    val displayedEpisodes = if (isExpanded || !hasMoreItems) episodes else episodes.take(maxCollapsedItems)
    val hiddenCount = episodes.size - maxCollapsedItems

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(14.dp)
    ) {
        // Day header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(48.dp)
                    .cornerRadius(10.dp)
                    .background(
                        if (isToday) GlanceTheme.colors.primary
                        else GlanceTheme.colors.surfaceVariant
                    )
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = getDayAbbreviation(dayOfWeek),
                        style = TextStyle(
                            color = if (isToday) GlanceTheme.colors.onPrimary
                            else GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = getDayName(dayOfWeek).take(3),
                        style = TextStyle(
                            color = if (isToday) GlanceTheme.colors.onPrimary
                            else GlanceTheme.colors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            if (episodes.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.width(12.dp))
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(8.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${episodes.size} ${if (episodes.size == 1) "ep" else "eps"}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Expand/Collapse Button (Header) - Only for Today if more items exist
            if (isToday && hasMoreItems) {
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                   // Clickable area for better touch target
                   Box(
                       modifier = GlanceModifier
                           .cornerRadius(20.dp)
                           .clickable(actionRunCallback<ToggleExpandTodayAction>())
                           .padding(8.dp),
                       contentAlignment = Alignment.Center
                   ) {
                       Image(
                           provider = ImageProvider(
                               if (isExpanded) R.drawable.expand_all_24px
                               else R.drawable.collapse_all_24px
                           ),
                           contentDescription = "Expand",
                           colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                           modifier = GlanceModifier.size(24.dp)
                       )
                   }
                }
            }
        }

        // Episodes list (limited when collapsed for today)
        if (episodes.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(10.dp))

            Column {
                displayedEpisodes.forEachIndexed { index, episode ->
                    val detailsIntent = createDetailsIntent(context, episode.mediaId)

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(8.dp)
                            .clickable(actionStartActivity(detailsIntent))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimeCompact(episode.airingAt),
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.width(44.dp)
                        )

                        Box(
                            modifier = GlanceModifier
                                .size(4.dp)
                                .cornerRadius(2.dp)
                                .background(GlanceTheme.colors.outline),
                            contentAlignment = Alignment.Center
                        ) {}

                        Spacer(modifier = GlanceModifier.width(10.dp))

                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = episode.titleUserPreferred,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = "Episode ${episode.episode}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Divider between episodes (not after last displayed item)
                    if (index < displayedEpisodes.size - 1) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlanceTheme.colors.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                }
            }
        } else {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = if (isMyList) "No favorites airing" else "No episodes scheduled",
                style = TextStyle(
                    color = ColorProvider(
                        day = Color(0xB3000000), // 70% black
                        night = Color(0xB3FFFFFF)  // 70% white
                    ),
                    fontSize = 12.sp
                )
            )
        }
    }
}


// -------------------------------------------------------------------------
// HELPERS
// -------------------------------------------------------------------------

private fun getDayAbbreviation(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        Calendar.MONDAY -> "M"
        Calendar.TUESDAY -> "T"
        Calendar.WEDNESDAY -> "W"
        Calendar.THURSDAY -> "T"
        Calendar.FRIDAY -> "F"
        Calendar.SATURDAY -> "S"
        Calendar.SUNDAY -> "S"
        else -> "?"
    }
}

private fun getDayLetter(dayOfWeek: Int): String {
    return getDayAbbreviation(dayOfWeek)
}

private fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        Calendar.SUNDAY -> "Sunday"
        else -> "Unknown"
    }
}

private fun getRelativeDay(currentDay: Int, offset: Int): Int {
    val adjusted = currentDay + offset
    return when {
        adjusted > 7 -> adjusted - 7
        adjusted < 1 -> adjusted + 7
        else -> adjusted
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(java.util.Date(timestamp * 1000))
}

private fun formatTimeCompact(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(java.util.Date(timestamp * 1000))
}

private fun createDetailsIntent(context: Context, mediaId: Int): Intent {
    return Intent(
        Intent.ACTION_VIEW,
        "anisync://details/$mediaId".toUri()
    ).apply {
        component = null
        setClass(context, MainActivity::class.java)
    }
}
