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
import androidx.glance.color.ColorProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QuickUpdateWidgetEntryPoint {
    fun libraryDao(): LibraryDao
}

class QuickUpdateWidget : GlanceAppWidget() {

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
            QuickUpdateWidgetEntryPoint::class.java
        )
        val dao = entryPoint.libraryDao()

        val entry = withContext(Dispatchers.IO) {
            try {
                dao.getMostRecentWatching()
            } catch (e: Exception) {
                null
            }
        }

        val heroBitmap = if (entry != null) {
            WidgetImageUtils.loadBitmap(
                context = appContext,
                url = entry.coverUrl,
                width = 600,
                height = 800
            )
        } else null

        provideContent {
            GlanceTheme {
                val size = LocalSize.current

                when {
                    size.height <= 110.dp -> {
                        QuickUpdateCompact(entry)
                    }
                    size.height <= 120.dp -> {
                        QuickUpdateMedium(entry)
                    }
                    else -> {
                        QuickUpdateExpanded(entry, heroBitmap)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickUpdateCompact(entry: LibraryEntryEntity?) {
    val context = LocalContext.current

    if (entry == null) {
        EmptyStateCompact()
        return
    }

    val detailsIntent = createDetailsIntent(context, entry.mediaId)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(40.dp)
                .height(60.dp)
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder - no nested fillMaxSize with background
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            val totalText = entry.totalEpisodes?.toString() ?: "?"
            Text(
                text = "Ep ${entry.progress + 1} / $totalText",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .clickable(
                    actionRunCallback<IncrementEpisodeCallback>(
                        actionParametersOf(
                            IncrementEpisodeCallback.MediaIdKey to entry.mediaId
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+1",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun QuickUpdateMedium(entry: LibraryEntryEntity?) {
    val context = LocalContext.current

    if (entry == null) {
        EmptyStateMedium()
        return
    }

    val detailsIntent = createDetailsIntent(context, entry.mediaId)
    val progressPercent = calculateProgressPercent(entry)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .height(68.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {}

        Spacer(modifier = GlanceModifier.width(16.dp))

        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Progress track - Fixed height container with manual positioning
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .cornerRadius(2.dp)
                    .background(GlanceTheme.colors.surfaceVariant),
                contentAlignment = Alignment.CenterStart
            ) {
                // Progress fill - fixed width calculation based on percentage
                val fillWidth = (progressPercent * 100).toInt().dp
                if (progressPercent > 0f) {
                    Box(
                        modifier = GlanceModifier
                            .width(fillWidth)
                            .height(4.dp)
                            .cornerRadius(2.dp)
                            .background(GlanceTheme.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next: Episode ${entry.progress + 1}",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (entry.totalEpisodes != null) {
                    Text(
                        text = "${entry.progress}/${entry.totalEpisodes}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.width(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = GlanceModifier
                    .size(56.dp, 48.dp)
                    .cornerRadius(16.dp)
                    .background(GlanceTheme.colors.primary)
                    .clickable(
                        actionRunCallback<IncrementEpisodeCallback>(
                            actionParametersOf(
                                IncrementEpisodeCallback.MediaIdKey to entry.mediaId
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+1",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Box(
                modifier = GlanceModifier
                    .size(48.dp)
                    .cornerRadius(12.dp)
                    .clickable(actionStartActivity(detailsIntent)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_menu_info_details),
                    contentDescription = "View Details",
                    colorFilter = androidx.glance.ColorFilter.tint(
                        GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = GlanceModifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickUpdateExpanded(entry: LibraryEntryEntity?, bitmap: Bitmap?) {
    val context = LocalContext.current

    if (entry == null) {
        EmptyStateExpanded()
        return
    }

    val detailsIntent = createDetailsIntent(context, entry.mediaId)
    val progressPercent = calculateProgressPercent(entry)

    // Define colors for expanded mode with proper transparency using Color with alpha
    val whiteColor = Color.White
    val white70 = Color.White.copy(alpha = 0.7f)
    val white90 = Color.White.copy(alpha = 0.9f)
    val white95 = Color.White.copy(alpha = 0.95f)
    val white30 = Color.White.copy(alpha = 0.3f)
    val black60 = Color.Black.copy(alpha = 0.6f)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(28.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        // Background Layer
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
                    .background(GlanceTheme.colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {}
        }

        // Scrim Layer
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(
                    ColorProvider(day = black60, night = black60)
                ),
            contentAlignment = Alignment.Center
        ) {}

        // Clickable overlay
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(detailsIntent)),
            contentAlignment = Alignment.Center
        ) {}

        // Content
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Progress Section
            if (entry.totalEpisodes != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    // Background track
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(6.dp)
                            .cornerRadius(3.dp)
                            .background(
                                ColorProvider(day = white30, night = white30)
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Progress fill - manual width calculation
                        val fillWidth = (progressPercent * 100).toInt().dp
                        if (progressPercent > 0) {
                            Box(
                                modifier = GlanceModifier
                                    .width(fillWidth)
                                    .height(6.dp)
                                    .cornerRadius(3.dp)
                                    .background(ColorProvider(day = whiteColor, night = whiteColor)),
                                contentAlignment = Alignment.Center
                            ) {}
                        }
                    }
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "${(progressPercent * 100).roundToInt()}%",
                        style = TextStyle(
                            color = ColorProvider(day = whiteColor, night = whiteColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(12.dp))
            }

            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(
                    color = ColorProvider(day = whiteColor, night = whiteColor),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                ),
                maxLines = 2
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = "Episode ${entry.progress + 1}${
                    entry.totalEpisodes?.let { " of $it" } ?: ""
                }",
                style = TextStyle(
                    color = ColorProvider(day = white90, night = white90),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(16.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to open details",
                    style = TextStyle(
                        color = ColorProvider(day = white70, night = white70),
                        fontSize = 12.sp
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // FAB-style button
                Box(
                    modifier = GlanceModifier
                        .size(72.dp, 56.dp)
                        .cornerRadius(28.dp)
                        .background(ColorProvider(day = white95, night = white95))
                        .clickable(
                            actionRunCallback<IncrementEpisodeCallback>(
                                actionParametersOf(
                                    IncrementEpisodeCallback.MediaIdKey to entry.mediaId
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(android.R.drawable.ic_input_add),
                            contentDescription = null,
                            colorFilter = androidx.glance.ColorFilter.tint(
                                GlanceTheme.colors.primary
                            ),
                            modifier = GlanceModifier.size(20.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = "EP",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCompact() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(openMainAppIntent())),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(android.R.drawable.ic_media_play),
            contentDescription = "Start watching",
            colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(32.dp)
        )
    }
}

@Composable
private fun EmptyStateMedium() {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity(openMainAppIntent()))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_input_add),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(24.dp)
            )
        }

        Spacer(modifier = GlanceModifier.width(16.dp))

        Column {
            Text(
                text = "Quick Update",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Add anime to your library",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp
                ),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun EmptyStateExpanded() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(28.dp)
            .clickable(actionStartActivity(openMainAppIntent())),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = GlanceModifier
                    .size(72.dp)
                    .cornerRadius(24.dp)
                    .background(GlanceTheme.colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_media_play),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier.size(40.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(16.dp))

            Text(
                text = "No Active Series",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Start watching to track progress",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(horizontal = 24.dp)
            )
        }
    }
}

// Helper functions
private fun createDetailsIntent(context: Context, mediaId: Int): Intent {
    return Intent(
        Intent.ACTION_VIEW,
        "anisync://details/$mediaId".toUri()
    ).apply {
        component = null
        setClass(context, MainActivity::class.java)
    }
}

private fun openMainAppIntent(): Intent {
    return Intent(Intent.ACTION_MAIN).apply {
        setClassName("com.anisync.android", "com.anisync.android.MainActivity")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}

private fun calculateProgressPercent(entry: LibraryEntryEntity): Float {
    if (entry.totalEpisodes == null || entry.totalEpisodes == 0) return 0f
    return (entry.progress.toFloat() / entry.totalEpisodes.toFloat()).coerceIn(0f, 1f)
}