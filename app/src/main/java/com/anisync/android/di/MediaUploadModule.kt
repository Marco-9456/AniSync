package com.anisync.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Dedicated [OkHttpClient] for media uploads. Long timeouts because users may
 * pick large videos on slow networks; sharing the Apollo client would risk
 * stalling GraphQL traffic behind a multi-minute upload.
 */
@Module
@InstallIn(SingletonComponent::class)
object MediaUploadModule {

    @Provides
    @Singleton
    fun provideMediaUploadOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .callTimeout(10, TimeUnit.MINUTES)
            // User-driven retry covers transient failures with feedback; auto-retry
            // would silently re-upload a 50 MB video on a transient EOF.
            .retryOnConnectionFailure(false)
            .build()
    }
}
