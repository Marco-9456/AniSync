package com.anisync.android.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.anisync.android.R
import com.anisync.android.data.AuthRepository
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.domain.ActivityLikeNotification
import com.anisync.android.domain.ActivityMentionNotification
import com.anisync.android.domain.ActivityMessageNotification
import com.anisync.android.domain.ActivityReplyLikeNotification
import com.anisync.android.domain.ActivityReplyNotification
import com.anisync.android.domain.ActivityReplySubscribedNotification
import com.anisync.android.domain.AiringNotification
import com.anisync.android.domain.AiringSchedule
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationRepository
import com.anisync.android.domain.PreferencesRepository
import com.anisync.android.domain.ThreadCommentLikeNotification
import com.anisync.android.domain.ThreadCommentMentionNotification
import com.anisync.android.domain.ThreadCommentReplyNotification
import com.anisync.android.domain.ThreadCommentSubscribedNotification
import com.anisync.android.domain.ThreadLikeNotification
import com.anisync.android.type.MediaType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.anisync.android.domain.Result as DomainResult

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val libraryDao: LibraryDao,
    private val imageLoader: ImageLoader,
    private val authRepository: AuthRepository,
    private val notificationPreferences: com.anisync.android.data.NotificationPreferences
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val MAX_NOTIFICATION_PAGES = 3
        
        // Two-tier upcoming notification system
        private const val ADVANCE_NOTICE_HOURS = 12 // First notification: "Episode 1 airs tomorrow at X"
        private const val IMMINENT_NOTICE_HOURS = 2  // Second notification: "Episode 1 airs in 2 hours"

        private const val GROUP_KEY_AIRING = "com.anisync.android.AIRING_GROUP"
        private const val GROUP_KEY_PLANNING = "com.anisync.android.PLANNING_GROUP"
        private const val SUMMARY_ID = 0
        private const val AIRING_NOTIFICATION_BASE_ID = 1000
        private const val PLANNING_NOTIFICATION_BASE_ID = 100000
        private const val UPCOMING_ADVANCE_NOTIFICATION_BASE_ID = 200000  // 12h notice
        private const val UPCOMING_IMMINENT_NOTIFICATION_BASE_ID = 300000 // 2h notice
        private const val SOCIAL_NOTIFICATION_BASE_ID = 400000

        private const val GROUP_KEY_SOCIAL = "com.anisync.android.SOCIAL_GROUP"
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        // Skip if user is not authenticated
        if (authRepository.getToken() == null) {
            Log.d(TAG, "Skipping notification check - user not authenticated")
            return androidx.work.ListenableWorker.Result.success()
        }

        return try {
            val isFirstRun = !preferencesRepository.hasNotificationsEverRun()

            if (isFirstRun) {
                // On first run, establish baseline without showing any notifications.
                // This prevents spamming the user with all historical notifications.
                performBaselineSync()
                preferencesRepository.markNotificationsHaveRun()
                Log.d(TAG, "First run: Baseline sync completed. Future runs will notify.")
            } else {
                // Normal run: check and notify based on granular settings
                if (notificationPreferences.watchingEnabled.value) {
                    checkWatchingListNotifications()
                } else {
                    Log.d(TAG, "Skipping watching list notifications - disabled by user")
                }
                
                if (notificationPreferences.upcomingEnabled.value) {
                    checkUpcomingPlanningEpisodes()
                } else {
                    Log.d(TAG, "Skipping upcoming notifications - disabled by user")
                }
                
                if (notificationPreferences.planningEnabled.value) {
                    checkPlanningFirstEpisodes()
                } else {
                    Log.d(TAG, "Skipping planning notifications - disabled by user")
                }

                // Social/Forum notifications — check if ANY forum or activity type is enabled
                val anySocialEnabled = notificationPreferences.threadCommentReplyEnabled.value ||
                    notificationPreferences.threadSubscribedEnabled.value ||
                    notificationPreferences.threadCommentMentionEnabled.value ||
                    notificationPreferences.threadLikeEnabled.value ||
                    notificationPreferences.threadCommentLikeEnabled.value ||
                    notificationPreferences.activityReplyEnabled.value ||
                    notificationPreferences.activityMentionEnabled.value ||
                    notificationPreferences.activityLikeEnabled.value ||
                    notificationPreferences.activityMessageEnabled.value

                if (anySocialEnabled) {
                    checkSocialNotifications()
                } else {
                    Log.d(TAG, "Skipping social notifications - all forum/activity types disabled")
                }
            }

            androidx.work.ListenableWorker.Result.success()
        } catch (e: com.anisync.android.data.util.ApiError.RateLimited) {
            Log.w(TAG, "Rate limited (wait ${e.retryAfterSeconds}s). Scheduling retry (attempt ${runAttemptCount + 1})")
            androidx.work.ListenableWorker.Result.retry()
        } catch (e: com.anisync.android.data.util.ApiError.Unauthorized) {
            Log.w(TAG, "Unauthorized — session expired, skipping notification check")
            androidx.work.ListenableWorker.Result.failure()
        } catch (e: com.anisync.android.data.util.ApiError.ServerError) {
            Log.e(TAG, "Server error ${e.statusCode} on attempt $runAttemptCount")
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                androidx.work.ListenableWorker.Result.retry()
            } else {
                androidx.work.ListenableWorker.Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed on attempt $runAttemptCount", e)
            
            val isRecoverable = e is java.net.UnknownHostException ||
                e is java.net.SocketTimeoutException ||
                e is java.io.IOException
            
            if (isRecoverable && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Log.d(TAG, "Scheduling retry (attempt ${runAttemptCount + 1})")
                androidx.work.ListenableWorker.Result.retry()
            } else {
                Log.w(TAG, "Worker failed permanently after $runAttemptCount attempts")
                androidx.work.ListenableWorker.Result.failure()
            }
        }
    }

    /**
     * On first run, fetch the current state and set baselines WITHOUT notifying.
     * This prevents spamming the user with all historical notifications.
     */
    private suspend fun performBaselineSync() {
        // 1. Set baseline for Watching notifications
        val repoResult = notificationRepository.getNotifications(1)
        if (repoResult is DomainResult.Success) {
            val allNotifications = repoResult.data
            val latestAiringId = allNotifications
                .filterIsInstance<AiringNotification>()
                .maxOfOrNull { it.id } ?: 0
            if (latestAiringId > 0) {
                preferencesRepository.setLastNotifiedId(latestAiringId)
                Log.d(TAG, "Baseline: Set lastNotifiedId to $latestAiringId")
            }
            val latestSocialId = allNotifications
                .filter { isSocialNotification(it) }
                .maxOfOrNull { it.id } ?: 0
            if (latestSocialId > 0) {
                preferencesRepository.setLastSocialNotifiedId(latestSocialId)
                Log.d(TAG, "Baseline: Set lastSocialNotifiedId to $latestSocialId")
            }
        }

        // 2. Mark all current planning items as "already notified"
        val planningEntries = libraryDao.getByType(MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }
        val planningMediaIds = planningEntries.map { it.mediaId }

        // Mark all planning items that have already aired Ep1 as notified
        val airedResult = notificationRepository.getFirstEpisodeAirings(planningMediaIds)
        if (airedResult is DomainResult.Success) {
            for (airing in airedResult.data) {
                preferencesRepository.markPlanningMediaAsNotified(airing.mediaId)
            }
            Log.d(TAG, "Baseline: Marked ${airedResult.data.size} planning items as already notified")
        }

        // Mark all upcoming items as notified (so we don't alert about already-upcoming shows)
        val upcomingResult = notificationRepository.getUpcomingFirstEpisodes(planningMediaIds, ADVANCE_NOTICE_HOURS)
        if (upcomingResult is DomainResult.Success) {
            for (airing in upcomingResult.data) {
                preferencesRepository.markUpcomingAiringNotified(airing.id)
            }
            Log.d(TAG, "Baseline: Marked ${upcomingResult.data.size} upcoming episodes as already notified")
        }
    }

    private suspend fun checkWatchingListNotifications() {
        val allNewAiring = mutableListOf<AiringNotification>()
        val lastNotifiedId = preferencesRepository.getLastNotifiedId()
        var currentPage = 1
        var hasMore = true
        
        // Get planning list media IDs to exclude Episode 1 notifications for them
        // (those are handled by checkPlanningFirstEpisodes with "Add to Watching" action)
        val planningMediaIds = if (notificationPreferences.planningEnabled.value) {
            libraryDao.getByType(MediaType.ANIME)
                .filter { it.status == LibraryStatus.PLANNING }
                .map { it.mediaId }
                .toSet()
        } else {
            emptySet()
        }

        while (hasMore && currentPage <= MAX_NOTIFICATION_PAGES) {
            val repoResult = notificationRepository.getNotifications(currentPage)
            
            when (repoResult) {
                is DomainResult.Success -> {
                    val notifications = repoResult.data
                    
                    val newOnThisPage = notifications
                        .filterIsInstance<AiringNotification>()
                        .filter { it.id > lastNotifiedId }
                        // Skip Episode 1 for Planning items (handled by checkPlanningFirstEpisodes)
                        .filter { notification ->
                            val isEpisode1ForPlanning = notification.episode == 1 && 
                                notification.media?.id in planningMediaIds
                            !isEpisode1ForPlanning
                        }
                    
                    // Check if we've passed all new airing notifications on this page
                    val allAiringOnPage = notifications.filterIsInstance<AiringNotification>()
                    val hasOlderAiring = allAiringOnPage.any { it.id <= lastNotifiedId }

                    if (newOnThisPage.isEmpty() && hasOlderAiring) {
                        // All airing notifications on this page are old
                        hasMore = false
                    } else if (newOnThisPage.isEmpty() && allAiringOnPage.isEmpty()) {
                        // No airing notifications on this page at all (e.g. only thread notifications)
                        // Continue to next page to find airing notifications
                        currentPage++
                        if (notifications.size < 20) hasMore = false
                    } else {
                        allNewAiring.addAll(newOnThisPage)
                        currentPage++
                        if (notifications.size < 20) hasMore = false
                    }
                }
                is DomainResult.Error -> {
                    Log.e(TAG, "Failed to fetch notifications page $currentPage: ${repoResult.message}", repoResult.exception)
                    hasMore = false
                }
            }
        }
        
        if (allNewAiring.isNotEmpty()) {
            val sortedAiring = allNewAiring.sortedBy { it.id }

            // Apply user-configured streaming delay. AniList stamps AiringNotification.createdAt
            // at the official airing moment, so an episode is "due" once
            // now >= createdAt + delay. Defer anything not yet due to the next worker run.
            //
            // To keep dedup correct, never advance lastNotifiedId past a deferred id —
            // a deferred id with a smaller value than an already-due id forces us to leave
            // the high-water mark at deferredId-1 so the deferred one is reprocessed next poll.
            val delaySeconds = notificationPreferences.streamingDelayMinutes.value * 60L
            val nowSeconds = System.currentTimeMillis() / 1000
            val cutoff = if (delaySeconds > 0L) {
                sortedAiring.firstOrNull { (nowSeconds - it.createdAt) < delaySeconds }?.id
                    ?: Int.MAX_VALUE
            } else {
                Int.MAX_VALUE
            }
            val toEmit = sortedAiring.filter { it.id < cutoff }
            val deferredCount = sortedAiring.size - toEmit.size

            for (notification in toEmit) {
                showNotification(notification)
            }

            if (toEmit.size >= 3) {
                showSummaryNotification(toEmit)
            }

            if (toEmit.isNotEmpty()) {
                val maxId = toEmit.maxOf { it.id }
                preferencesRepository.setLastNotifiedId(maxId)
            }

            Log.d(
                TAG,
                "Processed ${toEmit.size} new airing notifications (deferred $deferredCount due to streaming delay of ${delaySeconds / 60} min)"
            )
        }
    }

    /**
     * Check for new social/forum notifications (thread replies, mentions, likes, subscriptions).
     * Each type is gated behind its own preference toggle.
     */
    private suspend fun checkSocialNotifications() {
        val lastSocialId = preferencesRepository.getLastSocialNotifiedId()
        val allNewSocial = mutableListOf<Notification>()
        var currentPage = 1
        var hasMore = true

        while (hasMore && currentPage <= MAX_NOTIFICATION_PAGES) {
            val repoResult = notificationRepository.getNotifications(currentPage)

            when (repoResult) {
                is DomainResult.Success -> {
                    val notifications = repoResult.data

                    val socialOnPage = notifications.filter { notification ->
                        notification.id > lastSocialId && isSocialNotification(notification)
                    }

                    val olderSocialExists = notifications.any { notification ->
                        notification.id <= lastSocialId && isSocialNotification(notification)
                    }

                    if (socialOnPage.isEmpty() && olderSocialExists) {
                        hasMore = false
                    } else if (socialOnPage.isEmpty()) {
                        currentPage++
                        if (notifications.size < 20) hasMore = false
                    } else {
                        allNewSocial.addAll(socialOnPage)
                        currentPage++
                        if (notifications.size < 20) hasMore = false
                    }
                }
                is DomainResult.Error -> {
                    Log.e(TAG, "Failed to fetch social notifications page $currentPage: ${repoResult.message}", repoResult.exception)
                    hasMore = false
                }
            }
        }

        if (allNewSocial.isNotEmpty()) {
            val sorted = allNewSocial.sortedBy { it.id }

            for (notification in sorted) {
                // Respect per-type preferences
                val shouldNotify = when (notification) {
                    is ThreadCommentReplyNotification -> notificationPreferences.threadCommentReplyEnabled.value
                    is ThreadCommentSubscribedNotification -> notificationPreferences.threadSubscribedEnabled.value
                    is ThreadCommentMentionNotification -> notificationPreferences.threadCommentMentionEnabled.value
                    is ThreadLikeNotification -> notificationPreferences.threadLikeEnabled.value
                    is ThreadCommentLikeNotification -> notificationPreferences.threadCommentLikeEnabled.value
                    is ActivityReplyNotification,
                    is ActivityReplySubscribedNotification -> notificationPreferences.activityReplyEnabled.value
                    is ActivityMentionNotification -> notificationPreferences.activityMentionEnabled.value
                    is ActivityLikeNotification,
                    is ActivityReplyLikeNotification -> notificationPreferences.activityLikeEnabled.value
                    is ActivityMessageNotification -> notificationPreferences.activityMessageEnabled.value
                    else -> false
                }
                if (shouldNotify) {
                    showSocialNotification(notification)
                }
            }

            val maxId = sorted.maxOf { it.id }
            preferencesRepository.setLastSocialNotifiedId(maxId)
            Log.d(TAG, "Processed ${sorted.size} new social notifications")
        }
    }

    private fun isSocialNotification(notification: Notification): Boolean {
        return notification is ThreadCommentReplyNotification ||
            notification is ThreadCommentSubscribedNotification ||
            notification is ThreadCommentMentionNotification ||
            notification is ThreadLikeNotification ||
            notification is ThreadCommentLikeNotification ||
            notification is ActivityReplyNotification ||
            notification is ActivityReplySubscribedNotification ||
            notification is ActivityMentionNotification ||
            notification is ActivityLikeNotification ||
            notification is ActivityReplyLikeNotification ||
            notification is ActivityMessageNotification
    }

    /**
     * Display a social/forum notification using the appropriate channel.
     */
    private suspend fun showSocialNotification(notification: Notification) {
        val data = when (notification) {
            is ThreadCommentReplyNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "💬 Reply on \"${notification.threadTitle}\"",
                    content = "$userName ${notification.context}",
                    channelId = NotificationChannels.THREAD_COMMENT_REPLY_CHANNEL_ID,
                    threadId = notification.threadId,
                    commentId = notification.commentId
                )
            }
            is ThreadCommentSubscribedNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "🔔 Update on \"${notification.threadTitle}\"",
                    content = "$userName ${notification.context}",
                    channelId = NotificationChannels.THREAD_SUBSCRIBED_CHANNEL_ID,
                    threadId = notification.threadId,
                    commentId = notification.commentId
                )
            }
            is ThreadCommentMentionNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "📢 Mentioned in \"${notification.threadTitle}\"",
                    content = "$userName ${notification.context}",
                    channelId = NotificationChannels.THREAD_COMMENT_MENTION_CHANNEL_ID,
                    threadId = notification.threadId,
                    commentId = notification.commentId
                )
            }
            is ThreadLikeNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "❤️ Thread liked",
                    content = "$userName liked your thread \"${notification.threadTitle}\"",
                    channelId = NotificationChannels.THREAD_LIKE_CHANNEL_ID,
                    threadId = notification.threadId,
                    commentId = null
                )
            }
            is ThreadCommentLikeNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "❤️ Comment liked",
                    content = "$userName liked your comment in \"${notification.threadTitle}\"",
                    channelId = NotificationChannels.THREAD_COMMENT_LIKE_CHANNEL_ID,
                    threadId = notification.threadId,
                    commentId = notification.commentId
                )
            }
            is ActivityReplyNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "💬 Activity reply",
                    content = "$userName ${notification.context}",
                    channelId = NotificationChannels.ACTIVITY_REPLY_CHANNEL_ID,
                    activityId = notification.activityId
                )
            }
            is ActivityReplySubscribedNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "🔔 Activity update",
                    content = "$userName ${notification.context}",
                    channelId = NotificationChannels.ACTIVITY_REPLY_CHANNEL_ID,
                    activityId = notification.activityId
                )
            }
            is ActivityMentionNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "📢 Mentioned in activity",
                    content = "$userName ${notification.context}",
                    channelId = NotificationChannels.ACTIVITY_MENTION_CHANNEL_ID,
                    activityId = notification.activityId
                )
            }
            is ActivityLikeNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "❤️ Activity liked",
                    content = "$userName ${notification.context}",
                    channelId = NotificationChannels.ACTIVITY_LIKE_CHANNEL_ID,
                    activityId = notification.activityId
                )
            }
            is ActivityReplyLikeNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "❤️ Reply liked",
                    content = "$userName ${notification.context}",
                    channelId = NotificationChannels.ACTIVITY_LIKE_CHANNEL_ID,
                    activityId = notification.activityId
                )
            }
            is ActivityMessageNotification -> {
                val userName = notification.user?.name ?: "Someone"
                SocialNotificationData(
                    title = "✉️ New message",
                    content = "$userName ${notification.context}",
                    channelId = NotificationChannels.ACTIVITY_MESSAGE_CHANNEL_ID,
                    activityId = notification.activityId
                )
            }
            else -> return
        }

        val notificationId = SOCIAL_NOTIFICATION_BASE_ID + notification.id
        val deepLinkUri = when {
            data.activityId != null -> "anisync://activity/${data.activityId}"
            data.commentId != null -> "anisync://forum/thread/${data.threadId}?commentId=${data.commentId}"
            data.threadId != null -> "anisync://forum/thread/${data.threadId}"
            else -> "anisync://details/0"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon: Bitmap? = when (notification) {
            is ThreadCommentReplyNotification -> notification.user?.avatarUrl
            is ThreadCommentSubscribedNotification -> notification.user?.avatarUrl
            is ThreadCommentMentionNotification -> notification.user?.avatarUrl
            is ThreadLikeNotification -> notification.user?.avatarUrl
            is ThreadCommentLikeNotification -> notification.user?.avatarUrl
            is ActivityReplyNotification -> notification.user?.avatarUrl
            is ActivityReplySubscribedNotification -> notification.user?.avatarUrl
            is ActivityMentionNotification -> notification.user?.avatarUrl
            is ActivityLikeNotification -> notification.user?.avatarUrl
            is ActivityReplyLikeNotification -> notification.user?.avatarUrl
            is ActivityMessageNotification -> notification.user?.avatarUrl
            else -> null
        }?.let { loadImage(it) }

        val builder = NotificationCompat.Builder(applicationContext, data.channelId)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(data.title)
            .setContentText(data.content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_SOCIAL)
            .setContentIntent(pendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    private data class SocialNotificationData(
        val title: String,
        val content: String,
        val channelId: String,
        val threadId: Int? = null,
        val commentId: Int? = null,
        val activityId: Int? = null
    )

    /**
     * Check for upcoming Episode 1 airings for Planning list items.
     * Implements a two-tier notification system:
     * - 12 hours before: "Episode 1 airs tomorrow at 3:00 PM"
     * - 2 hours before: "Episode 1 is airing in 2 hours"
     */
    private suspend fun checkUpcomingPlanningEpisodes() {
        val planningEntries = libraryDao.getByType(MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }

        if (planningEntries.isEmpty()) return

        val mediaIds = planningEntries.map { it.mediaId }
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        
        // Get upcoming Episode 1 airings within next 12 hours
        val result = notificationRepository.getUpcomingFirstEpisodes(mediaIds, ADVANCE_NOTICE_HOURS)

        if (result is DomainResult.Success) {
            val upcomingAirings = result.data
            
            // Clean up old tracking - only keep IDs that are still upcoming
            val currentAiringIds = upcomingAirings.map { it.id }.toSet()
            preferencesRepository.cleanupOldUpcomingAirings(currentAiringIds)
            
            for (airing in upcomingAirings) {
                val hoursUntil = ((airing.airingAt - currentTimeSeconds) / 3600).toInt()
                
                // Determine which tier of notification to send
                when {
                    hoursUntil <= IMMINENT_NOTICE_HOURS -> {
                        // 2 hours or less: Send imminent notification
                        val imminentKey = "imminent_${airing.id}"
                        if (!preferencesRepository.hasNotifiedWithKey(imminentKey)) {
                            showImminentEpisodeNotification(airing, hoursUntil)
                            preferencesRepository.markNotifiedWithKey(imminentKey)
                            preferencesRepository.markUpcomingAiringNotified(airing.id)
                        }
                    }
                    hoursUntil <= ADVANCE_NOTICE_HOURS -> {
                        // Between 2-12 hours: Send advance notification (only once)
                        val advanceKey = "advance_${airing.id}"
                        if (!preferencesRepository.hasNotifiedWithKey(advanceKey)) {
                            showAdvanceEpisodeNotification(airing)
                            preferencesRepository.markNotifiedWithKey(advanceKey)
                            preferencesRepository.markUpcomingAiringNotified(airing.id)
                        }
                    }
                }
            }
        } else if (result is DomainResult.Error) {
            Log.e(TAG, "Failed to fetch upcoming episodes: ${result.message}", result.exception)
        }
    }

    /**
     * Check for already-aired Episode 1 for Planning list items.
     * This is a reactive notification - "Episode 1 is now available!"
     * 
     * IMPORTANT: Skips media that have already received upcoming notifications
     * to prevent duplicate alerts.
     */
    private suspend fun checkPlanningFirstEpisodes() {
        val planningEntries = libraryDao.getByType(MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }

        val mediaIds = planningEntries.map { it.mediaId }
        
        // NOTE: We intentionally do NOT call cleanupOrphanedPlanningIds here.
        // Once an "Episode 1 aired" notification has been sent for a media ID,
        // that fact should persist permanently. If a user moves an item out of
        // Planning and then back, they should not receive the notification again.
        
        if (planningEntries.isEmpty()) return

        val notifiedIds = preferencesRepository.getNotifiedPlanningMediaIds()
        val unnotifiedIds = mediaIds.filter { it !in notifiedIds }
        if (unnotifiedIds.isEmpty()) return

        // Get upcoming airing IDs to cross-reference and avoid duplicates
        val upcomingNotifiedAiringIds = preferencesRepository.getNotifiedUpcomingAiringIds()

        val result = notificationRepository.getFirstEpisodeAirings(unnotifiedIds)

        if (result is DomainResult.Success) {
            for (airing in result.data) {
                // Skip if we already sent an upcoming notification for this specific airing
                // This prevents "Episode 1 is now available" after "Episode 1 airs in 2 hours"
                if (airing.id in upcomingNotifiedAiringIds) {
                    Log.d(TAG, "Skipping '${airing.mediaTitle}' - already notified as upcoming")
                    // Still mark as notified to prevent future checks
                    preferencesRepository.markPlanningMediaAsNotified(airing.mediaId)
                    continue
                }
                
                showPlanningFirstEpisodeNotification(airing)
                preferencesRepository.markPlanningMediaAsNotified(airing.mediaId)
            }
            if (result.data.isNotEmpty()) {
                Log.d(TAG, "Processed ${result.data.size} planning first episode notifications")
            }
        } else if (result is DomainResult.Error) {
            Log.e(TAG, "Failed to fetch planning first episodes: ${result.message}", result.exception)
        }
    }

    private suspend fun showNotification(notification: AiringNotification) {
        val notificationId = AIRING_NOTIFICATION_BASE_ID + notification.id
        val media = notification.media
        val title = media?.title ?: "New Episode"
        val content = "Episode ${notification.episode} has aired!"
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://details/${media?.id ?: 0}"))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon: Bitmap? = media?.coverUrl?.let { url ->
            loadImage(url)
        }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.AIRING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_AIRING)
            .setContentIntent(pendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Show advance notification (12 hours before).
     * Example: "Episode 1 airs tomorrow at 3:00 PM"
     */
    private suspend fun showAdvanceEpisodeNotification(airing: AiringSchedule) {
        val notificationId = UPCOMING_ADVANCE_NOTIFICATION_BASE_ID + airing.id
        val title = "📅 Premiere Alert: ${airing.mediaTitle}"
        
        // Format the airing time in user's locale
        val airingDate = java.util.Date(airing.airingAt * 1000)
        val timeFormat = DateFormat.getTimeFormat(applicationContext)
        val formattedTime = timeFormat.format(airingDate)
        
        // Determine if it's today or tomorrow
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        calendar.time = airingDate
        val airingDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        
        val dayPrefix = when {
            airingDay == currentDay -> "today"
            airingDay == currentDay + 1 -> "tomorrow"
            else -> {
                val dateFormat = DateFormat.getDateFormat(applicationContext)
                "on ${dateFormat.format(airingDate)}"
            }
        }
        
        val content = "Episode 1 airs $dayPrefix at $formattedTime"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://details/${airing.mediaId}"))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon: Bitmap? = airing.mediaCoverUrl?.let { url ->
            loadImage(url)
        }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.UPCOMING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_PLANNING)
            .setContentIntent(pendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "Sent advance notification for ${airing.mediaTitle}: $content")
    }

    /**
     * Show imminent notification (2 hours or less before).
     * Example: "Episode 1 is airing in 2 hours!"
     */
    private suspend fun showImminentEpisodeNotification(airing: AiringSchedule, hoursUntil: Int) {
        val notificationId = UPCOMING_IMMINENT_NOTIFICATION_BASE_ID + airing.id
        val title = "🔔 Starting Soon: ${airing.mediaTitle}"
        
        val content = when {
            hoursUntil < 1 -> "Episode 1 is airing in less than an hour!"
            hoursUntil == 1 -> "Episode 1 is airing in about an hour!"
            else -> "Episode 1 is airing in $hoursUntil hours!"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://details/${airing.mediaId}"))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon: Bitmap? = airing.mediaCoverUrl?.let { url ->
            loadImage(url)
        }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.UPCOMING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Higher priority for imminent notifications
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_PLANNING)
            .setContentIntent(pendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "Sent imminent notification for ${airing.mediaTitle}: $content")
    }

    private suspend fun showPlanningFirstEpisodeNotification(airing: AiringSchedule) {
        val notificationId = PLANNING_NOTIFICATION_BASE_ID + airing.mediaId
        val title = "${airing.mediaTitle} has started!"
        val content = "Episode 1 is now available"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://details/${airing.mediaId}"))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create "Add to Watching" action button
        val addToWatchingIntent = Intent(applicationContext, AddToWatchingReceiver::class.java).apply {
            action = AddToWatchingReceiver.ACTION_ADD_TO_WATCHING
            putExtra(AddToWatchingReceiver.EXTRA_MEDIA_ID, airing.mediaId)
            putExtra(AddToWatchingReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AddToWatchingReceiver.EXTRA_MEDIA_TITLE, airing.mediaTitle)
        }
        val addToWatchingPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationId + 1000000, // Unique request code to avoid conflicts
            addToWatchingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon: Bitmap? = airing.mediaCoverUrl?.let { url ->
            loadImage(url)
        }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.PLANNING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_PLANNING)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification, // Icon for the action
                "Add to Watching",
                addToWatchingPendingIntent
            )

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    private fun showSummaryNotification(notifications: List<AiringNotification>) {
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${notifications.size} new episodes aired")
            .setSummaryText("AniSync")

        for (notification in notifications) {
            val title = notification.media?.title ?: "Anime"
            inboxStyle.addLine("Ep ${notification.episode}: $title")
        }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.AIRING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setStyle(inboxStyle)
            .setGroup(GROUP_KEY_AIRING)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SUMMARY_ID, builder.build())
    }

    private suspend fun loadImage(url: String): Bitmap? {
        val request = ImageRequest.Builder(applicationContext)
            .data(url)
            .allowHardware(false)
            .build()

        val result = imageLoader.execute(request)
        return if (result is SuccessResult) {
            result.drawable.toBitmap()
        } else {
            null
        }
    }
    
    private fun getApplicationIcon(): Int {
        return R.drawable.ic_notification
    }
}
