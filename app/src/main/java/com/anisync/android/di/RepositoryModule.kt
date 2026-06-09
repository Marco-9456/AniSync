package com.anisync.android.di

import com.anisync.android.data.ActivityRepositoryImpl
import com.anisync.android.data.CalendarRepositoryImpl
import com.anisync.android.data.DetailsRepositoryImpl
import com.anisync.android.data.DiscoverRepositoryImpl
import com.anisync.android.data.FeedRepositoryImpl
import com.anisync.android.data.ForumRepositoryImpl
import com.anisync.android.data.LibraryRepositoryImpl
import com.anisync.android.data.LinkPreviewProviderImpl
import com.anisync.android.data.NotificationRepositoryImpl
import com.anisync.android.data.ProfileRepositoryImpl
import com.anisync.android.data.SearchRepositoryImpl
import com.anisync.android.data.StatisticsRepositoryImpl
import com.anisync.android.data.UserOptionsRepositoryImpl
import com.anisync.android.data.repository.PreferencesRepositoryImpl
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.CalendarRepository
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.FeedRepository
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LinkPreviewProvider
import com.anisync.android.domain.NotificationRepository
import com.anisync.android.domain.PreferencesRepository
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.SearchRepository
import com.anisync.android.domain.StatisticsRepository
import com.anisync.android.domain.UserOptionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindLibraryRepository(
        impl: LibraryRepositoryImpl
    ): LibraryRepository

    @Binds
    abstract fun bindDiscoverRepository(
        impl: DiscoverRepositoryImpl
    ): DiscoverRepository

    @Binds
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    abstract fun bindDetailsRepository(
        impl: DetailsRepositoryImpl
    ): DetailsRepository

    @Binds
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository

    @Binds
    abstract fun bindStatisticsRepository(
        impl: StatisticsRepositoryImpl
    ): StatisticsRepository

    @Binds
    abstract fun bindForumRepository(
        impl: ForumRepositoryImpl
    ): ForumRepository

    @Binds
    abstract fun bindLinkPreviewProvider(
        impl: LinkPreviewProviderImpl
    ): LinkPreviewProvider

    @Binds
    abstract fun bindActivityRepository(
        impl: ActivityRepositoryImpl
    ): ActivityRepository

    @Binds
    abstract fun bindFeedRepository(
        impl: FeedRepositoryImpl
    ): FeedRepository

    @Binds
    abstract fun bindCalendarRepository(
        impl: CalendarRepositoryImpl
    ): CalendarRepository

    @Binds
    abstract fun bindUserOptionsRepository(
        impl: UserOptionsRepositoryImpl
    ): UserOptionsRepository
}
