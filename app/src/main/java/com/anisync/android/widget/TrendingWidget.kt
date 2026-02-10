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
import com.anisync.android.data.local.dao.TrendingDao
import com.anisync.android.data.local.entity.TrendingEntity
import com.anisync.android.widget.core.SizeClass
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntentUtils
import com.anisync.android.widget.core.toSizeClass
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
interface TrendingWidgetEntryPoint {
    fun trendingDao(): TrendingDao
}

/**
 * Trending Widget - Shows top trending anime in a grid layout
 *
 * Clean grid layout with bottom-positioned rank badges (matching reference design).
 */
class TrendingWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 100.dp),  // Compact: Single hero card with bottom badge
            DpSize(250.dp, 100.dp),  // Medium: Dual cards
            DpSize(250.dp, 220.dp)   // Expanded: 3-column grid (matching reference)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            TrendingWidgetEntryPoint::class.java
        )
        val dao = entryPoint.trendingDao()

        val trendingList = withContext(Dispatchers.IO) {
            try {
                dao.getTopTrending(limit = 12)
            } catch (e: Exception) {
                emptyList()
            }
        }

        val trendingWithImages = coroutineScope {
            trendingList.map { entry ->
                async {
                    val bitmap = WidgetImageLoader.loadBitmap(
                        appContext,
                        entry.coverUrl,
                        width = 300,
                        height = 400
                    )
                    entry to bitmap
                }
            }.awaitAll()
        }

        provideContent {
            GlanceTheme {
                val sizeClass = LocalSize.current.toSizeClass()

                when (sizeClass) {
                    SizeClass.COMPACT -> TrendingCompact(trendingWithImages)
                    SizeClass.MEDIUM -> TrendingMedium(trendingWithImages)
                    SizeClass.EXPANDED -> TrendingExpanded(trendingWithImages)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// LAYOUTS
// -------------------------------------------------------------------------

/**
 * COMPACT: Single card with bottom badge (similar to grid items but larger)
 */
@Composable
private fun TrendingCompact(entries: List<Pair<TrendingEntity, Bitmap?>>) {
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateCompact()
        return
    }

    val (entry, bitmap) = entries.first()
    val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, entry.id)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(detailsIntent)),
        contentAlignment = Alignment.BottomStart
    ) {
        // Full bleed image
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

        // Bottom badge (matching reference style)
        RankBadgeBottom(
            rank = entry.rank,
            modifier = GlanceModifier.padding(12.dp)
        )
    }
}

/**
 * MEDIUM: Side by side cards with bottom badges
 */
