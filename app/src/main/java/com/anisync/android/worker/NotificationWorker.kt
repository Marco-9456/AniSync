package com.anisync.android.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anisync.android.domain.AiringNotification
import com.anisync.android.domain.NotificationRepository
import com.anisync.android.domain.Result
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            when (val result = notificationRepository.getNotifications(1)) {
                is com.anisync.android.domain.Result.Success -> {
                    val notifications = result.data
                    
                    val prefs = applicationContext.getSharedPreferences("anisync_prefs", Context.MODE_PRIVATE)
                    val lastNotifiedId = prefs.getInt("last_notified_id", 0)
                    
                    val newAiring = notifications
                        .filterIsInstance<AiringNotification>()
                        .filter { it.id > lastNotifiedId }
                        .maxByOrNull { it.id }

                    if (newAiring != null) {
                        showNotification(newAiring)
                        prefs.edit().putInt("last_notified_id", newAiring.id).apply()
                    }

                    Result.success()
                }
                is com.anisync.android.domain.Result.Error -> {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(notification: AiringNotification) {
        val channelId = "airing_notifications"
        val notificationId = notification.id

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Airing Episodes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new airing episodes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = notification.media?.title ?: "New Episode"
        val content = "Episode ${notification.episode} has aired!"

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(getApplicationIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
    
    private fun getApplicationIcon(): Int {
        return android.R.drawable.stat_notify_sync 
    }
}
