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
 * Catbox.moe — permanent free hosting. POSTs `reqtype=fileupload` + `fileToUpload`
 * to https://catbox.moe/user/api.php; response body is the plain-text URL.
 *
 * [userhash] is optional and binds the upload to a Catbox account, allowing later
 * deletion. Anonymous uploads work without it but cannot be removed.
 */
class CatboxUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) : MediaUploader {

    /** Set by the factory before each upload — binds the upload to a Catbox account when non-blank. */
    var userhash: String = ""

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
                .apply { if (userhash.isNotBlank()) addFormDataPart("userhash", userhash) }
                .addFormDataPart("fileToUpload", filename, fileBody)
                .build()
            val request = Request.Builder()
                .url(ENDPOINT)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                // URL response is < 200 bytes; cap to defend against misconfigured hosts.
                val text = response.peekBody(MAX_RESPONSE_BYTES).string().trim()
                if (!response.isSuccessful || !text.startsWith("http")) {
                    error("Catbox upload failed (${response.code}): ${text.take(200)}")
                }
                UploadedMedia(url = text, mime = mime, kind = mediaKindFromMime(mime))
            }
        }
    }

    companion object {
        private const val ENDPOINT = "https://catbox.moe/user/api.php"
        private const val MAX_RESPONSE_BYTES = 4L * 1024L
    }
}
