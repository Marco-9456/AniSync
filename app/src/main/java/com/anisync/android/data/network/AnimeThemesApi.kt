package com.anisync.android.data.network

import com.anisync.android.BuildConfig
import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.MediaThemes
import com.anisync.android.domain.ThemeAudio
import com.anisync.android.domain.ThemePerformer
import com.anisync.android.domain.ThemeType
import com.anisync.android.domain.ThemeVersion
import com.anisync.android.domain.ThemeVideo
import com.anisync.android.domain.VideoOverlap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only client for the AnimeThemes API, the source of a title's openings and endings.
 *
 * One GraphQL request resolves the AniList id and returns the themes with it. It is
 * anonymous, so there is no key to ship and nothing to refresh.
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
            .encryptedClientHello()
            .build()
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Every theme AnimeThemes lists for the AniList title [anilistId]. An empty result means
     * the title is not in their database, which is a real answer rather than a failure.
     */
    suspend fun getThemes(anilistId: Int): MediaThemes = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("query", THEMES_QUERY)
            putJsonObject("variables") {
                putJsonArray("id") { add(anilistId) }
            }
        }
        decodeThemes(post(payload.toString()))
    }

    internal fun decodeThemes(body: String): MediaThemes {
        val response = json.decodeFromString<ThemesResponseDto>(body)
        if (response.errors.isNotEmpty()) {
            throw AnimeThemesException.Query(response.errors.mapNotNull { it.message })
        }
        val anime = response.data?.anime?.firstOrNull() ?: return MediaThemes()
        return MediaThemes(animeSlug = anime.slug, themes = anime.themes.mapNotNull { it.toDomain() })
    }

    private fun post(body: String): String {
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
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
        private const val ENDPOINT = "https://graphql.animethemes.moe/"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 20L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val USER_AGENT =
            "AniSync/${BuildConfig.VERSION_NAME} (+https://github.com/Marco-9456/AniSync)"

        private val THEMES_QUERY = """
            query(${'$'}id: [Int!]) {
              findAnimeByExternalSite(site: ANILIST, id: ${'$'}id) {
                slug
                animethemes {
                  id type sequence slug
                  group { name }
                  song {
                    title { romaji }
                    performances {
                      relevance as alias memberAs memberAlias
                      artist { name { main } }
                      member { name { main } }
                    }
                  }
                  animethemeentries {
                    id version episodes notes spoiler nsfw
                    videos {
                      nodes { id link resolution nc subbed lyrics source overlap audio { link size } }
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }
}

/** What the AnimeThemes lookup can fail with, kept apart so a rate limit can be retried. */
sealed class AnimeThemesException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class Network(cause: Throwable) : AnimeThemesException("AnimeThemes unreachable", cause)

    class RateLimited(val retryAfterSeconds: Long?) :
        AnimeThemesException("AnimeThemes rate limit reached")

    class Http(val statusCode: Int) : AnimeThemesException("AnimeThemes returned $statusCode")

    /**
     * GraphQL reports validation, depth and complexity failures with HTTP 200, so the body has
     * to be checked. Without this a rejected query would read as a title with no themes and be
     * cached for a week.
     */
    class Query(val messages: List<String>) :
        AnimeThemesException("AnimeThemes rejected the query: ${messages.joinToString()}")
}

// --- Wire format ---

@Serializable
private data class ThemesResponseDto(
    val data: ThemesDataDto? = null,
    val errors: List<GraphQLErrorDto> = emptyList()
)

@Serializable
private data class GraphQLErrorDto(val message: String? = null)

@Serializable
private data class ThemesDataDto(
    @SerialName("findAnimeByExternalSite") val anime: List<AnimeDto>? = null
)

@Serializable
private data class AnimeDto(
    val slug: String? = null,
    @SerialName("animethemes") val themes: List<ThemeDto> = emptyList()
)

@Serializable
private data class ThemeDto(
    val id: Int,
    val type: String? = null,
    val sequence: Int? = null,
    val slug: String? = null,
    val group: ThemeGroupDto? = null,
    val song: SongDto? = null,
    @SerialName("animethemeentries") val entries: List<EntryDto> = emptyList()
)

@Serializable
private data class ThemeGroupDto(val name: String? = null)

@Serializable
private data class SongDto(
    val title: SongTitleDto? = null,
    val performances: List<PerformanceDto> = emptyList()
)

@Serializable
private data class SongTitleDto(val romaji: String? = null)

/**
 * A band is credited once per member, so the same artist can appear several times. [alias] is the
 * name the artist is credited under and [as] the character sung as, with member twins of both.
 */
@Serializable
private data class PerformanceDto(
    val relevance: Int? = null,
    val `as`: String? = null,
    val alias: String? = null,
    val memberAs: String? = null,
    val memberAlias: String? = null,
    val artist: ArtistDto? = null,
    val member: ArtistDto? = null
)

@Serializable
private data class ArtistDto(val name: ArtistNameDto? = null)

@Serializable
private data class ArtistNameDto(val main: String? = null)

@Serializable
private data class EntryDto(
    val id: Int,
    val version: Int? = null,
    val episodes: String? = null,
    val notes: String? = null,
    val spoiler: Boolean = false,
    val nsfw: Boolean = false,
    val videos: VideoConnectionDto = VideoConnectionDto()
)

@Serializable
private data class VideoConnectionDto(val nodes: List<VideoDto> = emptyList())

@Serializable
private data class VideoDto(
    val id: Int,
    val link: String? = null,
    val resolution: Int? = null,
    val nc: Boolean = false,
    val subbed: Boolean = false,
    val lyrics: Boolean = false,
    val source: String? = null,
    val overlap: String? = null,
    val audio: AudioDto? = null
)

@Serializable
private data class AudioDto(val link: String? = null, val size: Long? = null)

private fun ThemeDto.toDomain(): MediaTheme? {
    val themeType = when (type?.uppercase()) {
        "OP" -> ThemeType.OP
        "ED" -> ThemeType.ED
        "IN" -> ThemeType.IN
        else -> return null
    }
    val all = entries.map { it.toDomain() }
    val playable = all.filter { it.videos.isNotEmpty() }
    // Insert songs are catalogued as songs and almost never carry a video, so they are kept for
    // their credits and episodes. An opening or ending with nothing to play is left out.
    val versions = when {
        playable.isNotEmpty() -> playable
        themeType == ThemeType.IN -> all
        else -> emptyList()
    }
    if (versions.isEmpty()) return null

    val performances = song?.performances.orEmpty().sortedBy { it.relevance ?: Int.MAX_VALUE }

    return MediaTheme(
        id = id,
        type = themeType,
        sequence = sequence,
        slug = slug ?: (themeType.name + (sequence ?: "")),
        songTitle = song?.title?.romaji?.takeIf { it.isNotBlank() },
        artists = performances.mapNotNull { it.creditedArtist() }.distinct(),
        versions = versions,
        groupName = group?.name?.takeIf { it.isNotBlank() },
        performers = performances.mapNotNull { it.toPerformer() }.distinct()
    )
}

private fun PerformanceDto.creditedArtist(): String? =
    alias?.takeIf { it.isNotBlank() } ?: artist?.name?.main?.takeIf { it.isNotBlank() }

private fun PerformanceDto.toPerformer(): ThemePerformer? {
    val credited = creditedArtist() ?: return null
    val memberName = memberAlias?.takeIf { it.isNotBlank() }
        ?: member?.name?.main?.takeIf { it.isNotBlank() }
    return if (memberName != null) {
        ThemePerformer(
            name = memberName,
            character = memberAs?.takeIf { it.isNotBlank() },
            band = credited
        )
    } else {
        ThemePerformer(name = credited, character = `as`?.takeIf { it.isNotBlank() })
    }
}

private fun EntryDto.toDomain(): ThemeVersion {
    // Every video of an entry points at the same audio file, so the first one found stands for all.
    val audio = videos.nodes.firstNotNullOfOrNull { video ->
        video.audio?.link?.takeIf { it.isNotBlank() }?.let { ThemeAudio(it, video.audio.size) }
    }
    return ThemeVersion(
        id = id,
        version = version ?: 1,
        rawEpisodes = episodes,
        notes = notes?.takeIf { it.isNotBlank() },
        spoiler = spoiler,
        nsfw = nsfw,
        videos = videos.nodes.mapNotNull { it.toDomain() },
        audio = audio
    )
}

private fun VideoDto.toDomain(): ThemeVideo? {
    val href = link?.takeIf { it.isNotBlank() } ?: return null
    return ThemeVideo(
        id = id,
        url = href,
        resolution = resolution,
        creditless = nc,
        subbed = subbed,
        lyrics = lyrics,
        source = source?.takeIf { it.isNotBlank() },
        overlap = when (overlap?.uppercase()) {
            "TRANS" -> VideoOverlap.Transition
            "OVER" -> VideoOverlap.Over
            else -> VideoOverlap.None
        }
    )
}
