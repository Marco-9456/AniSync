package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.anisync.android.widget.actions.ToggleFilterAction
import com.anisync.android.widget.core.SizeClass
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntentUtils
import com.anisync.android.widget.core.toSizeClass
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AiringTodayWidgetEntryPoint {
    fun airingScheduleDao(): AiringScheduleDao
}

/**
 * A beautifully redesigned Jetpack Glance widget that displays today's anime airing schedule.
 * Adapts seamlessly across Compact, Medium, and Expanded states using a modern card-based UI
 * and an elegant timeline layout.
 */
class AiringTodayWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 100.dp),  // Compact (1 immediate item Card)
            DpSize(250.dp, 100.dp),  // Medium (1 wide immediate item Card)
            DpSize(250.dp, 220.dp),  // Expanded (Timeline List)
            DpSize(310.dp, 310.dp)   // Large Expanded
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            AiringTodayWidgetEntryPoint::class.java
        )
        val dao = entryPoint.airingScheduleDao()

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis / 1000
        val endOfDay = startOfDay + 86400

        val allSchedules = withContext(Dispatchers.IO) {
            try {
                dao.getAiringBetween(startOfDay, endOfDay)
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Higher res fetch for premium rendering on rounded cards
        val schedulesWithImages = coroutineScope {
            allSchedules.map { entry ->
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
                    entry to bitmap
                }
            }.awaitAll()
        }

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val filterMyList = prefs[ToggleFilterAction.FilterKey] ?: false

                val filteredData = if (filterMyList) {
                    schedulesWithImages.filter { it.first.isWatching }
                } else {
                    schedulesWithImages
                }

                val sizeClass = LocalSize.current.toSizeClass()

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                ) {
                    when (sizeClass) {
                        SizeClass.COMPACT, SizeClass.MEDIUM -> {
                            AiringImmediateItem(
                                entries = filteredData,
                                isMyList = filterMyList,
                                sizeClass = sizeClass
                            )
                        }

                        SizeClass.EXPANDED -> {
                            AiringExpanded(
                                entries = filteredData,
                                isMyList = filterMyList
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// LAYOUTS
// -------------------------------------------------------------------------

/**
 * Compact/Medium layout that finds and highlights the single most immediate upcoming episode in a premium card.
 * Intelligently scales dimensions based on available SizeClass to prevent truncation.
 */
@Composable
private fun AiringImmediateItem(
    entries: List<Pair<AiringScheduleEntity, Bitmap?>>,
    isMyList: Boolean,
    sizeClass: SizeClass
) {
    val context = LocalContext.current

    // Find the next airing item today, or fallback to the first item if all have already aired.
    val entry = entries.firstOrNull { (it.first.airingAt * 1000) > System.currentTimeMillis() }
        ?: entries.firstOrNull()

    if (entry == null) {
        // Tappable empty state that toggles the filter
        WidgetEmptyState(
            config = EmptyStateConfig(
                iconResId = R.drawable.today_24px,
                title = if (isMyList) "No favorites today" else "Nothing today",
                subtitle = if (isMyList) "Tap to view global schedule" else "Check back later"
            ),
            sizeClass = SizeClass.COMPACT,
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionRunCallback<ToggleFilterAction>())
        )
        return
    }

    val (schedule, bitmap) = entry
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(schedule.airingAt * 1000))
    val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, schedule.mediaId)

    // Dynamic sizing to prevent cramping in COMPACT mode
    val isCompact = sizeClass == SizeClass.COMPACT
    val cardPadding = if (isCompact) 8.dp else 12.dp
    val posterWidth = if (isCompact) 52.dp else 64.dp
    val posterHeight = if (isCompact) 76.dp else 96.dp
    val spacing = if (isCompact) 8.dp else 12.dp
    val iconSize = if (isCompact) 14.dp else 16.dp
    val headerText = if (isCompact) timeString else "Airing at $timeString"

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner Card Container
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(20.dp) // Premium large rounding
                .clickable(actionStartActivity(detailsIntent))
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaPoster(
                bitmap = bitmap,
                width = posterWidth,
                height = posterHeight,
                cornerRadius = 12.dp
            )

            Spacer(modifier = GlanceModifier.width(spacing))

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.today_24px),
                        contentDescription = null,
                        colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                        modifier = GlanceModifier.size(iconSize)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = headerText,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = WidgetTypography.Label.large,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = schedule.titleUserPreferred,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = WidgetTypography.Body.large,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StandardEpisodeBadge(episodeNumber = schedule.episode)
                    if (schedule.isWatching) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Image(
                            provider = ImageProvider(R.drawable.ic_bookmark_24px),
                            contentDescription = "On My List",
                            colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                            modifier = GlanceModifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Expanded layout featuring a floating header, interactive toggle pill, and a beautiful modern timeline.
 */
@Composable
private fun AiringExpanded(
    entries: List<Pair<AiringScheduleEntity, Bitmap?>>,
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
                    provider = ImageProvider(R.drawable.today_24px),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    modifier = GlanceModifier.size(20.dp)
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Text(
                text = "Airing Today",
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
                    .clickable(actionRunCallback<ToggleFilterAction>())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMyList) "My List" else "All",
                    style = TextStyle(
                        color = if (isMyList) GlanceTheme.colors.onPrimary
                        else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = WidgetTypography.Label.large,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // --- Timeline List ---
        if (entries.isEmpty()) {
            WidgetEmptyState(
                config = EmptyStateConfig(
                    iconResId = R.drawable.today_24px,
                    title = if (isMyList) "No favorites today" else "Nothing today",
                    subtitle = if (isMyList) "Tap 'All' above for global schedule" else "Check back tomorrow"
                ),
                sizeClass = SizeClass.EXPANDED,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                itemsIndexed(entries) { index, item ->
                    TimelineItem(
                        item = item,
                        isFirst = index == 0,
                        isLast = index == entries.lastIndex
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// REUSABLE UI COMPONENTS
// -------------------------------------------------------------------------

/**
 * A beautiful, integrated timeline cell that blends the vertical line, time badge, and elevated media card.
 */
@Composable
private fun TimelineItem(
    item: Pair<AiringScheduleEntity, Bitmap?>,
    isFirst: Boolean,
    isLast: Boolean
) {
    val (schedule, bitmap) = item
    val context = LocalContext.current
    val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, schedule.mediaId)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(schedule.airingAt * 1000))

    // Wrapping Column handles the start/end padding to prevent scrollbar overlap
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(104.dp), // Fixed height ensures the timeline stem connects perfectly
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Timeline Stem (Line + Time Badge) ---
            Box(
                modifier = GlanceModifier
                    .width(48.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Background Line Stems
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = GlanceModifier
                            .width(2.dp)
                            .defaultWeight()
                            .background(
                                if (isFirst) GlanceTheme.colors.widgetBackground
                                else GlanceTheme.colors.surfaceVariant
                            )
                    ) {}
                    Box(
                        modifier = GlanceModifier
                            .width(2.dp)
                            .defaultWeight()
                            .background(
                                if (isLast) GlanceTheme.colors.widgetBackground
                                else GlanceTheme.colors.surfaceVariant
                            )
                    ) {}
                }

                // Centered Time Badge
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(6.dp)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeString,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = WidgetTypography.Label.medium,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // --- Media Card ---
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(vertical = 6.dp), // Gap between stacked cards
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(16.dp)
                        .clickable(actionStartActivity(detailsIntent))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaPoster(
                        bitmap = bitmap,
                        width = 56.dp,
                        height = 80.dp,
                        cornerRadius = 8.dp
                    )

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = schedule.titleUserPreferred,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = WidgetTypography.Body.large,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StandardEpisodeBadge(episodeNumber = schedule.episode)

                            if (schedule.isWatching) {
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Image(
                                    provider = ImageProvider(R.drawable.ic_bookmark_24px),
                                    contentDescription = "On My List",
                                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                                    modifier = GlanceModifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}