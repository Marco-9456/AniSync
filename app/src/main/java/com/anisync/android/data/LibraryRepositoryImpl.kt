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
import com.anisync.android.data.mapper.todayUtcMillis
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Result
import com.anisync.android.data.account.AccountStore
import com.anisync.android.util.AniListTextEncoder.encodeForAniList
import com.anisync.android.type.MediaListStatus
import com.anisync.android.type.MediaType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Owner id that matches no rows — used when there is no active account. */
private const val NO_OWNER = -1

private val SCORE_FORMAT_BY_NAME: Map<String, com.anisync.android.domain.ScoreFormat> = mapOf(
    "POINT_100" to com.anisync.android.domain.ScoreFormat.POINT_100,
    "POINT_10_DECIMAL" to com.anisync.android.domain.ScoreFormat.POINT_10_DECIMAL,
    "POINT_10" to com.anisync.android.domain.ScoreFormat.POINT_10,
    "POINT_5" to com.anisync.android.domain.ScoreFormat.POINT_5,
    "POINT_3" to com.anisync.android.domain.ScoreFormat.POINT_3,
)

internal fun mapScoreFormat(name: String?): com.anisync.android.domain.ScoreFormat =
    name?.let { SCORE_FORMAT_BY_NAME[it] } ?: com.anisync.android.domain.ScoreFormat.POINT_100

class LibraryRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val libraryDao: LibraryDao,
    private val appSettings: AppSettings,
    private val accountStore: AccountStore
) : LibraryRepository {

    private fun currentOwnerId(): Int = accountStore.activeAccount.value?.id ?: NO_OWNER

    /**
     * Observe the active account's library from Room (SSOT). Re-subscribes when the active account
     * changes, so switching accounts immediately shows the new account's cached entries.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLibrary(username: String, type: MediaType): Flow<List<LibraryEntry>> {
        return accountStore.activeAccount
            .flatMapLatest { account ->
                libraryDao.observeByType(account?.id ?: NO_OWNER, type)
            }
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Fetch from network and update local cache.
     * The Flow from getLibrary() will emit automatically.
     */
    override suspend fun refreshLibrary(username: String, type: MediaType): Result<Unit> {
        return safeApiCall {
            var resolvedScoreFormat: com.anisync.android.type.ScoreFormat? = null
            val actualUsername = if (username.isBlank()) {
                val viewerResponse = apolloClient.query(GetViewerQuery()).execute()
                resolvedScoreFormat = viewerResponse.data?.Viewer?.mediaListOptions?.scoreFormat
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
            val options = response.data?.MediaListCollection?.user?.mediaListOptions

            val scoreFormatApi = resolvedScoreFormat
                ?: apolloClient.query(GetViewerQuery())
                    .fetchPolicy(FetchPolicy.CacheFirst)
                    .execute()
                    .data?.Viewer?.mediaListOptions?.scoreFormat
            if (scoreFormatApi != null) {
                appSettings.setUserScoreFormat(mapScoreFormat(scoreFormatApi.name))
            }

            val animeCustomLists = options?.animeList?.customLists?.filterNotNull() ?: emptyList()
            val mangaCustomLists = options?.mangaList?.customLists?.filterNotNull() ?: emptyList()

            val apiAnimeSet = animeCustomLists.toHashSet()
            val apiMangaSet = mangaCustomLists.toHashSet()

            val currentAnimeOrder = appSettings.animeListOrder.value
            val currentAnimeOrderSet = currentAnimeOrder.toHashSet()
            val syncedAnime = currentAnimeOrder.filter { it.startsWith("status:") || it in apiAnimeSet } +
                    animeCustomLists.filter { it !in currentAnimeOrderSet }
            if (syncedAnime != currentAnimeOrder) {
                appSettings.setAnimeListOrder(syncedAnime)
            }

            val currentMangaOrder = appSettings.mangaListOrder.value
            val currentMangaOrderSet = currentMangaOrder.toHashSet()
            val syncedManga = currentMangaOrder.filter { it.startsWith("status:") || it in apiMangaSet } +
                    mangaCustomLists.filter { it !in currentMangaOrderSet }
            if (syncedManga != currentMangaOrder) {
                appSettings.setMangaListOrder(syncedManga)
            }

            val apiCustomNamesForType = if (type == MediaType.ANIME) apiAnimeSet else apiMangaSet

            // Group by entry ID to handle duplicates from custom lists
            val entryMap = mutableMapOf<Int, LibraryEntry>()

            lists.filterNotNull().forEach { group ->
                val listName = group.name ?: return@forEach
                val isCustom = group.isCustomList == true ||
                        (group.isCustomList != false && listName in apiCustomNamesForType)

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
                            cover = com.anisync.android.domain.CoverImage.of(media?.coverImage?.medium, media?.coverImage?.large, media?.coverImage?.extraLarge),
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
                            customLists = if (isCustom) listOf(listName) else emptyList(),
                            isPrivate = entry.`private` ?: false,
                            hiddenFromStatusLists = entry.hiddenFromStatusLists ?: false
                        )
                    } else if (isCustom && !existing.customLists.contains(listName)) {
                        entryMap[entryId] = existing.copy(customLists = existing.customLists + listName)
                    }
                }
            }

            // Collapse any rows that resolve to the same media into one (AniList media merges, or a
            // duplicate written by an external sync tool, can produce two list entries for one media).
            // Keep the most recently updated, carrying over every custom-list membership, so the cache
            // holds a single row per media and the grid never gets two cards with the same key.
            val entries = entryMap.values
                .groupBy { it.mediaId }
                .map { (_, dups) ->
                    if (dups.size == 1) {
                        dups[0]
                    } else {
                        val keep = dups.maxByOrNull { it.updatedAt ?: 0L } ?: dups[0]
                        keep.copy(customLists = dups.flatMap { it.customLists }.distinct())
                    }
                }

            // Smart merge to preserve locally-added entries during API sync delay.
            // Tag rows with the active account so each account's library persists independently.
            val owner = currentOwnerId()
            libraryDao.smartMergeByType(owner, type, entries.map { it.toEntity(type).copy(ownerId = owner) })
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
        val entry = libraryDao.getEntry(currentOwnerId(), mediaId) ?: return Result.Error("Entry not found")
        val isCompleted = entry.status == LibraryStatus.COMPLETED
        val now = todayUtcMillis()

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
        val owner = currentOwnerId()
        val entry = libraryDao.getEntry(owner, mediaId) ?: return Result.Error("Entry not found")

        // Determine the total based on media type
        val total = if (entry.mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes

        // Check if this progress update completes the media
        val isCompleted = total != null && total > 0 && progress >= total
        val now = todayUtcMillis()

        if (isCompleted) {
            // Auto-set completedAt when finishing
            libraryDao.updateStatusProgressAndCompletedAt(
                ownerId = owner,
                mediaId = mediaId,
                status = LibraryStatus.COMPLETED,
                progress = progress,
                completedAt = now
            )
        } else {
            libraryDao.updateProgress(owner, mediaId, progress)
        }
        return Result.Success(Unit)
    }

    override suspend fun updateEntry(entry: LibraryEntry): Result<Unit> {
        val owner = currentOwnerId()
        // Get original entry to detect status changes
        val originalEntry = libraryDao.getEntry(owner, entry.mediaId)
        val now = todayUtcMillis()
        
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
        libraryDao.updateEntry(updatedEntry.toEntity(updatedEntry.type ?: MediaType.ANIME).copy(ownerId = owner))

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
                    notes = Optional.presentIfNotNull(updatedEntry.notes?.let(::encodeForAniList)),
                    startedAt = updatedEntry.startedAt?.let { Optional.present(it.toFuzzyDateInput()) } ?: Optional.absent(),
                    completedAt = updatedEntry.completedAt?.let { Optional.present(it.toFuzzyDateInput()) } ?: Optional.absent(),
                    customLists = Optional.present(updatedEntry.customLists),
                    `private` = Optional.present(updatedEntry.isPrivate),
                    hiddenFromStatusLists = Optional.present(updatedEntry.hiddenFromStatusLists)
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
        libraryDao.deleteByMediaId(currentOwnerId(), mediaId)

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
            
            // Get the full stored order (may contain status: entries)
            val currentOrder = if (isAnime) appSettings.animeListOrder.value else appSettings.mangaListOrder.value
            
            // Extract only custom list names for the API (filter out status: entries)
            val customOnlyList = currentOrder.filter { !it.startsWith("status:") }
            val apiList = if (customList !in customOnlyList) {
                customOnlyList + customList
            } else {
                customOnlyList
            }
            
            // Full order for local storage — append new list at end
            val fullOrder = if (customList !in currentOrder) {
                currentOrder + customList
            } else {
                currentOrder
            }
            
            val response = apolloClient.mutation(
                com.anisync.android.UpdateCustomListsMutation(
                    animeCustomLists = if (isAnime) Optional.present(apiList) else Optional.absent(),
                    mangaCustomLists = if (!isAnime) Optional.present(apiList) else Optional.absent()
                )
            ).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Create custom list failed"
                throw Exception(errorMessage)
            } else {
                // Update local so it doesn't blink out before a refresh
                if (isAnime) {
                    appSettings.setAnimeListOrder(fullOrder)
                } else {
                    appSettings.setMangaListOrder(fullOrder)
                }
            }
        }
    }

}
