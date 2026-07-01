package com.anisync.android.data

import com.anisync.android.GetUserStatisticsQuery
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.ActivityHistoryDay
import com.anisync.android.domain.AnimeStatistics
import com.anisync.android.domain.CachePolicy
import com.anisync.android.domain.CountryStat
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.LengthStat
import com.anisync.android.domain.MangaStatistics
import com.anisync.android.domain.ReleaseYearStat
import com.anisync.android.domain.Result
import com.anisync.android.domain.ScoreStat
import com.anisync.android.domain.StaffStat
import com.anisync.android.domain.StartYearStat
import com.anisync.android.domain.StatisticsRepository
import com.anisync.android.domain.StatusStat
import com.anisync.android.domain.StudioStat
import com.anisync.android.domain.TagStat
import com.anisync.android.domain.UserStatistics
import com.anisync.android.domain.VoiceActorStat
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import javax.inject.Inject

class StatisticsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : StatisticsRepository {

    @Suppress("DEPRECATION") // User.stats is deprecated but is the only source of activityHistory
    override suspend fun getUserStatistics(
        userId: Int,
        policy: CachePolicy
    ): Result<UserStatistics> {
        return safeApiCall {
            val response = apolloClient.query(
                GetUserStatisticsQuery(userId = Optional.present(userId))
            ).fetchPolicy(policy.toFetchPolicy()).execute()

            val user = response.data?.User
                ?: throw Exception("User not found")

            val animeApi = user.statistics?.anime
                ?: throw Exception("No anime statistics available")
            val mangaApi = user.statistics?.manga

            val scoreFormatApi = user.mediaListOptions?.scoreFormat
            val domainScoreFormat = scoreFormatApi?.let { format ->
                when (format.name) {
                    "POINT_100" -> com.anisync.android.domain.ScoreFormat.POINT_100
                    "POINT_10_DECIMAL" -> com.anisync.android.domain.ScoreFormat.POINT_10_DECIMAL
                    "POINT_10" -> com.anisync.android.domain.ScoreFormat.POINT_10
                    "POINT_5" -> com.anisync.android.domain.ScoreFormat.POINT_5
                    "POINT_3" -> com.anisync.android.domain.ScoreFormat.POINT_3
                    else -> null
                }
            }

            UserStatistics(
                userId = user.id ?: throw Exception("User ID not found"),
                userName = user.name ?: "Unknown",
                scoreFormat = domainScoreFormat,
                animeStats = animeApi.toDomain(),
                mangaStats = mangaApi?.toDomain(),
                activityHistory = user.stats?.activityHistory.orEmpty().mapNotNull { entry ->
                    val date = entry?.date ?: return@mapNotNull null
                    ActivityHistoryDay(
                        date = date.toLong(),
                        amount = entry.amount ?: 0,
                        level = entry.level ?: 0
                    )
                }
            )
        }
    }

    private fun CachePolicy.toFetchPolicy(): FetchPolicy = when (this) {
        CachePolicy.CacheFirst -> FetchPolicy.CacheFirst
        CachePolicy.NetworkOnly -> FetchPolicy.NetworkOnly
        CachePolicy.NetworkFirst -> FetchPolicy.NetworkFirst
    }

