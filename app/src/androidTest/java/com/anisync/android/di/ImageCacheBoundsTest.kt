package com.anisync.android.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins the image disk cache ceiling.
 *
 * Coil computes `maxSizePercent` from the filesystem's *total* blocks, not its free space, so on
 * any device past roughly 6 GB the percentage never binds and the ceiling is what actually bounds
 * the cache. That number is the whole point of the setting and it is invisible from outside the
 * process, so assert it here.
 *
 * The loader is shut down afterwards: Coil treats two live [coil.disk.DiskCache] instances over
 * one directory as a corruption risk, and this builds a second one alongside whatever the running
 * app holds.
 */
@RunWith(AndroidJUnit4::class)
class ImageCacheBoundsTest {

    private var imageLoader: ImageLoader? = null

    @After
    fun releaseLoader() {
        imageLoader?.shutdown()
        imageLoader = null
    }

    @OptIn(ExperimentalCoilApi::class)
    @Test
    fun diskCacheStaysWithinItsDeclaredBounds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = ImageLoaderModule.provideImageLoader(context).also { imageLoader = it }
        val diskCache = requireNotNull(loader.diskCache)

        assertTrue(
            "expected a size between the floor and the ceiling, was ${diskCache.maxSize}",
            diskCache.maxSize in FLOOR_BYTES..CEILING_BYTES
        )
        assertEquals(
            "a test device this size should land on the ceiling",
            CEILING_BYTES,
            diskCache.maxSize
        )
    }

    private companion object {
        const val FLOOR_BYTES = 32L * 1024 * 1024
        const val CEILING_BYTES = 128L * 1024 * 1024
    }
}
