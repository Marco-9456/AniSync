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
import com.anisync.android.domain.User
import com.anisync.android.domain.indefiniteNoun
import com.anisync.android.domain.noun
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
        private const val PAGE_SIZE = 20

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

        val watchingEnabled = notificationPreferences.watchingEnabled.value
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
                    // First time we see this account — establish baselines silently. Only mark the
                    // baseline done when it fully succeeded, otherwise a failed first fetch would
                    // flood the user with that account's entire history on the next run.
                    if (performBaselineSync(ctx, isActive)) {
                        preferencesRepository.markNotificationsHaveRun(ctx.id)
                        Log.d(TAG, "Baseline sync done for ${ctx.name}")
                    } else {
                        Log.w(TAG, "Baseline sync incomplete for ${ctx.name} — will retry next run")
                    }
                    continue
                }

                // AniList feed (airing + social) — both checks read the same feed, so fetch the
                // pages once per account and share the result.
                if (watchingEnabled || anySocialEnabled) {
                    val airingWatermark = preferencesRepository.getLastNotifiedId(ctx.id)
                    val socialWatermark = preferencesRepository.getLastSocialNotifiedId(ctx.id)
                    val stopBelowId = minOf(
                        if (watchingEnabled) airingWatermark else Int.MAX_VALUE,
                        if (anySocialEnabled) socialWatermark else Int.MAX_VALUE
                    )
                    val recent = fetchRecentNotifications(ctx, stopBelowId)

                    if (watchingEnabled) {
                        val planningMediaIds = if (isActive && notificationPreferences.planningEnabled.value) {
                            libraryDao.getByType(ctx.id, MediaType.ANIME)
                                .filter { it.status == LibraryStatus.PLANNING }
                                .map { it.mediaId }
                                .toSet()
                        } else {
                            emptySet()
                        }
                        notifyNewAiring(ctx, recent, airingWatermark, planningMediaIds)
                    }
                    if (anySocialEnabled) {
                        notifyNewSocial(ctx, recent, socialWatermark)
                    }
                }

                // Planning / upcoming "Episode 1" alerts depend on the locally-cached library,
                // so they only run for the active account. The "has started" check must run first:
                // it consumes the upcoming-notified markers that checkUpcomingPlanningEpisodes
                // prunes once an episode leaves the upcoming window.
                if (isActive) {
                    if (notificationPreferences.planningEnabled.value) checkPlanningFirstEpisodes(ctx)
                    if (notificationPreferences.upcomingEnabled.value) checkUpcomingPlanningEpisodes(ctx)
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
     * safeApiCall folds ApiErrors into Result.Error, so rate-limit/auth conditions never reach
     * doWork() as exceptions on their own — resurface the two it reacts to (retry / mark expired).
     */
    private fun DomainResult.Error.rethrowWorkerSignals() {
        when (val e = exception) {
            is ApiError.RateLimited, is ApiError.Unauthorized -> throw e
            else -> Unit
        }
    }

    /**
     * On an account's first run, fetch current state and set baselines WITHOUT notifying, so the
     * user isn't spammed with that account's historical notifications. Planning/upcoming baseline
     * runs only for the active account (it uses the locally-cached library).
     *
     * @return true when every baseline fetch succeeded.
     */
    private suspend fun performBaselineSync(ctx: AcctCtx, isActive: Boolean): Boolean {
        var complete = true

        when (val repoResult = notificationRepository.getNotifications(1, ctx.token)) {
            is DomainResult.Success -> {
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
            is DomainResult.Error -> {
                repoResult.rethrowWorkerSignals()
                complete = false
            }
        }

        if (!isActive) return complete

        val planningEntries = libraryDao.getByType(ctx.id, MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }
        val planningMediaIds = planningEntries.map { it.mediaId }

        when (val airedResult = notificationRepository.getFirstEpisodeAirings(planningMediaIds)) {
            is DomainResult.Success -> {
                for (airing in airedResult.data) {
                    preferencesRepository.markPlanningMediaAsNotified(ctx.id, airing.mediaId)
                }
            }
            is DomainResult.Error -> {
                airedResult.rethrowWorkerSignals()
                complete = false
            }
        }
        when (val upcomingResult = notificationRepository.getUpcomingFirstEpisodes(planningMediaIds, ADVANCE_NOTICE_HOURS)) {
            is DomainResult.Success -> {
                for (airing in upcomingResult.data) {
                    preferencesRepository.markUpcomingAiringNotified(ctx.id, airing.id)
                }
            }
            is DomainResult.Error -> {
                upcomingResult.rethrowWorkerSignals()
                complete = false
            }
        }
        return complete
    }

    /**
     * Pulls the newest notification pages once per account. Stops as soon as a page reaches ids
     * at/below [stopBelowId] (every consumer's watermark is covered), the feed runs short, or the
     * page cap is hit.
     */
    private suspend fun fetchRecentNotifications(ctx: AcctCtx, stopBelowId: Int): List<Notification> {
        val fetched = mutableListOf<Notification>()
        var page = 1
        while (page <= MAX_NOTIFICATION_PAGES) {
            when (val result = notificationRepository.getNotifications(page, ctx.token)) {
                is DomainResult.Success -> {
                    fetched += result.data
                    if (result.data.size < PAGE_SIZE || result.data.any { it.id <= stopBelowId }) {
                        return fetched
                    }
                }
                is DomainResult.Error -> {
                    result.rethrowWorkerSignals()
                    Log.e(TAG, "Failed to fetch notifications page $page for ${ctx.name}: ${result.message}", result.exception)
                    return fetched
                }
            }
            page++
        }
        return fetched
    }

    private suspend fun notifyNewAiring(
        ctx: AcctCtx,
        recent: List<Notification>,
        lastNotifiedId: Int,
        planningMediaIds: Set<Int>
    ) {
        val newAiring = recent
            .filterIsInstance<AiringNotification>()
            .filter { it.id > lastNotifiedId }
            // Skip Episode 1 for Planning items (handled by checkPlanningFirstEpisodes)
            .filterNot { it.episode == 1 && it.media?.id in planningMediaIds }
            .sortedBy { it.id }
        if (newAiring.isEmpty()) return

        // Apply the user-configured streaming delay (defer episodes not yet "due").
        val delaySeconds = notificationPreferences.streamingDelayMinutes.value * 60L
        val nowSeconds = System.currentTimeMillis() / 1000
        val cutoff = if (delaySeconds > 0L) {
            newAiring.firstOrNull { (nowSeconds - it.createdAt) < delaySeconds }?.id
                ?: Int.MAX_VALUE
        } else {
            Int.MAX_VALUE
        }
        val toEmit = newAiring.filter { it.id < cutoff }
        if (toEmit.isEmpty()) return

        for (notification in toEmit) {
            showAiringNotification(notification, ctx)
        }
        if (toEmit.size >= 3) {
            showSummaryNotification(toEmit, ctx)
        }
        preferencesRepository.setLastNotifiedId(ctx.id, toEmit.maxOf { it.id })
    }

    /**
     * Surface new social/forum notifications (thread replies, mentions, likes, subscriptions).
     * Each type is gated behind its own preference toggle; the watermark still advances past
     * disabled types so re-enabling them doesn't replay history.
     */
    private suspend fun notifyNewSocial(ctx: AcctCtx, recent: List<Notification>, lastSocialId: Int) {
        val newSocial = recent
            .filter { it.id > lastSocialId && isSocialNotification(it) }
            .sortedBy { it.id }
        if (newSocial.isEmpty()) return

        for (notification in newSocial) {
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

        preferencesRepository.setLastSocialNotifiedId(ctx.id, newSocial.maxOf { it.id })
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

    /** ` in "Thread title"` suffix, or nothing when the title is blank. */
    private fun inThread(title: String): String =
        title.takeIf { it.isNotBlank() }?.let { " in \"$it\"" }.orEmpty()

    /**
     * Display a social/forum notification. Copy follows the messaging convention:
     * title = who, text = what they did.
     */
    private suspend fun showSocialNotification(notification: Notification, ctx: AcctCtx) {
        val data = when (notification) {
            is ThreadCommentReplyNotification -> SocialNotificationData(
                user = notification.user,
                content = "Replied to your comment${inThread(notification.threadTitle)}",
                channelId = NotificationChannels.THREAD_COMMENT_REPLY_CHANNEL_ID,
                threadId = notification.threadId,
                commentId = notification.commentId
            )
            is ThreadCommentSubscribedNotification -> SocialNotificationData(
                user = notification.user,
                content = notification.threadTitle.takeIf { it.isNotBlank() }
                    ?.let { "Commented in \"$it\"" } ?: "Commented in a thread you're subscribed to",
                channelId = NotificationChannels.THREAD_SUBSCRIBED_CHANNEL_ID,
                threadId = notification.threadId,
                commentId = notification.commentId
            )
            is ThreadCommentMentionNotification -> SocialNotificationData(
                user = notification.user,
                content = "Mentioned you in a comment${inThread(notification.threadTitle)}",
                channelId = NotificationChannels.THREAD_COMMENT_MENTION_CHANNEL_ID,
                threadId = notification.threadId,
                commentId = notification.commentId
            )
            is ThreadLikeNotification -> SocialNotificationData(
                user = notification.user,
                content = "Liked your thread" +
                    notification.threadTitle.takeIf { it.isNotBlank() }?.let { " \"$it\"" }.orEmpty(),
                channelId = NotificationChannels.THREAD_LIKE_CHANNEL_ID,
                threadId = notification.threadId
            )
            is ThreadCommentLikeNotification -> SocialNotificationData(
                user = notification.user,
                content = "Liked your comment${inThread(notification.threadTitle)}",
                channelId = NotificationChannels.THREAD_COMMENT_LIKE_CHANNEL_ID,
                threadId = notification.threadId,
                commentId = notification.commentId
            )
            is ActivityReplyNotification -> SocialNotificationData(
                user = notification.user,
                content = "Replied to your ${notification.activity?.kind.noun()}",
                channelId = NotificationChannels.ACTIVITY_REPLY_CHANNEL_ID,
                activityId = notification.activityId
            )
            is ActivityReplySubscribedNotification -> SocialNotificationData(
                user = notification.user,
                content = "Replied to a post you're subscribed to",
                channelId = NotificationChannels.ACTIVITY_REPLY_CHANNEL_ID,
                activityId = notification.activityId
            )
            is ActivityMentionNotification -> SocialNotificationData(
                user = notification.user,
                content = "Mentioned you in ${notification.activity?.kind.indefiniteNoun()}",
                channelId = NotificationChannels.ACTIVITY_MENTION_CHANNEL_ID,
                activityId = notification.activityId
            )
            is ActivityLikeNotification -> SocialNotificationData(
                user = notification.user,
                content = "Liked your ${notification.activity?.kind.noun()}",
                channelId = NotificationChannels.ACTIVITY_LIKE_CHANNEL_ID,
                activityId = notification.activityId
            )
            is ActivityReplyLikeNotification -> SocialNotificationData(
                user = notification.user,
                content = "Liked your reply",
                channelId = NotificationChannels.ACTIVITY_LIKE_CHANNEL_ID,
                activityId = notification.activityId
            )
            is ActivityMessageNotification -> SocialNotificationData(
                user = notification.user,
                content = "Sent you a message",
                channelId = NotificationChannels.ACTIVITY_MESSAGE_CHANNEL_ID,
                activityId = notification.activityId
            )
            else -> return
        }

        val notificationId = SOCIAL_NOTIFICATION_BASE_ID + notification.id
        val deepLinkUri = when {
            data.activityId != null -> "anisync://activity/${data.activityId}"
            data.commentId != null -> "anisync://forum/thread/${data.threadId}?commentId=${data.commentId}"
            data.threadId != null -> "anisync://forum/thread/${data.threadId}"
            else -> "anisync://notifications"
        }

        val largeIcon: Bitmap? = data.user?.avatarUrl?.let { loadImage(it) }

        val builder = NotificationCompat.Builder(applicationContext, data.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(data.user?.name ?: "Someone")
            .setContentText(data.content)
            .setAutoCancel(true)
            .setWhen(notification.createdAt.toLong() * 1000L)
            .setShowWhen(true)
            .setGroup(groupKey(GROUP_KEY_SOCIAL, ctx))
            .setContentIntent(deepLinkIntent(deepLinkUri, ctx, notificationId))

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        post(ctx, notificationId, builder)
    }

    private data class SocialNotificationData(
        val user: User?,
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

        when (val result = notificationRepository.getUpcomingFirstEpisodes(mediaIds, ADVANCE_NOTICE_HOURS)) {
            is DomainResult.Success -> {
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
            }
            is DomainResult.Error -> {
                result.rethrowWorkerSignals()
                Log.e(TAG, "Failed to fetch upcoming episodes: ${result.message}", result.exception)
            }
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

        when (val result = notificationRepository.getFirstEpisodeAirings(unnotifiedIds)) {
            is DomainResult.Success -> {
                for (airing in result.data) {
                    if (airing.id in upcomingNotifiedAiringIds) {
                        // Already told the user this premiere was coming — don't repeat it.
                        preferencesRepository.markPlanningMediaAsNotified(ctx.id, airing.mediaId)
                        continue
                    }
                    showPlanningFirstEpisodeNotification(airing, ctx)
                    preferencesRepository.markPlanningMediaAsNotified(ctx.id, airing.mediaId)
                }
            }
            is DomainResult.Error -> {
                result.rethrowWorkerSignals()
                Log.e(TAG, "Failed to fetch planning first episodes: ${result.message}", result.exception)
            }
        }
    }

    private suspend fun showAiringNotification(notification: AiringNotification, ctx: AcctCtx) {
        val notificationId = AIRING_NOTIFICATION_BASE_ID + notification.id
        val media = notification.media
        val title = media?.title ?: "New episode"
        val content = "Episode ${notification.episode} has aired"

        val largeIcon: Bitmap? = media?.coverUrl?.let { loadImage(it) }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.AIRING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
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
        val title = airing.mediaTitle

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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setGroup(groupKey(GROUP_KEY_PLANNING, ctx))
            .setContentIntent(deepLinkIntent("anisync://details/${airing.mediaId}", ctx, notificationId))

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        post(ctx, notificationId, builder)
        Log.d(TAG, "Sent advance notification for ${airing.mediaTitle}: $content")
    }

    private suspend fun showImminentEpisodeNotification(airing: AiringSchedule, hoursUntil: Int, ctx: AcctCtx) {
        val notificationId = UPCOMING_IMMINENT_NOTIFICATION_BASE_ID + airing.id
        val title = airing.mediaTitle

        val content = when {
            hoursUntil < 1 -> "Episode 1 airs in less than an hour"
            hoursUntil == 1 -> "Episode 1 airs in about an hour"
            else -> "Episode 1 airs in about $hoursUntil hours"
        }

        val largeIcon: Bitmap? = airing.mediaCoverUrl?.let { loadImage(it) }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.UPCOMING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setGroup(groupKey(GROUP_KEY_PLANNING, ctx))
            .setContentIntent(deepLinkIntent("anisync://details/${airing.mediaId}", ctx, notificationId))

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        post(ctx, notificationId, builder)
        Log.d(TAG, "Sent imminent notification for ${airing.mediaTitle}: $content")
    }

    private suspend fun showPlanningFirstEpisodeNotification(airing: AiringSchedule, ctx: AcctCtx) {
        val notificationId = PLANNING_NOTIFICATION_BASE_ID + airing.mediaId
        val title = airing.mediaTitle
        val content = "Episode 1 is now available"

        // "Add to Watching" action button
        val addToWatchingIntent = Intent(applicationContext, AddToWatchingReceiver::class.java).apply {
            action = AddToWatchingReceiver.ACTION_ADD_TO_WATCHING
            putExtra(AddToWatchingReceiver.EXTRA_MEDIA_ID, airing.mediaId)
            putExtra(AddToWatchingReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AddToWatchingReceiver.EXTRA_NOTIFICATION_TAG, notificationTag(ctx))
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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
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
        val summaryTitle = "${notifications.size} new episodes aired"
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(summaryTitle)
            .setSummaryText(if (ctx.showLabel) ctx.name else "AniSync")

        for (notification in notifications) {
            val title = notification.media?.title ?: "Anime"
            inboxStyle.addLine("$title — Episode ${notification.episode}")
        }

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.AIRING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(summaryTitle)
            .setStyle(inboxStyle)
            .setGroup(groupKey(GROUP_KEY_AIRING, ctx))
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(deepLinkIntent("anisync://notifications", ctx, SUMMARY_ID))

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

    private fun notificationTag(ctx: AcctCtx) = "acct_${ctx.id}"

    /**
     * Posts under a per-account tag so the same AniList notification id from two accounts can't
     * overwrite each other in the tray, and labels the account when more than one is signed in.
     */
    private fun post(ctx: AcctCtx, id: Int, builder: NotificationCompat.Builder) {
        if (ctx.showLabel) builder.setSubText(ctx.name)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationTag(ctx), id, builder.build())
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
}
