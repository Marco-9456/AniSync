package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.anisync.android.MainActivity
import com.anisync.android.R
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.entity.LibraryEntryEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpNextWidgetEntryPoint {
    fun libraryDao(): LibraryDao
}

class UpNextWidget : GlanceAppWidget() {

    // Sizing Guide: Define responsive breakpoints to adapt layout
    // See: Widgets/sizing.md
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp), // 2x1 (approx)
            DpSize(250.dp, 100.dp), // 3x1 / 4x1
            DpSize(250.dp, 200.dp)  // 3x2 / 4x2 + (Standard List)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, UpNextWidgetEntryPoint::class.java)
        val dao = entryPoint.libraryDao()

        // Data Logic: Segregated for clarity
        // Prioritize items that are actually "Next" to watch
        val dataWithBitmaps = try {
            val allUpNext = dao.getUpNext().take(50)
            val sortedData = allUpNext.sortedWith(
                compareByDescending<LibraryEntryEntity> { entry ->
                    val nextEp = entry.progress + 1
                    val total = entry.totalEpisodes
                    val hasNextEpisode = (total == null || nextEp <= total) &&
                            (entry.nextAiringEpisode == null || nextEp < entry.nextAiringEpisode)
                    if (hasNextEpisode) 1 else 0
                }.thenByDescending { it.lastUpdated }
            ).take(10)

            // Parallel image fetching
            coroutineScope {
                sortedData.map { entry ->
                    async {
                        val bitmap = WidgetImageUtils.loadBitmap(appContext, entry.coverUrl, width = 150, height = 220)
                        entry to bitmap
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                UpNextWidgetContent(dataWithBitmaps)
            }
        }
    }
}

@Composable
fun UpNextWidgetContent(data: List<Pair<LibraryEntryEntity, Bitmap?>>) {
    // Style Guide: Use widgetBackground and appWidgetBackground()
    // appWidgetBackground() ensures the system corner radius is applied (Android S+)
    // See: Widgets/style.md -> Shape
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp)
    ) {
        val size = LocalSize.current

        // Layout Guide: Hide header if height is restricted (e.g. 2x1 widget) to focus on content
        val showHeader = size.height >= 120.dp

        if (showHeader) {
            WidgetHeader()
            Spacer(modifier = GlanceModifier.height(12.dp))
        }

        if (data.isEmpty()) {
            EmptyState()
        } else {
            // Layout Guide: "Lists" canonical layout
            // See: Widgets/layouts.md
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                items(data) { (entry, bitmap) ->
                    UpNextItem(entry, bitmap)
                    Spacer(modifier = GlanceModifier.height(12.dp)) // Good vertical rhythm
                }
            }
        }
    }
}

@Composable
fun WidgetHeader() {
    // Discovery Guide: Keep the widget header consistent
    // Includes App Icon (if space permits) and Title
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon / Logo representation
        Image(
            provider = ImageProvider(R.drawable.ic_launcher_foreground), // Ensure you have a drawable resource
            contentDescription = null,
            modifier = GlanceModifier.size(24.dp)
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = "Up Next",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())

        // Quality Guide: Provide deep link to main app context
        val appIntent = Intent(LocalContext.current, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Quality Guide: Touch Target fix.
        // Wrapped in Box to ensure 48dp clickable area without stretching the icon.
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .clickable(actionStartActivity(appIntent)),
            contentAlignment = Alignment.CenterEnd // Align icon to end
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_add),
                contentDescription = "Open App",
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}

@Composable
fun UpNextItem(entry: LibraryEntryEntity, bitmap: Bitmap?) {
    val totalEp = entry.totalEpisodes
    val totalEpText = if (totalEp != null) "/$totalEp" else ""
    val nextEp = entry.progress + 1

    val detailsIntent = Intent(
        Intent.ACTION_VIEW,
        "anisync://details/${entry.mediaId}".toUri()
    ).apply {
        component = null
        setClass(LocalContext.current, MainActivity::class.java)
    }

    // Style Guide: Surface coloring and rounded corners for items
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(12.dp) // Item radius distinct from widget radius
            .padding(12.dp)
            .clickable(actionStartActivity(detailsIntent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Content Preview: Image
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .height(68.dp)
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.surfaceVariant), // Placeholder color
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

        Spacer(modifier = GlanceModifier.width(16.dp))

        // Typography: Hierarchy (Headline vs Body)
        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            // Updated Typography: increased size and maxLines for readability
            Text(
                text = entry.titleUserPreferred,
                maxLines = 2,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Episode $nextEp",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (totalEpText.isNotEmpty()) {
                    Text(
                        text = totalEpText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        modifier = GlanceModifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    // Quality Guide: WT-1 "Zero and empty states are intentional"
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(android.R.drawable.ic_menu_recent_history), // Use appropriate empty icon
            contentDescription = null,
            colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier.size(48.dp)
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "All caught up!",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = "Add to your library to see updates.",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}