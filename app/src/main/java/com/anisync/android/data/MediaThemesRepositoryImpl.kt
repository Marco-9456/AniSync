package com.anisync.android.data

import com.anisync.android.data.local.dao.MediaThemesDao
import com.anisync.android.data.local.entity.MediaThemesEntity
import com.anisync.android.data.network.AnimeThemesApi
import com.anisync.android.data.network.AnimeThemesException
import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.MediaThemes
import com.anisync.android.domain.MediaThemesRepository
import com.anisync.android.domain.Result
import kotlinx.coroutines.CancellationException
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
        if (cached.fetchedAt < FORMAT_SINCE_MS) return true
        return System.currentTimeMillis() - cached.fetchedAt > CACHE_TTL_MS
    }

    override suspend fun themesFor(mediaIds: List<Int>): Map<Int, List<MediaTheme>> {
        val found = LinkedHashMap<Int, List<MediaTheme>>()
        val missing = ArrayList<Int>()
        for (id in mediaIds.distinct()) {
            val cached = dao.get(id)
            if (cached != null && !isStale(id)) found[id] = cached.themes.inShowOrder() else missing += id
        }
        if (missing.isEmpty()) return found
        val looked = try {
            api.getThemes(missing)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return found
        }
        val now = System.currentTimeMillis()
        for (id in missing) {
            val lookup = looked[id] ?: MediaThemes()
            val themes = lookup.themes.inShowOrder()
            dao.upsert(MediaThemesEntity(mediaId = id, animeSlug = lookup.animeSlug, themes = themes, fetchedAt = now))
            found[id] = themes
        }
        return found
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
        if (cached.fetchedAt < FORMAT_SINCE_MS) return true
        return System.currentTimeMillis() - cached.fetchedAt > CACHE_TTL_MS
    }

    override suspend fun themesFor(mediaIds: List<Int>): Map<Int, List<MediaTheme>> {
        val found = LinkedHashMap<Int, List<MediaTheme>>()
        val missing = ArrayList<Int>()
        for (id in mediaIds.distinct()) {
            val cached = dao.get(id)
            if (cached != null && !isStale(id)) found[id] = cached.themes.inShowOrder() else missing += id
        }
        if (missing.isEmpty()) return found
        val looked = try {
            api.getThemes(missing)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return found
        }
        val now = System.currentTimeMillis()
        for (id in missing) {
            val lookup = looked[id] ?: MediaThemes()
            val themes = lookup.themes.inShowOrder()
            dao.upsert(MediaThemesEntity(mediaId = id, animeSlug = lookup.animeSlug, themes = themes, fetchedAt = now))
            found[id] = themes
        }
        return found
    }

    /** One batch for [ids], stored as it lands. A failure is tried once more, a rejected query is not. */
    private suspend fun lookUp(ids: List<Int>): Map<Int, List<MediaTheme>>? {
        repeat(LOOKUP_ATTEMPTS) { attempt ->
            try {
                val looked = api.getThemes(ids)
                val now = System.currentTimeMillis()
                return ids.associateWith { id ->
                    val lookup = looked[id] ?: MediaThemes()
                    val themes = lookup.themes.inShowOrder()
                    dao.upsert(MediaThemesEntity(mediaId = id, animeSlug = lookup.animeSlug, themes = themes, fetchedAt = now))
                    themes
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AnimeThemesException.Query) {
                Log.w(TAG, "AnimeThemes refused the batch", e)
                return null
            } catch (e: Exception) {
                Log.w(TAG, "AnimeThemes batch failed", e)
                if (attempt == LOOKUP_ATTEMPTS - 1) return null
                val wait = (e as? AnimeThemesException.RateLimited)?.retryAfterSeconds?.times(1000L) ?: RETRY_DELAY_MS
                delay(wait.coerceAtMost(MAX_RETRY_DELAY_MS))
            }
        }
        return null
    }

    private companion object {
        val CACHE_TTL_MS = TimeUnit.DAYS.toMillis(7)

        /**
         * Rows stored before groups, credits, audio and insert songs were read are missing all
         * four, so they refetch once instead of waiting out the week. Move this forward whenever
         * the stored shape gains something a cached row cannot have.
         */
        const val FORMAT_SINCE_MS = 1_792_188_000_000L // 2026-10-16 22:00 UTC
    }
}
