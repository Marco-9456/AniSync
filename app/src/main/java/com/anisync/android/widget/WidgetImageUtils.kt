package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetImageUtils {
    suspend fun loadBitmap(
        context: Context, 
        url: String?, 
        width: Int = 200, 
        height: Int = 300,
        skipCache: Boolean = false
    ): Bitmap? {
        if (url.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
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
        }
    }
}
