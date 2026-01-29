package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
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
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.anisync.android.MainActivity
import com.anisync.android.R
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
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
                    val bitmap = WidgetImageUtils.loadBitmap(
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

                val size = LocalSize.current

                when {
                    size.height <= 110.dp -> AiringCompact(
                        entries = filteredData,
                        isMyList = filterMyList,
                        allCount = allSchedules.size
                    )
                    size.height <= 120.dp -> AiringMedium(
                        entries = filteredData,
                        isMyList = filterMyList,
                        allCount = allSchedules.size
                    )
                    else -> AiringExpanded(
                        entries = filteredData,
                        isMyList = filterMyList,
                        allCount = allSchedules.size
                    )
                }
            }
        }
    }
}

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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time Column with optional star badge (only in All mode for favorites)
        Column(
            modifier = GlanceModifier.width(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeString.substringBefore(":"),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = timeString.substringAfter(":"),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            // Show star badge only in All mode and if this item is in user's list
            if (!isMyList && schedule.isWatching) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(4.dp)
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "★",
                        style = TextStyle(
                            color = GlanceTheme.colors.onTertiaryContainer,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .height(48.dp)
                .background(GlanceTheme.colors.outline),
            contentAlignment = Alignment.Center
        ) {}

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Row {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(4.dp)
                        .background(
                            if (isMyList) GlanceTheme.colors.primaryContainer
                            else GlanceTheme.colors.surfaceVariant
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isMyList) "My List" else "All",
                        style = TextStyle(
                            color = if (isMyList) GlanceTheme.colors.onPrimaryContainer
                            else GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = schedule.titleUserPreferred,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                text = "Episode ${schedule.episode}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .clickable(actionRunCallback<ToggleFilterAction>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_sort_by_size),
                contentDescription = "Toggle Filter",
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AiringMedium(
    entries: List<Pair<AiringScheduleEntity, Bitmap?>>,
    isMyList: Boolean,
    allCount: Int
) {
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateMedium(isMyList)
        return
    }

    val primary = entries.first()
    val secondary = entries.getOrNull(1)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Airing Today",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            Box(
                modifier = GlanceModifier
                    .cornerRadius(16.dp)
                    .background(
                        if (isMyList) GlanceTheme.colors.primary
                        else GlanceTheme.colors.surfaceVariant
                    )
                    .clickable(actionRunCallback<ToggleFilterAction>())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMyList) "My List" else "All ($allCount)",
                    style = TextStyle(
                        color = if (isMyList) GlanceTheme.colors.onPrimary
                        else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (secondary != null && !isMyList) {
            Row(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                AiringCompactCard(primary, GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(8.dp))
                AiringCompactCard(secondary, GlanceModifier.defaultWeight())
            }
        } else {
            AiringRichCard(primary)
        }
    }
}

@Composable
private fun AiringExpanded(
    entries: List<Pair<AiringScheduleEntity, Bitmap?>>,
    isMyList: Boolean,
    allCount: Int
) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Airing Today",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (isMyList) "From your favorites" else "$allCount shows today",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }

            // Filter Pill - No star icon for My List, just text
            Box(
                modifier = GlanceModifier
                    .cornerRadius(20.dp)
                    .background(
                        if (isMyList) GlanceTheme.colors.primary
                        else GlanceTheme.colors.surfaceVariant
                    )
                    .clickable(actionRunCallback<ToggleFilterAction>())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMyList) "My List" else "All", // REMOVED STAR ICON
                    style = TextStyle(
                        color = if (isMyList) GlanceTheme.colors.onPrimary
                        else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        if (entries.isEmpty()) {
            EmptyStateExpanded(isMyList)
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(entries) { (schedule, bitmap) ->
                    // Pass isMyList to control star badge visibility
                    AiringTimelineItem(schedule, bitmap, isMyList)
                    Spacer(modifier = GlanceModifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun AiringCompactCard(
    entry: Pair<AiringScheduleEntity, Bitmap?>,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    val (schedule, _) = entry
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(schedule.airingAt * 1000))
    val detailsIntent = createDetailsIntent(context, schedule.mediaId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(12.dp)
            .clickable(actionStartActivity(detailsIntent))
            .padding(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = timeStr,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = schedule.titleUserPreferred,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        Text(
            text = "EP ${schedule.episode}",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun AiringRichCard(entry: Pair<AiringScheduleEntity, Bitmap?>) {
    val context = LocalContext.current
    val (schedule, _) = entry
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(schedule.airingAt * 1000))
    val detailsIntent = createDetailsIntent(context, schedule.mediaId)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(12.dp)
            .clickable(actionStartActivity(detailsIntent))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .height(48.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeStr,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

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
                text = "Episode ${schedule.episode}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
private fun AiringTimelineItem(
    schedule: AiringScheduleEntity,
    bitmap: Bitmap?,
    isMyList: Boolean // Added parameter to control star visibility
) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(schedule.airingAt * 1000))
    val detailsIntent = createDetailsIntent(context, schedule.mediaId)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(detailsIntent))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = GlanceModifier.width(52.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = timeStr,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            // Only show star badge in All mode (not My List mode) AND if it's in favorites
            if (!isMyList && schedule.isWatching) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(4.dp)
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "★",
                        style = TextStyle(
                            color = GlanceTheme.colors.onTertiaryContainer,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .height(56.dp)
                .background(GlanceTheme.colors.outline),
            contentAlignment = Alignment.Center
        ) {}

        Spacer(modifier = GlanceModifier.width(12.dp))

        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .height(68.dp)
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "EP",
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(0x80000000),
                            night = Color(0x80FFFFFF)
                        ),
                        fontSize = 12.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = schedule.titleUserPreferred,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Episode ${schedule.episode}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp
                )
            )
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
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_today),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(24.dp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
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
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_today),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(28.dp)
            )
        }

        Spacer(modifier = GlanceModifier.width(16.dp))

        Column {
            Text(
                text = if (isMyList) "No favorites airing today" else "No shows airing today",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            if (isMyList) {
                Text(
                    text = "Tap filter button to see all shows",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyStateExpanded(isMyList: Boolean) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = GlanceModifier
                    .size(64.dp)
                    .cornerRadius(20.dp)
                    .background(GlanceTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_menu_today),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.size(32.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(16.dp))

            Text(
                text = if (isMyList) "Your favorites aren't airing today" else "Nothing on the schedule today",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center // This is okay here as TextStyle param
                ),
                modifier = GlanceModifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (isMyList) {
                Text(
                    text = "Switch to 'All' to discover new shows",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
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