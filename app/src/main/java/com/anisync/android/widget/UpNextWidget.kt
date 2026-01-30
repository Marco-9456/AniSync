package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.widget.RemoteViews
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
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.anisync.android.data.AppSettings
import com.anisync.android.data.StreamingService
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.domain.ExternalLinkType
import com.anisync.android.widget.core.SizeClass
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntentUtils
import com.anisync.android.widget.core.toSizeClass
import com.anisync.android.widget.designsystem.components.WidgetProgressBar
import com.anisync.android.widget.designsystem.tokens.WidgetDimensions
import com.anisync.android.widget.designsystem.tokens.WidgetTypography
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.core.graphics.toColorInt

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpNextWidgetEntryPoint {
    fun libraryDao(): LibraryDao
    fun mediaDetailsDao(): MediaDetailsDao
    fun airingScheduleDao(): AiringScheduleDao // Added for airing times
    fun appSettings(): AppSettings
}

class UpNextWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 100.dp),
            DpSize(250.dp, 100.dp),
            DpSize(250.dp, 300.dp)
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
        val airingScheduleDao = entryPoint.airingScheduleDao()
        val appSettings = entryPoint.appSettings()

        val streamingService = appSettings.getPreferredStreamingServiceDirect()

        // Fetch airing schedules for a longer time range (30 days) to capture all upcoming episodes
        val allAiringSchedules = withContext(Dispatchers.IO) {
            try {
                val currentTimeSeconds = System.currentTimeMillis() / 1000
                airingScheduleDao.getAiringBetweenForUser(
                    currentTimeSeconds,
                    currentTimeSeconds + 30L * 24 * 60 * 60 // 30 days ahead
                )
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Get entries and filter to only currently airing shows (RELEASING status)
        // Also include null mediaStatus for backwards compatibility with cached data before refresh
        val entries = withContext(Dispatchers.IO) {
            try {
                dao.getUpNext()
                    // Filter to shows that are currently airing (or unknown status for legacy data)
                    .filter { it.mediaStatus == null || it.mediaStatus == "RELEASING" }
                    .take(10)
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Build airing times map for entries
        val airingTimes = entries.associate { entry ->
            val nextEpNumber = entry.progress + 1
            // Find the next episode that user needs to watch (their progress + 1)
            val schedule = allAiringSchedules.find { 
                it.mediaId == entry.mediaId && it.episode >= nextEpNumber 
            }
            entry.mediaId to (schedule?.airingAt ?: 0L)
        }
        
        // Sort entries by airing time (soonest first), entries with no airing time go last
        val sortedEntries = entries.sortedWith(
            compareBy<LibraryEntryEntity> { airingTimes[it.mediaId] == 0L }
                .thenBy { airingTimes[it.mediaId] ?: Long.MAX_VALUE }
        )

        val streamingIconBitmap = loadStreamingServiceIcon(appContext, streamingService)

        val streamingUrls = withContext(Dispatchers.IO) {
            sortedEntries.associate { entry ->
                val url = findStreamingUrl(mediaDetailsDao, entry.mediaId, streamingService)
                entry.mediaId to url
            }
        }

        val entriesWithImages = coroutineScope {
            sortedEntries.map { entry ->
                async {
                    val bitmap = WidgetImageLoader.loadBitmap(
                        appContext,
                        entry.coverUrl,
                        width = 300,
                        height = 450
                    )
                    entry to bitmap
                }
            }.awaitAll()
        }

        provideContent {
            GlanceTheme {
                val sizeClass = LocalSize.current.toSizeClass()

                when (sizeClass) {
                    SizeClass.COMPACT -> UpNextCompact(
                        sortedEntries,
                        streamingService,
                        streamingIconBitmap,
                        streamingUrls,
                        airingTimes
                    )

                    SizeClass.MEDIUM -> UpNextMedium(
                        sortedEntries,
                        streamingService,
                        streamingIconBitmap,
                        streamingUrls,
                        airingTimes
                    )

                    SizeClass.EXPANDED -> UpNextExpanded(
                        entriesWithImages,
                        streamingService,
                        streamingIconBitmap,
                        streamingUrls,
                        airingTimes
                    )
                }
            }
        }
    }

    private suspend fun loadStreamingServiceIcon(
        context: Context,
        service: StreamingService
    ): Bitmap? {
        if (service == StreamingService.NONE || service.iconUrl == null) return null
        return try {
            // Skip cache to ensure fresh icon when streaming service changes
            WidgetImageLoader.loadBitmap(context, service.iconUrl, width = 64, height = 64, skipCache = true)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun findStreamingUrl(
        mediaDetailsDao: MediaDetailsDao,
        mediaId: Int,
        preferredService: StreamingService
    ): String? {
        if (preferredService == StreamingService.NONE) return null
        val mediaDetails = mediaDetailsDao.getById(mediaId) ?: return null
        return mediaDetails.externalLinks
            .filter { it.type == ExternalLinkType.STREAMING && it.url != null }
            .find { link ->
                link.site.equals(preferredService.displayName, ignoreCase = true) ||
                        link.site.contains(
                            preferredService.displayName.split(" ").first(),
                            ignoreCase = true
                        )
            }?.url
    }
}

@Composable
private fun UpNextCompact(
    entries: List<LibraryEntryEntity>,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?,
    streamingUrls: Map<Int, String?>,
    airingTimes: Map<Int, Long>
) {
    val context = LocalContext.current
    if (entries.isEmpty()) {
        EmptyStateCompact()
        return
    }

    val entry = entries.first()
    val nextEp = entry.progress + 1
    val streamingUrl = streamingUrls[entry.mediaId]
    val intent = createStreamingOrDetailsIntent(context, streamingUrl, entry.mediaId)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(12.dp)
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

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "Up Next",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
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
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                maxLines = 1
            )
        }

        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center
        ) {
            getStreamingIconProvider(streamingService, streamingIconBitmap).let { (provider, isFallback) ->
                Image(
                    provider = provider,
                    contentDescription = "Watch",
                    modifier = GlanceModifier.size(24.dp),
                    colorFilter = if (isFallback) androidx.glance.ColorFilter.tint(
                        GlanceTheme.colors.onPrimary
                    ) else null
                )
            }
        }
    }
}

