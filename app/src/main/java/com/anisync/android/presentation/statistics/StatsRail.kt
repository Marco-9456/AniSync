package com.anisync.android.presentation.statistics

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Update
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.presentation.components.MediaTypeToggle
import com.anisync.android.presentation.components.PinnedRail
import com.anisync.android.presentation.components.RailChip
import com.anisync.android.presentation.profile.ProfileStatsType
import com.anisync.android.presentation.profile.StatsView
import com.anisync.android.presentation.profile.components.RailScroll
import com.anisync.android.presentation.profile.components.RevealSelectedChip
import com.anisync.android.type.MediaType

/**
 * The Statistics tab's rail: the anime and manga toggle pinned, and a chip per view that switches
 * the dashboard the way Library's chips switch lists.
 */
@Composable
fun StatsRail(
    statsType: ProfileStatsType,
    views: List<StatsView>,
    selectedView: StatsView,
    onTypeSelected: (ProfileStatsType) -> Unit,
    onViewSelected: (StatsView) -> Unit,
    isOwnProfile: Boolean,
    scroll: RailScroll,
    modifier: Modifier = Modifier,
) {
    RevealSelectedChip(scroll, selectedView, views.indexOf(selectedView))
    PinnedRail(
        pinned = {
            MediaTypeToggle(
                selected = if (statsType == ProfileStatsType.MANGA) MediaType.MANGA else MediaType.ANIME,
                onSelect = { onTypeSelected(if (it == MediaType.MANGA) ProfileStatsType.MANGA else ProfileStatsType.ANIME) },
            )
        },
        listState = scroll.state,
        measuredInset = scroll.pinnedInset,
        modifier = modifier,
    ) {
        items(views, key = { it.name }) { view ->
            RailChip(
                label = stringResource(view.labelRes(statsType, isOwnProfile)),
                icon = view.icon(statsType),
                selected = view == selectedView,
                onClick = { onViewSelected(view) },
            )
        }
    }
}

@StringRes
internal fun StatsView.labelRes(statsType: ProfileStatsType, isOwnProfile: Boolean): Int = when (this) {
    StatsView.OVERVIEW -> R.string.profile_tab_overview
    StatsView.RECAPS -> R.string.settings_recaps
    StatsView.ACTIVITY -> R.string.profile_tab_activity
    StatsView.TASTE -> R.string.stats_view_taste
    StatsView.HOW_YOU_WATCH -> howTheyWatchRes(statsType == ProfileStatsType.MANGA, isOwnProfile)
    StatsView.ERAS -> R.string.stats_view_eras
    StatsView.PEOPLE -> R.string.stats_view_people
}

@StringRes
internal fun howTheyWatchRes(manga: Boolean, isOwnProfile: Boolean): Int = when {
    isOwnProfile && manga -> R.string.stats_view_how_you_read
    isOwnProfile -> R.string.stats_view_how_you_watch
    manga -> R.string.stats_view_how_they_read
    else -> R.string.stats_view_how_they_watch
}

private fun StatsView.icon(statsType: ProfileStatsType): ImageVector = when (this) {
    StatsView.OVERVIEW -> Icons.Default.BarChart
    StatsView.RECAPS -> Icons.Default.AutoAwesomeMotion
    StatsView.ACTIVITY -> Icons.Default.CalendarMonth
    StatsView.TASTE -> Icons.Default.Star
    StatsView.HOW_YOU_WATCH ->
        if (statsType == ProfileStatsType.MANGA) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Tv
    StatsView.ERAS -> Icons.Default.Update
    StatsView.PEOPLE -> Icons.Default.Group
}
