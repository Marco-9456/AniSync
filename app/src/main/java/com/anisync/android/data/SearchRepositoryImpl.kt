package com.anisync.android.data

import com.anisync.android.GetSearchTaxonomyQuery
import com.anisync.android.SearchAllQuery
import com.anisync.android.SearchEverythingQuery
import com.anisync.android.SearchMediaQuery
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaTag
import com.anisync.android.domain.Result
import com.anisync.android.domain.map
import com.anisync.android.domain.SearchEverythingResult
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchPage
import com.anisync.android.domain.SearchRepository
import com.anisync.android.domain.SearchResult
import com.anisync.android.type.MediaType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val appSettings: AppSettings,
) : SearchRepository {

    /**
     * Resolves the `isAdult` query filter. The per-search [AdultMode] chip takes precedence; when it
     * is [AdultMode.ANY] (the default) the global "show adult content" preference acts as a floor, so
     * a user with adult content off never sees 18+ results unless they explicitly ask for them via
     * the chip. This is the fix for the previously-inert toggle.
     */
    private fun resolveAdultFilter(mode: com.anisync.android.domain.AdultMode): Optional<Boolean?> =
        resolveAdultIsAdult(mode, appSettings.showAdultContent.value)
            ?.let { Optional.present(it) } ?: Optional.absent()

    @Volatile private var cachedGenres: List<String>? = null
    @Volatile private var cachedTags: List<MediaTag>? = null

    /**
     * In-flight dedup keyed by query + filters. A re-trigger of the same search
     * (rotation, refocus, rapid identical input) suspends on the mutex and then
     * hits the now-warm normalized cache instead of issuing a second request.
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

    override suspend fun searchMedia(
        query: String,
        type: MediaType,
        filters: SearchFilters,
        page: Int,
        perPage: Int,
        countOnly: Boolean
    ): Result<SearchPage> {
        return safeApiCall {
            val response = apolloClient.query(
                SearchMediaQuery(
                    search = if (query.isBlank()) Optional.absent() else Optional.present(query),
                    type = Optional.present(type),
                    page = Optional.present(page),
                    perPage = Optional.present(perPage),
                    sort = Optional.present(listOf(filters.sort.apiValue)),
                    genre_in = filters.genresIncluded.toOptionalList(),
                    genre_not_in = filters.genresExcluded.toOptionalList(),
                    tag_in = filters.tagsIncluded.toOptionalList(),
                    tag_not_in = filters.tagsExcluded.toOptionalList(),
                    format_in = filters.formats.toOptionalList(),
                    status_in = filters.statuses.toOptionalList(),
                    source_in = filters.sources.toOptionalList(),
                    startDate_greater = filters.yearRange.min?.let { Optional.present(it * 10000 + 101) }
                        ?: Optional.absent(),
                    startDate_lesser = filters.yearRange.max?.let { Optional.present(it * 10000 + 1231) }
                        ?: Optional.absent(),
                    season = filters.season?.let { Optional.present(it) } ?: Optional.absent(),
                    averageScore = filters.score.exact(),
                    averageScore_greater = filters.score.greater(),
                    averageScore_lesser = filters.score.lesser(),
                    episodes = filters.episodes.exact(),
                    episodes_greater = filters.episodes.greater(),
                    episodes_lesser = filters.episodes.lesser(),
                    chapters = filters.chapters.exact(),
                    chapters_greater = filters.chapters.greater(),
                    chapters_lesser = filters.chapters.lesser(),
                    countryOfOrigin = filters.country?.let { Optional.present(it.code) }
                        ?: Optional.absent(),
                    isAdult = resolveAdultFilter(filters.adultMode),
                    countOnly = Optional.present(countOnly)
                )
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val pageData = response.data?.Page
            val pageInfo = pageData?.pageInfo
            val entries = if (countOnly) {
                emptyList()
            } else {
                pageData?.media?.filterNotNull()?.map { it.toLibraryEntry() } ?: emptyList()
            }
            SearchPage(
                entries = entries,
                total = pageInfo?.total ?: entries.size,
                hasNextPage = pageInfo?.hasNextPage ?: false,
                currentPage = pageInfo?.currentPage ?: page
            )
        }
    }

    override suspend fun searchAll(query: String): Result<GroupedSearchResults> {
        return safeApiCall {
            val response = apolloClient.query(
                SearchAllQuery(
                    search = Optional.present(query),
                    page = Optional.present(1),
                    perPage = Optional.present(5)
                )
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val data = response.data

            val characters = data?.characters?.characters?.filterNotNull()?.map { c ->
                SearchResult.CharacterResult(
                    id = c.id,
                    displayName = c.name?.userPreferred ?: c.name?.full ?: "Unknown",
                    nativeName = c.name?.native,
                    imageUrl = c.image?.medium,
                    favourites = c.favourites
                )
            } ?: emptyList()

            val staff = data?.staff?.staff?.filterNotNull()?.map { s ->
                SearchResult.StaffResult(
                    id = s.id,
                    displayName = s.name?.userPreferred ?: s.name?.full ?: "Unknown",
                    nativeName = s.name?.native,
                    imageUrl = s.image?.medium,
                    primaryOccupations = s.primaryOccupations?.filterNotNull() ?: emptyList(),
                    favourites = s.favourites
                )
            } ?: emptyList()

            val users = data?.users?.users?.filterNotNull()?.map { u ->
                SearchResult.UserResult(
                    id = u.id,
                    displayName = u.name,
                    imageUrl = u.avatar?.medium
                )
            } ?: emptyList()

            val studios = data?.studios?.studios?.filterNotNull()?.map { s ->
                SearchResult.StudioResult(
                    id = s.id,
                    displayName = s.name,
                    favourites = s.favourites
                )
            } ?: emptyList()

            GroupedSearchResults(
                characters = characters,
                staff = staff,
                users = users,
                studios = studios
            )
        }
    }

    override suspend fun searchEverything(
        query: String,
        filters: SearchFilters,
        perPage: Int,
        wantAnime: Boolean,
        wantManga: Boolean,
        wantEntities: Boolean
    ): Result<SearchEverythingResult> {
        val key = "all:$query:${filters.hashCode()}:$perPage:$wantAnime:$wantManga:$wantEntities"
        return dedupe(key) {
            safeApiCall {
                val response = apolloClient.query(
                    SearchEverythingQuery(
                        search = if (query.isBlank()) Optional.absent() else Optional.present(query),
                        perPage = Optional.present(perPage),
                        wantAnime = wantAnime,
                        wantManga = wantManga,
                        wantEntities = wantEntities,
                        sort = Optional.present(listOf(filters.sort.apiValue)),
                        genre_in = filters.genresIncluded.toOptionalList(),
                        genre_not_in = filters.genresExcluded.toOptionalList(),
                        tag_in = filters.tagsIncluded.toOptionalList(),
                        tag_not_in = filters.tagsExcluded.toOptionalList(),
                        format_in = filters.formats.toOptionalList(),
                        status_in = filters.statuses.toOptionalList(),
                        source_in = filters.sources.toOptionalList(),
                        startDate_greater = filters.yearRange.min?.let { Optional.present(it * 10000 + 101) }
                            ?: Optional.absent(),
                        startDate_lesser = filters.yearRange.max?.let { Optional.present(it * 10000 + 1231) }
                            ?: Optional.absent(),
                        season = filters.season?.let { Optional.present(it) } ?: Optional.absent(),
                        averageScore = filters.score.exact(),
                        averageScore_greater = filters.score.greater(),
                        averageScore_lesser = filters.score.lesser(),
                        episodes = filters.episodes.exact(),
                        episodes_greater = filters.episodes.greater(),
                        episodes_lesser = filters.episodes.lesser(),
                        chapters = filters.chapters.exact(),
                        chapters_greater = filters.chapters.greater(),
                        chapters_lesser = filters.chapters.lesser(),
                        countryOfOrigin = filters.country?.let { Optional.present(it.code) }
                            ?: Optional.absent(),
                        isAdult = resolveAdultFilter(filters.adultMode)
                    )
                )
                    .fetchPolicy(FetchPolicy.NetworkFirst)
                    .execute()

                val data = response.data

                val animeEntries = data?.anime?.media?.filterNotNull()?.map { node ->
                    val m = node.mediaCardFields
                    LibraryEntry(
                        id = 0,
                        mediaId = m.id,
                        titleRomaji = m.title?.romaji,
                        titleEnglish = m.title?.english,
                        titleNative = m.title?.native,
                        titleUserPreferred = m.title?.userPreferred ?: "Unknown",
                        coverUrl = m.coverImage?.extraLarge,
                        cover = com.anisync.android.domain.CoverImage.of(
                            m.coverImage?.medium, m.coverImage?.large, m.coverImage?.extraLarge
                        ),
                        progress = 0,
                        totalEpisodes = m.episodes,
                        totalChapters = m.chapters,
                        totalVolumes = m.volumes,
                        type = m.type,
                        format = m.format,
                        status = LibraryStatus.UNKNOWN,
                        mediaStatus = m.status?.name
                    )
                }.orEmpty()

                val mangaEntries = data?.manga?.media?.filterNotNull()?.map { node ->
                    val m = node.mediaCardFields
                    LibraryEntry(
                        id = 0,
                        mediaId = m.id,
                        titleRomaji = m.title?.romaji,
                        titleEnglish = m.title?.english,
                        titleNative = m.title?.native,
                        titleUserPreferred = m.title?.userPreferred ?: "Unknown",
                        coverUrl = m.coverImage?.extraLarge,
                        cover = com.anisync.android.domain.CoverImage.of(
                            m.coverImage?.medium, m.coverImage?.large, m.coverImage?.extraLarge
                        ),
                        progress = 0,
                        totalEpisodes = m.episodes,
                        totalChapters = m.chapters,
                        totalVolumes = m.volumes,
                        type = m.type,
                        format = m.format,
                        status = LibraryStatus.UNKNOWN,
                        mediaStatus = m.status?.name
                    )
                }.orEmpty()

                val characters = data?.characters?.characters?.filterNotNull()?.map { c ->
                    SearchResult.CharacterResult(
                        id = c.id,
                        displayName = c.name?.userPreferred ?: c.name?.full ?: "Unknown",
                        nativeName = c.name?.native,
                        imageUrl = c.image?.medium,
                        favourites = c.favourites
                    )
                } ?: emptyList()

                val staff = data?.staff?.staff?.filterNotNull()?.map { s ->
                    SearchResult.StaffResult(
                        id = s.id,
                        displayName = s.name?.userPreferred ?: s.name?.full ?: "Unknown",
                        nativeName = s.name?.native,
                        imageUrl = s.image?.medium,
                        primaryOccupations = s.primaryOccupations?.filterNotNull() ?: emptyList(),
                        favourites = s.favourites
                    )
                } ?: emptyList()

                val users = data?.users?.users?.filterNotNull()?.map { u ->
                    SearchResult.UserResult(
                        id = u.id,
                        displayName = u.name,
                        imageUrl = u.avatar?.medium
                    )
                } ?: emptyList()

                val studios = data?.studios?.studios?.filterNotNull()?.map { s ->
                    SearchResult.StudioResult(
                        id = s.id,
                        displayName = s.name,
                        favourites = s.favourites
                    )
                } ?: emptyList()

                SearchEverythingResult(
                    anime = animeEntries,
                    manga = mangaEntries,
                    animeHasNextPage = data?.anime?.pageInfo?.hasNextPage ?: false,
                    mangaHasNextPage = data?.manga?.pageInfo?.hasNextPage ?: false,
                    grouped = GroupedSearchResults(
                        characters = characters,
                        staff = staff,
                        users = users,
                        studios = studios
                    )
                )
            }
        }
    }

    override suspend fun getGenres(): Result<List<String>> {
        cachedGenres?.let { return Result.Success(it) }
        return loadTaxonomy().map { it.first }
    }

    override suspend fun getTags(): Result<List<MediaTag>> {
        cachedTags?.let { return Result.Success(it) }
        return loadTaxonomy().map { it.second }
    }

    private suspend fun loadTaxonomy(): Result<Pair<List<String>, List<MediaTag>>> {
        return safeApiCall {
            val response = apolloClient.query(GetSearchTaxonomyQuery())
                .fetchPolicy(FetchPolicy.CacheFirst)
                .execute()
            val genres = response.data?.GenreCollection?.filterNotNull().orEmpty()
            val tags = response.data?.MediaTagCollection?.filterNotNull()?.map { t ->
                MediaTag(
                    id = t.id,
                    name = t.name,
                    description = t.description,
                    category = t.category,
                    isAdult = t.isAdult ?: false
                )
            }.orEmpty()
            cachedGenres = genres
            cachedTags = tags
            genres to tags
        }
    }

    private fun SearchMediaQuery.Medium.toLibraryEntry(): LibraryEntry = LibraryEntry(
        id = 0,
        mediaId = id ?: 0,
        titleRomaji = title?.romaji,
        titleEnglish = title?.english,
        titleNative = title?.native,
        titleUserPreferred = title?.userPreferred ?: "Unknown",
        coverUrl = coverImage?.extraLarge,
        cover = com.anisync.android.domain.CoverImage.of(
            coverImage?.medium,
            coverImage?.large,
            coverImage?.extraLarge
        ),
        progress = 0,
        totalEpisodes = episodes,
        totalChapters = chapters,
        totalVolumes = volumes,
        type = type,
        format = format,
        status = LibraryStatus.UNKNOWN,
        mediaStatus = status?.name
    )

    private fun <T> Set<T>.toOptionalList(): Optional<List<T?>?> =
        if (isEmpty()) Optional.absent() else Optional.present(this.toList())

    /**
     * AniList's `*_greater` / `*_lesser` filters are strict, so "X or more"
     * needs `value - 1` and "X or less" needs `value + 1`. `EXACTLY` uses the
     * exact-match field instead.
     */
    private fun com.anisync.android.domain.IntComparatorFilter.exact(): Optional<Int?> =
        if (mode == com.anisync.android.domain.ComparatorMode.EXACTLY && value != null) {
            Optional.present(value)
        } else Optional.absent()

    private fun com.anisync.android.domain.IntComparatorFilter.greater(): Optional<Int?> =
        if (mode == com.anisync.android.domain.ComparatorMode.AT_LEAST && value != null) {
            Optional.present(value - 1)
        } else Optional.absent()

    private fun com.anisync.android.domain.IntComparatorFilter.lesser(): Optional<Int?> =
        if (mode == com.anisync.android.domain.ComparatorMode.AT_MOST && value != null) {
            Optional.present(value + 1)
        } else Optional.absent()
}
