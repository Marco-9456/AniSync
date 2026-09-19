package com.anisync.android.presentation.forum.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchResult
import com.anisync.android.domain.ThreadSortOption
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.forum.forumCategoryTabs
import com.anisync.android.presentation.util.bouncyClickable

/** The label shown on the sort chip and in the sheet's grid. */
@StringRes
fun ThreadSortOption.sortLabelRes(): Int = when (this) {
    ThreadSortOption.RECENTLY_REPLIED -> R.string.forum_sort_recently_active
    ThreadSortOption.NEWEST -> R.string.forum_sort_newest
    ThreadSortOption.OLDEST -> R.string.forum_sort_oldest
    ThreadSortOption.MOST_REPLIES -> R.string.forum_sort_most_replies
    ThreadSortOption.MOST_VIEWED -> R.string.forum_sort_most_viewed
    ThreadSortOption.TITLE -> R.string.forum_sort_title
    ThreadSortOption.RELEVANCE -> R.string.forum_sort_best_match
}

/**
 * Ordering and narrowing in one sheet, in the shape Library's "Sort & filter" already set: an
 * uppercase section label, a two-column grid of pills for the single-choice set, chip rows for the
 * rest, and a filled confirm button.
 *
 * The category is the rail's, so picking one here moves the rail too.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ForumSortFilterSheet(
    sort: ThreadSortOption,
    selectedCategoryId: Int?,
    media: LibraryEntry?,
    author: SearchResult.UserResult?,
    subscribedOnly: Boolean,
    resultCount: Int,
    onSortChange: (ThreadSortOption) -> Unit,
    onCategoryChange: (Int?) -> Unit,
    onToggleSubscribedOnly: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    // Expanded straight away: at the partial height the sheet clipped its own confirm button.
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                // Capped so the pinned confirm button below can never be pushed off screen.
                .heightIn(max = 470.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.forum_sort_and_filter),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.forum_reset),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .bouncyClickable(onClick = onReset, clipShape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)

            SectionLabel(R.string.forum_sort_by)
            val options = ThreadSortOption.entries
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { option ->
                            SortPill(
                                label = stringResource(option.sortLabelRes()),
                                selected = option == sort,
                                onClick = { onSortChange(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Text(
                text = stringResource(R.string.forum_sort_best_match_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp)
            )

            SectionLabel(R.string.forum_narrow_by)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categoryLabel = forumCategoryTabs
                    .firstOrNull { it.id == selectedCategoryId }
                    ?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.forum_category_all)
                NarrowChip(
                    label = stringResource(R.string.forum_filter_category_value, categoryLabel),
                    active = selectedCategoryId != null,
                    trailingChevron = true,
                    onClick = {
                        // Cycling is wrong here; clearing is the one thing the sheet can do without
                        // opening a second surface, and the rail carries the full list.
                        onCategoryChange(null)
                    }
                )
                NarrowChip(
                    label = stringResource(
                        R.string.forum_filter_media_value,
                        media?.titleUserPreferred ?: stringResource(R.string.forum_filter_any)
                    ),
                    active = media != null,
                    trailingChevron = false,
                    onClick = {}
                )
                NarrowChip(
                    label = stringResource(
                        R.string.forum_filter_author_value,
                        author?.displayName ?: stringResource(R.string.forum_filter_anyone)
                    ),
                    active = author != null,
                    trailingChevron = false,
                    onClick = {}
                )
                NarrowChip(
                    label = stringResource(R.string.forum_filter_subscribed),
                    active = subscribedOnly,
                    trailingChevron = false,
                    onClick = onToggleSubscribedOnly
                )
            }

        }
        // The confirm button stays out of the scroll: a sheet whose only way to apply is below the
        // fold reads as having no way to apply at all.
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onApply,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.forum_show_threads_count, resultCount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(@StringRes res: Int) {
    Text(
        text = stringResource(res).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 10.dp)
    )
}

@Composable
private fun SortPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(48.dp)
            .bouncyClickable(onClick = onClick, clipShape = RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NarrowChip(
    label: String,
    active: Boolean,
    trailingChevron: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = CircleShape,
        modifier = Modifier
            .height(36.dp)
            .bouncyClickable(onClick = onClick, clipShape = CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = if (trailingChevron) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
            if (trailingChevron) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
