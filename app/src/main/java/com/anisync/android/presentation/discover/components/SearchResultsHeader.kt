package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.data.DiscoverViewMode
import com.anisync.android.presentation.discover.ResultCategory

/**
 * Strip rendered above search results. Left side: scrollable category chips
 * with empty categories hidden, so the user can't tap into a section that
 * returned nothing. Right side: list/grid toggle.
 */
@Composable
fun SearchResultsHeader(
    activeCategory: ResultCategory,
    availableCategories: Set<ResultCategory>,
    viewMode: DiscoverViewMode,
    onCategoryChange: (ResultCategory) -> Unit,
    onViewModeChange: (DiscoverViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedCategories = listOf(
        ResultCategory.ALL,
        ResultCategory.ANIME,
        ResultCategory.MANGA,
        ResultCategory.CHARACTERS,
        ResultCategory.STAFF,
        ResultCategory.USERS,
        ResultCategory.STUDIOS
    ).filter { it in availableCategories }

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(start = 8.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            orderedCategories.forEach { category ->
                FilterChip(
                    selected = activeCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category.label()) }
                )
            }
        }
        ViewModeToggle(
            viewMode = viewMode,
            onViewModeChange = onViewModeChange
        )
    }
}

@Composable
private fun ViewModeToggle(
    viewMode: DiscoverViewMode,
    onViewModeChange: (DiscoverViewMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(
            onClick = { onViewModeChange(DiscoverViewMode.LIST) },
            modifier = Modifier.size(36.dp),
            colors = if (viewMode == DiscoverViewMode.LIST) {
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else IconButtonDefaults.iconButtonColors()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = "List view",
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = { onViewModeChange(DiscoverViewMode.GRID) },
            modifier = Modifier.size(36.dp),
            colors = if (viewMode == DiscoverViewMode.GRID) {
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else IconButtonDefaults.iconButtonColors()
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Grid view",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun ResultCategory.label(): String = when (this) {
    ResultCategory.ALL -> "All"
    ResultCategory.ANIME -> "Anime"
    ResultCategory.MANGA -> "Manga"
    ResultCategory.CHARACTERS -> "Characters"
    ResultCategory.STAFF -> "Staff"
    ResultCategory.USERS -> "Users"
    ResultCategory.STUDIOS -> "Studios"
}
