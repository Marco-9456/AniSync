package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.text.TextStyle
import com.anisync.android.R
import com.anisync.android.data.local.dao.TrendingDao
import com.anisync.android.data.local.entity.TrendingEntity
import com.anisync.android.widget.core.SizeClass
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetIntentUtils
import com.anisync.android.widget.core.toSizeClass
import com.anisync.android.widget.designsystem.components.EmptyStateConfig
import com.anisync.android.widget.designsystem.components.MediaPoster
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
interface TrendingWidgetEntryPoint {
    fun trendingDao(): TrendingDao
}

/**
 * Action callback to handle clicks on the discover search icon.
 */
class OpenDiscoverAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // TODO: Implement the logic to open the Discover screen in the app.
    }
}

/**
 * A beautifully redesigned Jetpack Glance widget that displays the current top trending anime leaderboard.
 * Adapts seamlessly across Compact, Medium, and Expanded states using a modern card-based UI.
 */
class TrendingWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 100.dp),  // Compact (Shows #1 Trending Card)
            DpSize(250.dp, 100.dp),  // Medium (Shows Top 3 Horizontal Cards)
            DpSize(250.dp, 250.dp),  // Expanded (Shows Full List of Cards)
            DpSize(310.dp, 310.dp)   // Large Expanded
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            TrendingWidgetEntryPoint::class.java
        )
        val dao = entryPoint.trendingDao()

        try {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        } catch (e: Exception) {
            null
        }

        val trendingItems = withContext(Dispatchers.IO) {
            try {
                dao.getTopTrending(limit = 10)
            } catch (e: Exception) {
                emptyList()
            }
        }

        val loadedImages = supervisorScope {
            trendingItems.map { entry ->
                async(Dispatchers.IO) {
                    val bitmap = try {
                        WidgetImageLoader.loadBitmap(
                            appContext,
                            entry.coverUrl,
                            width = 200, // Slightly higher res for beautiful rendering
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

                if (trendingItems.isEmpty()) {
                    WidgetEmptyState(
                        config = EmptyStateConfig(
                            iconResId = R.drawable.local_fire_department_24px,
                            title = "Nothing Trending",
                            subtitle = "Check back later"
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
                            SizeClass.COMPACT -> {
                                TrendingCompact(
                                    item = trendingItems.first(),
                                    bitmap = loadedImages[trendingItems.first().id]
                                )
                            }

                            SizeClass.MEDIUM -> {
                                TrendingMedium(
                                    items = trendingItems.take(3),
                                    loadedImages = loadedImages
                                )
                            }

                            SizeClass.EXPANDED -> {
                                TrendingExpanded(
                                    items = trendingItems,
                                    loadedImages = loadedImages
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
 * Compact layout highlighting the #1 currently trending anime in a premium pill/card style.
 */
@Composable
private fun TrendingCompact(
    item: TrendingEntity,
    bitmap: Bitmap?
) {
    val context = LocalContext.current
    val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, item.id)

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
                .cornerRadius(20.dp) // Beautiful large rounding
                .clickable(actionStartActivity(detailsIntent))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopStart) {
                MediaPoster(
                    bitmap = bitmap,
                    width = 64.dp, // Slightly larger
                    height = 96.dp,
                    cornerRadius = 12.dp
                )

                // Overlaid Rank Badge
                RankBadge(rank = item.rank)
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.local_fire_department_24px),
                        contentDescription = null,
                        colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                        modifier = GlanceModifier.size(16.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Trending",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = WidgetTypography.Caption.large,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = item.titleUserPreferred,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = WidgetTypography.Body.large,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )

                if (item.averageScore != null) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.kid_star_24px),
                            contentDescription = null,
                            colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                            modifier = GlanceModifier.size(14.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = "${item.averageScore}%",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = WidgetTypography.Caption.large,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Medium layout that creates a "shelf" of the Top 3 trending items using individual mini-cards.
 */
@Composable
private fun TrendingMedium(
    items: List<TrendingEntity>,
    loadedImages: Map<Int, Bitmap?>
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val context = LocalContext.current
            val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, item.id)

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity(detailsIntent))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.TopStart) {
                    MediaPoster(
                        bitmap = loadedImages[item.id],
                        width = WidgetDimensions.Poster.widthCompact,
                        height = WidgetDimensions.Poster.heightCompact,
                        cornerRadius = 12.dp
                    )
                    // Overlaid Rank Badge
                    RankBadge(rank = item.rank)
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = item.titleUserPreferred,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = WidgetTypography.Caption.large,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            if (index < items.size - 1) {
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
        }
    }
}

/**
 * Expanded layout featuring a beautiful floating header and modern card-based list.
 */
@Composable
private fun TrendingExpanded(
    items: List<TrendingEntity>,
    loadedImages: Map<Int, Bitmap?>
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
                    provider = ImageProvider(R.drawable.local_fire_department_24px),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    modifier = GlanceModifier.size(20.dp)
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Text(
                text = "Trending Now",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = WidgetTypography.Title.large,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            // Minimalist Search / Discover Button
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(10.dp)
                    .clickable(actionRunCallback<OpenDiscoverAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_search_24px),
                    contentDescription = "Open Discover Screen",
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.size(18.dp)
                )
            }
        }

        // --- Leaderboard List ---
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            itemsIndexed(items) { index, item ->
                val context = LocalContext.current
                val detailsIntent = WidgetIntentUtils.createDetailsIntent(context, item.id)

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

                        Box(contentAlignment = Alignment.TopStart) {
                            MediaPoster(
                                bitmap = loadedImages[item.id],
                                width = 56.dp,
                                height = 80.dp,
                                cornerRadius = 8.dp
                            )
                            RankBadge(rank = item.rank)
                        }

                        Spacer(modifier = GlanceModifier.width(16.dp))

                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = item.titleUserPreferred,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = WidgetTypography.Body.large,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 2
                            )

                            if (item.averageScore != null) {
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        provider = ImageProvider(R.drawable.kid_star_24px),
                                        contentDescription = null,
                                        colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                                        modifier = GlanceModifier.size(14.dp)
                                    )
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                    Text(
                                        text = "${item.averageScore}% Score",
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = WidgetTypography.Caption.large,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Add bottom padding for the very last item so it doesn't hug the edge
                if (index == items.size - 1) {
                    Spacer(modifier = GlanceModifier.height(12.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// REUSABLE UI COMPONENTS
// -------------------------------------------------------------------------

/**
 * A sleek, high-contrast rank badge that sits flush against the top-left of a poster.
 */
@Composable
private fun RankBadge(rank: Int) {
    Box(
        modifier = GlanceModifier
            .padding(top = 4.dp, start = 4.dp)
            .background(GlanceTheme.colors.primary)
            .cornerRadius(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$rank",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimary,
                fontSize = WidgetTypography.Caption.medium,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}