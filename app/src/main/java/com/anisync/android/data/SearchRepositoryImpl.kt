package com.anisync.android.data

import com.anisync.android.data.util.InflightTracker
import com.anisync.android.GetSearchTaxonomyQuery
import com.anisync.android.SearchAllQuery
import com.anisync.android.SearchEverythingQuery
import com.anisync.android.SearchMediaQuery
import com.anisync.android.data.mapper.mapFuzzyDateToLong
import com.anisync.android.data.mapper.toMediaSort
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
import com.anisync.android.fragment.MediaCardFields
import com.anisync.android.type.MediaSort
import com.anisync.android.type.MediaType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.doNotStore
import com.apollographql.cache.normalized.fetchPolicy
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
     * the chip. This is the fix for the previously-inert toggle. AniList's account also says whether
     * search and quick search take in 18+ titles by default, and leaves applying that to each app.
     */
    private fun resolveAdultFilter(
        mode: com.anisync.android.domain.AdultMode,
        quickSearch: Boolean,
    ): Optional<Boolean?> {
        val includeByDefault =
            if (quickSearch) appSettings.adultInQuickSearch.value else appSettings.adultInSearch.value
        return resolveAdultIsAdult(mode, appSettings.showAdultContent.value, includeByDefault)
            ?.let { Optional.present(it) } ?: Optional.absent()
    }

    private fun SearchFilters.sortFor(type: MediaType, query: String): MediaSort =
        sort.toMediaSort(type, appSettings.titleLanguage.value, hasQuery = query.isNotBlank())

    @Volatile private var cachedGenres: List<String>? = null
    @Volatile private var cachedTags: List<MediaTag>? = null

    /** A re-triggered identical search joins the request in flight instead of issuing a second. */
    private val inflight = InflightTracker()

    private suspend fun <T> dedupe(key: String, block: suspend () -> T): T =
        inflight.deduplicate(key, block = block)

    override suspend fun searchMedia(
        query: String,
        type: MediaType,
        filters: SearchFilters,
        page: Int,
        perPage: Int
    ): Result<SearchPage> {
        return safeApiCall {
            val response = apolloClient.query(
                SearchMediaQuery(
                    search = if (query.isBlank()) Optional.absent() else Optional.present(query),
                    type = Optional.present(type),
                    page = Optional.present(page),
                    perPage = Optional.present(perPage),
                    sort = Optional.present(listOf(filters.sortFor(type, query))),
                    genre_in = filters.genresIncluded.toOptionalList(),
                    genre_not_in = filters.genresExcluded.toOptionalList(),
                    tag_in = filters.tagsIncluded.toOptionalList(),
                    tag_not_in = filters.tagsExcluded.toOptionalList(),
                    format_in = filters.formats.toOptionalList(),
                    status_in = filters.statuses.toOptionalList(),
                    source_in = filters.sources.toOptionalList(),
                    startDate_greater = filters.startDateFloor(),
                    startDate_lesser = filters.startDateCeiling(),
                    season = filters.season?.let { Optional.present(it) } ?: Optional.absent(),
                    seasonYear = filters.seasonYear()?.let { Optional.present(it) } ?: Optional.absent(),
                    weightedScore = filters.score.exact(),
                    weightedScore_greater = filters.score.greater(),
                    weightedScore_lesser = filters.score.lesser(),
                    episodes = filters.episodes.exact(),
                    episodes_greater = filters.episodes.greater(),
                    episodes_lesser = filters.episodes.lesser(),
                    chapters = filters.chapters.exact(),
                    chapters_greater = filters.chapters.greater(),
                    chapters_lesser = filters.chapters.lesser(),
                    countryOfOrigin = filters.country?.let { Optional.present(it.code) }
                        ?: Optional.absent(),
                    isAdult = resolveAdultFilter(filters.adultMode, quickSearch = true)
                )
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .doNotStore(true)
                .execute()

            val pageData = response.data?.Page
            val pageInfo = pageData?.pageInfo
            val entries = pageData?.media?.filterNotNull()?.map { it.toLibraryEntry() } ?: emptyList()
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
                .doNotStore(true)
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
        page: Int,
        perPage: Int,
        wantAnime: Boolean,
        wantManga: Boolean,
        wantEntities: Boolean,
        wantStudios: Boolean
    ): Result<SearchEverythingResult> {
        val key = "all:$query:${filters.hashCode()}:$page:$perPage:$wantAnime:$wantManga:$wantEntities:$wantStudios"
        return dedupe(key) {
            safeApiCall {
                val response = apolloClient.query(
                    SearchEverythingQuery(
                        search = if (query.isBlank()) Optional.absent() else Optional.present(query),
                        page = Optional.present(page),
                        perPage = Optional.present(perPage),
                        wantAnime = wantAnime,
                        wantManga = wantManga,
                        wantEntities = wantEntities,
                        wantStudios = wantStudios,
                        animeSort = Optional.present(listOf(filters.sortFor(MediaType.ANIME, query))),
                        mangaSort = Optional.present(listOf(filters.sortFor(MediaType.MANGA, query))),
                        genre_in = filters.genresIncluded.toOptionalList(),
                        genre_not_in = filters.genresExcluded.toOptionalList(),
                        tag_in = filters.tagsIncluded.toOptionalList(),
                        tag_not_in = filters.tagsExcluded.toOptionalList(),
                        format_in = filters.formats.toOptionalList(),
                        status_in = filters.statuses.toOptionalList(),
                        source_in = filters.sources.toOptionalList(),
                        startDate_greater = filters.startDateFloor(),
                        startDate_lesser = filters.startDateCeiling(),
                        season = filters.season?.let { Optional.present(it) } ?: Optional.absent(),
                        seasonYear = filters.seasonYear()?.let { Optional.present(it) } ?: Optional.absent(),
                        weightedScore = filters.score.exact(),
                        weightedScore_greater = filters.score.greater(),
                        weightedScore_lesser = filters.score.lesser(),
                        episodes = filters.episodes.exact(),
                        episodes_greater = filters.episodes.greater(),
                        episodes_lesser = filters.episodes.lesser(),
                        chapters = filters.chapters.exact(),
                        chapters_greater = filters.chapters.greater(),
                        chapters_lesser = filters.chapters.lesser(),
                        countryOfOrigin = filters.country?.let { Optional.present(it.code) }
                            ?: Optional.absent(),
                        isAdult = resolveAdultFilter(filters.adultMode, quickSearch = false)
                    )
                )
                    .fetchPolicy(FetchPolicy.NetworkFirst)
                    .execute()

                val data = response.data

                val animeEntries = data?.anime?.media?.filterNotNull()
                    ?.map { it.mediaCardFields.toSearchEntry() }.orEmpty()

                val mangaEntries = data?.manga?.media?.filterNotNull()
                    ?.map { it.mediaCardFields.toSearchEntry() }.orEmpty()

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
                    animeTotal = data?.anime?.pageInfo?.total ?: animeEntries.size,
                    mangaTotal = data?.manga?.pageInfo?.total ?: mangaEntries.size,
                    charactersHasNextPage = data?.characters?.pageInfo?.hasNextPage ?: false,
                    staffHasNextPage = data?.staff?.pageInfo?.hasNextPage ?: false,
                    usersHasNextPage = data?.users?.pageInfo?.hasNextPage ?: false,
                    studiosHasNextPage = data?.studios?.pageInfo?.hasNextPage ?: false,
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

    /**
     * A search result as the result row and poster read it. The score, start date and next episode
     * ride the shared card fragment already, so carrying them costs the request nothing.
     */
    private fun MediaCardFields.toSearchEntry(): LibraryEntry = LibraryEntry(
        id = 0,
        mediaId = id,
        titleRomaji = title?.romaji,
        titleEnglish = title?.english,
        titleNative = title?.native,
        titleUserPreferred = title?.userPreferred ?: "Unknown",
        coverUrl = coverImage?.extraLarge,
        cover = com.anisync.android.domain.CoverImage.of(
            coverImage?.medium, coverImage?.large, coverImage?.extraLarge
        ),
        progress = 0,
        totalEpisodes = episodes,
        totalChapters = chapters,
        totalVolumes = volumes,
        type = type,
        format = format,
        status = LibraryStatus.UNKNOWN,
        mediaStatus = status?.name,
        averageScore = averageScore,
        mediaStartDate = startDate?.let { mapFuzzyDateToLong(it.year, it.month, it.day) },
        nextAiringEpisode = nextAiringEpisode?.episode,
        timeUntilAiring = nextAiringEpisode?.timeUntilAiring,
        nextAiringEpisodeTime = nextAiringEpisode?.airingAt?.toLong()
    )

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
     * A season picked with a single year goes out as AniList's seasonYear, which counts a late
     * December premiere into the Winter after it. A start date range would leave it out.
     */
    private fun SearchFilters.seasonYear(): Int? =
        yearRange.min?.takeIf { season != null && it == yearRange.max }

    private fun SearchFilters.startDateFloor(): Optional<Int?> =
        yearRange.min?.takeIf { seasonYear() == null }?.let { Optional.present(it * 10000 + 101) }
            ?: Optional.absent()

    private fun SearchFilters.startDateCeiling(): Optional<Int?> =
        yearRange.max?.takeIf { seasonYear() == null }?.let { Optional.present(it * 10000 + 1231) }
            ?: Optional.absent()

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
