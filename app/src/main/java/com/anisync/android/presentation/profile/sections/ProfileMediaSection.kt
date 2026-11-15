package com.anisync.android.presentation.profile.sections

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.MediaPosterCard
import com.anisync.android.presentation.components.MediaTypeToggle
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.library.LibraryTab
import com.anisync.android.presentation.library.components.LibraryRailChip
import com.anisync.android.presentation.profile.components.ProfileMediaListCard
import com.anisync.android.presentation.profile.components.ProfileTabRail
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
 * between the Anime and Manga tabs, the way it switches libraries on the Library screen.
 */
@Composable
internal fun MediaStatusRail(
    itemsByStatus: Map<LibraryStatus, List<LibraryEntry>>,
    selectedStatus: LibraryStatus?,
    mediaType: MediaType,
    onStatusSelected: (LibraryStatus?) -> Unit,
    onMediaTypeSelected: (MediaType) -> Unit
) {
    val showUnknown = itemsByStatus[LibraryStatus.UNKNOWN].orEmpty().isNotEmpty() ||
        selectedStatus == LibraryStatus.UNKNOWN
    val chips = ListChip.entries.filter { it != ListChip.UNKNOWN || showUnknown }
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
    onAddToPlanning: ((Int) -> Unit)? = null
) {
    // A reload keeps the list on screen rather than swapping it for a spinner (#164).
    if (isLoading && !hasEntries) {
        item(key = "media_loading_$transitionPrefix", contentType = "loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AppCircularProgressIndicator()
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
            onOpenSort = onOpenSort,
            onClearFilters = onClearFilters
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
                    actionIcon = if (tools.filters.isActive) Icons.Default.FilterList else null,
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

    item(key = "media_top_spacer_$transitionPrefix") { Spacer(modifier = Modifier.height(12.dp)) }

    val showStatus = selectedStatus == null || tools.query.isNotBlank()
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
                    MediaPosterCard(
                        item = item,
                        onClick = { onMediaClick(item.mediaId) },
                        titleLanguage = titleLanguage,
                        transitionPrefix = transitionPrefix,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(gridColumns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(20.dp))
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
                        ProfileMediaListCard(
                            entry = item,
                            mediaType = mediaType,
                            onClick = { onMediaClick(item.mediaId) },
                            titleLanguage = titleLanguage,
                            showStatus = showStatus,
                            yours = if (isOwnProfile) null else viewer[item.mediaId],
                            yourScoreFormat = yourScoreFormat,
                            onLongClick = if (canPlan) ({ menuOpen = true }) else null,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            transitionPrefix = transitionPrefix
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
        Spacer(modifier = Modifier.height(10.dp))
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
