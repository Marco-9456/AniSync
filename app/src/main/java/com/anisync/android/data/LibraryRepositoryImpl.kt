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
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class LibraryRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val libraryDao: LibraryDao,
    private val appSettings: AppSettings
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
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            
            if (response.hasErrors()) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error"
                throw Exception(errorMessage)
            }

            val lists = response.data?.MediaListCollection?.lists ?: emptyList()
            
            // Extract all defined custom list names to ensure empty lists are also tracked
            val options = response.data?.MediaListCollection?.user?.mediaListOptions
            val animeCustomLists = options?.animeList?.customLists?.filterNotNull() ?: emptyList()
            val mangaCustomLists = options?.mangaList?.customLists?.filterNotNull() ?: emptyList()

            // Update global settings if the network data has changed to persist empty custom lists
            if (animeCustomLists.isNotEmpty()) {
                val currentOrder = appSettings.animeListOrder.value
                val newLists = animeCustomLists.filter { it !in currentOrder }
                if (newLists.isNotEmpty()) {
                    appSettings.setAnimeListOrder(currentOrder + newLists)
                }
            }
            if (mangaCustomLists.isNotEmpty()) {
                val currentOrder = appSettings.mangaListOrder.value
                val newLists = mangaCustomLists.filter { it !in currentOrder }
                if (newLists.isNotEmpty()) {
                    appSettings.setMangaListOrder(currentOrder + newLists)
                }
            }

            // Group by entry ID to handle duplicates from custom lists
            val entryMap = mutableMapOf<Int, LibraryEntry>()
            val standardListNames = setOf("Watching", "Reading", "Completed", "Dropped", "Paused", "Planning", "Repeating")

            lists.filterNotNull().forEach { group ->
                val listName = group.name ?: return@forEach
                val isCustom = listName !in standardListNames

                group.entries?.filterNotNull()?.forEach { entry ->
                    val entryId = entry.id ?: return@forEach
                    val media = entry.media
                    val existing = entryMap[entryId]

                    if (existing == null) {
                        val status = entry.status?.toDomainStatus() ?: LibraryStatus.UNKNOWN
                        entryMap[entryId] = LibraryEntry(
                            id = entryId,
                            mediaId = media?.id ?: 0,
                            titleRomaji = media?.title?.romaji,
                            titleEnglish = media?.title?.english,
                            titleNative = media?.title?.native,
                            titleUserPreferred = media?.title?.userPreferred ?: "Unknown Title",
                            coverUrl = media?.coverImage?.extraLarge,
                            progress = entry.progress ?: 0,
                            totalEpisodes = media?.episodes,
                            totalChapters = media?.chapters,
                            totalVolumes = media?.volumes,
                            type = media?.type,
                            status = status,
                            nextAiringEpisode = media?.nextAiringEpisode?.episode,
                            timeUntilAiring = media?.nextAiringEpisode?.timeUntilAiring,
                            mediaStatus = media?.status?.name,
                            nextAiringEpisodeTime = media?.nextAiringEpisode?.airingAt?.toLong(),
                            score = entry.score,
                            rewatches = entry.repeat ?: 0,
                            notes = entry.notes,
                            startedAt = entry.startedAt?.let { mapFuzzyDateToLong(it.year, it.month, it.day) },
                            completedAt = entry.completedAt?.let { mapFuzzyDateToLong(it.year, it.month, it.day) },
                            updatedAt = entry.updatedAt?.toLong()?.times(1000L),
                            createdAt = entry.createdAt?.toLong()?.times(1000L),
                            mediaStartDate = media?.startDate?.let { mapFuzzyDateToLong(it.year, it.month, it.day) },
                            customLists = if (isCustom) listOf(listName) else emptyList()
                        )
                    } else if (isCustom && !existing.customLists.contains(listName)) {
                        entryMap[entryId] = existing.copy(customLists = existing.customLists + listName)
                    }
                }
            }

            val entries = entryMap.values.toList()
            
            // Smart merge to preserve locally-added entries during API sync delay
            libraryDao.smartMergeByType(type, entries.map { it.toEntity(type) })
        }
    }

    /**
     * Optimistic local update + network sync.
     * Automatically changes status to COMPLETED if progress reaches total.
     * Also sets completedAt date when completing.
     */
    /**
     * Optimistic local update + network sync.
     * Automatically changes status to COMPLETED if progress reaches total.
     * Also sets completedAt date when completing.
     */
    override suspend fun updateProgress(mediaId: Int, progress: Int): Result<Unit> {
        // 1. Update local
        val localResult = updateProgressLocal(mediaId, progress)
        if (localResult is Result.Error) return localResult

        // 2. Need to recalculate completion status for Sync parameters
        // (Refetching entry or duplicating logic - refetching is safer)
        val entry = libraryDao.getEntry(mediaId) ?: return Result.Error("Entry not found")
        val isCompleted = entry.status == LibraryStatus.COMPLETED
        val now = System.currentTimeMillis()

        // 3. Try sync to network
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.SaveMediaListEntryMutation(
                    mediaId = Optional.present(mediaId),
                    progress = Optional.present(progress),
                    status = if (isCompleted) Optional.present(MediaListStatus.COMPLETED) else Optional.absent(),
                    completedAt = if (isCompleted) Optional.present(now.toFuzzyDateInput()) else Optional.absent()
                )
            ).execute()

            if (response.data?.SaveMediaListEntry == null || response.hasErrors()) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Sync failed"
                throw Exception(errorMessage)
            }
        }
    }

    override suspend fun updateProgressLocal(mediaId: Int, progress: Int): Result<Unit> {
        val entry = libraryDao.getEntry(mediaId) ?: return Result.Error("Entry not found")
        
        // Determine the total based on media type
        val total = if (entry.mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
        
        // Check if this progress update completes the media
        val isCompleted = total != null && total > 0 && progress >= total
        val now = System.currentTimeMillis()
        
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
        return Result.Success(Unit)
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
                    completedAt = updatedEntry.completedAt?.let { Optional.present(it.toFuzzyDateInput()) } ?: Optional.absent(),
                    customLists = Optional.present(updatedEntry.customLists)
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

    override suspend fun deleteCustomList(customList: String, type: com.anisync.android.type.MediaType): com.anisync.android.domain.Result<Unit> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.DeleteCustomListMutation(
                    customList = customList,
                    type = type
                )
            ).execute()

            if (response.data?.DeleteCustomList?.deleted == true && !response.hasErrors()) {
                // Success - trigger refresh to update local data
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Delete custom list failed"
                throw Exception(errorMessage)
            }
        }
    }

    override suspend fun createCustomList(customList: String, type: com.anisync.android.type.MediaType): com.anisync.android.domain.Result<Unit> {
        return safeApiCall {
            val isAnime = type == com.anisync.android.type.MediaType.ANIME
            
            // First we need to get the user's current lists so we don't overwrite them
            val currentOrder = if (isAnime) appSettings.animeListOrder.value else appSettings.mangaListOrder.value
            
            // Make sure the new list is included
            val newList = if (customList !in currentOrder) {
                currentOrder + customList
            } else {
                currentOrder
            }
            
            val response = apolloClient.mutation(
                com.anisync.android.UpdateCustomListsMutation(
                    animeCustomLists = if (isAnime) Optional.present(newList) else Optional.absent(),
                    mangaCustomLists = if (!isAnime) Optional.present(newList) else Optional.absent()
                )
            ).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Create custom list failed"
                throw Exception(errorMessage)
            } else {
                // Update local so it doesn't blink out before a refresh
                if (isAnime) {
                    appSettings.setAnimeListOrder(newList)
                } else {
                    appSettings.setMangaListOrder(newList)
                }
            }
        }
    }

}
