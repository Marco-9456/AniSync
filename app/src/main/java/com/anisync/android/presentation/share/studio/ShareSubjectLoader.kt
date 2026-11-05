package com.anisync.android.presentation.share.studio

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import com.anisync.android.R
import com.anisync.android.ReviewDetailsQuery
import com.anisync.android.data.AppSettings
import com.anisync.android.data.CoverQuality
import com.anisync.android.data.TitleLanguage
import com.anisync.android.data.util.AppLocale
import com.anisync.android.data.util.dataOrThrow
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.ADULT_GENRES
import com.anisync.android.domain.CachePolicy
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.RecapRepository
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.data.account.AccountStore
import com.anisync.android.presentation.recap.PREVIEW_STUDIO_ID
import com.anisync.android.presentation.recap.RecapCardFactory
import com.anisync.android.presentation.recap.RecapPreviewStore
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.StatisticsRepository
import com.anisync.android.domain.UserProfile
import com.anisync.android.domain.formatCommunityScore
import com.anisync.android.domain.getOrElse
import com.anisync.android.presentation.navigation.ShareStudio
import com.anisync.android.presentation.share.cards.CardImage
import com.anisync.android.presentation.share.cards.GenreCount
import com.anisync.android.presentation.share.cards.KnownFor
import com.anisync.android.presentation.share.cards.KnownForRole
import com.anisync.android.presentation.share.cards.ShareSubject
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.aniListProfileSeedColor
import com.anisync.android.util.AniListUrls
import com.anisync.android.util.getName
import com.anisync.android.util.getTitle
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.fetchPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/** What a studio route can open. */
enum class ShareStudioSubject { MEDIA, CHARACTER, STAFF, REVIEW, STATS, FAVOURITES, RECAP }

/**
 * Builds the subject a studio route names from what the app already holds, going to the network
 * only for what the page that opened the studio did not leave in a cache.
 */
