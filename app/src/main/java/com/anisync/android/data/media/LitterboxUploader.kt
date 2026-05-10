package com.anisync.android.data.media

import android.content.Context
import android.net.Uri
import com.anisync.android.domain.media.UploadedMedia
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Litterbox — temporary file hosting on the same infrastructure as Catbox. The
 * uploaded file is auto-deleted after [time] (`1h`, `24h`, or `72h`).
 */
class LitterboxUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) : MediaUploader {

    /** Set by the factory before each upload — keeps the class parameter-free for Hilt. */
    var time: String = "1h"

    override suspend fun upload(
        uri: Uri,
        mime: String,
        onProgress: (Long, Long) -> Unit
    ): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        runCatching {
            val filename = context.queryDisplayName(uri)
            val fileBody = UriRequestBody(context, uri, mime) { up, total -> onProgress(up, total) }
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("time", time)
                .addFormDataPart("fileToUpload", filename, fileBody)
                .build()
            val request = Request.Builder()
                .url(ENDPOINT)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.peekBody(MAX_RESPONSE_BYTES).string().trim()
                if (!response.isSuccessful || !text.startsWith("http")) {
                    error("Litterbox upload failed (${response.code}): ${text.take(200)}")
                }
                UploadedMedia(url = text, mime = mime, kind = mediaKindFromMime(mime))
            }
        }
    }

    companion object {
        private const val ENDPOINT = "https://litterbox.catbox.moe/resources/internals/api.php"
        private const val MAX_RESPONSE_BYTES = 4L * 1024L
    }
}
