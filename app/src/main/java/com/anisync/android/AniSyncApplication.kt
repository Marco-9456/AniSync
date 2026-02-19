package com.anisync.android

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltAndroidApp
class AniSyncApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        val mainThreadTime = measureTimeMillis {
            com.anisync.android.worker.NotificationChannels.createChannels(this)
        }
        Log.d("PerfMetrics", "Main thread AppInit took $mainThreadTime ms")

        applicationScope.launch {
            val backgroundInitTime = measureTimeMillis {
                scheduleWorkersBackground()
            }
            Log.d("PerfMetrics", "Background Worker scheduling took $backgroundInitTime ms")
        }
    }

    private fun scheduleWorkersBackground() {
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workManager = WorkManager.getInstance(this@AniSyncApplication)

        // Schedule Airing Updates
        val airingRequest =
            PeriodicWorkRequestBuilder<com.anisync.android.worker.AiringScheduleWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(networkConstraints)
                .build()

        workManager.enqueueUniquePeriodicWork(
            "AiringScheduleWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            airingRequest
        )

        // Schedule Trending Worker
        val trendingRequest = PeriodicWorkRequestBuilder<com.anisync.android.worker.TrendingWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "TrendingWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            trendingRequest
        )

        // Schedule Widget Refresh
        com.anisync.android.worker.WidgetRefreshWorker.schedule(this@AniSyncApplication)
    }
}