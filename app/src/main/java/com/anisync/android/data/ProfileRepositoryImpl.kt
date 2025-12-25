package com.anisync.android.data

import com.anisync.android.GetUserProfileQuery
import com.anisync.android.GetUserActivitiesQuery
import com.anisync.android.GetViewerQuery
import com.anisync.android.data.local.dao.UserProfileDao
import com.anisync.android.data.local.toDomain
import com.anisync.android.data.local.toEntity
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.UserProfile
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
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
        return try {
            val actualUsername = if (username.isBlank()) {
                val viewerResponse = apolloClient.query(GetViewerQuery()).execute()
                viewerResponse.data?.Viewer?.name
                    ?: return Result.Error("Unable to get current user")
            } else {
                username
            }

            val response = apolloClient.query(
                GetUserProfileQuery(name = Optional.present(actualUsername))
            ).execute()

            val user = response.data?.User
                ?: return Result.Error("User not found")

            // Fetch Activities
            val activitiesResponse = apolloClient.query(
                GetUserActivitiesQuery(userId = Optional.present(user.id))
            ).execute()

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

            val favorites = user.favourites?.anime?.nodes?.filterNotNull()?.map { media ->
                LibraryEntry(
                    id = 0,
                    mediaId = media.id ?: 0,
                    title = media.title?.userPreferred ?: "Unknown",
                    coverUrl = media.coverImage?.large,
                    progress = 0,
                    totalEpisodes = null,
                    totalChapters = null,
                    totalVolumes = null,
                    type = null,
                    status = LibraryStatus.UNKNOWN
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
                favoriteAnime = favorites,
                activities = activities
            )

            userProfileDao.insert(profile.toEntity())
            Result.Success(Unit)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }
}
