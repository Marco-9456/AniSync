package com.anisync.android.presentation.components.filtersheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchResult
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.UserAvatar

/**
 * Shared building blocks for the media/author picker filter sheets. Used by both
 * the forum advanced search and the recent-reviews filters so the picker UI stays
 * identical across features. See [FilterSheetScaffold]/[FilterOptionRow] for the
 * surrounding sheet chrome.
 */

/** Rounded search field with a leading magnifier and trailing progress spinner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSearchField(
    query: String,
    placeholder: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (isSearching) {
            { AppCircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
        } else null,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

/**
 * Renders the picker result region: an error, an empty-state message (once a
 * non-blank query has settled), or the [content] list of result rows.
 */
@Composable
fun PickerResults(
    error: String?,
    query: String,
    isEmpty: Boolean,
    isSearching: Boolean,
    emptyText: String,
    content: @Composable () -> Unit
) {
    when {
        error != null -> Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )

        query.isNotBlank() && isEmpty && !isSearching -> Text(
            text = emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** Selectable media result row: cover thumbnail + preferred title. */
@Composable
fun MediaPickerRow(
    entry: LibraryEntry,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = entry.cover.url() ?: entry.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = entry.titleUserPreferred,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Selectable user result row: avatar + display name. */
@Composable
fun AuthorPickerRow(
    user: SearchResult.UserResult,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                url = user.imageUrl,
                contentDescription = user.displayName,
                size = 40.dp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
