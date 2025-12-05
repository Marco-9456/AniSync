package com.anisync.android.di

import com.anisync.android.data.DiscoverRepositoryImpl
import com.anisync.android.data.LibraryRepositoryImpl
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryRepository
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
}
