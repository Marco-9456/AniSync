package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anisync.android.R
import com.anisync.android.domain.AVAILABLE_GENRES
import com.anisync.android.domain.SearchFilters
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType
import java.util.Calendar

/**
 * Search Filter Dialog
 * Replaces the Bottom Sheet with a centered, floating dialog.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterDialog(
    filters: SearchFilters,
    mediaType: MediaType,
    onFiltersChanged: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Slightly narrower than screen
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh, // Slightly higher elevation color
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // We keep the button in the layout (invisible) to prevent height jumps
                    val hasFilters = filters.hasActiveFilters
                    TextButton(
                        onClick = { onFiltersChanged(SearchFilters()) },
                        enabled = hasFilters,
                        modifier = Modifier.alpha(if (hasFilters) 1f else 0f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.filter_clear_all))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f) // Takes remaining space
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {

                    // Genres Section (FlowRow)
                    FilterSection(title = stringResource(R.string.filter_genres)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AVAILABLE_GENRES.forEach { genre ->
                                val selected = genre in filters.genres
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val newGenres = if (selected) filters.genres - genre else filters.genres + genre
                                        onFiltersChanged(filters.copy(genres = newGenres))
                                    },
                                    label = { Text(genre) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Season Section
                    FilterSection(title = stringResource(R.string.filter_season)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaSeason.entries.filter { it != MediaSeason.UNKNOWN__ }.forEach { season ->
                                val selected = filters.season == season
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onFiltersChanged(filters.copy(season = if (selected) null else season))
                                    },
                                    label = { Text(getSeasonLabel(season)) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Year Section (Horizontal Scroll is better for timeline/numbers)
                    FilterSection(title = stringResource(R.string.filter_year)) {
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val years = (currentYear downTo 1970).toList()
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            items(years) { year ->
                                val selected = filters.year == year
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onFiltersChanged(filters.copy(year = if (selected) null else year))
                                    },
                                    label = { Text(year.toString()) },
                                    border = if (selected) null else FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    // Format Section
                    FilterSection(title = stringResource(R.string.filter_format)) {
                        val availableFormats = if (mediaType == MediaType.ANIME) {
                            listOf(MediaFormat.TV, MediaFormat.TV_SHORT, MediaFormat.MOVIE, MediaFormat.SPECIAL, MediaFormat.OVA, MediaFormat.ONA, MediaFormat.MUSIC)
                        } else {
                            listOf(MediaFormat.MANGA, MediaFormat.NOVEL, MediaFormat.ONE_SHOT)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableFormats.forEach { format ->
                                val selected = format in filters.formats
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val newFormats = if (selected) filters.formats - format else filters.formats + format
                                        onFiltersChanged(filters.copy(formats = newFormats))
                                    },
                                    label = { Text(format.toLabel()) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Status Section
                    FilterSection(title = stringResource(R.string.filter_status)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaStatus.entries.filter { it != MediaStatus.UNKNOWN__ }.forEach { status ->
                                val selected = filters.status == status
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onFiltersChanged(filters.copy(status = if (selected) null else status))
                                    },
                                    label = { Text(status.toLabel()) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.filter_show_results),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun getSeasonLabel(season: MediaSeason): String {
    return when (season) {
        MediaSeason.WINTER -> stringResource(R.string.season_winter)
        MediaSeason.SPRING -> stringResource(R.string.season_spring)
        MediaSeason.SUMMER -> stringResource(R.string.season_summer)
        MediaSeason.FALL -> stringResource(R.string.season_fall)
        MediaSeason.UNKNOWN__ -> ""
    }
}

/**
 * Horizontally scrollable row of filter chips for media format selection.
 * Shows different options based on whether the current media type is Anime or Manga.
 */
@Composable
fun SearchFiltersRow(
    mediaType: MediaType,
    selectedFormat: MediaFormat?,
    onFormatSelected: (MediaFormat?) -> Unit,
    modifier: Modifier = Modifier
) {
    val formats = when (mediaType) {
        MediaType.ANIME -> listOf(
            MediaFormat.TV,
            MediaFormat.MOVIE,
            MediaFormat.OVA,
            MediaFormat.SPECIAL,
            MediaFormat.ONA
        )
        MediaType.MANGA -> listOf(
            MediaFormat.MANGA,
            MediaFormat.NOVEL,
            MediaFormat.ONE_SHOT
        )
        else -> emptyList()
    }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        FilterChip(
            selected = selectedFormat == null,
            onClick = { onFormatSelected(null) },
            label = { Text(stringResource(R.string.all)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Format-specific chips
        formats.forEach { format ->
            FilterChip(
                selected = selectedFormat == format,
                onClick = { onFormatSelected(format) },
                label = { Text(format.toDisplayString()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * Convert MediaFormat enum to user-friendly display string.
 */
private fun MediaFormat.toDisplayString(): String {
    return when (this) {
        MediaFormat.TV -> "TV"
        MediaFormat.TV_SHORT -> "TV Short"
        MediaFormat.MOVIE -> "Movie"
        MediaFormat.SPECIAL -> "Special"
        MediaFormat.OVA -> "OVA"
        MediaFormat.ONA -> "ONA"
        MediaFormat.MUSIC -> "Music"
        MediaFormat.MANGA -> "Manga"
        MediaFormat.NOVEL -> "Novel"
        MediaFormat.ONE_SHOT -> "One-shot"
        else -> this.name
    }
}
