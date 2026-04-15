package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.anisync.android.R
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.widget.actions.ToggleCalendarFilterAction
import com.anisync.android.widget.core.SizeClass
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntentUtils
import com.anisync.android.widget.designsystem.components.EmptyStateConfig
import com.anisync.android.widget.designsystem.components.MediaPoster
import com.anisync.android.widget.designsystem.components.StandardEpisodeBadge
import com.anisync.android.widget.designsystem.components.WidgetEmptyState
import com.anisync.android.widget.designsystem.tokens.WidgetTypography
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

/**
 * Hilt entry point used to inject dependencies directly into the widget.
 * Since Glance widgets do not have a standard Android component lifecycle,
 * we use this interface to resolve the [AiringScheduleDao].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WeeklyCalendarWidgetEntryPoint {
    fun airingScheduleDao(): AiringScheduleDao
}

/**
 * An action callback triggered when the user clicks on a specific day in the calendar strip.
 * It updates the widget's internal state with the new offset relative to today.
 */
class ChangeSelectedDayAction : ActionCallback {
    companion object {
        val SelectedDayOffsetKey = intPreferencesKey("selected_day_offset")
        val OffsetParamKey = ActionParameters.Key<Int>("offset_param")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val offset = parameters[OffsetParamKey] ?: 0
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[SelectedDayOffsetKey] = offset
        }
        WeeklyCalendarWidget().update(context, glanceId)
    }
}

/**
 * A Jetpack Glance widget that displays an interactive 7-day schedule of airing anime.
 * Uses the modern card-based Material 3 design system. It strictly requires a minimum size
 * of 4x4 launcher cells.
 */
class WeeklyCalendarWidget : GlanceAppWidget() {

