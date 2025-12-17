package com.anisync.android.data.local

import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.data.local.entity.MediaDetailsEntity
import com.anisync.android.data.local.entity.UserProfileEntity
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.UserProfile
import com.anisync.android.type.MediaType

/**
 * Extension functions to convert between Room entities and domain models.
 */

// --- LibraryEntry ---

fun LibraryEntryEntity.toDomain(): LibraryEntry = LibraryEntry(
    id = id,
    mediaId = mediaId,
    title = title,
    coverUrl = coverUrl,
    progress = progress,
    totalEpisodes = totalEpisodes,
    totalChapters = totalChapters,
    totalVolumes = totalVolumes,
    type = mediaType,
    status = status,
    nextAiringEpisode = nextAiringEpisode,
    timeUntilAiring = timeUntilAiring,
    mediaStatus = mediaStatus,
    score = score,
    rewatches = rewatches,
    notes = notes,
    startedAt = startedAt,
    completedAt = completedAt
)

fun LibraryEntry.toEntity(mediaType: MediaType): LibraryEntryEntity = LibraryEntryEntity(
    id = id,
    mediaId = mediaId,
    title = title,
    coverUrl = coverUrl,
    progress = progress,
    totalEpisodes = totalEpisodes,
    totalChapters = totalChapters,
    totalVolumes = totalVolumes,
    mediaType = mediaType,
    status = status,
    nextAiringEpisode = nextAiringEpisode,
    timeUntilAiring = timeUntilAiring,
    mediaStatus = mediaStatus,
    score = score,
    rewatches = rewatches,
    notes = notes,
    startedAt = startedAt,
    completedAt = completedAt
)

// --- MediaDetails ---

fun MediaDetailsEntity.toDomain(): MediaDetails = MediaDetails(
    id = id,
    title = title,
    coverUrl = coverUrl,
    bannerUrl = bannerUrl,
    description = description,
    score = score,
    episodes = episodes,
    chapters = chapters,
    volumes = volumes,
    type = mediaType,
    status = status,
    format = format,
    genres = genres,
    studio = studio,
    year = year,
    listEntryId = listEntryId,
    listStatus = listStatus,
    listProgress = listProgress,
    characters = characters,
    relations = relations
)

fun MediaDetails.toEntity(): MediaDetailsEntity = MediaDetailsEntity(
    id = id,
    title = title,
    coverUrl = coverUrl,
    bannerUrl = bannerUrl,
    description = description,
    score = score,
    episodes = episodes,
    chapters = chapters,
    volumes = volumes,
    mediaType = type,
    status = status,
    format = format,
    genres = genres,
    studio = studio,
    year = year,
    listEntryId = listEntryId,
    listStatus = listStatus,
    listProgress = listProgress,
    characters = characters,
    relations = relations
)

// --- UserProfile ---

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    animeCount = animeCount,
    daysWatched = daysWatched,
    mangaCount = mangaCount,
    chaptersRead = chaptersRead,
    meanScore = meanScore,
    favoriteAnime = favoriteAnime
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    animeCount = animeCount,
    daysWatched = daysWatched,
    mangaCount = mangaCount,
    chaptersRead = chaptersRead,
    meanScore = meanScore,
    favoriteAnime = favoriteAnime
)
