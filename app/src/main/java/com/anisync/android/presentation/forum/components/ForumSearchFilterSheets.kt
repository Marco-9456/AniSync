package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.domain.ForumCategory
import com.anisync.android.domain.ForumSearchFilters
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchResult
import com.anisync.android.domain.ThreadSortOption
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.filtersheet.AuthorPickerRow
import com.anisync.android.presentation.components.filtersheet.FilterOptionRow
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.components.filtersheet.MediaPickerRow
import com.anisync.android.presentation.components.filtersheet.PickerResults
import com.anisync.android.presentation.components.filtersheet.PickerSearchField
import com.anisync.android.presentation.forum.defaultCategories
import com.anisync.android.type.MediaType

/**
 * Hosts the advanced forum search filter sheets. Only one is shown at a time,
 * keyed by [opened]; mirrors the Discover SearchFilterSheetHost pattern and is
 * built on the shared [FilterSheetScaffold]/[FilterOptionRow] primitives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumSearchFilterSheetHost(
    opened: ForumFilterId?,
    filters: ForumSearchFilters,
    mediaPickerType: MediaType,
    mediaPickerQuery: String,
    mediaPickerResults: List<LibraryEntry>,
    isMediaPickerSearching: Boolean,
    authorPickerQuery: String,
    authorPickerResults: List<SearchResult.UserResult>,
    isAuthorPickerSearching: Boolean,
    pickerError: String?,
    onSortChange: (ThreadSortOption) -> Unit,
    onCategoryChange: (ForumCategory?) -> Unit,
    onMediaPickerTypeChange: (MediaType) -> Unit,
    onMediaPickerQueryChange: (String) -> Unit,
    onSelectMedia: (LibraryEntry) -> Unit,
    onClearMedia: () -> Unit,
    onAuthorPickerQueryChange: (String) -> Unit,
    onSelectAuthor: (SearchResult.UserResult) -> Unit,
    onClearAuthor: () -> Unit,
    onDismiss: () -> Unit
) {
    when (opened) {
        null -> Unit

        ForumFilterId.SORT -> FilterSheetScaffold(title = "Sort by", onDismiss = onDismiss) {
            ThreadSortOption.entries.forEach { option ->
                FilterOptionRow(
                    label = option.shortLabel(),
                    selected = filters.sort == option,
                    onClick = {
                        onSortChange(option)
                        onDismiss()
                    }
                )
            }
        }

        ForumFilterId.CATEGORY -> FilterSheetScaffold(
            title = "Forum category",
            onDismiss = onDismiss,
            onReset = {
                onCategoryChange(null)
                onDismiss()
            },
            resetEnabled = filters.category != null
        ) {
            FilterOptionRow(
                label = "All categories",
                selected = filters.category == null,
                onClick = {
                    onCategoryChange(null)
                    onDismiss()
                }
            )
            defaultCategories.forEach { category ->
                FilterOptionRow(
                    label = category.name,
                    selected = filters.category?.id == category.id,
                    onClick = {
                        onCategoryChange(category)
                        onDismiss()
                    }
                )
            }
        }

        ForumFilterId.MEDIA -> FilterSheetScaffold(
            title = "Filter by media",
            onDismiss = onDismiss,
            onReset = {
                onClearMedia()
                onDismiss()
            },
            resetEnabled = filters.media != null
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mediaPickerType == MediaType.ANIME,
                        onClick = { onMediaPickerTypeChange(MediaType.ANIME) },
                        label = { Text("Anime") }
                    )
                    FilterChip(
                        selected = mediaPickerType == MediaType.MANGA,
                        onClick = { onMediaPickerTypeChange(MediaType.MANGA) },
                        label = { Text("Manga") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                PickerSearchField(
                    query = mediaPickerQuery,
                    placeholder = "Search media…",
                    isSearching = isMediaPickerSearching,
                    onQueryChange = onMediaPickerQueryChange
                )
                Spacer(Modifier.height(12.dp))
                PickerResults(
                    error = pickerError,
                    query = mediaPickerQuery,
                    isEmpty = mediaPickerResults.isEmpty(),
                    isSearching = isMediaPickerSearching,
                    emptyText = "No media found"
                ) {
                    mediaPickerResults.forEach { entry ->
                        MediaPickerRow(
                            entry = entry,
                            selected = filters.media?.mediaId == entry.mediaId,
                            onClick = {
                                onSelectMedia(entry)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }

        ForumFilterId.AUTHOR -> FilterSheetScaffold(
            title = "Filter by author",
            onDismiss = onDismiss,
            onReset = {
                onClearAuthor()
                onDismiss()
            },
            resetEnabled = filters.author != null
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                PickerSearchField(
                    query = authorPickerQuery,
                    placeholder = "Search users…",
                    isSearching = isAuthorPickerSearching,
                    onQueryChange = onAuthorPickerQueryChange
                )
                Spacer(Modifier.height(12.dp))
                PickerResults(
                    error = pickerError,
                    query = authorPickerQuery,
                    isEmpty = authorPickerResults.isEmpty(),
                    isSearching = isAuthorPickerSearching,
                    emptyText = "No users found"
                ) {
                    authorPickerResults.forEach { user ->
                        AuthorPickerRow(
                            user = user,
                            selected = filters.author?.id == user.id,
                            onClick = {
                                onSelectAuthor(user)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header card shown above the search results when a media filter is active.
 * Surfaces the picked media and a shortcut to start a thread tied to it
 * (pre-filling the create-thread media picker).
 */
@Composable
fun ForumMediaFilterHeader(
    media: LibraryEntry,
    onCreateThread: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = media.cover.url() ?: media.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 40.dp, height = 56.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Discussions about",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = media.titleUserPreferred,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear media filter")
                }
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onCreateThread,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Start a discussion about this")
            }
        }
    }
}
