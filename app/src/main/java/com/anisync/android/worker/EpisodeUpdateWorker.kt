package com.anisync.android.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.widget.UpNextWidget
import com.anisync.android.widget.QuickUpdateWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EpisodeUpdateWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val libraryDao: LibraryDao,
    private val libraryRepository: com.anisync.android.domain.LibraryRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val mediaId = inputData.getInt("mediaId", -1)
        if (mediaId == -1) return Result.failure()

        try {
            val entry = libraryDao.getEntry(mediaId) ?: return Result.failure()
            // Increment progress
            val newProgress = entry.progress + 1
            
            // Use Repository to update local + sync to network
            val result = libraryRepository.updateProgress(mediaId, newProgress)
            
            if (result is com.anisync.android.domain.Result.Error) {
                // If sync failed but local might be updated, we proceed to update widgets
                // But ideally we should match repository behavior.
                // Repository implementation updates local first then syncs.
            }

            // Update Widgets
            val manager = GlanceAppWidgetManager(appContext)
            
            val upNextIds = manager.getGlanceIds(UpNextWidget::class.java)
            upNextIds.forEach { glanceId ->
                UpNextWidget().update(appContext, glanceId)
            }

            val quickUpdateIds = manager.getGlanceIds(QuickUpdateWidget::class.java)
            quickUpdateIds.forEach { glanceId ->
                QuickUpdateWidget().update(appContext, glanceId)
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
