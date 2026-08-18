package com.anisync.android.data

import android.content.Context
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.apolloStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one list of everything AniSync caches on disk, so the figure shown in Settings and the
 * bytes the clear button reclaims cannot drift apart.
 *
 * Two of these sit outside [Context.getCacheDir] and used to be invisible to both: the Apollo
 * normalized cache lives in `databases/`, and the downloaded update APK in the app's external
 * files directory.
 */
@Singleton
class CacheInventory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val apolloClient: ApolloClient
) {

    /** Every cache, measured. */
    suspend fun totalBytes(): Long = withContext(Dispatchers.IO) {
        imageBytes() + apolloBytes() + updateApkBytes() + otherBytes()
    }

    /**
     * Empties every cache counted by [totalBytes].
     *
     * The image cache goes through Coil's own API rather than a directory delete: Coil holds an
     * open journal and its own size accounting, and pulling the directory out from under it
     * corrupts both. Apollo likewise clears through its store so the open SQLite connection stays
     * consistent.
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        imageLoader.diskCache?.clear()
        imageLoader.memoryCache?.clear()
        runCatching { apolloClient.apolloStore.clearAll() }
        updateApkDirs().forEach { it.deleteRecursively() }
        otherCacheFiles().forEach { it.deleteRecursively() }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun imageBytes(): Long = imageLoader.diskCache?.size ?: 0L

    private fun apolloBytes(): Long = apolloCacheFiles().sumOf { it.length() }

    private fun updateApkBytes(): Long = updateApkDirs().sumOf { it.sizeRecursive() }

    private fun otherBytes(): Long = otherCacheFiles().sumOf { it.sizeRecursive() }

    /**
     * The normalized cache and its write-ahead companions. Named explicitly because the file moved
     * from `databases/` into the cache directory when the cache library changed, and both spellings
     * need to keep resolving.
     */
    private fun apolloCacheFiles(): List<File> =
        listOf(context.getDatabasePath(APOLLO_CACHE_DB), File(context.cacheDir, APOLLO_CACHE_DB))
            .flatMap { db -> APOLLO_SUFFIXES.map { File(db.parentFile, db.name + it) } }
            .filter { it.isFile }

    /**
     * Both homes of the downloaded update APK. It moved out of the external *files* directory,
     * where nothing ever deleted it and Auto Backup happily uploaded it, so the old path stays
     * listed to sweep up what earlier versions left behind.
     */
    private fun updateApkDirs(): List<File> = listOfNotNull(
        context.externalCacheDir?.resolve(APK_DIR),
        // resolve() rather than getExternalFilesDir(APK_DIR), which would recreate the directory
        // we are trying to be rid of.
        context.getExternalFilesDir(null)?.resolve(APK_DIR)
    ).filter { it.isDirectory }

    /** Everything else under the cache directories, minus what is already counted above. */
    private fun otherCacheFiles(): List<File> {
        val counted = alreadyCounted()
        return listOfNotNull(context.cacheDir, context.externalCacheDir)
            .flatMap { root -> root.listFiles().orEmpty().filterNot { it.name in counted } }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun alreadyCounted(): Set<String> =
        setOf(imageLoader.diskCache?.directory?.name ?: IMAGE_CACHE_DIR, APK_DIR) +
            APOLLO_SUFFIXES.map { APOLLO_CACHE_DB + it }

    private fun File?.sizeRecursive(): Long =
        this?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    private companion object {
        const val APOLLO_CACHE_DB = "apollo_cache.db"
        const val APK_DIR = "apk"
        const val IMAGE_CACHE_DIR = "image_cache"

        /** SQLite keeps its journal and shared-memory files alongside the database. */
        val APOLLO_SUFFIXES = listOf("", "-journal", "-wal", "-shm")
    }
}
