package com.anisync.android.di

import android.content.Context
import com.anisync.android.cache.Cache.cache
import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.cache.normalized.storeReceivedDate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

/**
 * Provides Apollo GraphQL client with two-tier normalized caching:
 * 1. Memory cache (fast, volatile) - 10MB
 * 2. SQLite cache (persistent, survives app restarts)
 *
 * The SQLite tier lives in the cache directory, so the system can reclaim it under storage
 * pressure, Auto Backup leaves it out, and the in-app clear button reaches it. It used to sit in
 * `databases/` where none of those were true and nothing ever evicted it.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApolloModule {

    private const val MEMORY_CACHE_SIZE = 10 * 1024 * 1024 // 10 MB
    private const val CACHE_DATABASE_NAME = "apollo_cache.db"

    /** SQLite keeps its journal and shared-memory files alongside the database. */
    private val SQLITE_SUFFIXES = listOf("", "-journal", "-wal", "-shm")

    /**
     * How long a cached field counts as fresh when the schema does not say otherwise. Reads past
     * this age miss and refetch, and [com.anisync.android.worker.CacheMaintenanceWorker] collects
     * them. Room, not this cache, is what the app reads offline.
     */
    private val DEFAULT_MAX_AGE = 1.days

    @Provides
    @Singleton
    fun provideApolloClient(
        @ApplicationContext context: Context,
        authorizationInterceptor: AuthorizationInterceptor
    ): ApolloClient {
        discardLegacyCache(context)

        // Two-tier cache: Memory (fast) -> SQLite (persistent)
        val cacheFactory = MemoryCacheFactory(maxSizeBytes = MEMORY_CACHE_SIZE)
            .chain(SqlNormalizedCacheFactory(context = context, name = CACHE_DATABASE_NAME))

        return ApolloClient.Builder()
            .serverUrl("https://graphql.anilist.co")
            .addHttpInterceptor(authorizationInterceptor)
            .cache(cacheFactory, defaultMaxAge = DEFAULT_MAX_AGE)
            // Stamps each field with when it arrived, which is what lets
            // [com.anisync.android.worker.CacheMaintenanceWorker] tell stale from fresh. Without it
            // there is nothing for garbage collection to act on.
            .storeReceivedDate(true)
            .build()
    }

    /**
     * Removes the pre-migration cache from `databases/`.
     *
     * Two reasons it cannot simply be left there. Its records are in the old JSON format, which the
     * current cache cannot read. And the factory resolves its path as "use `databases/` if a file
     * is already there, otherwise the cache directory", so leaving it behind would pin the cache to
     * the one location this migration exists to get out of.
     */
    private fun discardLegacyCache(context: Context) {
        val legacy = context.getDatabasePath(CACHE_DATABASE_NAME)
        SQLITE_SUFFIXES.forEach { suffix ->
            java.io.File(legacy.parentFile, legacy.name + suffix).delete()
        }
    }
}
