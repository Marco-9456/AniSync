package com.anisync.android.di

import com.anisync.android.data.network.RateLimitGate
import com.anisync.android.data.network.RateLimitMonitor
import com.anisync.android.data.network.RateLimitPersistence
import com.anisync.android.data.network.RetryPolicy
import com.anisync.android.data.network.SharedPreferencesRateLimitPersistence
import com.anisync.android.data.util.Clock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Collaborators for the AniList rate limiter, kept apart from the Apollo client wiring. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindRateLimitPersistence(
        impl: SharedPreferencesRateLimitPersistence,
    ): RateLimitPersistence

    companion object {
        /** Monotonic, so the gate is unaffected by the user changing the device clock. */
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.System

        @Provides
        @Singleton
        fun provideRetryPolicy(): RetryPolicy = RetryPolicy()

        @Provides
        @Singleton
        fun provideRateLimitGate(
            clock: Clock,
            monitor: RateLimitMonitor,
            persistence: RateLimitPersistence,
        ): RateLimitGate = RateLimitGate(clock, monitor, persistence)
    }
}
