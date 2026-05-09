package com.anisync.android.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anisync.android.MainActivity
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import com.anisync.android.data.update.UpdateCheckResult
import com.anisync.android.data.update.UpdateManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic WorkManager worker that checks for app updates in the background.
 *
 * When a new version is found, it posts a system notification.
 * Tapping the notification opens [MainActivity], which will then perform
 * its own launch check and display the in-app update dialog.
 *
 * This worker only runs when auto-update is enabled (checked at execution time).
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateManager: UpdateManager,
    private val appSettings: AppSettings
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "UpdateCheckWorker"
        private const val NOTIFICATION_ID = 9001
    }

    override suspend fun doWork(): Result {
        // Respect the user's preference — if auto-update is disabled, skip silently.
        if (!appSettings.autoUpdateEnabled.value) {
            Log.d(TAG, "Auto-update disabled, skipping check")
            return Result.success()
        }

        val allowPrerelease = appSettings.allowPrerelease.value

        return when (val result = updateManager.checkForUpdate(allowPrerelease)) {
            is UpdateCheckResult.Available -> {
                Log.i(TAG, "Update available: ${result.release.tagName}")
                showUpdateNotification(result.release.tagName)
                Result.success()
            }
            is UpdateCheckResult.UpToDate -> {
                Log.d(TAG, "App is up to date")
                Result.success()
            }
            is UpdateCheckResult.Error -> {
                Log.e(TAG, "Update check failed", result.exception)
                Result.retry()
            }
        }
    }

    private fun showUpdateNotification(tagName: String) {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, NotificationChannels.UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(appContext.getString(R.string.update_notification_title, appContext.getString(R.string.app_name)))
            .setContentText(appContext.getString(R.string.update_notification_body, tagName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
