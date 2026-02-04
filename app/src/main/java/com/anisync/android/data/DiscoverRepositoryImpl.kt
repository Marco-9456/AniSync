package com.anisync.android.data

import com.anisync.android.GetMediaBySortQuery
import com.anisync.android.GetPaginatedMediaQuery
import com.anisync.android.GetUpcomingMediaQuery
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.PaginatedResult
import com.anisync.android.domain.Result
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSort
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.exception.ApolloException
import javax.inject.Inject

class DiscoverRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : DiscoverRepository {

    override suspend fun getTrending(type: MediaType): Result<List<LibraryEntry>> {
        return fetchMedia(listOf(MediaSort.TRENDING_DESC), type)
    }

    override suspend fun getPopular(type: MediaType): Result<List<LibraryEntry>> {
        return fetchMedia(listOf(MediaSort.POPULARITY_DESC), type)
    }

    override suspend fun getUpcoming(type: MediaType): Result<List<LibraryEntry>> {
        return try {
            val response = apolloClient.query(
                GetUpcomingMediaQuery(
                    perPage = Optional.present(10),
                    type = Optional.present(type),
                    status = Optional.present(MediaStatus.NOT_YET_RELEASED)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            val entries = response.data?.Page?.media?.filterNotNull()
                ?.filter { it.season != null } // Upcoming = has confirmed season
                ?.map { media -> media.toLibraryEntry("UPCOMING") }
                ?: emptyList()
            
            Result.Success(entries)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun getTBA(type: MediaType): Result<List<LibraryEntry>> {
        return try {
            val response = apolloClient.query(
                GetUpcomingMediaQuery(
                    perPage = Optional.present(20), // Fetch more to ensure we get 10 after filtering
                    type = Optional.present(type),
                    status = Optional.present(MediaStatus.NOT_YET_RELEASED)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            val entries = response.data?.Page?.media?.filterNotNull()
                ?.filter { it.season == null } // TBA = no confirmed season
                ?.take(10)
                ?.map { media -> media.toLibraryEntry("TBA") }
                ?: emptyList()
            
            Result.Success(entries)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun getPaginatedSection(
        sectionType: String,
        type: MediaType,
        page: Int,
        format: MediaFormat?
    ): Result<PaginatedResult<LibraryEntry>> {
        return try {
            val (sort, status) = when (sectionType) {
                "trending" -> listOf(MediaSort.TRENDING_DESC) to null
                "popular" -> listOf(MediaSort.POPULARITY_DESC) to null
                "upcoming" -> listOf(MediaSort.POPULARITY_DESC) to MediaStatus.NOT_YET_RELEASED
                "tba" -> listOf(MediaSort.POPULARITY_DESC) to MediaStatus.NOT_YET_RELEASED
                else -> listOf(MediaSort.POPULARITY_DESC) to null
            }

            val response = apolloClient.query(
                GetPaginatedMediaQuery(
                    page = Optional.present(page),
                    perPage = Optional.present(20),
                    sort = Optional.present(sort),
                    type = Optional.present(type),
                    status = status?.let { Optional.present(it) } ?: Optional.absent(),
                    format = format?.let { Optional.present(it) } ?: Optional.absent()
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            val pageInfo = response.data?.Page?.pageInfo
            val allMedia = response.data?.Page?.media?.filterNotNull() ?: emptyList()
            
            // For upcoming/tba, we need to filter by season presence
            val filteredMedia = when (sectionType) {
                "upcoming" -> allMedia.filter { it.season != null }
                "tba" -> allMedia.filter { it.season == null }
                else -> allMedia
            }

            val entries = filteredMedia.map { media ->
                LibraryEntry(
                    id = 0,
                    mediaId = media.id ?: 0,

                    titleRomaji = media.title?.romaji,
                    titleEnglish = media.title?.english,
                    titleNative = media.title?.native,
                    titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                    coverUrl = media.coverImage?.extraLarge,
                    progress = 0,
                    totalEpisodes = media.episodes,
                    totalChapters = media.chapters,
                    totalVolumes = media.volumes,
                    type = media.type,
                    format = media.format,
                    status = LibraryStatus.UNKNOWN,
                    mediaStatus = when (sectionType) {
                        "upcoming" -> "UPCOMING"
                        "tba" -> "TBA"
                        else -> null
                    },
                    averageScore = media.averageScore
                )
            }

            Result.Success(
                PaginatedResult(
                    items = entries,
                    hasNextPage = pageInfo?.hasNextPage ?: false,
                    currentPage = pageInfo?.currentPage ?: page,
                    totalPages = pageInfo?.lastPage ?: 1
                )
            )
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    private suspend fun fetchMedia(sort: List<MediaSort>, type: MediaType): Result<List<LibraryEntry>> {
        return try {
            val response = apolloClient.query(
                GetMediaBySortQuery(
                    sort = Optional.present(sort),
                    type = Optional.present(type),
                    perPage = Optional.present(10)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            val entries = response.data?.Page?.media?.filterNotNull()?.map { media ->
                LibraryEntry(
                    id = 0,
                    mediaId = media.id ?: 0,

                    titleRomaji = media.title?.romaji,
                    titleEnglish = media.title?.english,
                    titleNative = media.title?.native,
                    titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                    coverUrl = media.coverImage?.extraLarge,
                    progress = 0,
                    totalEpisodes = media.episodes,
                    totalChapters = media.chapters,
                    totalVolumes = media.volumes,
                    type = media.type,
                    format = media.format,
                    status = LibraryStatus.UNKNOWN,
                    mediaStatus = null,
                    averageScore = media.averageScore
                )
            } ?: emptyList()
            
            Result.Success(entries)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    private fun GetUpcomingMediaQuery.Medium.toLibraryEntry(mediaStatus: String): LibraryEntry {
        return LibraryEntry(
            id = 0,
            mediaId = this.id ?: 0,
            titleRomaji = this.title?.romaji,
            titleEnglish = this.title?.english,
            titleNative = this.title?.native,
            titleUserPreferred = this.title?.userPreferred ?: "Unknown",
            coverUrl = this.coverImage?.extraLarge,
            progress = 0,
            totalEpisodes = this.episodes,
            totalChapters = this.chapters,
            totalVolumes = this.volumes,
            type = this.type,
            format = this.format,
            status = LibraryStatus.UNKNOWN,
            mediaStatus = mediaStatus,
            averageScore = this.averageScore
        )
    }
}
