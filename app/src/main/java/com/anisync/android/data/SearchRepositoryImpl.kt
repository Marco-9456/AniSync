package com.anisync.android.data

import com.anisync.android.SearchMediaQuery
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchRepository
import com.anisync.android.type.MediaType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.exception.ApolloException
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : SearchRepository {

    override suspend fun searchMedia(
        query: String, 
        type: MediaType,
        filters: SearchFilters
    ): Result<List<LibraryEntry>> {
        return try {
            val response = apolloClient.query(
                SearchMediaQuery(
                    search = Optional.present(query),
                    page = Optional.present(1),
                    perPage = Optional.present(20),
                    type = Optional.present(type),
                    status = filters.status?.let { Optional.present(it) } ?: Optional.absent(),
                    format = filters.formats.firstOrNull()?.let { Optional.present(it) } ?: Optional.absent(),
                    genres = if (filters.genres.isNotEmpty()) Optional.present(filters.genres.toList()) else Optional.absent(),
                    seasonYear = filters.year?.let { Optional.present(it) } ?: Optional.absent(),
                    season = filters.season?.let { Optional.present(it) } ?: Optional.absent()
                )
            ).execute()

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
                    mediaStatus = media.status?.name
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
