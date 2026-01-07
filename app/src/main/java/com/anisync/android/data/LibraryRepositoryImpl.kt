package com.anisync.android.data

import com.anisync.android.GetUserLibraryQuery
import com.anisync.android.GetViewerQuery
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.toDomain
import com.anisync.android.data.local.toEntity
import com.anisync.android.data.mapper.mapFuzzyDateToLong
import com.anisync.android.data.mapper.toApiStatus
import com.anisync.android.data.mapper.toDomainStatus
import com.anisync.android.data.mapper.toFuzzyDateInput
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Result
import com.anisync.android.type.MediaListStatus
import com.anisync.android.type.MediaType
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class LibraryRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val libraryDao: LibraryDao
) : LibraryRepository {

    /**
     * Observe library from local Room database (SSOT).
     * UI updates automatically when cache changes.
     */
    override fun observeLibrary(username: String, type: MediaType): Flow<List<LibraryEntry>> {
        return libraryDao.observeByType(type)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Fetch from network and update local cache.
     * The Flow from getLibrary() will emit automatically.
     */
    override suspend fun refreshLibrary(username: String, type: MediaType): Result<Unit> {
        return safeApiCall {
            // Resolve username
            val actualUsername = if (username.isBlank()) {
                val viewerResponse = apolloClient.query(GetViewerQuery()).execute()
                viewerResponse.data?.Viewer?.name
                    ?: throw Exception("Unable to get current user")
            } else {
                username
            }
            
            val response = apolloClient.query(
                GetUserLibraryQuery(username = actualUsername, type = type)
            ).execute()
            
            if (response.hasErrors()) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error"
                throw Exception(errorMessage)
            }

            val lists = response.data?.MediaListCollection?.lists ?: emptyList()

            // Map to domain then to entity
            val entries = lists.filterNotNull().flatMap { group ->
                group.entries?.filterNotNull()?.map { entry ->
                    val media = entry.media
                    
                    val status = entry.status?.toDomainStatus() ?: LibraryStatus.UNKNOWN

                    LibraryEntry(
                        id = entry.id ?: 0,
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
                        timeUntilAiring = media?.nextAiringEpisode?.timeUntilAiring,
                        score = entry.score,
                        rewatches = entry.repeat ?: 0,
                        notes = entry.notes,
                        startedAt = entry.startedAt?.let { mapFuzzyDateToLong(it.year, it.month, it.day) },
                        completedAt = entry.completedAt?.let { mapFuzzyDateToLong(it.year, it.month, it.day) },
                        updatedAt = entry.updatedAt?.toLong()?.times(1000L),
                        createdAt = entry.createdAt?.toLong()?.times(1000L),
                        mediaStartDate = media?.startDate?.let { mapFuzzyDateToLong(it.year, it.month, it.day) }
                    )
                } ?: emptyList()
            }
            
            // Atomic update to prevent UI flicker
            libraryDao.replaceByType(type, entries.map { it.toEntity(type) })
        }
    }

    /**
     * Optimistic local update + network sync.
     * Automatically changes status to COMPLETED if progress reaches total.
     * Also sets completedAt date when completing.
     */
    override suspend fun updateProgress(mediaId: Int, progress: Int): Result<Unit> {
        // 1. Get current entry to check for completion
        val entry = libraryDao.getEntry(mediaId) ?: return Result.Error("Entry not found")
        
        // Determine the total based on media type
        val total = if (entry.mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
        
        // Check if this progress update completes the media
        val isCompleted = total != null && total > 0 && progress >= total
        val now = System.currentTimeMillis()
        
        // 2. Update local immediately (UI sees change via Flow)
        if (isCompleted) {
            // Auto-set completedAt when finishing
            libraryDao.updateStatusProgressAndCompletedAt(
                mediaId = mediaId,
                status = LibraryStatus.COMPLETED,
                progress = progress,
                completedAt = now
            )
        } else {
            libraryDao.updateProgress(mediaId, progress)
        }

        // 3. Try sync to network
        return try {
            val response = apolloClient.mutation(
                com.anisync.android.SaveMediaListEntryMutation(
                    mediaId = Optional.present(mediaId),
                    progress = Optional.present(progress),
                    status = if (isCompleted) Optional.present(MediaListStatus.COMPLETED) else Optional.absent(),
                    completedAt = if (isCompleted) Optional.present(now.toFuzzyDateInput()) else Optional.absent()
                )
            ).execute()

            if (response.data?.SaveMediaListEntry != null && !response.hasErrors()) {
                Result.Success(Unit)
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Sync failed"
                Result.Error(errorMessage)
            }
        } catch (e: ApolloException) {
            // Local updated, network failed
            Result.Error("Offline: Saved locally", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun updateEntry(entry: LibraryEntry): Result<Unit> {
        // Get original entry to detect status changes
        val originalEntry = libraryDao.getEntry(entry.mediaId)
        val now = System.currentTimeMillis()
        
        // Auto-fill dates based on status changes
        var updatedEntry = entry
        
        // If changing to CURRENT (Watching/Reading) and startedAt is not set, auto-fill it
        if (entry.status == LibraryStatus.CURRENT && 
            originalEntry?.status != LibraryStatus.CURRENT &&
            entry.startedAt == null) {
            updatedEntry = updatedEntry.copy(startedAt = now)
        }
        
        // If changing to COMPLETED and completedAt is not set, auto-fill it
        if (entry.status == LibraryStatus.COMPLETED && 
            originalEntry?.status != LibraryStatus.COMPLETED &&
            entry.completedAt == null) {
            updatedEntry = updatedEntry.copy(completedAt = now)
        }
        
        // 1. Update local DB
        // We assume media type is present or default to ANIME logic for entity mapping
        libraryDao.updateEntry(updatedEntry.toEntity(updatedEntry.type ?: MediaType.ANIME))

        // 2. Sync to network
        return safeApiCall {
            val apiStatus = updatedEntry.status.toApiStatus()

            val response = apolloClient.mutation(
                com.anisync.android.SaveMediaListEntryMutation(
                    mediaId = Optional.present(updatedEntry.mediaId),
                    status = Optional.present(apiStatus),
                    progress = Optional.present(updatedEntry.progress),
                    score = Optional.presentIfNotNull(updatedEntry.score),
                    repeat = Optional.present(updatedEntry.rewatches),
                    notes = Optional.presentIfNotNull(updatedEntry.notes),
                    startedAt = updatedEntry.startedAt?.let { Optional.present(it.toFuzzyDateInput()) } ?: Optional.absent(),
                    completedAt = updatedEntry.completedAt?.let { Optional.present(it.toFuzzyDateInput()) } ?: Optional.absent()
                )
            ).execute()

            if (response.data?.SaveMediaListEntry != null && !response.hasErrors()) {
                // Success
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Sync failed"
                throw Exception(errorMessage)
            }
        }
    }

    override suspend fun deleteEntry(entryId: Int, mediaId: Int): Result<Unit> {
        // 1. Delete from local DB immediately (optimistic)
        libraryDao.deleteByMediaId(mediaId)

        // 2. Delete from network
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.DeleteMediaListEntryMutation(
                    id = Optional.present(entryId)
                )
            ).execute()

            if (response.data?.DeleteMediaListEntry?.deleted == true && !response.hasErrors()) {
                // Success
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Delete failed"
                throw Exception(errorMessage)
            }
        }
    }

}
