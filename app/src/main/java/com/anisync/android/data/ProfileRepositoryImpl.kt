package com.anisync.android.data

import com.anisync.android.GetUserActivitiesQuery
import com.anisync.android.GetUserFavoritesQuery
import com.anisync.android.GetUserFollowStateQuery
import com.anisync.android.GetUserProfileQuery
import com.anisync.android.GetViewerQuery
import com.anisync.android.ToggleUserFollowMutation
import com.anisync.android.data.local.dao.UserProfileDao
import com.anisync.android.data.local.toDomain
import com.anisync.android.data.local.toEntity
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.UserProfile
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val userProfileDao: UserProfileDao
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> {
        return userProfileDao.observe()
            .map { entity -> entity?.toDomain() }
    }

    override suspend fun fetchUserProfile(username: String): Result<UserProfile> {
        return safeApiCall {
            val actualUsername = username.ifBlank {
                val viewerResponse = apolloClient.query(GetViewerQuery())
                    .fetchPolicy(FetchPolicy.NetworkOnly)
                    .execute()
                viewerResponse.data?.Viewer?.name
                    ?: throw Exception("Unable to get current user")
            }

            val response = apolloClient.query(
                GetUserProfileQuery(name = Optional.present(actualUsername))
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            if (response.hasErrors()) {
                val firstError = response.errors?.firstOrNull()?.message
                throw Exception(firstError ?: "Failed to load user profile")
            }

            val user = response.data?.User
                ?: throw Exception("User not found: @$actualUsername")

            // Fetch Activities
            val activitiesResponse = apolloClient.query(
                GetUserActivitiesQuery(userId = Optional.present(user.id))
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            val rawActivities = mutableListOf<com.anisync.android.fragment.ActivityFields>()
            
            activitiesResponse.data?.allActivities?.activities?.filterNotNull()?.forEach { 
                rawActivities.add(it.activityFields) 
            }
            activitiesResponse.data?.textActivities?.activities?.filterNotNull()?.forEach { 
                rawActivities.add(it.activityFields) 
            }
            activitiesResponse.data?.messageReceived?.activities?.filterNotNull()?.forEach { 
                rawActivities.add(it.activityFields) 
            }
            activitiesResponse.data?.messageSent?.activities?.filterNotNull()?.forEach { 
                rawActivities.add(it.activityFields) 
            }

            val activities = rawActivities.mapNotNull { activityFields ->
                when {
                    activityFields.onListActivity != null -> {
                        val l = activityFields.onListActivity
                        val lastReply = l.replies?.lastOrNull()
                        com.anisync.android.domain.UserActivity(
                            id = l.id ?: 0,
                            type = com.anisync.android.domain.ActivityType.MEDIA_LIST,
                            status = l.status,
                            progress = l.progress,
                            mediaTitle = l.media?.title?.userPreferred ?: "Unknown",
                            mediaCoverUrl = l.media?.coverImage?.medium,
                            timestamp = (l.createdAt?.toLong() ?: 0L) * 1000L,
                            mediaScore = null,
                            userName = l.user?.name,
                            userAvatarUrl = l.user?.avatar?.medium,
                            replyUserName = lastReply?.user?.name,
                            replyUserAvatarUrl = lastReply?.user?.avatar?.medium,
                            repliedAt = lastReply?.createdAt?.toLong(),
                            likeCount = l.likeCount ?: 0,
                            replyCount = l.replyCount ?: 0,
                            isLiked = l.isLiked == true,
                            isLocked = l.isLocked == true,
                            isSubscribed = l.isSubscribed == true
                        )
                    }
                    activityFields.onTextActivity != null -> {
                        val t = activityFields.onTextActivity
                        val lastReply = t.replies?.lastOrNull()
                        com.anisync.android.domain.UserActivity(
                            id = t.id ?: 0,
                            type = com.anisync.android.domain.ActivityType.TEXT,
                            text = t.text,
                            timestamp = (t.createdAt?.toLong() ?: 0L) * 1000L,
                            userName = t.user?.name,
                            userAvatarUrl = t.user?.avatar?.medium,
                            likeCount = t.likeCount ?: 0,
                            replyCount = t.replyCount ?: 0,
                            isLiked = t.isLiked == true,
                            isLocked = t.isLocked == true,
                            isSubscribed = t.isSubscribed == true,
                            isPinned = t.isPinned == true,
                            replyUserName = lastReply?.user?.name,
                            replyUserAvatarUrl = lastReply?.user?.avatar?.medium,
                            repliedAt = lastReply?.createdAt?.toLong()
                        )
                    }
                    activityFields.onMessageActivity != null -> {
                        val m = activityFields.onMessageActivity
                        val lastReply = m.replies?.lastOrNull()
                        com.anisync.android.domain.UserActivity(
                            id = m.id ?: 0,
                            type = com.anisync.android.domain.ActivityType.MESSAGE,
                            text = m.message,
                            timestamp = (m.createdAt?.toLong() ?: 0L) * 1000L,
                            userName = m.messenger?.name,
                            userAvatarUrl = m.messenger?.avatar?.medium,
                            recipientName = m.recipient?.name,
                            recipientAvatarUrl = m.recipient?.avatar?.medium,
                            likeCount = m.likeCount ?: 0,
                            replyCount = m.replyCount ?: 0,
                            isLiked = m.isLiked == true,
                            isLocked = m.isLocked == true,
                            isSubscribed = m.isSubscribed == true,
                            isPrivate = m.isPrivate == true,
                            replyUserName = lastReply?.user?.name,
                            replyUserAvatarUrl = lastReply?.user?.avatar?.medium,
                            repliedAt = lastReply?.createdAt?.toLong()
                        )
                    }
                    else -> null
                }
            }
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }

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

            // Fetch all favorites recursively
            val favorites = fetchFavorites(user.id ?: 0)
            
                    val overviewManga = user.favourites?.manga?.nodes?.filterNotNull()?.map { media ->
                        com.anisync.android.domain.LibraryEntry(
                            id = 0,
                            mediaId = media.id,
                            titleRomaji = media.title?.romaji,
                            titleEnglish = media.title?.english,
                            titleNative = media.title?.native,
                            titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                            coverUrl = media.coverImage?.large,
                            progress = 0,
                            totalEpisodes = null,
                            totalChapters = media.chapters,
                            totalVolumes = media.volumes,
                            type = media.type,
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

            UserProfile(
                id = user.id ?: 0,
                name = user.name ?: "Unknown",
                avatarUrl = user.avatar?.large,
                bannerUrl = user.bannerImage,
                about = user.about,
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
        }
    }

    override suspend fun refreshProfile(username: String): Result<Unit> {
        return when (val result = fetchUserProfile(username)) {
            is Result.Success -> {
                userProfileDao.insert(result.data.toEntity())
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    override suspend fun updateAbout(about: String): Result<Unit> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.UpdateAboutMutation(about = Optional.present(about))
            ).execute()

            if (response.hasErrors()) {
                throw Exception(response.errors?.first()?.message ?: "Failed to update profile")
            }

            // Refresh profile to update local cache
            refreshProfile("")
        }
    }

    private suspend fun fetchFavorites(userId: Int): List<LibraryEntry> {
        val allFavorites = mutableListOf<LibraryEntry>()
        var page = 1
        var hasNextPage = true

        try {
            while (hasNextPage) {
                val response = apolloClient.query(
                    GetUserFavoritesQuery(userId = Optional.present(userId), page = Optional.present(page))
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

                val data = response.data?.User?.favourites?.anime
                val nodes = data?.nodes?.filterNotNull() ?: emptyList()
                val pageInfo = data?.pageInfo

                val entries = nodes.map { media ->
                    LibraryEntry(
                        id = 0, // Not a full library entry, just a favorite
                        mediaId = media.id ?: 0,
                        titleRomaji = media.title?.romaji,
                        titleEnglish = media.title?.english,
                        titleNative = media.title?.native,
                        titleUserPreferred = media.title?.userPreferred ?: "Unknown",
                        coverUrl = media.coverImage?.large,
                        progress = 0,
                        totalEpisodes = media.episodes,
                        totalChapters = media.chapters,
                        totalVolumes = media.volumes,
                        type = media.type,
                        status = LibraryStatus.UNKNOWN
                    )
                }
                
                allFavorites.addAll(entries)
                
                hasNextPage = pageInfo?.hasNextPage == true
                page++
            }
        } catch (e: Exception) {
            // Log error but return what we have so far
            android.util.Log.e("ProfileRepository", "Error fetching favorites page $page", e)
        }

        return allFavorites
    }

    override suspend fun getSocialData(userId: Int, page: Int): Result<com.anisync.android.domain.UserSocialPage> {
        return safeApiCall {
            val response = apolloClient.query(
                com.anisync.android.GetUserSocialDataQuery(
                    userId = userId,
                    page = Optional.present(page)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
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

    override suspend fun getFollowState(userId: Int): Result<Boolean> {
        return safeApiCall {
            val response = apolloClient.query(GetUserFollowStateQuery(userId = userId))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
            if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to get follow state")
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

    override suspend fun getUserReviews(userId: Int, page: Int): Result<com.anisync.android.domain.UserReviewsPage> {
        return safeApiCall {
            val response = apolloClient.query(
                com.anisync.android.GetUserReviewsQuery(
                    userId = userId,
                    page = Optional.present(page)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
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
                    mediaTitle = review.media?.title?.userPreferred,
                    mediaCoverUrl = review.media?.coverImage?.large,
                    mediaBannerUrl = review.media?.bannerImage
                )
            } ?: emptyList()

            com.anisync.android.domain.UserReviewsPage(
                reviews = reviews,
                hasNextPage = data.Page?.pageInfo?.hasNextPage == true
            )
        }
    }

    override suspend fun getUserAnimeList(username: String): Result<List<LibraryEntry>> {
        return fetchUserList(username, com.anisync.android.type.MediaType.ANIME)
    }

    override suspend fun getUserMangaList(username: String): Result<List<LibraryEntry>> {
        return fetchUserList(username, com.anisync.android.type.MediaType.MANGA)
    }

    private suspend fun fetchUserList(username: String, type: com.anisync.android.type.MediaType): Result<List<LibraryEntry>> {
        return safeApiCall {
            val query = com.anisync.android.GetUserLibraryQuery(username = username, type = type)

            val cacheResponse = apolloClient.query(query)
                .fetchPolicy(FetchPolicy.CacheFirst)
                .execute()

            val networkResponse = apolloClient.query(query)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val response = when {
                networkResponse.data?.MediaListCollection != null -> networkResponse
                cacheResponse.data?.MediaListCollection != null -> cacheResponse
                else -> networkResponse
            }

            val lists = response.data?.MediaListCollection?.lists?.filterNotNull() ?: emptyList()
            val entryMap = mutableMapOf<Int, LibraryEntry>()

            lists.forEach { group ->
                val listName = group.name ?: return@forEach
                val isCustom = group.isCustomList ?: false
                
                group.entries?.filterNotNull()?.forEach { entry ->
                    val entryId = entry.id ?: return@forEach
                    val media = entry.media
                    val existing = entryMap[entryId]

                    if (existing == null) {
                        val status = entry.status?.let {
                            when (it.name) {
                                "CURRENT" -> LibraryStatus.CURRENT
                                "PLANNING" -> LibraryStatus.PLANNING
                                "COMPLETED" -> LibraryStatus.COMPLETED
                                "DROPPED" -> LibraryStatus.DROPPED
                                "PAUSED" -> LibraryStatus.PAUSED
                                "REPEATING" -> LibraryStatus.REPEATING
                                else -> LibraryStatus.UNKNOWN
                            }
                        } ?: LibraryStatus.UNKNOWN
                        
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
            
            entryMap.values.toList().sortedByDescending { it.updatedAt }
        }
    }

    override suspend fun sendMessageActivity(
        recipientId: Int,
        message: String,
        isPrivate: Boolean
    ): Result<Unit> {
        return safeApiCall {
            val response = apolloClient.mutation(
                com.anisync.android.SaveMessageActivityMutation(
                    recipientId = recipientId,
                    message = message,
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
