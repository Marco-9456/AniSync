package com.anisync.android.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.widget.UpNextWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EpisodeUpdateWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val libraryDao: LibraryDao,
    private val accountStore: com.anisync.android.data.account.AccountStore,
    private val libraryRepository: com.anisync.android.domain.LibraryRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val mediaId = inputData.getInt("mediaId", -1)
        val amount = inputData.getInt("amount", 1) // Default to +1
        if (mediaId == -1) return Result.failure()

        try {
            val ownerId = accountStore.activeAccount.value?.id ?: -1
            val entry = libraryDao.getEntry(ownerId, mediaId) ?: return Result.failure()
            // Update progress
            val newProgress = (entry.progress + amount).coerceAtLeast(0)
            
            // 1. Update LOCAL only (Optimistic)
            val localResult = libraryRepository.updateProgressLocal(mediaId, newProgress)
            if (localResult is com.anisync.android.domain.Result.Error) {
                 return Result.failure()
            }

            // 2. Update Widgets IMMEDIATELY
            val manager = GlanceAppWidgetManager(appContext)
            
            val upNextIds = manager.getGlanceIds(UpNextWidget::class.java)
            upNextIds.forEach { glanceId ->
                UpNextWidget().update(appContext, glanceId)
            }

            // 3. Sync to Network (Background)
            // We call updateProgress, which will re-do local update (harmless) and then sync
            libraryRepository.updateProgress(mediaId, newProgress)

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
