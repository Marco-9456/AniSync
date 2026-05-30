package com.anisync.android.data

import com.anisync.android.AiringScheduleQuery
import com.anisync.android.data.mapper.toDomainStatus
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.AiringEpisode
import com.anisync.android.domain.CalendarRepository
import com.anisync.android.domain.Result
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import javax.inject.Inject

class CalendarRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val appSettings: AppSettings
) : CalendarRepository {

    override suspend fun getWeekSchedule(
        weekStartEpochSec: Long,
        weekEndEpochSec: Long
    ): Result<List<AiringEpisode>> = safeApiCall {
        val showAdult = appSettings.showAdultContent.value
        val episodes = mutableListOf<AiringEpisode>()
        var page = 1
        var hasNextPage = true

        // A single week rarely exceeds a couple hundred entries; cap pages as a safety net.
        while (hasNextPage && page <= MAX_PAGES) {
            val response = apolloClient.query(
                AiringScheduleQuery(
                    page = Optional.present(page),
                    perPage = Optional.present(PER_PAGE),
                    airingAtGreater = Optional.present(weekStartEpochSec.toInt()),
                    airingAtLesser = Optional.present(weekEndEpochSec.toInt())
                )
            )
                .fetchPolicy(FetchPolicy.NetworkFirst)
                .execute()

            val pageData = response.data?.Page
            val schedules = pageData?.airingSchedules?.filterNotNull().orEmpty()

            schedules.forEach { schedule ->
                val media = schedule.media ?: return@forEach
                val scheduleId = schedule.id ?: return@forEach
                val mediaId = media.id ?: return@forEach
                val isAdult = media.isAdult == true
                if (isAdult && !showAdult) return@forEach

                episodes += AiringEpisode(
                    id = scheduleId,
                    episode = schedule.episode ?: 0,
                    airingAt = (schedule.airingAt ?: 0).toLong(),
                    mediaId = mediaId,
                    titleRomaji = media.title?.romaji,
                    titleEnglish = media.title?.english,
                    titleNative = media.title?.native,
                    titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                    coverImageUrl = media.coverImage?.extraLarge
                        ?: media.coverImage?.large
                        ?: media.coverImage?.medium,
                    format = media.format?.rawValue,
                    averageScore = media.averageScore,
                    isOnList = media.mediaListEntry != null,
                    listStatus = media.mediaListEntry?.status?.toDomainStatus(),
                    isAdult = isAdult
                )
            }

            hasNextPage = pageData?.pageInfo?.hasNextPage == true
            page++
        }

        episodes.sortedBy { it.airingAt }
    }

    private companion object {
        const val PER_PAGE = 50
        const val MAX_PAGES = 10
    }
}
