package de.mrxxxxx.anisyncplus.calendar.network

import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarClient
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarResponse
import de.mrxxxxx.anisyncplus.calendar.domain.SOURCE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Clock

sealed class AniWorldClientException(message: String) : IOException(message) {
    class HttpStatus(val status: Int) : AniWorldClientException("AniWorld returned HTTP $status")
    class InvalidContentType(val value: String?) : AniWorldClientException("Unexpected Content-Type: $value")
    class BodyTooLarge(val limitBytes: Int) : AniWorldClientException("AniWorld response exceeds $limitBytes bytes")
    class EmptyBody : AniWorldClientException("AniWorld returned an empty body")
    class BlockPage : AniWorldClientException("AniWorld returned a block or challenge page")
}

class OkHttpAniWorldCalendarClient(
    private val client: OkHttpClient,
    private val sourceUrl: String = SOURCE_URL,
    private val clock: Clock = Clock.systemUTC()
) : AniWorldCalendarClient {
    override suspend fun fetch(): AniWorldCalendarResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(sourceUrl)
            .header("User-Agent", "AniSyncPlus/1 AniWorldCalendarClient")
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "de-DE,de;q=0.9")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw AniWorldClientException.HttpStatus(response.code)
            val contentType = response.header("Content-Type")
            if (contentType?.lowercase()?.let { "text/html" in it || "application/xhtml+xml" in it } != true) {
                throw AniWorldClientException.InvalidContentType(contentType)
            }
            val body = response.body ?: throw AniWorldClientException.EmptyBody()
            val bytes = body.byteStream().use { it.readNBytes(MAX_BODY_BYTES + 1) }
            if (bytes.size > MAX_BODY_BYTES) throw AniWorldClientException.BodyTooLarge(MAX_BODY_BYTES)
            val html = bytes.toString(Charsets.UTF_8)
            if (html.isBlank()) throw AniWorldClientException.EmptyBody()
            val lowercase = html.take(CHALLENGE_SCAN_CHARS).lowercase()
            if (BLOCK_MARKERS.any(lowercase::contains)) throw AniWorldClientException.BlockPage()
            AniWorldCalendarResponse(html, clock.instant(), response.code)
        }
    }

    companion object {
        const val MAX_BODY_BYTES = 2 * 1024 * 1024
        private const val CHALLENGE_SCAN_CHARS = 64 * 1024
        private val BLOCK_MARKERS = listOf("just a moment", "cf-chl-", "cloudflare", "access denied", "captcha")
    }
}
