package com.anisync.android.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.NotificationBadgeStore
import com.anisync.android.data.NotificationPreferences
import com.anisync.android.data.NotificationReadStore
import com.anisync.android.domain.GetNotificationsUseCase
import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationFilter
import com.anisync.android.domain.NotificationReadState
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The inbox.
 *
 * Read state is not derived here. It belongs to [NotificationReadStore], which persists it per
 * account, so leaving the screen, killing the process or receiving a new notification cannot turn
 * an already-read row new again. This ViewModel only reports what the user did (opened a row,
 * marked the inbox read) and re-flags the list whenever the stored state changes.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotifications: GetNotificationsUseCase,
    private val badgeStore: NotificationBadgeStore,
    private val readStore: NotificationReadStore,
    private val notificationPreferences: NotificationPreferences
) : ViewModel() {

    /** Read state of the account active now. An account switch rebuilds this ViewModel. */
    private val readState: StateFlow<NotificationReadState> = readStore.state()

    private val _uiState = MutableStateFlow(
        NotificationsUiState(
            readTrackingEnabled = notificationPreferences.inboxReadTrackingEnabled.value
        )
    )
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var nextPage = 1
    private var loadJob: Job? = null

    private data class FilterSnapshot(
        val items: List<Notification>,
        val entries: List<NotificationEntry>,
        val nextPage: Int,
        val hasNextPage: Boolean
    )

    private val snapshots = mutableMapOf<NotificationFilter, FilterSnapshot>()

    /**
     * The unfiltered list. Placing the watermark is only ever done from this one: a filtered page
     * is a subset of the same ids, so a count taken over the whole inbox would land in the wrong
     * place on it.
     */
    private var unfiltered: List<Notification> = emptyList()
    private var unfilteredHasMore = true

    private var markedOnOpen = false

    /** With tracking off, the visit itself reads the inbox, once. */
    private var clearedOnOpen = false

    init {
        viewModelScope.launch { readState.collect { reflag() } }
        viewModelScope.launch {
            notificationPreferences.inboxReadTrackingEnabled.collect { enabled ->
                _uiState.update { it.copy(readTrackingEnabled = enabled) }
                // Switched on while the inbox is open: the record was dropped when it went off, so
                // it needs placing again before the list can say what is new.
                if (enabled) badgeStore.serverUnreadCount.value?.let { count -> anchor(count) }
                reflag()
            }
        }
        // An account this device has no record of needs AniList's unread count to place its first
        // watermark, and a cold open can reach the inbox before anything has fetched one.
        viewModelScope.launch {
            badgeStore.serverUnreadCount.filterNotNull().collect { anchor(it) }
        }
        if (badgeStore.serverUnreadCount.value == null) {
            viewModelScope.launch { badgeStore.refresh() }
        }
        readStore.syncBadge()
        readStore.retryPendingReset()
        load(reset = true, isInitial = true)
    }

    fun onAction(action: NotificationsAction) {
        when (action) {
            is NotificationsAction.SetFilter -> selectFilter(action.filter)
            NotificationsAction.Refresh -> load(reset = true, isInitial = false, refreshing = true)
            NotificationsAction.LoadNextPage -> {
                val s = _uiState.value
                if (!s.hasNextPage || s.isLoading || s.isPaginating || s.isRefreshing) return
                load(reset = false, isInitial = false)
            }
            NotificationsAction.Retry -> load(reset = true, isInitial = true)
            NotificationsAction.MarkAllRead -> markAllRead()
            is NotificationsAction.MarkRead -> markRead(action.key)
        }
    }

    /**
     * Everything in the inbox, not only what the current filter shows. The reset query the store
     * fires returns page one unfiltered, which corrects the watermark if this list was narrow.
     */
    private fun markAllRead() {
        if (!_uiState.value.readTrackingEnabled) return
        readStore.markAllRead(known())
    }

    private fun markRead(key: String) {
        if (!_uiState.value.readTrackingEnabled) return
        val entry = _uiState.value.entries.firstOrNull { it.key == key } ?: return
        if (!entry.isUnread) return
        // The whole inbox goes with it: reading the last unread row is what lets the store report
        // the inbox read to AniList, and only this list can say whether it was the last one.
        readStore.markRead(entry.all, known())
    }

    /** Everything loaded, across filters. Unread rows are the newest, so they are all in here. */
    private fun known(): List<Notification> =
        (unfiltered + _uiState.value.items).distinctBy { it.id }

    private fun anchor(serverUnreadCount: Int) {
        if (!_uiState.value.readTrackingEnabled) return
        if (readState.value.anchored) return
        if (unfiltered.isEmpty() && unfilteredHasMore) return
        readStore.anchor(unfiltered, serverUnreadCount, unfilteredHasMore)
    }

    /** Re-applies read state to the visible list and to every filter held in reserve. */
    private fun reflag() {
        _uiState.update { it.withEntries(it.entries) }
        val state = readState.value.takeIf { _uiState.value.readTrackingEnabled }
        for ((filter, snapshot) in snapshots.toList()) {
            snapshots[filter] = snapshot.copy(entries = snapshot.entries.flagUnread(state))
        }
    }

    private fun NotificationsUiState.withEntries(
        entries: List<NotificationEntry>
    ): NotificationsUiState {
        val flagged = entries.flagUnread(readState.value.takeIf { readTrackingEnabled })
        val (new, earlier) = flagged.partition { it.isUnread }
        return copy(entries = flagged, newEntries = new, earlierEntries = earlier)
    }

    private fun selectFilter(filter: NotificationFilter) {
        val current = _uiState.value
        if (filter == current.filter) return

        loadJob?.cancel()
        // Preserve current filter's loaded data so the user can return to it instantly.
        if (current.items.isNotEmpty()) {
            snapshots[current.filter] = FilterSnapshot(
                items = current.items,
                entries = current.entries,
                nextPage = nextPage,
                hasNextPage = current.hasNextPage
            )
        }

        val cached = snapshots[filter]
        if (cached != null) {
            nextPage = cached.nextPage
            _uiState.update {
                it.copy(
                    filter = filter,
                    items = cached.items,
                    hasNextPage = cached.hasNextPage,
                    isLoading = false,
                    isRefreshing = false,
                    isPaginating = false,
                    errorMessage = null
                ).withEntries(cached.entries)
            }
            // Silent background refresh; UI shows cached data immediately, no spinner.
            load(reset = true, isInitial = false, quiet = true)
        } else {
            nextPage = 1
            _uiState.update {
                it.copy(
                    filter = filter,
                    items = emptyList(),
                    hasNextPage = true,
                    isLoading = true,
                    isRefreshing = false,
                    isPaginating = false,
                    errorMessage = null
                ).withEntries(emptyList())
            }
            load(reset = true, isInitial = true)
        }
    }

    private fun load(
        reset: Boolean,
        isInitial: Boolean,
        refreshing: Boolean = false,
        quiet: Boolean = false
    ) {
        loadJob?.cancel()
        if (reset) nextPage = 1

        if (!quiet) {
            _uiState.update {
                it.copy(
                    isLoading = isInitial,
                    isRefreshing = refreshing,
                    isPaginating = !isInitial && !refreshing,
                    errorMessage = null
                )
            }
        }

        val filter = _uiState.value.filter
        // Reading the inbox is not the same as reading the notifications in it, so a page never
        // resets the unread count. The exception is read tracking being off, where the visit is the
        // only thing left that can mark anything read.
        val resetCount = !_uiState.value.readTrackingEnabled &&
            !clearedOnOpen &&
            filter == NotificationFilter.ALL

        loadJob = viewModelScope.launch {
            val result = getNotifications.getPage(
                page = nextPage,
                typeFilter = filter.types,
                resetUnreadCount = resetCount
            )
            // Late response from a previous filter, drop it.
            if (_uiState.value.filter != filter) return@launch

            when (result) {
                is Result.Success -> {
                    val page = result.data
                    val state = _uiState.value
                    // Dedupe by id: new notifications push older ones across page boundaries, so a
                    // page can re-deliver an item already held. Ungrouped rows key off "single_<id>",
                    // and a duplicate id crashes the LazyColumn with a duplicate key.
                    val merged =
                        if (reset) page.items
                        else (state.items + page.items).distinctBy { it.id }
                    val grouped = withContext(Dispatchers.Default) { groupNotifications(merged) }

                    if (filter == NotificationFilter.ALL) {
                        unfiltered = merged
                        unfilteredHasMore = page.hasNextPage
                        badgeStore.serverUnreadCount.value?.let { anchor(it) }
                    }
                    if (resetCount) {
                        clearedOnOpen = true
                        badgeStore.markedAllRead()
                    }

                    _uiState.update {
                        it.copy(
                            items = merged,
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            hasNextPage = page.hasNextPage,
                            errorMessage = null
                        ).withEntries(grouped)
                    }
                    snapshots[filter] = FilterSnapshot(
                        items = merged,
                        entries = _uiState.value.entries,
                        nextPage = if (page.hasNextPage) nextPage + 1 else nextPage,
                        hasNextPage = page.hasNextPage
                    )
                    if (page.hasNextPage) nextPage++
                    markOnOpenIfRequested(filter, merged)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    /** Opt-in: the visit marks the inbox read, the way the AniList site behaves. */
    private fun markOnOpenIfRequested(filter: NotificationFilter, items: List<Notification>) {
        if (markedOnOpen || filter != NotificationFilter.ALL) return
        if (!_uiState.value.readTrackingEnabled) return
        if (!notificationPreferences.inboxMarkReadOnOpen.value) return
        markedOnOpen = true
        readStore.markAllRead(items)
    }
}
