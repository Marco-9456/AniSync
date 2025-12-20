package com.anisync.android.data

import com.anisync.android.DeleteMediaListEntryMutation
import com.anisync.android.GetMediaDetailsQuery
import com.anisync.android.SaveMediaListEntryMutation
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.data.local.toDomain
import com.anisync.android.GetCharacterDetailsQuery
import com.anisync.android.data.local.toEntity
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.domain.Result
import com.anisync.android.domain.VoiceActor
import com.anisync.android.type.MediaListStatus
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DetailsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val mediaDetailsDao: MediaDetailsDao,
    private val libraryDao: LibraryDao
) : DetailsRepository {

    /**
     * Observe media details from local cache.
     */
    override fun observeMediaDetails(id: Int): Flow<MediaDetails?> {
        return mediaDetailsDao.observeById(id)
            .map { entity -> entity?.toDomain() }
    }

    /**
     * Fetch from network and update local cache.
     */
    override suspend fun refreshMediaDetails(id: Int): Result<Unit> {
        return try {
            val response = apolloClient.query(
                GetMediaDetailsQuery(id = Optional.present(id))
            ).execute()

            val media = response.data?.Media
                ?: return Result.Error("Media not found")

            val listEntry = media.mediaListEntry
            val listStatus = when (listEntry?.status) {
                MediaListStatus.CURRENT -> LibraryStatus.CURRENT
                MediaListStatus.PLANNING -> LibraryStatus.PLANNING
                MediaListStatus.COMPLETED -> LibraryStatus.COMPLETED
                MediaListStatus.DROPPED -> LibraryStatus.DROPPED
                MediaListStatus.PAUSED -> LibraryStatus.PAUSED
                MediaListStatus.REPEATING -> LibraryStatus.REPEATING
                else -> null
            }

            val characters = media.characters?.edges?.filterNotNull()?.map { edge ->
                CharacterInfo(
                    id = edge.node?.id ?: 0,
                    name = edge.node?.name?.userPreferred ?: "Unknown",
                    imageUrl = edge.node?.image?.large,
                    role = edge.role?.name ?: "UNKNOWN"
                )
            } ?: emptyList()

            val relations = media.relations?.edges?.filterNotNull()?.map { edge ->
                val node = edge.node
                RelatedMedia(
                    id = node?.id ?: 0,
                    title = node?.title?.userPreferred ?: "Unknown",
                    coverUrl = node?.coverImage?.large,
                    format = node?.format?.name,
                    status = node?.status?.name,
                    relationType = edge.relationType?.name ?: "UNKNOWN"
                )
            } ?: emptyList()

            // Use english title if available, otherwise romaji
            val title = media.title?.english ?: media.title?.romaji ?: "Unknown"

            val details = MediaDetails(
                id = media.id ?: 0,
                title = title,
                coverUrl = media.coverImage?.extraLarge,
                bannerUrl = media.bannerImage,
                description = media.description?.stripHtml() ?: "",
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

            // Update cache
            mediaDetailsDao.insert(details.toEntity())
            
            Result.Success(Unit)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun updateMediaListEntry(
        mediaId: Int,
        status: LibraryStatus,
        progress: Int
    ): Result<Unit> {
        return try {
            val apiStatus = when (status) {
                LibraryStatus.CURRENT -> MediaListStatus.CURRENT
                LibraryStatus.PLANNING -> MediaListStatus.PLANNING
                LibraryStatus.COMPLETED -> MediaListStatus.COMPLETED
                LibraryStatus.DROPPED -> MediaListStatus.DROPPED
                LibraryStatus.PAUSED -> MediaListStatus.PAUSED
                LibraryStatus.REPEATING -> MediaListStatus.REPEATING
                LibraryStatus.UNKNOWN -> MediaListStatus.CURRENT
            }

            val response = apolloClient.mutation(
                SaveMediaListEntryMutation(
                    mediaId = Optional.present(mediaId),
                    status = Optional.present(apiStatus),
                    progress = Optional.present(progress)
                )
            ).execute()

            if (response.data?.SaveMediaListEntry != null && !response.hasErrors()) {
                // Refresh cache to get updated list entry
                refreshMediaDetails(mediaId)
                // Also update the library cache so LibraryScreen reflects the change immediately
                libraryDao.updateStatusAndProgress(mediaId, status, progress)
                Result.Success(Unit)
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Update failed"
                Result.Error(errorMessage)
            }
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun deleteMediaListEntry(entryId: Int, mediaId: Int): Result<Unit> {
        return try {
            val response = apolloClient.mutation(
                DeleteMediaListEntryMutation(id = Optional.present(entryId))
            ).execute()

            if (response.data?.DeleteMediaListEntry?.deleted == true && !response.hasErrors()) {
                // Remove from library cache so LibraryScreen reflects the deletion immediately
                libraryDao.deleteByMediaId(mediaId)
                Result.Success(Unit)
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Delete failed"
                Result.Error(errorMessage)
            }
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    private fun String.stripHtml(): String {
        return this.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
    }
    override suspend fun getCharacterDetails(id: Int): Result<CharacterDetails> {
        return try {
            val response = apolloClient.query(
                GetCharacterDetailsQuery(id = id)
            ).execute()

            val charData = response.data?.Character
                ?: return Result.Error("Character not found")

            val mediaList = charData.media?.edges?.filterNotNull()?.mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                val voiceActorNode = edge.voiceActors?.firstOrNull() // Get first voice actor
                
                CharacterMedia(
                    id = node.id ?: 0,
                    title = node.title?.userPreferred ?: "Unknown",
                    coverUrl = node.coverImage?.large,
                    type = node.type,
                    voiceActor = voiceActorNode?.let { va -> 
                        VoiceActor(
                           id = va.id ?: 0,
                           name = va.name?.full ?: "Unknown",
                           imageUrl = va.image?.medium
                        ) 
                    }
                )
            } ?: emptyList()

            val character = CharacterDetails(
                id = charData.id ?: 0,
                name = charData.name?.full ?: "Unknown",
                nativeName = charData.name?.native,
                imageUrl = charData.image?.large,
                description = charData.description?.stripHtml(),
                gender = charData.gender,
                age = charData.age,
                bloodType = charData.bloodType,
                dateOfBirth = charData.dateOfBirth?.let { dob ->
                   if (dob.month != null && dob.day != null) {
                       "${dob.month}/${dob.day}" + (if (dob.year != null) "/${dob.year}" else "") 
                   } else null
                },
                favourites = charData.favourites,
                media = mediaList
            )

            Result.Success(character)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }
}
