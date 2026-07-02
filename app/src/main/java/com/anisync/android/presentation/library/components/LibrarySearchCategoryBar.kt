package com.anisync.android.presentation.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.libraryTabLabel
import com.anisync.android.type.MediaType

/**
 * Discover-style category strip shown above library search results: a horizontally scrollable row of
 * FilterChips — one per list that has at least one match, plus an always-present "All" — each showing
 * its match count. Tapping a chip scopes the results to that list. Modeled on the Discover screen's
 * `SearchResultsHeader` (#91).
 *
 * @param categories ordered `(tabId, matchCount)` pairs; the caller puts "All" first and omits empty
 *   lists so the user can't tap into a section that returned nothing.
 */
@Composable
fun LibrarySearchCategoryBar(
    activeCategory: String,
    categories: List<Pair<String, Int>>,
    mediaType: MediaType,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (id, count) ->
            FilterChip(
                selected = activeCategory == id,
                onClick = { onCategoryChange(id) },
                label = { Text("${libraryTabLabel(id, mediaType)} $count") }
            )
        }
    }
}
