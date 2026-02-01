package com.anisync.android.di

import android.content.Context
import androidx.room.Room
import com.anisync.android.data.local.AppDatabase
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "anisync.db"
        )
            // During development, destroy and recreate database on schema changes
            // TODO: Replace with proper migrations before production release
            .addMigrations(AppDatabase.MIGRATION_12_13)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideLibraryDao(database: AppDatabase): LibraryDao {
        return database.libraryDao()
    }

    @Provides
    fun provideMediaDetailsDao(database: AppDatabase): MediaDetailsDao {
        return database.mediaDetailsDao()
    }

    @Provides
    fun provideUserProfileDao(database: AppDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    fun provideAiringScheduleDao(database: AppDatabase): com.anisync.android.data.local.dao.AiringScheduleDao {
        return database.airingScheduleDao()
    }

    @Provides
    fun provideTrendingDao(database: AppDatabase): com.anisync.android.data.local.dao.TrendingDao {
        return database.trendingDao()
    }
}
