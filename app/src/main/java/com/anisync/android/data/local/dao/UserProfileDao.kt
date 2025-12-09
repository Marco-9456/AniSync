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
     * Observe the cached user profile.
     * Emits null if not cached.
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observe(): Flow<UserProfileEntity?>

    /**
     * Get the cached user profile (one-time read).
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun get(): UserProfileEntity?

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
