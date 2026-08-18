package com.anisync.android.data.network

import com.anisync.android.BuildConfig
import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.MediaThemes
import com.anisync.android.domain.ThemeType
import com.anisync.android.domain.ThemeVersion
import com.anisync.android.domain.ThemeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only client for the AnimeThemes API, the source of a title's openings and endings.
 *
 * Two requests, because the API refuses a nested include deep enough to do it in one:
 * the AniList id resolves to an AnimeThemes slug through `/resource`, then `/anime/{slug}`
 * returns the themes. Both are anonymous, so there is no key to ship and nothing to
 * refresh. Sparse fieldsets keep the second response to what the UI actually draws.
 *
 * Failures surface as [AnimeThemesException] so the repository can tell a rate limit
 * apart from a title that simply is not listed.
 */
@Singleton
class AnimeThemesApi @Inject constructor() {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Every theme AnimeThemes lists for the AniList title [anilistId]. An empty result means
     * the title is not in their database, which is a real answer rather than a failure.
     */
    suspend fun getThemes(anilistId: Int): MediaThemes = withContext(Dispatchers.IO) {
        val slug = resolveSlug(anilistId) ?: return@withContext MediaThemes()
        MediaThemes(animeSlug = slug, themes = fetchThemes(slug))
    }

    private fun resolveSlug(anilistId: Int): String? {
        val url = BASE_URL.newBuilder()
            .addPathSegment("resource")
            .addQueryParameter("filter[site]", "AniList")
            .addQueryParameter("filter[external_id]", anilistId.toString())
            .addQueryParameter("include", "anime")
            .addQueryParameter("fields[resource]", "id")
            .addQueryParameter("fields[anime]", "slug")
            .build()

        val body = get(url)
        val response = json.decodeFromString<ResourceListDto>(body)
        return response.resources
            .firstNotNullOfOrNull { resource -> resource.anime.firstNotNullOfOrNull { it.slug } }
    }

    private fun fetchThemes(slug: String): List<MediaTheme> {
        val url = BASE_URL.newBuilder()
            .addPathSegment("anime")
            .addPathSegment(slug)
            .addQueryParameter(
                "include",
                "animethemes.song.artists,animethemes.animethemeentries.videos"
            )
            .addQueryParameter("fields[anime]", "id,slug")
            .addQueryParameter("fields[animetheme]", "id,type,sequence,slug")
            .addQueryParameter("fields[song]", "id,title")
            .addQueryParameter("fields[artist]", "name")
            .addQueryParameter("fields[animethemeentry]", "id,version,episodes,notes,spoiler,nsfw")
            .addQueryParameter("fields[video]", "id,link,resolution,nc,subbed,lyrics,source")
            .build()

        val body = get(url)
        val response = json.decodeFromString<AnimeEnvelopeDto>(body)
        return response.anime?.themes.orEmpty().mapNotNull { it.toDomain() }
    }

    private fun get(url: HttpUrl): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw AnimeThemesException.Network(e)
        }

        response.use {
            if (!it.isSuccessful) {
                if (it.code == HTTP_TOO_MANY_REQUESTS) {
                    val retryAfter = it.header("Retry-After")?.toLongOrNull()
                    throw AnimeThemesException.RateLimited(retryAfter)
                }
                throw AnimeThemesException.Http(it.code)
            }
            return it.body?.string() ?: throw AnimeThemesException.Http(it.code)
        }
    }

    companion object {
        private val BASE_URL = "https://api.animethemes.moe".toHttpUrl()
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 20L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val USER_AGENT =
            "AniSync/${BuildConfig.VERSION_NAME} (+https://github.com/Marco-9456/AniSync)"
    }
}

/** What the AnimeThemes lookup can fail with, kept apart so a rate limit can be retried. */
sealed class AnimeThemesException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class Network(cause: Throwable) : AnimeThemesException("AnimeThemes unreachable", cause)

    class RateLimited(val retryAfterSeconds: Long?) :
        AnimeThemesException("AnimeThemes rate limit reached")

    class Http(val statusCode: Int) : AnimeThemesException("AnimeThemes returned $statusCode")
}

// --- Wire format ---

@Serializable
private data class ResourceListDto(val resources: List<ResourceDto> = emptyList())

@Serializable
private data class ResourceDto(val anime: List<AnimeRefDto> = emptyList())

@Serializable
private data class AnimeRefDto(val slug: String? = null)

@Serializable
private data class AnimeEnvelopeDto(val anime: AnimeDto? = null)

@Serializable
private data class AnimeDto(
    @SerialName("animethemes") val themes: List<ThemeDto> = emptyList()
)

@Serializable
private data class ThemeDto(
    val id: Int,
    val type: String? = null,
    val sequence: Int? = null,
    val slug: String? = null,
    val song: SongDto? = null,
    @SerialName("animethemeentries") val entries: List<EntryDto> = emptyList()
)

@Serializable
private data class SongDto(
    val title: String? = null,
    val artists: List<ArtistDto> = emptyList()
)

@Serializable
private data class ArtistDto(val name: String? = null)

@Serializable
private data class EntryDto(
    val id: Int,
    val version: Int? = null,
    val episodes: String? = null,
    val notes: String? = null,
    val spoiler: Boolean = false,
    val nsfw: Boolean = false,
    val videos: List<VideoDto> = emptyList()
)

@Serializable
private data class VideoDto(
    val id: Int,
    val link: String? = null,
    val resolution: Int? = null,
    val nc: Boolean = false,
    val subbed: Boolean = false,
    val lyrics: Boolean = false,
    val source: String? = null
)

private fun ThemeDto.toDomain(): MediaTheme? {
    val themeType = when (type?.uppercase()) {
        "OP" -> ThemeType.OP
        "ED" -> ThemeType.ED
        else -> return null
    }
    val versions = entries.map { it.toDomain() }.filter { it.videos.isNotEmpty() }
    if (versions.isEmpty()) return null

    return MediaTheme(
        id = id,
        type = themeType,
        sequence = sequence,
        slug = slug ?: (themeType.name + (sequence ?: 1)),
        songTitle = song?.title?.takeIf { it.isNotBlank() },
        artists = song?.artists.orEmpty().mapNotNull { it.name?.takeIf(String::isNotBlank) },
        versions = versions
    )
}

private fun EntryDto.toDomain(): ThemeVersion = ThemeVersion(
    id = id,
    version = version ?: 1,
    rawEpisodes = episodes,
    notes = notes?.takeIf { it.isNotBlank() },
    spoiler = spoiler,
    nsfw = nsfw,
    videos = videos.mapNotNull { it.toDomain() }
)

private fun VideoDto.toDomain(): ThemeVideo? {
    val href = link?.takeIf { it.isNotBlank() } ?: return null
    return ThemeVideo(
        id = id,
        url = href,
        resolution = resolution,
        creditless = nc,
        subbed = subbed,
        lyrics = lyrics,
        source = source?.takeIf { it.isNotBlank() }
    )
}
