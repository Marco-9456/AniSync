package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anisync.android.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user profile operations.
 */
@Dao
interface UserProfileDao {
    
    /**
     * Observe a specific account's cached profile (the row's primary key is the user id, so this
     * scopes the own-profile cache per account). Emits null if not cached.
     */
    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    fun observe(userId: Int): Flow<UserProfileEntity?>

    /**
     * Get a specific account's cached profile (one-time read).
     */
    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    suspend fun get(userId: Int): UserProfileEntity?

    /**
     * Insert or replace user profile.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    /**
     * Clear the cached profile.
     */
    @Query("DELETE FROM user_profile")
    suspend fun clear()
}
