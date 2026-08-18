package com.anisync.android.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anisync.android.cache.Cache
import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.api.SchemaCoordinatesMaxAgeProvider
import com.apollographql.cache.normalized.apolloStore
import com.apollographql.cache.normalized.garbageCollect
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

/**
 * Keeps the normalized cache from growing without end.
 *
 * The cache has no automatic eviction. Left alone it only ever grows, which is how it reached
 * 5.5 MB of records that nothing read. Two passes bound it:
 *
 * - **Garbage collection** drops fields older than their max age, then the references and records
 *   left unreachable behind them. This is the pass that reflects actual staleness.
 * - **Trim** is the backstop. If collection cannot keep up, the oldest records are removed until
 *   the cache is back under [MAX_CACHE_BYTES], so the ceiling holds regardless.
 *
 * Room owns everything the app needs offline, so nothing here costs the user data, only a refetch.
 */
@HiltWorker
class CacheMaintenanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apolloClient: ApolloClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val store = apolloClient.apolloStore
            val collected = store.garbageCollect(
                SchemaCoordinatesMaxAgeProvider(Cache.maxAges, defaultMaxAge = DEFAULT_MAX_AGE)
            )
            val remaining = store.trim(maxSizeBytes = MAX_CACHE_BYTES, trimFactor = TRIM_FACTOR)
            Log.d(TAG, "Garbage collected $collected, cache now $remaining bytes")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cache maintenance failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CacheMaintenance"
        private const val WORK_NAME = "CacheMaintenanceWorker"

        /**
         * Ceiling for the normalized cache. Generous next to the roughly 800 KB a heavy session
         * produces, and small next to the image cache, which is the footprint users actually see.
         */
        private const val MAX_CACHE_BYTES = 16L * 1024 * 1024

        /** How much to drop once over the ceiling, so trimming is not a per-run cliff edge. */
        private const val TRIM_FACTOR = 0.1f

        /**
         * Applies to anything the schema does not give its own age. A day keeps a session warm
         * while making sure nothing lingers for weeks unread.
         */
        private val DEFAULT_MAX_AGE = 1.days

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CacheMaintenanceWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder().setRequiresBatteryNotLow(true).build()
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
