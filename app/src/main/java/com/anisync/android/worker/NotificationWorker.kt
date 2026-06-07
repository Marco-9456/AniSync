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
import com.anisync.android.data.account.AccountStore
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.util.ApiError
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
    private val accountStore: AccountStore,
    private val notificationPreferences: com.anisync.android.data.NotificationPreferences
) : CoroutineWorker(appContext, workerParams) {

    /** Per-iteration account context threaded through the checks + notification builders. */
    private data class AcctCtx(
        val id: Int,
        val token: String,
        val name: String,
        val showLabel: Boolean,
    )

    companion object {
        private const val TAG = "NotificationWorker"
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
        // Poll every signed-in account that still has a (non-expired) token.
        val accounts = accountStore.accounts.value.filterNot { it.isExpired }
        if (accounts.isEmpty()) {
            Log.d(TAG, "No usable accounts — skipping notification check")
            return androidx.work.ListenableWorker.Result.success()
        }

        val activeId = accountStore.activeAccount.value?.id
        val showLabel = accounts.size > 1

        val anySocialEnabled = notificationPreferences.threadCommentReplyEnabled.value ||
            notificationPreferences.threadSubscribedEnabled.value ||
            notificationPreferences.threadCommentMentionEnabled.value ||
            notificationPreferences.threadLikeEnabled.value ||
            notificationPreferences.threadCommentLikeEnabled.value ||
            notificationPreferences.activityReplyEnabled.value ||
            notificationPreferences.activityMentionEnabled.value ||
            notificationPreferences.activityLikeEnabled.value ||
            notificationPreferences.activityMessageEnabled.value

        for (account in accounts) {
            val ctx = AcctCtx(account.id, account.token, account.name, showLabel)
            val isActive = account.id == activeId
            try {
                if (!preferencesRepository.hasNotificationsEverRun(ctx.id)) {
                    // First time we see this account — establish baselines silently.
                    performBaselineSync(ctx, isActive)
                    preferencesRepository.markNotificationsHaveRun(ctx.id)
                    Log.d(TAG, "Baseline sync done for ${ctx.name}")
                    continue
                }

                // AniList feed (airing + social) — works for any account via its token.
                if (notificationPreferences.watchingEnabled.value) {
                    val planningMediaIds = if (isActive && notificationPreferences.planningEnabled.value) {
                        libraryDao.getByType(ctx.id, MediaType.ANIME)
                            .filter { it.status == LibraryStatus.PLANNING }
                            .map { it.mediaId }
                            .toSet()
                    } else {
                        emptySet()
                    }
                    checkWatchingListNotifications(ctx, planningMediaIds)
                }
                if (anySocialEnabled) {
                    checkSocialNotifications(ctx)
                }

                // Planning / upcoming "Episode 1" alerts depend on the locally-cached library,
                // so they only run for the active account.
                if (isActive) {
                    if (notificationPreferences.upcomingEnabled.value) checkUpcomingPlanningEpisodes(ctx)
                    if (notificationPreferences.planningEnabled.value) checkPlanningFirstEpisodes(ctx)
                }
            } catch (e: ApiError.RateLimited) {
                // Back off the whole worker; per-account dedup makes the retry idempotent.
                Log.w(TAG, "Rate limited on ${ctx.name} (wait ${e.retryAfterSeconds}s) — retrying worker")
                return androidx.work.ListenableWorker.Result.retry()
            } catch (e: ApiError.Unauthorized) {
                // This account's token expired/revoked — flag only it; keep polling the rest.
                Log.w(TAG, "Unauthorized for ${ctx.name} — marking account expired")
                accountStore.markExpired(ctx.id)
            } catch (e: Exception) {
                // One account failing must not abort the others.
                Log.e(TAG, "Notification check failed for ${ctx.name}", e)
            }
        }

        return androidx.work.ListenableWorker.Result.success()
    }

    /**
     * On an account's first run, fetch current state and set baselines WITHOUT notifying, so the
     * user isn't spammed with that account's historical notifications. Planning/upcoming baseline
     * runs only for the active account (it uses the locally-cached library).
     */
    private suspend fun performBaselineSync(ctx: AcctCtx, isActive: Boolean) {
        val repoResult = notificationRepository.getNotifications(1, ctx.token)
        if (repoResult is DomainResult.Success) {
            val allNotifications = repoResult.data
            val latestAiringId = allNotifications
                .filterIsInstance<AiringNotification>()
                .maxOfOrNull { it.id } ?: 0
            if (latestAiringId > 0) preferencesRepository.setLastNotifiedId(ctx.id, latestAiringId)
            val latestSocialId = allNotifications
                .filter { isSocialNotification(it) }
                .maxOfOrNull { it.id } ?: 0
            if (latestSocialId > 0) preferencesRepository.setLastSocialNotifiedId(ctx.id, latestSocialId)
        }

        if (!isActive) return

        val planningEntries = libraryDao.getByType(ctx.id, MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }
        val planningMediaIds = planningEntries.map { it.mediaId }

        val airedResult = notificationRepository.getFirstEpisodeAirings(planningMediaIds)
        if (airedResult is DomainResult.Success) {
            for (airing in airedResult.data) {
                preferencesRepository.markPlanningMediaAsNotified(ctx.id, airing.mediaId)
            }
        }
        val upcomingResult = notificationRepository.getUpcomingFirstEpisodes(planningMediaIds, ADVANCE_NOTICE_HOURS)
        if (upcomingResult is DomainResult.Success) {
            for (airing in upcomingResult.data) {
                preferencesRepository.markUpcomingAiringNotified(ctx.id, airing.id)
            }
        }
    }

    private suspend fun checkWatchingListNotifications(ctx: AcctCtx, planningMediaIds: Set<Int>) {
        val allNewAiring = mutableListOf<AiringNotification>()
        val lastNotifiedId = preferencesRepository.getLastNotifiedId(ctx.id)
        var currentPage = 1
        var hasMore = true

        while (hasMore && currentPage <= MAX_NOTIFICATION_PAGES) {
            val repoResult = notificationRepository.getNotifications(currentPage, ctx.token)

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

                    val allAiringOnPage = notifications.filterIsInstance<AiringNotification>()
                    val hasOlderAiring = allAiringOnPage.any { it.id <= lastNotifiedId }

                    if (newOnThisPage.isEmpty() && hasOlderAiring) {
                        hasMore = false
                    } else if (newOnThisPage.isEmpty() && allAiringOnPage.isEmpty()) {
                        currentPage++
                        if (notifications.size < 20) hasMore = false
                    } else {
                        allNewAiring.addAll(newOnThisPage)
                        currentPage++
                        if (notifications.size < 20) hasMore = false
                    }
                }
                is DomainResult.Error -> {
                    Log.e(TAG, "Failed to fetch notifications page $currentPage for ${ctx.name}: ${repoResult.message}", repoResult.exception)
                    hasMore = false
                }
            }
        }

        if (allNewAiring.isNotEmpty()) {
            val sortedAiring = allNewAiring.sortedBy { it.id }

            // Apply the user-configured streaming delay (defer episodes not yet "due").
            val delaySeconds = notificationPreferences.streamingDelayMinutes.value * 60L
            val nowSeconds = System.currentTimeMillis() / 1000
            val cutoff = if (delaySeconds > 0L) {
                sortedAiring.firstOrNull { (nowSeconds - it.createdAt) < delaySeconds }?.id
                    ?: Int.MAX_VALUE
            } else {
                Int.MAX_VALUE
            }
            val toEmit = sortedAiring.filter { it.id < cutoff }

            for (notification in toEmit) {
                showNotification(notification, ctx)
            }
            if (toEmit.size >= 3) {
                showSummaryNotification(toEmit, ctx)
            }
            if (toEmit.isNotEmpty()) {
                preferencesRepository.setLastNotifiedId(ctx.id, toEmit.maxOf { it.id })
            }
        }
    }

    /**
     * Check for new social/forum notifications (thread replies, mentions, likes, subscriptions).
     * Each type is gated behind its own preference toggle.
     */
    private suspend fun checkSocialNotifications(ctx: AcctCtx) {
        val lastSocialId = preferencesRepository.getLastSocialNotifiedId(ctx.id)
        val allNewSocial = mutableListOf<Notification>()
        var currentPage = 1
        var hasMore = true

        while (hasMore && currentPage <= MAX_NOTIFICATION_PAGES) {
            val repoResult = notificationRepository.getNotifications(currentPage, ctx.token)

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
                    Log.e(TAG, "Failed to fetch social notifications page $currentPage for ${ctx.name}: ${repoResult.message}", repoResult.exception)
                    hasMore = false
                }
            }
        }

        if (allNewSocial.isNotEmpty()) {
            val sorted = allNewSocial.sortedBy { it.id }

            for (notification in sorted) {
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
                    showSocialNotification(notification, ctx)
                }
            }

            preferencesRepository.setLastSocialNotifiedId(ctx.id, sorted.maxOf { it.id })
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
    private suspend fun showSocialNotification(notification: Notification, ctx: AcctCtx) {
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
            else -> "anisync://notifications"
        }

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
            .setWhen(notification.createdAt.toLong() * 1000L)
            .setShowWhen(true)
            .setGroup(groupKey(GROUP_KEY_SOCIAL, ctx))
            .setContentIntent(deepLinkIntent(deepLinkUri, ctx, notificationId))

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        post(ctx, notificationId, builder)
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
     * Check for upcoming Episode 1 airings for Planning list items (active account only).
     * Two-tier: 12h advance, then 2h imminent.
     */
    private suspend fun checkUpcomingPlanningEpisodes(ctx: AcctCtx) {
        val planningEntries = libraryDao.getByType(ctx.id, MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }
        if (planningEntries.isEmpty()) return

        val mediaIds = planningEntries.map { it.mediaId }
        val currentTimeSeconds = System.currentTimeMillis() / 1000

        val result = notificationRepository.getUpcomingFirstEpisodes(mediaIds, ADVANCE_NOTICE_HOURS)
        if (result is DomainResult.Success) {
            val upcomingAirings = result.data
            val currentAiringIds = upcomingAirings.map { it.id }.toSet()
            preferencesRepository.cleanupOldUpcomingAirings(ctx.id, currentAiringIds)

            for (airing in upcomingAirings) {
                val hoursUntil = ((airing.airingAt - currentTimeSeconds) / 3600).toInt()
                when {
                    hoursUntil <= IMMINENT_NOTICE_HOURS -> {
                        val imminentKey = "imminent_${airing.id}"
                        if (!preferencesRepository.hasNotifiedWithKey(ctx.id, imminentKey)) {
                            showImminentEpisodeNotification(airing, hoursUntil, ctx)
                            preferencesRepository.markNotifiedWithKey(ctx.id, imminentKey)
                            preferencesRepository.markUpcomingAiringNotified(ctx.id, airing.id)
                        }
                    }
                    hoursUntil <= ADVANCE_NOTICE_HOURS -> {
                        val advanceKey = "advance_${airing.id}"
                        if (!preferencesRepository.hasNotifiedWithKey(ctx.id, advanceKey)) {
                            showAdvanceEpisodeNotification(airing, ctx)
                            preferencesRepository.markNotifiedWithKey(ctx.id, advanceKey)
                            preferencesRepository.markUpcomingAiringNotified(ctx.id, airing.id)
                        }
                    }
                }
            }
        } else if (result is DomainResult.Error) {
            Log.e(TAG, "Failed to fetch upcoming episodes: ${result.message}", result.exception)
        }
    }

    /**
     * Check for already-aired Episode 1 for Planning list items (active account only).
     */
    private suspend fun checkPlanningFirstEpisodes(ctx: AcctCtx) {
        val planningEntries = libraryDao.getByType(ctx.id, MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }
        if (planningEntries.isEmpty()) return

        val mediaIds = planningEntries.map { it.mediaId }
        val notifiedIds = preferencesRepository.getNotifiedPlanningMediaIds(ctx.id)
        val unnotifiedIds = mediaIds.filter { it !in notifiedIds }
        if (unnotifiedIds.isEmpty()) return

        val upcomingNotifiedAiringIds = preferencesRepository.getNotifiedUpcomingAiringIds(ctx.id)
        val result = notificationRepository.getFirstEpisodeAirings(unnotifiedIds)

        if (result is DomainResult.Success) {
            for (airing in result.data) {
                if (airing.id in upcomingNotifiedAiringIds) {
                    preferencesRepository.markPlanningMediaAsNotified(ctx.id, airing.mediaId)
                    continue
                }
                showPlanningFirstEpisodeNotification(airing, ctx)
                preferencesRepository.markPlanningMediaAsNotified(ctx.id, airing.mediaId)
            }
        } else if (result is DomainResult.Error) {
            Log.e(TAG, "Failed to fetch planning first episodes: ${result.message}", result.exception)
        }
    }

    private suspend fun showNotification(notification: AiringNotification, ctx: AcctCtx) {
        val notificationId = AIRING_NOTIFICATION_BASE_ID + notification.id
        val media = notification.media
        val title = media?.title ?: "New Episode"
        val content = "Episode ${notification.episode} has aired!"

        val largeIcon: Bitmap? = media?.coverUrl?.let { loadImage(it) }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.AIRING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            // Stamp with the airing moment (AniList createdAt) so it's consistent across devices
            // regardless of when each device's worker polled.
            .setWhen(notification.createdAt.toLong() * 1000L)
            .setShowWhen(true)
            .setGroup(groupKey(GROUP_KEY_AIRING, ctx))
            // Body tap opens the in-app inbox so the user lands on the row that fired it.
            .setContentIntent(deepLinkIntent("anisync://notifications", ctx, notificationId))

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        post(ctx, notificationId, builder)
    }

    private suspend fun showAdvanceEpisodeNotification(airing: AiringSchedule, ctx: AcctCtx) {
        val notificationId = UPCOMING_ADVANCE_NOTIFICATION_BASE_ID + airing.id
        val title = "📅 Premiere Alert: ${airing.mediaTitle}"

        val airingDate = java.util.Date(airing.airingAt * 1000)
        val timeFormat = DateFormat.getTimeFormat(applicationContext)
        val formattedTime = timeFormat.format(airingDate)

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

        val largeIcon: Bitmap? = airing.mediaCoverUrl?.let { loadImage(it) }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.UPCOMING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(groupKey(GROUP_KEY_PLANNING, ctx))
            .setContentIntent(deepLinkIntent("anisync://details/${airing.mediaId}", ctx, notificationId))

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        post(ctx, notificationId, builder)
        Log.d(TAG, "Sent advance notification for ${airing.mediaTitle}: $content")
    }

    private suspend fun showImminentEpisodeNotification(airing: AiringSchedule, hoursUntil: Int, ctx: AcctCtx) {
        val notificationId = UPCOMING_IMMINENT_NOTIFICATION_BASE_ID + airing.id
        val title = "🔔 Starting Soon: ${airing.mediaTitle}"

        val content = when {
            hoursUntil < 1 -> "Episode 1 is airing in less than an hour!"
            hoursUntil == 1 -> "Episode 1 is airing in about an hour!"
            else -> "Episode 1 is airing in $hoursUntil hours!"
        }

        val largeIcon: Bitmap? = airing.mediaCoverUrl?.let { loadImage(it) }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.UPCOMING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(groupKey(GROUP_KEY_PLANNING, ctx))
            .setContentIntent(deepLinkIntent("anisync://details/${airing.mediaId}", ctx, notificationId))

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        post(ctx, notificationId, builder)
        Log.d(TAG, "Sent imminent notification for ${airing.mediaTitle}: $content")
    }

    private suspend fun showPlanningFirstEpisodeNotification(airing: AiringSchedule, ctx: AcctCtx) {
        val notificationId = PLANNING_NOTIFICATION_BASE_ID + airing.mediaId
        val title = "${airing.mediaTitle} has started!"
        val content = "Episode 1 is now available"

        // "Add to Watching" action button
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

        val largeIcon: Bitmap? = airing.mediaCoverUrl?.let { loadImage(it) }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.PLANNING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setWhen(airing.airingAt * 1000L)
            .setShowWhen(true)
            .setGroup(groupKey(GROUP_KEY_PLANNING, ctx))
            .setContentIntent(deepLinkIntent("anisync://details/${airing.mediaId}", ctx, notificationId))
            .addAction(
                R.drawable.ic_notification,
                "Add to Watching",
                addToWatchingPendingIntent
            )

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        post(ctx, notificationId, builder)
    }

    private fun showSummaryNotification(notifications: List<AiringNotification>, ctx: AcctCtx) {
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${notifications.size} new episodes aired")
            .setSummaryText(if (ctx.showLabel) ctx.name else "AniSync")

        for (notification in notifications) {
            val title = notification.media?.title ?: "Anime"
            inboxStyle.addLine("Ep ${notification.episode}: $title")
        }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.AIRING_CHANNEL_ID)
            .setSmallIcon(getApplicationIcon())
            .setStyle(inboxStyle)
            .setGroup(groupKey(GROUP_KEY_AIRING, ctx))
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        post(ctx, SUMMARY_ID, builder)
    }

    // ── Multi-account notification helpers ──────────────────────────────────────────────

    /** Group key salted per account so each account's notifications group under their own summary. */
    private fun groupKey(base: String, ctx: AcctCtx) = "$base.${ctx.id}"

    /** Deep link tagged with the account so a tap can switch to it first (see MainActivity). */
    private fun deepLinkIntent(uri: String, ctx: AcctCtx, requestCode: Int): PendingIntent {
        val withAccount = if (uri.contains('?')) "$uri&account=${ctx.id}" else "$uri?account=${ctx.id}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(withAccount))
        return PendingIntent.getActivity(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Posts under a per-account tag so the same AniList notification id from two accounts can't
     * overwrite each other in the tray, and labels the account when more than one is signed in.
     */
    private fun post(ctx: AcctCtx, id: Int, builder: NotificationCompat.Builder) {
        if (ctx.showLabel) builder.setSubText(ctx.name)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify("acct_${ctx.id}", id, builder.build())
    }

    private suspend fun loadImage(url: String): Bitmap? {
        val request = ImageRequest.Builder(applicationContext)
            .data(url)
            .size(256, 256)
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