    private fun GetUserStatisticsQuery.Anime.toDomain(): AnimeStatistics {
        val minutes = minutesWatched ?: 0
        return AnimeStatistics(
            totalCount = count ?: 0,
            episodesWatched = episodesWatched ?: 0,
            minutesWatched = minutes,
            daysWatched = minutes / 1440f,
            meanScore = (meanScore ?: 0.0).toFloat(),
            standardDeviation = (standardDeviation ?: 0.0).toFloat(),
            statusDistribution = statuses?.mapNotNull { s ->
                s?.let { StatusStat(it.status?.name ?: "Unknown", it.count ?: 0) }
            } ?: emptyList(),
            genreDistribution = genres?.mapNotNull { g ->
                g?.let {
                    GenreStat(
                        genre = it.genre ?: "Unknown",
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.minutesWatched ?: 0) / 60f
                    )
                }
            } ?: emptyList(),
            tagDistribution = tags?.mapNotNull { t ->
                t?.tag?.let {
                    TagStat(
                        id = it.id,
                        name = it.name,
                        count = t.count,
                        meanScore = (t.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (t.minutesWatched ?: 0) / 60f
                    )
                }
            } ?: emptyList(),
            scoreDistribution = scores?.mapNotNull { s ->
                s?.let {
                    ScoreStat(
                        score = it.score ?: 0,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.minutesWatched ?: 0) / 60f
                    )
                }
            }?.filter { it.score > 0 } ?: emptyList(),
            formatDistribution = formats?.mapNotNull { f ->
                f?.let {
                    FormatStat(
                        format = it.format?.name ?: "Unknown",
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.minutesWatched ?: 0) / 60f
                    )
                }
            } ?: emptyList(),
            releaseYearDistribution = releaseYears?.mapNotNull { y ->
                y?.let {
                    ReleaseYearStat(
                        year = it.releaseYear ?: 0,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.minutesWatched ?: 0) / 60f
                    )
                }
            }?.filter { it.year > 0 }?.sortedByDescending { it.year } ?: emptyList(),
            startYearDistribution = startYears?.mapNotNull { y ->
                y?.let {
                    StartYearStat(
                        year = it.startYear ?: 0,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.minutesWatched ?: 0) / 60f
                    )
                }
            }?.filter { it.year > 0 }?.sortedByDescending { it.year } ?: emptyList(),
            lengthDistribution = lengths?.mapNotNull { l ->
                l?.let {
                    val label = it.length ?: return@let null
                    LengthStat(
                        length = label,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.minutesWatched ?: 0) / 60f
                    )
                }
            } ?: emptyList(),
            studioDistribution = studios?.mapNotNull { s ->
                val studio = s?.studio ?: return@mapNotNull null
                StudioStat(
                    id = studio.id,
                    studioName = studio.name ?: "Unknown",
                    count = s.count,
                    meanScore = (s.meanScore ?: 0.0).toFloat(),
                    hoursWatched = (s.minutesWatched ?: 0) / 60f
                )
            } ?: emptyList(),
            voiceActorDistribution = voiceActors?.mapNotNull { v ->
                v?.voiceActor?.let { va ->
                    VoiceActorStat(
                        id = va.id,
                        name = va.name?.full ?: "Unknown",
                        imageUrl = va.image?.medium,
                        count = v.count,
                        meanScore = (v.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (v.minutesWatched ?: 0) / 60f
                    )
                }
            } ?: emptyList(),
            staffDistribution = staff?.mapNotNull { s ->
                s?.staff?.let { st ->
                    StaffStat(
                        id = st.id,
                        name = st.name?.full ?: "Unknown",
                        imageUrl = st.image?.medium,
                        count = s.count,
                        meanScore = (s.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (s.minutesWatched ?: 0) / 60f
                    )
                }
            } ?: emptyList(),
            countryDistribution = countries?.mapNotNull { c ->
                c?.let {
                    val code = it.country?.toString() ?: return@let null
                    CountryStat(
                        countryCode = code,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.minutesWatched ?: 0) / 60f
                    )
                }
            } ?: emptyList()
        )
    }

    private fun GetUserStatisticsQuery.Manga.toDomain(): MangaStatistics {
        return MangaStatistics(
            totalCount = count ?: 0,
            chaptersRead = chaptersRead ?: 0,
            volumesRead = volumesRead ?: 0,
            meanScore = (meanScore ?: 0.0).toFloat(),
            standardDeviation = (standardDeviation ?: 0.0).toFloat(),
            statusDistribution = statuses?.mapNotNull { s ->
                s?.let { StatusStat(it.status?.name ?: "Unknown", it.count ?: 0) }
            } ?: emptyList(),
            genreDistribution = genres?.mapNotNull { g ->
                g?.let {
                    GenreStat(
                        genre = it.genre ?: "Unknown",
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.chaptersRead ?: 0).toFloat()
                    )
                }
            } ?: emptyList(),
            tagDistribution = tags?.mapNotNull { t ->
                t?.tag?.let {
                    TagStat(
                        id = it.id,
                        name = it.name,
                        count = t.count,
                        meanScore = (t.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (t.chaptersRead ?: 0).toFloat()
                    )
                }
            } ?: emptyList(),
            scoreDistribution = scores?.mapNotNull { s ->
                s?.let {
                    ScoreStat(
                        score = it.score ?: 0,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.chaptersRead ?: 0).toFloat()
                    )
                }
            }?.filter { it.score > 0 } ?: emptyList(),
            formatDistribution = formats?.mapNotNull { f ->
                f?.let {
                    FormatStat(
                        format = it.format?.name ?: "Unknown",
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.chaptersRead ?: 0).toFloat()
                    )
                }
            } ?: emptyList(),
            releaseYearDistribution = releaseYears?.mapNotNull { y ->
                y?.let {
                    ReleaseYearStat(
                        year = it.releaseYear ?: 0,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.chaptersRead ?: 0).toFloat()
                    )
                }
            }?.filter { it.year > 0 }?.sortedByDescending { it.year } ?: emptyList(),
            startYearDistribution = startYears?.mapNotNull { y ->
                y?.let {
                    StartYearStat(
                        year = it.startYear ?: 0,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.chaptersRead ?: 0).toFloat()
                    )
                }
            }?.filter { it.year > 0 }?.sortedByDescending { it.year } ?: emptyList(),
            lengthDistribution = lengths?.mapNotNull { l ->
                l?.let {
                    val label = it.length ?: return@let null
                    LengthStat(
                        length = label,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.chaptersRead ?: 0).toFloat()
                    )
                }
            } ?: emptyList(),
            staffDistribution = staff?.mapNotNull { s ->
                s?.staff?.let { st ->
                    StaffStat(
                        id = st.id,
                        name = st.name?.full ?: "Unknown",
                        imageUrl = st.image?.medium,
                        count = s.count,
                        meanScore = (s.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (s.chaptersRead ?: 0).toFloat()
                    )
                }
            } ?: emptyList(),
            countryDistribution = countries?.mapNotNull { c ->
                c?.let {
                    val code = it.country?.toString() ?: return@let null
                    CountryStat(
                        countryCode = code,
                        count = it.count,
                        meanScore = (it.meanScore ?: 0.0).toFloat(),
                        hoursWatched = (it.chaptersRead ?: 0).toFloat()
                    )
                }
            } ?: emptyList()
        )
    }
}
