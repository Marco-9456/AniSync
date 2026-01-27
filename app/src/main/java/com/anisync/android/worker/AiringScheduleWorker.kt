package com.anisync.android.worker

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anisync.android.AiringScheduleQuery
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.widget.AiringTodayWidget
import com.anisync.android.widget.WeeklyCalendarWidget
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AiringScheduleWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apolloClient: ApolloClient,
    private val airingScheduleDao: AiringScheduleDao,
    private val libraryDao: LibraryDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDaySeconds = calendar.timeInMillis / 1000
            // Fetch 7 days of data to support WeeklyCalendarWidget
            val endOfWeekSeconds = startOfDaySeconds + (7 * 86400)

            val response = apolloClient.query(
                AiringScheduleQuery(
                    page = Optional.present(1),
                    perPage = Optional.present(100),  // Increased for 7-day range
                    airingAtGreater = Optional.present(startOfDaySeconds.toInt()),
                    airingAtLesser = Optional.present(endOfWeekSeconds.toInt())
                )
            ).execute()

            if (response.hasErrors()) {
                Log.e("AiringScheduleWorker", "API Error: ${response.errors?.firstOrNull()?.message}")
                return Result.retry()
            }

            val schedules = response.data?.Page?.airingSchedules?.filterNotNull() ?: emptyList()
            
            val entities = schedules.mapNotNull { schedule ->
                val media = schedule.media
                if (media == null) return@mapNotNull null

                // Safely handle nullable ids from GraphQL
                val sId = schedule.id ?: return@mapNotNull null
                val mId = media.id ?: return@mapNotNull null
                
                // Check if user is watching this anime
                val libraryEntry = libraryDao.getEntry(mId)
                val isWatching = libraryEntry?.status == LibraryStatus.CURRENT || libraryEntry?.status == LibraryStatus.PLANNING

                AiringScheduleEntity(
                    id = sId,
                    mediaId = mId,
                    airingAt = schedule.airingAt?.toLong() ?: 0L,
                    episode = schedule.episode ?: 0,
                    titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                    coverUrl = media.coverImage?.extraLarge,
                    format = media.format?.rawValue,
                    isWatching = isWatching
                )
            }

            airingScheduleDao.clearAll() // Simple cache strategy: replace all
            airingScheduleDao.insertAll(entities)

            // Update Widgets
            val manager = GlanceAppWidgetManager(appContext)
            manager.getGlanceIds(AiringTodayWidget::class.java).forEach { id ->
                AiringTodayWidget().update(appContext, id)
            }
            manager.getGlanceIds(WeeklyCalendarWidget::class.java).forEach { id ->
                WeeklyCalendarWidget().update(appContext, id)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