    // Explicitly define the state definition to ensure Glance initializes and maps states correctly
    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 250.dp),  // 4x4 cells (Minimum allowed size)
            DpSize(250.dp, 310.dp),  // 4x5 cells
            DpSize(310.dp, 250.dp),  // 5x4 cells
            DpSize(310.dp, 310.dp)   // 5x5 cells
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WeeklyCalendarWidgetEntryPoint::class.java
        )
        val dao = entryPoint.airingScheduleDao()

        // Safely extract preferences before loading data
        val prefs = try {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        } catch (e: Exception) {
            null
        }

        val filterMyList = prefs?.get(ToggleCalendarFilterAction.CalendarFilterKey) ?: false
        val selectedDayOffset = prefs?.get(ChangeSelectedDayAction.SelectedDayOffsetKey) ?: 0

        val today = LocalDate.now()
        val startOfTodayMillis = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val endOfWeekMillis = today.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val selectedDate = today.plusDays(selectedDayOffset.toLong())

        // Fetch the entire 7-day schedule from the local DB exactly once per update session.
        val weeklySchedules = withContext(Dispatchers.IO) {
            try {
                dao.getAiringBetween(startOfTodayMillis, endOfWeekMillis)
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Apply filters based on whether the user only wants to see shows from "My List"
        val displaySchedules = if (filterMyList) {
            weeklySchedules.filter { it.isWatching }
        } else {
            weeklySchedules
        }

        val groupedByDate = displaySchedules.groupBy { schedule ->
            Instant.ofEpochSecond(schedule.airingAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        val selectedEpisodes = groupedByDate[selectedDate] ?: emptyList()

        // Pre-load all cover images safely using a supervisorScope with higher res for cards
        val loadedImages = supervisorScope {
            selectedEpisodes.map { entry ->
                async(Dispatchers.IO) {
                    val bitmap = try {
                        WidgetImageLoader.loadBitmap(
                            appContext,
                            entry.coverUrl,
                            width = 200,
                            height = 300
                        )
                    } catch (e: Exception) {
                        null
                    }
                    entry.id to bitmap
                }
            }.awaitAll().toMap()
        }

        provideContent {
            GlanceTheme {
                val currentPrefs = currentState<Preferences>()
                val isMyList = currentPrefs[ToggleCalendarFilterAction.CalendarFilterKey] ?: false

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                ) {
                    CalendarExpanded(
                        today = today,
                        selectedDate = selectedDate,
                        selectedEpisodes = selectedEpisodes,
                        loadedImages = loadedImages,
                        isMyList = isMyList
                    )
                }
            }
        }
    }
}

/**
 * The primary fully-expanded layout for the Weekly Calendar Widget.
 * Displays a 7-day interactive selector strip and a scrollable timeline of beautifully styled cards.
 */
@Composable
private fun CalendarExpanded(
    today: LocalDate,
    selectedDate: LocalDate,
    selectedEpisodes: List<AiringScheduleEntity>,
    loadedImages: Map<Int, Bitmap?>,
    isMyList: Boolean
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        // --- Header Section ---
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.calendar_view_week_24px),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    modifier = GlanceModifier.size(20.dp)
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Text(
                text = "Weekly Schedule",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = WidgetTypography.Title.large,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            // Dynamic Filter Toggle Pill
            Box(
                modifier = GlanceModifier
                    .cornerRadius(100.dp) // Full pill shape
                    .background(
                        if (isMyList) GlanceTheme.colors.primary
                        else GlanceTheme.colors.surfaceVariant
                    )
                    .clickable(actionRunCallback<ToggleCalendarFilterAction>())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMyList) "My List" else "All",
                    style = TextStyle(
                        color = if (isMyList) GlanceTheme.colors.onPrimary
                        else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = WidgetTypography.Caption.large,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // --- Day Selector Strip ---
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            for (i in 0..6) {
                val date = today.plusDays(i.toLong())
                val isSelected = date == selectedDate
                val dayChar =
                    date.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, Locale.getDefault())
                        .uppercase()
                val dateNum = date.dayOfMonth.toString()

                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 2.dp)
                        .cornerRadius(12.dp)
                        .background(
                            if (isSelected) GlanceTheme.colors.primary
                            else ColorProvider(Color.Transparent, Color.Transparent)
                        )
                        .clickable(
                            actionRunCallback<ChangeSelectedDayAction>(
                                actionParametersOf(ChangeSelectedDayAction.OffsetParamKey to i)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = dayChar,
                            style = TextStyle(
                                color = if (isSelected) GlanceTheme.colors.onPrimary
                                else GlanceTheme.colors.onSurfaceVariant,
                                fontSize = WidgetTypography.Caption.large,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = dateNum,
                            style = TextStyle(
                                color = if (isSelected) GlanceTheme.colors.onPrimary
                                else GlanceTheme.colors.onSurface,
                                fontSize = WidgetTypography.Body.large,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // --- Episode List Cards ---
        if (selectedEpisodes.isEmpty()) {
            WidgetEmptyState(
                config = EmptyStateConfig(
                    iconResId = R.drawable.calendar_view_week_24px,
                    title = if (isMyList) "No favorites airing" else "No episodes scheduled",
                    subtitle = "Try selecting another day"
                ),
                sizeClass = SizeClass.EXPANDED,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                itemsIndexed(selectedEpisodes) { index, episode ->
                    val context = LocalContext.current
                    val detailsIntent =
                        WidgetIntentUtils.createDetailsIntent(context, episode.mediaId)
                    val bitmap = loadedImages[episode.id]

                    // Render as elevated, isolated cards
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(GlanceTheme.colors.surface)
                                .cornerRadius(16.dp)
                                .clickable(actionStartActivity(detailsIntent))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTimeCompact(episode.airingAt),
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = WidgetTypography.Body.large,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier.width(48.dp)
                            )

                            Spacer(modifier = GlanceModifier.width(12.dp))

                            MediaPoster(
                                bitmap = bitmap,
                                width = 56.dp,
                                height = 80.dp,
                                cornerRadius = 8.dp
                            )

                            Spacer(modifier = GlanceModifier.width(16.dp))

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = episode.titleUserPreferred,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = WidgetTypography.Body.large,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 2
                                )

                                Spacer(modifier = GlanceModifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StandardEpisodeBadge(episodeNumber = episode.episode)

                                    if (episode.isWatching) {
                                        Spacer(modifier = GlanceModifier.width(8.dp))
                                        Image(
                                            provider = ImageProvider(R.drawable.ic_bookmark_24px),
                                            contentDescription = "On My List",
                                            colorFilter = androidx.glance.ColorFilter.tint(
                                                GlanceTheme.colors.primary
                                            ),
                                            modifier = GlanceModifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add bottom padding for the very last item so it doesn't hug the edge
                    if (index == selectedEpisodes.size - 1) {
                        Spacer(modifier = GlanceModifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Formats a Unix timestamp into a compact standard time string.
 * @param timestamp The Unix timestamp in seconds.
 * @return A formatted string (e.g. "14:30") in the local timezone.
 */
private fun formatTimeCompact(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val time = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
    return time.format(formatter)
}