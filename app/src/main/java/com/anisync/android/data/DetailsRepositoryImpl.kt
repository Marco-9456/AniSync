package com.anisync.android.data

import com.anisync.android.GetMediaDetailsQuery
import com.anisync.android.SaveMediaListEntryMutation
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.domain.Result
import com.anisync.android.type.MediaListStatus
import com.anisync.android.util.stripHtml
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import javax.inject.Inject

class DetailsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : DetailsRepository {

    override suspend fun getMediaDetails(id: Int): Result<MediaDetails> {
        return try {
            val response = apolloClient.query(
                GetMediaDetailsQuery(id = Optional.present(id))
            ).execute()

            val media = response.data?.Media
                ?: return Result.Error("Media not found")
                
            val listEntry = media.mediaListEntry

            // Map API status to domain status
            val listStatus = when (listEntry?.status) {
                MediaListStatus.CURRENT -> LibraryStatus.CURRENT
                MediaListStatus.PLANNING -> LibraryStatus.PLANNING
                MediaListStatus.COMPLETED -> LibraryStatus.COMPLETED
                MediaListStatus.DROPPED -> LibraryStatus.DROPPED
                MediaListStatus.PAUSED -> LibraryStatus.PAUSED
                MediaListStatus.REPEATING -> LibraryStatus.REPEATING
                else -> null
            }
            
            // Map characters
            val characters = media.characters?.edges?.filterNotNull()?.map { edge ->
                CharacterInfo(
                    id = edge.node?.id ?: 0,
                    name = edge.node?.name?.userPreferred ?: "Unknown",
                    imageUrl = edge.node?.image?.large,
                    role = edge.role?.name ?: "SUPPORTING"
                )
            } ?: emptyList()
            
            // Map relations
            val relations = media.relations?.edges?.filterNotNull()?.map { edge ->
                RelatedMedia(
                    id = edge.node?.id ?: 0,
                    title = edge.node?.title?.userPreferred ?: "Unknown",
                    coverUrl = edge.node?.coverImage?.large,
                    format = edge.node?.format?.name,
                    status = edge.node?.status?.name,
                    relationType = edge.relationType?.name ?: "OTHER"
                )
            } ?: emptyList()

            val details = MediaDetails(
                id = media.id ?: 0,
                title = media.title?.english ?: media.title?.romaji ?: "Unknown Title",
                coverUrl = media.coverImage?.extraLarge,
                bannerUrl = media.bannerImage,
                description = media.description?.stripHtml() ?: "No description available.",
                score = media.averageScore,
                episodes = media.episodes,
                chapters = media.chapters,
                volumes = media.volumes,
                type = media.type,
                status = media.status?.name ?: "UNKNOWN",
                format = media.format?.name,
                genres = media.genres?.filterNotNull() ?: emptyList(),
                studio = media.studios?.nodes?.firstOrNull()?.name,
                year = media.startDate?.year,
                listEntryId = listEntry?.id,
                listStatus = listStatus,
                listProgress = listEntry?.progress,
                characters = characters,
                relations = relations
            )
            
            Result.Success(details)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun updateMediaListEntry(mediaId: Int, status: LibraryStatus, progress: Int): Result<Unit> {
        return try {
            val apiStatus = when (status) {
                LibraryStatus.CURRENT -> MediaListStatus.CURRENT
                LibraryStatus.PLANNING -> MediaListStatus.PLANNING
                LibraryStatus.COMPLETED -> MediaListStatus.COMPLETED
                LibraryStatus.DROPPED -> MediaListStatus.DROPPED
                LibraryStatus.PAUSED -> MediaListStatus.PAUSED
                LibraryStatus.REPEATING -> MediaListStatus.REPEATING
                LibraryStatus.UNKNOWN -> MediaListStatus.PLANNING
            }

            val response = apolloClient.mutation(
                SaveMediaListEntryMutation(
                    mediaId = Optional.present(mediaId),
                    status = Optional.present(apiStatus),
                    progress = Optional.present(progress)
                )
            ).execute()

            if (response.data?.SaveMediaListEntry != null && !response.hasErrors()) {
                Result.Success(Unit)
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Failed to update entry"
                Result.Error(errorMessage)
            }
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun deleteMediaListEntry(listEntryId: Int): Result<Unit> {
        return try {
            val response = apolloClient.mutation(
                com.anisync.android.DeleteMediaListEntryMutation(
                    id = Optional.present(listEntryId)
                )
            ).execute()

            if (response.data?.DeleteMediaListEntry?.deleted == true && !response.hasErrors()) {
                Result.Success(Unit)
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Failed to delete entry"
                Result.Error(errorMessage)
            }
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }
}
