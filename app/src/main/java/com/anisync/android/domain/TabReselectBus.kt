package com.anisync.android.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** The five top-level destinations, as the reselect gestures address them. */
enum class MainTab {
    LIBRARY,
    DISCOVER,
    FEED,
    FORUM,
    PROFILE;

    /**
     * Whether this tab owns a search bar for the second tap to open. Feed has nothing to search
     * (AniList exposes no activity text search) and people search lives in Discover, so those two
     * answer a second tap the way they answer the first.
     */
    val hasSearch: Boolean get() = this == LIBRARY || this == DISCOVER || this == FORUM
}

/** A request aimed at one tab. [id] only ever grows, so a late reader can still tell it apart. */
data class TabRequest(val tab: MainTab? = null, val id: Long = 0L) {
    fun next(target: MainTab) = TabRequest(target, id + 1)
}

/**
 * Cross-screen bridge for the navigation bar's tap gestures: tapping the tab you are already on
 * asks it to scroll back to the top, and two quick taps on a tab ask it to open its search, from
 * the tab you are on and from any other alike.
 *
 * The bar lives in MainScreen while the scroll position and the search bar live inside each tab's
 * own screen, so neither side can reach the other directly. Requests are monotonic counters rather
 * than events, for the same reason [DiscoverSearchLauncher] keeps state: a tab's ViewModel may not
 * be collecting at the moment the gesture lands, and a counter read late still says what to do
 * where a dropped event would say nothing.
 */
@Singleton
class TabReselectBus @Inject constructor() {

    private val _scrollToTop = MutableStateFlow(TabRequest())
    val scrollToTop: StateFlow<TabRequest> = _scrollToTop.asStateFlow()

    private val _openSearch = MutableStateFlow(TabRequest())
    val openSearch: StateFlow<TabRequest> = _openSearch.asStateFlow()

    fun requestScrollToTop(tab: MainTab) = _scrollToTop.update { it.next(tab) }

    fun requestSearch(tab: MainTab) = _openSearch.update { it.next(tab) }
}

/**
 * Forwards the requests aimed at [tab] into that tab's UI state, which is where its screen reads
 * them. Pass no [onSearch] for a tab that has nothing to search.
 *
 * The callbacks take no argument on purpose: each tab keeps its own counter and bumps it, rather
 * than adopting the bus id. Discover's search counter already has a second source in
 * [DiscoverSearchLauncher], and two sequences writing one field would let it run backwards.
 */
fun TabReselectBus.observeTab(
    tab: MainTab,
    scope: CoroutineScope,
    onScrollToTop: () -> Unit,
    onSearch: (() -> Unit)? = null
) {
    scope.launch {
        scrollToTop.collect { request -> if (request.tab == tab) onScrollToTop() }
    }
    if (onSearch == null) return
    scope.launch {
        openSearch.collect { request -> if (request.tab == tab) onSearch() }
    }
}
