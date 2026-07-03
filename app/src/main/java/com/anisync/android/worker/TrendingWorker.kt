package com.anisync.android.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anisync.android.GetTrendingQuery
import com.anisync.android.data.local.dao.TrendingDao
import com.anisync.android.data.local.entity.TrendingEntity
import com.anisync.android.data.util.ApiError
import com.anisync.android.type.MediaSeason
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
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
            
            val response = apolloClient.query(
                GetTrendingQuery(
                    season = Optional.present(season),
                    seasonYear = Optional.present(year),
                    perPage = Optional.present(10)
                )
            )
                // This worker's whole job is refreshing trending; the implicit
                // CacheFirst default made every run re-serve the first response.
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

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

            Result.success()
        } catch (e: ApiError.RateLimited) {
            Log.w("TrendingWorker", "Rate limited, will retry. Wait: ${e.retryAfterSeconds}s")
            Result.retry()
        } catch (e: ApiError.Unauthorized) {
            Log.w("TrendingWorker", "Unauthorized — skipping")
            Result.failure()
        } catch (e: ApiError.ServerError) {
            Log.e("TrendingWorker", "Server error ${e.statusCode}, will retry")
            Result.retry()
        } catch (e: java.io.IOException) {
            Log.w("TrendingWorker", "Network error, will retry", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e("TrendingWorker", "Unexpected error", e)
            Result.failure()
        }
    }
}
