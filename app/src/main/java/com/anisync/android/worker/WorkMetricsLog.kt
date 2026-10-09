package com.anisync.android.worker

import android.content.Context
import android.util.Log
import androidx.work.analytics.ExperimentalWorkMetricsApi
import androidx.work.analytics.WorkMetricsInfo
import androidx.work.analytics.WorkMetricsInfoRepository
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

    fun start(context: Context, scope: CoroutineScope): WorkMetricsInfoRepository {
        val repository = WorkMetricsInfoRepository(context)
        scope.launch {
            repository.finishedWorkMetricsInfoFlow.collect { Log.d(TAG, it.toLogLine()) }
        }
        return repository
    }

    private fun WorkMetricsInfo.toLogLine(): String {
        val stops = stopReasonCounts.entries.joinToString(",") { "${it.key}:${it.value}" }
        val waitMs = if (firstStartTimeMillis > 0) firstStartTimeMillis - enqueueTimeMillis else -1
        return "AniSyncWork worker=${workerClassName?.substringAfterLast('.')} state=$state " +
            "run_ms=$workerDurationMillis total_ms=$totalRuntimeMillis wait_ms=$waitMs " +
            "attempts=$runAttemptCount retries=$explicitRetryCount stops=${stops.ifEmpty { "none" }}"
    }
}
