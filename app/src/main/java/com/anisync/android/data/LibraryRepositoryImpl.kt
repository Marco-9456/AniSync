package com.anisync.android.data

import com.anisync.android.GetUserLibraryQuery
import com.anisync.android.GetViewerQuery
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Result
import com.anisync.android.type.MediaListStatus
import com.anisync.android.type.MediaType
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloException
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : LibraryRepository {

    override suspend fun getLibrary(username: String, type: MediaType): Result<List<LibraryEntry>> {
        return try {
            // If no username provided, try to get current authenticated user
            val actualUsername = if (username.isBlank()) {
                val viewerResponse = apolloClient.query(GetViewerQuery()).execute()
                viewerResponse.data?.Viewer?.name
                    ?: return Result.Error("Unable to get current user")
            } else {
                username
            }
            
            val response = apolloClient.query(GetUserLibraryQuery(username = actualUsername, type = type)).execute()
            
            if (response.hasErrors()) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error"
                return Result.Error(errorMessage)
            }

            val lists = response.data?.MediaListCollection?.lists ?: emptyList()

            // Flatten lists
            val entries = lists.filterNotNull().flatMap { group ->
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
                        totalChapters = media?.chapters,
                        totalVolumes = media?.volumes,
                        type = media?.type,
                        status = status,
                        nextAiringEpisode = media?.nextAiringEpisode?.episode,
                        timeUntilAiring = media?.nextAiringEpisode?.timeUntilAiring
                    )
                } ?: emptyList()
            }
            
            Result.Success(entries)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun updateProgress(mediaId: Int, progress: Int): Result<Unit> {
        return try {
            val response = apolloClient.mutation(
                com.anisync.android.SaveMediaListEntryMutation(
                    mediaId = com.apollographql.apollo3.api.Optional.present(mediaId),
                    progress = com.apollographql.apollo3.api.Optional.present(progress)
                )
            ).execute()

            if (response.data?.SaveMediaListEntry != null && !response.hasErrors()) {
                Result.Success(Unit)
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Failed to update progress"
                Result.Error(errorMessage)
            }
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }
}
