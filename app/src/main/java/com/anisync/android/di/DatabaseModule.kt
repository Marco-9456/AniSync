package com.anisync.android.di

import android.content.Context
import androidx.room.Room
import com.anisync.android.data.local.AppDatabase
import com.anisync.android.data.local.Migrations
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.data.local.dao.SavedForumThreadDao
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
            .addMigrations(*Migrations.ALL_MIGRATIONS)
            // ┌─────────────────────────────────────────────────────────────────┐
            // │  ⚠️  DEVELOPMENT ONLY - REMOVE BEFORE PRODUCTION RELEASE  ⚠️   │
            // ├─────────────────────────────────────────────────────────────────┤
            // │  This allows destructive recreation when migrations are missing │
            // │                                                                 │
            // │  Before publishing to Play Store:                               │
            // │  1. Remove the .fallbackToDestructiveMigration() call below     │
            // │  2. Ensure all migrations are defined in Migrations.kt          │
            // │  3. Test upgrade paths from version 1 to current                │
            // │  4. Run MigrationTest.kt to verify all migrations               │
            // └─────────────────────────────────────────────────────────────────┘
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

    @Provides
    fun provideSavedForumThreadDao(database: AppDatabase): SavedForumThreadDao {
        return database.savedForumThreadDao()
    }
}
