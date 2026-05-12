package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.StudioStat
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.util.formatDecimal
import com.anisync.android.ui.theme.LocalExpressiveTypography

@Composable
fun <T> HorizontalStatsSection(
    title: String,
    items: List<T>,
    key: ((T) -> Any)? = null,
    onActionClick: (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    Column {
        SectionHeader(
            title = title,
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            onActionClick = onActionClick
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = key) { item ->
                itemContent(item)
            }
        }
    }
}

@Composable
fun GenreCardModern(genre: GenreStat) {
    val expressive = LocalExpressiveTypography.current
    val secondary = MaterialTheme.colorScheme.secondaryContainer
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val gradientBrush = remember(secondary, surfaceHigh) {
        Brush.linearGradient(colors = listOf(secondary, surfaceHigh))
    }

    Card(
        modifier = Modifier.width(176.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = genre.count.toString(),
                        style = expressive.statNumericMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (genre.meanScore > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = formatDecimal(genre.meanScore),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = genre.genre,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${genre.count} entries",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun StudioCardModern(studio: StudioStat, onClick: () -> Unit = {}) {
    val expressive = LocalExpressiveTypography.current
    val tertiary = MaterialTheme.colorScheme.tertiaryContainer
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val gradientBrush = remember(tertiary, surfaceHigh) {
        Brush.linearGradient(colors = listOf(tertiary, surfaceHigh))
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(176.dp)
            .height(196.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = studio.count.toString(),
                        style = expressive.statNumericMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    if (studio.meanScore > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = formatDecimal(studio.meanScore),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = studio.studioName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${studio.count} entries",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "HorizontalStatsSection — genres", widthDp = 360)
@Composable
private fun HorizontalGenresPreview() {
    StatPreviewSurface(isDark = false) {
        HorizontalStatsSection(
            title = "Top genres",
            items = previewGenres,
            key = { it.genre }
        ) { GenreCardModern(it) }
    }
}

@Preview(showBackground = true, name = "HorizontalStatsSection — studios", widthDp = 360)
@Composable
private fun HorizontalStudiosPreview() {
    StatPreviewSurface(isDark = false) {
        HorizontalStatsSection(
            title = "Top studios",
            items = previewStudios,
            key = { it.studioName }
        ) { StudioCardModern(it) }
    }
}

@Preview(showBackground = true, name = "GenreCard — long name + no score")
@Composable
private fun GenreCardEdgePreview() {
    StatPreviewSurface(isDark = false) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GenreCardModern(previewGenres.first())
            GenreCardModern(GenreStat(
                genre = "Mystery / Psychological Thriller With Way Too Many Words",
                count = 9, meanScore = 0f, hoursWatched = 30f
            ))
        }
    }
}

@Preview(showBackground = true, name = "StudioCard — single-char + long name")
@Composable
private fun StudioCardEdgePreview() {
    StatPreviewSurface(isDark = false) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StudioCardModern(StudioStat(id = 9999, studioName = "Z", count = 1, meanScore = 6.0f, hoursWatched = 2f))
            StudioCardModern(previewStudios.last())
        }
    }
}

@Preview(showBackground = true, name = "HorizontalStatsSection — empty", widthDp = 360, heightDp = 80)
@Composable
private fun HorizontalEmptyPreview() {
    StatPreviewSurface(isDark = false) {
        HorizontalStatsSection<GenreStat>(
            title = "Top genres",
            items = emptyList()
        ) { GenreCardModern(it) }
    }
}

@Preview(showBackground = true, name = "HorizontalStatsSection — dark", widthDp = 360)
@Composable
private fun HorizontalDarkPreview() {
    StatPreviewSurface(isDark = true) {
        HorizontalStatsSection(
            title = "Top genres",
            items = previewGenres,
            key = { it.genre }
        ) { GenreCardModern(it) }
    }
}

// endregion
