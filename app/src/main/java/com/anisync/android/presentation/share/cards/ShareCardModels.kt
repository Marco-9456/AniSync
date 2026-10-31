package com.anisync.android.presentation.share.cards

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.anisync.android.R
import com.anisync.android.data.CardSize
import com.anisync.android.domain.LibraryStatus

/** The polarity a card's scheme is built in. */
enum class CardTone { DARK, LIGHT, PURE_BLACK }

/** What fills a card behind its content. [CLEAR] keeps the corners transparent, so only [CardSize.CHAT] offers it. */
enum class CardBackground { ARTWORK_BLUR, GRADIENT, SOLID, CLEAR }

/** A figure the Details panel can take off a card. */
enum class CardDetail(@StringRes val label: Int) {
    SCORE(R.string.share_toggle_score),
    POPULARITY(R.string.share_detail_popularity),
    FAVOURITES(R.string.share_detail_favourites),
    PROGRESS(R.string.share_toggle_progress),
    STUDIO(R.string.share_detail_studio),
    GENRES(R.string.share_detail_genres),
    TIME(R.string.share_detail_time),
    TASTE(R.string.share_detail_taste),
    RANKING(R.string.share_detail_ranking),
    BIRTHDAY(R.string.share_detail_birthday),
    AGE(R.string.share_detail_age),
    GENDER(R.string.share_detail_gender),
    KNOWN_FOR(R.string.share_known_for),
    ROLES(R.string.share_detail_roles),
    ACTIVE_SINCE(R.string.share_detail_active_since),
    HOME_TOWN(R.string.share_detail_home_town),
    AUTHOR(R.string.share_detail_author),
    TOP_TITLES(R.string.share_detail_top_titles),
    HANDLE(R.string.share_detail_handle),
}

/** Everything the studio lets the viewer change about a card, besides its template and palette. */
@Immutable
data class CardSpec(
    val size: CardSize,
    val tone: CardTone = CardTone.DARK,
    val background: CardBackground = CardBackground.SOLID,
    val dim: Float = DEFAULT_DIM,
    val details: Set<CardDetail> = CardDetail.entries.toSet(),
    val caption: String = "",
) {
    fun shows(detail: CardDetail): Boolean = detail in details

    companion object {
        /** The dim the frames are drawn at. */
        const val DEFAULT_DIM = 0.4f
        const val CAPTION_LIMIT = 80
    }
}

/** A cover or portrait a card draws, with what it is called. */
@Immutable
data class CardImage(val title: String, val url: String?)

/** One genre and how many titles carry it. */
@Immutable
data class GenreCount(val genre: String, val count: Int)

/** What a card is about, already resolved into what its templates print. */
@Immutable
sealed interface ShareSubject {
    /** Shown under the studio title. */
    val name: String

    /** What Copy link copies and Share sends beside the image. */
    val link: String

    /** The picture the swatches come from and the artwork blur shows. */
    val artworkUrl: String?

    /** AniList's own colour for the subject, offered as the first swatch. */
    val aniListColor: Color?

    /** The AniList name the brand pill carries, or null when nobody is signed in. */
    val handle: String?

    /** The figures this subject's Details panel offers, in panel order. */
    val details: List<CardDetail>

    @Immutable
    data class Media(
        val id: Int,
        val isManga: Boolean,
        override val name: String,
        val coverUrl: String?,
        val bannerUrl: String?,
        override val aniListColor: Color?,
        val format: String?,
        val year: Int?,
        val studios: List<String>,
        val genres: List<String>,
        /** The community score in the viewer's own scale, or null when AniList has none. */
        val score: String?,
        val popularity: Int?,
        val favourites: Int?,
        /** Episodes or chapters, null while the total is unknown. */
        val total: Int?,
        val listStatus: LibraryStatus?,
        val progress: Int?,
        val releaseStatus: String,
        override val handle: String?,
        override val link: String,
    ) : ShareSubject {
        override val artworkUrl: String? get() = coverUrl
        override val details: List<CardDetail>
            get() = listOf(
                CardDetail.SCORE, CardDetail.POPULARITY, CardDetail.FAVOURITES, CardDetail.PROGRESS,
                CardDetail.STUDIO, CardDetail.GENRES, CardDetail.HANDLE,
            )
    }

    @Immutable
    data class Stats(
        override val name: String,
        val avatarUrl: String?,
        val bannerUrl: String?,
        override val aniListColor: Color?,
        val isManga: Boolean,
        val count: Int,
        /** Minutes watched for anime, null for manga. */
        val minutes: Int?,
        /** Episodes watched or chapters read. */
        val units: Int,
        val meanScore: Float,
        val standardDeviation: Float,
        val completed: Int,
        /** By titles, most first, adult genres already removed when they may not show. */
        val genres: List<GenreCount>,
        val topStudio: String?,
        val topStudioCount: Int,
        val topStudioMean: Float,
        val favourites: List<CardImage>,
        val isOwnProfile: Boolean,
        override val link: String,
    ) : ShareSubject {
        override val artworkUrl: String? get() = bannerUrl ?: avatarUrl
        override val handle: String? get() = name
        override val details: List<CardDetail>
            get() = listOf(
                CardDetail.SCORE, CardDetail.TIME, CardDetail.TASTE, CardDetail.GENRES, CardDetail.STUDIO,
                CardDetail.FAVOURITES, CardDetail.HANDLE,
            )
    }

