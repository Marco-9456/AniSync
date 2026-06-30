package com.anisync.android.data

import com.anisync.android.DeleteMediaListEntryMutation
import com.anisync.android.GetCharacterDetailsQuery
import com.anisync.android.GetMediaDetailsQuery
import com.anisync.android.GetStaffDetailsQuery
import com.anisync.android.GetStudioDetailsQuery
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
import com.anisync.android.domain.MediaFollowingEntry
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.RecommendedMedia
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.domain.Result
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.StudioDetails
import com.anisync.android.domain.StudioMediaEntry
import com.anisync.android.domain.Tag
import com.anisync.android.domain.Trailer
import com.anisync.android.domain.VoiceActor
import com.anisync.android.domain.VoicedCharacter
import com.anisync.android.type.MediaType
import com.anisync.android.util.AniListTextEncoder.encodeForAniList
import com.anisync.android.util.stripHtml
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val HOUR_MS = 60L * 60L * 1000L
private const val DAY_MS = 24L * HOUR_MS

private val MONTH_ABBR = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private fun formatFuzzyDateLong(month: Int?, day: Int?, year: Int?): String? {
    val m = month?.takeIf { it in 1..12 }?.let { MONTH_ABBR[it - 1] }
    val d = day?.let { if (it < 10) "0$it" else it.toString() }
    return when {
        m != null && d != null && year != null -> "$m $d, $year"
        year != null -> year.toString()
        else -> null
    }
}

private fun formatFuzzyDateShort(month: Int?, day: Int?, year: Int?): String? {
    val m = month?.takeIf { it in 1..12 }?.let { MONTH_ABBR[it - 1] }
    return when {
        m != null && day != null && year != null -> "$m $day, $year"
        m != null && day != null -> "$m $day"
        year != null -> year.toString()
        else -> null
    }
}

class DetailsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val mediaDetailsDao: MediaDetailsDao,
    private val libraryDao: LibraryDao,
    private val favouriteOverrideStore: FavouriteOverrideStore,
    private val accountStore: com.anisync.android.data.account.AccountStore
) : DetailsRepository {

    private fun currentOwnerId(): Int = accountStore.activeAccount.value?.id ?: -1

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
            }?.distinctBy { "${it.id}_${it.role}" } ?: emptyList()

            val staff = media.staff?.edges?.filterNotNull()?.mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                com.anisync.android.domain.StaffInfo(
                    id = node.id,
                    nameFull = node.name?.full ?: "Unknown",
                    nameNative = node.name?.native,
                    nameUserPreferred = node.name?.userPreferred ?: node.name?.full ?: "Unknown",
                    imageUrl = node.image?.large,
                    role = edge.role.orEmpty(),
                    primaryOccupations = node.primaryOccupations?.filterNotNull().orEmpty()
                )
            }?.distinctBy { "${it.id}_${it.role}" } ?: emptyList()

            val relations = media.relations?.edges?.filterNotNull()?.map { edge ->
                val node = edge.node
                RelatedMedia(
                    id = node?.id ?: 0,
                    titleRomaji = node?.title?.romaji,
                    titleEnglish = node?.title?.english,
                    titleNative = node?.title?.native,
                    titleUserPreferred = node?.title?.userPreferred ?: "Unknown",
                    coverUrl = node?.coverImage?.large,
                    cover = com.anisync.android.domain.CoverImage.of(node?.coverImage?.medium, node?.coverImage?.large, node?.coverImage?.extraLarge),
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

            val formattedDate = formatFuzzyDateLong(
                media.startDate?.month, media.startDate?.day, media.startDate?.year
            )
            val formattedEndDate = formatFuzzyDateLong(
                media.endDate?.month, media.endDate?.day, media.endDate?.year
            )

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
                        cover = com.anisync.android.domain.CoverImage.of(rec.coverImage?.medium, rec.coverImage?.large, rec.coverImage?.extraLarge),
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

            // AniList returns animation studios (isMain) and producers/distributors
            // (non-main) in one studio connection; split them here.
            val studioEdges = media.studios?.edges?.filterNotNull().orEmpty()
            val mainStudios = studioEdges
                .filter { it.isMain }
                .mapNotNull { edge -> edge.node?.let { com.anisync.android.domain.StudioRef(it.id, it.name) } }
            val producers = studioEdges
                .filter { !it.isMain }
                .mapNotNull { edge -> edge.node?.let { com.anisync.android.domain.StudioRef(it.id, it.name) } }
            val synonyms = media.synonyms?.filterNotNull().orEmpty()
            // AniList stores hashtags as a single space-separated string (e.g. "#rezero #リゼロ").
            val hashtags = media.hashtag
                ?.split(" ")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()

            val details = MediaDetails(
                id = media.id ?: 0,
                titleRomaji = titleRomaji,
                titleEnglish = titleEnglish,
                titleNative = titleNative,
                titleUserPreferred = titleUserPreferred,
                coverUrl = media.coverImage?.extraLarge,
                cover = com.anisync.android.domain.CoverImage.of(media.coverImage?.medium, media.coverImage?.large, media.coverImage?.extraLarge),
                coverColor = media.coverImage?.color,
                bannerUrl = media.bannerImage,
                description = media.description?.stripHtml() ?: "",
                score = media.averageScore,
                meanScore = media.meanScore,
                popularity = media.popularity,
                favourites = media.favourites,
                episodes = media.episodes,
                nextAiringEpisode = media.nextAiringEpisode?.let { airing ->
                    com.anisync.android.domain.NextAiringEpisode(
                        episode = airing.episode,
                        airingAt = airing.airingAt.toLong(),
                        timeUntilAiring = airing.timeUntilAiring
                    )
                },
                chapters = media.chapters,
                volumes = media.volumes,
                type = media.type,
                status = media.status?.name ?: "UNKNOWN",
                format = media.format?.name,
                genres = media.genres?.filterNotNull() ?: emptyList(),
                synonyms = synonyms,
                hashtags = hashtags,
                source = media.source?.name,
                studio = mainStudios.firstOrNull(),
                studios = mainStudios,
                producers = producers,
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
                listNotes = listEntry?.notes,
                listEntryPrivate = listEntry?.`private`,
                listEntryHiddenFromStatusLists = listEntry?.hiddenFromStatusLists,
                characters = characters,
                staff = staff,
                relations = relations,
                externalLinks = externalLinks,
                recommendations = recommendations,
                reviews = reviews,
                isFavourite = media.isFavourite ?: false,
                isRecommendationBlocked = media.isRecommendationBlocked,
                isReviewBlocked = media.isReviewBlocked
            )

            mediaDetailsDao.insert(details.toEntity())
        }
    }

    override suspend fun refreshMediaDetailsIfStale(id: Int): Result<Unit> {
        val cached = mediaDetailsDao.getById(id)
            ?: return refreshMediaDetails(id) // nothing cached yet → must fetch

        val now = System.currentTimeMillis()

        // The cached airing countdown has already elapsed (an episode aired since we
        // cached this): the data is provably stale, so refetch regardless of TTL.
        // nextAiringEpisodeTime is a Unix timestamp in seconds.
        val countdownElapsed =
            cached.nextAiringEpisodeTime?.let { it * 1000L < now } == true

        val age = now - cached.lastUpdated
        val stale = countdownElapsed || age >= staleAfterMillis(cached.status)

        return if (stale) refreshMediaDetails(id) else Result.Success(Unit)
    }

    /**
     * How long a cached media-details row stays fresh, keyed by AniList media status.
     * Airing / upcoming media changes often (new episodes, countdown, schedule
     * publication) so it expires quickly; finished media rarely changes so it lingers.
     */
    private fun staleAfterMillis(status: String): Long = when (status) {
        "RELEASING" -> 3 * HOUR_MS          // weekly episodes + live countdown
        "NOT_YET_RELEASED" -> 6 * HOUR_MS   // air schedule can be published any time
        "FINISHED", "CANCELLED" -> 7 * DAY_MS
        else -> DAY_MS                       // HIATUS / unknown
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
                val owner = currentOwnerId()
                val existingEntry = libraryDao.getEntry(owner, mediaId)

                if (existingEntry != null) {
                    libraryDao.updateStatusAndProgress(owner, mediaId, status, progress)
                } else {
                    val savedEntry = response.data?.SaveMediaListEntry
                    val cachedMedia = mediaDetailsDao.getById(mediaId)

                    if (cachedMedia != null) {
                        val newEntry = com.anisync.android.data.local.entity.LibraryEntryEntity(
                            id = savedEntry?.id ?: 0,
                            ownerId = owner,
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
                libraryDao.deleteByMediaId(currentOwnerId(), mediaId)
            } else {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Delete failed"
                throw Exception(errorMessage)
            }
        }
    }

    override suspend fun toggleFavourite(mediaId: Int, mediaType: MediaType): Result<Boolean> {
        // Flip the cached flag immediately so the heart fills/empties before the
        // network round-trip. We deliberately do NOT re-run GetMediaDetails after
        // the toggle (read-after-write on AniList is eventually consistent and
        // would overwrite the flip with a stale value) and do NOT derive the new
        // state from the mutation response's anime/manga nodes (paged, ~25 per
        // page — the toggled item is often absent from the first page, which
        // would falsely report "not favourited" and snap the heart back).
        // Mutation success is sufficient evidence the toggle landed.
        val previous = mediaDetailsDao.getById(mediaId)?.isFavourite ?: false
        val optimistic = !previous
        mediaDetailsDao.updateFavouriteStatus(mediaId, optimistic)

        val result = safeApiCall {
            val mutation = if (mediaType == MediaType.MANGA) {
                ToggleFavouriteMutation(
                    animeId = Optional.absent(),
                    mangaId = Optional.present(mediaId),
                    characterId = Optional.absent(),
                    staffId = Optional.absent(),
                    studioId = Optional.absent()
                )
            } else {
                ToggleFavouriteMutation(
                    animeId = Optional.present(mediaId),
                    mangaId = Optional.absent(),
                    characterId = Optional.absent(),
                    staffId = Optional.absent(),
                    studioId = Optional.absent()
                )
            }

            val response = apolloClient.mutation(mutation).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.firstOrNull()?.message ?: "Toggle favourite failed"
                throw Exception(errorMessage)
            }

            optimistic
        }

        if (result is Result.Error) {
            mediaDetailsDao.updateFavouriteStatus(mediaId, previous)
        }
        return result
    }

    override suspend fun toggleCharacterFavourite(
        characterId: Int,
        newState: Boolean
    ): Result<Unit> {
        return safeApiCall {
            val mutation = ToggleFavouriteMutation(
                animeId = Optional.absent(),
                mangaId = Optional.absent(),
                characterId = Optional.present(characterId),
                staffId = Optional.absent(),
                studioId = Optional.absent()
            )

            val response = apolloClient.mutation(mutation).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.firstOrNull()?.message ?: "Toggle favourite failed"
                throw Exception(errorMessage)
            }
            // Mutation success is sufficient — do not derive state from the
            // paged characters.nodes (only ~25 per page; toggled item often absent).
            // Override bridges AniList's eventual-consistency window: a follow-up
            // Character(id) query can still return the pre-toggle isFavourite for
            // several seconds. Cleared by getCharacterDetails once server catches up.
            favouriteOverrideStore.set(FavouriteEntity.CHARACTER, characterId, newState)
        }
    }

    override suspend fun toggleStaffFavourite(
        staffId: Int,
        newState: Boolean
    ): Result<Unit> {
        return safeApiCall {
            val mutation = ToggleFavouriteMutation(
                animeId = Optional.absent(),
                mangaId = Optional.absent(),
                characterId = Optional.absent(),
                staffId = Optional.present(staffId),
                studioId = Optional.absent()
            )

            val response = apolloClient.mutation(mutation).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.firstOrNull()?.message ?: "Toggle favourite failed"
                throw Exception(errorMessage)
            }
            // See toggleCharacterFavourite — paged response + EC window.
            favouriteOverrideStore.set(FavouriteEntity.STAFF, staffId, newState)
        }
    }

    override suspend fun toggleStudioFavourite(
        studioId: Int,
        newState: Boolean
    ): Result<Unit> {
        return safeApiCall {
            val mutation = ToggleFavouriteMutation(
                animeId = Optional.absent(),
                mangaId = Optional.absent(),
                characterId = Optional.absent(),
                staffId = Optional.absent(),
                studioId = Optional.present(studioId)
            )

            val response = apolloClient.mutation(mutation).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.firstOrNull()?.message ?: "Toggle favourite failed"
                throw Exception(errorMessage)
            }
            // See toggleCharacterFavourite — paged response + EC window.
            favouriteOverrideStore.set(FavouriteEntity.STUDIO, studioId, newState)
        }
    }

    override suspend fun getCharacterDetails(id: Int, page: Int): Result<CharacterDetails> {
        return safeApiCall {
            // NetworkOnly: the toggle favourite mutation does not patch the normalized
            // cache, so CacheFirst would serve stale isFavourite on re-entry.
            val response = apolloClient.query(
                GetCharacterDetailsQuery(id = id, page = Optional.presentIfNotNull(page))
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

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
                    cover = com.anisync.android.domain.CoverImage.of(node.coverImage?.medium, node.coverImage?.large, node.coverImage?.extraLarge),
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

            val serverIsFav = charData.isFavourite ?: false
            // Bridge AniList eventual consistency: clear override if server caught up,
            // else apply override on top of (stale) server value.
            favouriteOverrideStore.clearIfMatches(FavouriteEntity.CHARACTER, id, serverIsFav)
            val effectiveIsFav =
                favouriteOverrideStore.get(FavouriteEntity.CHARACTER, id) ?: serverIsFav

            CharacterDetails(
                id = charData.id ?: 0,
                name = charData.name?.full ?: "Unknown",
                nativeName = charData.name?.native,
                nameUserPreferred = charData.name?.userPreferred ?: charData.name?.full ?: "Unknown",
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
                isFavourite = effectiveIsFav,
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

    override suspend fun getMediaFollowing(
        mediaId: Int,
        page: Int,
        perPage: Int
    ): Result<Pair<List<MediaFollowingEntry>, Boolean>> {
        return safeApiCall {
            val response = apolloClient.query(
                com.anisync.android.GetMediaFollowingQuery(
                    mediaId = mediaId,
                    page = page,
                    perPage = perPage
                )
            ).execute()

            if (response.hasErrors() || response.data == null) {
                throw Exception(
                    response.errors?.firstOrNull()?.message ?: "Failed to fetch following"
                )
            }

            val pageData = response.data?.Page
            val hasNextPage = pageData?.pageInfo?.hasNextPage ?: false
            val nodes = pageData?.mediaList?.filterNotNull() ?: emptyList()

            val mapped = nodes.mapNotNull { node ->
                val user = node.user ?: return@mapNotNull null
                MediaFollowingEntry(
                    userId = user.id,
                    userName = user.name,
                    // Prefer `large` — AniList serves the animated GIF original here;
                    // `medium` is often a static resized frame.
                    userAvatarUrl = user.avatar?.large ?: user.avatar?.medium,
                    status = node.status?.toDomainStatus()
                        ?: com.anisync.android.domain.LibraryStatus.UNKNOWN,
                    score = node.score?.takeIf { it > 0.0 },
                    progress = node.progress,
                    scoreFormat = mapScoreFormat(user.mediaListOptions?.scoreFormat?.name),
                    notes = node.notes?.takeIf { it.isNotBlank() }
                )
            }

            Pair(mapped, hasNextPage)
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

    @Volatile
    private var cachedViewerId: Int? = null

    private suspend fun resolveViewerId(): Int? {
        cachedViewerId?.let { return it }
        return try {
            val response = apolloClient.query(com.anisync.android.GetViewerQuery())
                .fetchPolicy(FetchPolicy.CacheFirst)
                .execute()
            response.data?.Viewer?.id?.also { cachedViewerId = it }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getViewerReview(mediaId: Int): Result<com.anisync.android.domain.ViewerReview?> {
        return safeApiCall {
            val viewerId = resolveViewerId() ?: throw Exception("Not signed in")
            val response = apolloClient.query(
                com.anisync.android.GetViewerReviewQuery(mediaId = mediaId, userId = viewerId)
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to load review")
            }

            val node = response.data?.Page?.reviews?.filterNotNull()?.firstOrNull()
                ?: return@safeApiCall null

            com.anisync.android.domain.ViewerReview(
                id = node.id,
                summary = node.summary ?: "",
                body = node.body ?: "",
                score = node.score ?: 0,
                isPrivate = node.`private` ?: false
            )
        }
    }

    override suspend fun saveReview(
        reviewId: Int?,
        mediaId: Int,
        body: String,
        summary: String,
        score: Int,
        private: Boolean
    ): Result<Int> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.SaveReviewMutation(
                    id = Optional.presentIfNotNull(reviewId),
                    mediaId = Optional.present(mediaId),
                    body = Optional.present(encodeForAniList(body)),
                    summary = Optional.present(encodeForAniList(summary)),
                    score = Optional.present(score),
                    private = Optional.present(private)
                )
            ).execute()

            if (response.hasErrors() || response.data?.SaveReview == null) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to save review")
            }

            // Pull the new review into the cached details so the section updates.
            refreshMediaDetails(mediaId)

            response.data?.SaveReview?.id ?: 0
        }
    }

    override suspend fun deleteReview(reviewId: Int, mediaId: Int): Result<Unit> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.DeleteReviewMutation(id = Optional.present(reviewId))
            ).execute()

            if (response.hasErrors() || response.data?.DeleteReview?.deleted != true) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to delete review")
            }

            // Drop the deleted review from the cached details.
            refreshMediaDetails(mediaId)
        }
    }

    override suspend fun getStaffDetails(
        id: Int,
        page: Int,
        staffMediaPage: Int
    ): Result<StaffDetails> {
        return safeApiCall {
            // See getCharacterDetails — favourite toggle doesn't update the cache.
            val response = apolloClient.query(
                GetStaffDetailsQuery(
                    id = id,
                    page = Optional.presentIfNotNull(page),
                    staffMediaPage = Optional.presentIfNotNull(staffMediaPage)
                )
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val staffData = response.data?.Staff
                ?: throw Exception("Staff not found")

            val alternativeNames = staffData.name?.alternative?.filterNotNull() ?: emptyList()

            val pageInfo = staffData.characterMedia?.pageInfo
            val hasNextPage = pageInfo?.hasNextPage ?: false

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
                    characterNameNative = firstChar.name?.native,
                    characterNameUserPreferred = firstChar.name?.userPreferred
                        ?: firstChar.name?.full ?: "Unknown",
                    characterImageUrl = firstChar.image?.medium,
                    mediaAppearances = entries.mapNotNull { (edge, _) ->
                        val node = edge.node ?: return@mapNotNull null
                        CharacterMediaAppearance(
                            mediaId = node.id ?: 0,
                            mediaTitle = node.title?.userPreferred ?: "Unknown",
                            mediaTitleRomaji = node.title?.romaji,
                            mediaTitleEnglish = node.title?.english,
                            mediaTitleNative = node.title?.native,
                            coverUrl = node.coverImage?.large,
                            cover = com.anisync.android.domain.CoverImage.of(node.coverImage?.medium, node.coverImage?.large, node.coverImage?.extraLarge),
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

            val serverIsFav = staffData.isFavourite ?: false
            favouriteOverrideStore.clearIfMatches(FavouriteEntity.STAFF, id, serverIsFav)
            val effectiveIsFav =
                favouriteOverrideStore.get(FavouriteEntity.STAFF, id) ?: serverIsFav

            val productionMedia = staffData.staffMedia?.edges?.filterNotNull()?.mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                val nodeId = node.id ?: return@mapNotNull null
                com.anisync.android.domain.StaffProductionMedia(
                    mediaId = nodeId,
                    titleUserPreferred = node.title?.userPreferred ?: "Unknown",
                    titleRomaji = node.title?.romaji,
                    titleEnglish = node.title?.english,
                    titleNative = node.title?.native,
                    coverUrl = node.coverImage?.large,
                    cover = com.anisync.android.domain.CoverImage.of(
                        node.coverImage?.medium,
                        node.coverImage?.large,
                        node.coverImage?.extraLarge
                    ),
                    type = node.type,
                    startYear = node.startDate?.year,
                    staffRole = edge.staffRole,
                    popularity = node.popularity,
                    averageScore = node.averageScore,
                    favourites = node.favourites,
                    isOnList = node.mediaListEntry?.id != null
                )
            }?.distinctBy { "${it.mediaId}_${it.staffRole.orEmpty()}" } ?: emptyList()
            val productionMediaHasNextPage =
                staffData.staffMedia?.pageInfo?.hasNextPage ?: false

            StaffDetails(
                id = staffData.id,
                name = staffData.name?.full ?: "Unknown",
                nativeName = staffData.name?.native,
                nameUserPreferred = staffData.name?.userPreferred ?: staffData.name?.full ?: "Unknown",
                alternativeNames = alternativeNames,
                imageUrl = staffData.image?.large,
                description = staffData.description,
                gender = staffData.gender,
                age = staffData.age,
                bloodType = staffData.bloodType,
                dateOfBirth = staffData.dateOfBirth?.let {
                    formatFuzzyDateShort(it.month, it.day, it.year)
                },
                dateOfDeath = staffData.dateOfDeath?.let {
                    formatFuzzyDateShort(it.month, it.day, it.year)
                },
                favourites = staffData.favourites,
                isFavourite = effectiveIsFav,
                language = staffData.languageV2,
                primaryOccupations = staffData.primaryOccupations?.filterNotNull() ?: emptyList(),
                yearsActive = staffData.yearsActive?.filterNotNull() ?: emptyList(),
                homeTown = staffData.homeTown,
                voicedCharacters = voicedCharacters,
                hasNextPage = hasNextPage,
                productionMedia = productionMedia,
                productionMediaHasNextPage = productionMediaHasNextPage
            )
        }
    }

    override suspend fun getStudioDetails(id: Int, page: Int): Result<StudioDetails> {
        return safeApiCall {
            // See getCharacterDetails — favourite toggle doesn't update the cache.
            val response = apolloClient.query(
                GetStudioDetailsQuery(id = Optional.present(id), page = Optional.present(page))
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val studioData = response.data?.Studio
                ?: throw Exception("Studio not found")

            val pageInfo = studioData.media?.pageInfo
            val hasNextPage = pageInfo?.hasNextPage ?: false

            // AniList can return the same media twice when a media has multiple
            // production links to a single studio (e.g. franchise re-issues).
            // Dedup by mediaId, preferring the edge marked as main studio.
            val mediaList = studioData.media?.edges?.filterNotNull()?.mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                val mediaId = node.id ?: return@mapNotNull null
                StudioMediaEntry(
                    mediaId = mediaId,
                    titleUserPreferred = node.title?.userPreferred ?: "Unknown",
                    titleRomaji = node.title?.romaji,
                    titleEnglish = node.title?.english,
                    titleNative = node.title?.native,
                    coverUrl = node.coverImage?.large,
                    cover = com.anisync.android.domain.CoverImage.of(
                        node.coverImage?.medium,
                        node.coverImage?.large,
                        node.coverImage?.extraLarge
                    ),
                    format = node.format?.name,
                    type = node.type,
                    status = node.status?.name,
                    year = node.startDate?.year,
                    averageScore = node.averageScore,
                    popularity = node.popularity,
                    favourites = node.favourites,
                    isMainStudio = edge.isMainStudio,
                    isOnList = node.mediaListEntry?.id != null
                )
            }
                ?.groupBy { it.mediaId }
                ?.map { (_, group) -> group.firstOrNull { it.isMainStudio } ?: group.first() }
                ?: emptyList()

            val serverIsFav = studioData.isFavourite
            favouriteOverrideStore.clearIfMatches(FavouriteEntity.STUDIO, id, serverIsFav)
            val effectiveIsFav =
                favouriteOverrideStore.get(FavouriteEntity.STUDIO, id) ?: serverIsFav

            StudioDetails(
                id = studioData.id,
                name = studioData.name,
                isAnimationStudio = studioData.isAnimationStudio,
                siteUrl = studioData.siteUrl,
                favourites = studioData.favourites ?: 0,
                isFavourite = effectiveIsFav,
                media = mediaList,
                hasNextPage = hasNextPage
            )
        }
    }
}