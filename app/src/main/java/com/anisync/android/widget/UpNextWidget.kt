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
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
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
import com.anisync.android.widget.designsystem.tokens.WidgetDimensions
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
 * A Jetpack Glance widget that displays the user's immediate upcoming watch-list schedule.
 * Automatically adapts between a single-item compact view and a multi-item expanded view.
 */
class UpNextWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 100.dp),  // Compact (1 item)
            DpSize(250.dp, 100.dp),  // Medium (1 wide item)
            DpSize(250.dp, 250.dp),  // Expanded (List of items)
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

        // Safely extract preferences (e.g., config targets) if the state file exists
        try {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        } catch (e: Exception) {
            null
        }

        val nowSeconds = System.currentTimeMillis() / 1000
        // Look ahead up to 30 days to find the next airing episodes for the user
        val futureSeconds = nowSeconds + (30L * 24 * 60 * 60)

        // Fetch "Up Next" schedule from DB.
        // getAiringBetweenForUser automatically filters by isWatching = 1
        val upcomingSchedules = withContext(Dispatchers.IO) {
            try {
                dao.getAiringBetweenForUser(nowSeconds, futureSeconds)
            } catch (e: Exception) {
                emptyList()
            }
        }

        // We only need at most the next 3 items, even in the largest layout.
        val itemsToDisplay = upcomingSchedules.take(3)

        // Safely preload the cover images. Using supervisorScope prevents a single network
        // failure from terminating the entire widget update session.
        val loadedImages = supervisorScope {
            itemsToDisplay.map { entry ->
                async(Dispatchers.IO) {
                    val bitmap = try {
                        WidgetImageLoader.loadBitmap(
                            appContext,
                            entry.coverUrl,
                            width = 120,
                            height = 180
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
                            iconResId = R.drawable.calendar_view_week_24px, // Fallback icon
                            title = "You're all caught up!",
                            subtitle = "No upcoming favorites"
                        ),
                        sizeClass = sizeClass,
                        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
                            .background(GlanceTheme.colors.surface)
                    )
                } else {
                    when (sizeClass) {
                        SizeClass.COMPACT, SizeClass.MEDIUM -> {
                            // Show only the single most immediate episode
                            UpNextSingleItem(
                                episode = itemsToDisplay.first(),
                                bitmap = loadedImages[itemsToDisplay.first().id],
                                nowSeconds = nowSeconds
                            )
                        }

                        SizeClass.EXPANDED -> {
                            // Show a list of the next 3 episodes
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

// -------------------------------------------------------------------------
// LAYOUTS
// -------------------------------------------------------------------------

/**
 * Compact/Medium layout that highlights exactly ONE upcoming episode.
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

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(WidgetDimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(detailsIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaPoster(
                bitmap = bitmap,
                width = WidgetDimensions.Poster.widthCompact,
                height = WidgetDimensions.Poster.heightCompact,
                cornerRadius = WidgetDimensions.cornerRadiusSmall
            )

            Spacer(modifier = GlanceModifier.width(WidgetDimensions.Spacer.medium))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Up Next",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = WidgetTypography.Caption.large,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.xsmall))
                Text(
                    text = episode.titleUserPreferred,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = WidgetTypography.Body.large,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )

                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.xsmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StandardEpisodeBadge(episodeNumber = episode.episode)
                    Spacer(modifier = GlanceModifier.width(WidgetDimensions.Spacer.small))
                    Text(
                        text = formatTimeUntil(episode.airingAt, nowSeconds),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = WidgetTypography.Caption.large
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        if (watchIntent != null) {
            Spacer(modifier = GlanceModifier.width(WidgetDimensions.Spacer.medium))
            WatchButton(
                intent = watchIntent,
                fontSize = WidgetTypography.Body.medium
            )
        }
    }
}

/**
 * Expanded layout that shows a timeline of the next several upcoming episodes.
 */
@Composable
private fun UpNextList(
    episodes: List<AiringScheduleEntity>,
    loadedImages: Map<Int, Bitmap?>,
    nowSeconds: Long
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
    ) {
        // --- Header Section ---
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(
                    horizontal = WidgetDimensions.paddingLarge,
                    vertical = WidgetDimensions.paddingLarge
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Up Next",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = WidgetTypography.Title.large,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
        }

        // --- Episode List Timeline ---
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            itemsIndexed(episodes) { index, episode ->
                val context = LocalContext.current
                val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, episode.mediaId)
                val watchIntent = createWatchIntent(episode.streamingSeriesUrl)

                // Wrapping the Row and Divider in a Column prevents Glance overlay issues
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = WidgetDimensions.paddingLarge,
                                vertical = WidgetDimensions.paddingMedium
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStartActivity(detailsIntent)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaPoster(
                                bitmap = loadedImages[episode.id],
                                width = WidgetDimensions.Poster.widthMedium,
                                height = WidgetDimensions.Poster.heightMedium,
                                cornerRadius = WidgetDimensions.cornerRadiusSmall
                            )

                            Spacer(modifier = GlanceModifier.width(WidgetDimensions.Spacer.medium))

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = episode.titleUserPreferred,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = WidgetTypography.Title.small,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 2
                                )

                                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.xsmall))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StandardEpisodeBadge(episodeNumber = episode.episode)
                                    Spacer(modifier = GlanceModifier.width(WidgetDimensions.Spacer.small))
                                    TimeBadgeFromString(
                                        timeString = formatTimeUntil(
                                            episode.airingAt,
                                            nowSeconds
                                        )
                                    )
                                }
                            }
                        }

                        if (watchIntent != null) {
                            Spacer(modifier = GlanceModifier.width(WidgetDimensions.Spacer.medium))
                            WatchButton(
                                intent = watchIntent,
                                fontSize = WidgetTypography.Caption.large
                            )
                        }
                    }

                    // Divider between items
                    if (index < episodes.size - 1) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(horizontal = WidgetDimensions.paddingLarge)
                        ) {
                            Spacer(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(GlanceTheme.colors.surfaceVariant)
                            )
                        }
                    }
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
            .cornerRadius(WidgetDimensions.cornerRadiusPill)
            .background(GlanceTheme.colors.primary)
            .clickable(actionStartActivity(intent))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Watch",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimary,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
