package com.anisync.android.presentation.recap

import android.content.Context
import com.anisync.android.data.AppSettings
import com.anisync.android.data.util.AppLocale
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.recap.PageSeed
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.isShort
import com.anisync.android.domain.recap.rankNote
import com.anisync.android.domain.recap.recapPages
import com.anisync.android.domain.recap.seed
import com.anisync.android.presentation.share.cards.CardImage
import com.anisync.android.presentation.share.cards.RecapCard
import com.anisync.android.presentation.share.cards.RecapTitle
import com.anisync.android.presentation.share.cards.ShareSubject
import com.anisync.android.presentation.util.formatMonthYear
import com.anisync.android.ui.theme.genreSeed
import com.anisync.android.ui.theme.mediaArtworkSeedColor
import com.anisync.android.util.AniListUrls
import com.anisync.android.util.TitleUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/** Turns a stored recap into what its story and its share cards print. */
class RecapCardFactory @Inject constructor(
    private val appSettings: AppSettings,
    @ApplicationContext private val context: Context,
) {
    private val locale: Locale
        get() = AppLocale.wrap(context).resources.configuration.locales[0] ?: Locale.getDefault()

    /** The titles the story may name, in the app's title language. */
    fun titles(stored: StoredRecap): Map<Int, StoryTitle> {
        val language = appSettings.titleLanguage.value
        return stored.media.mapValues { (_, media) ->
            StoryTitle(
                id = media.id,
                title = TitleUtils.getTitle(language, media.titleRomaji, media.titleEnglish, media.titleNative, media.title),
                coverUrl = media.coverUrl,
                color = mediaArtworkSeedColor(media.color),
                format = media.format,
                year = media.seasonYear ?: media.startDate?.year,
                studio = media.mainStudio,
                isFilm = media.isFilm,
                isShort = media.isShort(),
            )
        }
    }

    /**
     * The story's model, every page's seed worked out with it. [previous] is last month's recap, for
     * the comparison on the time page.
     */
    fun story(stored: StoredRecap, previous: StoredRecap?, userName: String, avatarUrl: String?, isPreview: Boolean): StoryModel {
        val recap = stored.recap
        val titles = titles(stored)
        val pages = recapPages(recap)
        val topColor = recap.ranked.firstOrNull()?.let { titles[it.mediaId]?.color }
        val seeds = pages.map { page ->
            when (val seed = page.seed) {
                is PageSeed.Title -> recap.ranked.getOrNull(seed.rank)?.let { titles[it.mediaId]?.color } ?: topColor
                PageSeed.TopGenre -> recap.genres.firstOrNull()?.let { genreSeed(it.name) } ?: topColor
                PageSeed.App -> null
            }
        }
        return StoryModel(
            period = stored.period,
            recap = recap,
            userName = userName,
            avatarUrl = avatarUrl,
            titles = titles,
            pages = pages,
            seeds = seeds,
            previousEpisodes = previous?.recap?.episodes?.takeIf { it > 0 },
            rankNote = rankNote(recap, stored.media),
            card = card(stored, userName, titles),
            coverColor = coverColorHex(stored),
            isPreview = isPreview,
        )
    }

    fun card(stored: StoredRecap, userName: String, titles: Map<Int, StoryTitle> = titles(stored)): ShareSubject.Recap {
        val recap = stored.recap
        val locale = locale
        val period = stored.period
        val month = (period as? RecapPeriod.Month)?.month
        val year = month?.year ?: (period as RecapPeriod.Year).year
        val top = recap.ranked.take(5).mapNotNull { ranked ->
            titles[ranked.mediaId]?.let { RecapTitle(it.title, it.coverUrl, ranked.minutes) }
        }
        val months = recap.episodesByMonth
        val card = RecapCard(
            userName = userName,
            isYear = month == null,
            periodTitle = month?.let { formatMonthYear(it, locale) } ?: year.toString(),
            monthName = month?.month?.getDisplayName(TextStyle.FULL_STANDALONE, locale),
            year = year,
            episodes = recap.episodes,
            minutes = recap.minutes,
            titles = recap.titles,
            finished = recap.finished,
            activeDays = recap.activeDays,
            top = top,
            genres = recap.genres.take(3).map { it.name },
            months = months?.let { Month.entries.map { m -> it[m] ?: 0 } }.orEmpty(),
            monthInitials = if (months != null) Month.entries.map { it.getDisplayName(TextStyle.NARROW_STANDALONE, locale) } else emptyList(),
            busiestMonthName = recap.busiestMonth?.getDisplayName(TextStyle.FULL_STANDALONE, locale),
            topGenre = recap.genres.firstOrNull()?.name,
            topStudio = recap.studios.firstOrNull()?.name,
            mangaChapters = if (month == null) recap.manga?.chapters else null,
            covers = recap.ranked.take(3).mapNotNull { titles[it.mediaId] }.map { CardImage(it.title, it.coverUrl) },
            coverColor = recap.ranked.firstOrNull()?.let { titles[it.mediaId]?.color },
        )
        return ShareSubject.Recap(card = card, handle = userName, link = AniListUrls.userUrl(userName))
    }

    /** The top title's AniList colour as AniList writes it, for routes that carry a cover colour. */
    fun coverColorHex(stored: StoredRecap): String? =
        stored.recap.ranked.firstOrNull()?.let { stored.media[it.mediaId]?.color }
}
