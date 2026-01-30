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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpNextWidgetEntryPoint {
    fun libraryDao(): LibraryDao
    fun mediaDetailsDao(): MediaDetailsDao
    fun airingScheduleDao(): AiringScheduleDao // Added for airing times
    fun appSettings(): AppSettings
}

// Helper class to hold calculated display data
data class UpNextDisplayItem(
    val entry: LibraryEntryEntity,
    val displayEpisode: Int,
    val airingTime: Long,
    val bitmap: Bitmap? = null,
    val streamingUrl: String? = null
)

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
                // Start looking from the past (30 days ago) to capture episodes that have already aired
                val startTimeSeconds = currentTimeSeconds - (30 * 24 * 60 * 60)
                airingScheduleDao.getAiringBetweenForUser(
                    startTimeSeconds,
                    currentTimeSeconds + 30L * 24 * 60 * 60 // 30 days ahead
                )
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Get entries and filter to only currently airing shows (RELEASING status)
        val entries = withContext(Dispatchers.IO) {
            try {
                dao.getUpNext()
                    .filter { it.mediaStatus == null || it.mediaStatus == "RELEASING" }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val currentTimeSeconds = System.currentTimeMillis() / 1000

        // Build display items with smart episode logic
        val unsortedDisplayItems = entries.map { entry ->
            val nextEpNumber = entry.progress + 1

            // Try to find schedule for the immediate next episode
            var schedule = allAiringSchedules.find {
                it.mediaId == entry.mediaId && it.episode == nextEpNumber
            }

            var displayEpisode = nextEpNumber
            var airingAt = schedule?.airingAt ?: 0L

            // Logic: If the episode aired more than 30 minutes ago,
            // we assume the user missed it or it's "Available Now" time has passed.
            // We switch to displaying the NEXT episode.
            if (airingAt > 0 && airingAt < (currentTimeSeconds - 1800)) {
                val nextNextEpNumber = nextEpNumber + 1
                schedule = allAiringSchedules.find {
                    it.mediaId == entry.mediaId && it.episode == nextNextEpNumber
                }
                displayEpisode = nextNextEpNumber
                airingAt = schedule?.airingAt ?: 0L
            }

            // Fallback logic for missing schedule data
            if (airingAt == 0L) {
                val nextAiringEp = entry.nextAiringEpisode
                if (nextAiringEp != null) {
                    if (nextAiringEp == displayEpisode) {
                        // This IS the next airing episode, use the cached time
                        airingAt = entry.nextAiringEpisodeTime ?: 0L
                    } else if (nextAiringEp > displayEpisode) {
                        // The next airing episode is FUTURE to this one -> This one is released
                        // Set to a past time (e.g., 1 hour ago) to trigger "Available Now"
                        airingAt = currentTimeSeconds - 3600
                    }
                }
            }

            UpNextDisplayItem(entry, displayEpisode, airingAt)
        }

        // Sort items by airing time (soonest first), items with unknown time go last
        val sortedDisplayItems = unsortedDisplayItems.sortedWith(
            compareBy<UpNextDisplayItem> { it.airingTime == 0L }
                .thenBy { it.airingTime }
        ).take(10)

        val streamingIconBitmap = loadStreamingServiceIcon(appContext, streamingService)

        val streamingUrls = withContext(Dispatchers.IO) {
            sortedDisplayItems.associate { item ->
                val url = findStreamingUrl(mediaDetailsDao, item.entry.mediaId, streamingService)
                item.entry.mediaId to url
            }
        }

        val finalDisplayItems = coroutineScope {
            sortedDisplayItems.map { item ->
                async {
                    val bitmap = WidgetImageLoader.loadBitmap(
                        appContext,
                        item.entry.coverUrl,
                        width = 300,
                        height = 450
                    )
                    item.copy(
                        bitmap = bitmap,
                        streamingUrl = streamingUrls[item.entry.mediaId]
                    )
                }
            }.awaitAll()
        }

        provideContent {
            GlanceTheme {
                val sizeClass = LocalSize.current.toSizeClass()

                when (sizeClass) {
                    SizeClass.COMPACT -> UpNextCompact(
                        finalDisplayItems,
                        streamingService,
                        streamingIconBitmap
                    )

                    SizeClass.MEDIUM -> UpNextMedium(
                        finalDisplayItems,
                        streamingService,
                        streamingIconBitmap
                    )

                    SizeClass.EXPANDED -> UpNextExpanded(
                        finalDisplayItems,
                        streamingService,
                        streamingIconBitmap
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
    items: List<UpNextDisplayItem>,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?
) {
    val context = LocalContext.current
    if (items.isEmpty()) {
        EmptyStateCompact()
        return
    }

    val item = items.first()
    val intent = createStreamingOrDetailsIntent(context, item.streamingUrl, item.entry.mediaId)

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
                text = "${item.displayEpisode}",
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
                text = item.entry.titleUserPreferred,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                text = "Episode ${item.displayEpisode}",
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
    items: List<UpNextDisplayItem>,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?
) {
    val context = LocalContext.current
    if (items.isEmpty()) {
        EmptyStateMedium()
        return
    }

    val item = items.first()
    val playIntent = createStreamingOrDetailsIntent(context, item.streamingUrl, item.entry.mediaId)
    val totalEp = item.entry.totalEpisodes
    // Calculate progress based on user's actual progress, not the displayed episode
    val progressPercent =
        if (totalEp != null && totalEp > 0) item.entry.progress.toFloat() / totalEp else 0f

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
                    text = "${item.displayEpisode}",
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
                text = item.entry.titleUserPreferred,
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
                    text = "${item.entry.progress}/$totalEp watched",
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
            if (items.size > 1) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Box(
                    modifier = GlanceModifier.size(48.dp).cornerRadius(12.dp)
                        .background(GlanceTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${items.size - 1}",
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
    items: List<UpNextDisplayItem>,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?
) {
    if (items.isEmpty()) {
        EmptyStateExpanded()
        return
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            // .padding(16.dp) removed to allow LazyColumn to be full width
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), // Added padding here
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.upcoming_24px),
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
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
        }

        Spacer(modifier = GlanceModifier.height(0.dp)) // Header padding handles it

        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val firstFutureIndex = items.indexOfFirst { it.airingTime > currentTimeSeconds }
        // If no future items, no hero (or fall back to 0 if preferred, but user dislikes available now being hero)
        // Let's assume strict "Future = Hero" logic.

        LazyColumn(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index: Int, item: UpNextDisplayItem ->
                val isHero = index == firstFutureIndex
                // Wrap card in Column with padding
                Column(
                    modifier = GlanceModifier.fillMaxWidth().padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (index < items.lastIndex) 12.dp else 16.dp
                    )
                ) {
                    CountdownCard(
                        item = item,
                        isHero = isHero,
                        streamingService = streamingService,
                        streamingIconBitmap = streamingIconBitmap
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownCard(
    item: UpNextDisplayItem,
    isHero: Boolean,
    streamingService: StreamingService,
    streamingIconBitmap: Bitmap?
) {
    val context = LocalContext.current
    val detailsIntent = createDetailsIntent(context, item.entry.mediaId)
    val playIntent = createStreamingOrDetailsIntent(context, item.streamingUrl, item.entry.mediaId)

    val currentTime = System.currentTimeMillis() / 1000L
    val diffSeconds = item.airingTime - currentTime

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
                if (item.bitmap != null) {
                    Image(
                        provider = ImageProvider(item.bitmap),
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
                    text = item.entry.titleUserPreferred,
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
                        text = "EPISODE ${item.displayEpisode}",
                        style = TextStyle(
                            color = badgeTextColor,
                            fontSize = if (isHero) 12.sp else 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // FIX: Check if airingTime is valid (greater than 0)
                if (item.airingTime > 0) {
                    if (diffSeconds > 0) {
                        // Future: Episode is airing in the future
                        val days = TimeUnit.SECONDS.toDays(diffSeconds)

                        if (days > 0) {
                            // Show formatted date for > 24 hours
                            val date = Date(item.airingTime * 1000L)
                            val format = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = format.format(date),
                                    style = TextStyle(
                                        color = GlanceTheme.colors.primary,
                                        fontSize = if (isHero) 16.sp else 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        } else {
                            // < 1 day: Use real-time Chronometer for HH:MM:SS countdown
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CountdownChronometer(
                                    targetEpochSeconds = item.airingTime
                                )
                            }
                        }
                    } else {
                        // Past: Episode has already aired.
                        // Since we filtered "stale" episodes in provideGlance,
                        // if we are here, it means the episode aired within the last 30 minutes.
                        // Display "AVAILABLE NOW".
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AVAILABLE NOW",
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    // No airing time available? Check if we have *any* date
                    Text(
                        text = "Unknown Date",
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