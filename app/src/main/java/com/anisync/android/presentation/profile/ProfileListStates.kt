package com.anisync.android.presentation.profile

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.anisync.android.presentation.profile.lists.ProfileListTools

/** A list state per [ProfileUiState.contentKey], so each tab and chip comes back where it was left. */
@Stable
internal class ProfileListStates(saved: Map<String, Pair<Int, Int>> = emptyMap()) {
    private val states = saved.mapValuesTo(mutableMapOf()) { (_, place) -> LazyListState(place.first, place.second) }

    fun of(key: String): LazyListState = states.getOrPut(key) { LazyListState() }

    companion object {
        val Saver: Saver<ProfileListStates, Any> = listSaver(
            save = { holder ->
                holder.states.flatMap { (key, state) -> listOf(key, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }
            },
            restore = { flat ->
                ProfileListStates(flat.chunked(3).associate { (key, index, offset) -> key as String to ((index as Int) to (offset as Int)) })
            }
        )
    }
}

@Composable
internal fun rememberProfileListStates(): ProfileListStates =
    rememberSaveable(saver = ProfileListStates.Saver) { ProfileListStates() }

/** What sits under the tabs: the tab with its chip, type or filter. Each keeps its own place. */
val ProfileUiState.contentKey: String
    get() = when (selectedTab) {
        ProfileTab.OVERVIEW -> selectedTab.name
        ProfileTab.STATS -> "STATS_${selectedStatsType.name}_${selectedStatsView.name}"
        ProfileTab.ACTIVITY -> "ACTIVITY_${selectedActivityFilter.name}"
        ProfileTab.ANIME, ProfileTab.MANGA -> {
            val status = if (listTools.query.isNotBlank()) listTools.searchStatus else selectedListStatus
            "${selectedTab.name}_${status?.name ?: "ALL"}"
        }
        ProfileTab.FAVORITES -> "FAVORITES_${selectedFavoritesFilter.name}"
        ProfileTab.SOCIAL -> "SOCIAL_${selectedSocialTab.name}"
        ProfileTab.COMPARE -> "COMPARE_${compareType.name}"
    }

/** The list's search, sort, filters and view, which start the rows over when any of them changes. */
private val ProfileUiState.listToolsKey: ProfileListTools?
    get() = if (selectedTab.isList) listTools.copy(searchStatus = null) else null

private class ShownContent(var contentKey: String, var toolsKey: ProfileListTools?, var onPage: Boolean)

/**
 * Where the open list starts when what sits under the tabs changes, in the frame it first draws.
 * A page opens at its top, a tab or a chip comes back where it was left unless [keepsPlace] says
 * otherwise, and a new search, sort, filter or view starts the rows over.
 */
@Composable
internal fun ProfileListPlacement(uiState: ProfileUiState, listState: LazyListState, keepsPlace: () -> Boolean) {
    val contentKey = uiState.contentKey
    val toolsKey = uiState.listToolsKey
    val onPage = uiState.selectedTab.isPage
    val shown = remember { ShownContent(contentKey, toolsKey, onPage) }
    SideEffect {
        val restart = if (shown.contentKey != contentKey) {
            if (onPage) !shown.onPage else !keepsPlace()
        } else {
            shown.toolsKey != toolsKey
        }
        if (restart) listState.requestScrollToItem(0)
        shown.contentKey = contentKey
        shown.toolsKey = toolsKey
        shown.onPage = onPage
    }
}
