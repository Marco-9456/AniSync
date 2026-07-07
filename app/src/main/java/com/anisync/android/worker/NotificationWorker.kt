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
import com.anisync.android.domain.FollowingNotification
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
        private const val GROUP_KEY_SOCIAL = "com.anisync.android.SOCIAL_GROUP"

        // Tray slot = (tag "acct_<accountId>_<category>", id stable per target). A newer event for
        // the same target replaces its stale tray entry instead of piling up next to it.
        private const val CATEGORY_AIRING = "airing"
        private const val CATEGORY_PLANNING = "planning"
        private const val CATEGORY_UPCOMING = "upcoming"
        private const val CATEGORY_SOCIAL_SUMMARY = "social"
        private const val SUMMARY_ID = 0
    }

    /** The tray slot a social notification lands in; events sharing a slot collapse to one entry. */
    private data class SocialSlot(val category: String, val id: Int)

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
            notificationPreferences.activityMessageEnabled.value ||
            notificationPreferences.followsEnabled.value

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
     * Surface new social/forum notifications (thread replies, mentions, likes, subscriptions,
     * messages, follows). Each type is gated behind its own preference toggle; the watermark still
     * advances past disabled types so re-enabling them doesn't replay history. Events aimed at the
     * same target collapse into one tray entry per [SocialSlot].
     */
    private suspend fun notifyNewSocial(ctx: AcctCtx, recent: List<Notification>, lastSocialId: Int) {
        val newSocial = recent
            .filter { it.id > lastSocialId && isSocialNotification(it) }
            .sortedBy { it.id }
        if (newSocial.isEmpty()) return

        val enabled = newSocial.filter { isTypeEnabled(it) }
        val bySlot = enabled.groupBy { socialSlot(it) }
        for ((slot, members) in bySlot) {
            if (slot != null) showSocialNotification(slot, members, ctx)
        }
        if (bySlot.isNotEmpty()) maybePostSocialSummary(ctx)

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
            notification is ActivityMessageNotification ||
            notification is FollowingNotification
    }

    private fun isTypeEnabled(notification: Notification): Boolean = when (notification) {
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
        is FollowingNotification -> notificationPreferences.followsEnabled.value
        else -> false
    }

    private fun socialSlot(notification: Notification): SocialSlot? = when (notification) {
        is ThreadCommentReplyNotification -> SocialSlot("thread_reply", notification.threadId)
        is ThreadCommentSubscribedNotification -> SocialSlot("thread_sub", notification.threadId)
        is ThreadCommentMentionNotification -> SocialSlot("thread_mention", notification.threadId)
        is ThreadLikeNotification -> SocialSlot("thread_like", notification.threadId)
        is ThreadCommentLikeNotification -> SocialSlot("comment_like", notification.commentId ?: notification.threadId)
        is ActivityReplyNotification -> SocialSlot("act_reply", notification.activityId ?: notification.id)
        is ActivityReplySubscribedNotification -> SocialSlot("act_reply", notification.activityId ?: notification.id)
        is ActivityMentionNotification -> SocialSlot("act_mention", notification.activityId ?: notification.id)
        is ActivityLikeNotification -> SocialSlot("act_like", notification.activityId ?: notification.id)
        is ActivityReplyLikeNotification -> SocialSlot("reply_like", notification.activityId ?: notification.id)
        is ActivityMessageNotification -> SocialSlot("message", notification.user?.id ?: notification.id)
        is FollowingNotification -> SocialSlot("follow", notification.user?.id ?: notification.id)
        else -> null
    }

    private fun socialActor(notification: Notification): User? = when (notification) {
        is ThreadCommentReplyNotification -> notification.user
        is ThreadCommentSubscribedNotification -> notification.user
        is ThreadCommentMentionNotification -> notification.user
        is ThreadLikeNotification -> notification.user
        is ThreadCommentLikeNotification -> notification.user
        is ActivityReplyNotification -> notification.user
        is ActivityReplySubscribedNotification -> notification.user
        is ActivityMentionNotification -> notification.user
        is ActivityLikeNotification -> notification.user
        is ActivityReplyLikeNotification -> notification.user
        is ActivityMessageNotification -> notification.user
        is FollowingNotification -> notification.user
        else -> null
    }

    /** "Hameru", "Hameru and Bob", "Hameru and 2 others" — newest actor first. */
    private fun actorsLabel(actors: List<User>): String = when (actors.size) {
        0 -> "Someone"
        1 -> actors[0].name
        2 -> "${actors[0].name} and ${actors[1].name}"
        else -> "${actors[0].name} and ${actors.size - 1} others"
    }

    /** ` in "Thread title"` suffix, or nothing when the title is blank. */
    private fun inThread(title: String): String =
        title.takeIf { it.isNotBlank() }?.let { " in \"$it\"" }.orEmpty()

    /**
     * Display one tray entry for all [members] that landed in the same [slot]. Copy follows the
     * messaging convention: title = who, text = what they did; multiple events for the same target
     * combine actors and switch to a counted phrase.
     */
    private suspend fun showSocialNotification(slot: SocialSlot, members: List<Notification>, ctx: AcctCtx) {
        val rep = members.last()
        val count = members.size
        val actors = members.asReversed().mapNotNull(::socialActor).distinctBy { it.id }

        val data = when (rep) {
            is ThreadCommentReplyNotification -> SocialNotificationData(
                content = if (count > 1) "$count new replies${inThread(rep.threadTitle)}"
                else "Replied to your comment${inThread(rep.threadTitle)}",
                channelId = NotificationChannels.THREAD_COMMENT_REPLY_CHANNEL_ID,
                threadId = rep.threadId,
                commentId = rep.commentId
            )
            is ThreadCommentSubscribedNotification -> SocialNotificationData(
                content = when {
                    count > 1 -> "$count new comments${inThread(rep.threadTitle)}"
                    rep.threadTitle.isNotBlank() -> "Commented in \"${rep.threadTitle}\""
                    else -> "Commented in a thread you're subscribed to"
                },
                channelId = NotificationChannels.THREAD_SUBSCRIBED_CHANNEL_ID,
                threadId = rep.threadId,
                commentId = rep.commentId
            )
            is ThreadCommentMentionNotification -> SocialNotificationData(
                content = "Mentioned you in a comment${inThread(rep.threadTitle)}",
                channelId = NotificationChannels.THREAD_COMMENT_MENTION_CHANNEL_ID,
                threadId = rep.threadId,
                commentId = rep.commentId
            )
            is ThreadLikeNotification -> SocialNotificationData(
                content = "Liked your thread" +
                    rep.threadTitle.takeIf { it.isNotBlank() }?.let { " \"$it\"" }.orEmpty(),
                channelId = NotificationChannels.THREAD_LIKE_CHANNEL_ID,
                threadId = rep.threadId
            )
            is ThreadCommentLikeNotification -> SocialNotificationData(
                content = "Liked your comment${inThread(rep.threadTitle)}",
                channelId = NotificationChannels.THREAD_COMMENT_LIKE_CHANNEL_ID,
                threadId = rep.threadId,
                commentId = rep.commentId
            )
            is ActivityReplyNotification -> SocialNotificationData(
                content = if (count > 1) "$count new replies to your ${rep.activity?.kind.noun()}"
                else "Replied to your ${rep.activity?.kind.noun()}",
                channelId = NotificationChannels.ACTIVITY_REPLY_CHANNEL_ID,
                activityId = rep.activityId
            )
            is ActivityReplySubscribedNotification -> SocialNotificationData(
                content = if (count > 1) "$count new replies to a post you're subscribed to"
                else "Replied to a post you're subscribed to",
                channelId = NotificationChannels.ACTIVITY_REPLY_CHANNEL_ID,
                activityId = rep.activityId
            )
            is ActivityMentionNotification -> SocialNotificationData(
                content = "Mentioned you in ${rep.activity?.kind.indefiniteNoun()}",
                channelId = NotificationChannels.ACTIVITY_MENTION_CHANNEL_ID,
                activityId = rep.activityId
            )
            is ActivityLikeNotification -> SocialNotificationData(
                content = "Liked your ${rep.activity?.kind.noun()}",
                channelId = NotificationChannels.ACTIVITY_LIKE_CHANNEL_ID,
                activityId = rep.activityId
            )
            is ActivityReplyLikeNotification -> SocialNotificationData(
                content = "Liked your reply",
                channelId = NotificationChannels.ACTIVITY_LIKE_CHANNEL_ID,
                activityId = rep.activityId
            )
            is ActivityMessageNotification -> SocialNotificationData(
                content = if (count > 1) "$count new messages" else "Sent you a message",
                channelId = NotificationChannels.ACTIVITY_MESSAGE_CHANNEL_ID,
                activityId = rep.activityId
            )
            is FollowingNotification -> SocialNotificationData(
                content = "Started following you",
                channelId = NotificationChannels.FOLLOW_CHANNEL_ID,
                userName = rep.user?.name
            )
            else -> return
        }

        val deepLinkUri = when {
            data.activityId != null -> "anisync://activity/${data.activityId}"
            data.commentId != null -> "anisync://forum/thread/${data.threadId}?commentId=${data.commentId}"
            data.threadId != null -> "anisync://forum/thread/${data.threadId}"
            data.userName != null -> "anisync://user/${Uri.encode(data.userName)}"
            else -> "anisync://notifications"
        }

        val largeIcon: Bitmap? = actors.firstOrNull()?.avatarUrl?.let { loadImage(it) }

        val builder = NotificationCompat.Builder(applicationContext, data.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(actorsLabel(actors))
            .setContentText(data.content)
            .setAutoCancel(true)
            .setWhen(rep.createdAt.toLong() * 1000L)
            .setShowWhen(true)
            .setGroup(groupKey(GROUP_KEY_SOCIAL, ctx))
            .setContentIntent(deepLinkIntent(deepLinkUri, ctx, slot.id))

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        post(ctx, slot.category, slot.id, builder)
    }

    private data class SocialNotificationData(
        val content: String,
        val channelId: String,
        val threadId: Int? = null,
        val commentId: Int? = null,
        val activityId: Int? = null,
        val userName: String? = null
    )

    /**
     * Group summary so social notifications bundle in the tray. Only needed once two or more are
     * actually showing; the system expands/collapses the stack and drops the summary with the
     * last child.
     */
    private fun maybePostSocialSummary(ctx: AcctCtx) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val group = groupKey(GROUP_KEY_SOCIAL, ctx)
        val activeInGroup = nm.activeNotifications.count { sbn ->
            sbn.notification.group == group &&
                (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) == 0
        }
        if (activeInGroup < 2) return

        val builder = NotificationCompat.Builder(applicationContext, NotificationChannels.ACTIVITY_REPLY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New activity")
            .setContentText("$activeInGroup notifications")
            .setGroup(group)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(deepLinkIntent("anisync://notifications", ctx, SUMMARY_ID))

        post(ctx, CATEGORY_SOCIAL_SUMMARY, SUMMARY_ID, builder)
    }

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
        val notificationId = notification.id
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

        post(ctx, CATEGORY_AIRING, notificationId, builder)
    }

    private suspend fun showAdvanceEpisodeNotification(airing: AiringSchedule, ctx: AcctCtx) {
        // Same slot as the imminent tier: "starting soon" replaces "airs tomorrow" in the tray.
        val notificationId = airing.id
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

        post(ctx, CATEGORY_UPCOMING, notificationId, builder)
        Log.d(TAG, "Sent advance notification for ${airing.mediaTitle}: $content")
    }

    private suspend fun showImminentEpisodeNotification(airing: AiringSchedule, hoursUntil: Int, ctx: AcctCtx) {
        val notificationId = airing.id
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

        post(ctx, CATEGORY_UPCOMING, notificationId, builder)
        Log.d(TAG, "Sent imminent notification for ${airing.mediaTitle}: $content")
    }

    private suspend fun showPlanningFirstEpisodeNotification(airing: AiringSchedule, ctx: AcctCtx) {
        val notificationId = airing.mediaId
        val title = airing.mediaTitle
        val content = "Episode 1 is now available"

        // "Add to Watching" action button
        val addToWatchingIntent = Intent(applicationContext, AddToWatchingReceiver::class.java).apply {
            action = AddToWatchingReceiver.ACTION_ADD_TO_WATCHING
            putExtra(AddToWatchingReceiver.EXTRA_MEDIA_ID, airing.mediaId)
            putExtra(AddToWatchingReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AddToWatchingReceiver.EXTRA_NOTIFICATION_TAG, notificationTag(ctx, CATEGORY_PLANNING))
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

        post(ctx, CATEGORY_PLANNING, notificationId, builder)
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

        post(ctx, CATEGORY_AIRING, SUMMARY_ID, builder)
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

    private fun notificationTag(ctx: AcctCtx, category: String) = "acct_${ctx.id}_$category"

    /**
     * Posts under a per-account, per-category tag: two accounts can't overwrite each other, and
     * within an account the id only has to be unique per category (it's the target's own id, so
     * a newer event for the same target replaces the stale entry). Labels the account when more
     * than one is signed in.
     */
    private fun post(ctx: AcctCtx, category: String, id: Int, builder: NotificationCompat.Builder) {
        if (ctx.showLabel) builder.setSubText(ctx.name)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationTag(ctx, category), id, builder.build())
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
