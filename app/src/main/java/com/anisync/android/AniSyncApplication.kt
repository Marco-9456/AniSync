package com.anisync.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AniSyncApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels on app startup
        com.anisync.android.worker.NotificationChannels.createChannels(this)

        // Schedule Workers
        scheduleAiringUpdates()
    }

    private fun scheduleAiringUpdates() {
        val request = androidx.work.PeriodicWorkRequestBuilder<com.anisync.android.worker.AiringScheduleWorker>(
            1, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AiringScheduleWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP, // Keep existing if running
            request
        )
        
        // Also run once immediately for dev/debugging
        val oneTime = androidx.work.OneTimeWorkRequestBuilder<com.anisync.android.worker.AiringScheduleWorker>()
             .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
        androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
            "AiringScheduleWorker_OneTime",
             androidx.work.ExistingWorkPolicy.REPLACE,
             oneTime
        )

        // Schedule Trending Worker (Every 12 hours)
        val trendingRequest = androidx.work.PeriodicWorkRequestBuilder<com.anisync.android.worker.TrendingWorker>(
            12, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrendingWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            trendingRequest
        )
        
        // One time immediate for dev
        val trendingOneTime = androidx.work.OneTimeWorkRequestBuilder<com.anisync.android.worker.TrendingWorker>()
             .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
             
        androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
            "TrendingWorker_OneTime",
             androidx.work.ExistingWorkPolicy.REPLACE,
             trendingOneTime
        )

        // Schedule Widget Refresh Worker (Every 15 minutes for countdown updates)
        com.anisync.android.worker.WidgetRefreshWorker.schedule(this)
    }
}
