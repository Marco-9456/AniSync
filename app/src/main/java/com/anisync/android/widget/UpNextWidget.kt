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
import com.anisync.android.data.AppSettings
import com.anisync.android.data.StreamingService
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.domain.ExternalLinkType
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
    fun mediaDetailsDao(): MediaDetailsDao
    fun appSettings(): AppSettings
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
        val mediaDetailsDao = entryPoint.mediaDetailsDao()
        val appSettings = entryPoint.appSettings()

        // Get the preferred streaming service
        val streamingService = appSettings.preferredStreamingService.value

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

        // Load streaming service icon from URL (with fallback)
        val streamingIconBitmap = loadStreamingServiceIcon(appContext, streamingService)

        // Fetch streaming URLs for each entry from cached media details
        val streamingUrls = withContext(Dispatchers.IO) {
            entries.associate { entry ->
                val url = findStreamingUrl(mediaDetailsDao, entry.mediaId, streamingService)
                entry.mediaId to url
            }
        }

        // Pre-load cover images for all entries
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
                    size.height <= 110.dp -> UpNextCompact(entries, streamingService, streamingIconBitmap, streamingUrls)
                    size.height <= 120.dp -> UpNextMedium(entries, streamingService, streamingIconBitmap, streamingUrls)
                    else -> UpNextExpanded(entriesWithImages, streamingService, streamingIconBitmap, streamingUrls)
                }
            }
        }
    }

    /**
     * Load the streaming service icon from its URL.
     * Falls back to null if loading fails (composables will use fallback drawable).
     */
    private suspend fun loadStreamingServiceIcon(
        context: Context,
        service: StreamingService
    ): Bitmap? {
        if (service == StreamingService.NONE || service.iconUrl == null) {
            return null
        }
        
        return try {
            WidgetImageUtils.loadBitmap(
                context,
                service.iconUrl,
                width = 64,
                height = 64
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Find the streaming URL for a media entry that matches the user's preferred streaming service.
     * Returns null if no matching streaming link is found.
     */
    private suspend fun findStreamingUrl(
        mediaDetailsDao: MediaDetailsDao,
        mediaId: Int,
        preferredService: StreamingService
    ): String? {
        if (preferredService == StreamingService.NONE) {
            return null
        }
        
        val mediaDetails = mediaDetailsDao.getById(mediaId) ?: return null
        val externalLinks = mediaDetails.externalLinks
        
        // Find a streaming link that matches the preferred service name
        // The site name in external links should match the display name (e.g., "Crunchyroll", "Netflix")
        return externalLinks
            .filter { it.type == ExternalLinkType.STREAMING && it.url != null }
            .find { link -> 
                link.site.equals(preferredService.displayName, ignoreCase = true) ||
                // Handle slight variations (e.g., "Amazon Prime Video" vs "Prime Video")
                link.site.contains(preferredService.displayName.split(" ").first(), ignoreCase = true)
            }
            ?.url
    }
}

/**
 * Creates an ImageProvider for the streaming service icon.
 * Uses the loaded bitmap if available, otherwise falls back to the service's fallback drawable.
 */
@Composable
private fun getStreamingIconProvider(
    service: StreamingService,
    iconBitmap: Bitmap?
): ImageProvider {
    return if (iconBitmap != null) {
        ImageProvider(iconBitmap)
    } else {
        ImageProvider(service.fallbackDrawable)
    }
}

@Composable
private fun UpNextCompact(
    entries: List<LibraryEntryEntity>,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?,
    streamingUrls: Map<Int, String?>
) {
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateCompact()
        return
    }

    val entry = entries.first()
    val nextEp = entry.progress + 1
    val streamingUrl = streamingUrls[entry.mediaId]
    val playIntent = createStreamingOrDetailsIntent(context, streamingUrl, entry.mediaId)
    val detailsIntent = createDetailsIntent(context, entry.mediaId)
    val iconProvider = getStreamingIconProvider(streamingService, streamingIconBitmap)

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

        // Play Action - 48dp touch target with streaming service icon
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(actionStartActivity(playIntent)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = iconProvider,
                contentDescription = "Watch Episode $nextEp on ${streamingService.displayName}",
                colorFilter = if (streamingIconBitmap == null) {
                    androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary)
                } else null,
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun UpNextMedium(
    entries: List<LibraryEntryEntity>,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?,
    streamingUrls: Map<Int, String?>
) {
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateMedium()
        return
    }

    val entry = entries.first()
    val nextEp = entry.progress + 1
    val streamingUrl = streamingUrls[entry.mediaId]
    val playIntent = createStreamingOrDetailsIntent(context, streamingUrl, entry.mediaId)
    val totalEp = entry.totalEpisodes
    val progressPercent = if (totalEp != null && totalEp > 0) {
        (entry.progress.toFloat() / totalEp.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val iconProvider = getStreamingIconProvider(streamingService, streamingIconBitmap)

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
            // Primary: Watch next with streaming service icon
            Box(
                modifier = GlanceModifier
                    .size(56.dp, 48.dp)
                    .cornerRadius(16.dp)
                    .background(GlanceTheme.colors.primary)
                    .clickable(actionStartActivity(playIntent)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = iconProvider,
                    contentDescription = "Play",
                    colorFilter = if (streamingIconBitmap == null) {
                        androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary)
                    } else null,
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
private fun UpNextExpanded(
    entriesWithImages: List<Pair<LibraryEntryEntity, Bitmap?>>,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?,
    streamingUrls: Map<Int, String?>
) {
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
                UpNextListItem(entry, bitmap, streamingService, streamingIconBitmap, streamingUrls)
                Spacer(modifier = GlanceModifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun UpNextListItem(
    entry: LibraryEntryEntity,
    bitmap: Bitmap?,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?,
    streamingUrls: Map<Int, String?>
) {
    val context = LocalContext.current
    val nextEp = entry.progress + 1
    val streamingUrl = streamingUrls[entry.mediaId]
    val playIntent = createStreamingOrDetailsIntent(context, streamingUrl, entry.mediaId)
    val detailsIntent = createDetailsIntent(context, entry.mediaId)
    val totalEp = entry.totalEpisodes?.toString() ?: "?"
    val iconProvider = getStreamingIconProvider(streamingService, streamingIconBitmap)

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

        // Play button with streaming service icon
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(actionStartActivity(playIntent)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = iconProvider,
                contentDescription = "Watch",
                colorFilter = if (streamingIconBitmap == null) {
                    androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary)
                } else null,
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

/**
 * Create an intent to open the streaming URL if available, otherwise open the details page.
 * @param streamingUrl The URL to the streaming service (e.g., Crunchyroll page for the anime)
 * @param mediaId The media ID to fall back to if no streaming URL is available
 */
private fun createStreamingOrDetailsIntent(context: Context, streamingUrl: String?, mediaId: Int): Intent {
    return if (streamingUrl != null) {
        // Open the streaming service URL in browser/app
        Intent(Intent.ACTION_VIEW, streamingUrl.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    } else {
        // Fall back to opening the details page in the app
        createDetailsIntent(context, mediaId)
    }
}

private fun openMainAppIntent(): Intent {
    return Intent(Intent.ACTION_MAIN).apply {
        setClassName("com.anisync.android", "com.anisync.android.MainActivity")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
