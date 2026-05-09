package com.anisync.android.widget.core

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Centralized image loading utilities for widgets.
 * Part of the core package for shared widget infrastructure.
 */
object WidgetImageLoader {

    private fun getImageLoader(context: Context) = context.applicationContext.imageLoader

    /**
     * Load a bitmap from a URL for use in widgets.
     *
     * @param context Application context
     * @param url Image URL to load
     * @param width Desired width in pixels
     * @param height Desired height in pixels
     * @param skipCache If true, bypasses cache (useful when image may have changed)
     * @return Loaded Bitmap or null if loading failed
     */
    suspend fun loadBitmap(
        context: Context,
        url: String?,
        width: Int = 200,
        height: Int = 300,
        skipCache: Boolean = false
    ): Bitmap? {
        if (url.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                val loader = getImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(width, height)
                    .apply {
                        if (skipCache) {
                            memoryCachePolicy(CachePolicy.DISABLED)
                            diskCachePolicy(CachePolicy.DISABLED)
                        }
                    }
                    .build()

                val result = loader.execute(request)
                result.drawable?.toBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
}
