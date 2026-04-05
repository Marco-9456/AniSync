package com.anisync.android.data

import com.anisync.android.SearchAllQuery
import com.anisync.android.SearchMediaQuery
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchRepository
import com.anisync.android.domain.SearchResult
import com.anisync.android.type.MediaType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : SearchRepository {

    override suspend fun searchMedia(
        query: String,
        type: MediaType,
        filters: SearchFilters
    ): Result<List<LibraryEntry>> {
        return safeApiCall {
            val response = apolloClient.query(
                SearchMediaQuery(
                    search = Optional.present(query),
                    page = Optional.present(1),
                    perPage = Optional.present(20),
                    type = Optional.present(type),
                    status = filters.status?.let { Optional.present(it) } ?: Optional.absent(),
                    format = filters.formats.firstOrNull()?.let { Optional.present(it) }
                        ?: Optional.absent(),
                    genres = if (filters.genres.isNotEmpty()) Optional.present(filters.genres.toList()) else Optional.absent(),
                    seasonYear = filters.year?.let { Optional.present(it) } ?: Optional.absent(),
                    season = filters.season?.let { Optional.present(it) } ?: Optional.absent()
                )
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            response.data?.Page?.media?.filterNotNull()?.map { media ->
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
        }
    }

    override suspend fun searchAll(query: String): Result<GroupedSearchResults> {
        return safeApiCall {
            val response = apolloClient.query(
                SearchAllQuery(
                    search = Optional.present(query),
                    page = Optional.present(1),
                    perPage = Optional.present(5)
                )
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val data = response.data

            val characters = data?.characters?.characters?.filterNotNull()?.map { c ->
                SearchResult.CharacterResult(
                    id = c.id,
                    displayName = c.name?.userPreferred ?: c.name?.full ?: "Unknown",
                    nativeName = c.name?.native,
                    imageUrl = c.image?.medium,
                    favourites = c.favourites
                )
            } ?: emptyList()

            val staff = data?.staff?.staff?.filterNotNull()?.map { s ->
                SearchResult.StaffResult(
                    id = s.id,
                    displayName = s.name?.userPreferred ?: s.name?.full ?: "Unknown",
                    nativeName = s.name?.native,
                    imageUrl = s.image?.medium,
                    primaryOccupations = s.primaryOccupations?.filterNotNull() ?: emptyList(),
                    favourites = s.favourites
                )
            } ?: emptyList()

            val users = data?.users?.users?.filterNotNull()?.map { u ->
                SearchResult.UserResult(
                    id = u.id,
                    displayName = u.name,
                    imageUrl = u.avatar?.medium
                )
            } ?: emptyList()

            val studios = data?.studios?.studios?.filterNotNull()?.map { s ->
                SearchResult.StudioResult(
                    id = s.id,
                    displayName = s.name,
                    favourites = s.favourites
                )
            } ?: emptyList()

            GroupedSearchResults(
                characters = characters,
                staff = staff,
                users = users,
                studios = studios
            )
        }
    }
}
