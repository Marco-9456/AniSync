package com.anisync.android.presentation.components.likes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.UserSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikesViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val forumRepository: ForumRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<UserSummary>>(emptyList())
    val users: StateFlow<List<UserSummary>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentTarget: LikesTarget? = null

    fun load(target: LikesTarget) {
        if (currentTarget == target) return
        currentTarget = target
        _users.value = emptyList()
        _errorMessage.value = null
        fetch(target)
    }

    private fun fetch(target: LikesTarget) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = when (target) {
                is LikesTarget.Activity -> activityRepository.getActivityLikes(target.id)
                is LikesTarget.ActivityReply -> activityRepository.getActivityReplyLikes(target.id)
                is LikesTarget.Thread -> forumRepository.getThreadLikes(target.id)
                is LikesTarget.ThreadComment -> forumRepository.getThreadCommentLikes(target.id)
            }
            when (result) {
                is Result.Success -> _users.value = result.data
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }
}
