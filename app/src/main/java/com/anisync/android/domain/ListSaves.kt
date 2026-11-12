package com.anisync.android.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Counts list edits that reached AniList, for the support card, which waits for the moment the app
 * has just done its job. A counter rather than an event, like [TabReselectBus], so a reader that
 * starts late only acts on what is new.
 */
@Singleton
class ListSaves @Inject constructor() {

    private val _count = MutableStateFlow(0L)
    val count: StateFlow<Long> = _count.asStateFlow()

    fun note() = _count.update { it + 1 }
}
