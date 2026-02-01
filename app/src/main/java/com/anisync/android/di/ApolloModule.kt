package com.anisync.android.di

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides Apollo GraphQL client with two-tier normalized caching:
 * 1. Memory cache (fast, volatile) - 10MB
 * 2. SQLite cache (persistent, survives app restarts) - Unlimited
 * 
 * Benefits:
 * - Instant cache hits for frequently accessed data
 * - Offline support for previously fetched data
 * - Automatic cache invalidation on mutations
 * - Reduced network calls and improved performance
 */
@Module
@InstallIn(SingletonComponent::class)
object ApolloModule {

    private const val MEMORY_CACHE_SIZE = 10 * 1024 * 1024 // 10 MB
    private const val CACHE_DATABASE_NAME = "apollo_cache.db"

    @Provides
    @Singleton
    fun provideApolloClient(
        @ApplicationContext context: Context,
        authorizationInterceptor: AuthorizationInterceptor
    ): ApolloClient {
        // Two-tier cache: Memory (fast) -> SQLite (persistent)
        // Memory cache is checked first, then SQLite on miss
        val memoryCacheFactory = MemoryCacheFactory(
            maxSizeBytes = MEMORY_CACHE_SIZE
        )
        
        val sqlCacheFactory = SqlNormalizedCacheFactory(
            context = context,
            name = CACHE_DATABASE_NAME
        )
        
        // Chain: Memory first, then SQLite for persistence
        val cacheFactory = memoryCacheFactory.chain(sqlCacheFactory)
        
        return ApolloClient.Builder()
            .serverUrl("https://graphql.anilist.co")
            .addHttpInterceptor(authorizationInterceptor)
            .normalizedCache(cacheFactory)
            .build()
    }
}
