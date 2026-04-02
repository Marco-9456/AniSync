package com.anisync.android.data

import com.anisync.android.DeleteMediaListEntryMutation
import com.anisync.android.GetCharacterDetailsQuery
import com.anisync.android.GetMediaDetailsQuery
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
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.ExternalLink
import com.anisync.android.domain.ExternalLinkType
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.RecommendedMedia
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.domain.Result
import com.anisync.android.domain.Tag
import com.anisync.android.domain.Trailer
import com.anisync.android.domain.VoiceActor
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
        return safeApiCall {
            val response = apolloClient.query(
                GetMediaDetailsQuery(id = Optional.present(id))
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            val media = response.data?.Media
                ?: throw Exception("Media not found")

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

            // Map external links, filtering out disabled ones
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

            // Title variants
            val titleRomaji = media.title?.romaji
            val titleEnglish = media.title?.english
            val titleNative = media.title?.native
            val titleUserPreferred = media.title?.userPreferred ?: "Unknown"

            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = media.startDate?.month?.let { if (it in 1..12) months[it - 1] else null }
            val day = media.startDate?.day?.let { if (it < 10) "0$it" else "$it" }
            val yearVal = media.startDate?.year
            
            val formattedDate = if (monthName != null && day != null && yearVal != null) {
                "$monthName $day, $yearVal"
            } else {
                 yearVal?.toString()
            }

            // Format end date
            val endMonthName = media.endDate?.month?.let { if (it in 1..12) months[it - 1] else null }
            val endDay = media.endDate?.day?.let { if (it < 10) "0$it" else "$it" }
            val endYearVal = media.endDate?.year
            
            val formattedEndDate = if (endMonthName != null && endDay != null && endYearVal != null) {
                "$endMonthName $endDay, $endYearVal"
            } else {
                 endYearVal?.toString()
            }

            // Format aired date range
            val airedDate = when {
                formattedDate != null && formattedEndDate != null -> "$formattedDate - $formattedEndDate"
                formattedDate != null -> formattedDate
                else -> null
            }

            // Map tags
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

            // Map trailer
            val trailer = media.trailer?.let { trailer ->
                if (trailer.id != null && trailer.site != null) {
                    Trailer(
                        id = trailer.id,
                        site = trailer.site,
                        thumbnail = trailer.thumbnail
                    )
                } else null
            }

            // Map recommendations
            val recommendations = media.recommendations?.nodes?.filterNotNull()?.mapNotNull { node ->
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

            // Map reviews
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

            // Update cache
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
                // Refresh cache to get updated list entry
                refreshMediaDetails(mediaId)
                
                // Check if entry already exists in library
                val existingEntry = libraryDao.getEntry(mediaId)
                
                if (existingEntry != null) {
                    // Update existing entry
                    libraryDao.updateStatusAndProgress(mediaId, status, progress)
                } else {
                    // Create new entry from cached media details
                    val savedEntry = response.data?.SaveMediaListEntry
                    val cachedMedia = mediaDetailsDao.getById(mediaId)
                    
                    android.util.Log.d("DetailsRepo", "Creating new library entry: mediaId=$mediaId, savedEntryId=${savedEntry?.id}, cachedMedia=${cachedMedia != null}")
                    
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
                            timeUntilAiring = null, // Not available from cached details
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
                        android.util.Log.d("DetailsRepo", "Inserting entry: id=${newEntry.id}, mediaId=${newEntry.mediaId}, type=${newEntry.mediaType}, createdAt=${newEntry.createdAt}")
                        libraryDao.insertOrReplace(newEntry)
                        android.util.Log.d("DetailsRepo", "Entry inserted successfully")
                    } else {
                        android.util.Log.w("DetailsRepo", "No cached media found for mediaId=$mediaId, entry NOT created")
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
                // Remove from library cache so LibraryScreen reflects the deletion immediately
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
                    mangaId = Optional.present(mediaId)
                )
            } else {
                ToggleFavouriteMutation(
                    animeId = Optional.present(mediaId),
                    mangaId = Optional.absent()
                )
            }

            val response = apolloClient.mutation(mutation).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Toggle favourite failed"
                throw Exception(errorMessage)
            }

            // Refresh to get the updated isFavourite state
            refreshMediaDetails(mediaId)

            // Return the new favourite state (toggled)
            val currentEntity = mediaDetailsDao.getById(mediaId)
            currentEntity?.isFavourite ?: false
        }
    }

    override suspend fun getCharacterDetails(id: Int): Result<CharacterDetails> {
        return safeApiCall {
            val response = apolloClient.query(
                GetCharacterDetailsQuery(id = id)
            ).execute()

            val charData = response.data?.Character
                ?: throw Exception("Character not found")

            val mediaList = charData.media?.edges?.filterNotNull()?.mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                val voiceActorNode = edge.voiceActors?.firstOrNull() // Get first voice actor
                
                CharacterMedia(
                    id = node.id ?: 0,
                    titleRomaji = node.title?.romaji,
                    titleEnglish = node.title?.english,
                    titleNative = node.title?.native,
                    titleUserPreferred = node.title?.userPreferred ?: "Unknown",
                    coverUrl = node.coverImage?.large,
                    type = node.type,
                    voiceActor = voiceActorNode?.let { va -> 
                        VoiceActor(
                           id = va.id ?: 0,
                           nameFull = va.name?.full ?: "Unknown",
                           nameNative = null, // Not fetching native name for VA list yet to save bandwidth, or add if needed
                           nameUserPreferred = va.name?.full ?: "Unknown", // Fallback to full
                           imageUrl = va.image?.medium
                        ) 
                    }
                )
            } ?: emptyList()

            CharacterDetails(
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
        }
    }
    override suspend fun getMediaReviews(mediaId: Int, page: Int): Result<Pair<List<MediaReview>, Boolean>> {
        return safeApiCall {
            val response = apolloClient.query(
                com.anisync.android.GetMediaReviewsQuery(
                    mediaId = mediaId,
                    page = page
                )
            ).execute()

            if (response.hasErrors() || response.data == null) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to fetch reviews")
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

    override suspend fun rateReview(reviewId: Int, rating: com.anisync.android.type.ReviewRating): Result<MediaReview> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.RateReviewMutation(
                    reviewId = com.apollographql.apollo.api.Optional.present(reviewId),
                    rating = com.apollographql.apollo.api.Optional.present(rating)
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

}
