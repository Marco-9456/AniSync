package com.anisync.android.worker

import com.anisync.android.data.network.RequestPriority
import com.anisync.android.data.network.withRequestPriority
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.type.MediaType
import com.anisync.android.widget.core.WidgetRefresh
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import com.anisync.android.domain.Result as DomainResult

/**
 * Fetches one media type library, then repaints the widgets.
 *
 * Here for the manga side of Watch Progress. Widgets read Room and never the network, which is right
 * for rendering, but a type the app never opened has no rows at all: switching the widget to Manga
 * showed the empty state and the only fix was to open the app, visit the manga tab and come back.
 * Now the widget asks for a sync when it finds nothing and gets repainted once the rows land. The
 * app asks for one as well when it comes back to the foreground, see [enqueueIfStale].
 *
 * The tap does not wait on this. It applies straight away against whatever Room has, this only
 * fills the gap after.
 */
@HiltWorker
class LibrarySyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val libraryRepository: LibraryRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result =
        // Every request this run makes yields to whatever the user is doing. When the
        // budget is short the gate refuses instead of waiting, and WorkManager reruns us.
        withRequestPriority(RequestPriority.Background) { syncLibrary() }

    private suspend fun syncLibrary(): Result {
        val type = inputData.getString(KEY_TYPE)
            ?.let { runCatching { MediaType.valueOf(it) }.getOrNull() }
            ?: return Result.failure()

        // Callers that only want a stale list fetched stop here when this process fetched it lately.
        val maxAge = inputData.getLong(KEY_MAX_AGE, -1L)
        val refreshedAt = libraryRepository.lastRefreshedAt(type)
        if (maxAge >= 0 && refreshedAt != null && System.currentTimeMillis() - refreshedAt < maxAge) {
            return Result.success()
        }

        // Empty username means resolve the signed in viewer, which the repository already does.
        return when (libraryRepository.refreshLibrary("", type)) {
            is DomainResult.Success -> {
                WidgetRefresh.all(appContext)
                // Titles added elsewhere need their tags too, or the library's tag filter misses them.
                libraryRepository.syncLibraryTags(type)
                Result.success()
            }

            is DomainResult.Error -> Result.retry()
        }
    }

    companion object {
        private const val KEY_TYPE = "media_type"
        private const val KEY_MAX_AGE = "max_age_ms"

        /** How long a fetched list is trusted before the app coming back fetches it again. */
        private val STALE_AFTER_MS = TimeUnit.MINUTES.toMillis(10)

        /**
         * Fetches [type] unless this process already did in the last [STALE_AFTER_MS].
         *
         * The list indicators on the browsing screens, the tracker on a title page and the widgets
         * all read Room. Fetching only when the Library tab opened left a manga list the user never
         * opened looking untracked, and every list on whatever the last visit saw, however much was
         * watched on the website or in another app since.
         */
        fun enqueueIfStale(context: Context, type: MediaType) {
            enqueue(context, type, maxAgeMs = STALE_AFTER_MS)
        }

        /**
         * Fetches [type] again even when a sync is already under way, since that one can have started
         * before AniList changed the scores.
         */
        fun resync(context: Context, type: MediaType) {
            enqueue(context, type, policy = ExistingWorkPolicy.REPLACE)
        }

        /**
         * Asks for a sync of [type], one in flight per type at most.
         *
         * KEEP not REPLACE: the widget asks on every render while the list is empty, and replacing
         * would restart the fetch each time and never finish it.
         */
        fun enqueue(
            context: Context,
            type: MediaType,
            maxAgeMs: Long = -1L,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        ) {
            val request = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_TYPE, type.rawValue)
                        .putLong(KEY_MAX_AGE, maxAgeMs)
                        .build()
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "library_sync_${type.rawValue}",
                policy,
                request
            )
        }
    }
}
