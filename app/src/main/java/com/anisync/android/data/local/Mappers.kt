package com.anisync.android.data.local

import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.data.local.entity.MediaDetailsEntity
import com.anisync.android.data.local.entity.UserProfileEntity
import com.anisync.android.domain.CoverImage
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
    cover = CoverImage.of(coverMedium, coverLarge, coverExtraLarge),
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
    mediaStartDate = mediaStartDate,
    customLists = customLists,
    isPrivate = isPrivate,
    hiddenFromStatusLists = hiddenFromStatusLists
)

fun LibraryEntry.toEntity(mediaType: MediaType): LibraryEntryEntity = LibraryEntryEntity(
    id = id,
    mediaId = mediaId,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    titleUserPreferred = titleUserPreferred,
    coverUrl = coverUrl,
    coverMedium = cover?.medium,
    coverLarge = cover?.large,
    coverExtraLarge = cover?.extraLarge,
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
    mediaStartDate = mediaStartDate,
    customLists = customLists,
    isPrivate = isPrivate,
    hiddenFromStatusLists = hiddenFromStatusLists
)

// --- MediaDetails ---

fun MediaDetailsEntity.toDomain(): MediaDetails = MediaDetails(
    id = id,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    titleUserPreferred = titleUserPreferred,
    coverUrl = coverUrl,
    cover = CoverImage.of(coverMedium, coverLarge, coverExtraLarge),
    coverColor = coverColor,
    bannerUrl = bannerUrl,
    description = description,
    score = score,
    meanScore = meanScore,
    popularity = popularity,
    favourites = favourites,
    episodes = episodes,
    nextAiringEpisode = nextAiringEpisode?.let { ep ->
        nextAiringEpisodeTime?.let { airingAt ->
            com.anisync.android.domain.NextAiringEpisode(
                episode = ep,
                airingAt = airingAt,
                timeUntilAiring = nextAiringTimeUntil ?: ((airingAt - System.currentTimeMillis() / 1000).toInt().coerceAtLeast(0))
            )
        }
    },
    chapters = chapters,
    volumes = volumes,
    type = mediaType,
    status = status,
    format = format,
    genres = genres,
    synonyms = synonyms,
    hashtags = hashtags,
    source = source,
    studio = studio?.let { name ->
        studioId?.let { id -> com.anisync.android.domain.StudioRef(id, name) }
    },
    studios = studios,
    producers = producers,
    year = year,
    startDate = startDate,
    endDate = endDate,
    season = season,
    seasonYear = seasonYear,
    duration = duration,
    tags = tags,
    trailer = trailer,
    listEntryId = listEntryId,
    listStatus = listStatus,
    listProgress = listProgress,
    listEntryPrivate = listEntryPrivate,
    listEntryHiddenFromStatusLists = listEntryHiddenFromStatusLists,
    characters = characters,
    staff = staff,
    relations = relations,
    externalLinks = externalLinks,
    recommendations = recommendations,
    reviews = reviews,
    isFavourite = isFavourite,
    isRecommendationBlocked = isRecommendationBlocked,
    isReviewBlocked = isReviewBlocked
)

fun MediaDetails.toEntity(): MediaDetailsEntity = MediaDetailsEntity(
    id = id,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    titleUserPreferred = titleUserPreferred,
    coverUrl = coverUrl,
    coverMedium = cover?.medium,
    coverLarge = cover?.large,
    coverExtraLarge = cover?.extraLarge,
    coverColor = coverColor,
    bannerUrl = bannerUrl,
    description = description,
    score = score,
    meanScore = meanScore,
    popularity = popularity,
    favourites = favourites,
    episodes = episodes,
    nextAiringEpisode = nextAiringEpisode?.episode,
    nextAiringEpisodeTime = nextAiringEpisode?.airingAt,
    nextAiringTimeUntil = nextAiringEpisode?.timeUntilAiring,
    chapters = chapters,
    volumes = volumes,
    mediaType = type,
    status = status,
    format = format,
    genres = genres,
    synonyms = synonyms,
    hashtags = hashtags,
    source = source,
    studio = studio?.name,
    studioId = studio?.id,
    studios = studios,
    producers = producers,
    year = year,
    startDate = startDate,
    endDate = endDate,
    season = season,
    seasonYear = seasonYear,
    duration = duration,
    tags = tags,
    trailer = trailer,
    listEntryId = listEntryId,
    listStatus = listStatus,
    listProgress = listProgress,
    listEntryPrivate = listEntryPrivate,
    listEntryHiddenFromStatusLists = listEntryHiddenFromStatusLists,
    characters = characters,
    staff = staff,
    relations = relations,
    externalLinks = externalLinks,
    recommendations = recommendations,
    reviews = reviews,
    isFavourite = isFavourite,
    isRecommendationBlocked = isRecommendationBlocked,
    isReviewBlocked = isReviewBlocked
)

// --- UserProfile ---

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    about = about,
    aboutMarkdown = aboutMarkdown,
    activeAt = activeAt,
    animeCount = animeCount,
    daysWatched = daysWatched,
    mangaCount = mangaCount,
    chaptersRead = chaptersRead,
    meanScore = meanScore,
    animeStatusCounts = animeStatusCounts,
    favoriteAnime = favoriteAnime,
    activities = activities,
    topGenres = topGenres,
    favoriteMangaOverview = favoriteMangaOverview,
    favoriteCharactersOverview = favoriteCharactersOverview,
    favoriteStaffOverview = favoriteStaffOverview,
    favoriteStudiosOverview = favoriteStudiosOverview,
    donatorTier = donatorTier,
    donatorBadge = donatorBadge,
    moderatorRoles = moderatorRoles,
    createdAt = createdAt
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    about = about,
    aboutMarkdown = aboutMarkdown,
    activeAt = activeAt,
    animeCount = animeCount,
    daysWatched = daysWatched,
    mangaCount = mangaCount,
    chaptersRead = chaptersRead,
    meanScore = meanScore,
    animeStatusCounts = animeStatusCounts,
    favoriteAnime = favoriteAnime,
    activities = activities,
    topGenres = topGenres,
    favoriteMangaOverview = favoriteMangaOverview,
    favoriteCharactersOverview = favoriteCharactersOverview,
    favoriteStaffOverview = favoriteStaffOverview,
    favoriteStudiosOverview = favoriteStudiosOverview,
    donatorTier = donatorTier,
    donatorBadge = donatorBadge,
    moderatorRoles = moderatorRoles,
    createdAt = createdAt
)
