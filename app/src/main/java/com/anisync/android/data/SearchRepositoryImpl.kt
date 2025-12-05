package com.anisync.android.data

import com.anisync.android.SearchMediaQuery
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.SearchRepository
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : SearchRepository {

    override suspend fun searchMedia(query: String): List<LibraryEntry> {
        val response = apolloClient.query(
            SearchMediaQuery(
                search = Optional.present(query),
                page = Optional.present(1),
                perPage = Optional.present(20)
            )
        ).execute()

        return response.data?.Page?.media?.filterNotNull()?.map { media ->
            LibraryEntry(
                id = 0, // Not a library entry, so ID is 0
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
