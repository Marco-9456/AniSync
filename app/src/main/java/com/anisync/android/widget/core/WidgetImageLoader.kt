package com.anisync.android.widget.core

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/**
 * Image loading for widgets.
 *
 * Two rules that do not apply elsewhere in the app:
 *
 *  - **No hardware bitmaps.** A `Config.HARDWARE` bitmap has no pixel data in the client process
 *    and cannot be written into the `RemoteViews` parcel, so it renders as nothing. Coil enables
 *    them by default from API 28, which makes this an easy blank-image bug to ship.
 *  - **Sizes come from [WidgetImageBudget]**, never a constant, because the platform caps the
 *    total bitmap memory of one widget update and that cap is split across every image on screen.
 */
object WidgetImageLoader {

    private fun loader(context: Context) = context.applicationContext.imageLoader

    suspend fun loadBitmap(
        context: Context,
        url: String?,
        size: Size,
        skipCache: Boolean = false
    ): Bitmap? {
        if (url.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(size.width, size.height)
                    .allowHardware(false)
                    .apply {
                        if (skipCache) {
                            memoryCachePolicy(CachePolicy.DISABLED)
                            diskCachePolicy(CachePolicy.DISABLED)
                        }
                    }
                    .build()

                loader(context).execute(request).drawable?.toBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Load a batch of covers in parallel, keyed by caller id.
     *
     * The batch is what makes budgeting possible: the per-image size falls out of how many are
     * being loaded, so a day with thirty episodes decodes thirty small covers rather than thirty
     * full-size ones and blowing the update.
     */
    suspend fun loadCovers(
        context: Context,
        requests: List<CoverRequest>,
        size: Size
    ): Map<Int, Bitmap?> {
        if (requests.isEmpty()) return emptyMap()
        return supervisorScope {
            requests
                .map { request ->
                    async(Dispatchers.IO) { request.id to loadBitmap(context, request.url, size) }
                }
                .awaitAll()
                .toMap()
        }
    }

    /** One cover to fetch. [id] is whatever the caller keys its UI list by. */
    data class CoverRequest(val id: Int, val url: String?)
}
