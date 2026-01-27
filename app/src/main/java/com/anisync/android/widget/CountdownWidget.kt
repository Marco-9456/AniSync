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
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
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
interface CountdownWidgetEntryPoint {
    fun airingScheduleDao(): AiringScheduleDao
    fun libraryDao(): LibraryDao
}

/**
 * Countdown Widget - Redesigned v2.0
 *
 * Displays countdown timers for upcoming episodes with a focus on urgency
 * and visual hierarchy. Uses "time remaining" as the primary visual element.
 */
class CountdownWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 100.dp),  // Compact: 2x1 - Single large countdown digits
            DpSize(250.dp, 100.dp),  // Medium: 3x1/4x1 - Dual countdowns or rich single
            DpSize(250.dp, 220.dp)   // Expanded: 3x2+ - Hero cards with background images
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            CountdownWidgetEntryPoint::class.java
        )
        val airingScheduleDao = entryPoint.airingScheduleDao()
        val libraryDao = entryPoint.libraryDao()

        // Fetch upcoming (next 14 days), prioritize user's watchlist
        val now = System.currentTimeMillis() / 1000
        val rangeEnd = now + (14 * 24 * 60 * 60)

        val upcoming = withContext(Dispatchers.IO) {
            try {
                // Get upcoming from airing schedule (entries user is watching)
                val airingScheduleEntries = airingScheduleDao.getAiringBetween(now, rangeEnd)
                    .filter { it.isWatching }
                    .sortedBy { it.airingAt }

                // Get library entries with upcoming episodes (timeUntilAiring is in seconds)
                val libraryEntries = libraryDao.getUpNext()
                val upcomingFromLibrary = libraryEntries.filter { entry ->
                    entry.timeUntilAiring != null && entry.timeUntilAiring > 0
                }.sortedBy { it.timeUntilAiring }

                // Get mediaIds already in airing schedule to avoid duplicates
                val airingMediaIds = airingScheduleEntries.map { it.mediaId }.toSet()

                // Convert library entries to airing schedule format for unified display
                val libraryAsAiring = upcomingFromLibrary
                    .filter { it.mediaId !in airingMediaIds }
                    .map { entry ->
                        AiringScheduleEntity(
                            id = entry.mediaId, // Use mediaId as id for library-derived entries
                            mediaId = entry.mediaId,
                            airingAt = now + (entry.timeUntilAiring ?: 0),
                            episode = entry.nextAiringEpisode ?: (entry.progress + 1),
                            titleUserPreferred = entry.titleUserPreferred,
                            coverUrl = entry.coverUrl,
                            format = entry.mediaStatus, // Use mediaStatus as format fallback
                            isWatching = true
                        )
                    }

                // Merge both sources and sort by airing time
                (airingScheduleEntries + libraryAsAiring)
                    .sortedBy { it.airingAt }
                    .take(10)
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Load images for expanded mode (hero cards)
        val upcomingWithImages = coroutineScope {
            upcoming.map { entry ->
                async {
                    val bitmap = WidgetImageUtils.loadBitmap(
                        appContext,
                        entry.coverUrl,
                        width = 400,
                        height = 300 // Landscape aspect for hero cards
                    )
                    entry to bitmap
                }
            }.awaitAll()
        }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current

                when {
                    size.height <= 110.dp -> CountdownCompact(upcoming)
                    size.height <= 120.dp -> CountdownMedium(upcoming)
                    else -> CountdownExpanded(upcomingWithImages)
                }
            }
        }
    }
}

/**
 * COMPACT LAYOUT (2x1)
 * Canonical: "Focus" layout with large typography
 * Displays one urgent countdown with maximum time visibility
 */
@Composable
private fun CountdownCompact(entries: List<AiringScheduleEntity>) {
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateCompact()
        return
    }

    val entry = entries.first()
    val timeString = formatCountdown(entry.airingAt - (System.currentTimeMillis() / 1000))
    val detailsIntent = createDetailsIntent(context, entry.mediaId)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large countdown digits block
        Box(
            modifier = GlanceModifier
                .width(80.dp)
                .height(64.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeString,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Episode ${entry.episode}",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "until airing",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Details action
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .clickable(actionStartActivity(detailsIntent)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_info_details),
                contentDescription = "Details",
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(24.dp)
            )
        }
    }
}

/**
 * MEDIUM LAYOUT (3x1/4x1)
 * Canonical: "Row" with dual emphasis
 * Shows two countdowns side by side or one rich card
 */
