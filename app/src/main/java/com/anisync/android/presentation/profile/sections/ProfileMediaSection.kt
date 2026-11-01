package com.anisync.android.presentation.profile.sections

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.MediaTypeToggle
import com.anisync.android.presentation.library.LibraryTab
import com.anisync.android.presentation.library.components.LibraryRailChip
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.profile.components.ProfileMediaListCard
import com.anisync.android.presentation.profile.components.ProfileTabRail
import com.anisync.android.type.MediaType

private val mediaStatusTabOrder = listOf(
    LibraryStatus.CURRENT,
    LibraryStatus.REPEATING,
    LibraryStatus.PAUSED,
    LibraryStatus.COMPLETED,
    LibraryStatus.PLANNING,
    LibraryStatus.DROPPED
)

/**
 * The list tabs' statuses, docked with the profile tabs. The type toggle pinned at the start moves
 * between the Anime and Manga tabs, the way it switches libraries on the Library screen.
 */
@Composable
internal fun MediaStatusRail(
    itemsByStatus: Map<LibraryStatus, List<LibraryEntry>>,
    selectedStatus: LibraryStatus,
    mediaType: MediaType,
    onStatusSelected: (LibraryStatus) -> Unit,
    onMediaTypeSelected: (MediaType) -> Unit
) {
    val statuses = buildList {
        addAll(mediaStatusTabOrder)
        val hasUnknownEntries = itemsByStatus[LibraryStatus.UNKNOWN].orEmpty().isNotEmpty()
        if (hasUnknownEntries || selectedStatus == LibraryStatus.UNKNOWN) {
            add(LibraryStatus.UNKNOWN)
        }
    }
    ProfileTabRail(
        options = statuses,
        selected = selectedStatus,
        pinned = { MediaTypeToggle(selected = mediaType, onSelect = onMediaTypeSelected) }
    ) { status, selected ->
        LibraryRailChip(
            tab = LibraryTab.Standard(status),
            mediaType = mediaType,
            count = itemsByStatus[status]?.size,
            selected = selected,
            onClick = { onStatusSelected(status) }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun LazyListScope.profileMediaTab(
    itemsByStatus: Map<LibraryStatus, List<LibraryEntry>>,
    selectedStatus: LibraryStatus,
    isLoading: Boolean,
    mediaType: MediaType,
    @StringRes emptyMessageRes: Int,
    onMediaClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    transitionPrefix: String,
    listColumns: Int = 1,
    modifier: Modifier = Modifier
) {
    val items = itemsByStatus[selectedStatus].orEmpty()
    // A reload keeps the list on screen rather than swapping it for a spinner (#164).
    if (isLoading && items.isEmpty()) {
        item(key = "media_loading_${transitionPrefix}", contentType = "loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AppCircularProgressIndicator()
            }
        }
    } else if (items.isEmpty()) {
        item(key = "media_empty_${transitionPrefix}_${selectedStatus.name}", contentType = "empty") {
            PlaceholderTabContent(
                message = statusEmptyMessage(selectedStatus = selectedStatus, mediaType = mediaType, emptyMessageRes = emptyMessageRes),
                modifier = modifier
            )
        }
    } else {
        // Read-only list rows (#78): denser than a poster grid and roomy enough to carry each
        // entry's score/progress/format/rewatches plus the user's note. Chunked into [listColumns]
        // so wide windows fill the width instead of stretching one column.
        val rowItems = items.chunked(listColumns)
        item(key = "media_top_spacer_${transitionPrefix}") { Spacer(modifier = Modifier.height(16.dp)) }

        itemsIndexed(
            items = rowItems,
            key = { index, _ -> "media_row_${transitionPrefix}_${selectedStatus.name}_$index" },
            contentType = { _, _ -> "media_list_row" }
        ) { _, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileMediaListCard(
                            entry = item,
                            mediaType = mediaType,
                            onClick = { onMediaClick(item.mediaId) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            transitionPrefix = transitionPrefix
                        )
                    }
                }
                repeat(listColumns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun statusEmptyMessage(
    selectedStatus: LibraryStatus,
    mediaType: MediaType,
    @StringRes emptyMessageRes: Int
): String {
    return when (selectedStatus) {
        LibraryStatus.CURRENT -> if (mediaType == MediaType.MANGA) {
            stringResource(R.string.empty_reading)
        } else {
            stringResource(R.string.empty_watching)
        }
        LibraryStatus.PLANNING -> stringResource(R.string.empty_planning)
        LibraryStatus.COMPLETED -> stringResource(R.string.empty_completed)
        LibraryStatus.REPEATING,
        LibraryStatus.PAUSED,
        LibraryStatus.DROPPED,
        LibraryStatus.UNKNOWN -> stringResource(emptyMessageRes)
    }
}
