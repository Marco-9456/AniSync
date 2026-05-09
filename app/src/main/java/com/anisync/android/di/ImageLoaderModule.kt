package com.anisync.android.di

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides an optimized ImageLoader with:
 * - 50MB disk cache for offline image storage
 * - 12.5% memory cache of available app heap
 * - 200ms crossfade animation for smooth loading
 * - Hardware bitmaps enabled for GPU-accelerated rendering
 * - GIF decoding (hardware-accelerated on API 28+)
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    private const val DISK_CACHE_SIZE = 50L * 1024 * 1024  // 50 MB
    private const val MEMORY_CACHE_PERCENT = 0.125         // 12.5% of app heap
    private const val CROSSFADE_DURATION_MS = 200

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
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
            // Respect cache headers from server
            .respectCacheHeaders(true)
            .build()
    }
}
