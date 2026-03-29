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

            val activities = activitiesResponse.data?.Page?.activities?.filterNotNull()?.mapNotNull { it.onListActivity }?.map { activity ->
                com.anisync.android.domain.UserActivity(
                    id = activity.id ?: 0,
                    status = activity.status,
                    progress = activity.progress,
                    mediaTitle = activity.media?.title?.userPreferred ?: "Unknown",
                    mediaCoverUrl = activity.media?.coverImage?.medium,
                    timestamp = (activity.createdAt?.toLong() ?: 0L) * 1000L,
                    mediaScore = null
                )
            } ?: emptyList()

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

            // Fetch all favorites recursively
            val favorites = fetchFavorites(user.id ?: 0)

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
                activities = activities
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
