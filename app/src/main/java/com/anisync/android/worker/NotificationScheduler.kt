package com.anisync.android.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val WORK_NAME = "notification_worker"
        private const val REPEAT_INTERVAL_MINUTES = 15L
    }

    private val workManager by lazy { WorkManager.getInstance(context) }

    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(REPEAT_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setInitialDelay(REPEAT_INTERVAL_MINUTES, TimeUnit.MINUTES)
            // The worker retries on AniList rate limits; wait out the limit window instead of
            // hammering again seconds later (default backoff starts at 30s).
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }
}
