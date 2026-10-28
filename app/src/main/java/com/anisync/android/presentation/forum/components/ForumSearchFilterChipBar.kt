package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ForumSearchFilters
import com.anisync.android.domain.ThreadSortOption

/** Identifies which forum filter sheet the user opened from the chip bar. */
enum class ForumFilterId { SORT, CATEGORY, MEDIA, AUTHOR }

/**
 * Horizontal chip bar for the advanced forum search. Sort / Category / Media /
 * Author open a bottom sheet; Subscribed is a direct toggle (auth-gated upstream
 * — AniList only returns subscribed threads for the signed-in user).
 */
@Composable
fun ForumSearchFilterChipBar(
    filters: ForumSearchFilters,
    onChipTap: (ForumFilterId) -> Unit,
    onToggleSubscribed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SheetChip(
            label = stringResource(R.string.library_filter_chip_sort, filters.sort.shortLabel()),
            active = filters.sort != ThreadSortOption.Default,
            onClick = { onChipTap(ForumFilterId.SORT) }
        )
        SheetChip(
            label = filters.category?.name?.let { "Category · $it" } ?: "Category",
            active = filters.category != null,
            onClick = { onChipTap(ForumFilterId.CATEGORY) }
        )
        SheetChip(
            label = filters.media?.titleUserPreferred?.let { "Media · $it" } ?: "Media",
            active = filters.media != null,
            onClick = { onChipTap(ForumFilterId.MEDIA) }
        )
        SheetChip(
            label = filters.author?.displayName?.let { "Author · $it" } ?: "Author",
            active = filters.author != null,
            onClick = { onChipTap(ForumFilterId.AUTHOR) }
        )
        FilterChip(
            selected = filters.subscribedOnly,
            onClick = onToggleSubscribed,
            label = { Text(stringResource(R.string.forum_filter_subscribed)) },
            leadingIcon = if (filters.subscribedOnly) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null
        )
    }
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

/** Human-readable label for a thread sort option, used by the chip + sort sheet. */
@Composable
internal fun ThreadSortOption.shortLabel(): String = stringResource(
    when (this) {
        ThreadSortOption.RECENTLY_REPLIED -> R.string.forum_sort_field_last_reply
        ThreadSortOption.LEAST_RECENTLY_REPLIED -> R.string.forum_sort_least_recently_replied
        ThreadSortOption.NEWEST -> R.string.forum_sort_newest_short
        ThreadSortOption.OLDEST -> R.string.forum_sort_oldest_short
        ThreadSortOption.MOST_REPLIES -> R.string.forum_sort_most_replies
        ThreadSortOption.FEWEST_REPLIES -> R.string.forum_sort_fewest_replies
        ThreadSortOption.MOST_VIEWED -> R.string.forum_sort_most_viewed
        ThreadSortOption.LEAST_VIEWED -> R.string.forum_sort_least_viewed
        ThreadSortOption.TITLE -> R.string.forum_sort_title_az
        ThreadSortOption.TITLE_DESC -> R.string.forum_sort_title_za
        ThreadSortOption.RELEVANCE -> R.string.forum_sort_field_relevance
    }
)
