package com.anisync.android.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.UserSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Discriminator for what kind of object the like list belongs to. Activity and
 * ActivityReply share the same "list of users" shape but live behind different
 * AniList queries.
 */
sealed interface ActivityLikesTarget {
    val id: Int
    data class Activity(override val id: Int) : ActivityLikesTarget
    data class Reply(override val id: Int) : ActivityLikesTarget
}

@HiltViewModel
class ActivityLikesViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<UserSummary>>(emptyList())
    val users: StateFlow<List<UserSummary>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentTarget: ActivityLikesTarget? = null

    fun load(target: ActivityLikesTarget) {
        if (currentTarget == target) return
        currentTarget = target
        _users.value = emptyList()
        _errorMessage.value = null
        fetch(target)
    }

    private fun fetch(target: ActivityLikesTarget) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = when (target) {
                is ActivityLikesTarget.Activity -> activityRepository.getActivityLikes(target.id)
                is ActivityLikesTarget.Reply -> activityRepository.getActivityReplyLikes(target.id)
            }
            when (result) {
                is Result.Success -> _users.value = result.data
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }
}
