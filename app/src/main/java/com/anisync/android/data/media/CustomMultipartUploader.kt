package com.anisync.android.data.media

import android.content.Context
import android.net.Uri
import com.anisync.android.domain.media.UploadedMedia
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Power-user host: any service that accepts a multipart POST and returns a URL,
 * either as the raw response body or inside a JSON path. Configured via
 * [config] (URL, file field, optional auth header, optional JSON path).
 *
 * If [Config.responseJsonPath] is empty/null, the response body is treated as
 * the URL (works for catbox-clones, 0x0.st, etc.). Otherwise dotted path
 * traversal selects the URL from the JSON tree.
 */
class CustomMultipartUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) : MediaUploader {

    /** Set by the factory before each upload. */
    var config: Config = Config()

    data class Config(
        val url: String = "",
        val fileFieldName: String = "fileToUpload",
        val authHeader: String? = null,
        val responseJsonPath: String? = null
    )

    override suspend fun upload(
        uri: Uri,
        mime: String,
        onProgress: (Long, Long) -> Unit
    ): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.url.isNotBlank()) {
                "Custom host URL is not set. Open Settings → Media upload to configure it."
            }
            val filename = context.queryDisplayName(uri)
            val fileBody = UriRequestBody(context, uri, mime) { up, total -> onProgress(up, total) }
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    config.fileFieldName.ifBlank { "fileToUpload" },
                    filename,
                    fileBody
                )
                .build()
            val builder = Request.Builder().url(config.url).post(body)
            config.authHeader?.takeIf { it.isNotBlank() }?.let { header ->
                val (name, value) = parseHeader(header)
                builder.header(name, value)
            }
            client.newCall(builder.build()).execute().use { response ->
                // Cap the consumed body so a misconfigured host returning a large
                // payload (HTML dump, raw image echo, etc.) cannot OOM the parser.
                val text = response.peekBody(MAX_RESPONSE_BYTES).string()
                if (!response.isSuccessful) {
                    error("Custom host upload failed (${response.code}): ${text.take(200)}")
                }
                val resolved = resolveUrl(text, config.responseJsonPath)
                    ?: error("Could not extract URL from response: ${text.take(200)}")
                UploadedMedia(url = resolved, mime = mime, kind = mediaKindFromMime(mime))
            }
        }
    }

    private fun resolveUrl(body: String, jsonPath: String?): String? {
        val trimmed = body.trim()
        if (jsonPath.isNullOrBlank()) {
            return trimmed.takeIf { it.startsWith("http") }
        }
        return runCatching {
            var node: JsonElement? = Json.parseToJsonElement(trimmed)
            for (part in jsonPath.split('.')) {
                if (part.isBlank()) continue
                node = (node as? JsonObject)?.get(part)
            }
            (node as? JsonPrimitive)?.contentOrNull
        }.getOrNull()
    }

    companion object {
        private const val MAX_RESPONSE_BYTES = 64L * 1024L
    }

    private fun parseHeader(header: String): Pair<String, String> {
        val idx = header.indexOf(':')
        return if (idx <= 0) "Authorization" to header.trim()
        else header.substring(0, idx).trim() to header.substring(idx + 1).trim()
    }

    private val JsonPrimitive.contentOrNull: String? get() = runCatching { content }.getOrNull()
}