    @Immutable
    data class Favourites(
        override val name: String,
        val avatarUrl: String?,
        val bannerUrl: String?,
        override val aniListColor: Color?,
        val isManga: Boolean,
        val entries: List<CardImage>,
        override val link: String,
    ) : ShareSubject {
        override val artworkUrl: String? get() = entries.firstOrNull()?.url ?: bannerUrl
        override val handle: String? get() = name
        override val details: List<CardDetail> get() = listOf(CardDetail.RANKING, CardDetail.HANDLE)
    }

    @Immutable
    data class Character(
        val id: Int,
        override val name: String,
        val nativeName: String?,
        val imageUrl: String?,
        val favourites: Int?,
        val birthday: String?,
        val gender: String?,
        val age: String?,
        val knownFor: KnownFor?,
        override val handle: String?,
        override val link: String,
    ) : ShareSubject {
        override val artworkUrl: String? get() = imageUrl
        override val aniListColor: Color? get() = null
        override val details: List<CardDetail>
            get() = listOf(
                CardDetail.FAVOURITES, CardDetail.BIRTHDAY, CardDetail.GENDER, CardDetail.AGE,
                CardDetail.KNOWN_FOR, CardDetail.HANDLE,
            )
    }

    @Immutable
    data class Staff(
        val id: Int,
        override val name: String,
        val nativeName: String?,
        val imageUrl: String?,
        val occupation: String?,
        val favourites: Int?,
        /** Characters voiced, or titles worked on for staff who voice nobody. */
        val roles: List<CardImage>,
        val voicesRoles: Boolean,
        val activeSince: Int?,
        val homeTown: String?,
        override val handle: String?,
        override val link: String,
    ) : ShareSubject {
        override val artworkUrl: String? get() = imageUrl
        override val aniListColor: Color? get() = null
        override val details: List<CardDetail>
            get() = listOf(
                CardDetail.FAVOURITES, CardDetail.ROLES, CardDetail.ACTIVE_SINCE, CardDetail.HOME_TOWN, CardDetail.HANDLE,
            )
    }

    @Immutable
    data class Review(
        val id: Int,
        val mediaTitle: String,
        val bannerUrl: String?,
        val coverUrl: String?,
        override val aniListColor: Color?,
        val summary: String,
        val score: Int,
        val authorName: String,
        val authorAvatarUrl: String?,
        val createdAt: Long,
        val helpful: Int,
        override val handle: String?,
        override val link: String,
    ) : ShareSubject {
        override val name: String get() = mediaTitle
        override val artworkUrl: String? get() = coverUrl ?: bannerUrl
        override val details: List<CardDetail>
            get() = listOf(CardDetail.SCORE, CardDetail.AUTHOR, CardDetail.HANDLE)
    }

    @Immutable
    data class Recap(
        val card: RecapCard,
        override val handle: String?,
        override val link: String,
    ) : ShareSubject {
        override val name: String get() = card.periodTitle
        override val artworkUrl: String? get() = card.covers.firstOrNull()?.url
        override val aniListColor: Color? get() = card.coverColor
        override val details: List<CardDetail>
            get() = listOf(CardDetail.TOP_TITLES, CardDetail.GENRES, CardDetail.HANDLE)
    }
}

/** How a character appears in the title they are best known for. */
enum class KnownForRole { MAIN, SUPPORTING, BACKGROUND }

/** The title a character is best known for, and how many more they appear in. */
@Immutable
data class KnownFor(val title: String, val coverUrl: String?, val role: KnownForRole, val more: Int)

/** A recap as its share cards print it. Built from a real recap in Phase 5, from preview data until then. */
@Immutable
data class RecapCard(
    val userName: String,
    val isYear: Boolean,
    /** "September 2026" or "2025", for the studio subtitle. */
    val periodTitle: String,
    /** The month name on a monthly card, null on a yearly one. */
    val monthName: String?,
    val year: Int,
    val episodes: Int,
    val minutes: Int,
    val titles: Int,
    val finished: Int,
    val activeDays: Int,
    val top: List<RecapTitle>,
    val genres: List<String>,
    /** Episodes per month on a yearly card, January first. */
    val months: List<Int>,
    val monthInitials: List<String>,
    val busiestMonthName: String?,
    val topGenre: String?,
    val topStudio: String?,
    val mangaChapters: Int?,
    val covers: List<CardImage>,
    val coverColor: Color?,
)

@Immutable
data class RecapTitle(val title: String, val coverUrl: String?, val minutes: Int)

/** A card layout. Every template draws all four sizes, so a new one is a single composable. */
interface ShareTemplate<in S : ShareSubject> {
    @get:StringRes
    val label: Int

    /** The background the frames draw this template on. */
    val defaultBackground: CardBackground

    @Composable
    fun Card(subject: S, spec: CardSpec)
}
