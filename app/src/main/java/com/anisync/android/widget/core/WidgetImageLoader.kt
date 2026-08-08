package com.anisync.android.widget.core

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/**
 * Image loading for widgets.
 *
 * Two rules that do not apply anywhere else in the app:
 *
 *  - No hardware bitmaps. A Config.HARDWARE bitmap holds no pixels in our process and cannot go
 *    into the RemoteViews parcel, so it draws as nothing. Coil turns them on by default from API
 *    28, which makes this an easy blank image bug to ship.
 *  - Sizes come from [WidgetImageBudget], never a constant. The platform caps the total bitmap
 *    memory of one update and that cap is split across every image on screen.
 */
object WidgetImageLoader {

    private fun loader(context: Context) = context.applicationContext.imageLoader

    suspend fun loadBitmap(
        context: Context,
        url: String?,
        size: Size,
        cornerRadiusDp: Int = COVER_CORNER_RADIUS_DP,
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
                        if (cornerRadiusDp > 0) {
                            // Rounded into the bitmap. A rounded background drawable is no help,
                            // the bitmap draws over it with square corners, and clipToOutline is
                            // not remotable.
                            transformations(
                                RoundedCornersTransformation(context.widgetPx(cornerRadiusDp).toFloat())
                            )
                        }
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
     * Loads a batch of covers in parallel, keyed by caller id.
     *
     * Batching is what makes the budget work: the per image size comes out of how many we are
     * loading, so a day with thirty episodes decodes thirty small covers instead of thirty full
     * size ones and blowing the update.
     */
    suspend fun loadCovers(
        context: Context,
        requests: List<CoverRequest>,
        size: Size,
        cornerRadiusDp: Int = COVER_CORNER_RADIUS_DP
    ): Map<Int, Bitmap?> {
        if (requests.isEmpty()) return emptyMap()
        return supervisorScope {
            requests
                .map { request ->
                    async(Dispatchers.IO) {
                        request.id to loadBitmap(context, request.url, size, cornerRadiusDp)
                    }
                }
                .awaitAll()
                .toMap()
        }
    }

    /** One cover to fetch. [id] is whatever the caller keys its list by. */
    data class CoverRequest(val id: Int, val url: String?)

    /** Same rounding the widget posters use. */
    const val COVER_CORNER_RADIUS_DP = 8
}
