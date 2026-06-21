package com.anisync.android.presentation.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.navigation.NavHostController
import com.anisync.android.presentation.navigation.CharacterDetails
import com.anisync.android.presentation.navigation.MediaDetails
import com.anisync.android.presentation.navigation.PaneDetailHost
import com.anisync.android.presentation.navigation.StaffDetails
import com.anisync.android.presentation.navigation.StudioDetails

private const val SEARCH_PANE_SOURCE = "discover_search"

/**
 * What a tapped search result opens in the wide Discover-search detail pane. Media/character/staff/studio
 * reuse the shared in-pane media graph. **Users are intentionally excluded**: a user result opens the
 * full-screen profile (like tapping a user anywhere else) — [com.anisync.android.presentation.profile.ProfileScreen]
 * re-applies the app theme, which expects an `Activity` context and crashes inside the search overlay's
 * popup window.
 */
sealed interface SearchTarget {
    data class Media(val id: Int) : SearchTarget
    data class Character(val id: Int) : SearchTarget
    data class Staff(val id: Int) : SearchTarget
    data class Studio(val id: Int) : SearchTarget
}

internal fun SearchTarget.toPaneRoute(): Any = when (this) {
    is SearchTarget.Media -> MediaDetails(id, SEARCH_PANE_SOURCE)
    is SearchTarget.Character -> CharacterDetails(id)
    is SearchTarget.Staff -> StaffDetails(id)
    is SearchTarget.Studio -> StudioDetails(id)
}

/** Persists the open search target across configuration changes (for the scaffold's selection). */
internal val SearchTargetSaver = listSaver<SearchTarget?, Any>(
    save = { target ->
        when (target) {
            is SearchTarget.Media -> listOf("media", target.id)
            is SearchTarget.Character -> listOf("character", target.id)
            is SearchTarget.Staff -> listOf("staff", target.id)
            is SearchTarget.Studio -> listOf("studio", target.id)
            null -> emptyList()
        }
    },
    restore = { saved ->
        when (saved.getOrNull(0)) {
            "media" -> SearchTarget.Media(saved[1] as Int)
            "character" -> SearchTarget.Character(saved[1] as Int)
            "staff" -> SearchTarget.Staff(saved[1] as Int)
            "studio" -> SearchTarget.Studio(saved[1] as Int)
            else -> null
        }
    },
)

/**
 * Detail pane for the wide Discover-search two-pane: hosts the tapped [target]'s screen in a
 * self-contained [PaneDetailHost], reusing the shared media graph (relations and "see all" grids drill
 * WITHIN the pane; cross-feature destinations escalate to the app [navController]).
 */
@Composable
fun SearchDetailPane(
    target: SearchTarget,
    navController: NavHostController,
    onClose: () -> Unit,
) {
    PaneDetailHost(
        startRoute = target.toPaneRoute(),
        navController = navController,
        onClose = onClose,
    )
}
