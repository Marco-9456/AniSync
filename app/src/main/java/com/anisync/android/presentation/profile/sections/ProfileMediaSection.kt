package com.anisync.android.presentation.profile.sections

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.MediaTypeToggle
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.library.LibraryTab
import com.anisync.android.presentation.library.components.LibraryRailChip
import com.anisync.android.presentation.profile.components.ProfileListGridCell
import com.anisync.android.presentation.profile.components.ProfileListRow
import com.anisync.android.presentation.profile.components.ProfileTabRail
import com.anisync.android.presentation.profile.lists.ProfileListFilters
import com.anisync.android.presentation.profile.lists.ProfileListResults
import com.anisync.android.presentation.profile.lists.ProfileListSummary
import com.anisync.android.presentation.profile.lists.ProfileListTools
import com.anisync.android.presentation.profile.lists.ProfileListView
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.presentation.util.toListIcon
import com.anisync.android.type.MediaType

internal enum class ListChip(val status: LibraryStatus?) {
    ALL(null),
    CURRENT(LibraryStatus.CURRENT),
    REPEATING(LibraryStatus.REPEATING),
    PAUSED(LibraryStatus.PAUSED),
    COMPLETED(LibraryStatus.COMPLETED),
    PLANNING(LibraryStatus.PLANNING),
    DROPPED(LibraryStatus.DROPPED),
    UNKNOWN(LibraryStatus.UNKNOWN)
}

/**
 * The list tabs' chips, docked with the profile tabs. The type toggle pinned at the start moves
 * between the Anime and Manga tabs, the way it switches libraries on the Library screen. While
 * searching, [searchCounts] turns the chips into the statuses the matches have.
 */
