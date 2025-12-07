package com.anisync.android.di

import com.anisync.android.data.DiscoverRepositoryImpl
import com.anisync.android.data.LibraryRepositoryImpl
import com.anisync.android.data.ProfileRepositoryImpl
import com.anisync.android.data.DetailsRepositoryImpl
import com.anisync.android.data.SearchRepositoryImpl
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.SearchRepository
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
        impl: com.anisync.android.data.NotificationRepositoryImpl
    ): com.anisync.android.domain.NotificationRepository
}
