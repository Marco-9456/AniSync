package com.anisync.android.data

import com.anisync.android.data.local.dao.MediaThemesDao
import com.anisync.android.data.local.entity.MediaThemesEntity
import com.anisync.android.data.network.AnimeThemesApi
import com.anisync.android.data.network.AnimeThemesException
import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.MediaThemes
import com.anisync.android.domain.MediaThemesRepository
import com.anisync.android.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed openings and endings, refreshed from AnimeThemes.
 *
 * Themes change about as often as a show gets a new season, so the cache is kept for a
 * week. A title AnimeThemes has never heard of is cached the same way, as an empty list,
 * because "not listed" is an answer worth remembering.
 */
@Singleton
class MediaThemesRepositoryImpl @Inject constructor(
    private val api: AnimeThemesApi,
    private val dao: MediaThemesDao
) : MediaThemesRepository {

    override fun observeThemes(mediaId: Int): Flow<MediaThemes?> =
        dao.observe(mediaId).map { entity ->
            entity?.let { MediaThemes(animeSlug = it.animeSlug, themes = it.themes.inShowOrder()) }
        }

    override suspend fun refreshThemes(mediaId: Int): Result<MediaThemes> = try {
        val lookup = api.getThemes(mediaId)
        dao.upsert(
            MediaThemesEntity(
                mediaId = mediaId,
                animeSlug = lookup.animeSlug,
                themes = lookup.themes.inShowOrder(),
                fetchedAt = System.currentTimeMillis()
            )
        )
        Result.Success(lookup.copy(themes = lookup.themes.inShowOrder()))
    } catch (e: AnimeThemesException.RateLimited) {
        Result.Error(
            message = "AnimeThemes is rate limiting requests. Try again shortly.",
            code = 429,
            countdownSeconds = e.retryAfterSeconds,
            exception = e
        )
    } catch (e: AnimeThemesException.Network) {
        Result.Error("No connection to AnimeThemes.", exception = e)
    } catch (e: AnimeThemesException.Http) {
        Result.Error("AnimeThemes returned an error (${e.statusCode}).", code = e.statusCode, exception = e)
    } catch (e: Exception) {
        Result.Error("Could not read the AnimeThemes response.", exception = e)
    }

    /**
     * Openings first, then endings, each by their own number. AnimeThemes returns them in the
     * order they were added, which interleaves the two once a show gets a second cour, and the
     * rail has no group headings to explain that away. Applied on read as well as on write so a
     * row cached before this stops being wrong without a refetch.
     */
    private fun List<MediaTheme>.inShowOrder(): List<MediaTheme> =
        sortedWith(compareBy({ it.type.ordinal }, { it.sequence ?: Int.MAX_VALUE }, { it.slug }))

    override suspend fun isStale(mediaId: Int): Boolean {
        val cached = dao.get(mediaId) ?: return true
        return System.currentTimeMillis() - cached.fetchedAt > CACHE_TTL_MS
    }

    private companion object {
        val CACHE_TTL_MS = TimeUnit.DAYS.toMillis(7)
    }
}
