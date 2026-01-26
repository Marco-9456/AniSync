package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, TrendingWidgetEntryPoint::class.java)
        val dao = entryPoint.trendingDao()

        val trendingList = try {
            dao.getTopTrending(limit = 12) // Top 12 items
        } catch (e: Exception) {
            emptyList()
        }

        val dataWithBitmaps = coroutineScope {
            trendingList.map { entry ->
                async {
                    // Larger images for grid
                    val bitmap = WidgetImageUtils.loadBitmap(appContext, entry.coverUrl, width = 200, height = 300)
                    entry to bitmap
                }
            }.awaitAll()
        }
        
        // Chunk into rows of 3 for "Grid" effect using LazyColumn
        val rows = dataWithBitmaps.chunked(3)

        provideContent {
            GlanceTheme {
                TrendingWidgetContent(rows)
            }
        }
    }
}

@Composable
fun TrendingWidgetContent(rows: List<List<Pair<TrendingEntity, android.graphics.Bitmap?>>>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
    ) {
        Text(
            text = "Trending Now",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(bottom = 12.dp)
        )

        if (rows.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(), // This might need weight if inside column
                contentAlignment = Alignment.Center
            ) {
                 Text(
                    text = "Loading trends...",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(rows) { rowItems ->
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        rowItems.forEachIndexed { index, item ->
                            val (entry, bitmap) = item
                            
                            // Item
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .height(160.dp) // Fixed height for grid row
                                    .padding(4.dp)
                            ) {
                                TrendingItem(entry, bitmap)
                            }
                        }
                        
                        // Fill empty space if row is incomplete
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = GlanceModifier.defaultWeight())
                            }
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TrendingItem(entry: TrendingEntity, bitmap: android.graphics.Bitmap?) {
    val detailsIntent = Intent(
        Intent.ACTION_VIEW,
        "anisync://details/${entry.id}".toUri()
    ).apply {
        component = null
        setClass(androidx.glance.LocalContext.current, MainActivity::class.java)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(detailsIntent)),
        contentAlignment = Alignment.BottomStart
    ) {
        // Image
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
                    .background(GlanceTheme.colors.surfaceVariant),
                content = {}
            )
        }
        
        // Rank Badge
        Box(
            modifier = GlanceModifier
                .padding(4.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "#${entry.rank}",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