class ShareSubjectLoader @Inject constructor(
    private val detailsRepository: DetailsRepository,
    private val profileRepository: ProfileRepository,
    private val statisticsRepository: StatisticsRepository,
    private val apolloClient: ApolloClient,
    private val appSettings: AppSettings,
    private val recapRepository: RecapRepository,
    private val recapCards: RecapCardFactory,
    private val recapPreviews: RecapPreviewStore,
    private val accountStore: AccountStore,
    @ApplicationContext private val context: Context,
) {
    private val resources get() = AppLocale.wrap(context).resources
    private val locale: Locale get() = resources.configuration.locales[0] ?: Locale.getDefault()

    /**
     * The route's subject, or why there is none. A failed request comes back as it failed, with the
     * countdown of a rate limit, and [NOTHING] means there is nothing to share.
     */
    suspend fun load(route: ShareStudio): Result<ShareSubject> {
        val kind = runCatching { ShareStudioSubject.valueOf(route.subject) }.getOrNull() ?: return NOTHING
        val viewer = profileRepository.observeProfile().firstOrNull()
        return when (kind) {
            ShareStudioSubject.MEDIA -> media(route.id, viewer?.name)
            ShareStudioSubject.CHARACTER -> character(route.id, viewer?.name)
            ShareStudioSubject.STAFF -> staff(route.id, viewer?.name)
            ShareStudioSubject.REVIEW -> review(route.id, viewer?.name)
            ShareStudioSubject.STATS -> stats(route, viewer)
            ShareStudioSubject.FAVOURITES -> favourites(route, viewer)
            ShareStudioSubject.RECAP -> recap(route, viewer)
        }
    }

    /** The active account's recap for the route's period, or the Developer Tools preview it names. */
    private suspend fun recap(route: ShareStudio, viewer: UserProfile?): Result<ShareSubject.Recap> {
        val period = route.periodKey?.let(RecapPeriod::fromKey) ?: return NOTHING
        if (route.id == PREVIEW_STUDIO_ID) {
            val preview = recapPreviews.preview?.takeIf { it.recap.period == period } ?: return NOTHING
            return Result.Success(recapCards.card(preview.recap, preview.userName))
        }
        val stored = recapRepository.storedRecap(period)
            ?: recapRepository.getRecap(period).getOrElse { return it }
            ?: return NOTHING
        val name = accountStore.activeAccount.value?.name ?: viewer?.name.orEmpty()
        return Result.Success(recapCards.card(stored, name))
    }

    private suspend fun media(id: Int, handle: String?): Result<ShareSubject.Media> {
        val details = detailsRepository.observeMediaDetails(id).firstOrNull()
            ?: run {
                detailsRepository.refreshMediaDetails(id).getOrElse { return it }
                detailsRepository.observeMediaDetails(id).firstOrNull()
            }
            ?: return NOTHING
        val isManga = details.type == MediaType.MANGA
        val studios = details.studios.map { it.name }.ifEmpty { listOfNotNull(details.studio?.name) }
        return Result.Success(ShareSubject.Media(
            id = details.id,
            isManga = isManga,
            name = details.getTitle(appSettings.titleLanguage.value),
            coverUrl = details.cover?.preferred(CoverQuality.EXTRA_LARGE) ?: details.coverUrl,
            bannerUrl = details.bannerUrl,
            aniListColor = parseHex(details.coverColor),
            format = details.format,
            year = details.seasonYear ?: details.year,
            studios = studios,
            genres = details.genres.filterAdult(),
            score = cardScore(details.score ?: details.meanScore),
            popularity = details.popularity,
            favourites = details.favourites,
            total = if (isManga) details.chapters else details.episodes,
            listStatus = details.listStatus,
            progress = details.listProgress,
            releaseStatus = details.status,
            handle = handle,
            link = AniListUrls.mediaUrl(details.id, details.type),
        ))
    }

    private suspend fun character(id: Int, handle: String?): Result<ShareSubject.Character> {
        val details = detailsRepository.getCharacterDetails(id).getOrElse { return it }
        val language = appSettings.titleLanguage.value
        return Result.Success(ShareSubject.Character(
            id = details.id,
            name = details.getName(language),
            nativeName = details.nativeName?.takeIf { it != details.getName(language) },
            imageUrl = details.imageUrl,
            favourites = details.favourites,
            birthday = details.dateOfBirth?.let(::birthday),
            gender = details.gender?.takeIf { it.isNotBlank() },
            age = details.age?.let(::age),
            knownFor = knownFor(details),
            handle = handle,
            link = AniListUrls.characterUrl(details.id),
        ))
    }

    private suspend fun staff(id: Int, handle: String?): Result<ShareSubject.Staff> {
        val details = detailsRepository.getStaffDetails(id).getOrElse { return it }
        val language = appSettings.titleLanguage.value
        val voiced = details.voicedCharacters.take(ROLES).map { CardImage(it.getName(language), it.characterImageUrl) }
        val roles = voiced.ifEmpty {
            details.productionMedia
                .sortedByDescending { it.popularity ?: 0 }
                .distinctBy { it.mediaId }
                .take(ROLES)
                .map { CardImage(it.titleUserPreferred, it.cover?.preferred(CoverQuality.LARGE) ?: it.coverUrl) }
        }
        return Result.Success(ShareSubject.Staff(
            id = details.id,
            name = details.getName(language),
            nativeName = details.nativeName?.takeIf { it != details.getName(language) },
            imageUrl = details.imageUrl,
            occupation = details.primaryOccupations.firstOrNull(),
            favourites = details.favourites,
            roles = roles,
            voicesRoles = voiced.isNotEmpty(),
            activeSince = details.yearsActive.firstOrNull(),
            homeTown = details.homeTown?.takeIf { it.isNotBlank() },
            handle = handle,
            link = AniListUrls.staffUrl(details.id),
        ))
    }

    private suspend fun review(id: Int, handle: String?): Result<ShareSubject.Review> {
        val data = safeApiCall {
            apolloClient.query(ReviewDetailsQuery(id = Optional.present(id)))
                .fetchPolicy(FetchPolicy.CacheFirst)
                .execute()
                .dataOrThrow()
                .Review
        }.getOrElse { return it } ?: return NOTHING
        return Result.Success(ShareSubject.Review(
            id = data.id,
            mediaTitle = data.media?.title?.userPreferred.orEmpty(),
            bannerUrl = data.media?.bannerImage,
            coverUrl = data.media?.coverImage?.large,
            aniListColor = parseHex(data.media?.coverImage?.color),
            summary = data.summary.orEmpty(),
            score = data.score ?: 0,
            authorName = data.user?.name.orEmpty(),
            authorAvatarUrl = data.user?.avatar?.large ?: data.user?.avatar?.medium,
            createdAt = data.createdAt.toLong(),
            helpful = data.rating ?: 0,
            handle = handle,
            link = "https://anilist.co/review/${data.id}",
        ))
    }

    private suspend fun stats(route: ShareStudio, viewer: UserProfile?): Result<ShareSubject.Stats> {
        val profile = profile(route, viewer).getOrElse { return it }
        val statistics = statisticsRepository.getUserStatistics(profile.id, CachePolicy.CacheFirst).getOrElse { return it }
        val isManga = route.mediaType == MediaType.MANGA.rawValue
        val language = appSettings.titleLanguage.value
        val favourites = (if (isManga) profile.favoriteMangaOverview else profile.favoriteAnime).take(FAVOURITES).map { it.toCardImage(language) }
        return Result.Success(if (isManga) {
            val manga = statistics.mangaStats ?: return NOTHING
            ShareSubject.Stats(
                name = profile.name,
                avatarUrl = profile.avatarUrl,
                bannerUrl = profile.bannerUrl,
                aniListColor = aniListProfileSeedColor(profile.profileColor),
                isManga = true,
                count = manga.totalCount,
                minutes = null,
                units = manga.chaptersRead,
                meanScore = manga.meanScore,
                standardDeviation = manga.standardDeviation,
                completed = manga.statusDistribution.firstOrNull { it.status == COMPLETED }?.count ?: 0,
                genres = manga.genreDistribution.map { GenreCount(it.genre, it.count) }.filterAdultGenres(),
                topStudio = null,
                topStudioCount = 0,
                topStudioMean = 0f,
                favourites = favourites,
                isOwnProfile = profile.id == viewer?.id,
                link = profileLink(profile.name),
            )
        } else {
            val anime = statistics.animeStats
            val studio = anime.studioDistribution.maxByOrNull { it.count }
            ShareSubject.Stats(
                name = profile.name,
                avatarUrl = profile.avatarUrl,
                bannerUrl = profile.bannerUrl,
                aniListColor = aniListProfileSeedColor(profile.profileColor),
                isManga = false,
                count = anime.totalCount,
                minutes = anime.minutesWatched,
                units = anime.episodesWatched,
                meanScore = anime.meanScore,
                standardDeviation = anime.standardDeviation,
                completed = anime.statusDistribution.firstOrNull { it.status == COMPLETED }?.count ?: 0,
                genres = anime.genreDistribution.map { GenreCount(it.genre, it.count) }.filterAdultGenres(),
                topStudio = studio?.studioName,
                topStudioCount = studio?.count ?: 0,
                topStudioMean = studio?.meanScore ?: 0f,
                favourites = favourites,
                isOwnProfile = profile.id == viewer?.id,
                link = profileLink(profile.name),
            )
        })
    }

    private suspend fun favourites(route: ShareStudio, viewer: UserProfile?): Result<ShareSubject.Favourites> {
        val profile = profile(route, viewer).getOrElse { return it }
        val isManga = route.mediaType == MediaType.MANGA.rawValue
        val language = appSettings.titleLanguage.value
        val entries = (if (isManga) profile.favoriteMangaOverview else profile.favoriteAnime).map { it.toCardImage(language) }
        if (entries.isEmpty()) return NOTHING
        return Result.Success(ShareSubject.Favourites(
            name = profile.name,
            avatarUrl = profile.avatarUrl,
            bannerUrl = profile.bannerUrl,
            aniListColor = aniListProfileSeedColor(profile.profileColor),
            isManga = isManga,
            entries = entries,
            link = profileLink(profile.name),
        ))
    }

    private suspend fun profile(route: ShareStudio, viewer: UserProfile?): Result<UserProfile> {
        if (viewer != null && (viewer.id == route.id || viewer.name == route.userName)) {
            return Result.Success(profileRepository.observeProfile().first() ?: viewer)
        }
        val name = route.userName ?: return NOTHING
        return profileRepository.fetchUserProfile(name, forceNetwork = false)
    }

    /**
     * The community score in the viewer's own scale. Stars and faces do not read on a tile, so those
     * scales get AniList's percentage instead.
     */
    private fun cardScore(score: Int?): String? {
        val value = score?.takeIf { it > 0 } ?: return null
        return when (val format = appSettings.userScoreFormat.value) {
            ScoreFormat.POINT_5, ScoreFormat.POINT_3 -> resources.getString(R.string.tags_rank_percent, value)
            else -> formatCommunityScore(value, format)
        }
    }

    /** The title a character is best known for: their most popular main role, or their most popular one. */
    private fun knownFor(details: CharacterDetails): KnownFor? {
        val main = details.media.filter { it.characterRole == MAIN }.maxByOrNull { it.popularity ?: 0 }
        val pick = main ?: details.media.maxByOrNull { it.popularity ?: 0 } ?: return null
        val language = appSettings.titleLanguage.value
        val total = details.mediaTotal ?: details.media.size
        return KnownFor(
            title = pick.getTitle(language),
            coverUrl = pick.cover?.preferred(CoverQuality.LARGE) ?: pick.coverUrl,
            role = when (pick.characterRole) {
                MAIN -> KnownForRole.MAIN
                SUPPORTING -> KnownForRole.SUPPORTING
                else -> KnownForRole.BACKGROUND
            },
            more = (total - 1).coerceAtLeast(0),
        )
    }

    /** "8/21" or "8/21/1990" as the day and month in the app's language, "21 August". */
    private fun birthday(value: String): String? {
        val parts = value.split('/').mapNotNull { it.toIntOrNull() }
        if (parts.size < 2) return null
        val calendar = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, LEAP_YEAR)
            set(Calendar.MONTH, parts[0] - 1)
            set(Calendar.DAY_OF_MONTH, parts[1])
        }
        val pattern = DateFormat.getBestDateTimePattern(locale, "dMMMM")
        return SimpleDateFormat(pattern, locale).format(calendar.time)
    }

    /** AniList writes ranges as "15-19", the card says "15 to 19". */
    private fun age(value: String): String? {
        val trimmed = value.trim().trimEnd('+').takeIf { it.isNotEmpty() } ?: return null
        val range = trimmed.split('-').map { it.trim() }
        return if (range.size == 2 && range.all { it.isNotEmpty() }) {
            resources.getString(R.string.stats_length_range, range[0], range[1])
        } else {
            value.trim()
        }
    }

    private fun List<String>.filterAdult(): List<String> =
        if (appSettings.showAdultContent.value) this else filterNot { it in ADULT_GENRES }

    private fun List<GenreCount>.filterAdultGenres(): List<GenreCount> =
        (if (appSettings.showAdultContent.value) this else filterNot { it.genre in ADULT_GENRES })
            .sortedByDescending { it.count }

    private fun LibraryEntry.toCardImage(language: TitleLanguage) =
        CardImage(getTitle(language), cover?.preferred(CoverQuality.LARGE) ?: coverUrl)

    private fun profileLink(name: String) = "https://anilist.co/user/$name"

    private fun parseHex(hex: String?): Color? {
        val value = hex?.trim()?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16) ?: return null
        return Color(0xFF000000 or value)
    }

    companion object {
        /** No request failed, there is simply nothing on the route to share. */
        val NOTHING = Result.Error("Nothing to share")

        private const val ROLES = 3
        private const val FAVOURITES = 5
        private const val COMPLETED = "COMPLETED"
        private const val MAIN = "MAIN"
        private const val SUPPORTING = "SUPPORTING"

        /** A leap year, so 29 February still formats. */
        private const val LEAP_YEAR = 2000
    }
}
