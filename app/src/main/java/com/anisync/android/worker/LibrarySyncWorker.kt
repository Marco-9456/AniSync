package com.anisync.android.worker

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
import kotlinx.coroutines.flow.first
import com.anisync.android.domain.Result as DomainResult

/**
 * Fetches one media type library, then repaints the widgets.
 *
 * Here for the manga side of Watch Progress. Widgets read Room and never the network, which is right
 * for rendering, but a type the app never opened has no rows at all: switching the widget to Manga
 * showed the empty state and the only fix was to open the app, visit the manga tab and come back.
 * Now the widget asks for a sync when it finds nothing and gets repainted once the rows land.
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

    override suspend fun doWork(): Result {
        val type = inputData.getString(KEY_TYPE)
            ?.let { runCatching { MediaType.valueOf(it) }.getOrNull() }
            ?: return Result.failure()

        // Callers that only want the first fill stop here once the type has rows.
        if (inputData.getBoolean(KEY_ONLY_IF_EMPTY, false) &&
            libraryRepository.observeLibrary("", type).first().isNotEmpty()
        ) {
            return Result.success()
        }

        // Empty username means resolve the signed in viewer, which the repository already does.
        return when (libraryRepository.refreshLibrary("", type)) {
            is DomainResult.Success -> {
                WidgetRefresh.all(appContext)
                Result.success()
            }

            is DomainResult.Error -> Result.retry()
        }
    }

    companion object {
        private const val KEY_TYPE = "media_type"
        private const val KEY_ONLY_IF_EMPTY = "only_if_empty"

        /**
         * Fills a type that has never been synced on this account, and does nothing otherwise.
         *
         * The list indicators on the browsing screens read Room, so a manga list the user never
         * opened would leave every manga card looking like it is not tracked.
         */
        fun enqueueIfEmpty(context: Context, type: MediaType) {
            enqueue(context, type, onlyIfEmpty = true)
        }

        /**
         * Asks for a sync of [type], one in flight per type at most.
         *
         * KEEP not REPLACE: the widget asks on every render while the list is empty, and replacing
         * would restart the fetch each time and never finish it.
         */
        fun enqueue(context: Context, type: MediaType, onlyIfEmpty: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_TYPE, type.rawValue)
                        .putBoolean(KEY_ONLY_IF_EMPTY, onlyIfEmpty)
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
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
