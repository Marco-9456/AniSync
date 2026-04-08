package com.anisync.android.data

import com.anisync.android.GetUserStatisticsQuery
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.AnimeStatistics
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.MangaStatistics
import com.anisync.android.domain.ReleaseYearStat
import com.anisync.android.domain.Result
import com.anisync.android.domain.ScoreStat
import com.anisync.android.domain.StatisticsRepository
import com.anisync.android.domain.StatusStat
import com.anisync.android.domain.StudioStat
import com.anisync.android.domain.UserStatistics
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import javax.inject.Inject

/**
 * Implementation of [StatisticsRepository] using Apollo GraphQL client.
 */
class StatisticsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : StatisticsRepository {

    override suspend fun getUserStatistics(userId: Int): Result<UserStatistics> {
        return safeApiCall {
            val response = apolloClient.query(
                GetUserStatisticsQuery(userId = Optional.present(userId))
            ).execute()

            val user = response.data?.User
                ?: throw Exception("User not found")

            val animeStats = user.statistics?.anime
                ?: throw Exception("No anime statistics available")

            val mangaStats = user.statistics?.manga

            val minutesWatched = animeStats.minutesWatched ?: 0
            val daysWatched = minutesWatched / 1440f // Convert minutes to days

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
                animeStats = AnimeStatistics(
                    totalCount = animeStats.count ?: 0,
                    episodesWatched = animeStats.episodesWatched ?: 0,
                    minutesWatched = minutesWatched,
                    daysWatched = daysWatched,
                    meanScore = (animeStats.meanScore ?: 0.0).toFloat(),
                    statusDistribution = animeStats.statuses?.mapNotNull { status ->
                        status?.let {
                            StatusStat(
                                status = it.status?.name ?: "Unknown",
                                count = it.count ?: 0
                            )
                        }
                    } ?: emptyList(),
                    genreDistribution = animeStats.genres?.mapNotNull { genre ->
                        genre?.let {
                            GenreStat(
                                genre = it.genre ?: "Unknown",
                                count = it.count,
                                meanScore = (it.meanScore ?: 0.0).toFloat(),
                                hoursWatched = (it.minutesWatched ?: 0) / 60f
                            )
                        }
                    } ?: emptyList(),
                    scoreDistribution = animeStats.scores?.mapNotNull { score ->
                        score?.let {
                            ScoreStat(
                                score = it.score ?: 0,
                                count = it.count,
                                meanScore = (it.meanScore ?: 0.0).toFloat(),
                                hoursWatched = (it.minutesWatched ?: 0) / 60f
                            )
                        }
                    }?.filter { it.score > 0 } ?: emptyList(),
                    formatDistribution = animeStats.formats?.mapNotNull { format ->
                        format?.let {
                            FormatStat(
                                format = it.format?.name ?: "Unknown",
                                count = it.count,
                                meanScore = (it.meanScore ?: 0.0).toFloat(),
                                hoursWatched = (it.minutesWatched ?: 0) / 60f
                            )
                        }
                    } ?: emptyList(),
                    releaseYearDistribution = animeStats.releaseYears?.mapNotNull { year ->
                        year?.let {
                            ReleaseYearStat(
                                year = it.releaseYear ?: 0,
                                count = it.count,
                                meanScore = (it.meanScore ?: 0.0).toFloat(),
                                hoursWatched = (it.minutesWatched ?: 0) / 60f
                            )
                        }
                    }?.filter { it.year > 0 }?.sortedByDescending { it.year } ?: emptyList(),
                    studioDistribution = animeStats.studios?.mapNotNull { studio ->
                        studio?.let {
                            StudioStat(
                                studioName = it.studio?.name ?: "Unknown",
                                count = it.count,
                                meanScore = (it.meanScore ?: 0.0).toFloat(),
                                hoursWatched = (it.minutesWatched ?: 0) / 60f
                            )
                        }
                    } ?: emptyList()
                ),
                mangaStats = mangaStats?.let {
                    MangaStatistics(
                        totalCount = it.count ?: 0,
                        chaptersRead = it.chaptersRead ?: 0,
                        meanScore = (it.meanScore ?: 0.0).toFloat()
                    )
                }
            )
        }
    }
}
