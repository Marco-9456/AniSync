package com.anisync.android.presentation.settings.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.data.AppSettings
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.SegmentedTabItem
import com.anisync.android.presentation.components.CompletedCardConfig
import com.anisync.android.presentation.components.LibraryMediaCard
import com.anisync.android.presentation.components.WatchingCardConfig
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.PreviewTheme
import com.anisync.android.ui.theme.toAmoled
import com.materialkolor.PaletteStyle

// OPTIMIZATION: Move constant data structures to top-level to avoid allocation on every recomposition.
private val Statuses = listOf(
    LibraryStatus.CURRENT to Icons.Default.PlayArrow,
    LibraryStatus.COMPLETED to Icons.Default.Done,
    LibraryStatus.PLANNING to Icons.Default.Schedule
)

private val Formats = listOf(
    Triple(MediaFormat.TV, "TV", Icons.Default.Tv),
    Triple(MediaFormat.MOVIE, "Movie", Icons.Default.Movie),
    Triple(MediaFormat.OVA, "OVA", Icons.Default.Book)
)

/**
 * A horizontal component showcase preview for the Look and Feel settings screen.
 *
 * Displays actual UI components from the app in a realistic "Dashboard" layout.
 * Redesigned to be compact and non-scrollable vertically to fit Settings screens.
 */
@Composable
fun PhonePreview(
    seedColor: Color?,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    modifier: Modifier = Modifier,
    amoled: Boolean = false
) {
    val context = LocalContext.current
    // Own instance so @Preview renders without LocalAppSettings; app context avoids pinning the
    // Activity in the composition.
    val appSettings = remember { AppSettings(context.applicationContext) }

    // OPTIMIZATION: Memoize the dynamic color scheme creation.
    // Generating dynamic schemes can be expensive; we only want to do this if the context or mode changes.
    val dynamicColorScheme = remember(context, isDarkMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            null
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val themeContent = @Composable {
            CompositionLocalProvider(LocalAppSettings provides appSettings) {
                // Outer container imitating a phone screen surface or card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    ComponentPreviewContent()
                }
            }
        }

        if (seedColor != null) {
            PreviewTheme(
                seedColor = seedColor,
                isDark = isDarkMode,
                style = paletteStyle,
                amoled = amoled,
                content = themeContent
            )
        } else if (dynamicColorScheme != null) {
            // OPTIMIZATION: Use the memoized scheme
            val scheme = if (amoled && isDarkMode) dynamicColorScheme.toAmoled() else dynamicColorScheme
            PreviewTheme(colorScheme = scheme, content = themeContent)
        } else {
            PreviewTheme(
                seedColor = Color(0xFF6750A4),
                isDark = isDarkMode,
                style = paletteStyle,
                amoled = amoled,
                content = themeContent
            )
        }
    }
}

/**
 * Main content showing a realistic library screen fragment.
 */
@Composable
private fun ComponentPreviewContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.library_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Status Tabs
        var selectedTabIndex by remember { mutableIntStateOf(0) }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // OPTIMIZATION: Use 'itemsIndexed' with a key.
            // Providing a stable key (the status enum) helps Compose skip recomposition of unmodified items.
            itemsIndexed(
                items = Statuses,
                key = { _, (status, _) -> status }
            ) { index, (status, icon) ->
                SegmentedTabItem(
                    index = index,
                    selectedIndex = selectedTabIndex,
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    icon = icon,
                    label = status.name.lowercase().replaceFirstChar { it.uppercase() }
                )
            }
        }

        // Library Cards Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // OPTIMIZATION: Provide a key for library entries.
            // Uses the unique ID of the entry. Essential for performance if this list updates.
            items(
                items = mockLibraryEntries,
                key = { it.id }
            ) { entry ->
                LibraryMediaCard(
                    entry = entry,
                    mediaType = entry.type ?: MediaType.ANIME,
                    titleLanguage = TitleLanguage.ROMAJI,
                    onClick = { },
                    modifier = Modifier.width(150.dp),
                    config = if (entry.status == LibraryStatus.COMPLETED) {
                        CompletedCardConfig
                    } else {
                        WatchingCardConfig
                    },
                    onIncrement = { },
                    onDecrement = { }
                )
            }
        }

        // Statistics Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PreviewStatCard() }
            item { PreviewFormatsCard() }
        }
    }
}

