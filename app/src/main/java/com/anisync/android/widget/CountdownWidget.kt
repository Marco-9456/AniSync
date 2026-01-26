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
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.unit.ColorProvider
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

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CountdownWidgetEntryPoint {
    fun airingScheduleDao(): AiringScheduleDao
}

class CountdownWidget : GlanceAppWidget() {

    // Layout Guide: Consistent responsive sizing
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp), // 2x1
            DpSize(250.dp, 100.dp), // 3x1 / 4x1
            DpSize(250.dp, 200.dp)  // 3x2+
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, CountdownWidgetEntryPoint::class.java)
        val dao = entryPoint.airingScheduleDao()

        // Data Logic: Fetch upcoming airing episodes (next 14 days)
        val now = System.currentTimeMillis() / 1000
        val rangeEnd = now + (14 * 24 * 60 * 60) // 2 weeks

        val upcomingSchedules = try {
            dao.getAiringBetween(now, rangeEnd)
                .filter { it.isWatching } // Only show user's list
                .sortedBy { it.airingAt }
                .take(10) // Limit for widget performance
        } catch (e: Exception) {
            emptyList()
        }

        // Image Logic: Load bitmaps for the list
        val dataWithBitmaps = coroutineScope {
            upcomingSchedules.map { entry ->
                async {
                    val bitmap = WidgetImageUtils.loadBitmap(appContext, entry.coverUrl, width = 400, height = 300)
                    entry to bitmap
                }
            }.awaitAll()
        }

        provideContent {
            GlanceTheme {
                CountdownWidgetContent(dataWithBitmaps)
            }
        }
    }
}

@Composable
fun CountdownWidgetContent(data: List<Pair<AiringScheduleEntity, Bitmap?>>) {
    // Style Guide: System Coherence
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
            CountdownHeader()
            Spacer(modifier = GlanceModifier.height(12.dp))
        }

        if (data.isEmpty()) {
            EmptyCountdownState()
        } else {
            // Layout Guide: Scrollable List of Hero Cards
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(data) { (entry, bitmap) ->
                    CountdownCard(entry, bitmap)
                    Spacer(modifier = GlanceModifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun CountdownHeader() {
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
        Text(
            text = "Countdown",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        val appIntent = Intent(LocalContext.current, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Quality Guide: Touch Target 48dp
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .clickable(actionStartActivity(appIntent)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_agenda),
                contentDescription = "Open Schedule",
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CountdownCard(entry: AiringScheduleEntity, bitmap: Bitmap?) {
    val detailsIntent = Intent(
        Intent.ACTION_VIEW,
        "anisync://details/${entry.mediaId}".toUri()
    ).apply {
        component = null
        setClass(LocalContext.current, MainActivity::class.java)
    }

    // Time Calculation
    val now = System.currentTimeMillis() / 1000
    val secondsDiff = entry.airingAt - now
    val timeString = formatCountdown(secondsDiff)

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(180.dp) // Fixed height for list items
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(detailsIntent)),
        contentAlignment = Alignment.BottomStart
    ) {
        // 1. Background Image
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.primaryContainer)
            ) {}
        }

        // 2. Dark Overlay (Gradient effect for readability)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.Black.copy(alpha = 0.5f)))
        ) {}

        // 3. Content
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section: Info
            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 2
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = "Episode ${entry.episode}",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF80DEEA)), // Cyan 200 equivalent
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Bottom Section: The Timer
            // Big, bold typography
            Text(
                text = timeString,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = "REMAINING",
                style = TextStyle(
                    color = ColorProvider(Color.LightGray),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun EmptyCountdownState() {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_lock_idle_alarm),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(32.dp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "No upcoming episodes",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
            Text(
                text = "Add anime to your watchlist",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }
    }
}

private fun formatCountdown(secondsDiff: Long): String {
    if (secondsDiff < 0) return "Aired"

    val days = secondsDiff / 86400
    val hours = (secondsDiff % 86400) / 3600
    val minutes = (secondsDiff % 3600) / 60

    return when {
        days > 1 -> "${days}d ${hours}h"
        days == 1L -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}