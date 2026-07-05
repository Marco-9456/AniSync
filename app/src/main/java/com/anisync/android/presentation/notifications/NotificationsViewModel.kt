package com.anisync.android.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.NotificationBadgeStore
import com.anisync.android.domain.GetNotificationsUseCase
import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationFilter
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotifications: GetNotificationsUseCase,
    private val badgeStore: NotificationBadgeStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
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

    init {
        // Clear the unread badge on any entry into the inbox (profile bell, deep link, system
        // notification tap) — not just the profile path. The ALL first-page load below asks the
        // server to reset its count too, so the next badge refresh agrees.
        badgeStore.clearOptimistically()
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
        }
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
                    entries = cached.entries,
                    hasNextPage = cached.hasNextPage,
                    isLoading = false,
                    isRefreshing = false,
                    isPaginating = false,
                    errorMessage = null
                )
            }
            // Silent background refresh; UI shows cached data immediately, no spinner.
            load(reset = true, isInitial = false, quiet = true)
        } else {
            nextPage = 1
            _uiState.update {
                it.copy(
                    filter = filter,
                    items = emptyList(),
                    entries = emptyList(),
                    hasNextPage = true,
                    isLoading = true,
                    isRefreshing = false,
                    isPaginating = false,
                    errorMessage = null
                )
            }
            load(reset = true, isInitial = true)
        }
    }

    private fun load(reset: Boolean, isInitial: Boolean, refreshing: Boolean = false, quiet: Boolean = false) {
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
        // Reset unread count only on first page load with the All filter (matches AniList web behavior).
        val resetUnread = reset && filter == NotificationFilter.ALL

        loadJob = viewModelScope.launch {
            val result = getNotifications.getPage(
                page = nextPage,
                typeFilter = filter.types,
                resetUnreadCount = resetUnread
            )
            // Late response from a previous filter — drop it.
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
                    _uiState.update {
                        it.copy(
                            items = merged,
                            entries = grouped,
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            hasNextPage = page.hasNextPage,
                            errorMessage = null
                        )
                    }
                    snapshots[filter] = FilterSnapshot(
                        items = merged,
                        entries = grouped,
                        nextPage = if (page.hasNextPage) nextPage + 1 else nextPage,
                        hasNextPage = page.hasNextPage
                    )
                    if (page.hasNextPage) nextPage++
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
}
