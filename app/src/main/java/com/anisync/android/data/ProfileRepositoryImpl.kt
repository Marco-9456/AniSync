package com.anisync.android.data

import com.anisync.android.GetFullUserProfileQuery
import com.anisync.android.GetUserActivitiesQuery
import com.anisync.android.GetUserFavoritesQuery
import com.anisync.android.GetUserFollowStateQuery
import com.anisync.android.GetViewerQuery
import com.anisync.android.ToggleUserFollowMutation
import com.anisync.android.data.account.AccountStore
import com.anisync.android.data.local.dao.UserProfileDao
import com.anisync.android.data.local.toDomain
import com.anisync.android.data.local.toEntity
import com.anisync.android.data.mapper.toDomainStatus
import com.anisync.android.data.util.safeApiCall
import android.os.SystemClock
import android.os.Trace
import com.anisync.android.domain.CachePolicy
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ProfileRefreshTimings
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.UserProfile
import com.anisync.android.util.AniListTextEncoder.encodeForAniList
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import com.anisync.android.data.mapper.toDomain as activityFieldsToDomain

class ProfileRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val userProfileDao: UserProfileDao,
    private val notificationBadgeStore: NotificationBadgeStore,
    private val accountStore: AccountStore
) : ProfileRepository {

    /**
     * In-flight request dedup keyed by (operation + params hash). A second
     * concurrent caller suspends on the mutex until the first completes;
     * the second's Apollo query (still `CacheFirst` when policy allows)
     * then hits the now-warm normalized cache instead of hitting network.
     */
    private val inflightMutexes = ConcurrentHashMap<String, Mutex>()

    private suspend fun <T> dedupe(key: String, block: suspend () -> T): T {
        val mtx = inflightMutexes.computeIfAbsent(key) { Mutex() }
        return try {
            mtx.withLock { block() }
        } finally {
            if (!mtx.isLocked) inflightMutexes.remove(key, mtx)
        }
    }

    private fun CachePolicy.toFetchPolicy(): FetchPolicy = when (this) {
        CachePolicy.CacheFirst -> FetchPolicy.CacheFirst
        CachePolicy.NetworkOnly -> FetchPolicy.NetworkOnly
        CachePolicy.NetworkFirst -> FetchPolicy.NetworkFirst
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeProfile(): Flow<UserProfile?> {
        // Re-subscribe per active account so the own-profile cache is account-scoped (the row's
        // primary key is the user id), and switching accounts shows the right cached profile.
        return accountStore.activeAccount
            .flatMapLatest { account ->
                userProfileDao.observe(account?.id ?: -1)
            }
            .map { entity -> entity?.toDomain() }
    }

    override suspend fun fetchUserProfile(username: String, forceNetwork: Boolean): Result<UserProfile> {
        return when (val r = fetchUserProfileTimed(username, forceNetwork)) {
            is Result.Success -> Result.Success(r.data.first)
            is Result.Error -> r
        }
    }

    private suspend fun fetchUserProfileTimed(
        username: String,
        forceNetwork: Boolean
    ): Result<Pair<UserProfile, ProfileRefreshTimings>> = dedupe("profile:${username}:$forceNetwork") {
        safeApiCall {
            val isOwnProfile = username.isBlank()

            // Resolve the userId up front when we can. The own profile reuses its
            // cached id; a cold first launch with no cache pays a one-time
            // GetViewer to learn id + name. Knowing the id lets us fold the
            // activity feeds into the single GetFullUserProfile request via
            // `includeActivities`. Target profiles are addressed by name (their id
            // is unknown until User resolves), so they fetch activities as a
            // lightweight follow-up — still fewer round-trips than before and with
            // no favourites fan-out.
            var knownUserId: Int? = null
            var queryName: String? = null
            if (isOwnProfile) {
                knownUserId = accountStore.activeAccount.value?.id?.takeIf { it > 0 }
                if (knownUserId == null) {
                    val viewerResponse = apolloClient.query(GetViewerQuery())
                        .fetchPolicy(FetchPolicy.NetworkOnly)
                        .execute()
                    val viewer = viewerResponse.data?.Viewer
                        ?: throw Exception("Unable to get current user")
                    knownUserId = viewer.id
                    queryName = viewer.name
                }
            } else {
                queryName = username
            }

            val includeActivities = knownUserId != null
            val policy = if (forceNetwork) FetchPolicy.NetworkOnly else FetchPolicy.CacheFirst

            val profileQueryStart = SystemClock.elapsedRealtime()
            Trace.beginSection("AniSync.Profile.Query.FullProfile")
            val response = try {
                apolloClient.query(
                    GetFullUserProfileQuery(
                        userId = knownUserId?.let { Optional.present(it) } ?: Optional.absent(),
                        name = queryName?.let { Optional.present(it) } ?: Optional.absent(),
                        // Own-profile path piggybacks viewer fields so the notification
                        // badge updates without a separate GetViewer round-trip.
                        includeViewer = Optional.present(isOwnProfile),
                        // Activity feeds only resolve when the userId is already known;
                        // otherwise they're fetched once User has resolved the id below.
                        includeActivities = Optional.present(includeActivities)
                    )
                )
                    .fetchPolicy(policy)
                    .execute()
            } finally {
                Trace.endSection()
            }
            val profileQueryMs = SystemClock.elapsedRealtime() - profileQueryStart

            if (response.hasErrors()) {
                val firstError = response.errors?.firstOrNull()?.message
                throw Exception(firstError ?: "Failed to load user profile")
            }

            val user = response.data?.User
                ?: throw Exception("User not found: @${queryName ?: knownUserId}")

            // Refresh the notification badge from the piggy-backed Viewer block.
            response.data?.Viewer?.unreadNotificationCount?.let {
                notificationBadgeStore.setFromServer(it)
            }

            // Page-1 favourites preview only — the eager all-pages fan-out is gone.
            // The Favorites tab pulls the remaining pages on demand via
            // getFavoriteAnime(), so a profile load/refresh costs a single request.
            val favorites = user.favourites?.anime?.nodes?.filterNotNull()?.map { node ->
                val m = node.mediaCardFields
                LibraryEntry(
                    id = 0,
                    mediaId = m.id,
                    titleRomaji = m.title?.romaji,
                    titleEnglish = m.title?.english,
                    titleNative = m.title?.native,
                    titleUserPreferred = m.title?.userPreferred ?: "Unknown",
                    coverUrl = m.coverImage?.large,
                    cover = com.anisync.android.domain.CoverImage.of(
                        m.coverImage?.medium,
                        m.coverImage?.large,
                        m.coverImage?.extraLarge
                    ),
                    progress = 0,
                    totalEpisodes = m.episodes,
                    totalChapters = m.chapters,
                    totalVolumes = m.volumes,
                    type = m.type,
                    status = LibraryStatus.UNKNOWN
                )
            }.orEmpty()

            // Activities: folded into the unified response when the userId was known
            // up front; otherwise fetched now that User has resolved the id.
            val activitiesStart = SystemClock.elapsedRealtime()
            val rawActivities = mutableListOf<com.anisync.android.fragment.ActivityFields>()
            if (includeActivities) {
                response.data?.allActivities?.activities?.filterNotNull()?.forEach { rawActivities.add(it.activityFields) }
                response.data?.textActivities?.activities?.filterNotNull()?.forEach { rawActivities.add(it.activityFields) }
                response.data?.messageReceived?.activities?.filterNotNull()?.forEach { rawActivities.add(it.activityFields) }
                response.data?.messageSent?.activities?.filterNotNull()?.forEach { rawActivities.add(it.activityFields) }
            } else {
                Trace.beginSection("AniSync.Profile.Query.UserActivities")
                val activitiesResponse = try {
                    apolloClient.query(GetUserActivitiesQuery(userId = Optional.present(user.id)))
                        .fetchPolicy(policy)
                        .execute()
                } finally {
                    Trace.endSection()
                }
                activitiesResponse.data?.allActivities?.activities?.filterNotNull()?.forEach { rawActivities.add(it.activityFields) }
                activitiesResponse.data?.textActivities?.activities?.filterNotNull()?.forEach { rawActivities.add(it.activityFields) }
                activitiesResponse.data?.messageReceived?.activities?.filterNotNull()?.forEach { rawActivities.add(it.activityFields) }
                activitiesResponse.data?.messageSent?.activities?.filterNotNull()?.forEach { rawActivities.add(it.activityFields) }
            }
            val activitiesMs = SystemClock.elapsedRealtime() - activitiesStart

            val activities = rawActivities
                .mapNotNull { it.activityFieldsToDomain() }
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<com.anisync.android.domain.UserActivity> { it.isPinned }
                        .thenByDescending { it.timestamp }
                )

            val stats = user.statistics?.anime
            val mangaStats = user.statistics?.manga
            val minutesWatched = stats?.minutesWatched ?: 0
            val daysWatched = minutesWatched / 1440f

            // Parse anime status counts
            val statusCounts = stats?.statuses?.filterNotNull()?.associate { 
                it.status to (it.count ?: 0) 
            } ?: emptyMap()
            
            val animeStatusCounts = com.anisync.android.domain.AnimeStatusCounts(
                watching = statusCounts[com.anisync.android.type.MediaListStatus.CURRENT] ?: 0,
                completed = statusCounts[com.anisync.android.type.MediaListStatus.COMPLETED] ?: 0,
                onHold = statusCounts[com.anisync.android.type.MediaListStatus.PAUSED] ?: 0,
                dropped = statusCounts[com.anisync.android.type.MediaListStatus.DROPPED] ?: 0,
                planning = statusCounts[com.anisync.android.type.MediaListStatus.PLANNING] ?: 0
            )
            
            val topGenres = stats?.genres?.filterNotNull()?.map { genre ->
                com.anisync.android.domain.GenreStat(
                    genre = genre.genre ?: "Unknown",
                    count = genre.count ?: 0,
                    meanScore = genre.meanScore?.toFloat() ?: 0f
                )
            } ?: emptyList()

            // Favorites already fetched in parallel with activities above.

                    val overviewManga = user.favourites?.manga?.nodes?.filterNotNull()?.map { media ->
                        val m = media.mediaCardFields
                        com.anisync.android.domain.LibraryEntry(
                            id = 0,
                            mediaId = m.id,
                            titleRomaji = m.title?.romaji,
                            titleEnglish = m.title?.english,
                            titleNative = m.title?.native,
                            titleUserPreferred = m.title?.userPreferred ?: "Unknown",
                            coverUrl = m.coverImage?.large,
                            cover = com.anisync.android.domain.CoverImage.of(m.coverImage?.medium, m.coverImage?.large, m.coverImage?.extraLarge),
                            progress = 0,
                            totalEpisodes = null,
                            totalChapters = m.chapters,
                            totalVolumes = m.volumes,
                            type = m.type,
                            format = null,
                            status = com.anisync.android.domain.LibraryStatus.UNKNOWN
                        )
                    } ?: emptyList()

            val overviewCharacters = user.favourites?.characters?.nodes?.filterNotNull()?.map { char ->
                com.anisync.android.domain.CharacterInfo(
                    id = char.id ?: 0,
                    nameFull = char.name?.userPreferred ?: "Unknown",
                    nameNative = null,
                    nameUserPreferred = char.name?.userPreferred ?: "Unknown",
                    imageUrl = char.image?.large,
                    role = ""
                )
            } ?: emptyList()

            val overviewStaff = user.favourites?.staff?.nodes?.filterNotNull()?.map { staff ->
                com.anisync.android.domain.StaffDetails(
                    id = staff.id ?: 0,
                    name = staff.name?.userPreferred ?: "Unknown",
                    nativeName = null,
                    nameUserPreferred = staff.name?.userPreferred ?: "Unknown",
                    imageUrl = staff.image?.large,
                    description = null,
                    gender = null,
                    age = null,
                    bloodType = null,
                    dateOfBirth = null,
                    dateOfDeath = null,
                    favourites = null,
                    language = null,
                    primaryOccupations = emptyList(),
                    yearsActive = emptyList(),
                    homeTown = null,
                    voicedCharacters = emptyList()
                )
            } ?: emptyList()

            val overviewStudios = user.favourites?.studios?.nodes?.filterNotNull()?.map { studio ->
                com.anisync.android.domain.StudioInfo(
                    id = studio.id,
                    name = studio.name
                )
            } ?: emptyList()

            val profile = UserProfile(
                id = user.id ?: 0,
                name = user.name ?: "Unknown",
                profileColor = user.options?.profileColor,
                avatarUrl = user.avatar?.large,
                bannerUrl = user.bannerImage,
                about = user.about,
                aboutMarkdown = user.aboutRaw,
                activeAt = user.updatedAt?.toLong()?.times(1000),
                animeCount = stats?.count ?: 0,
                daysWatched = daysWatched,
                mangaCount = mangaStats?.count ?: 0,
                chaptersRead = mangaStats?.chaptersRead ?: 0,
                meanScore = stats?.meanScore?.toFloat() ?: 0f,
                animeStatusCounts = animeStatusCounts,
                favoriteAnime = favorites,
                activities = activities,
                topGenres = topGenres,
                favoriteMangaOverview = overviewManga,
                favoriteCharactersOverview = overviewCharacters,
                favoriteStaffOverview = overviewStaff,
                favoriteStudiosOverview = overviewStudios,
                donatorTier = user.donatorTier ?: 0,
                donatorBadge = user.donatorBadge,
                moderatorRoles = user.moderatorRoles?.filterNotNull()?.map { it.name } ?: emptyList(),
                createdAt = user.createdAt?.toLong()?.times(1000)
            )

            val timings = ProfileRefreshTimings(
                profileQueryMs = profileQueryMs,
                activitiesQueryMs = activitiesMs,
                // Favourites are page-1 only on the hot path now; the rest loads
                // lazily from the Favorites tab, so there's no fan-out to time here.
                favoritesTotalMs = 0L,
                favoritesFirstPageMs = 0L,
                favoritesRestMs = 0L,
                favoritesPageCount = 1
            )
            profile to timings
        }
    }

    override suspend fun refreshProfile(username: String, forceNetwork: Boolean): Result<Unit> {
        return when (val r = refreshProfileTimed(username, forceNetwork)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> r
        }
    }

    override suspend fun refreshProfileTimed(
        username: String,
        forceNetwork: Boolean
    ): Result<ProfileRefreshTimings> {
        return when (val result = fetchUserProfileTimed(username, forceNetwork)) {
            is Result.Success -> {
                userProfileDao.insert(result.data.first.toEntity())
                Result.Success(result.data.second)
            }
            is Result.Error -> result
        }
    }

    override suspend fun updateAbout(about: String): Result<Unit> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.UpdateAboutMutation(about = Optional.present(encodeForAniList(about)))
            ).execute()

            if (response.hasErrors()) {
                throw Exception(response.errors?.first()?.message ?: "Failed to update profile")
            }

            // Refresh profile to update local cache
            refreshProfile("")
        }
    }

    private suspend fun fetchFavoritesTimed(
        userId: Int,
        policy: FetchPolicy = FetchPolicy.NetworkOnly,
        // Page 1 served from the calling `GetUserProfile` response when the
        // caller already paid for it inline. `null` falls back to issuing
        // `GetUserFavoritesQuery(page=1)` separately.
        seedPageOne: FavoritesPage? = null
    ): FavoritesResult {
        // Fetch page 1 sequentially to learn `lastPage`, then fan out the
        // remaining pages with a bounded Semaphore so the refresh latency is
        // O(slowest page) instead of O(total pages).
        val firstPageStart = SystemClock.elapsedRealtime()
        val firstPage = if (seedPageOne != null) {
            seedPageOne
        } else {
            try {
                Trace.beginSection("AniSync.Profile.Query.Favorites.Page1")
                try {
                    fetchFavoritesPage(userId, 1, policy)
                } finally {
                    Trace.endSection()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileRepository", "Error fetching favorites page 1", e)
                return FavoritesResult(emptyList(), 0, SystemClock.elapsedRealtime() - firstPageStart, 0L)
            }
        }
        // Seeded calls cost zero ms; only count real network time.
        val firstPageMs = if (seedPageOne != null) 0L else SystemClock.elapsedRealtime() - firstPageStart

        val lastPage = firstPage.lastPage ?: 1
        if (lastPage <= 1) {
            return FavoritesResult(firstPage.entries, 1, firstPageMs, 0L)
        }

        val concurrencyLimit = Semaphore(permits = 4)
        val restStart = SystemClock.elapsedRealtime()
        val rest = coroutineScope {
            (2..lastPage).map { page ->
                async {
                    concurrencyLimit.withPermit {
                        Trace.beginSection("AniSync.Profile.Query.Favorites.Page$page")
                        try {
                            fetchFavoritesPage(userId, page, policy).entries
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "ProfileRepository",
                                "Error fetching favorites page $page",
                                e
                            )
                            emptyList()
                        } finally {
                            Trace.endSection()
                        }
                    }
                }
            }.awaitAll()
        }
        val restMs = SystemClock.elapsedRealtime() - restStart

        val total = ArrayList<LibraryEntry>(firstPage.entries.size + rest.sumOf { it.size })
        total.addAll(firstPage.entries)
        rest.forEach(total::addAll)
        return FavoritesResult(total, lastPage, firstPageMs, restMs)
    }

    private data class FavoritesResult(
        val entries: List<LibraryEntry>,
        val pageCount: Int,
        val firstPageMs: Long,
        val restMs: Long
    )

    private data class FavoritesPage(val entries: List<LibraryEntry>, val lastPage: Int?)

    private suspend fun fetchFavoritesPage(
        userId: Int,
        page: Int,
        policy: FetchPolicy
    ): FavoritesPage {
        val response = apolloClient.query(
            GetUserFavoritesQuery(userId = Optional.present(userId), page = Optional.present(page))
        )
            .fetchPolicy(policy)
            .execute()

        val data = response.data?.User?.favourites?.anime
        val nodes = data?.nodes?.filterNotNull() ?: emptyList()
        val pageInfo = data?.pageInfo

        val entries = nodes.map { node ->
            val m = node.mediaCardFields
            LibraryEntry(
                id = 0,
                mediaId = m.id,
                titleRomaji = m.title?.romaji,
                titleEnglish = m.title?.english,
                titleNative = m.title?.native,
                titleUserPreferred = m.title?.userPreferred ?: "Unknown",
                coverUrl = m.coverImage?.large,
                cover = com.anisync.android.domain.CoverImage.of(
                    m.coverImage?.medium,
                    m.coverImage?.large,
                    m.coverImage?.extraLarge
                ),
                progress = 0,
                totalEpisodes = m.episodes,
                totalChapters = m.chapters,
                totalVolumes = m.volumes,
                type = m.type,
                status = LibraryStatus.UNKNOWN
            )
        }

        return FavoritesPage(entries = entries, lastPage = pageInfo?.lastPage)
    }

    override suspend fun getFavoriteAnime(userId: Int, policy: CachePolicy): Result<List<LibraryEntry>> =
        dedupe("favAnime:$userId:$policy") {
            safeApiCall {
                // Page 1 + bounded fan-out for the remaining pages. Invoked only when
                // the Favorites tab opens, never on the profile load/refresh hot path.
                fetchFavoritesTimed(userId, policy.toFetchPolicy(), seedPageOne = null).entries
            }
        }

    override suspend fun getSocialData(
        userId: Int,
        page: Int,
        policy: CachePolicy
    ): Result<com.anisync.android.domain.UserSocialPage> = dedupe("social:$userId:$page:$policy") {
        safeApiCall {
            val response = apolloClient.query(
                com.anisync.android.GetUserSocialDataQuery(
                    userId = userId,
                    page = Optional.present(page)
                )
            )
            .fetchPolicy(policy.toFetchPolicy())
            .execute()

            if (response.hasErrors()) {
                throw Exception(response.errors?.first()?.message ?: "Unknown API Error")
            }

            val data = response.data ?: throw Exception("Empty response data")

            val following = data.following?.following?.filterNotNull()?.map {
                com.anisync.android.domain.SocialUser(
                    id = it.id,
                    name = it.name,
                    avatarUrl = it.avatar?.large
                )
            } ?: emptyList()

            val followers = data.followers?.followers?.filterNotNull()?.map {
                com.anisync.android.domain.SocialUser(
                    id = it.id,
                    name = it.name,
                    avatarUrl = it.avatar?.large
                )
            } ?: emptyList()

            val threads = data.threads?.threads?.filterNotNull()?.map { thread ->
                com.anisync.android.domain.ForumThread(
                    id = thread.id,
                    title = thread.title ?: "",
                    body = null,
                    replyCount = thread.replyCount ?: 0,
                    viewCount = thread.viewCount ?: 0,
                    likeCount = thread.likeCount ?: 0,
                    isLiked = thread.isLiked ?: false,
                    isSubscribed = thread.isSubscribed ?: false,
                    isLocked = thread.isLocked ?: false,
                    isSticky = thread.isSticky ?: false,
                    authorId = thread.user?.id ?: 0,
                    authorName = thread.user?.name ?: "Unknown",
                    authorAvatarUrl = thread.user?.avatar?.large,
                    repliedAt = thread.repliedAt?.toLong(),
                    replyUserName = thread.replyUser?.name,
                    replyUserAvatarUrl = thread.replyUser?.avatar?.large,
                    replyCommentId = thread.replyCommentId,
                    categories = thread.categories?.filterNotNull()?.map {
                        com.anisync.android.domain.ForumCategory(
                            id = it.id,
                            name = it.name ?: ""
                        )
                    } ?: emptyList(),
                    createdAt = thread.createdAt.toLong(),
                    updatedAt = thread.updatedAt.toLong(),
                    siteUrl = thread.siteUrl,
                    mediaTitle = null,
                    mediaCoverUrl = null
                )
            } ?: emptyList()

            val comments = data.comments?.threadComments?.filterNotNull()?.map { comment ->
                com.anisync.android.domain.SocialThreadComment(
                    id = comment.id,
                    threadId = comment.thread?.id ?: 0,
                    threadTitle = comment.thread?.title ?: "",
                    commentHtml = comment.comment,
                    likeCount = comment.likeCount,
                    isLiked = comment.isLiked ?: false,
                    createdAt = comment.createdAt.toLong(),
                    authorId = comment.user?.id ?: 0,
                    authorName = comment.user?.name ?: "Unknown",
                    authorAvatarUrl = comment.user?.avatar?.large
                )
            } ?: emptyList()

            com.anisync.android.domain.UserSocialPage(
                data = com.anisync.android.domain.UserSocialData(
                    following = following,
                    followers = followers,
                    threads = threads,
                    comments = comments
                ),
                followingHasNextPage = data.following?.pageInfo?.hasNextPage == true,
                followersHasNextPage = data.followers?.pageInfo?.hasNextPage == true,
                threadsHasNextPage = data.threads?.pageInfo?.hasNextPage == true,
                commentsHasNextPage = data.comments?.pageInfo?.hasNextPage == true
            )
        }
    }

    override suspend fun getFollowState(userId: Int, policy: CachePolicy): Result<Boolean> =
        dedupe("follow:$userId:$policy") {
            safeApiCall {
                val response = apolloClient.query(GetUserFollowStateQuery(userId = userId))
                    .fetchPolicy(policy.toFetchPolicy())
                    .execute()
                if (response.hasErrors()) {
                    throw Exception(
                        response.errors?.firstOrNull()?.message ?: "Failed to get follow state"
                    )
                }

                response.data?.User?.isFollowing ?: false
            }
        }

    override suspend fun toggleFollow(userId: Int): Result<Boolean> {
        return safeApiCall {
            val response = apolloClient.mutation(ToggleUserFollowMutation(userId = userId)).execute()
            if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to toggle follow")
            }

            response.data?.ToggleFollow?.isFollowing ?: false
        }
    }

    override suspend fun getUserReviews(
        userId: Int,
        page: Int,
        policy: CachePolicy
    ): Result<com.anisync.android.domain.UserReviewsPage> = dedupe("reviews:$userId:$page:$policy") {
        safeApiCall {
            val response = apolloClient.query(
                com.anisync.android.GetUserReviewsQuery(
                    userId = userId,
                    page = Optional.present(page)
                )
            )
            .fetchPolicy(policy.toFetchPolicy())
            .execute()

            if (response.hasErrors()) {
                throw Exception(response.errors?.first()?.message ?: "Failed to fetch user reviews")
            }

            val data = response.data ?: throw Exception("Empty response data")

            val reviews = data.Page?.reviews?.filterNotNull()?.map { review ->
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
                    mediaId = review.media?.id,
                    mediaTitle = review.media?.title?.userPreferred,
                    mediaCoverUrl = review.media?.coverImage?.large,
                    mediaCover = com.anisync.android.domain.CoverImage.of(review.media?.coverImage?.medium, review.media?.coverImage?.large, review.media?.coverImage?.extraLarge),
                    mediaBannerUrl = review.media?.bannerImage
                )
            } ?: emptyList()

            com.anisync.android.domain.UserReviewsPage(
                reviews = reviews,
                hasNextPage = data.Page?.pageInfo?.hasNextPage == true
            )
        }
    }

    override suspend fun getUserActivitiesPage(
        userId: Int,
        page: Int,
        policy: CachePolicy
    ): Result<com.anisync.android.domain.UserActivitiesPage> =
        dedupe("activitiesPage:$userId:$page:$policy") {
            safeApiCall {
                val response = apolloClient.query(
                    com.anisync.android.GetUserActivitiesPageQuery(
                        userId = Optional.present(userId),
                        page = page
                    )
                )
                    .fetchPolicy(policy.toFetchPolicy())
                    .execute()

                if (response.hasErrors()) {
                    throw Exception(
                        response.errors?.firstOrNull()?.message ?: "Failed to load activities"
                    )
                }

                val pageData = response.data?.Page
                val activities = pageData?.activities
                    ?.filterNotNull()
                    ?.mapNotNull { it.activityFields.activityFieldsToDomain() }
                    ?.distinctBy { it.id }
                    ?: emptyList()

                com.anisync.android.domain.UserActivitiesPage(
                    activities = activities,
                    hasNextPage = pageData?.pageInfo?.hasNextPage == true,
                    currentPage = pageData?.pageInfo?.currentPage ?: page
                )
            }
        }

    override suspend fun getUserAnimeList(username: String): Result<List<LibraryEntry>> =
        dedupe("animeList:$username") {
            fetchUserList(username, com.anisync.android.type.MediaType.ANIME)
        }

    override suspend fun getUserMangaList(username: String): Result<List<LibraryEntry>> =
        dedupe("mangaList:$username") {
            fetchUserList(username, com.anisync.android.type.MediaType.MANGA)
        }

    private suspend fun fetchUserList(username: String, type: com.anisync.android.type.MediaType): Result<List<LibraryEntry>> {
        return safeApiCall {
            val query = com.anisync.android.GetUserLibraryQuery(username = username, type = type)

            val response = apolloClient.query(query)
                .fetchPolicy(FetchPolicy.CacheFirst)
                .execute()
                .takeIf { it.data?.MediaListCollection != null }
                ?: apolloClient.query(query)
                    .fetchPolicy(FetchPolicy.NetworkOnly)
                    .execute()

            val lists = response.data?.MediaListCollection?.lists?.filterNotNull() ?: emptyList()
            val entryMap = HashMap<Int, LibraryEntry>(lists.sumOf { it.entries?.size ?: 0 })

            lists.forEach { group ->
                val listName = group.name ?: return@forEach
                val isCustom = group.isCustomList ?: false

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
                            updatedAt = entry.updatedAt?.toLong()?.times(1000L),
                            createdAt = entry.createdAt?.toLong()?.times(1000L),
                            customLists = if (isCustom) listOf(listName) else emptyList(),
                            isPrivate = entry.`private` ?: false,
                            hiddenFromStatusLists = entry.hiddenFromStatusLists ?: false
                        )
                    } else if (isCustom && !existing.customLists.contains(listName)) {
                        entryMap[entryId] = existing.copy(customLists = existing.customLists + listName)
                    }
                }
            }

            entryMap.values.sortedByDescending { it.updatedAt }
        }
    }

    override suspend fun sendMessageActivity(
        recipientId: Int,
        message: String,
        isPrivate: Boolean,
        id: Int?
    ): Result<Unit> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.SaveMessageActivityMutation(
                    id = if (id != null) Optional.present(id) else Optional.absent(),
                    recipientId = recipientId,
                    message = encodeForAniList(message),
                    `private` = Optional.present(isPrivate)
                )
            ).execute()
            if (response.hasErrors()) {
                throw Exception(
                    response.errors?.firstOrNull()?.message ?: "Failed to send message"
                )
            }
            Unit
        }
    }
}
