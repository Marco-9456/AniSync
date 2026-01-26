package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
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
import androidx.glance.text.TextStyle
import com.anisync.android.MainActivity
import com.anisync.android.R
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AiringTodayWidgetEntryPoint {
    fun airingScheduleDao(): AiringScheduleDao
}

class AiringTodayWidget : GlanceAppWidget() {

    // Layout Guide: Consistent responsive sizing with UpNextWidget
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp), // 2x1
            DpSize(250.dp, 100.dp), // 3x1 / 4x1
            DpSize(250.dp, 200.dp)  // 3x2+
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, AiringTodayWidgetEntryPoint::class.java)
        val dao = entryPoint.airingScheduleDao()

        // Data Logic: Calculate today's range
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis / 1000
        val endOfDay = startOfDay + 86400

        val allSchedules = try {
            dao.getAiringBetween(startOfDay, endOfDay)
        } catch (e: Exception) {
            emptyList()
        }

        // Prefetch images efficiently
        // Using consistent width/height requests as UpNextWidget for cache hits if possible
        val dataWithBitmaps = coroutineScope {
            allSchedules.map { entry ->
                async {
                    val bitmap = WidgetImageUtils.loadBitmap(appContext, entry.coverUrl, width = 150, height = 220)
                    entry to bitmap
                }
            }.awaitAll()
        }

        provideContent {
            GlanceTheme {
                // State Management
                val prefs = currentState<Preferences>()
                // Use the Key from the separate file
                val filterMyList = prefs[ToggleFilterAction.FilterKey] ?: false

                val filteredData = if (filterMyList) {
                    dataWithBitmaps.filter { it.first.isWatching }
                } else {
                    dataWithBitmaps
                }

                AiringTodayWidgetContent(filteredData, filterMyList)
            }
        }
    }
}

@Composable
fun AiringTodayWidgetContent(
    data: List<Pair<AiringScheduleEntity, Bitmap?>>,
    isMyList: Boolean
) {
    // Style Guide: Consistent container style
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp)
    ) {
        val size = LocalSize.current
        val showHeader = size.height >= 120.dp

        if (showHeader) {
            AiringHeader(isMyList)
            Spacer(modifier = GlanceModifier.height(12.dp))
        }

        if (data.isEmpty()) {
            // Quality Guide: Informative empty state
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_menu_today),
                        contentDescription = null,
                        colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                        modifier = GlanceModifier.size(48.dp)
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = if (isMyList) "No favorites airing" else "Nothing airing today",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (isMyList) {
                        Text(
                            text = "Switch to 'All' to discover new shows.",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            ),
                            modifier = GlanceModifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(data) { (entry, bitmap) ->
                    AiringItem(entry, bitmap)
                    Spacer(modifier = GlanceModifier.height(12.dp)) // Increased spacing for better touch/visuals
                }
            }
        }
    }
}

@Composable
fun AiringHeader(isMyList: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        Image(
            provider = ImageProvider(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = GlanceModifier.size(24.dp)
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = "Airing Today",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        // Filter Toggle
        // Visual feedback for active state
        val bgModifier = if (isMyList) {
            GlanceModifier.background(GlanceTheme.colors.primary)
        } else {
            GlanceModifier.background(GlanceTheme.colors.surfaceVariant)
        }

        val textColor = if (isMyList) {
            GlanceTheme.colors.onPrimary
        } else {
            GlanceTheme.colors.onSurfaceVariant
        }

        // Quality Guide: Touch Target.
        // We wrap the pill in a Box that ensures height is at least 48dp for clicking
        Box(
            modifier = GlanceModifier
                .height(48.dp)
                .clickable(actionRunCallback<ToggleFilterAction>()),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Visual Pill
            Box(
                modifier = GlanceModifier
                    .cornerRadius(16.dp)
                    .then(bgModifier)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMyList) "My List" else "All",
                    style = TextStyle(
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun AiringItem(entry: AiringScheduleEntity, bitmap: Bitmap?) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val date = Date(entry.airingAt * 1000)
    val timeString = timeFormat.format(date)

    val detailsIntent = Intent(
        Intent.ACTION_VIEW,
        "anisync://details/${entry.mediaId}".toUri()
    ).apply {
        component = null
        setClass(LocalContext.current, MainActivity::class.java)
    }

    // Style Guide: Consistent card styling
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(12.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(detailsIntent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time Column - Distinct visuals
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.width(48.dp)
        ) {
            Text(
                text = timeString,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
        }

        // Vertical Divider
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .height(40.dp)
                .background(GlanceTheme.colors.outline)
        ) {}

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Content
        Row(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Image - Consistent Aspect Ratio (48x68 approx)
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
                }
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                // Typography: Improved readability
                Text(
                    text = entry.titleUserPreferred,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    ),
                    maxLines = 2
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Episode ${entry.episode}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}