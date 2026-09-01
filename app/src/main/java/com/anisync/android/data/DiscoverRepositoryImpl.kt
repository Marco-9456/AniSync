package com.anisync.android.data

import com.anisync.android.GetMediaBySortQuery
import com.anisync.android.GetPaginatedMediaQuery
import com.anisync.android.GetUpcomingMediaQuery
import com.anisync.android.data.util.dataOrThrow
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.PaginatedResult
import com.anisync.android.domain.Result
import com.anisync.android.domain.map
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSort
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.doNotStore
import com.apollographql.cache.normalized.fetchPolicy
import javax.inject.Inject

class DiscoverRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : DiscoverRepository {

    override suspend fun getTrending(type: MediaType): Result<List<LibraryEntry>> {
        return fetchMedia(listOf(MediaSort.TRENDING_DESC), type)
    }

    override suspend fun getPopular(type: MediaType): Result<List<LibraryEntry>> {
        return fetchMedia(listOf(MediaSort.POPULARITY_DESC), type)
    }

    override suspend fun getNewlyAdded(type: MediaType): Result<List<LibraryEntry>> {
        return fetchMedia(listOf(MediaSort.ID_DESC), type)
    }

    override suspend fun getRecentReviews(
        mediaType: MediaType?,
        mediaId: Int?,
        userId: Int?,
        sort: com.anisync.android.domain.ReviewSortOption,
        page: Int
    ): Result<com.anisync.android.domain.UserReviewsPage> {
        return safeApiCall {
            val response = apolloClient.query(
                com.anisync.android.GetRecentReviewsQuery(
                    mediaType = mediaType?.let { Optional.present(it) } ?: Optional.absent(),
                    mediaId = mediaId?.let { Optional.present(it) } ?: Optional.absent(),
                    userId = userId?.let { Optional.present(it) } ?: Optional.absent(),
                    sort = Optional.present(listOf(sort.apiValue)),
                    page = Optional.present(page)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()

            if (response.hasErrors()) {
                throw Exception(response.errors?.first()?.message ?: "Failed to fetch recent reviews")
            }

            val reviews = response.data?.Page?.reviews?.filterNotNull()?.map { review ->
                com.anisync.android.domain.MediaReview(
                    id = review.id,
                    summary = review.summary ?: "",
                    body = review.body,
                    score = review.score ?: 0,
                    rating = review.rating ?: 0,
                    ratingAmount = review.ratingAmount ?: 0,
                    userRating = review.userRating?.name,
                    userName = review.user?.name ?: "Unknown",
                    userAvatarUrl = review.user?.avatar?.large,
                    createdAt = review.createdAt.toLong(),
                    mediaTitle = review.media?.title?.userPreferred,
                    mediaCoverUrl = review.media?.coverImage?.large,
                    mediaCover = com.anisync.android.domain.CoverImage.of(
                        review.media?.coverImage?.medium,
                        review.media?.coverImage?.large,
                        review.media?.coverImage?.extraLarge
                    ),
                    mediaBannerUrl = review.media?.bannerImage
                )
            } ?: emptyList()

            com.anisync.android.domain.UserReviewsPage(
                reviews = reviews,
                hasNextPage = response.data?.Page?.pageInfo?.hasNextPage == true
            )
        }
    }

    override suspend fun getUpcoming(type: MediaType): Result<List<LibraryEntry>> {
        return safeApiCall {
            val response = apolloClient.query(
                GetUpcomingMediaQuery(
                    perPage = Optional.present(10),
                    type = Optional.present(type),
                    status = Optional.present(MediaStatus.NOT_YET_RELEASED)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()

            response.dataOrThrow().Page?.media?.filterNotNull()
                ?.filter { it.season != null }
                ?.map { media -> media.toLibraryEntry("UPCOMING") }
                .orEmpty()
        }
    }

    override suspend fun getTBA(type: MediaType): Result<List<LibraryEntry>> {
        return safeApiCall {
            val response = apolloClient.query(
                GetUpcomingMediaQuery(
                    perPage = Optional.present(20),
                    type = Optional.present(type),
                    status = Optional.present(MediaStatus.NOT_YET_RELEASED)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()

            response.dataOrThrow().Page?.media?.filterNotNull()
                ?.filter { it.season == null }
                ?.take(10)
                ?.map { media -> media.toLibraryEntry("TBA") }
                .orEmpty()
        }
    }

    /**
     * The whole NOT_YET_RELEASED set in one request, each entry tagged UPCOMING or TBA by whether
     * AniList gave it a season. [getUpcoming] and [getTBA] still serve the section grids, which
     * page the two halves separately; Discover shows them as one rail because the split is an
     * artefact of the data, not something a reader is browsing by. On Manga the split collapses
     * entirely, since manga entries almost never carry a season.
     */
    override suspend fun getNotYetReleased(type: MediaType): Result<List<LibraryEntry>> {
        return safeApiCall {
            val response = apolloClient.query(
                GetUpcomingMediaQuery(
                    perPage = Optional.present(20),
                    type = Optional.present(type),
                    status = Optional.present(MediaStatus.NOT_YET_RELEASED)
                )
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .doNotStore(true)
                .execute()

            response.dataOrThrow().Page?.media?.filterNotNull()
                ?.take(10)
                ?.map { media -> media.toLibraryEntry(if (media.season != null) "UPCOMING" else "TBA") }
                .orEmpty()
        }
    }

    /**
     * Media that is actively coming out. Discover shows this on the Manga tab in the slot the
     * airing timeline holds for anime, because `airingSchedules` has no manga counterpart.
     */
    override suspend fun getReleasing(type: MediaType): Result<List<LibraryEntry>> {
        return getPaginatedSection("releasing", type, page = 1, format = null)
            .map { it.items }
    }

    override suspend fun getPaginatedSection(
        sectionType: String,
        type: MediaType,
        page: Int,
        format: MediaFormat?
    ): Result<PaginatedResult<LibraryEntry>> {
        return safeApiCall {
            val (sort, status) = when (sectionType) {
                "trending" -> listOf(MediaSort.TRENDING_DESC) to null
                "popular" -> listOf(MediaSort.POPULARITY_DESC) to null
                "upcoming" -> listOf(MediaSort.POPULARITY_DESC) to MediaStatus.NOT_YET_RELEASED
                "tba" -> listOf(MediaSort.POPULARITY_DESC) to MediaStatus.NOT_YET_RELEASED
                "newly_added" -> listOf(MediaSort.ID_DESC) to null
                "releasing" -> listOf(MediaSort.TRENDING_DESC) to MediaStatus.RELEASING
                // The unreleased rail merges the season and no-season halves, so its grid must
                // not re-apply the split "upcoming" and "tba" filter on.
                "not_yet_released" -> listOf(MediaSort.POPULARITY_DESC) to MediaStatus.NOT_YET_RELEASED
                else -> listOf(MediaSort.POPULARITY_DESC) to null
            }

            val response = apolloClient.query(
                GetPaginatedMediaQuery(
                    page = Optional.present(page),
                    perPage = Optional.present(20),
                    sort = Optional.present(sort),
                    type = Optional.present(type),
                    status = status?.let { Optional.present(it) } ?: Optional.absent(),
                    format = format?.let { Optional.present(it) } ?: Optional.absent()
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()

            val pageData = response.dataOrThrow().Page
            val pageInfo = pageData?.pageInfo
            val allMedia = pageData?.media?.filterNotNull().orEmpty()
            
            val filteredMedia = when (sectionType) {
                "upcoming" -> allMedia.filter { it.season != null }
                "tba" -> allMedia.filter { it.season == null }
                else -> allMedia
            }

            val entries = filteredMedia.map { media ->
                LibraryEntry(
                    id = 0,
                    mediaId = media.id ?: 0,
                    titleRomaji = media.title?.romaji,
                    titleEnglish = media.title?.english,
                    titleNative = media.title?.native,
                    titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                    coverUrl = media.coverImage?.extraLarge,
                    cover = com.anisync.android.domain.CoverImage.of(media.coverImage?.medium, media.coverImage?.large, media.coverImage?.extraLarge),
                    progress = 0,
                    totalEpisodes = media.episodes,
                    totalChapters = media.chapters,
                    totalVolumes = media.volumes,
                    type = media.type,
                    format = media.format,
                    status = LibraryStatus.UNKNOWN,
                    mediaStatus = when (sectionType) {
                        "upcoming" -> "UPCOMING"
                        "tba" -> "TBA"
                        else -> null
                    },
                    averageScore = media.averageScore,
                    seasonYear = media.seasonYear,
                    season = media.season
                )
            }

            PaginatedResult(
                items = entries,
                hasNextPage = pageInfo?.hasNextPage ?: false,
                currentPage = pageInfo?.currentPage ?: page,
                totalPages = pageInfo?.lastPage ?: 1
            )
        }
    }

    private suspend fun fetchMedia(sort: List<MediaSort>, type: MediaType): Result<List<LibraryEntry>> {
        return safeApiCall {
            val response = apolloClient.query(
                GetMediaBySortQuery(
                    sort = Optional.present(sort),
                    type = Optional.present(type),
                    perPage = Optional.present(10)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()

            response.dataOrThrow().Page?.media?.filterNotNull()?.map { media ->
                LibraryEntry(
                    id = 0,
                    mediaId = media.id ?: 0,
                    titleRomaji = media.title?.romaji,
                    titleEnglish = media.title?.english,
                    titleNative = media.title?.native,
                    titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                    coverUrl = media.coverImage?.extraLarge,
                    cover = com.anisync.android.domain.CoverImage.of(media.coverImage?.medium, media.coverImage?.large, media.coverImage?.extraLarge),
                    progress = 0,
                    totalEpisodes = media.episodes,
                    totalChapters = media.chapters,
                    totalVolumes = media.volumes,
                    type = media.type,
                    format = media.format,
                    status = LibraryStatus.UNKNOWN,
                    mediaStatus = null,
                    averageScore = media.averageScore,
                    genres = media.genres?.filterNotNull().orEmpty(),
                    bannerUrl = media.bannerImage,
                    seasonYear = media.seasonYear
                )
            }.orEmpty()
        }
    }

    private fun GetUpcomingMediaQuery.Medium.toLibraryEntry(mediaStatus: String): LibraryEntry {
        return LibraryEntry(
            id = 0,
            mediaId = this.id ?: 0,
            titleRomaji = this.title?.romaji,
            titleEnglish = this.title?.english,
            titleNative = this.title?.native,
            titleUserPreferred = this.title?.userPreferred ?: "Unknown",
            coverUrl = this.coverImage?.extraLarge,
            cover = com.anisync.android.domain.CoverImage.of(this.coverImage?.medium, this.coverImage?.large, this.coverImage?.extraLarge),
            progress = 0,
            totalEpisodes = this.episodes,
            totalChapters = this.chapters,
            totalVolumes = this.volumes,
            type = this.type,
            format = this.format,
            status = LibraryStatus.UNKNOWN,
            mediaStatus = mediaStatus,
            averageScore = this.averageScore,
            seasonYear = this.seasonYear,
            season = this.season
        )
    }
}
