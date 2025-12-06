package com.anisync.android.data

import com.anisync.android.GetMediaDetailsQuery
import com.anisync.android.SaveMediaListEntryMutation
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.type.MediaListStatus
import com.anisync.android.util.stripHtml
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import javax.inject.Inject

class DetailsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : DetailsRepository {

    override suspend fun getMediaDetails(id: Int): MediaDetails? {
        val response = apolloClient.query(
            GetMediaDetailsQuery(id = Optional.present(id))
        ).execute()

        val media = response.data?.Media ?: return null
        val listEntry = media.mediaListEntry

        // Map API status to domain status
        val listStatus = when (listEntry?.status) {
            com.anisync.android.type.MediaListStatus.CURRENT -> LibraryStatus.CURRENT
            com.anisync.android.type.MediaListStatus.PLANNING -> LibraryStatus.PLANNING
            com.anisync.android.type.MediaListStatus.COMPLETED -> LibraryStatus.COMPLETED
            com.anisync.android.type.MediaListStatus.DROPPED -> LibraryStatus.DROPPED
            com.anisync.android.type.MediaListStatus.PAUSED -> LibraryStatus.PAUSED
            com.anisync.android.type.MediaListStatus.REPEATING -> LibraryStatus.REPEATING
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

        return MediaDetails(
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
    }

    override suspend fun updateMediaListEntry(mediaId: Int, status: LibraryStatus, progress: Int): Boolean {
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

        return response.data?.SaveMediaListEntry != null && !response.hasErrors()
    }

    override suspend fun deleteMediaListEntry(listEntryId: Int): Boolean {
         val response = apolloClient.mutation(
            com.anisync.android.DeleteMediaListEntryMutation(
                id = Optional.present(listEntryId)
            )
        ).execute()

        return response.data?.DeleteMediaListEntry?.deleted == true && !response.hasErrors()
    }
}
