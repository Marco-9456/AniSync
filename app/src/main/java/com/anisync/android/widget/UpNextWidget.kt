package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
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
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.anisync.android.R
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.widget.core.SizeClass
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntentUtils
import com.anisync.android.widget.core.toSizeClass
import com.anisync.android.widget.designsystem.components.EmptyStateConfig
import com.anisync.android.widget.designsystem.components.MediaPoster
import com.anisync.android.widget.designsystem.components.StandardEpisodeBadge
import com.anisync.android.widget.designsystem.components.TimeBadgeFromString
import com.anisync.android.widget.designsystem.components.WidgetEmptyState
import com.anisync.android.widget.designsystem.tokens.WidgetTypography
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/**
 * Hilt entry point used to inject dependencies directly into the widget.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpNextWidgetEntryPoint {
    fun airingScheduleDao(): AiringScheduleDao
}

/**
 * A beautifully redesigned Jetpack Glance widget that displays the user's immediate upcoming watch-list schedule.
 * Adapts seamlessly across Compact, Medium, and Expanded states using a modern card-based UI.
 */
class UpNextWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 100.dp),  // Compact (1 item Card)
            DpSize(250.dp, 100.dp),  // Medium (1 wide item Card)
            DpSize(250.dp, 250.dp),  // Expanded (List of Cards)
            DpSize(310.dp, 310.dp)   // Large Expanded
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            UpNextWidgetEntryPoint::class.java
        )
        val dao = entryPoint.airingScheduleDao()

        try {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        } catch (e: Exception) {
            null
        }

        val nowSeconds = System.currentTimeMillis() / 1000
        val futureSeconds = nowSeconds + (30L * 24 * 60 * 60)

        val upcomingSchedules = withContext(Dispatchers.IO) {
            try {
                dao.getAiringBetweenForUser(nowSeconds, futureSeconds)
            } catch (e: Exception) {
                emptyList()
            }
        }

        val itemsToDisplay = upcomingSchedules.take(3)

        val loadedImages = supervisorScope {
            itemsToDisplay.map { entry ->
                async(Dispatchers.IO) {
                    val bitmap = try {
                        WidgetImageLoader.loadBitmap(
                            appContext,
                            entry.coverUrl,
                            width = 200, // Higher res for beautiful rendering on rounded cards
                            height = 300
                        )
                    } catch (e: Exception) {
                        null
                    }
                    entry.id to bitmap
                }
            }.awaitAll().toMap()
        }

        provideContent {
            GlanceTheme {
                val sizeClass = LocalSize.current.toSizeClass()

                if (itemsToDisplay.isEmpty()) {
                    WidgetEmptyState(
                        config = EmptyStateConfig(
                            iconResId = R.drawable.calendar_view_week_24px,
                            title = "You're all caught up!",
                            subtitle = "No upcoming favorites"
                        ),
                        sizeClass = sizeClass,
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .appWidgetBackground()
                            .background(GlanceTheme.colors.widgetBackground)
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .appWidgetBackground()
                            .background(GlanceTheme.colors.widgetBackground)
                    ) {
                        when (sizeClass) {
                            SizeClass.COMPACT, SizeClass.MEDIUM -> {
                                UpNextSingleItem(
                                    episode = itemsToDisplay.first(),
                                    bitmap = loadedImages[itemsToDisplay.first().id],
                                    nowSeconds = nowSeconds
                                )
                            }

                            SizeClass.EXPANDED -> {
                                UpNextList(
                                    episodes = itemsToDisplay,
                                    loadedImages = loadedImages,
                                    nowSeconds = nowSeconds
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// LAYOUTS
// -------------------------------------------------------------------------

/**
 * Compact/Medium layout that highlights exactly ONE upcoming episode in a premium card.
 */
@Composable
private fun UpNextSingleItem(
    episode: AiringScheduleEntity,
    bitmap: Bitmap?,
    nowSeconds: Long
) {
    val context = LocalContext.current
    val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, episode.mediaId)
    val watchIntent = createWatchIntent(episode.streamingSeriesUrl)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner Card Container
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(20.dp) // Large premium rounding
                .clickable(actionStartActivity(detailsIntent))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaPoster(
                bitmap = bitmap,
                width = 64.dp, // Slightly larger poster
                height = 96.dp,
                cornerRadius = 12.dp
            )

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.calendar_view_week_24px),
                        contentDescription = null,
                        colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                        modifier = GlanceModifier.size(16.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Up Next",
                        style = WidgetTypography.labelLarge(
                            color = GlanceTheme.colors.primary
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = episode.titleUserPreferred,
                    style = WidgetTypography.bodyLarge(
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 2
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StandardEpisodeBadge(episodeNumber = episode.episode)
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = formatTimeUntil(episode.airingAt, nowSeconds),
                        style = WidgetTypography.labelLarge(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            weight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
            }

            if (watchIntent != null) {
                Spacer(modifier = GlanceModifier.width(12.dp))
                WatchButton(
                    intent = watchIntent,
                    fontSize = WidgetTypography.Label.large
                )
            }
        }
    }
}

/**
 * Expanded layout featuring a floating header and modern card-based list of episodes.
 */
@Composable
private fun UpNextList(
    episodes: List<AiringScheduleEntity>,
    loadedImages: Map<Int, Bitmap?>,
    nowSeconds: Long
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        // --- Header Section ---
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.calendar_view_week_24px),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    modifier = GlanceModifier.size(20.dp)
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Text(
                text = "Up Next",
                style = WidgetTypography.titleLarge(
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight()
            )
        }

        // --- Episode List Cards ---
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            itemsIndexed(episodes) { index, episode ->
                val context = LocalContext.current
                val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, episode.mediaId)
                val watchIntent = createWatchIntent(episode.streamingSeriesUrl)

                // Render as elevated, isolated cards instead of flat dividers
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(GlanceTheme.colors.surface)
                            .cornerRadius(16.dp)
                            .clickable(actionStartActivity(detailsIntent))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MediaPoster(
                            bitmap = loadedImages[episode.id],
                            width = 56.dp,
                            height = 80.dp,
                            cornerRadius = 8.dp
                        )

                        Spacer(modifier = GlanceModifier.width(16.dp))

                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = episode.titleUserPreferred,
                                style = WidgetTypography.bodyLarge(
                                    color = GlanceTheme.colors.onSurface
                                ),
                                maxLines = 2
                            )

                            Spacer(modifier = GlanceModifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StandardEpisodeBadge(episodeNumber = episode.episode)
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                TimeBadgeFromString(
                                    timeString = formatTimeUntil(
                                        episode.airingAt,
                                        nowSeconds
                                    )
                                )
                            }
                        }

                        if (watchIntent != null) {
                            Spacer(modifier = GlanceModifier.width(12.dp))
                            WatchButton(
                                intent = watchIntent,
                                fontSize = WidgetTypography.Label.large
                            )
                        }
                    }
                }

                // Add bottom padding for the very last item so it doesn't hug the edge
                if (index == episodes.size - 1) {
                    Spacer(modifier = GlanceModifier.height(12.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// HELPERS
// -------------------------------------------------------------------------

/**
 * Calculates and formats the time remaining until an episode airs.
 * Returns values like "In 2d 5h", "In 3h 15m", or "In 45m".
 */
private fun formatTimeUntil(airingAtSeconds: Long, nowSeconds: Long): String {
    val diffSeconds = airingAtSeconds - nowSeconds
    if (diffSeconds <= 0) return "Airing now"

    val days = diffSeconds / (24 * 3600)
    val hours = (diffSeconds % (24 * 3600)) / 3600
    val minutes = (diffSeconds % 3600) / 60

    return when {
        days > 0 -> "In ${days}d ${hours}h"
        hours > 0 -> "In ${hours}h ${minutes}m"
        else -> "In ${minutes}m"
    }
}

private fun createWatchIntent(url: String?): Intent? {
    val safeUrl = url
        ?.trim()
        ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        ?: return null

    return Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}

@Composable
private fun WatchButton(intent: Intent, fontSize: TextUnit) {
    Box(
        modifier = GlanceModifier
            .cornerRadius(100.dp) // Maximum pill shape rounding
            .background(GlanceTheme.colors.primary)
            .clickable(actionStartActivity(intent))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Watch",
            style = WidgetTypography.badgeText(
                color = GlanceTheme.colors.onPrimary,
                fontSize = fontSize
            )
        )
    }
}