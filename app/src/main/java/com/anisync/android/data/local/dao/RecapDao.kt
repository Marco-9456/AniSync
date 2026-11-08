package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.anisync.android.data.local.entity.RecapEntity
import com.anisync.android.data.local.entity.RecapSeen
import com.anisync.android.data.local.entity.RecapStamp
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecapDao {
    @Query("SELECT * FROM recaps WHERE ownerId = :ownerId")
    abstract fun observeAll(ownerId: Int): Flow<List<RecapEntity>>

    @Query("SELECT * FROM recaps WHERE ownerId = :ownerId")
    abstract suspend fun getAll(ownerId: Int): List<RecapEntity>

    @Query("SELECT * FROM recaps WHERE ownerId = :ownerId AND periodKey = :periodKey")
    abstract suspend fun get(ownerId: Int, periodKey: String): RecapEntity?

    @Query("SELECT periodKey, isFinal, builtAt, seenAt FROM recaps WHERE ownerId = :ownerId")
    abstract suspend fun getStamps(ownerId: Int): List<RecapStamp>

    @Query("SELECT periodKey, seenAt FROM recaps WHERE ownerId = :ownerId AND periodKey IN (:periodKeys)")
    abstract suspend fun getSeen(ownerId: Int, periodKeys: List<String>): List<RecapSeen>

    @Query("SELECT COALESCE(SUM(LENGTH(CAST(payload AS BLOB))), 0) FROM recaps WHERE ownerId = :ownerId")
    abstract fun observePayloadBytes(ownerId: Int): Flow<Long>

    @Upsert
    abstract suspend fun upsertAll(rows: List<RecapEntity>)

    /**
     * Stores [rows] without undoing an open that landed while they were being built: a row that
     * is seen in the table stays seen, whatever the build read before it started.
     */
    @Transaction
    open suspend fun upsertKeepingSeen(rows: List<RecapEntity>): List<RecapEntity> {
        val owner = rows.firstOrNull()?.ownerId ?: return rows
        val seen = getSeen(owner, rows.map { it.periodKey }).associate { it.periodKey to it.seenAt }
        val merged = rows.map { row -> seen[row.periodKey]?.let { row.copy(seenAt = it) } ?: row }
        upsertAll(merged)
        return merged
    }

    @Query("UPDATE recaps SET seenAt = :seenAt WHERE ownerId = :ownerId AND periodKey = :periodKey AND seenAt IS NULL")
    abstract suspend fun markSeen(ownerId: Int, periodKey: String, seenAt: Long)

    @Query("UPDATE recaps SET settingsKey = :settingsKey WHERE ownerId = :ownerId AND periodKey IN (:periodKeys)")
    abstract suspend fun updateSettingsKey(ownerId: Int, periodKeys: List<String>, settingsKey: String)

    @Query("DELETE FROM recaps WHERE ownerId = :ownerId AND periodKey IN (:periodKeys)")
    abstract suspend fun delete(ownerId: Int, periodKeys: List<String>)

    @Query("DELETE FROM recaps WHERE ownerId = :ownerId")
    abstract suspend fun deleteOwner(ownerId: Int)
}
