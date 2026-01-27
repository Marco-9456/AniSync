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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpNextWidgetEntryPoint {
    fun libraryDao(): LibraryDao
}

class UpNextWidget : GlanceAppWidget() {

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
            UpNextWidgetEntryPoint::class.java
        )
        val dao = entryPoint.libraryDao()

        // Fetch prioritized list
        val entries = withContext(Dispatchers.IO) {
            try {
                val allUpNext = dao.getUpNext().take(50)
                allUpNext.sortedWith(
                    compareByDescending<LibraryEntryEntity> { entry ->
                        val nextEp = entry.progress + 1
                        val total = entry.totalEpisodes
                        val hasNextEpisode = (total == null || nextEp <= total) &&
                                (entry.nextAiringEpisode == null || nextEp < entry.nextAiringEpisode)
                        if (hasNextEpisode) 1 else 0
                    }.thenByDescending { it.lastUpdated }
                ).take(10)
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Pre-load images for all entries - we'll decide whether to use them based on size inside provideContent
        // But since we can't call suspend functions inside provideContent, we load them here
        val entriesWithImages = coroutineScope {
            entries.map { entry ->
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
                val size = LocalSize.current

                when {
                    size.height <= 110.dp -> UpNextCompact(entries)
                    size.height <= 120.dp -> UpNextMedium(entries)
                    else -> UpNextExpanded(entriesWithImages)
                }
            }
        }
    }
}

@Composable
private fun UpNextCompact(entries: List<LibraryEntryEntity>) {
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateCompact()
        return
    }

    val entry = entries.first()
    val nextEp = entry.progress + 1
    val detailsIntent = createDetailsIntent(context, entry.mediaId)

    // TODO: Implement adaptive streaming service icons
    // Replace generic play icon with user's preferred streaming platform (Crunchyroll, Netflix, etc.)
    // Fetch preference from DataStore, resolve drawable resource per service, fallback to ic_media_play if unset/unavailable
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact Cover - Color placeholder with episode number
        Box(
            modifier = GlanceModifier
                .width(40.dp)
                .height(60.dp)
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$nextEp",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = "Up Next",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                text = "Episode $nextEp",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Play Action - 48dp touch target
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(actionStartActivity(detailsIntent)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_media_play),
                contentDescription = "Watch Episode $nextEp",
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary),
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun UpNextMedium(entries: List<LibraryEntryEntity>) {
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateMedium()
        return
    }

    val entry = entries.first()
    val nextEp = entry.progress + 1
    val detailsIntent = createDetailsIntent(context, entry.mediaId)
    val totalEp = entry.totalEpisodes
    val progressPercent = if (totalEp != null && totalEp > 0) {
        (entry.progress.toFloat() / totalEp.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Larger cover area with episode badge
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .height(68.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Use medium opacity color for "EP" label
                Text(
                    text = "EP",
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(0xFFFFFFFF).copy(alpha = 0.7f),
                            night = Color(0xFFFFFFFF).copy(alpha = 0.7f)
                        ),
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "$nextEp",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

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

            if (totalEp != null) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                // Progress bar
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .cornerRadius(2.dp)
                        .background(GlanceTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val fillWidth = (progressPercent * 100).toInt().dp
                    if (progressPercent > 0) {
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
                Text(
                    text = "${entry.progress}/$totalEp watched",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            } else {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "${entry.progress} episodes watched",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Action Column: Play + More
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Primary: Watch next
            Box(
                modifier = GlanceModifier
                    .size(56.dp, 48.dp)
                    .cornerRadius(16.dp)
                    .background(GlanceTheme.colors.primary)
                    .clickable(actionStartActivity(detailsIntent)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_media_play),
                    contentDescription = "Play",
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    modifier = GlanceModifier.size(24.dp)
                )
            }

            if (entries.size > 1) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                // Secondary: Show count of remaining
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .cornerRadius(12.dp)
                        .background(GlanceTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${entries.size - 1}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun UpNextExpanded(entriesWithImages: List<Pair<LibraryEntryEntity, Bitmap?>>) {
    if (entriesWithImages.isEmpty()) {
        EmptyStateExpanded()
        return
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp)
    ) {
        // Header
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
                text = "Up Next",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            if (entriesWithImages.size > 1) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(12.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${entriesWithImages.size} items",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // List
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(entriesWithImages) { (entry, bitmap) ->
                UpNextListItem(entry, bitmap)
                Spacer(modifier = GlanceModifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun UpNextListItem(entry: LibraryEntryEntity, bitmap: Bitmap?) {
    val context = LocalContext.current
    val nextEp = entry.progress + 1
    val detailsIntent = createDetailsIntent(context, entry.mediaId)
    val totalEp = entry.totalEpisodes?.toString() ?: "?"

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(detailsIntent))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover Image or Placeholder
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .height(68.dp)
                .cornerRadius(12.dp)
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
                // Fallback number
                Text(
                    text = "$nextEp",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(16.dp))

        // Content
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
                maxLines = 2
            )
            Spacer(modifier = GlanceModifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Episode badge
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EP $nextEp",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                if (entry.totalEpisodes != null) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "of $totalEp",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Play button
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(actionStartActivity(detailsIntent)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_media_play),
                contentDescription = "Watch",
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary),
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}

/**
 * EMPTY STATES
 */
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
            provider = ImageProvider(android.R.drawable.ic_menu_close_clear_cancel),
            contentDescription = "All caught up",
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
                .background(GlanceTheme.colors.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_recent_history),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                modifier = GlanceModifier.size(24.dp)
            )
        }

        Spacer(modifier = GlanceModifier.width(16.dp))

        Column {
            Text(
                text = "All Caught Up!",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "You're up to date with your watchlist",
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
                    .background(GlanceTheme.colors.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_menu_recent_history),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                    modifier = GlanceModifier.size(40.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(16.dp))

            Text(
                text = "You're All Caught Up!",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Check back later for new episodes",
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