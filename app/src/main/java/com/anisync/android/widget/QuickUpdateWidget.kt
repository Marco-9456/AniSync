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
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.unit.ColorProvider
import com.anisync.android.MainActivity
import com.anisync.android.R
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.entity.LibraryEntryEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QuickUpdateWidgetEntryPoint {
    fun libraryDao(): LibraryDao
}

class QuickUpdateWidget : GlanceAppWidget() {

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
        val entryPoint = EntryPointAccessors.fromApplication(appContext, QuickUpdateWidgetEntryPoint::class.java)
        val dao = entryPoint.libraryDao()

        val entry = try {
            dao.getMostRecentWatching()
        } catch (e: Exception) {
            null
        }

        // Load bitmap if entry exists (High Res for Hero Card)
        val bitmap = if (entry != null) {
            WidgetImageUtils.loadBitmap(appContext, entry.coverUrl, width = 600, height = 400)
        } else {
            null
        }

        provideContent {
            GlanceTheme {
                QuickUpdateWidgetContent(entry, bitmap)
            }
        }
    }
}

@Composable
fun QuickUpdateWidgetContent(entry: LibraryEntryEntity?, bitmap: Bitmap?) {
    // Style Guide: System Coherence
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp)
    ) {
        val size = LocalSize.current
        // Hide header on small widgets to prioritize content
        val showHeader = size.height >= 120.dp

        if (showHeader) {
            QuickUpdateHeader()
            Spacer(modifier = GlanceModifier.height(12.dp))
        }

        if (entry != null) {
            QuickUpdateHeroCard(entry, bitmap)
        } else {
            EmptyQuickUpdateState()
        }
    }
}

@Composable
fun QuickUpdateHeader() {
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
            text = "Quick Update",
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
                provider = ImageProvider(android.R.drawable.ic_menu_my_calendar),
                contentDescription = "Open Library",
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}

@Composable
fun QuickUpdateHeroCard(entry: LibraryEntryEntity, bitmap: Bitmap?) {
    val detailsIntent = Intent(
        Intent.ACTION_VIEW,
        "anisync://details/${entry.mediaId}".toUri()
    ).apply {
        component = null
        setClass(LocalContext.current, MainActivity::class.java)
    }

    val openDetailsAction = actionStartActivity(detailsIntent)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surfaceVariant), // Fallback color
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
        }

        // 2. Dark Gradient Overlay for Readability
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.Black.copy(alpha = 0.4f)))
        ) {}

        // 3. Clickable Container for Details (Full Card)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(openDetailsAction)
        ) {}

        // 4. Content Row (Bottom Aligned)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color.Black.copy(alpha = 0.3f))) // Text protection scrim
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Text Info
            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = entry.titleUserPreferred,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )
                Spacer(modifier = GlanceModifier.size(4.dp))
                Text(
                    text = "Episode ${entry.progress} / ${entry.totalEpisodes ?: "?"}",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF80DEEA)), // Cyan 200
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // 5. Increment Button (High Priority Action)
            // Explicit 48dp target size within a visual container
            Box(
                modifier = GlanceModifier
                    .size(56.dp) // Large tap target
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(16.dp)
                    .clickable(
                        actionRunCallback<IncrementEpisodeCallback>(
                            actionParametersOf(IncrementEpisodeCallback.MediaIdKey to entry.mediaId)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+1",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun EmptyQuickUpdateState() {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_media_play),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(48.dp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "No active anime",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = "Start watching to track progress",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        }
    }
}