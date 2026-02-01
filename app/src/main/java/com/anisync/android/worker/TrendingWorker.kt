package com.anisync.android.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anisync.android.GetTrendingQuery
import com.anisync.android.data.local.dao.TrendingDao
import com.anisync.android.data.local.entity.TrendingEntity
import com.anisync.android.type.MediaSeason
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class TrendingWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apolloClient: ApolloClient,
    private val trendingDao: TrendingDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) // 0-11
            
            // Determine Season
            val season = when (month) {
                0, 1, 2 -> MediaSeason.WINTER // Jan, Feb, Mar
                3, 4, 5 -> MediaSeason.SPRING // Apr, May, Jun
                6, 7, 8 -> MediaSeason.SUMMER // Jul, Aug, Sep
                else -> MediaSeason.FALL      // Oct, Nov, Dec
            }

            // Adjust year for Winter (Winter starts in Dec of previous year technically, but typically "Winter X" is start of year X)
            // But usually "Winter 2024" means Jan-Mar 2024. 
            // However, December is typically "Winter" of the NEXT year.
            // Let's keep it simple: based on Month index. 
            
            val response = apolloClient.query(
                GetTrendingQuery(
                    season = Optional.present(season),
                    seasonYear = Optional.present(year),
                    perPage = Optional.present(10)
                )
            ).execute()

            if (response.hasErrors()) {
                Log.e("TrendingWorker", "API Error: ${response.errors?.firstOrNull()?.message}")
                return Result.retry()
            }

            val mediaList = response.data?.Page?.media?.filterNotNull() ?: emptyList()
            
            val entities = mediaList.mapIndexedNotNull { index, media ->
                 val mId = media.id ?: return@mapIndexedNotNull null
                 
                 TrendingEntity(
                    id = mId,
                    titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                    coverUrl = media.coverImage?.extraLarge,
                    averageScore = media.averageScore,
                    rank = index + 1
                )
            }

            trendingDao.clearAll()
            trendingDao.insertAll(entities)

            // Note: TrendingWidget was removed, data is kept for future reimplementation

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

