package com.anisync.android.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
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

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val libraryDao: LibraryDao,
    private val imageLoader: ImageLoader
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "airing_notifications"
        private const val PLANNING_CHANNEL_ID = "planning_notifications"
        private const val GROUP_KEY_AIRING = "com.anisync.android.AIRING_GROUP"
        private const val GROUP_KEY_PLANNING = "com.anisync.android.PLANNING_GROUP"
        private const val SUMMARY_ID = 0
        private const val PLANNING_NOTIFICATION_BASE_ID = 100000
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            // Create notification channels
            createNotificationChannel()
            createPlanningNotificationChannel()

            // Check for Watching list airing notifications
            checkWatchingListNotifications()

            // Check for Planning list first episode airings
            checkPlanningFirstEpisodes()

            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    private suspend fun checkWatchingListNotifications() {
        val repoResult = notificationRepository.getNotifications(1)

        when (repoResult) {
            is DomainResult.Success -> {
                val notifications = repoResult.data

                val lastNotifiedId = preferencesRepository.getLastNotifiedId()

                val newAiring: List<AiringNotification> = notifications
                    .filterIsInstance<AiringNotification>()
                    .filter { it.id > lastNotifiedId }
                    .sortedBy { it.id }

                if (newAiring.isNotEmpty()) {
                    for (notification in newAiring) {
                        showNotification(notification)
                    }

                    if (newAiring.size > 1) {
                        showSummaryNotification(newAiring)
                    }

                    val maxId = newAiring.maxOf { it.id }
                    preferencesRepository.setLastNotifiedId(maxId)
                }
            }
            is DomainResult.Error -> {
                // Log error but don't fail the entire worker
            }
        }
    }

    private suspend fun checkPlanningFirstEpisodes() {
        // 1. Get Planning list media IDs from local DB
        val planningEntries = libraryDao.getByType(MediaType.ANIME)
            .filter { it.status == LibraryStatus.PLANNING }

        if (planningEntries.isEmpty()) return

        val mediaIds = planningEntries.map { it.mediaId }

        // 2. Get already notified IDs
        val notifiedIds = preferencesRepository.getNotifiedPlanningMediaIds()

        // 3. Filter to only unnotified
        val unnotifiedIds = mediaIds.filter { it !in notifiedIds }
        if (unnotifiedIds.isEmpty()) return

        // 4. Query API for first episode airings
        val result = notificationRepository.getFirstEpisodeAirings(unnotifiedIds)

        if (result is DomainResult.Success) {
            for (airing in result.data) {
                showPlanningFirstEpisodeNotification(airing)
                preferencesRepository.markPlanningMediaAsNotified(airing.mediaId)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Airing Episodes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new airing episodes"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPlanningNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                PLANNING_CHANNEL_ID,
                "Planning List Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when shows in your Planning list start airing"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun showNotification(notification: AiringNotification) {
        val notificationId = notification.id
        val media = notification.media
        val title = media?.title ?: "New Episode"
        val content = "Episode ${notification.episode} has aired!"
        
        // Deep link intent
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://details/${media?.id ?: 0}"))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Load Large Icon (Cover Image)
        val largeIcon: Bitmap? = media?.coverUrl?.let { url ->
            loadImage(url)
        }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
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

    private suspend fun showPlanningFirstEpisodeNotification(airing: AiringSchedule) {
        val notificationId = PLANNING_NOTIFICATION_BASE_ID + airing.mediaId
        val title = "${airing.mediaTitle} has started!"
        val content = "Episode 1 is now available"

        // Deep link intent
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://details/${airing.mediaId}"))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Load Large Icon (Cover Image)
        val largeIcon: Bitmap? = airing.mediaCoverUrl?.let { url ->
            loadImage(url)
        }

        val builder = NotificationCompat.Builder(applicationContext, PLANNING_CHANNEL_ID)
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

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
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
        return android.R.drawable.stat_notify_sync 
    }
}
