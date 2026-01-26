package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
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
    // Better Approach: Fetch everything, then filter in UI or simple memory filter.
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, AiringTodayWidgetEntryPoint::class.java)
        val dao = entryPoint.airingScheduleDao()
        
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
        
        // Prefetch images
        val dataWithBitmaps = coroutineScope {
            allSchedules.map { entry ->
                async {
                    val bitmap = WidgetImageUtils.loadBitmap(appContext, entry.coverUrl, width = 100, height = 150)
                    entry to bitmap
                }
            }.awaitAll()
        }

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
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
    data: List<Pair<AiringScheduleEntity, android.graphics.Bitmap?>>,
    isMyList: Boolean
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Airing Today",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            Button(
                text = if (isMyList) "My List" else "All",
                onClick = actionRunCallback<ToggleFilterAction>()
            )
        }

        if (data.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isMyList) "No favorites airing today" else "Nothing airing today",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(data) { (entry, bitmap) ->
                    AiringItem(entry, bitmap)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AiringItem(entry: AiringScheduleEntity, bitmap: android.graphics.Bitmap?) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val date = Date(entry.airingAt * 1000)
    val timeString = timeFormat.format(date)
    
    val detailsIntent = Intent(
        Intent.ACTION_VIEW,
        "anisync://details/${entry.mediaId}".toUri()
    ).apply {
        component = null
        setClass(androidx.glance.LocalContext.current, MainActivity::class.java)
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(8.dp)
            .clickable(actionStartActivity(detailsIntent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time
        Text(
            text = timeString,
            style = TextStyle(
                color = GlanceTheme.colors.primary, 
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(end = 12.dp)
        )

        // Image
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.size(40.dp),
                contentScale = ContentScale.Crop
            )
        } else {
             Image(
                provider = ImageProvider(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = GlanceModifier.size(40.dp),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = GlanceModifier.width(12.dp))

        // Title & Ep
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                maxLines = 1
            )
            Text(
                text = "Ep ${entry.episode}",
                style = TextStyle(color = GlanceTheme.colors.secondary)
            )
        }
    }
}