@Composable
internal fun MediaStatusRail(
    itemsByStatus: Map<LibraryStatus, List<LibraryEntry>>,
    selectedStatus: LibraryStatus?,
    mediaType: MediaType,
    onStatusSelected: (LibraryStatus?) -> Unit,
    onMediaTypeSelected: (MediaType) -> Unit,
    searchCounts: Map<LibraryStatus, Int>? = null
) {
    val showUnknown = itemsByStatus[LibraryStatus.UNKNOWN].orEmpty().isNotEmpty() ||
        selectedStatus == LibraryStatus.UNKNOWN
    val chips = if (searchCounts != null) {
        ListChip.entries.filter { it.status == null || it.status == selectedStatus || (searchCounts[it.status] ?: 0) > 0 }
    } else {
        ListChip.entries.filter { it != ListChip.UNKNOWN || showUnknown }
    }
    val loaded = itemsByStatus.isNotEmpty()
    ProfileTabRail(
        options = chips,
        selected = ListChip.entries.first { it.status == selectedStatus },
        pinned = { MediaTypeToggle(selected = mediaType, onSelect = onMediaTypeSelected) }
    ) { chip, selected ->
        LibraryRailChip(
            tab = chip.status?.let { LibraryTab.Standard(it) } ?: LibraryTab.All,
            mediaType = mediaType,
            count = when {
                searchCounts != null -> if (chip.status == null) searchCounts.values.sum() else searchCounts[chip.status] ?: 0
                !loaded -> null
                chip.status == null -> itemsByStatus.values.sumOf { it.size }
                else -> itemsByStatus[chip.status]?.size
            },
            selected = selected,
            onClick = { onStatusSelected(chip.status) }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun LazyListScope.profileMediaTab(
    results: ProfileListResults,
    tools: ProfileListTools,
    selectedStatus: LibraryStatus?,
    hasEntries: Boolean,
    isLoading: Boolean,
    loadFailed: Boolean,
    mediaType: MediaType,
    ownerName: String,
    isOwnProfile: Boolean,
    viewer: Map<Int, LibraryEntry>,
    yourScoreFormat: ScoreFormat,
    titleLanguage: TitleLanguage,
    onMediaClick: (Int) -> Unit,
    onOpenSort: () -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    transitionPrefix: String,
    listColumns: Int = 1,
    gridColumns: Int = 2,
    onAddToPlanning: ((Int) -> Unit)? = null,
    ownerAvatar: String? = null,
    yourAvatar: String? = null,
    onRemoveFilter: (ProfileListFilters) -> Unit = {}
) {
    // A reload keeps the list on screen rather than swapping it for a placeholder (#164). The first
    // load holds the rows' places, so the list lands without the page jumping.
    if (isLoading && !hasEntries) {
        item(key = "media_loading_$transitionPrefix", contentType = "loading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.profile_list_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                repeat(6) { ListRowPlaceholder() }
            }
        }
        return
    }
    if (loadFailed && !hasEntries) {
        item(key = "media_failed_$transitionPrefix", contentType = "empty") {
            EmptyState(
                icon = Icons.Default.CloudOff,
                title = stringResource(R.string.profile_list_failed),
                description = stringResource(R.string.profile_list_failed_body),
                actionLabel = stringResource(R.string.retry),
                actionIcon = Icons.Default.Refresh,
                onAction = onRetry,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
        return
    }

    item(key = "media_summary_$transitionPrefix", contentType = "summary") {
        ProfileListSummary(
            results = results,
            tools = tools,
            isOwnProfile = isOwnProfile,
            onRemoveFilter = onRemoveFilter
        )
    }

    val items = results.items
    if (items.isEmpty()) {
        val narrowed = tools.query.isNotBlank() || tools.filters.isActive
        item(key = "media_empty_${transitionPrefix}_${selectedStatus?.name}", contentType = "empty") {
            if (narrowed) {
                EmptyState(
                    icon = Icons.Default.FilterList,
                    title = stringResource(R.string.profile_list_nothing_matches),
                    description = stringResource(R.string.profile_list_nothing_matches_body),
                    actionLabel = if (tools.filters.isActive) stringResource(R.string.profile_list_clear_filters) else null,
                    actionIcon = if (tools.filters.isActive) Icons.Default.Close else null,
                    onAction = if (tools.filters.isActive) onClearFilters else null,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = stringResource(R.string.profile_list_empty_title),
                    description = emptyListMessage(selectedStatus, mediaType, ownerName, isOwnProfile),
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
        return
    }

    item(key = "media_top_spacer_$transitionPrefix") { Spacer(modifier = Modifier.height(8.dp)) }

    val showStatus = if (tools.query.isNotBlank()) tools.searchStatus == null else selectedStatus == null
    if (tools.view == ProfileListView.GRID) {
        itemsIndexed(
            items = items.chunked(gridColumns),
            key = { index, _ -> "media_grid_${transitionPrefix}_$index" },
            contentType = { _, _ -> "media_grid_row" }
        ) { _, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    ProfileListGridCell(
                        entry = item,
                        titleLanguage = titleLanguage,
                        onClick = { onMediaClick(item.mediaId) },
                        modifier = Modifier.weight(1f),
                        showYourStatus = !isOwnProfile
                    )
                }
                repeat(gridColumns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        return
    }

    // Read-only list rows (#78), chunked into [listColumns] so wide windows fill the width instead
    // of stretching one column.
    itemsIndexed(
        items = items.chunked(listColumns),
        key = { index, _ -> "media_row_${transitionPrefix}_$index" },
        contentType = { _, _ -> "media_list_row" }
    ) { _, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            row.forEach { item ->
                key(item.mediaId) {
                    val canPlan = onAddToPlanning != null && item.mediaId !in viewer
                    var menuOpen by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileListRow(
                            entry = item,
                            mediaType = mediaType,
                            ownerAvatar = ownerAvatar,
                            onClick = { onMediaClick(item.mediaId) },
                            titleLanguage = titleLanguage,
                            showStatus = showStatus,
                            yours = if (isOwnProfile) null else viewer[item.mediaId],
                            yourAvatar = yourAvatar,
                            yourScoreFormat = yourScoreFormat,
                            dateSort = tools.sort,
                            onLongClick = if (canPlan) ({ menuOpen = true }) else null
                        )
                        if (canPlan) {
                            val planningLabel = stringResource(R.string.profile_add_to_planning)
                            Menu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                item(
                                    text = planningLabel,
                                    leadingIcon = LibraryStatus.PLANNING.toListIcon(),
                                    onClick = {
                                        menuOpen = false
                                        onAddToPlanning?.invoke(item.mediaId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            repeat(listColumns - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun emptyListMessage(
    selectedStatus: LibraryStatus?,
    mediaType: MediaType,
    ownerName: String,
    isOwnProfile: Boolean
): String {
    if (!isOwnProfile) {
        val listName = selectedStatus?.toLabel(mediaType) ?: stringResource(R.string.all)
        return stringResource(R.string.profile_list_empty_other, ownerName, listName)
    }
    return when (selectedStatus) {
        LibraryStatus.CURRENT -> stringResource(if (mediaType == MediaType.MANGA) R.string.empty_reading else R.string.empty_watching)
        LibraryStatus.PLANNING -> stringResource(R.string.empty_planning)
        LibraryStatus.COMPLETED -> stringResource(R.string.empty_completed)
        else -> stringResource(R.string.profile_list_empty_own, selectedStatus?.toLabel(mediaType) ?: stringResource(R.string.all))
    }
}

/** A list row's shape while the list loads: the cover and three lines of text. */
@Composable
private fun ListRowPlaceholder() {
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(block)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
            listOf(0.7f, 0.45f, 0.55f).forEach { fraction ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(block)
                )
            }
        }
    }
}
