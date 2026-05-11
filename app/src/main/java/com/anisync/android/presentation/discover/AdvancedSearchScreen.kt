package com.anisync.android.presentation.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.OriginCountry
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SortOption
import com.anisync.android.presentation.discover.components.IncludeExcludeChip
import com.anisync.android.presentation.discover.components.RangeSliderWithLabels
import com.anisync.android.presentation.discover.components.TagPickerSheet
import com.anisync.android.presentation.discover.components.TriState
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaSource
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedSearchScreen(
    onApplied: () -> Unit,
    onClose: () -> Unit,
    viewModel: AdvancedSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (!state.initialized) {
            viewModel.initializeIfNeeded(SearchFilters(), "", MediaType.ANIME)
        }
    }
    val showAdult by viewModel.showAdultContent.collectAsStateWithLifecycle()
    val draft = state.draft
    val update: (SearchFilters) -> Unit = { viewModel.onAction(AdvancedSearchAction.UpdateFilters(it)) }
    var showTagPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filters") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    val active = draft.activeFilterCount
                    TextButton(
                        onClick = { viewModel.onAction(AdvancedSearchAction.ResetFilters) },
                        enabled = active > 0
                    ) {
                        Text(
                            text = if (active > 0) "Reset ($active)" else "Reset",
                            color = if (active > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ApplyBar(
                isLoading = state.isLoadingCount,
                count = state.liveCount,
                onApply = {
                    viewModel.submit()
                    onApplied()
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                FilterSectionHeader("Sort")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortOption.entries.forEach { option ->
                        val selected = draft.sort == option
                        FilterChip(
                            selected = selected,
                            onClick = { update(draft.copy(sort = option)) },
                            label = { Text(option.label()) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            item {
                FilterSectionHeader(
                    title = "Genres",
                    hint = "Tap to include · Long-press to exclude"
                )
                if (state.genres.isEmpty()) {
                    Text(
                        text = "Loading genres…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.genres.forEach { genre ->
                            val tri = when {
                                genre in draft.genresIncluded -> TriState.INCLUDED
                                genre in draft.genresExcluded -> TriState.EXCLUDED
                                else -> TriState.OFF
                            }
                            IncludeExcludeChip(
                                label = genre,
                                state = tri,
                                onStateChange = { newState ->
                                    val newIn = when (newState) {
                                        TriState.INCLUDED -> draft.genresIncluded + genre
                                        else -> draft.genresIncluded - genre
                                    }
                                    val newOut = when (newState) {
                                        TriState.EXCLUDED -> draft.genresExcluded + genre
                                        else -> draft.genresExcluded - genre
                                    }
                                    update(draft.copy(genresIncluded = newIn, genresExcluded = newOut))
                                }
                            )
                        }
                    }
                }
            }

            item {
                FilterSectionHeader(
                    title = "Tags",
                    hint = "Browse 200+ tags · Tap to include · Long-press to exclude"
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTagPicker = true },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val total = draft.tagsIncluded.size + draft.tagsExcluded.size
                            Text(
                                text = if (total == 0) "Browse tags" else "$total tag${if (total == 1) "" else "s"} selected",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            if (total > 0) {
                                Text(
                                    text = "${draft.tagsIncluded.size} included · ${draft.tagsExcluded.size} excluded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open tag picker"
                        )
                    }
                }
                if (draft.tagsIncluded.isNotEmpty() || draft.tagsExcluded.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        draft.tagsIncluded.take(8).forEach { tag ->
                            AssistChip(
                                onClick = {
                                    update(draft.copy(tagsIncluded = draft.tagsIncluded - tag))
                                },
                                label = { Text(tag) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        draft.tagsExcluded.take(8).forEach { tag ->
                            AssistChip(
                                onClick = {
                                    update(draft.copy(tagsExcluded = draft.tagsExcluded - tag))
                                },
                                label = { Text(tag) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    labelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
                }
            }

            item {
                FilterSectionHeader("Year")
                val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
                RangeSliderWithLabels(
                    value = draft.yearRange,
                    bounds = 1940..(currentYear + 1),
                    step = 1,
                    onValueChange = { update(draft.copy(yearRange = it)) }
                )
            }

            item {
                FilterSectionHeader("Score")
                RangeSliderWithLabels(
                    value = draft.scoreRange,
                    bounds = 0..100,
                    step = 5,
                    onValueChange = { update(draft.copy(scoreRange = it)) }
                )
            }

            item {
                val isAnime = state.mediaType == MediaType.ANIME
                FilterSectionHeader(if (isAnime) "Episodes" else "Chapters")
                val targetRange = if (isAnime) draft.episodesRange else draft.chaptersRange
                val bound = if (isAnime) 0..150 else 0..500
                RangeSliderWithLabels(
                    value = targetRange,
                    bounds = bound,
                    step = if (isAnime) 1 else 5,
                    onValueChange = {
                        update(
                            if (isAnime) draft.copy(episodesRange = it)
                            else draft.copy(chaptersRange = it)
                        )
                    }
                )
            }

            item {
                FilterSectionHeader("Format")
                val available = if (state.mediaType == MediaType.ANIME) {
                    listOf(
                        MediaFormat.TV, MediaFormat.TV_SHORT, MediaFormat.MOVIE,
                        MediaFormat.SPECIAL, MediaFormat.OVA, MediaFormat.ONA, MediaFormat.MUSIC
                    )
                } else {
                    listOf(MediaFormat.MANGA, MediaFormat.NOVEL, MediaFormat.ONE_SHOT)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    available.forEach { format ->
                        val selected = format in draft.formats
                        FilterChip(
                            selected = selected,
                            onClick = {
                                update(
                                    draft.copy(
                                        formats = if (selected) draft.formats - format
                                        else draft.formats + format
                                    )
                                )
                            },
                            label = { Text(format.toLabel()) }
                        )
                    }
                }
            }

            item {
                FilterSectionHeader("Status")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaStatus.entries
                        .filter { it != MediaStatus.UNKNOWN__ }
                        .forEach { status ->
                            val selected = status in draft.statuses
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    update(
                                        draft.copy(
                                            statuses = if (selected) draft.statuses - status
                                            else draft.statuses + status
                                        )
                                    )
                                },
                                label = { Text(status.toLabel()) }
                            )
                        }
                }
            }

            item {
                FilterSectionHeader("Season")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaSeason.entries
                        .filter { it != MediaSeason.UNKNOWN__ }
                        .forEach { season ->
                            val selected = draft.season == season
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    update(draft.copy(season = if (selected) null else season))
                                },
                                label = { Text(season.label()) }
                            )
                        }
                }
            }

            item {
                FilterSectionHeader("Source")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaSource.entries
                        .filter { it != MediaSource.UNKNOWN__ }
                        .forEach { source ->
                            val selected = source in draft.sources
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    update(
                                        draft.copy(
                                            sources = if (selected) draft.sources - source
                                            else draft.sources + source
                                        )
                                    )
                                },
                                label = { Text(source.label()) }
                            )
                        }
                }
            }

            item {
                FilterSectionHeader("Country of origin")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OriginCountry.entries.forEach { country ->
                        val selected = draft.country == country
                        FilterChip(
                            selected = selected,
                            onClick = {
                                update(draft.copy(country = if (selected) null else country))
                            },
                            label = { Text(country.displayName) }
                        )
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            item {
                FilterSectionHeader("Adult content")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show NSFW tags & filters",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Reveals 18+ tags in the picker and the adult-only filter below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showAdult,
                            onCheckedChange = {
                                viewModel.onAction(AdvancedSearchAction.SetAdultContent(it))
                            }
                        )
                    }
                }
                if (showAdult) {
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = draft.onlyAdult == null,
                            onClick = { update(draft.copy(onlyAdult = null)) },
                            label = { Text("Any") }
                        )
                        FilterChip(
                            selected = draft.onlyAdult == false,
                            onClick = { update(draft.copy(onlyAdult = false)) },
                            label = { Text("Hide adult") }
                        )
                        FilterChip(
                            selected = draft.onlyAdult == true,
                            onClick = { update(draft.copy(onlyAdult = true)) },
                            label = { Text("Adult only") }
                        )
                    }
                }
            }

            items(items = listOf(Unit), key = { "footer" }) {
                Box(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showTagPicker) {
        TagPickerSheet(
            tags = state.tags,
            included = draft.tagsIncluded,
            excluded = draft.tagsExcluded,
            showAdult = showAdult,
            onSelectionChange = { inc, exc ->
                update(draft.copy(tagsIncluded = inc, tagsExcluded = exc))
            },
            onDismiss = { showTagPicker = false }
        )
    }
}

@Composable
private fun FilterSectionHeader(
    title: String,
    hint: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ApplyBar(
    isLoading: Boolean,
    count: Int?,
    onApply: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onApply,
                enabled = count != 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    val label = when {
                        isLoading && count == null -> "Counting…"
                        count == null -> "Show results"
                        count == 0 -> "No matches"
                        else -> "Show $count result${if (count == 1) "" else "s"}"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun SortOption.label(): String = when (this) {
    SortOption.POPULARITY_DESC -> "Popularity"
    SortOption.SCORE_DESC -> "Score"
    SortOption.TRENDING_DESC -> "Trending"
    SortOption.FAVOURITES_DESC -> "Favourites"
    SortOption.START_DATE_DESC -> "Newest"
    SortOption.START_DATE -> "Oldest"
    SortOption.UPDATED_AT_DESC -> "Recently updated"
    SortOption.TITLE_ROMAJI -> "Title (Romaji)"
    SortOption.TITLE_ENGLISH -> "Title (English)"
    SortOption.EPISODES_DESC -> "Most episodes"
    SortOption.DURATION_DESC -> "Longest"
    SortOption.CHAPTERS_DESC -> "Most chapters"
    SortOption.VOLUMES_DESC -> "Most volumes"
}

private fun MediaSeason.label(): String = when (this) {
    MediaSeason.WINTER -> "Winter"
    MediaSeason.SPRING -> "Spring"
    MediaSeason.SUMMER -> "Summer"
    MediaSeason.FALL -> "Fall"
    MediaSeason.UNKNOWN__ -> ""
}

private fun MediaSource.label(): String = when (this) {
    MediaSource.ORIGINAL -> "Original"
    MediaSource.MANGA -> "Manga"
    MediaSource.LIGHT_NOVEL -> "Light Novel"
    MediaSource.VISUAL_NOVEL -> "Visual Novel"
    MediaSource.VIDEO_GAME -> "Video Game"
    MediaSource.NOVEL -> "Novel"
    MediaSource.DOUJINSHI -> "Doujinshi"
    MediaSource.ANIME -> "Anime"
    MediaSource.WEB_NOVEL -> "Web Novel"
    MediaSource.LIVE_ACTION -> "Live Action"
    MediaSource.GAME -> "Game"
    MediaSource.COMIC -> "Comic"
    MediaSource.MULTIMEDIA_PROJECT -> "Multimedia Project"
    MediaSource.PICTURE_BOOK -> "Picture Book"
    MediaSource.OTHER -> "Other"
    MediaSource.UNKNOWN__ -> ""
}
