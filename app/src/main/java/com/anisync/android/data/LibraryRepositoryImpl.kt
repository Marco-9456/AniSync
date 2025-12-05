package com.anisync.android.data

import com.anisync.android.GetUserLibraryQuery
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import com.anisync.android.type.MediaListStatus
import com.apollographql.apollo3.ApolloClient
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : LibraryRepository {

    override suspend fun getLibrary(username: String): List<LibraryEntry> {
        val response = apolloClient.query(GetUserLibraryQuery(username = username, type = MediaType.ANIME)).execute()
        
        if (response.hasErrors()) {
            // In a real app, parse errors. For now, empty or throw.
            return emptyList()
        }

        val lists = response.data?.MediaListCollection?.lists ?: return emptyList()

        // Flatten lists
        return lists.filterNotNull().flatMap { group ->
            group.entries?.filterNotNull()?.map { entry ->
                val media = entry.media
                
                // Map API Status to Domain Status
                val status = when (entry.status) {
                    MediaListStatus.CURRENT -> LibraryStatus.CURRENT
                    MediaListStatus.PLANNING -> LibraryStatus.PLANNING
                    MediaListStatus.COMPLETED -> LibraryStatus.COMPLETED
                    MediaListStatus.DROPPED -> LibraryStatus.DROPPED
                    MediaListStatus.PAUSED -> LibraryStatus.PAUSED
                    MediaListStatus.REPEATING -> LibraryStatus.REPEATING
                    else -> LibraryStatus.UNKNOWN
                }

                LibraryEntry(
                    id = entry.id ?: 0, // MediaList ID
                    mediaId = media?.id ?: 0,
                    title = media?.title?.userPreferred ?: "Unknown Title",
                    coverUrl = media?.coverImage?.extraLarge,
                    progress = entry.progress ?: 0,
                    totalEpisodes = media?.episodes,
                    status = status
                )
            } ?: emptyList()
        }
    }
}
