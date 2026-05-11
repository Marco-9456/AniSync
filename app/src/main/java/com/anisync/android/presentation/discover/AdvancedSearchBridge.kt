package com.anisync.android.presentation.discover

import com.anisync.android.domain.SearchFilters
import com.anisync.android.type.MediaType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries advanced-search state across the navigation boundary between
 * `DiscoverScreen` and `AdvancedSearchScreen`. Avoids forcing
 * [SearchFilters] (with its non-serializable Apollo enum fields) through
 * `SavedStateHandle`.
 *
 *  - DiscoverViewModel calls [submitInput] before navigating, so the
 *    advanced-search ViewModel starts from the same query / type / filters
 *    the user is currently looking at.
 *  - AdvancedSearchViewModel calls [emitResult] on Apply; DiscoverViewModel
 *    consumes from [results] and re-runs the search.
 */
@Singleton
class AdvancedSearchBridge @Inject constructor() {

    data class PendingInput(
        val query: String,
        val type: MediaType,
        val filters: SearchFilters
    )

    private val _pending = MutableStateFlow<PendingInput?>(null)
    val pending: StateFlow<PendingInput?> = _pending.asStateFlow()

    private val resultChannel = Channel<SearchFilters>(capacity = Channel.CONFLATED)
    val results = resultChannel.consumeAsFlow()

    fun submitInput(input: PendingInput) {
        _pending.value = input
    }

    fun consumePending(): PendingInput? = _pending.value.also { _pending.value = null }

    fun emitResult(filters: SearchFilters) {
        resultChannel.trySend(filters)
    }
}
