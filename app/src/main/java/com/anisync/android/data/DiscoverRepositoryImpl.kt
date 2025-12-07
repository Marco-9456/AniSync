package com.anisync.android.data

import com.anisync.android.GetMediaBySortQuery
import com.anisync.android.GetUpcomingMediaQuery
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaSort
import com.anisync.android.type.MediaType
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import javax.inject.Inject

class DiscoverRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : DiscoverRepository {

    override suspend fun getTrending(type: MediaType): List<LibraryEntry> {
        return fetchMedia(listOf(MediaSort.TRENDING_DESC), type)
    }

    override suspend fun getPopular(type: MediaType): List<LibraryEntry> {
        return fetchMedia(listOf(MediaSort.POPULARITY_DESC), type)
    }

    override suspend fun getUpcoming(type: MediaType): List<LibraryEntry> {
        val response = apolloClient.query(
            GetUpcomingMediaQuery(
                perPage = Optional.present(10),
                type = Optional.present(type),
                status = Optional.present(com.anisync.android.type.MediaStatus.NOT_YET_RELEASED)
            )
        ).execute()

        return response.data?.Page?.media?.filterNotNull()?.map { media ->
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
    }

    private suspend fun fetchMedia(sort: List<MediaSort>, type: MediaType): List<LibraryEntry> {
        val response = apolloClient.query(
            GetMediaBySortQuery(
                sort = Optional.present(sort),
                type = Optional.present(type),
                perPage = Optional.present(10)
            )
        ).execute()

        return response.data?.Page?.media?.filterNotNull()?.map { media ->
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
                mediaStatus = null // Status not fetched in this specific query currently
            )
        } ?: emptyList()
    }
}