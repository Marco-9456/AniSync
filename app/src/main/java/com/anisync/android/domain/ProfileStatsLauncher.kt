package com.anisync.android.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asks the viewer's own profile to open on Statistics, from a page that cannot reach its state,
 * such as the last page of a recap. A pending flag rather than an event, for the reason
 * [DiscoverSearchLauncher] keeps state: the profile may not exist yet when the request is made.
 */
@Singleton
class ProfileStatsLauncher @Inject constructor() {
    private val _pending = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    fun open() {
        _pending.value = true
    }

    fun consume() {
        _pending.value = false
    }
}