@Composable
private fun CountdownMedium(entries: List<AiringScheduleEntity>) {
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateMedium()
        return
    }

    val primary = entries.first()
    val secondary = entries.getOrNull(1)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Primary countdown - Large
        CountdownCompactCard(
            entry = primary,
            modifier = GlanceModifier.defaultWeight(),
            emphasized = true
        )

        if (secondary != null) {
            Spacer(modifier = GlanceModifier.width(12.dp))

            // Vertical divider
            Box(
                modifier = GlanceModifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(GlanceTheme.colors.outline),
                contentAlignment = Alignment.Center
            ) {}

            Spacer(modifier = GlanceModifier.width(12.dp))

            // Secondary countdown - Standard
            CountdownCompactCard(
                entry = secondary,
                modifier = GlanceModifier.defaultWeight(),
                emphasized = false
            )
        }
    }
}

/**
 * EXPANDED LAYOUT (3x2+)
 * Canonical: "Card" (Hero) with background images
 * Immersive countdown cards with gradient overlays
 */
@Composable
private fun CountdownExpanded(entries: List<Pair<AiringScheduleEntity, Bitmap?>>) {
    if (entries.isEmpty()) {
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
                text = "Countdown",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            if (entries.size > 1) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(12.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${entries.size} upcoming",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(entries) { (entry, bitmap) ->
                CountdownHeroCard(entry, bitmap)
                Spacer(modifier = GlanceModifier.height(12.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------
// Component Composables
// -------------------------------------------------------------------------

@Composable
private fun CountdownCompactCard(
    entry: AiringScheduleEntity,
    modifier: GlanceModifier = GlanceModifier,
    emphasized: Boolean
) {
    val context = LocalContext.current
    val timeString = formatCountdown(entry.airingAt - (System.currentTimeMillis() / 1000))
    val detailsIntent = createDetailsIntent(context, entry.mediaId)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(12.dp)
            .clickable(actionStartActivity(detailsIntent))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time digit block
        Box(
            modifier = GlanceModifier
                .size(if (emphasized) 48.dp else 40.dp)
                .cornerRadius(10.dp)
                .background(GlanceTheme.colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeString.substringBefore(" "), // Just the number
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimary,
                    fontSize = if (emphasized) 18.sp else 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = if (emphasized) 14.sp else 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "EP ${entry.episode} • ${timeString.substringAfter(" ")}", // Unit (d/h/m)
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun CountdownHeroCard(entry: AiringScheduleEntity, bitmap: Bitmap?) {
    val context = LocalContext.current
    val timeString = formatCountdown(entry.airingAt - (System.currentTimeMillis() / 1000))
    val detailsIntent = createDetailsIntent(context, entry.mediaId)

    // Define colors for overlay
    val black60 = Color.Black.copy(alpha = 0.6f)
    val whiteColor = Color.White
    val white90 = Color.White.copy(alpha = 0.9f)
    val cyanAccent = Color(0xFF80DEEA) // Cyan 200

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(180.dp)
            .cornerRadius(20.dp),
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
        } else {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {}
        }

        // 2. Gradient Scrim (60% black)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = black60, night = black60)),
            contentAlignment = Alignment.Center
        ) {}

        // 3. Clickable overlay
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(detailsIntent)),
            contentAlignment = Alignment.Center
        ) {}

        // 4. Content (Bottom-aligned)
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Large countdown at top of card content area
            Text(
                text = timeString,
                style = TextStyle(
                    color = ColorProvider(day = whiteColor, night = whiteColor),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "REMAINING",
                style = TextStyle(
                    color = ColorProvider(day = white90, night = white90),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(
                    color = ColorProvider(day = whiteColor, night = whiteColor),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Episode ${entry.episode}",
                style = TextStyle(
                    color = ColorProvider(day = cyanAccent, night = cyanAccent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// -------------------------------------------------------------------------
// Empty States
// -------------------------------------------------------------------------

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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_lock_idle_alarm),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(32.dp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "No upcoming",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun EmptyStateMedium() {
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
                .size(48.dp)
                .cornerRadius(16.dp)
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
                text = "All caught up!",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Nothing airing in the next 2 weeks",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
private fun EmptyStateExpanded() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
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
                text = "No Upcoming Episodes",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Your watchlist shows are up to date",
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

// -------------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------------

private fun formatCountdown(secondsDiff: Long): String {
    if (secondsDiff < 0) return "Aired"

    val days = secondsDiff / 86400
    val hours = (secondsDiff % 86400) / 3600
    val minutes = (secondsDiff % 3600) / 60

    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

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