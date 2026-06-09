package com.anisync.android.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.MediaFollowingEntry
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaFollowingViewModel @Inject constructor(
    private val detailsRepository: DetailsRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<MediaFollowingEntry>>(emptyList())
    val entries: StateFlow<List<MediaFollowingEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentPage = 1
    private var hasNextPage = true
    private var currentMediaId: Int? = null

    fun loadInitial(mediaId: Int) {
        if (currentMediaId == mediaId) return
        currentMediaId = mediaId
        currentPage = 1
        hasNextPage = true
        _entries.value = emptyList()
        fetchNextPage()
    }

    fun fetchNextPage() {
        if (_isLoading.value || !hasNextPage) return
        val mediaId = currentMediaId ?: return
        // Set synchronously so a fast scroll firing fetchNextPage() again before the
        // coroutine starts can't re-fetch the same page (double currentPage++ skips a page).
        _isLoading.value = true

        viewModelScope.launch {
            when (val result = detailsRepository.getMediaFollowing(
                mediaId = mediaId,
                page = currentPage,
                perPage = PAGE_SIZE
            )) {
                is Result.Success -> {
                    val (newEntries, hasNext) = result.data
                    // Dedupe by userId so a user already shown on an earlier page doesn't reappear
                    // and crash LazyColumn with a duplicate key.
                    _entries.value = (_entries.value + newEntries).distinctBy { it.userId }
                    hasNextPage = hasNext
                    if (hasNext) currentPage++
                }
                is Result.Error -> {
                    // Silent — sheet stays as-is
                }
            }
            _isLoading.value = false
        }
    }

    companion object {
        private const val PAGE_SIZE = 25
    }
}
