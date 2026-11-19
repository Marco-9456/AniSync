package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.result.ResultGroupHeader
import com.anisync.android.presentation.util.bouncyClickable

/**
 * What an empty search field offers: the searches run before, newest first. A tap runs one again,
 * a row's cross forgets it and Clear all forgets them all. Rows tile at the results' width on a
 * wide window.
 */
@Composable
fun RecentSearches(
    searches: List<String>,
    onSearch: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(RowMinWidth),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize().imePadding()
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
            ResultGroupHeader(
                title = stringResource(R.string.search_recent_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                action = {
                    HeaderAction(
                        label = stringResource(R.string.search_recent_clear_all),
                        description = stringResource(R.string.a11y_search_recent_clear_all),
                        onClick = onClearAll
                    )
                }
            )
        }
        items(searches, key = { it }, contentType = { "recent" }) { query ->
            RecentSearchRow(
                query = query,
                onSearch = { onSearch(query) },
                onRemove = { onRemove(query) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun RecentSearchRow(
    query: String,
    onSearch: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .bouncyClickable(
                onClick = onSearch,
                onClickLabel = stringResource(R.string.a11y_action_search_again),
                clipShape = RowShape
            )
            .padding(start = 16.dp, end = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_search_recent_remove, query),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val RowMinWidth = 360.dp
private val RowShape = RoundedCornerShape(16.dp)
