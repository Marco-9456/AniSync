package com.anisync.android.data.media

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Theme songs on disk, so a recap's song loops and plays again from the file instead of streaming
 * the whole file each time. Media3 allows one cache per folder in a process, hence the singleton.
 *
 * The files and their index both live in the cache directory, where the system can reclaim them
 * and Auto Backup skips them. Media3's default index is a database in `databases/`, which is why
 * this one brings its own.
 */
@OptIn(UnstableApi::class)
@Singleton
class ThemeAudioCache @Inject constructor(@ApplicationContext private val context: Context) {

    val cache: Cache by lazy {
        val index = IndexDatabase(context, File(context.cacheDir, INDEX_DB))
        SimpleCache(File(context.cacheDir, DIR), LeastRecentlyUsedCacheEvictor(MAX_BYTES), index)
    }

    /** Bytes on disk. Blocks until the index is read, so call it off the main thread. */
    fun bytes(): Long = cache.cacheSpace

    /** Deletes every song through the cache's own index, which a plain folder delete would leave stale. */
    fun clear() {
        cache.keys.forEach(cache::removeResource)
    }

    companion object {
        /** The folder the songs are kept in. */
        const val DIR = "theme_audio"

        /** The index, a database beside the folder, since the cache deletes any file it does not know inside it. */
        const val INDEX_DB = "theme_audio_index.db"

        /** Room for about fifteen songs, a year of recaps. */
        private const val MAX_BYTES = 32L * 1024 * 1024
    }
}

@OptIn(UnstableApi::class)
private class IndexDatabase(context: Context, file: File) :
    SQLiteOpenHelper(context.applicationContext, file.path, null, 1), DatabaseProvider {
    override fun onCreate(db: SQLiteDatabase) = Unit
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}
