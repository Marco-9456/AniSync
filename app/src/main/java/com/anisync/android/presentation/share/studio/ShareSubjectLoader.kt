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
import com.anisync.android.domain.ADULT_GENRES
import com.anisync.android.domain.CachePolicy
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.StatisticsRepository
import com.anisync.android.domain.UserProfile
import com.anisync.android.domain.formatCommunityScore
import com.anisync.android.presentation.navigation.ShareStudio
import com.anisync.android.presentation.share.cards.CardImage
import com.anisync.android.presentation.share.cards.GenreCount
import com.anisync.android.presentation.share.cards.KnownFor
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
enum class ShareStudioSubject { MEDIA, CHARACTER, STAFF, REVIEW, STATS, FAVOURITES }

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
    @ApplicationContext private val context: Context,
) {
    private val resources get() = AppLocale.wrap(context).resources
    private val locale: Locale get() = resources.configuration.locales[0] ?: Locale.getDefault()

    suspend fun load(route: ShareStudio): ShareSubject? {
        val kind = runCatching { ShareStudioSubject.valueOf(route.subject) }.getOrNull() ?: return null
        val viewer = profileRepository.observeProfile().firstOrNull()
        return when (kind) {
            ShareStudioSubject.MEDIA -> media(route.id, viewer?.name)
            ShareStudioSubject.CHARACTER -> character(route.id, viewer?.name)
            ShareStudioSubject.STAFF -> staff(route.id, viewer?.name)
            ShareStudioSubject.REVIEW -> review(route.id, viewer?.name)
            ShareStudioSubject.STATS -> stats(route, viewer)
            ShareStudioSubject.FAVOURITES -> favourites(route, viewer)
        }
    }

    private suspend fun media(id: Int, handle: String?): ShareSubject.Media? {
        val details = detailsRepository.observeMediaDetails(id).firstOrNull()
            ?: run {
                detailsRepository.refreshMediaDetails(id)
                detailsRepository.observeMediaDetails(id).firstOrNull()
            }
            ?: return null
        val isManga = details.type == MediaType.MANGA
        val studios = details.studios.map { it.name }.ifEmpty { listOfNotNull(details.studio?.name) }
        return ShareSubject.Media(
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
            score = formatCommunityScore(details.score ?: details.meanScore, appSettings.userScoreFormat.value),
            popularity = details.popularity,
            favourites = details.favourites,
            total = if (isManga) details.chapters else details.episodes,
            listStatus = details.listStatus,
            progress = details.listProgress,
            releaseStatus = details.status,
            handle = handle,
            link = AniListUrls.mediaUrl(details.id, details.type),
        )
    }

    private suspend fun character(id: Int, handle: String?): ShareSubject.Character? {
        val details = (detailsRepository.getCharacterDetails(id) as? Result.Success)?.data ?: return null
        val language = appSettings.titleLanguage.value
        return ShareSubject.Character(
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
        )
    }

    private suspend fun staff(id: Int, handle: String?): ShareSubject.Staff? {
        val details = (detailsRepository.getStaffDetails(id) as? Result.Success)?.data ?: return null
        val language = appSettings.titleLanguage.value
        val voiced = details.voicedCharacters.take(ROLES).map { CardImage(it.getName(language), it.characterImageUrl) }
        val roles = voiced.ifEmpty {
            details.productionMedia
                .sortedByDescending { it.popularity ?: 0 }
                .distinctBy { it.mediaId }
                .take(ROLES)
                .map { CardImage(it.titleUserPreferred, it.cover?.preferred(CoverQuality.LARGE) ?: it.coverUrl) }
        }
        return ShareSubject.Staff(
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
        )
    }

    private suspend fun review(id: Int, handle: String?): ShareSubject.Review? {
        val data = runCatching {
            apolloClient.query(ReviewDetailsQuery(id = Optional.present(id)))
                .fetchPolicy(FetchPolicy.CacheFirst)
                .execute()
                .data
                ?.Review
        }.getOrNull() ?: return null
        return ShareSubject.Review(
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
        )
    }

    private suspend fun stats(route: ShareStudio, viewer: UserProfile?): ShareSubject.Stats? {
        val profile = profile(route, viewer) ?: return null
        val statistics = (statisticsRepository.getUserStatistics(profile.id, CachePolicy.CacheFirst) as? Result.Success)?.data
            ?: return null
        val isManga = route.mediaType == MediaType.MANGA.rawValue
        val language = appSettings.titleLanguage.value
        val favourites = (if (isManga) profile.favoriteMangaOverview else profile.favoriteAnime).take(FAVOURITES).map { it.toCardImage(language) }
        return if (isManga) {
            val manga = statistics.mangaStats ?: return null
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
        }
    }

    private suspend fun favourites(route: ShareStudio, viewer: UserProfile?): ShareSubject.Favourites? {
        val profile = profile(route, viewer) ?: return null
        val isManga = route.mediaType == MediaType.MANGA.rawValue
        val language = appSettings.titleLanguage.value
        val entries = (if (isManga) profile.favoriteMangaOverview else profile.favoriteAnime).map { it.toCardImage(language) }
        if (entries.isEmpty()) return null
        return ShareSubject.Favourites(
            name = profile.name,
            avatarUrl = profile.avatarUrl,
            bannerUrl = profile.bannerUrl,
            aniListColor = aniListProfileSeedColor(profile.profileColor),
            isManga = isManga,
            entries = entries,
            link = profileLink(profile.name),
        )
    }

    private suspend fun profile(route: ShareStudio, viewer: UserProfile?): UserProfile? {
        if (viewer != null && (viewer.id == route.id || viewer.name == route.userName)) {
            return profileRepository.observeProfile().first() ?: viewer
        }
        val name = route.userName ?: return null
        return (profileRepository.fetchUserProfile(name, forceNetwork = false) as? Result.Success)?.data
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
            isMain = pick.characterRole == MAIN,
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
            resources.getString(R.string.share_age_range, range[0], range[1])
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

    private companion object {
        const val ROLES = 3
        const val FAVOURITES = 5
        const val COMPLETED = "COMPLETED"
        const val MAIN = "MAIN"

        /** A leap year, so 29 February still formats. */
        const val LEAP_YEAR = 2000
    }
}