@Composable
private fun UpNextMedium(
    entries: List<LibraryEntryEntity>,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?,
    streamingUrls: Map<Int, String?>,
    airingTimes: Map<Int, Long>
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
    val progressPercent =
        if (totalEp != null && totalEp > 0) entry.progress.toFloat() / totalEp else 0f

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .fillMaxHeight()
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "EP",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
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

        Column(modifier = GlanceModifier.defaultWeight()) {
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
                WidgetProgressBar(
                    progress = progressPercent,
                    height = WidgetDimensions.progressBarHeight
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "${entry.progress}/$totalEp watched",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = GlanceModifier.size(56.dp, 48.dp).cornerRadius(16.dp)
                    .background(GlanceTheme.colors.primary)
                    .clickable(actionStartActivity(playIntent)),
                contentAlignment = Alignment.Center
            ) {
                getStreamingIconProvider(streamingService, streamingIconBitmap).let { (provider, isFallback) ->
                    Image(
                        provider = provider,
                        contentDescription = "Play",
                        modifier = GlanceModifier.size(24.dp),
                        colorFilter = if (isFallback) androidx.glance.ColorFilter.tint(
                            GlanceTheme.colors.onPrimary
                        ) else null
                    )
                }
            }
            if (entries.size > 1) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Box(
                    modifier = GlanceModifier.size(48.dp).cornerRadius(12.dp)
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
    streamingUrls: Map<Int, String?>,
    airingTimes: Map<Int, Long>
) {
    if (entriesWithImages.isEmpty()) {
        EmptyStateExpanded()
        return
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_recent_history),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(24.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "Up Next",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = GlanceModifier.defaultWeight()
            )
        }

        Spacer(modifier = GlanceModifier.height(16.dp))

        LazyColumn(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            itemsIndexed(entriesWithImages) { index: Int, pair: Pair<LibraryEntryEntity, Bitmap?> ->
                val entry = pair.first
                val bitmap = pair.second
                val isHero = index == 0
                // Wrap card in Column with bottom padding for spacing
                Column(
                    modifier = GlanceModifier.fillMaxWidth().padding(
                        bottom = if (index < entriesWithImages.lastIndex) 12.dp else 0.dp
                    )
                ) {
                    CountdownCard(
                        entry = entry,
                        bitmap = bitmap,
                        isHero = isHero,
                        streamingService = streamingService,
                        streamingIconBitmap = streamingIconBitmap,
                        streamingUrl = streamingUrls[entry.mediaId],
                        airingTime = airingTimes[entry.mediaId] ?: 0L
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownCard(
    entry: LibraryEntryEntity,
    bitmap: Bitmap?,
    isHero: Boolean,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?,
    streamingUrl: String?,
    airingTime: Long // Actual Unix timestamp in seconds
) {
    val context = LocalContext.current
    val detailsIntent = createDetailsIntent(context, entry.mediaId)
    val playIntent = createStreamingOrDetailsIntent(context, streamingUrl, entry.mediaId)

    val nextEp = entry.progress + 1
    val currentTime = System.currentTimeMillis() / 1000L
    val diffSeconds = airingTime - currentTime

    // Hero uses dark surfaceVariant for better contrast with timer
    val posterWidth = if (isHero) 80.dp else 60.dp
    val posterHeight = if (isHero) 120.dp else 90.dp
    val titleSize = if (isHero) 18.sp else 16.sp
    val titleWeight = if (isHero) FontWeight.Bold else FontWeight.Medium
    val timerSize = if (isHero) 22.sp else 18.sp

    // Text colors: both use standard colors since both have dark backgrounds
    val titleColor = GlanceTheme.colors.onSurface
    val secondaryTextColor = GlanceTheme.colors.onSurfaceVariant

    val badgeBackground =
        if (isHero) GlanceTheme.colors.primary else GlanceTheme.colors.tertiaryContainer
    val badgeTextColor =
        if (isHero) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onTertiaryContainer

    // Both hero and non-hero cards use dark background for consistency
    val cardModifier = if (isHero) {
        GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(16.dp)
    } else {
        GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(16.dp)
    }

    val padding = if (isHero) 16.dp else 12.dp
    val crButtonSize = if (isHero) 44.dp else 36.dp
    val crIconSize = if (isHero) 24.dp else 20.dp

    // FIX: Wrap in Box for hero border effect
    Box(
        modifier = if (isHero) {
            GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.primary)
                .cornerRadius(18.dp)
                .padding(2.dp)
        } else GlanceModifier.fillMaxWidth()
    ) {
        Row(
            modifier = cardModifier
                .clickable(actionStartActivity(detailsIntent))
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(posterWidth)
                    .height(posterHeight)
                    .cornerRadius(8.dp)
                    .background(GlanceTheme.colors.surface),
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

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.titleUserPreferred,
                    style = TextStyle(
                        color = titleColor,
                        fontSize = titleSize,
                        fontWeight = titleWeight
                    ),
                    maxLines = if (isHero) 2 else 1
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                Box(
                    modifier = GlanceModifier
                        .background(badgeBackground)
                        .cornerRadius(99.dp)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EPISODE $nextEp",
                        style = TextStyle(
                            color = badgeTextColor,
                            fontSize = if (isHero) 12.sp else 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // FIX: Check if airingTime is valid (greater than 0)
                if (airingTime > 0 && diffSeconds > 0) {
                    val days = TimeUnit.SECONDS.toDays(diffSeconds)
                    val hours = TimeUnit.SECONDS.toHours(diffSeconds) % 24
                    val minutes = TimeUnit.SECONDS.toMinutes(diffSeconds) % 60
                    val seconds = diffSeconds % 60

                    if (days > 0) {
                        // Show compact format: "1d 16h 57m" without redundant labels
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${days}d ${hours}h ${minutes}m",
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = timerSize,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        // Use real-time Chronometer for HH:MM:SS countdown (within 24 hours)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CountdownChronometer(
                                targetEpochSeconds = airingTime
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = "LEFT",
                                style = TextStyle(
                                    color = secondaryTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                } else if (airingTime > 0) {
                    // Actually airing now or aired recently
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp)
                                .background(GlanceTheme.colors.tertiary)
                        ) {}
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = "AIRING NOW",
                            style = TextStyle(
                                color = GlanceTheme.colors.tertiary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    // No airing time available
                    Text(
                        text = "Coming Soon",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Box(
                modifier = GlanceModifier
                    .size(crButtonSize)
                    .cornerRadius(if (isHero) 22.dp else 18.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .clickable(actionStartActivity(playIntent)),
                contentAlignment = Alignment.Center
            ) {
                getStreamingIconProvider(
                    streamingService,
                    streamingIconBitmap
                ).let { (provider, isFallback) ->
                    Image(
                        provider = provider,
                        contentDescription = "Watch",
                        modifier = GlanceModifier.size(crIconSize),
                        colorFilter = if (isFallback) androidx.glance.ColorFilter.tint(
                            GlanceTheme.colors.primary
                        ) else null
                    )
                }
            }
        }
    }
}

/**
 * Returns a pair of (ImageProvider, isFallback) where isFallback indicates
 * whether the fallback drawable is being used (which should be tinted).
 */
@Composable
private fun getStreamingIconProvider(
    service: StreamingService,
    iconBitmap: Bitmap?
): Pair<ImageProvider, Boolean> {
    return if (iconBitmap != null) {
        Pair(ImageProvider(iconBitmap), false)
    } else {
        Pair(ImageProvider(service.fallbackDrawable), true)
    }
}

@Composable
private fun EmptyStateCompact() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(openMainAppIntent(context))),
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
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity(openMainAppIntent(context))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.size(48.dp).cornerRadius(12.dp)
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
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun EmptyStateExpanded() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(GlanceTheme.colors.surface).cornerRadius(28.dp)
            .clickable(actionStartActivity(openMainAppIntent(context))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = GlanceModifier.size(72.dp).cornerRadius(24.dp)
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
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
                modifier = GlanceModifier.padding(horizontal = 24.dp)
            )
        }
    }
}

private fun createDetailsIntent(context: Context, mediaId: Int) =
    WidgetIntentUtils.createDetailsIntent(context, mediaId)

private fun createStreamingOrDetailsIntent(
    context: Context,
    streamingUrl: String?,
    mediaId: Int
): Intent {
    return if (streamingUrl != null) {
        Intent(Intent.ACTION_VIEW, streamingUrl.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    } else {
        createDetailsIntent(context, mediaId)
    }
}

private fun openMainAppIntent(context: Context): Intent {
    // Use explicit intent with context to correctly target debug/release builds
    return Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}

/**
 * A Glance composable that wraps Android's native Chronometer widget
 * to display a real-time countdown that updates every second.
 *
 * @param targetEpochSeconds The target time in Unix seconds (epoch time)
 */
@Composable
private fun CountdownChronometer(
    targetEpochSeconds: Long,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    
    // Calculate the base time for Chronometer
    // Chronometer uses SystemClock.elapsedRealtime() as reference
    val elapsedNow = SystemClock.elapsedRealtime()
    val currentTimeMs = System.currentTimeMillis()
    val targetTimeMs = targetEpochSeconds * 1000
    val diffMs = targetTimeMs - currentTimeMs
    val base = elapsedNow + diffMs
    
    // Detect dark mode for theme-aware color
    val isDarkMode = (context.resources.configuration.uiMode and 
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    
    // Use M3 primary color that contrasts well with surfaceVariant background
    val textColor = if (isDarkMode) {
        "#D0BCFF".toColorInt() // M3 Primary (dark theme)
    } else {
        "#6750A4".toColorInt() // M3 Primary (light theme)
    }
    
    // Create RemoteViews with the Chronometer
    val remoteViews = RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
        setChronometer(R.id.countdown_timer, base, null, true)
        setTextColor(R.id.countdown_timer, textColor)
    }
    
    AndroidRemoteViews(remoteViews, modifier)
}