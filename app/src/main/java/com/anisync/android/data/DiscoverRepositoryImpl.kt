package com.anisync.android.data

import com.anisync.android.GetMediaBySortQuery
import com.anisync.android.GetUpcomingMediaQuery
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Result
import com.anisync.android.type.MediaSort
import com.anisync.android.type.MediaType
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
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
                    status = Optional.present(com.anisync.android.type.MediaStatus.NOT_YET_RELEASED)
                )
            ).execute()

            val entries = response.data?.Page?.media?.filterNotNull()?.map { media ->
                LibraryEntry(
                    id = 0,
                    mediaId = media.id ?: 0,
                    title = media.title?.userPreferred ?: "Unknown",
                    coverUrl = media.coverImage?.extraLarge,
                    progress = 0,
                    totalEpisodes = media.episodes,
                    totalChapters = media.chapters,
                    totalVolumes = media.volumes,
                    type = media.type,
                    status = LibraryStatus.UNKNOWN,
                    mediaStatus = "UPCOMING"
                )
            } ?: emptyList()
            
            Result.Success(entries)
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
            ).execute()

            val entries = response.data?.Page?.media?.filterNotNull()?.map { media ->
                LibraryEntry(
                    id = 0,
                    mediaId = media.id ?: 0,
                    title = media.title?.userPreferred ?: "Unknown",
                    coverUrl = media.coverImage?.extraLarge,
                    progress = 0,
                    totalEpisodes = media.episodes,
                    totalChapters = media.chapters,
                    totalVolumes = media.volumes,
                    type = media.type,
                    status = LibraryStatus.UNKNOWN,
                    mediaStatus = null
                )
            } ?: emptyList()
            
            Result.Success(entries)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }
}