/**
 * Mini stat card showing total count and key metrics.
 */
@Composable
private fun PreviewStatCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.width(170.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Main count
            Text(
                text = "42",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.statistics_total_anime),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.size(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.size(12.dp))

            // Sub stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Days watched
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "120",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = stringResource(R.string.stat_days),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Mean score
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "8.4",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                        Text(
                            text = stringResource(R.string.stat_score),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Mini formats card showing format breakdown.
 */
@Composable
private fun PreviewFormatsCard() {
    Card(
        modifier = Modifier.width(170.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.statistics_formats),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // OPTIMIZATION: Using the static 'Formats' list instead of recreating it here.
            Formats.forEachIndexed { index, (format, label, icon) ->
                PreviewFormatRow(
                    icon = icon,
                    label = label,
                    count = when (format) {
                        MediaFormat.TV -> 142
                        MediaFormat.MOVIE -> 28
                        MediaFormat.OVA -> 15
                        else -> 0
                    },
                    score = when (format) {
                        MediaFormat.TV -> "8.4"
                        MediaFormat.MOVIE -> "7.8"
                        MediaFormat.OVA -> "8.1"
                        else -> "0.0"
                    }
                )

                if (index < Formats.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewFormatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    score: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = score,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// =============================================================================
// MOCK DATA
// =============================================================================

private val mockLibraryEntries = listOf(
    LibraryEntry(
        id = 1,
        mediaId = 1,
        titleRomaji = "Frieren: Beyond Journey's End",
        titleEnglish = "Frieren: Beyond Journey's End",
        titleNative = "葬送のフリーレン",
        titleUserPreferred = "Frieren: Beyond Journey's End",
        coverUrl = "https://api.dicebear.com/9.x/thumbs/svg?seed=frieren&backgroundColor=b6e3f4",
        progress = 12,
        totalEpisodes = 28,
        totalChapters = null,
        totalVolumes = null,
        type = MediaType.ANIME,
        format = MediaFormat.TV,
        status = LibraryStatus.CURRENT,
        nextAiringEpisode = 13,
        timeUntilAiring = 86400
    ),
    LibraryEntry(
        id = 2,
        mediaId = 2,
        titleRomaji = "One Piece",
        titleEnglish = "One Piece",
        titleNative = "ワンピース",
        titleUserPreferred = "One Piece",
        coverUrl = "https://api.dicebear.com/9.x/thumbs/svg?seed=onepiece&backgroundColor=ffdfbf",
        progress = 1075,
        totalEpisodes = null,
        totalChapters = null,
        totalVolumes = null,
        type = MediaType.ANIME,
        format = MediaFormat.TV,
        status = LibraryStatus.CURRENT,
        nextAiringEpisode = 1076,
        timeUntilAiring = 172800
    ),
    LibraryEntry(
        id = 3,
        mediaId = 3,
        titleRomaji = "Attack on Titan",
        titleEnglish = "Attack on Titan",
        titleNative = "進撃の巨人",
        titleUserPreferred = "Attack on Titan",
        coverUrl = "https://api.dicebear.com/9.x/thumbs/svg?seed=attackontitan&backgroundColor=ffdfbf",
        progress = 88,
        totalEpisodes = 88,
        totalChapters = null,
        totalVolumes = null,
        type = MediaType.ANIME,
        format = MediaFormat.TV,
        status = LibraryStatus.COMPLETED,
        nextAiringEpisode = null,
        timeUntilAiring = null
    )
)

// =============================================================================
// PREVIEWS
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun PhonePreviewDark() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        PhonePreview(
            seedColor = Color(0xFF6750A4),
            isDarkMode = true,
            paletteStyle = PaletteStyle.TonalSpot,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhonePreviewLight() {
    MaterialTheme {
        PhonePreview(
            seedColor = Color(0xFFE91E63),
            isDarkMode = false,
            paletteStyle = PaletteStyle.TonalSpot,
            modifier = Modifier.padding(16.dp)
        )
    }
}