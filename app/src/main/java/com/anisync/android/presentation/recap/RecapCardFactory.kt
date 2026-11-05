package com.anisync.android.presentation.recap

import android.content.Context
import com.anisync.android.data.AppSettings
import com.anisync.android.data.util.AppLocale
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.isShort
import com.anisync.android.presentation.share.cards.CardImage
import com.anisync.android.presentation.share.cards.RecapCard
import com.anisync.android.presentation.share.cards.RecapTitle
import com.anisync.android.presentation.share.cards.ShareSubject
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
            periodTitle = month?.let { "${it.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${it.year}" } ?: year.toString(),
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
