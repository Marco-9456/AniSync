package com.anisync.android.data

import com.anisync.android.GetUserProfileQuery
import com.anisync.android.GetViewerQuery
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.UserProfile
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : ProfileRepository {

    override suspend fun getProfile(username: String): UserProfile? {
        // If no username provided, get current authenticated user
        val actualUsername = if (username.isBlank()) {
            val viewerResponse = apolloClient.query(GetViewerQuery()).execute()
            viewerResponse.data?.Viewer?.name ?: return null
        } else {
            username
        }
        
        val response = apolloClient.query(
            GetUserProfileQuery(name = Optional.present(actualUsername))
        ).execute()

        val user = response.data?.User ?: return null

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
                status = LibraryStatus.UNKNOWN
            )
        } ?: emptyList()

        return UserProfile(
            id = user.id ?: 0,
            name = user.name ?: "Unknown",
            avatarUrl = user.avatar?.large,
            bannerUrl = user.bannerImage,
            animeCount = stats?.count ?: 0,
            daysWatched = daysWatched,
            mangaCount = mangaStats?.count ?: 0,
            chaptersRead = mangaStats?.chaptersRead ?: 0,
            meanScore = stats?.meanScore?.toFloat() ?: 0f,
            favoriteAnime = favorites
        )
    }
}
