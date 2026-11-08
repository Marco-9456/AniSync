package com.anisync.android.worker

import android.content.Context
import android.util.Log
import androidx.work.ExecutionEventListener
import androidx.work.ListenableWorker
import androidx.work.ScheduleEventListener
import androidx.work.WorkInfo
import androidx.work.analytics.ExperimentalWorkMetricsApi
import androidx.work.analytics.WorkMetricsInfo
import androidx.work.analytics.WorkMetricsInfoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Debug builds only. Records every worker run through WorkManager's metrics listeners and logs one
 * `AniSyncWork` line per finished run, so the background request budget can be read next to the
 * `AniSyncNet event=admit priority=Background` lines without attaching a profiler.
 */
@OptIn(ExperimentalWorkMetricsApi::class)
internal object WorkMetricsLog {

    private const val TAG = "WorkMetrics"

    /** The listener to give WorkManager, which also starts the log. */
    fun start(context: Context, scope: CoroutineScope): Listener {
        val repository = WorkMetricsInfoRepository(context)
        scope.launch {
            repository.finishedWorkMetricsInfoFlow.collect { Log.d(TAG, it.toLogLine()) }
        }
        return Listener(repository)
    }

    /**
     * The metrics repository behind a guard. Its database can disagree with WorkManager's after a
     * run was cut off, and its insert then threw on every launch until the database was deleted, so
     * a failure here is logged and dropped. It only ever cost a log line.
     */
    class Listener internal constructor(private val repository: WorkMetricsInfoRepository) :
        ScheduleEventListener, ExecutionEventListener {
        override suspend fun onEnqueued(workInfo: WorkInfo) = guard { repository.onEnqueued(workInfo) }
        override suspend fun onUpdated(oldWorkInfo: WorkInfo, newWorkInfo: WorkInfo) =
            guard { repository.onUpdated(oldWorkInfo, newWorkInfo) }
        override suspend fun onUnblocked(workInfo: WorkInfo) = guard { repository.onUnblocked(workInfo) }
        override suspend fun onCancelled(workInfo: WorkInfo) = guard { repository.onCancelled(workInfo) }
        override suspend fun onPrerequisiteFailed(workInfo: WorkInfo) = guard { repository.onPrerequisiteFailed(workInfo) }
        override suspend fun onStarted(workInfo: WorkInfo) = guard { repository.onStarted(workInfo) }
        override suspend fun onStopped(stopReason: Int, workInfo: WorkInfo) = guard { repository.onStopped(stopReason, workInfo) }
        override suspend fun onFinished(result: ListenableWorker.Result, workInfo: WorkInfo) =
            guard { repository.onFinished(result, workInfo) }
        override suspend fun onException(throwable: Throwable, workInfo: WorkInfo) =
            guard { repository.onException(throwable, workInfo) }

        private inline fun guard(block: () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Work metrics dropped", e)
            }
        }
    }

    private fun WorkMetricsInfo.toLogLine(): String {
        val stops = stopReasonCounts.entries.joinToString(",") { "${it.key}:${it.value}" }
        val waitMs = if (firstStartTimeMillis > 0) firstStartTimeMillis - enqueueTimeMillis else -1
        return "AniSyncWork worker=${workerClassName?.substringAfterLast('.')} state=$state " +
            "run_ms=$workerDurationMillis total_ms=$totalRuntimeMillis wait_ms=$waitMs " +
            "attempts=$runAttemptCount retries=$explicitRetryCount stops=${stops.ifEmpty { "none" }}"
    }
}
