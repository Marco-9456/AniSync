package com.anisync.android.di

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.anisync.android.data.media.MediaHttp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides an optimized ImageLoader with:
 * - 256MB disk cache for offline image storage
 * - 12.5% memory cache of available app heap
 * - 200ms crossfade animation for smooth loading
 * - Hardware bitmaps enabled for GPU-accelerated rendering
 * - RGB_565 configuration for reduced memory footprint
 * - GIF decoding (hardware-accelerated on API 28+)
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    private const val DISK_CACHE_SIZE = 256L * 1024 * 1024 // 256 MB — fewer re-fetches of revisited covers/banners
    private const val MEMORY_CACHE_PERCENT = 0.125         // 12.5% of app heap
    private const val CROSSFADE_DURATION_MS = 200

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {
        return ImageLoader.Builder(context)
            // Browser User-Agent + flaky-network timeouts/retry. CDNs that bot-filter the
            // default OkHttp UA (catbox, imgur, …) are the main reason embedded images
            // "sometimes don't load". See [MediaHttp].
            .okHttpClient(MediaHttp.imageClient)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .bitmapConfig(Bitmap.Config.RGB_565)
            // Smooth crossfade transition when images load
            .crossfade(CROSSFADE_DURATION_MS)
            // Enable hardware bitmaps for faster GPU rendering
            .allowHardware(true)
            // Disk cache for offline image storage
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(DISK_CACHE_SIZE)
                    .build()
            }
            // Memory cache using percentage of available heap
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .build()
            }
            // Ignore server cache headers: AniList cover/banner URLs are content-addressed
            // and effectively immutable, but the CDN sends short/again-validate headers that
            // make Coil re-fetch on every view. Trusting our own disk cache instead means
            // covers load instantly on revisit and survive offline.
            .respectCacheHeaders(false)
            .build()
    }
}