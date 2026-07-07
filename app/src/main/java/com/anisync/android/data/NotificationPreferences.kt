package com.anisync.android.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Granular notification preferences.
 * Allows users to enable/disable specific notification types independently.
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Watching list - new episodes for shows you're watching
    private val _watchingEnabled = MutableStateFlow(prefs.getBoolean(KEY_WATCHING_ENABLED, true))
    val watchingEnabled: StateFlow<Boolean> = _watchingEnabled.asStateFlow()

    // Planning list - when Episode 1 airs for shows in your planning list
    private val _planningEnabled = MutableStateFlow(prefs.getBoolean(KEY_PLANNING_ENABLED, true))
    val planningEnabled: StateFlow<Boolean> = _planningEnabled.asStateFlow()

    // Upcoming alerts - proactive "airing soon" notifications
    private val _upcomingEnabled = MutableStateFlow(prefs.getBoolean(KEY_UPCOMING_ENABLED, true))
    val upcomingEnabled: StateFlow<Boolean> = _upcomingEnabled.asStateFlow()

    // Forum - thread comment replies
    private val _threadCommentReplyEnabled = MutableStateFlow(prefs.getBoolean(KEY_THREAD_COMMENT_REPLY_ENABLED, true))
    val threadCommentReplyEnabled: StateFlow<Boolean> = _threadCommentReplyEnabled.asStateFlow()

    // Forum - subscribed thread updates
    private val _threadSubscribedEnabled = MutableStateFlow(prefs.getBoolean(KEY_THREAD_SUBSCRIBED_ENABLED, true))
    val threadSubscribedEnabled: StateFlow<Boolean> = _threadSubscribedEnabled.asStateFlow()

    // Forum - thread comment mentions
    private val _threadCommentMentionEnabled = MutableStateFlow(prefs.getBoolean(KEY_THREAD_COMMENT_MENTION_ENABLED, true))
    val threadCommentMentionEnabled: StateFlow<Boolean> = _threadCommentMentionEnabled.asStateFlow()

    // Forum - thread likes
    private val _threadLikeEnabled = MutableStateFlow(prefs.getBoolean(KEY_THREAD_LIKE_ENABLED, true))
    val threadLikeEnabled: StateFlow<Boolean> = _threadLikeEnabled.asStateFlow()

    // Forum - thread comment likes
    private val _threadCommentLikeEnabled = MutableStateFlow(prefs.getBoolean(KEY_THREAD_COMMENT_LIKE_ENABLED, true))
    val threadCommentLikeEnabled: StateFlow<Boolean> = _threadCommentLikeEnabled.asStateFlow()

    // Activity - replies to your status / subscribed replies
    private val _activityReplyEnabled = MutableStateFlow(prefs.getBoolean(KEY_ACTIVITY_REPLY_ENABLED, true))
    val activityReplyEnabled: StateFlow<Boolean> = _activityReplyEnabled.asStateFlow()

    // Activity - @mentions inside an activity
    private val _activityMentionEnabled = MutableStateFlow(prefs.getBoolean(KEY_ACTIVITY_MENTION_ENABLED, true))
    val activityMentionEnabled: StateFlow<Boolean> = _activityMentionEnabled.asStateFlow()

    // Activity - likes on your activity / reply
    private val _activityLikeEnabled = MutableStateFlow(prefs.getBoolean(KEY_ACTIVITY_LIKE_ENABLED, true))
    val activityLikeEnabled: StateFlow<Boolean> = _activityLikeEnabled.asStateFlow()

    // Activity - direct messages
    private val _activityMessageEnabled = MutableStateFlow(prefs.getBoolean(KEY_ACTIVITY_MESSAGE_ENABLED, true))
    val activityMessageEnabled: StateFlow<Boolean> = _activityMessageEnabled.asStateFlow()

    // Activity - new followers
    private val _followsEnabled = MutableStateFlow(prefs.getBoolean(KEY_FOLLOWS_ENABLED, true))
    val followsEnabled: StateFlow<Boolean> = _followsEnabled.asStateFlow()

    // Streaming availability delay (minutes) for "episode aired" notifications.
    // Lets users on streaming sites that post episodes after the official airing time
    // postpone the notification so it lines up with when the episode is actually watchable.
    private val _streamingDelayMinutes = MutableStateFlow(
        prefs.getInt(KEY_STREAMING_DELAY_MINUTES, 0).coerceIn(MIN_STREAMING_DELAY_MINUTES, MAX_STREAMING_DELAY_MINUTES)
    )
    val streamingDelayMinutes: StateFlow<Int> = _streamingDelayMinutes.asStateFlow()

    fun setWatchingEnabled(enabled: Boolean) {
        _watchingEnabled.value = enabled
        prefs.edit().putBoolean(KEY_WATCHING_ENABLED, enabled).apply()
    }

    fun setPlanningEnabled(enabled: Boolean) {
        _planningEnabled.value = enabled
        prefs.edit().putBoolean(KEY_PLANNING_ENABLED, enabled).apply()
    }

    fun setUpcomingEnabled(enabled: Boolean) {
        _upcomingEnabled.value = enabled
        prefs.edit().putBoolean(KEY_UPCOMING_ENABLED, enabled).apply()
    }

    fun setThreadCommentReplyEnabled(enabled: Boolean) {
        _threadCommentReplyEnabled.value = enabled
        prefs.edit().putBoolean(KEY_THREAD_COMMENT_REPLY_ENABLED, enabled).apply()
    }

    fun setThreadSubscribedEnabled(enabled: Boolean) {
        _threadSubscribedEnabled.value = enabled
        prefs.edit().putBoolean(KEY_THREAD_SUBSCRIBED_ENABLED, enabled).apply()
    }

    fun setThreadCommentMentionEnabled(enabled: Boolean) {
        _threadCommentMentionEnabled.value = enabled
        prefs.edit().putBoolean(KEY_THREAD_COMMENT_MENTION_ENABLED, enabled).apply()
    }

    fun setThreadLikeEnabled(enabled: Boolean) {
        _threadLikeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_THREAD_LIKE_ENABLED, enabled).apply()
    }

    fun setThreadCommentLikeEnabled(enabled: Boolean) {
        _threadCommentLikeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_THREAD_COMMENT_LIKE_ENABLED, enabled).apply()
    }

    fun setActivityReplyEnabled(enabled: Boolean) {
        _activityReplyEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ACTIVITY_REPLY_ENABLED, enabled).apply()
    }

    fun setActivityMentionEnabled(enabled: Boolean) {
        _activityMentionEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ACTIVITY_MENTION_ENABLED, enabled).apply()
    }

    fun setActivityLikeEnabled(enabled: Boolean) {
        _activityLikeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ACTIVITY_LIKE_ENABLED, enabled).apply()
    }

    fun setActivityMessageEnabled(enabled: Boolean) {
        _activityMessageEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ACTIVITY_MESSAGE_ENABLED, enabled).apply()
    }

    fun setFollowsEnabled(enabled: Boolean) {
        _followsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_FOLLOWS_ENABLED, enabled).apply()
    }

    fun setStreamingDelayMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_STREAMING_DELAY_MINUTES, MAX_STREAMING_DELAY_MINUTES)
        _streamingDelayMinutes.value = clamped
        prefs.edit().putInt(KEY_STREAMING_DELAY_MINUTES, clamped).apply()
    }

    /**
     * Reset all notification preferences to default (all enabled).
     */
    fun resetToDefaults() {
        setWatchingEnabled(true)
        setPlanningEnabled(true)
        setUpcomingEnabled(true)
        setThreadCommentReplyEnabled(true)
        setThreadSubscribedEnabled(true)
        setThreadCommentMentionEnabled(true)
        setThreadLikeEnabled(true)
        setThreadCommentLikeEnabled(true)
        setActivityReplyEnabled(true)
        setActivityMentionEnabled(true)
        setActivityLikeEnabled(true)
        setActivityMessageEnabled(true)
        setFollowsEnabled(true)
        setStreamingDelayMinutes(0)
    }

    companion object {
        private const val PREFS_NAME = "notification_preferences"
        private const val KEY_WATCHING_ENABLED = "watching_enabled"
        private const val KEY_PLANNING_ENABLED = "planning_enabled"
        private const val KEY_UPCOMING_ENABLED = "upcoming_enabled"
        private const val KEY_THREAD_COMMENT_REPLY_ENABLED = "thread_comment_reply_enabled"
        private const val KEY_THREAD_SUBSCRIBED_ENABLED = "thread_subscribed_enabled"
        private const val KEY_THREAD_COMMENT_MENTION_ENABLED = "thread_comment_mention_enabled"
        private const val KEY_THREAD_LIKE_ENABLED = "thread_like_enabled"
        private const val KEY_THREAD_COMMENT_LIKE_ENABLED = "thread_comment_like_enabled"
        private const val KEY_ACTIVITY_REPLY_ENABLED = "activity_reply_enabled"
        private const val KEY_ACTIVITY_MENTION_ENABLED = "activity_mention_enabled"
        private const val KEY_ACTIVITY_LIKE_ENABLED = "activity_like_enabled"
        private const val KEY_ACTIVITY_MESSAGE_ENABLED = "activity_message_enabled"
        private const val KEY_FOLLOWS_ENABLED = "follows_enabled"
        private const val KEY_STREAMING_DELAY_MINUTES = "streaming_delay_minutes"
        const val MIN_STREAMING_DELAY_MINUTES = 0
        const val MAX_STREAMING_DELAY_MINUTES = 180
    }
}
