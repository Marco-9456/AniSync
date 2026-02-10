package com.anisync.android.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anisync.android.widget.AiringTodayWidget
import com.anisync.android.widget.UpNextWidget
import com.anisync.android.widget.WeeklyCalendarWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic worker to refresh time-sensitive widgets (countdowns).
 * Scheduled to run every 15 minutes (WorkManager minimum).
 * 
 * For widgets showing countdown timers, this ensures the displayed
 * time is relatively accurate even though real-time updates aren't possible.
 */
@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Update all time-sensitive widgets
            UpNextWidget().updateAll(appContext)
            AiringTodayWidget().updateAll(appContext)
            WeeklyCalendarWidget().updateAll(appContext)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "widget_refresh_work"

        /**
         * Schedule periodic widget refresh.
         * Uses 15-minute interval (WorkManager minimum for periodic work).
         */
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Cancel scheduled widget refresh.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
