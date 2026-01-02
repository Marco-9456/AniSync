package com.anisync.android.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
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
import com.anisync.android.domain.AiringNotification
import com.anisync.android.domain.AiringSchedule
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationRepository
import com.anisync.android.domain.PreferencesRepository
import com.anisync.android.domain.Result as DomainResult
import com.anisync.android.type.MediaType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val libraryDao: LibraryDao,
    private val imageLoader: ImageLoader,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val MAX_NOTIFICATION_PAGES = 3
        private const val UPCOMING_HOURS = 24 // Notify 24 hours before airing

        private const val GROUP_KEY_AIRING = "com.anisync.android.AIRING_GROUP"
        private const val GROUP_KEY_PLANNING = "com.anisync.android.PLANNING_GROUP"
        private const val SUMMARY_ID = 0
        private const val AIRING_NOTIFICATION_BASE_ID = 1000
        private const val PLANNING_NOTIFICATION_BASE_ID = 100000
        private const val UPCOMING_NOTIFICATION_BASE_ID = 200000
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        // Skip if user is not authenticated
        if (authRepository.getToken() == null) {
            Log.d(TAG, "Skipping notification check - user not authenticated")
            return androidx.work.ListenableWorker.Result.success()
        }

        return try {
            // Channels are initialized in Application, but we can ensure they exist here too if needed.
            // For now, relying on Application init or assume they exist.
            
            // Check for Watching list airing notifications
            checkWatchingListNotifications()

            // Check for upcoming Episode 1 airings in Planning list (proactive)
            checkUpcomingPlanningEpisodes()

            // Check for already-aired Episode 1 in Planning list (reactive)
            checkPlanningFirstEpisodes()

            androidx.work.ListenableWorker.Result.success()
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

    private suspend fun checkWatchingListNotifications() {
        val allNewAiring = mutableListOf<AiringNotification>()
        val lastNotifiedId = preferencesRepository.getLastNotifiedId()
        var currentPage = 1
        var hasMore = true
        
        while (hasMore && currentPage <= MAX_NOTIFICATION_PAGES) {
            val repoResult = notificationRepository.getNotifications(currentPage)
            
            when (repoResult) {
                is DomainResult.Success -> {
                    val notifications = repoResult.data
                    
                    val newOnThisPage = notifications
                        .filterIsInstance<AiringNotification>()
                        .filter { it.id > lastNotifiedId }
                    
                    if (newOnThisPage.isEmpty()) {
                        hasMore = false
                    } else {
                        allNewAiring.addAll(newOnThisPage)
                        currentPage++
                        
                        if (notifications.size < 20) {
                            hasMore = false
                        }
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
            
            for (notification in sortedAiring) {
                showNotification(notification)
            }
            
            if (sortedAiring.size >= 3) {
                showSummaryNotification(sortedAiring)
            }
            
            val maxId = sortedAiring.maxOf { it.id }
            preferencesRepository.setLastNotifiedId(maxId)
            
            Log.d(TAG, "Processed ${sortedAiring.size} new airing notifications")
        }
    }

    /**
     * Check for upcoming Episode 1 airings (within next 24 hours) for Planning list items.
     * This is a proactive notification - "Episode 1 airs in X hours!"
     */
    private suspend fun checkUpcomingPlanningEpisodes() {
        val planningEntries = libraryDao.getByType(MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }

        if (planningEntries.isEmpty()) return

        val mediaIds = planningEntries.map { it.mediaId }
        
        // Get upcoming Episode 1 airings within next 24 hours
        val result = notificationRepository.getUpcomingFirstEpisodes(mediaIds, UPCOMING_HOURS)

        if (result is DomainResult.Success) {
            val upcomingAirings = result.data
            
            // Clean up old tracking - only keep IDs that are still upcoming
            val currentAiringIds = upcomingAirings.map { it.id }.toSet()
            preferencesRepository.cleanupOldUpcomingAirings(currentAiringIds)
            
            // Get already notified upcoming airings
            val notifiedIds = preferencesRepository.getNotifiedUpcomingAiringIds()
            
            // Filter to only unnotified
            val newUpcoming = upcomingAirings.filter { it.id !in notifiedIds }
            
            for (airing in newUpcoming) {
                showUpcomingEpisodeNotification(airing)
                preferencesRepository.markUpcomingAiringNotified(airing.id)
            }
            
            if (newUpcoming.isNotEmpty()) {
                Log.d(TAG, "Processed ${newUpcoming.size} upcoming episode notifications")
            }
        } else if (result is DomainResult.Error) {
            Log.e(TAG, "Failed to fetch upcoming episodes: ${result.message}", result.exception)
        }
    }

    /**
     * Check for already-aired Episode 1 for Planning list items.
     * This is a reactive notification - "Episode 1 is now available!"
     */
    private suspend fun checkPlanningFirstEpisodes() {
        val planningEntries = libraryDao.getByType(MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }

        val mediaIds = planningEntries.map { it.mediaId }
        
        preferencesRepository.cleanupOrphanedPlanningIds(mediaIds.toSet())
        
        if (planningEntries.isEmpty()) return

        val notifiedIds = preferencesRepository.getNotifiedPlanningMediaIds()
        val unnotifiedIds = mediaIds.filter { it !in notifiedIds }
        if (unnotifiedIds.isEmpty()) return

        val result = notificationRepository.getFirstEpisodeAirings(unnotifiedIds)

        if (result is DomainResult.Success) {
            for (airing in result.data) {
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

    private suspend fun showUpcomingEpisodeNotification(airing: AiringSchedule) {
        val notificationId = UPCOMING_NOTIFICATION_BASE_ID + airing.id
        val title = "Premiere Alert: ${airing.mediaTitle}"
        
        // Calculate hours until airing
        val currentTime = System.currentTimeMillis() / 1000
        val hoursUntil = ((airing.airingAt - currentTime) / 3600).toInt()
        
        val content = when {
            hoursUntil <= 1 -> "Episode 1 airs in less than an hour!"
            hoursUntil < 12 -> "Episode 1 airs in about $hoursUntil hours!"
            hoursUntil < 24 -> "Episode 1 airs tomorrow!" // Simplified usage of 'tomorrow' for user friendlyness
            else -> "Episode 1 airs soon!"
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_PLANNING)
            .setContentIntent(pendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
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
