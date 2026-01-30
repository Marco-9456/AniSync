package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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
import com.anisync.android.widget.designsystem.components.MediaPoster
import com.anisync.android.widget.designsystem.components.TimeBadge
import com.anisync.android.widget.designsystem.tokens.WidgetDimensions
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

class AiringTodayWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 100.dp),
            DpSize(250.dp, 100.dp),
            DpSize(250.dp, 220.dp)
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

        val schedulesWithImages = coroutineScope {
            allSchedules.take(8).map { entry ->
                async {
                    val bitmap = WidgetImageLoader.loadBitmap(
                        appContext,
                        entry.coverUrl,
                        width = 150,
                        height = 220
                    )
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

                when (sizeClass) {
                    SizeClass.COMPACT -> AiringCompact(
                        entries = filteredData,
                        isMyList = filterMyList,
                        allCount = allSchedules.size
                    )
                    SizeClass.MEDIUM -> AiringMedium(
                        entries = filteredData,
                        isMyList = filterMyList,
                        allCount = allSchedules.size
                    )
                    SizeClass.EXPANDED -> AiringExpanded(
                        entries = filteredData,
                        isMyList = filterMyList
                    )
                }
            }
        }
    }
}

/**
 * COMPACT VIEW: Minimalist single item or count
 */
@Composable
private fun AiringCompact(
    entries: List<Pair<AiringScheduleEntity, Bitmap?>>,
    isMyList: Boolean,
    allCount: Int
) {
    val context = LocalContext.current
    val entry = entries.firstOrNull { (it.first.airingAt * 1000) > System.currentTimeMillis() }
        ?: entries.firstOrNull()

    if (entry == null) {
        EmptyStateCompact(isMyList)
        return
    }

    val (schedule, _) = entry
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(schedule.airingAt * 1000))
    val detailsIntent = createDetailsIntent(context, schedule.mediaId)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity(detailsIntent))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time Badge
        Box(
            modifier = GlanceModifier
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeString,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Content
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = schedule.titleUserPreferred,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                text = "Ep ${schedule.episode}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * MEDIUM VIEW: Simple list or cards
 */
@Composable
private fun AiringMedium(
    entries: List<Pair<AiringScheduleEntity, Bitmap?>>,
    isMyList: Boolean,
    allCount: Int
) {
    // Reusing the compact style but with a header for consistency with the new design language
    // In a real medium widget, we might show 2 items vertically.
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateMedium(isMyList)
        return
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.calendar_today_24px),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp),
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "Airing Today",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Show first 1-2 items
        entries.take(2).forEach { (schedule, _) ->
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeStr = timeFormat.format(Date(schedule.airingAt * 1000))

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeStr,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.width(40.dp)
                )
                Text(
                    text = schedule.titleUserPreferred,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * EXPANDED VIEW: Detailed Timeline Design (Matches Reference Image)
 */
@Composable
private fun AiringExpanded(
    entries: List<Pair<AiringScheduleEntity, Bitmap?>>,
    isMyList: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface) // Dark background
            .padding(16.dp)
    ) {
        // --- Header Section ---
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.calendar_today_24px), // Replace with your specific calendar icon
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.size(20.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "Airing Today",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(16.dp))

        // --- Segmented Control (Toggle) ---
        // Container
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(40.dp)
                .background(GlanceTheme.colors.surfaceVariant) // Darker grey container
                .cornerRadius(20.dp)
                .clickable(actionRunCallback<ToggleFilterAction>())
                .padding(4.dp)
        ) {
            // "All" Option
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(16.dp)
                    .background(
                        if (!isMyList) GlanceTheme.colors.onSurfaceVariant
                        else ColorProvider(day = Color.Transparent, night = Color.Transparent)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All",
                    style = TextStyle(
                        color = if (!isMyList) GlanceTheme.colors.surfaceVariant else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // "My List" Option
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(16.dp)
                    .background(
                        if (isMyList) GlanceTheme.colors.onSurfaceVariant
                        else ColorProvider(day = Color.Transparent, night = Color.Transparent)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My List",
                    style = TextStyle(
                        color = if (isMyList) GlanceTheme.colors.surfaceVariant else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(16.dp))

        // --- Timeline List ---
        if (entries.isEmpty()) {
            EmptyStateExpanded(isMyList)
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

@Composable
private fun TimelineItem(
    item: Pair<AiringScheduleEntity, Bitmap?>,
    isFirst: Boolean,
    isLast: Boolean
) {
    val (schedule, bitmap) = item
    val context = LocalContext.current
    val detailsIntent = createDetailsIntent(context, schedule.mediaId)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(schedule.airingAt * 1000))

    // Height of the item row - giving it a fixed minimum height helps align the lines perfectly
    val rowHeight = 80.dp

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(rowHeight)
            .clickable(actionStartActivity(detailsIntent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Timeline Column (Badge + Line) ---
        // We use a fixed width Box to contain the line and the badge
        Box(
            modifier = GlanceModifier.width(70.dp).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // The Vertical Line
            // Using a Column with two boxes to simulate Start/End/Through lines
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top half line
                Box(
                    modifier = GlanceModifier
                        .width(2.dp)
                        .defaultWeight()
                        .background(
                            if (isFirst) ColorProvider(day = Color.Transparent, night = Color.Transparent)
                            else GlanceTheme.colors.outline
                        )
                ) {}

                // Bottom half line
                Box(
                    modifier = GlanceModifier
                        .width(2.dp)
                        .defaultWeight()
                        .background(
                            if (isLast) ColorProvider(day = Color.Transparent, night = Color.Transparent)
                            else GlanceTheme.colors.outline
                        )
                ) {}
            }

            // The Time Badge (Pill) - Sits on top of the line
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primaryContainer) // Light Purple
                    .cornerRadius(8.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeString,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer, // Dark text
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        // --- Content Row ---
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Image
            Box(
                modifier = GlanceModifier
                    .width(48.dp)
                    .height(64.dp)
                    .cornerRadius(8.dp)
                    .background(GlanceTheme.colors.surfaceVariant), // Placeholder bg
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // Text Info
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = schedule.titleUserPreferred,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )
                Text(
                    text = "Episode ${schedule.episode}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }

            // Star Icon
            // Only show filled star if watching. Do not show empty star.
            if (schedule.isWatching) {
                Image(
                    provider = ImageProvider(android.R.drawable.btn_star_big_on),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary), // Yellow/Primary star
                    modifier = GlanceModifier.size(20.dp)
                )
            }
        }
    }
}


@Composable
private fun EmptyStateCompact(isMyList: Boolean) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .clickable(actionRunCallback<ToggleFilterAction>()),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isMyList) "No favorites" else "Nothing today",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun EmptyStateMedium(isMyList: Boolean) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isMyList) "No favorites airing today" else "No shows airing today",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun EmptyStateExpanded(isMyList: Boolean) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                provider = ImageProvider(R.drawable.calendar_today_24px),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(48.dp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = if (isMyList) "No favorites airing today" else "No schedule today",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            if (isMyList) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Tap 'All' to see global schedule",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

private fun createDetailsIntent(context: Context, mediaId: Int) = 
    WidgetIntentUtils.createDetailsIntent(context, mediaId)