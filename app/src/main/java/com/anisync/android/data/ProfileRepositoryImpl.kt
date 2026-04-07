package com.anisync.android.data

import com.anisync.android.GetUserActivitiesQuery
import com.anisync.android.GetUserFavoritesQuery
import com.anisync.android.GetUserProfileQuery
import com.anisync.android.GetViewerQuery
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

    override suspend fun refreshProfile(username: String): Result<Unit> {
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

            val user = response.data?.User
                ?: throw Exception("User not found")

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
                        com.anisync.android.domain.UserActivity(
                            id = activityFields.onListActivity.id ?: 0,
                            type = com.anisync.android.domain.ActivityType.MEDIA_LIST,
                            status = activityFields.onListActivity.status,
                            progress = activityFields.onListActivity.progress,
                            mediaTitle = activityFields.onListActivity.media?.title?.userPreferred ?: "Unknown",
                            mediaCoverUrl = activityFields.onListActivity.media?.coverImage?.medium,
                            timestamp = (activityFields.onListActivity.createdAt?.toLong() ?: 0L) * 1000L,
                            mediaScore = null
                        )
                    }
                    activityFields.onTextActivity != null -> {
                        com.anisync.android.domain.UserActivity(
                            id = activityFields.onTextActivity.id ?: 0,
                            type = com.anisync.android.domain.ActivityType.TEXT,
                            text = activityFields.onTextActivity.text,
                            timestamp = (activityFields.onTextActivity.createdAt?.toLong() ?: 0L) * 1000L,
                            userName = activityFields.onTextActivity.user?.name,
                            userAvatarUrl = activityFields.onTextActivity.user?.avatar?.medium
                        )
                    }
                    activityFields.onMessageActivity != null -> {
                        com.anisync.android.domain.UserActivity(
                            id = activityFields.onMessageActivity.id ?: 0,
                            type = com.anisync.android.domain.ActivityType.MESSAGE,
                            text = activityFields.onMessageActivity.message,
                            timestamp = (activityFields.onMessageActivity.createdAt?.toLong() ?: 0L) * 1000L,
                            userName = activityFields.onMessageActivity.messenger?.name,
                            userAvatarUrl = activityFields.onMessageActivity.messenger?.avatar?.medium,
                            recipientName = activityFields.onMessageActivity.recipient?.name
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

            val profile = UserProfile(
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
                favoriteStaffOverview = overviewStaff
            )

            userProfileDao.insert(profile.toEntity())
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
}
