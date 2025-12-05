package com.anisync.android.data

import com.anisync.android.GetMediaBySortQuery
import com.anisync.android.GetUpcomingMediaQuery
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaSort
import com.apollographql.apollo3.ApolloClient
import javax.inject.Inject

import com.apollographql.apollo3.api.Optional

class DiscoverRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : DiscoverRepository {

    override suspend fun getTrendingAnime(): List<LibraryEntry> {
        return fetchMedia(listOf(MediaSort.TRENDING_DESC))
    }

    override suspend fun getPopularAnime(): List<LibraryEntry> {
        return fetchMedia(listOf(MediaSort.POPULARITY_DESC))
    }

    override suspend fun getUpcomingAnime(): List<LibraryEntry> {
        val response = apolloClient.query(
            GetUpcomingMediaQuery(
                perPage = Optional.present(10),
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
                totalEpisodes = null,
                status = LibraryStatus.UNKNOWN
            )
        } ?: emptyList()
    }

    private suspend fun fetchMedia(sort: List<MediaSort>): List<LibraryEntry> {
        val response = apolloClient.query(
            GetMediaBySortQuery(
                sort = Optional.present(sort), 
                perPage = Optional.present(10)
            )
        ).execute()
        
        return response.data?.Page?.media?.filterNotNull()?.map { media ->
            LibraryEntry(
                id = 0, // No user library entry ID
                mediaId = media.id ?: 0,
                title = media.title?.userPreferred ?: "Unknown",
                coverUrl = media.coverImage?.extraLarge,
                progress = 0,
                totalEpisodes = null,
                status = LibraryStatus.UNKNOWN
            )
        } ?: emptyList()
    }
}
