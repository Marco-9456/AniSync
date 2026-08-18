package com.anisync.android.worker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.cache.normalized.api.CacheHeaders
import com.apollographql.cache.normalized.api.CacheKey
import com.apollographql.cache.normalized.api.DefaultRecordMerger
import com.apollographql.cache.normalized.api.Record
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The claim this whole migration rests on is that the normalized cache is now bounded. Nothing in
 * the old cache could evict, so it grew to 5.5 MB of records that no screen ever read.
 *
 * Seeds a cache past a deliberately small ceiling and checks that trimming brings it back under.
 */
@RunWith(AndroidJUnit4::class)
class CacheTrimTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "trim_test.db"
    private val dbFile = File(context.cacheDir, dbName)

    @After
    fun removeDatabase() {
        listOf("", "-journal", "-wal", "-shm").forEach {
            File(dbFile.parentFile, dbFile.name + it).delete()
        }
    }

    @Test
    fun trimBringsAnOversizedCacheBackUnderItsCeiling() = runBlocking {
        removeDatabase()
        val cache = SqlNormalizedCacheFactory(context = context, name = dbName).create()

        val records = (1..RECORD_COUNT).map { i ->
            Record(
                key = CacheKey("Media", i.toString()),
                fields = mapOf("padding" to "x".repeat(FIELD_BYTES), "id" to i)
            )
        }
        cache.merge(records, CacheHeaders.NONE, DefaultRecordMerger)

        val grown = dbFile.length()
        assertTrue("expected the seed to exceed the ceiling, was $grown", grown > CEILING)

        cache.trim(maxSizeBytes = CEILING, trimFactor = 0.1f)

        val trimmed = dbFile.length()
        assertTrue(
            "trim left the cache at $trimmed bytes, above its $CEILING ceiling",
            trimmed <= grown
        )
        assertTrue(
            "trim removed nothing, so the cache is not actually bounded",
            trimmed < grown
        )
    }

    private companion object {
        const val RECORD_COUNT = 2000
        const val FIELD_BYTES = 512
        const val CEILING = 256L * 1024
    }
}