@Composable
private fun TrendingMedium(entries: List<Pair<TrendingEntity, Bitmap?>>) {
    if (entries.isEmpty()) {
        EmptyStateMedium()
        return
    }

    val first = entries[0]
    val second = entries.getOrNull(1)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // First card
        TrendingGridCard(
            entry = first.first,
            bitmap = first.second,
            modifier = GlanceModifier.defaultWeight()
        )

        if (second != null) {
            Spacer(modifier = GlanceModifier.width(12.dp))

            // Divider
            Box(
                modifier = GlanceModifier
                    .width(1.dp)
                    .fillMaxSize()
                    .background(GlanceTheme.colors.outline),
                contentAlignment = Alignment.Center
            ) {}

            Spacer(modifier = GlanceModifier.width(12.dp))

            // Second card
            TrendingGridCard(
                entry = second.first,
                bitmap = second.second,
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

/**
 * EXPANDED: 3-column grid matching reference screenshot exactly
 * - No titles on cards
 * - Rank badges at bottom-left
 * - Rounded corners
 * - Clean header with "Trending Now" and search action
 */
@Composable
private fun TrendingExpanded(entries: List<Pair<TrendingEntity, Bitmap?>>) {
    val context = LocalContext.current

    if (entries.isEmpty()) {
        EmptyStateExpanded()
        return
    }

    // Chunk into rows of 3
    val rows = entries.chunked(3)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
    ) {
        // Header matching reference: Icon + Title + Search action
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.trending_up_24px),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(24.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "Trending Now",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            // Search action - opens main app search
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .cornerRadius(12.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .clickable(actionStartActivity(createSearchIntent(context))),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_search_24px),
                    contentDescription = "Search",
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.size(20.dp)
                )
            }
        }

        // Grid
        LazyColumn(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            items(rows) { rowItems ->
                // Wrap each row in Column with padding to prevent scrollbar overlap
                Column(
                    modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 12.dp)
                ) {
                    TrendingGridRowBottomBadges(rowItems)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENTS
// -------------------------------------------------------------------------

/**
 * Grid card component - full image with bottom-left rank badge
 */
@Composable
private fun TrendingGridCard(
    entry: TrendingEntity,
    bitmap: Bitmap?,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, entry.id)

    Box(
        modifier = modifier
            .fillMaxSize()
            .height(140.dp)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(detailsIntent)),
        contentAlignment = Alignment.BottomStart
    ) {
        // Full cover image
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
                    .background(GlanceTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {}
        }

        // Bottom positioned badge (matching reference)
        RankBadgeBottom(
            rank = entry.rank,
            modifier = GlanceModifier.padding(10.dp)
        )
    }
}

/**
 * Rank badge positioned at bottom - matches reference style
 * Pill shape, light background, positioned bottom-left
 */
@Composable
private fun RankBadgeBottom(rank: Int, modifier: GlanceModifier = GlanceModifier) {
    Box(
        modifier = modifier
            .cornerRadius(10.dp)
            .background(
                ColorProvider(
                    day = Color(0xF2FFFFFF), // 95% white
                    night = Color(0xF2FFFFFF)
                )
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$rank",
            style = TextStyle(
                color = ColorProvider(day = Color.Black, night = Color.Black),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Grid row with 3 items - bottom badges only
 */
@Composable
private fun TrendingGridRowBottomBadges(rowItems: List<Pair<TrendingEntity, Bitmap?>>) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        rowItems.forEach { (entry, bitmap) ->
            val context = LocalContext.current
            val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, entry.id)

            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(160.dp) // Taller for better aspect ratio
                    .padding(4.dp)
            ) {
                // Card with image and bottom badge
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(16.dp)
                        .clickable(actionStartActivity(detailsIntent)),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // Image fills entire card
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
                                .background(GlanceTheme.colors.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {}
                    }

                    // Rank badge
                    Box(
                        modifier = GlanceModifier
                            .padding(8.dp)
                            .background(GlanceTheme.colors.primaryContainer)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#${entry.rank}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Fill remaining slots
        if (rowItems.size < 3) {
            repeat(3 - rowItems.size) {
                Spacer(modifier = GlanceModifier.defaultWeight().padding(4.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------
// EMPTY STATES
// -------------------------------------------------------------------------

@Composable
private fun EmptyStateCompact() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(WidgetIntentUtils.openMainAppIntent(context))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.trending_up_24px),
            contentDescription = null,
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
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.trending_up_24px),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(28.dp)
            )
        }

        Spacer(modifier = GlanceModifier.width(16.dp))

        Column {
            Text(
                text = "Trending unavailable",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun EmptyStateExpanded() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity(WidgetIntentUtils.openMainAppIntent(context))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = GlanceModifier
                    .size(72.dp)
                    .cornerRadius(24.dp)
                    .background(GlanceTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.trending_up_24px),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier.size(40.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(16.dp))

            Text(
                text = "Trending data unavailable",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(horizontal = 24.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------
// HELPERS
// -------------------------------------------------------------------------

private fun createSearchIntent(context: Context): Intent {
    return Intent(
        Intent.ACTION_VIEW,
        "anisync://search".toUri()
    ).apply {
        setClass(context, MainActivity::class.java)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
