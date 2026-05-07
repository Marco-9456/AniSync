package com.anisync.android.data.media

import android.content.Context
import android.net.Uri
import com.anisync.android.domain.media.UploadedMedia
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Imgur anonymous upload. Uses the v3 API with an `Authorization: Client-ID <id>`
 * header. Limits at the time of writing: 10 MB / image, 200 MB / video.
 */
class ImgurUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) : MediaUploader {

    /** Set by the factory before each upload. */
    var clientId: String = ""

    override suspend fun upload(
        uri: Uri,
        mime: String,
        onProgress: (Long, Long) -> Unit
    ): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        runCatching {
            require(clientId.isNotBlank()) {
                "Imgur Client-ID is not set. Open Settings → Media upload to add one."
            }
            val filename = context.queryDisplayName(uri)
            val fileBody = UriRequestBody(context, uri, mime) { up, total -> onProgress(up, total) }
            val isVideo = mime.startsWith("video/", ignoreCase = true)
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(if (isVideo) "video" else "image", filename, fileBody)
                .build()
            val request = Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", "Client-ID $clientId")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Imgur upload failed (${response.code}): ${text.take(200)}")
                }
                val link = runCatching {
                    Json.parseToJsonElement(text)
                        .jsonObject["data"]
                        ?.jsonObject?.get("link")
                        ?.jsonPrimitive?.content
                }.getOrNull()
                    ?: error("Imgur returned no link in: ${text.take(200)}")
                UploadedMedia(url = link, mime = mime, kind = mediaKindFromMime(mime))
            }
        }
    }

    companion object {
        private const val ENDPOINT = "https://api.imgur.com/3/upload"
    }
}
