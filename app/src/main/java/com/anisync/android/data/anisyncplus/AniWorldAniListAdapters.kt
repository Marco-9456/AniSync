package com.anisync.android.data.anisyncplus

import com.anisync.android.SearchMediaQuery
import com.anisync.android.data.account.AccountStore
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import de.mrxxxxx.anisyncplus.calendar.api.AniListLibraryStateProvider
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldMatchCandidateProvider
import de.mrxxxxx.anisyncplus.calendar.domain.AniListMatchCandidate
import de.mrxxxxx.anisyncplus.calendar.domain.AniListUserMediaState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AniWorldAniListCandidateProvider @Inject constructor(
    private val apolloClient: ApolloClient
) : AniWorldMatchCandidateProvider {
    override suspend fun candidatesFor(rawTitle: String): List<AniListMatchCandidate> {
        val response = apolloClient.query(
            SearchMediaQuery(
                search = Optional.present(rawTitle),
                type = Optional.present(MediaType.ANIME),
                page = Optional.present(1),
                perPage = Optional.present(MAX_CANDIDATES)
            )
        ).fetchPolicy(FetchPolicy.NetworkOnly).execute()
        if (response.hasErrors()) {
            throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "AniList candidate search failed")
        }
        return response.data?.Page?.media.orEmpty().filterNotNull().mapNotNull { media ->
            val mediaId = media.id ?: return@mapNotNull null
            AniListMatchCandidate(
                mediaId = mediaId,
                titleUserPreferred = media.title?.userPreferred ?: return@mapNotNull null,
                titleEnglish = media.title?.english,
                titleRomaji = media.title?.romaji,
                titleNative = media.title?.native,
                synonyms = media.synonyms?.filterNotNull().orEmpty(),
                coverImageUrl = media.coverImage?.extraLarge
                    ?: media.coverImage?.large
                    ?: media.coverImage?.medium,
                averageScore = media.averageScore
            )
        }
    }

    private companion object {
        const val MAX_CANDIDATES = 10
    }
}

class AniWorldLibraryStateProvider @Inject constructor(
    private val accountStore: AccountStore,
    private val libraryDao: LibraryDao
) : AniListLibraryStateProvider {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeStates(): Flow<Map<Int, AniListUserMediaState>> =
        accountStore.activeAccount.flatMapLatest { account ->
            libraryDao.observeByType(account?.id ?: NO_OWNER, MediaType.ANIME)
        }.map { entries ->
            entries.associate { entry ->
                entry.mediaId to AniListUserMediaState(
                    mediaId = entry.mediaId,
                    status = entry.status.name,
                    progress = entry.progress
                )
            }
        }

    private companion object {
        const val NO_OWNER = -1
    }
}
