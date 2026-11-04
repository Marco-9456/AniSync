package com.anisync.android.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.anisync.android.data.account.AccountStore
import com.anisync.android.data.AppSettings
import com.anisync.android.data.network.RequestPriority
import com.anisync.android.data.network.withRequestPriority
import com.anisync.android.data.util.ApiError
import com.anisync.android.domain.RecapRepository
import com.anisync.android.domain.recap.nextRecapRun
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import com.anisync.android.domain.Result as DomainResult

/**
 * Builds the recaps that became due, on the 1st at the time chosen in the Recaps settings, then
 * queues its own next run.
 *
 * One-time work rather than periodic, because periodic work cannot be pinned to a date and would
 * drift into delivering a month's recap a day late. Each run measures the delay to the next 1st
 * again, so a changed time or a clock change is picked up by the run after it.
 *
 * Only the active account is built. Rows are scoped by account, so another account keeps what it
 * had and gets its own recaps once it is the active one on a 1st.
 */
@HiltWorker
class RecapWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val recapRepository: RecapRepository,
    private val appSettings: AppSettings,
    private val accountStore: AccountStore,
    private val notifier: RecapNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (inputData.getBoolean(KEY_FIRST_BUILD, false)) return firstBuild()
        val owner = accountStore.activeAccount.value?.id
        val outcome = withRequestPriority(RequestPriority.Background) {
            recapRepository.buildDue(monthly = appSettings.recapMonthly.value, yearly = appSettings.recapYearly.value)
        }
        when (outcome) {
            is DomainResult.Success -> if (owner != null && owner == accountStore.activeAccount.value?.id) {
                // A notification that cannot be posted must not cost next month's run.
                try {
                    notifier.announce(owner, outcome.data)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Recap notification failed", e)
                }
            }
            is DomainResult.Error -> {
                Log.w(TAG, "Recap build failed: ${outcome.message}")
                if (outcome.isWorthRetrying() && runAttemptCount < MAX_ATTEMPTS) return Result.retry()
            }
        }
        schedule(applicationContext, appSettings.recapTime.value, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    /** The first build a shelf asks for: everything retention keeps, no notification, no schedule change. */
    private suspend fun firstBuild(): Result {
        val outcome = withRequestPriority(RequestPriority.Background) { recapRepository.backfill() }
        if (outcome is DomainResult.Error) {
            Log.w(TAG, "First recap build failed: ${outcome.message}")
            if (outcome.isWorthRetrying() && runAttemptCount < MAX_ATTEMPTS) return Result.retry()
        }
        return Result.success()
    }

    private fun DomainResult.Error.isWorthRetrying(): Boolean = when (exception) {
        is ApiError.Deferred, is ApiError.RateLimited, is ApiError.ServerError,
        is ApiError.Offline, is ApiError.Timeout -> true
        else -> exception is java.io.IOException
    }

    companion object {
        private const val TAG = "RecapWorker"
        private const val WORK_NAME = "recap_job"
        private const val MAX_ATTEMPTS = 5

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Queues the run for the coming 1st at [time]. KEEP leaves an already queued run alone, the
         * worker itself appends the next one after it finishes, and a changed time replaces it.
         */
        fun schedule(context: Context, time: LocalTime, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
            val clock = Clock.systemDefaultZone()
            val delayMs = Duration.between(ZonedDateTime.now(clock), nextRecapRun(clock, time)).toMillis()
            val request = OneTimeWorkRequestBuilder<RecapWorker>()
                .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(WORK_NAME, policy, request)
        }

        /**
         * Moves the queued run to a new [time]. A run that is going or waiting to retry is left alone,
         * since it builds what is due and then queues the one after it at the new time itself.
         */
        suspend fun reschedule(context: Context, time: LocalTime) {
            val infos = WorkManager.getInstance(context.applicationContext).getWorkInfosForUniqueWorkFlow(WORK_NAME).first()
            if (queuedRunMayMove(infos.map { it.state to it.runAttemptCount })) {
                schedule(context, time, ExistingWorkPolicy.REPLACE)
            }
        }

        /** True when nothing is under way or waiting to retry, so replacing the queued run loses nothing. */
        internal fun queuedRunMayMove(runs: List<Pair<WorkInfo.State, Int>>): Boolean = runs.none { (state, attempts) ->
            state == WorkInfo.State.RUNNING || state == WorkInfo.State.BLOCKED ||
                (state == WorkInfo.State.ENQUEUED && attempts > 0)
        }

        const val FIRST_BUILD_NAME = "recap_first_build"
        private const val KEY_FIRST_BUILD = "first_build"

        /**
         * Builds every recap retention keeps, for an account with none stored yet. KEEP holds a build
         * that is already queued or running, so opening the shelf again never starts a second one.
         */
        fun firstBuild(context: Context) {
            val request = OneTimeWorkRequestBuilder<RecapWorker>()
                .setInputData(workDataOf(KEY_FIRST_BUILD to true))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(FIRST_BUILD_NAME, ExistingWorkPolicy.KEEP, request)
        }

        /** Stops a first build that is queued or running, so it cannot store again what was just cleared. */
        fun cancelFirstBuild(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(FIRST_BUILD_NAME)
        }

        /** Developer Tools: runs the job now, as it would run on the 1st, then queues the next one. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RecapWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
