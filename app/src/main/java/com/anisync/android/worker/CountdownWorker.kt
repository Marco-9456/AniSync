package com.anisync.android.worker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anisync.android.NextAiringEpisodesQuery
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import com.anisync.android.widget.CountdownWidget
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltWorker
class CountdownWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apolloClient: ApolloClient,
    private val libraryDao: LibraryDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val nowSeconds = System.currentTimeMillis() / 1000

            // 1. Get Watching/Planning anime IDs from Library
            // Queries all "Anime" entries. 
            val animeEntries = libraryDao.getByType(MediaType.ANIME)
            val relevantIds = animeEntries
                .filter { it.status == LibraryStatus.CURRENT || it.status == LibraryStatus.PLANNING }
                .map { it.mediaId }

            if (relevantIds.isEmpty()) {
                clearCountdownData()
                updateWidget()
                return Result.success()
            }

            // 2. Fetch NEXT airing episode for these IDs
            // We can pass a list of IDs to the query.
            // Note: If the list is huge, we might need to chunk it, but for a user library it's usually fine (<500).
            val chunkedIds = relevantIds.chunked(50) // Safe chunk size
            
            var bestSchedule: NextAiringEpisodesQuery.AiringSchedule? = null

            for (chunk in chunkedIds) {
                val response = apolloClient.query(
                    NextAiringEpisodesQuery(
                        mediaIds = Optional.present(chunk),
                        airingAtGreater = Optional.present(nowSeconds.toInt()),
                        perPage = Optional.present(10) // We rely on 'sort: TIME' to get the nearest ones
                    )
                ).execute()

                if (response.hasErrors()) continue

                val schedules = response.data?.Page?.airingSchedules?.filterNotNull() ?: emptyList()
                
                // Filter valid schedules with non-null airing time
                val validSchedules = schedules.filter { it.airingAt != null }
                val nearest = validSchedules.minByOrNull { it.airingAt!! }
                
                if (nearest != null) {
                    val currentBest = bestSchedule
                    if (currentBest == null) {
                         bestSchedule = nearest
                    } else {
                         // null check safety
                         val nearTime = nearest.airingAt
                         val bestTime = currentBest.airingAt
                         
                         if (nearTime != null && bestTime != null && nearTime < bestTime) {
                             bestSchedule = nearest
                         }
                    }
                }
            }

            // 3. Save the SINGLE best result to SharedPrefs
            if (bestSchedule != null) {
                val media = bestSchedule.media
                val mId = media?.id
                
                if (media != null && mId != null) {
                    val ep = bestSchedule.episode ?: 0
                    val at = bestSchedule.airingAt?.toLong() ?: 0L
                    
                    val data = CountdownData(
                        mediaId = mId,
                        title = media.title?.userPreferred ?: "Unknown",
                        coverUrl = media.coverImage?.extraLarge,
                        episode = ep,
                        airingAt = at
                    )
                    saveCountdownData(data)
                }
            } else {
                clearCountdownData()
            }

            // 4. Update Widget
            updateWidget()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun saveCountdownData(data: CountdownData) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Json.encodeToString(data)
        prefs.edit().putString(KEY_DATA, json).apply()
    }

    private fun clearCountdownData() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DATA).apply()
    }

    private suspend fun updateWidget() {
        val manager = GlanceAppWidgetManager(appContext)
        manager.getGlanceIds(CountdownWidget::class.java).forEach { id ->
            CountdownWidget().update(appContext, id)
        }
    }

    companion object {
        const val PREFS_NAME = "countdown_widget_prefs"
        const val KEY_DATA = "countdown_data"
    }
}

@kotlinx.serialization.Serializable
data class CountdownData(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val episode: Int,
    val airingAt: Long
)
