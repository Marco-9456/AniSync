package com.anisync.android.di

import android.os.Build
import com.anisync.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Dedicated [OkHttpClient] for media uploads. Long timeouts because users may
 * pick large videos on slow networks; sharing the Apollo client would risk
 * stalling GraphQL traffic behind a multi-minute upload.
 *
 * Pinned to HTTP/1.1: catbox.moe's HTTP/2 edge intermittently RSTs streams
 * mid-upload (surfaces to the user as `stream was reset: PROTOCOL_ERROR`).
 * Identifying User-Agent: catbox filters bulk/automated clients by UA — the
 * default `okhttp/x.y.z` UA trips their bot filter and returns 412 "Invalid
 * uploader" on some files.
 */
@Module
@InstallIn(SingletonComponent::class)
object MediaUploadModule {

    @Provides
    @Singleton
    fun provideMediaUploadOkHttpClient(): OkHttpClient {
        val userAgent = "AniSync/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.SDK_INT})"
        return OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val req = chain.request()
                val withUa = if (req.header("User-Agent") == null) {
                    req.newBuilder().header("User-Agent", userAgent).build()
                } else req
                chain.proceed(withUa)
            }
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
