package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.anisync.android.data.local.entity.RecapEntity
import com.anisync.android.data.local.entity.RecapStamp
import kotlinx.coroutines.flow.Flow

@Dao
interface RecapDao {
    @Query("SELECT * FROM recaps WHERE ownerId = :ownerId")
    fun observeAll(ownerId: Int): Flow<List<RecapEntity>>

    @Query("SELECT * FROM recaps WHERE ownerId = :ownerId")
    suspend fun getAll(ownerId: Int): List<RecapEntity>

    @Query("SELECT * FROM recaps WHERE ownerId = :ownerId AND periodKey = :periodKey")
    suspend fun get(ownerId: Int, periodKey: String): RecapEntity?

    @Query("SELECT periodKey, isFinal, builtAt, seenAt FROM recaps WHERE ownerId = :ownerId")
    suspend fun getStamps(ownerId: Int): List<RecapStamp>

    @Query("SELECT COALESCE(SUM(LENGTH(CAST(payload AS BLOB))), 0) FROM recaps WHERE ownerId = :ownerId")
    fun observePayloadBytes(ownerId: Int): Flow<Long>

    @Upsert
    suspend fun upsertAll(rows: List<RecapEntity>)

    @Query("UPDATE recaps SET seenAt = :seenAt WHERE ownerId = :ownerId AND periodKey = :periodKey AND seenAt IS NULL")
    suspend fun markSeen(ownerId: Int, periodKey: String, seenAt: Long)

    @Query("UPDATE recaps SET settingsKey = :settingsKey WHERE ownerId = :ownerId AND periodKey IN (:periodKeys)")
    suspend fun updateSettingsKey(ownerId: Int, periodKeys: List<String>, settingsKey: String)

    @Query("DELETE FROM recaps WHERE ownerId = :ownerId AND periodKey IN (:periodKeys)")
    suspend fun delete(ownerId: Int, periodKeys: List<String>)

    @Query("DELETE FROM recaps WHERE ownerId = :ownerId")
    suspend fun deleteOwner(ownerId: Int)
}
