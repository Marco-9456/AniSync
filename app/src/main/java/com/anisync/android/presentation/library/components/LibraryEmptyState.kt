package com.anisync.android.presentation.library.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.library.LibraryTab
import com.anisync.android.presentation.util.LIBRARY_ALL_TAB_ID
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.presentation.util.toListIcon
import com.anisync.android.type.MediaType
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.ui.theme.ListIndicatorColor
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.listIndicatorColor

/**
 * The library's one empty state.
 *
 * Every list used to fall through to "Nothing to show here" unless it was Watching, Planning or
 * Completed — a sentence that names nothing and offers nothing. This says which list is empty, in
 * that list's own colour and shape, and gives one way out.
 *
 * The variant that matters most is [filterCount] > 0: with filtering in the library, an empty tab
 * now has two possible causes, and reporting the wrong one sends people looking for titles they
 * never lost.
 */
@Composable
fun LibraryEmptyState(
    tab: LibraryTab,
    mediaType: MediaType,
    filterCount: Int,
    onBrowseDiscover: () -> Unit,
    onGoToTab: (String) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = emptyStateSpec(tab, mediaType, filterCount, onBrowseDiscover, onGoToTab, onClearFilters)

    EmptyState(
        icon = spec.icon,
        title = spec.title,
        description = spec.description,
        actionLabel = spec.actionLabel,
        actionIcon = spec.actionIcon,
        onAction = spec.onAction,
        emblemShape = spec.kind.badgeShape(),
        emblemContainer = spec.colors.container,
        emblemContent = spec.colors.content,
        actionEmphasised = spec.actionEmphasised,
        animationKey = tab to filterCount,
        // The nav bar floats over the bottom of the list, so an evenly centred block sits
        // visually low. Reserving its inset puts the state in the middle of what you can see.
        modifier = modifier.padding(bottom = LocalMainNavBarInset.current)
    )
}

private data class EmptyStateSpec(
    val kind: ListIndicatorKind,
    val colors: ListIndicatorColor,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionLabel: String,
    val actionIcon: ImageVector,
    val actionEmphasised: Boolean,
    val onAction: () -> Unit
)

