package com.anisync.android.presentation.feed.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.FeedFilter
import com.anisync.android.domain.FeedMediaType
import com.anisync.android.domain.FeedScope
import com.anisync.android.presentation.components.SegmentedTabItem
import com.anisync.android.presentation.util.rememberHapticFeedback

private sealed interface FilterRowEntry {
    data class Filter(val value: FeedFilter) : FilterRowEntry
    data object Separator : FilterRowEntry
    data class Media(val value: FeedMediaType) : FilterRowEntry
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeedFilterBar(
    filter: FeedFilter,
    scope: FeedScope,
    mediaType: FeedMediaType,
    onFilterChange: (FeedFilter) -> Unit,
    onScopeChange: (FeedScope) -> Unit,
    onMediaTypeChange: (FeedMediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Global / Following toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            ToggleButton(
                checked = scope == FeedScope.GLOBAL,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onScopeChange(FeedScope.GLOBAL)
                },
                modifier = Modifier.weight(1f),
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
            ) {
                Icon(imageVector = Icons.Default.Public, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.feed_scope_global),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            ToggleButton(
                checked = scope == FeedScope.FOLLOWING,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onScopeChange(FeedScope.FOLLOWING)
                },
                modifier = Modifier.weight(1f),
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
            ) {
                Icon(imageVector = Icons.Default.Group, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.feed_scope_following),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Row 2: filter chips (All / Status / List · Anime / Manga)
        val entries: List<FilterRowEntry> = remember(filter) {
            buildList {
                FeedFilter.entries.forEach { add(FilterRowEntry.Filter(it)) }
                if (filter == FeedFilter.LIST) {
                    add(FilterRowEntry.Separator)
                    add(FilterRowEntry.Media(FeedMediaType.ANIME))
                    add(FilterRowEntry.Media(FeedMediaType.MANGA))
                }
            }
        }

        val filterIndex = remember(filter) {
            FeedFilter.entries.indexOf(filter).coerceAtLeast(0)
        }
        val mediaIndex = remember(mediaType) {
            FeedMediaType.entries.indexOf(mediaType).coerceAtLeast(0)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(
                items = entries,
                key = { _, entry ->
                    when (entry) {
                        is FilterRowEntry.Filter -> "filter_${entry.value.name}"
                        FilterRowEntry.Separator -> "separator"
                        is FilterRowEntry.Media -> "media_${entry.value.name}"
                    }
                }
            ) { _, entry ->
                when (entry) {
                    is FilterRowEntry.Filter -> {
                        val idx = FeedFilter.entries.indexOf(entry.value)
                        SegmentedTabItem(
                            index = idx,
                            selectedIndex = filterIndex,
                            selected = entry.value == filter,
                            onClick = { onFilterChange(entry.value) },
                            icon = filterIcon(entry.value),
                            label = stringResource(filterLabelRes(entry.value))
                        )
                    }
                    FilterRowEntry.Separator -> {
                        Text(
                            text = stringResource(R.string.separator_bullet),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    is FilterRowEntry.Media -> {
                        val idx = FeedMediaType.entries.indexOf(entry.value)
                        SegmentedTabItem(
                            index = idx,
                            selectedIndex = mediaIndex,
                            selected = entry.value == mediaType,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onMediaTypeChange(entry.value)
                            },
                            icon = mediaIcon(entry.value),
                            label = stringResource(mediaLabelRes(entry.value))
                        )
                    }
                }
            }
        }
    }
}

private fun filterIcon(filter: FeedFilter): ImageVector = when (filter) {
    FeedFilter.ALL -> Icons.Default.DynamicFeed
    FeedFilter.STATUS -> Icons.AutoMirrored.Outlined.Notes
    FeedFilter.LIST -> Icons.AutoMirrored.Filled.ViewList
}

private fun filterLabelRes(filter: FeedFilter): Int = when (filter) {
    FeedFilter.ALL -> R.string.feed_filter_all
    FeedFilter.STATUS -> R.string.feed_filter_status
    FeedFilter.LIST -> R.string.feed_filter_list
}

private fun mediaIcon(mediaType: FeedMediaType): ImageVector = when (mediaType) {
    FeedMediaType.ANIME -> Icons.Default.Tv
    FeedMediaType.MANGA -> Icons.AutoMirrored.Filled.MenuBook
}

private fun mediaLabelRes(mediaType: FeedMediaType): Int = when (mediaType) {
    FeedMediaType.ANIME -> R.string.media_type_anime
    FeedMediaType.MANGA -> R.string.media_type_manga
}
