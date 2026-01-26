package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
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
import androidx.glance.text.TextStyle

import com.anisync.android.MainActivity
import com.anisync.android.R
import com.anisync.android.data.local.dao.TrendingDao
import com.anisync.android.data.local.entity.TrendingEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TrendingWidgetEntryPoint {
    fun trendingDao(): TrendingDao
}

class TrendingWidget : GlanceAppWidget() {

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
        val entryPoint = EntryPointAccessors.fromApplication(appContext, TrendingWidgetEntryPoint::class.java)
        val dao = entryPoint.trendingDao()

        val trendingList = try {
            dao.getTopTrending(limit = 12) // Top 12 items
        } catch (e: Exception) {
            emptyList()
        }

        // Prefetch images
        val dataWithBitmaps = coroutineScope {
            trendingList.map { entry ->
                async {
                    // Larger images for grid visual quality
                    val bitmap = WidgetImageUtils.loadBitmap(appContext, entry.coverUrl, width = 200, height = 300)
                    entry to bitmap
                }
            }.awaitAll()
        }

        // Layout Guide: Grid Construction
        // Chunk into rows of 3 for a balanced grid layout
        val rows = dataWithBitmaps.chunked(3)

        provideContent {
            GlanceTheme {
                TrendingWidgetContent(rows)
            }
        }
    }
}

@Composable
fun TrendingWidgetContent(rows: List<List<Pair<TrendingEntity, Bitmap?>>>) {
    // Style Guide: System Coherence (Background & Radius)
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
            TrendingHeader()
            Spacer(modifier = GlanceModifier.height(12.dp))
        }

        if (rows.isEmpty()) {
            EmptyTrendsState()
        } else {
            // Layout Guide: "Image Grid" Canonical Layout
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(rows) { rowItems ->
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        rowItems.forEachIndexed { index, item ->
                            val (entry, bitmap) = item

                            // Grid Item Container
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .height(180.dp) // Updated to better match poster aspect ratio (approx 2:3)
                                    .padding(4.dp)
                            ) {
                                TrendingItem(entry, bitmap)
                            }
                        }

                        // Fill empty space to maintain column width if row is incomplete
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = GlanceModifier.defaultWeight().padding(4.dp))
                            }
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun TrendingHeader() {
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
            text = "Trending Now",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        // Quick Access Action
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
                provider = ImageProvider(android.R.drawable.ic_menu_search), // Search/Explore icon
                contentDescription = "Browse",
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}

@Composable
fun TrendingItem(entry: TrendingEntity, bitmap: Bitmap?) {
    val detailsIntent = Intent(
        Intent.ACTION_VIEW,
        "anisync://details/${entry.id}".toUri()
    ).apply {
        component = null
        setClass(LocalContext.current, MainActivity::class.java)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(12.dp) // Soft rounded corners for images
            .clickable(actionStartActivity(detailsIntent)),
        contentAlignment = Alignment.BottomStart
    ) {
        // Background / Image
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = entry.titleUserPreferred,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surfaceVariant)
            ) {}
        }

        // Gradient overlay for text readability could go here,
        // but simple badge is cleaner for small widgets.

        // Rank Badge
        Box(
            modifier = GlanceModifier
                .padding(8.dp)
                .background(GlanceTheme.colors.primaryContainer) // Semi-transparent backing
                .cornerRadius(8.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "#${entry.rank}",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun EmptyTrendsState() {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                provider = ImageProvider(android.R.drawable.stat_notify_error),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(32.dp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Unable to load trends",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        }
    }
}