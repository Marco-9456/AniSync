package com.anisync.android.data

import com.anisync.android.DeleteMediaListEntryMutation
import com.anisync.android.GetCharacterDetailsQuery
import com.anisync.android.GetMediaDetailsQuery
import com.anisync.android.GetStaffDetailsQuery
import com.anisync.android.SaveMediaListEntryMutation
import com.anisync.android.ToggleFavouriteMutation
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.data.local.toDomain
import com.anisync.android.data.local.toEntity
import com.anisync.android.data.mapper.toApiStatus
import com.anisync.android.data.mapper.toDomainStatus
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.domain.CharacterMediaAppearance
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.ExternalLink
import com.anisync.android.domain.ExternalLinkType
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.RecommendedMedia
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.domain.Result
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.Tag
import com.anisync.android.domain.Trailer
import com.anisync.android.domain.VoiceActor
import com.anisync.android.domain.VoicedCharacter
import com.anisync.android.type.MediaType
import com.anisync.android.util.stripHtml
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DetailsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val mediaDetailsDao: MediaDetailsDao,
    private val libraryDao: LibraryDao
) : DetailsRepository {

    override fun observeMediaDetails(id: Int): Flow<MediaDetails?> {
        return mediaDetailsDao.observeById(id)
            .map { entity -> entity?.toDomain() }
    }

    override suspend fun refreshMediaDetails(id: Int): Result<Unit> {
        return safeApiCall {
            val response = apolloClient.query(
                GetMediaDetailsQuery(id = Optional.present(id))
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val media = response.data?.Media ?: throw Exception("Media not found")

            val listEntry = media.mediaListEntry
            val listStatus = listEntry?.status?.toDomainStatus()

            val characters = media.characters?.edges?.filterNotNull()?.map { edge ->
                CharacterInfo(
                    id = edge.node?.id ?: 0,
                    nameFull = edge.node?.name?.full ?: "Unknown",
                    nameNative = edge.node?.name?.native,
                    nameUserPreferred = edge.node?.name?.userPreferred ?: "Unknown",
                    imageUrl = edge.node?.image?.large,
                    role = edge.role?.name ?: "UNKNOWN"
                )
            } ?: emptyList()

            val relations = media.relations?.edges?.filterNotNull()?.map { edge ->
                val node = edge.node
                RelatedMedia(
                    id = node?.id ?: 0,
                    titleRomaji = node?.title?.romaji,
                    titleEnglish = node?.title?.english,
                    titleNative = node?.title?.native,
                    titleUserPreferred = node?.title?.userPreferred ?: "Unknown",
                    coverUrl = node?.coverImage?.large,
                    format = node?.format?.name,
                    status = node?.status?.name,
                    relationType = edge.relationType?.name ?: "UNKNOWN"
                )
            } ?: emptyList()

            val externalLinks = media.externalLinks?.filterNotNull()
                ?.filter { it.isDisabled != true }
                ?.map { link ->
                    ExternalLink(
                        id = link.id,
                        url = link.url,
                        site = link.site,
                        type = when (link.type?.name) {
                            "STREAMING" -> ExternalLinkType.STREAMING
                            "SOCIAL" -> ExternalLinkType.SOCIAL
                            "INFO" -> ExternalLinkType.INFO
                            else -> null
                        },
                        color = link.color,
                        icon = link.icon,
                        language = link.language,
                        notes = link.notes
                    )
                } ?: emptyList()

            val titleRomaji = media.title?.romaji
            val titleEnglish = media.title?.english
            val titleNative = media.title?.native
            val titleUserPreferred = media.title?.userPreferred ?: "Unknown"

            val months = listOf(
                "Jan",
                "Feb",
                "Mar",
                "Apr",
                "May",
                "Jun",
                "Jul",
                "Aug",
                "Sep",
                "Oct",
                "Nov",
                "Dec"
            )
            val monthName =
                media.startDate?.month?.let { if (it in 1..12) months[it - 1] else null }
            val day = media.startDate?.day?.let { if (it < 10) "0$it" else "$it" }
            val yearVal = media.startDate?.year

            val formattedDate = if (monthName != null && day != null && yearVal != null) {
                "$monthName $day, $yearVal"
            } else {
                yearVal?.toString()
            }

            val endMonthName =
                media.endDate?.month?.let { if (it in 1..12) months[it - 1] else null }
            val endDay = media.endDate?.day?.let { if (it < 10) "0$it" else "$it" }
            val endYearVal = media.endDate?.year

            val formattedEndDate =
                if (endMonthName != null && endDay != null && endYearVal != null) {
                    "$endMonthName $endDay, $endYearVal"
                } else {
                    endYearVal?.toString()
                }

            val tags = media.tags?.filterNotNull()?.map { tag ->
                Tag(
                    name = tag.name ?: "",
                    category = tag.category ?: "",
                    description = tag.description,
                    isMediaSpoiler = tag.isMediaSpoiler ?: false,
                    isGeneralSpoiler = tag.isGeneralSpoiler ?: false,
                    rank = tag.rank
                )
            } ?: emptyList()

            val trailer = media.trailer?.let { trailer ->
                if (trailer.id != null && trailer.site != null) {
                    Trailer(
                        id = trailer.id,
                        site = trailer.site,
                        thumbnail = trailer.thumbnail
                    )
                } else null
            }

            val recommendations =
                media.recommendations?.nodes?.filterNotNull()?.mapNotNull { node ->
                    val rec = node.mediaRecommendation ?: return@mapNotNull null
                    RecommendedMedia(
                        id = rec.id,
                        titleRomaji = rec.title?.romaji,
                        titleEnglish = rec.title?.english,
                        titleNative = rec.title?.native,
                        titleUserPreferred = rec.title?.userPreferred ?: "Unknown",
                        coverUrl = rec.coverImage?.large,
                        format = rec.format?.name,
                        score = rec.averageScore,
                        rating = node.rating ?: 0,
                        userRating = node.userRating?.name
                    )
                } ?: emptyList()

            val reviews = media.reviews?.nodes?.filterNotNull()?.map { node ->
                MediaReview(
                    id = node.id,
                    summary = node.summary ?: "",
                    body = node.body,
                    score = node.score ?: 0,
                    rating = node.rating ?: 0,
                    ratingAmount = node.ratingAmount ?: 0,
                    userRating = node.userRating?.name,
                    userName = node.user?.name ?: "Unknown",
                    userAvatarUrl = node.user?.avatar?.medium,
                    createdAt = (node.createdAt ?: 0).toLong()
                )
            } ?: emptyList()

            val details = MediaDetails(
                id = media.id ?: 0,
                titleRomaji = titleRomaji,
                titleEnglish = titleEnglish,
                titleNative = titleNative,
                titleUserPreferred = titleUserPreferred,
                coverUrl = media.coverImage?.extraLarge,
                bannerUrl = media.bannerImage,
                description = media.description?.stripHtml() ?: "",
                score = media.averageScore,
                episodes = media.episodes,
                nextAiringEpisode = media.nextAiringEpisode?.episode,
                chapters = media.chapters,
                volumes = media.volumes,
                type = media.type,
                status = media.status?.name ?: "UNKNOWN",
                format = media.format?.name,
                genres = media.genres?.filterNotNull() ?: emptyList(),
                source = media.source?.name,
                studio = media.studios?.nodes?.firstOrNull()?.name,
                year = media.startDate?.year,
                startDate = formattedDate,
                endDate = formattedEndDate,
                season = media.season?.name,
                seasonYear = media.startDate?.year,
                duration = media.duration,
                tags = tags,
                trailer = trailer,
                listEntryId = listEntry?.id,
                listStatus = listStatus,
                listProgress = listEntry?.progress,
                characters = characters,
                relations = relations,
                externalLinks = externalLinks,
                recommendations = recommendations,
                reviews = reviews,
                isFavourite = media.isFavourite ?: false
            )

            mediaDetailsDao.insert(details.toEntity())
        }
    }

    override suspend fun updateMediaListEntry(
        mediaId: Int,
        status: LibraryStatus,
        progress: Int
    ): Result<Unit> {
        return safeApiCall {
            val apiStatus = status.toApiStatus()

            val response = apolloClient.mutation(
                SaveMediaListEntryMutation(
                    mediaId = Optional.present(mediaId),
                    status = Optional.present(apiStatus),
                    progress = Optional.present(progress)
                )
            ).execute()

            if (response.data?.SaveMediaListEntry != null && !response.hasErrors()) {
                refreshMediaDetails(mediaId)
                val existingEntry = libraryDao.getEntry(mediaId)

                if (existingEntry != null) {
                    libraryDao.updateStatusAndProgress(mediaId, status, progress)
                } else {
                    val savedEntry = response.data?.SaveMediaListEntry
                    val cachedMedia = mediaDetailsDao.getById(mediaId)

                    if (cachedMedia != null) {
                        val newEntry = com.anisync.android.data.local.entity.LibraryEntryEntity(
                            id = savedEntry?.id ?: 0,
                            mediaId = mediaId,
                            titleRomaji = cachedMedia.titleRomaji,
                            titleEnglish = cachedMedia.titleEnglish,
                            titleNative = cachedMedia.titleNative,
                            titleUserPreferred = cachedMedia.titleUserPreferred,
                            coverUrl = cachedMedia.coverUrl,
                            progress = progress,
                            totalEpisodes = cachedMedia.episodes,
                            totalChapters = cachedMedia.chapters,
                            totalVolumes = cachedMedia.volumes,
                            mediaType = cachedMedia.mediaType,
                            status = status,
                            nextAiringEpisode = cachedMedia.nextAiringEpisode,
                            timeUntilAiring = null,
                            mediaStatus = cachedMedia.status,
                            nextAiringEpisodeTime = cachedMedia.nextAiringEpisodeTime,
                            score = 0.0,
                            rewatches = 0,
                            notes = null,
                            startedAt = if (status == LibraryStatus.CURRENT) System.currentTimeMillis() else null,
                            completedAt = null,
                            updatedAt = System.currentTimeMillis(),
                            createdAt = System.currentTimeMillis(),
                            mediaStartDate = null
                        )
                        libraryDao.insertOrReplace(newEntry)
                    }
                }
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Update failed"
                throw Exception(errorMessage)
            }
        }
    }

    override suspend fun deleteMediaListEntry(entryId: Int, mediaId: Int): Result<Unit> {
        return safeApiCall {
            val response = apolloClient.mutation(
                DeleteMediaListEntryMutation(id = Optional.present(entryId))
            ).execute()

            if (response.data?.DeleteMediaListEntry?.deleted == true && !response.hasErrors()) {
                libraryDao.deleteByMediaId(mediaId)
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Delete failed"
                throw Exception(errorMessage)
            }
        }
    }

    override suspend fun toggleFavourite(mediaId: Int, mediaType: MediaType): Result<Boolean> {
        return safeApiCall {
            val mutation = if (mediaType == MediaType.MANGA) {
                ToggleFavouriteMutation(
                    animeId = Optional.absent(),
                    mangaId = Optional.present(mediaId),
                    characterId = Optional.absent(),
                    staffId = Optional.absent()
                )
            } else {
                ToggleFavouriteMutation(
                    animeId = Optional.present(mediaId),
                    mangaId = Optional.absent(),
                    characterId = Optional.absent(),
                    staffId = Optional.absent()
                )
            }

            val response = apolloClient.mutation(mutation).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.firstOrNull()?.message ?: "Toggle favourite failed"
                throw Exception(errorMessage)
            }

            refreshMediaDetails(mediaId)
            val currentEntity = mediaDetailsDao.getById(mediaId)
            currentEntity?.isFavourite ?: false
        }
    }

    override suspend fun toggleCharacterFavourite(characterId: Int): Result<Boolean> {
        return safeApiCall {
            val mutation = ToggleFavouriteMutation(
                animeId = Optional.absent(),
                mangaId = Optional.absent(),
                characterId = Optional.present(characterId),
                staffId = Optional.absent()
            )

            val response = apolloClient.mutation(mutation).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.firstOrNull()?.message ?: "Toggle favourite failed"
                throw Exception(errorMessage)
            }

            response.data?.ToggleFavourite?.characters?.nodes
                ?.any { it?.id == characterId } ?: false
        }
    }

    override suspend fun toggleStaffFavourite(staffId: Int): Result<Boolean> {
        return safeApiCall {
            val mutation = ToggleFavouriteMutation(
                animeId = Optional.absent(),
                mangaId = Optional.absent(),
                characterId = Optional.absent(),
                staffId = Optional.present(staffId)
            )

            val response = apolloClient.mutation(mutation).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.firstOrNull()?.message ?: "Toggle favourite failed"
                throw Exception(errorMessage)
            }

            response.data?.ToggleFavourite?.staff?.nodes
                ?.any { it?.id == staffId } ?: false
        }
    }

    override suspend fun getCharacterDetails(id: Int, page: Int): Result<CharacterDetails> {
        return safeApiCall {
            val response = apolloClient.query(
                GetCharacterDetailsQuery(id = id, page = Optional.presentIfNotNull(page))
            ).execute()

            val charData = response.data?.Character
                ?: throw Exception("Character not found")

            val alternativeNames = charData.name?.alternative?.filterNotNull() ?: emptyList()

            val pageInfo = charData.media?.pageInfo
            val hasNextPage = pageInfo?.hasNextPage ?: false

            val mediaList = charData.media?.edges?.filterNotNull()?.mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null

                val voiceActorsList = edge.voiceActors?.mapNotNull { va ->
                    if (va == null) return@mapNotNull null
                    VoiceActor(
                        id = va.id ?: 0,
                        nameFull = va.name?.full ?: "Unknown",
                        nameNative = va.name?.native,
                        nameUserPreferred = va.name?.userPreferred ?: va.name?.full ?: "Unknown",
                        imageUrl = va.image?.medium,
                        language = va.languageV2
                    )
                } ?: emptyList()

                CharacterMedia(
                    id = node.id ?: 0,
                    titleRomaji = node.title?.romaji,
                    titleEnglish = node.title?.english,
                    titleNative = node.title?.native,
                    titleUserPreferred = node.title?.userPreferred ?: "Unknown",
                    coverUrl = node.coverImage?.large,
                    bannerUrl = node.bannerImage,
                    type = node.type,
                    characterRole = edge.characterRole?.name,
                    startYear = node.startDate?.year,
                    popularity = node.popularity,
                    averageScore = node.averageScore,
                    favourites = node.favourites,
                    isOnList = node.mediaListEntry?.id != null,
                    voiceActors = voiceActorsList
                )
            } ?: emptyList()

            CharacterDetails(
                id = charData.id ?: 0,
                name = charData.name?.full ?: "Unknown",
                nativeName = charData.name?.native,
                alternativeNames = alternativeNames,
                imageUrl = charData.image?.large,
                description = charData.description,
                gender = charData.gender,
                age = charData.age,
                bloodType = charData.bloodType,
                dateOfBirth = charData.dateOfBirth?.let { dob ->
                    if (dob.month != null && dob.day != null) {
                        "${dob.month}/${dob.day}" + (if (dob.year != null) "/${dob.year}" else "")
                    } else null
                },
                favourites = charData.favourites,
                isFavourite = charData.isFavourite ?: false,
                media = mediaList,
                hasNextPage = hasNextPage
            )
        }
    }

    override suspend fun getMediaReviews(
        mediaId: Int,
        page: Int
    ): Result<Pair<List<MediaReview>, Boolean>> {
        return safeApiCall {
            val response = apolloClient.query(
                com.anisync.android.GetMediaReviewsQuery(
                    mediaId = mediaId,
                    page = page
                )
            ).execute()

            if (response.hasErrors() || response.data == null) {
                throw Exception(
                    response.errors?.firstOrNull()?.message ?: "Failed to fetch reviews"
                )
            }

            val pageData = response.data?.Page
            val hasNextPage = pageData?.pageInfo?.hasNextPage ?: false
            val nodes = pageData?.reviews?.filterNotNull() ?: emptyList()

            val mappedReviews = nodes.map { node ->
                MediaReview(
                    id = node.id,
                    summary = node.summary ?: "",
                    body = node.body,
                    score = node.score ?: 0,
                    rating = node.rating ?: 0,
                    ratingAmount = node.ratingAmount ?: 0,
                    userRating = node.userRating?.name,
                    userName = node.user?.name ?: "Unknown",
                    userAvatarUrl = node.user?.avatar?.medium,
                    createdAt = (node.createdAt ?: 0).toLong()
                )
            }

            Pair(mappedReviews, hasNextPage)
        }
    }

    override suspend fun rateReview(
        reviewId: Int,
        rating: com.anisync.android.type.ReviewRating
    ): Result<MediaReview> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.RateReviewMutation(
                    reviewId = Optional.present(reviewId),
                    rating = Optional.present(rating)
                )
            ).execute()

            if (response.hasErrors() || response.data == null) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to rate review")
            }

            val rateReviewData = response.data?.RateReview
                ?: throw Exception("Failed to rate review")

            MediaReview(
                id = rateReviewData.id,
                summary = "", // Empty as not used by the updater
                body = null,
                score = 0,
                rating = rateReviewData.rating ?: 0,
                ratingAmount = rateReviewData.ratingAmount ?: 0,
                userRating = rateReviewData.userRating?.name,
                userName = "",
                userAvatarUrl = null,
                createdAt = 0L
            )
        }
    }

    override suspend fun rateRecommendation(
        mediaId: Int,
        recommendationId: Int,
        rating: com.anisync.android.type.RecommendationRating
    ): Result<Pair<Int, String?>> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.SaveRecommendationMutation(
                    mediaId = Optional.present(mediaId),
                    mediaRecommendationId = Optional.present(recommendationId),
                    rating = Optional.present(rating)
                )
            ).execute()

            if (response.hasErrors() || response.data == null) {
                throw Exception(
                    response.errors?.firstOrNull()?.message ?: "Failed to rate recommendation"
                )
            }

            val data = response.data?.SaveRecommendation
                ?: throw Exception("Failed to rate recommendation")

            Pair(data.rating ?: 0, data.userRating?.name)
        }
    }

    override suspend fun getStaffDetails(id: Int, page: Int): Result<StaffDetails> {
        return safeApiCall {
            val response = apolloClient.query(
                GetStaffDetailsQuery(id = id, page = Optional.presentIfNotNull(page))
            ).execute()

            val staffData = response.data?.Staff
                ?: throw Exception("Staff not found")

            val alternativeNames = staffData.name?.alternative?.filterNotNull() ?: emptyList()

            val pageInfo = staffData.characterMedia?.pageInfo
            val hasNextPage = pageInfo?.hasNextPage ?: false

            val months = listOf(
                "Jan",
                "Feb",
                "Mar",
                "Apr",
                "May",
                "Jun",
                "Jul",
                "Aug",
                "Sep",
                "Oct",
                "Nov",
                "Dec"
            )

            fun formatFuzzyDate(month: Int?, day: Int?, year: Int?): String? {
                val monthName = month?.let { if (it in 1..12) months[it - 1] else null }
                return when {
                    monthName != null && day != null && year != null -> "$monthName $day, $year"
                    monthName != null && day != null -> "$monthName $day"
                    year != null -> "$year"
                    else -> null
                }
            }

            val characterMap =
                linkedMapOf<Int, MutableList<Pair<GetStaffDetailsQuery.Edge, GetStaffDetailsQuery.Character>>>()
            staffData.characterMedia?.edges?.filterNotNull()?.forEach { edge ->
                edge.characters?.filterNotNull()?.forEach { character ->
                    val charId = character.id ?: return@forEach
                    characterMap.getOrPut(charId) { mutableListOf() }.add(edge to character)
                }
            }

            val voicedCharacters = characterMap.map { (charId, entries) ->
                val firstChar = entries.first().second
                VoicedCharacter(
                    characterId = charId,
                    characterName = firstChar.name?.full ?: "Unknown",
                    characterImageUrl = firstChar.image?.medium,
                    mediaAppearances = entries.mapNotNull { (edge, _) ->
                        val node = edge.node ?: return@mapNotNull null
                        CharacterMediaAppearance(
                            mediaId = node.id ?: 0,
                            mediaTitle = node.title?.userPreferred ?: "Unknown",
                            coverUrl = node.coverImage?.large,
                            startYear = node.startDate?.year,
                            characterRole = edge.characterRole?.name,
                            popularity = node.popularity,
                            averageScore = node.averageScore,
                            favourites = node.favourites,
                            isOnList = node.mediaListEntry?.id != null
                        )
                    }
                )
            }

            StaffDetails(
                id = staffData.id,
                name = staffData.name?.full ?: "Unknown",
                nativeName = staffData.name?.native,
                alternativeNames = alternativeNames,
                imageUrl = staffData.image?.large,
                description = staffData.description,
                gender = staffData.gender,
                age = staffData.age,
                bloodType = staffData.bloodType,
                dateOfBirth = staffData.dateOfBirth?.let {
                    formatFuzzyDate(it.month, it.day, it.year)
                },
                dateOfDeath = staffData.dateOfDeath?.let {
                    formatFuzzyDate(it.month, it.day, it.year)
                },
                favourites = staffData.favourites,
                isFavourite = staffData.isFavourite ?: false,
                language = staffData.languageV2,
                primaryOccupations = staffData.primaryOccupations?.filterNotNull() ?: emptyList(),
                yearsActive = staffData.yearsActive?.filterNotNull() ?: emptyList(),
                homeTown = staffData.homeTown,
                voicedCharacters = voicedCharacters,
                hasNextPage = hasNextPage
            )
        }
    }
}