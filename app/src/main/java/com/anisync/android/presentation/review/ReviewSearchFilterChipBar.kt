package com.anisync.android.presentation.review

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.ReviewSortOption
import com.anisync.android.domain.SearchResult

/** Identifies which review filter sheet the user opened from the chip bar. */
enum class ReviewFilterId { SORT, MEDIA, AUTHOR }

/**
 * Horizontal chip bar for filtering the expanded recent-reviews list. Mirrors the
 * forum advanced-search chip bar: Sort / Media / Author open a bottom sheet. The
 * leading All / Anime / Manga toggle predates the sheet filters and is hidden once
 * a specific [media] is chosen (the media already fixes the type).
 */
@Composable
fun ReviewSearchFilterChipBar(
    filter: ReviewMediaFilter,
    sort: ReviewSortOption,
    media: LibraryEntry?,
    author: SearchResult.UserResult?,
    onTypeSelect: (ReviewMediaFilter) -> Unit,
    onChipTap: (ReviewFilterId) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (media == null) {
            TypeChip(ReviewMediaFilter.ALL, filter, R.string.reviews_filter_all, onTypeSelect)
            TypeChip(ReviewMediaFilter.ANIME, filter, R.string.media_type_anime, onTypeSelect)
            TypeChip(ReviewMediaFilter.MANGA, filter, R.string.media_type_manga, onTypeSelect)
        }

        SheetChip(
            label = "Sort · ${sort.shortLabel()}",
            active = sort != ReviewSortOption.Default,
            onClick = { onChipTap(ReviewFilterId.SORT) }
        )
        SheetChip(
            label = media?.titleUserPreferred?.let { "Media · $it" } ?: "Media",
            active = media != null,
            onClick = { onChipTap(ReviewFilterId.MEDIA) }
        )
        SheetChip(
            label = author?.displayName?.let { "Author · $it" } ?: "Author",
            active = author != null,
            onClick = { onChipTap(ReviewFilterId.AUTHOR) }
        )
    }
}

@Composable
private fun TypeChip(
    value: ReviewMediaFilter,
    selected: ReviewMediaFilter,
    @StringRes labelRes: Int,
    onSelect: (ReviewMediaFilter) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(stringResource(labelRes)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun SheetChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/** Human-readable label for a review sort option, used by the chip + sort sheet. */
internal fun ReviewSortOption.shortLabel(): String = when (this) {
    ReviewSortOption.NEWEST -> "Newest"
    ReviewSortOption.OLDEST -> "Oldest"
    ReviewSortOption.HIGHEST_SCORE -> "Highest score"
    ReviewSortOption.LOWEST_SCORE -> "Lowest score"
    ReviewSortOption.MOST_LIKED -> "Most liked"
    ReviewSortOption.RECENTLY_UPDATED -> "Recently updated"
}
