package com.anisync.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.anisync.android.data.proto.AuthToken
import com.anisync.android.data.serializer.AuthTokenSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing DataStore instances.
 * 
 * This module configures Proto DataStore with Google Tink encryption
 * for secure storage of authentication tokens.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Provides the encrypted AuthToken DataStore.
     * 
     * @param context Application context for file storage
     * @param serializer The AuthToken serializer with encryption
     * @return DataStore instance for AuthToken
     */
    @Provides
    @Singleton
    fun provideAuthTokenDataStore(
        @ApplicationContext context: Context,
        serializer: AuthTokenSerializer
    ): DataStore<AuthToken> {
        return DataStoreFactory.create(
            serializer = serializer,
            produceFile = { context.dataStoreFile("auth_token.pb") }
        )
    }
}