@Composable
private fun emptyStateSpec(
    tab: LibraryTab,
    mediaType: MediaType,
    filterCount: Int,
    onBrowseDiscover: () -> Unit,
    onGoToTab: (String) -> Unit,
    onClearFilters: () -> Unit
): EmptyStateSpec {
    val isManga = mediaType == MediaType.MANGA
    val browse = stringResource(R.string.library_empty_action_browse)

    // Filters hide titles from every list the same way, so this reason outranks the list itself.
    if (filterCount > 0) {
        return EmptyStateSpec(
            kind = ListIndicatorKind.CUSTOM,
            colors = ListIndicatorColor(
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            icon = Icons.Default.Tune,
            title = stringResource(R.string.library_empty_filtered_title, tab.getLabel(mediaType)),
            description = pluralStringResource(
                R.plurals.library_empty_filtered_desc,
                filterCount,
                filterCount
            ),
            actionLabel = stringResource(R.string.library_empty_action_clear_filters),
            actionIcon = Icons.Default.Close,
            actionEmphasised = true,
            onAction = onClearFilters
        )
    }

    // Resolved up here: a local function cannot be @Composable, and these read string resources.
    val goToWatchingLabel = stringResource(
        R.string.library_empty_action_go_to,
        LibraryStatus.CURRENT.toLabel(mediaType)
    )
    val goToCompletedLabel = stringResource(
        R.string.library_empty_action_go_to,
        LibraryStatus.COMPLETED.toLabel(mediaType)
    )
    val goToWatching = { onGoToTab("status:${LibraryStatus.CURRENT.name}") }
    val goToCompleted = { onGoToTab("status:${LibraryStatus.COMPLETED.name}") }

    return when (tab) {
        is LibraryTab.All -> EmptyStateSpec(
            kind = ListIndicatorKind.CUSTOM,
            colors = listIndicatorColor(ListIndicatorKind.CUSTOM),
            icon = Icons.Default.AllInclusive,
            title = stringResource(R.string.library_empty_all_title),
            description = stringResource(R.string.library_empty_all_desc),
            actionLabel = browse,
            actionIcon = Icons.AutoMirrored.Filled.TrendingUp,
            actionEmphasised = false,
            onAction = onBrowseDiscover
        )

        is LibraryTab.Favorites -> EmptyStateSpec(
            kind = ListIndicatorKind.CUSTOM,
            colors = ListIndicatorColor(
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer
            ),
            icon = Icons.Default.Favorite,
            title = stringResource(R.string.library_empty_favorites_title),
            description = stringResource(R.string.library_empty_favorites_desc),
            actionLabel = browse,
            actionIcon = Icons.AutoMirrored.Filled.TrendingUp,
            actionEmphasised = false,
            onAction = onBrowseDiscover
        )

        is LibraryTab.Custom -> EmptyStateSpec(
            kind = ListIndicatorKind.CUSTOM,
            colors = listIndicatorColor(ListIndicatorKind.CUSTOM),
            icon = Icons.AutoMirrored.Filled.List,
            title = stringResource(R.string.library_empty_custom_title, tab.name),
            description = stringResource(R.string.library_empty_custom_desc),
            actionLabel = stringResource(R.string.library_empty_action_choose),
            actionIcon = Icons.Default.CheckCircle,
            actionEmphasised = false,
            onAction = { onGoToTab(LIBRARY_ALL_TAB_ID) }
        )

        is LibraryTab.Standard -> {
            val kind = tab.status.toIndicatorKind()
            val colors = listIndicatorColor(kind)
            val icon = tab.status.toListIcon()
            when (tab.status) {
                LibraryStatus.CURRENT -> EmptyStateSpec(
                    kind, colors, icon,
                    stringResource(R.string.library_empty_current_title),
                    stringResource(R.string.library_empty_current_desc),
                    browse, Icons.AutoMirrored.Filled.TrendingUp, false, onBrowseDiscover
                )

                LibraryStatus.REPEATING -> {
                    EmptyStateSpec(
                        kind, colors, icon,
                        stringResource(
                            if (isManga) {
                                R.string.library_empty_repeating_title_manga
                            } else {
                                R.string.library_empty_repeating_title
                            }
                        ),
                        stringResource(R.string.library_empty_repeating_desc),
                        goToCompletedLabel, Icons.Default.CheckCircle, false, goToCompleted
                    )
                }

                LibraryStatus.PLANNING -> EmptyStateSpec(
                    kind, colors, icon,
                    stringResource(R.string.library_empty_planning_title),
                    stringResource(
                        if (isManga) {
                            R.string.library_empty_planning_desc_manga
                        } else {
                            R.string.library_empty_planning_desc
                        }
                    ),
                    browse, Icons.AutoMirrored.Filled.TrendingUp, false, onBrowseDiscover
                )

                LibraryStatus.PAUSED -> {
                    EmptyStateSpec(
                        kind, colors, icon,
                        stringResource(R.string.library_empty_paused_title),
                        stringResource(R.string.library_empty_paused_desc),
                        goToWatchingLabel, Icons.Default.PlayArrow, false, goToWatching
                    )
                }

                LibraryStatus.COMPLETED -> {
                    EmptyStateSpec(
                        kind, colors, icon,
                        stringResource(R.string.library_empty_completed_title),
                        stringResource(R.string.library_empty_completed_desc),
                        goToWatchingLabel, Icons.Default.PlayArrow, false, goToWatching
                    )
                }

                LibraryStatus.DROPPED -> {
                    EmptyStateSpec(
                        kind, colors, icon,
                        stringResource(R.string.library_empty_dropped_title),
                        stringResource(R.string.library_empty_dropped_desc),
                        goToWatchingLabel, Icons.Default.PlayArrow, false, goToWatching
                    )
                }

                LibraryStatus.UNKNOWN -> EmptyStateSpec(
                    kind, colors, icon,
                    stringResource(R.string.library_empty_all_title),
                    stringResource(R.string.library_empty_all_desc),
                    browse, Icons.AutoMirrored.Filled.TrendingUp, false, onBrowseDiscover
                )
            }
        }
    }
}
