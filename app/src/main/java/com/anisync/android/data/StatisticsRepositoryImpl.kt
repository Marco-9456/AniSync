package com.anisync.android.data

import com.anisync.android.GetUserStatisticsQuery
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
        return try {
            val response = apolloClient.query(
                GetUserStatisticsQuery(userId = Optional.present(userId))
            ).execute()

            val user = response.data?.User
                ?: return Result.Error("User not found")

            val animeStats = user.statistics?.anime
                ?: return Result.Error("No anime statistics available")

            val mangaStats = user.statistics?.manga

            val minutesWatched = animeStats.minutesWatched ?: 0
            val daysWatched = minutesWatched / 1440f // Convert minutes to days

            val userStatistics = UserStatistics(
                userId = user.id ?: return Result.Error("User ID not found"),
                userName = user.name ?: "Unknown",
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

            Result.Success(userStatistics)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch statistics")
        }
    }
}
