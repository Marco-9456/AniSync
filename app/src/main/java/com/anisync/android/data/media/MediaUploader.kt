package com.anisync.android.data.media

import android.net.Uri
import com.anisync.android.domain.media.UploadedMedia

/**
 * Single-shot upload of a file behind a [Uri] to a third-party host. Implementations
 * read the bytes lazily via the app's [android.content.ContentResolver] and stream
 * them; they should not load the file into memory eagerly.
 *
 * Returning a [Result.failure] means the URL was never produced — callers can show
 * the [Throwable.message] verbatim. Cancellation throws CancellationException as
 * usual; callers should let it propagate.
 */
interface MediaUploader {
    suspend fun upload(
        uri: Uri,
        mime: String,
        onProgress: (uploadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<UploadedMedia>
}
