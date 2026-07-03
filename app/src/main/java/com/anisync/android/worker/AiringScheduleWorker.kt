package com.anisync.android.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anisync.android.AiringScheduleQuery
import com.anisync.android.data.AppSettings
import com.anisync.android.data.StreamingService
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.data.util.ApiError
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.widget.AiringTodayWidget
import com.anisync.android.widget.UpNextWidget
import com.anisync.android.widget.WeeklyCalendarWidget
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AiringScheduleWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apolloClient: ApolloClient,
    private val airingScheduleDao: AiringScheduleDao,
    private val libraryDao: LibraryDao,
    private val accountStore: com.anisync.android.data.account.AccountStore,
    private val appSettings: AppSettings
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val INITIAL_WORK_NAME = "AiringScheduleWorkerInitial"

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<AiringScheduleWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                INITIAL_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDaySeconds = calendar.timeInMillis / 1000
            // Fetch 7 days of data for future widget support
            val endOfWeekSeconds = startOfDaySeconds + (7 * 86400)

            val entities = mutableListOf<AiringScheduleEntity>()
            var page = 1
            var hasNextPage = true
            val preferredStreamingService = appSettings.getPreferredStreamingServiceDirect()

            while (page <= 3 && hasNextPage) {
                val response = apolloClient.query(
                    AiringScheduleQuery(
                        page = Optional.present(page),
                        perPage = Optional.present(50),
                        airingAtGreater = Optional.present(startOfDaySeconds.toInt()),
                        airingAtLesser = Optional.present(endOfWeekSeconds.toInt())
                    )
                )
                    // Query variables repeat within a day, so the implicit
                    // CacheFirst default turned intraday refreshes into no-ops
                    // and schedule changes (delays, swaps) were never picked up.
                    .fetchPolicy(FetchPolicy.NetworkOnly)
                    .execute()

                if (response.hasErrors()) {
                    return Result.retry()
                }

                val pageData = response.data?.Page
                val schedules = pageData?.airingSchedules?.filterNotNull() ?: emptyList()

                val pageEntities = schedules.mapNotNull { schedule ->
                    val media = schedule.media ?: return@mapNotNull null

                    // Safely handle nullable ids from GraphQL
                    val sId = schedule.id ?: return@mapNotNull null
                    val mId = media.id ?: return@mapNotNull null

                    // Check if user is actively watching this anime (only CURRENT status, not PLANNING)
                    val libraryEntry = libraryDao.getEntry(accountStore.activeAccount.value?.id ?: -1, mId)
                    val isWatching = libraryEntry?.status == LibraryStatus.CURRENT

                    val streamingSeriesUrl = selectStreamingSeriesUrl(
                        externalLinks = media.externalLinks,
                        preferredService = preferredStreamingService
                    )

                    AiringScheduleEntity(
                        id = sId,
                        mediaId = mId,
                        airingAt = schedule.airingAt?.toLong() ?: 0L,
                        episode = schedule.episode ?: 0,
                        titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                        coverUrl = media.coverImage?.extraLarge,
                        format = media.format?.rawValue,
                        isWatching = isWatching,
                        streamingSeriesUrl = streamingSeriesUrl
                    )
                }

                entities.addAll(pageEntities)
                hasNextPage = pageData?.pageInfo?.hasNextPage == true
                page++
            }

            airingScheduleDao.clearAll() // Simple cache strategy: replace all
            airingScheduleDao.insertAll(entities)

            // Update Widgets
            val manager = GlanceAppWidgetManager(appContext)
            manager.getGlanceIds(AiringTodayWidget::class.java).forEach { id ->
                AiringTodayWidget().update(appContext, id)
            }
            manager.getGlanceIds(UpNextWidget::class.java).forEach { id ->
                UpNextWidget().update(appContext, id)
            }
            manager.getGlanceIds(WeeklyCalendarWidget::class.java).forEach { id ->
                WeeklyCalendarWidget().update(appContext, id)
            }

            Result.success()
        } catch (e: ApiError.RateLimited) {
            Result.retry()
        } catch (e: ApiError.Unauthorized) {
            Result.failure()
        } catch (e: ApiError.ServerError) {
            Result.retry()
        } catch (e: java.io.IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private data class StreamingCandidate(
        val site: String?,
        val url: String
    )

    private fun selectStreamingSeriesUrl(
        externalLinks: List<AiringScheduleQuery.ExternalLink?>?,
        preferredService: StreamingService
    ): String? {
        val candidates = externalLinks
            .orEmpty()
            .asSequence()
            .filterNotNull()
            .filter { link -> link.type?.rawValue == "STREAMING" }
            .mapNotNull { link ->
                val validUrl = link.url
                    ?.trim()
                    ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                    ?: return@mapNotNull null

                StreamingCandidate(
                    site = link.site?.trim()?.takeIf { it.isNotBlank() },
                    url = validUrl
                )
            }
            .toList()

        if (candidates.isEmpty()) {
            return null
        }

        if (preferredService != StreamingService.NONE) {
            val preferredName = preferredService.displayName.trim()
            val preferredCandidate = candidates.firstOrNull { candidate ->
                val site = candidate.site ?: return@firstOrNull false
                site.equals(preferredName, ignoreCase = true) ||
                    site.contains(preferredName, ignoreCase = true) ||
                    preferredName.contains(site, ignoreCase = true)
            }
            if (preferredCandidate != null) {
                return preferredCandidate.url
            }
        }

        return candidates.first().url
    }
}
