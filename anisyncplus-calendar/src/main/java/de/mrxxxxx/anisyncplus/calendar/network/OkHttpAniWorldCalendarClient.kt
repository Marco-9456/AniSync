package de.mrxxxxx.anisyncplus.calendar.network

import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarClient
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarResponse
import de.mrxxxxx.anisyncplus.calendar.domain.SOURCE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.time.Clock

sealed class AniWorldClientException(message: String) : IOException(message) {
    class HttpStatus(val status: Int) : AniWorldClientException("AniWorld returned HTTP $status")
    class InvalidContentType(val value: String?) : AniWorldClientException("Unexpected Content-Type: $value")
    class BodyTooLarge(val limitBytes: Int) : AniWorldClientException("AniWorld response exceeds $limitBytes bytes")
    class EmptyBody : AniWorldClientException("AniWorld returned an empty body")
}

class OkHttpAniWorldCalendarClient(
    private val client: OkHttpClient,
    private val sourceUrl: String = SOURCE_URL,
    private val clock: Clock = Clock.systemUTC(),
    private val diagnosticLogger: (String) -> Unit = {}
) : AniWorldCalendarClient {
    override suspend fun fetch(): AniWorldCalendarResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(sourceUrl)
            .header("User-Agent", "AniSyncPlus/1 AniWorldCalendarClient")
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "de-DE,de;q=0.9")
            .get()
            .build()

        diagnostic("request_start host=${request.url.host} path=${request.url.encodedPath}")
        try {
            client.newCall(request).execute().use { response ->
                val responseUrl = response.request.url
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
                diagnostic(
                    "response_ok status=${response.code} host=${responseUrl.host} " +
                        "path=${responseUrl.encodedPath} contentType=${safe(contentType)} " +
                        "bytes=${bytes.size} sha256=${sha256(bytes)} title=${safeTitle(html)}"
                )
                AniWorldCalendarResponse(html, clock.instant(), response.code)
            }
        } catch (throwable: Throwable) {
            diagnostic(
                "request_failed type=${throwable::class.simpleName ?: "UnknownError"} " +
                    "message=${safe(throwable.message)}"
            )
            throw throwable
        }
    }

    private fun diagnostic(message: String) {
        runCatching { diagnosticLogger(message) }
    }

    private fun safe(value: String?): String = value.orEmpty()
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .take(MAX_DIAGNOSTIC_VALUE_CHARS)

    private fun safeTitle(html: String): String {
        val raw = TITLE_REGEX.find(html.take(TITLE_SCAN_CHARS))?.groupValues?.get(1)
        return safe(raw?.replace(Regex("\\s+"), " ")?.trim())
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    companion object {
        const val MAX_BODY_BYTES = 2 * 1024 * 1024
        private const val TITLE_SCAN_CHARS = 64 * 1024
        private const val MAX_DIAGNOSTIC_VALUE_CHARS = 160
        private val TITLE_REGEX = Regex(
            "<title[^>]*>(.*?)</title>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
}
