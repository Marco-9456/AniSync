package com.anisync.android.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import com.anisync.android.cache.Cache.cache
import com.anisync.android.di.ImageLoaderModule
import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Covers the promise the Storage screen makes: the figure it shows is what the button reclaims.
 *
 * The bug this replaced was a clear path that only knew about the two cache directories, so the
 * Apollo cache in `databases/` and the downloaded update APK were neither counted nor removed. On a
 * lightly used install that meant reporting 2 MB while holding 10 MB.
 */
@RunWith(AndroidJUnit4::class)
class CacheInventoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var imageLoader: ImageLoader? = null

    private val apkDir = File(context.externalCacheDir, "apk")
    private val legacyApkDir = File(context.getExternalFilesDir(null), "apk")
    private val strayCacheFile = File(context.cacheDir, "stray-fixture.bin")

    @After
    fun cleanUp() {
        imageLoader?.shutdown()
        imageLoader = null
        apkDir.deleteRecursively()
        legacyApkDir.deleteRecursively()
        strayCacheFile.delete()
    }

    private fun inventory(): CacheInventory {
        val loader = ImageLoaderModule.provideImageLoader(context).also { imageLoader = it }
        val apollo = ApolloClient.Builder()
            .serverUrl("https://graphql.anilist.co")
            .cache(MemoryCacheFactory())
            .build()
        return CacheInventory(context, loader, apollo)
    }

    @Test
    fun countsTheUpdateApkInBothItsOldAndNewHomes() = runBlocking {
        val inventory = inventory()
        val before = inventory.totalBytes()

        apkDir.mkdirs()
        legacyApkDir.mkdirs()
        File(apkDir, "latest.apk").writeBytes(ByteArray(40_000))
        File(legacyApkDir, "latest.apk").writeBytes(ByteArray(20_000))

        val after = inventory.totalBytes()
        assertEquals(
            "both APK locations must be counted, including the one earlier versions used",
            60_000L,
            after - before
        )
    }

    @Test
    fun clearRemovesWhatItCounted() = runBlocking {
        val inventory = inventory()

        apkDir.mkdirs()
        File(apkDir, "latest.apk").writeBytes(ByteArray(40_000))
        strayCacheFile.writeBytes(ByteArray(10_000))
        assertTrue(inventory.totalBytes() > 0)

        inventory.clearAll()

        assertTrue("the downloaded APK survived a clear", !File(apkDir, "latest.apk").exists())
        assertTrue("a stray cache file survived a clear", !strayCacheFile.exists())
    }

    @OptIn(ExperimentalCoilApi::class)
    @Test
    fun clearEmptiesTheImageCacheThroughCoilRatherThanDeletingItsDirectory() = runBlocking {
        val inventory = inventory()
        val diskCache = requireNotNull(imageLoader?.diskCache)

        inventory.clearAll()

        assertEquals("image cache should report empty after a clear", 0L, diskCache.size)
        assertTrue(
            "Coil's directory must still exist, since deleting it out from under the open " +
                "journal is what corrupts the cache",
            diskCache.directory.toFile().exists()
        )
    }
}
