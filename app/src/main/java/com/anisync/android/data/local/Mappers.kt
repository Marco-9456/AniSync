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
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    titleUserPreferred = titleUserPreferred,
    coverUrl = coverUrl,
    progress = progress,
    totalEpisodes = totalEpisodes,
    totalChapters = totalChapters,
    totalVolumes = totalVolumes,
    type = mediaType,
    status = status,
    nextAiringEpisode = nextAiringEpisode,
    timeUntilAiring = timeUntilAiring,
    nextAiringEpisodeTime = nextAiringEpisodeTime,
    mediaStatus = mediaStatus,
    score = score,
    rewatches = rewatches,
    notes = notes,
    startedAt = startedAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
    createdAt = createdAt,
    mediaStartDate = mediaStartDate
)

fun LibraryEntry.toEntity(mediaType: MediaType): LibraryEntryEntity = LibraryEntryEntity(
    id = id,
    mediaId = mediaId,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    titleUserPreferred = titleUserPreferred,
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
    // Use domain value if present, otherwise calculate from relative time
    nextAiringEpisodeTime = nextAiringEpisodeTime
        ?: (if (timeUntilAiring != null) (System.currentTimeMillis() / 1000) + timeUntilAiring else null),
    score = score,
    rewatches = rewatches,
    notes = notes,
    startedAt = startedAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
    createdAt = createdAt,
    mediaStartDate = mediaStartDate
)

// --- MediaDetails ---

fun MediaDetailsEntity.toDomain(): MediaDetails = MediaDetails(
    id = id,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    titleUserPreferred = titleUserPreferred,
    coverUrl = coverUrl,
    bannerUrl = bannerUrl,
    description = description,
    score = score,
    episodes = episodes,
    nextAiringEpisode = nextAiringEpisode,
    chapters = chapters,
    volumes = volumes,
    type = mediaType,
    status = status,
    format = format,
    genres = genres,
    studio = studio,
    year = year,
    startDate = startDate,
    season = season,
    seasonYear = seasonYear,
    listEntryId = listEntryId,
    listStatus = listStatus,
    listProgress = listProgress,
    characters = characters,
    relations = relations,
    externalLinks = externalLinks,
    isFavourite = isFavourite
)

fun MediaDetails.toEntity(): MediaDetailsEntity = MediaDetailsEntity(
    id = id,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    titleUserPreferred = titleUserPreferred,
    coverUrl = coverUrl,
    bannerUrl = bannerUrl,
    description = description,
    score = score,
    episodes = episodes,
    nextAiringEpisode = nextAiringEpisode,
    chapters = chapters,
    volumes = volumes,
    mediaType = type,
    status = status,
    format = format,
    genres = genres,
    studio = studio,
    year = year,
    startDate = startDate,
    season = season,
    seasonYear = seasonYear,
    listEntryId = listEntryId,
    listStatus = listStatus,
    listProgress = listProgress,
    characters = characters,
    relations = relations,
    externalLinks = externalLinks,
    isFavourite = isFavourite
)

// --- UserProfile ---

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    about = about,
    activeAt = activeAt,
    animeCount = animeCount,
    daysWatched = daysWatched,
    mangaCount = mangaCount,
    chaptersRead = chaptersRead,
    meanScore = meanScore,
    animeStatusCounts = animeStatusCounts,
    favoriteAnime = favoriteAnime,
    activities = activities
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    about = about,
    activeAt = activeAt,
    animeCount = animeCount,
    daysWatched = daysWatched,
    mangaCount = mangaCount,
    chaptersRead = chaptersRead,
    meanScore = meanScore,
    animeStatusCounts = animeStatusCounts,
    favoriteAnime = favoriteAnime,
    activities = activities
)
