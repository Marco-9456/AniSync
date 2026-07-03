package com.anisync.android.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-screen bridge for "open Discover search with these filters" requests
 * (ranking cards, genre/tag chips on media details, …).
 *
 * Two independent readers react to a request, over two separate flows:
 * - MainScreen collects [navigationRequests] to switch to the Discover tab. A
 *   buffered SharedFlow, NOT the state itself: when DiscoverViewModel is already
 *   alive it consumes [pending] within microseconds, and a conflated StateFlow
 *   would drop the non-null value before MainScreen's collector ever saw it
 *   (observed: filters applied but no tab switch).
 * - DiscoverViewModel reads [pending] for the filters, applies them and asks the
 *   screen to expand the search overlay, then [consume]s the request. State (not
 *   an event) so a request made before Discover ever composed isn't lost.
 */
@Singleton
class DiscoverSearchLauncher @Inject constructor() {

    private val _pending = MutableStateFlow<SearchFilters?>(null)
    val pending: StateFlow<SearchFilters?> = _pending.asStateFlow()

    private val _navigationRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val navigationRequests: SharedFlow<Unit> = _navigationRequests.asSharedFlow()

    fun launch(filters: SearchFilters) {
        _pending.value = filters
        _navigationRequests.tryEmit(Unit)
    }

    fun consume() {
        _pending.value = null
    }
}
