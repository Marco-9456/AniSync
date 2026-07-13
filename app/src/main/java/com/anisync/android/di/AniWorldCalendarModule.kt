package com.anisync.android.di

import android.content.Context
import com.anisync.android.data.anisyncplus.AniWorldAniListCandidateProvider
import com.anisync.android.data.anisyncplus.AniWorldLibraryStateProvider
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.mrxxxxx.anisyncplus.calendar.api.AniListLibraryStateProvider
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarClient
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarParser
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarRepository
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldMatchCandidateProvider
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldRefreshCoordinator
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldTitleMatcher
import de.mrxxxxx.anisyncplus.calendar.api.EffectiveReleaseRepository
import de.mrxxxxx.anisyncplus.calendar.local.AniWorldCalendarDao
import de.mrxxxxx.anisyncplus.calendar.local.AniWorldCalendarDatabase
import de.mrxxxxx.anisyncplus.calendar.matching.DefaultAniWorldTitleMatcher
import de.mrxxxxx.anisyncplus.calendar.network.OkHttpAniWorldCalendarClient
import de.mrxxxxx.anisyncplus.calendar.parser.JsoupAniWorldCalendarParser
import de.mrxxxxx.anisyncplus.calendar.repository.RoomAniWorldCalendarRepository
import okhttp3.OkHttpClient
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AniWorldCalendarModule {
    @Provides
    @Singleton
    fun provideCalendarDatabase(@ApplicationContext context: Context): AniWorldCalendarDatabase =
        Room.databaseBuilder(
            context,
            AniWorldCalendarDatabase::class.java,
            AniWorldCalendarDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideCalendarDao(database: AniWorldCalendarDatabase): AniWorldCalendarDao =
        database.calendarDao()

    @Provides
    @Singleton
    fun provideCalendarClient(): AniWorldCalendarClient = OkHttpAniWorldCalendarClient(
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    )

    @Provides
    fun provideCalendarParser(): AniWorldCalendarParser = JsoupAniWorldCalendarParser()

    @Provides
    fun provideTitleMatcher(): AniWorldTitleMatcher = DefaultAniWorldTitleMatcher()

    @Provides
    fun provideCandidateProvider(impl: AniWorldAniListCandidateProvider): AniWorldMatchCandidateProvider = impl

    @Provides
    fun provideLibraryStateProvider(impl: AniWorldLibraryStateProvider): AniListLibraryStateProvider = impl

    @Provides
    @Singleton
    fun provideRoomCalendarRepository(
        dao: AniWorldCalendarDao,
        client: AniWorldCalendarClient,
        parser: AniWorldCalendarParser,
        matcher: AniWorldTitleMatcher,
        candidateProvider: AniWorldMatchCandidateProvider,
        libraryStateProvider: AniListLibraryStateProvider
    ): RoomAniWorldCalendarRepository = RoomAniWorldCalendarRepository(
        dao = dao,
        client = client,
        parser = parser,
        matcher = matcher,
        candidateProvider = candidateProvider,
        libraryStateProvider = libraryStateProvider,
        clock = Clock.systemUTC()
    )

    @Provides
    fun provideCalendarRepository(impl: RoomAniWorldCalendarRepository): AniWorldCalendarRepository = impl

    @Provides
    fun provideRefreshCoordinator(impl: RoomAniWorldCalendarRepository): AniWorldRefreshCoordinator = impl

    @Provides
    fun provideEffectiveReleaseRepository(impl: RoomAniWorldCalendarRepository): EffectiveReleaseRepository = impl
}
