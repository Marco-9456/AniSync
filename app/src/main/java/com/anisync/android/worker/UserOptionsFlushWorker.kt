package com.anisync.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anisync.android.domain.UserOptionsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background safety net for the local-first options sync: flushes any pending option edits to AniList.
 * Enqueued on every edit (unique, network-constrained) so changes still reach the server if the app
 * is killed before the in-process debounce fires. [UserOptionsRepository.flush] is guarded and a
 * no-op when nothing is pending, so this overlapping with the debounce flush is harmless.
 */
@HiltWorker
class UserOptionsFlushWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: UserOptionsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        repository.flush()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "UserOptionsFlush"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<UserOptionsFlushWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
