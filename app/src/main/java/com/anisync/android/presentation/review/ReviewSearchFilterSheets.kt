package com.anisync.android.presentation.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.ReviewSortOption
import com.anisync.android.domain.SearchResult
import com.anisync.android.presentation.components.filtersheet.AuthorPickerRow
import com.anisync.android.presentation.components.filtersheet.FilterOptionRow
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.components.filtersheet.MediaPickerRow
import com.anisync.android.presentation.components.filtersheet.PickerResults
import com.anisync.android.presentation.components.filtersheet.PickerSearchField
import com.anisync.android.type.MediaType

/**
 * Hosts the recent-reviews filter sheets. Only one is shown at a time, keyed by
 * [opened]; mirrors the forum [com.anisync.android.presentation.forum.components.ForumSearchFilterSheetHost]
 * and is built on the same shared [FilterSheetScaffold]/[FilterOptionRow] and
 * picker primitives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSearchFilterSheetHost(
    opened: ReviewFilterId?,
    sort: ReviewSortOption,
    media: LibraryEntry?,
    author: SearchResult.UserResult?,
    mediaPickerType: MediaType,
    mediaPickerQuery: String,
    mediaPickerResults: List<LibraryEntry>,
    isMediaPickerSearching: Boolean,
    authorPickerQuery: String,
    authorPickerResults: List<SearchResult.UserResult>,
    isAuthorPickerSearching: Boolean,
    pickerError: String?,
    onSortChange: (ReviewSortOption) -> Unit,
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

        ReviewFilterId.SORT -> FilterSheetScaffold(title = "Sort reviews", onDismiss = onDismiss) {
            ReviewSortOption.entries.forEach { option ->
                FilterOptionRow(
                    label = option.shortLabel(),
                    selected = sort == option,
                    onClick = {
                        onSortChange(option)
                        onDismiss()
                    }
                )
            }
        }

        ReviewFilterId.MEDIA -> FilterSheetScaffold(
            title = "Filter by media",
            onDismiss = onDismiss,
            onReset = {
                onClearMedia()
                onDismiss()
            },
            resetEnabled = media != null
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
                            selected = media?.mediaId == entry.mediaId,
                            onClick = {
                                onSelectMedia(entry)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }

        ReviewFilterId.AUTHOR -> FilterSheetScaffold(
            title = "Filter by author",
            onDismiss = onDismiss,
            onReset = {
                onClearAuthor()
                onDismiss()
            },
            resetEnabled = author != null
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
                            selected = author?.id == user.id,
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
