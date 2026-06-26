package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.Flow

/**
 * DAO for library entry operations with reactive Flow support.
 *
 * All reads/writes are scoped to an `ownerId` (the AniList user id) so multiple accounts'
 * libraries coexist in one table — switching accounts shows the right account's entries instantly
 * from cache instead of wiping and refetching.
 */
@Dao
interface LibraryDao {

    /**
     * Observe an account's library entries by media type.
     * Emits a new list whenever that account's data changes.
     */
    @Query("SELECT * FROM library_entries WHERE ownerId = :ownerId AND mediaType = :type ORDER BY titleUserPreferred ASC")
    fun observeByType(ownerId: Int, type: MediaType): Flow<List<LibraryEntryEntity>>

    /**
     * Get an account's entries by type (non-reactive, for one-time reads).
     */
    @Query("SELECT * FROM library_entries WHERE ownerId = :ownerId AND mediaType = :type")
    suspend fun getByType(ownerId: Int, type: MediaType): List<LibraryEntryEntity>

    /**
     * Get "Up Next" entries for an account: Watching status and not completed.
     * Sorted by last updated to show most recently watched first.
     */
    @Query("SELECT * FROM library_entries WHERE ownerId = :ownerId AND status = 'CURRENT' AND (totalEpisodes IS NULL OR progress < totalEpisodes) ORDER BY lastUpdated DESC")
    suspend fun getUpNext(ownerId: Int): List<LibraryEntryEntity>

    @Query("SELECT * FROM library_entries WHERE ownerId = :ownerId AND status = 'CURRENT' AND (totalEpisodes IS NULL OR progress < totalEpisodes) ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getMostRecentWatching(ownerId: Int): LibraryEntryEntity?

    /**
     * Get a single entry by mediaId for a specific account.
     */
    @Query("SELECT * FROM library_entries WHERE ownerId = :ownerId AND mediaId = :mediaId LIMIT 1")
    suspend fun getEntry(ownerId: Int, mediaId: Int): LibraryEntryEntity?

    /**
     * Observe a single entry by mediaId for a specific account. Emits whenever that row changes —
     * used by the details screen to reflect the viewer's note straight from the library (the
     * source of truth) instead of the separately-cached media_details row, which can lag.
     */
    @Query("SELECT * FROM library_entries WHERE ownerId = :ownerId AND mediaId = :mediaId LIMIT 1")
    fun observeEntry(ownerId: Int, mediaId: Int): Flow<LibraryEntryEntity?>

    /**
     * Insert or replace entries. The entities carry their own [LibraryEntryEntity.ownerId].
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LibraryEntryEntity>)

    /**
     * Insert or replace a single entry (entity carries its ownerId).
     * Used when adding new media to library from details screen.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entry: LibraryEntryEntity)

    /**
     * Delete all entries of a media type for an account.
     */
    @Query("DELETE FROM library_entries WHERE ownerId = :ownerId AND mediaType = :type")
    suspend fun deleteByType(ownerId: Int, type: MediaType)

    /**
     * Update progress for a specific media in an account.
     */
    @Query("UPDATE library_entries SET progress = :progress, lastUpdated = :timestamp WHERE ownerId = :ownerId AND mediaId = :mediaId")
    suspend fun updateProgress(ownerId: Int, mediaId: Int, progress: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Atomic transaction: delete an account's old entries of a type and insert new ones.
     * @deprecated Use smartMergeByType instead to preserve locally-added entries
     */
    @Transaction
    suspend fun replaceByType(ownerId: Int, type: MediaType, entries: List<LibraryEntryEntity>) {
        deleteByType(ownerId, type)
        insertAll(entries)
    }

    /**
     * Smart merge: preserves locally-added entries while syncing with API, scoped to one account.
     */
    @Transaction
    suspend fun smartMergeByType(ownerId: Int, type: MediaType, apiEntries: List<LibraryEntryEntity>) {
        val localEntries = getByType(ownerId, type)
        // The API list holds one canonical entry id per media (it's deduped by media upstream).
        val apiIdByMedia = HashMap<Int, Int>(apiEntries.size)
        for (e in apiEntries) apiIdByMedia[e.mediaId] = e.id
        val now = System.currentTimeMillis()
        val recentThreshold = 5 * 60 * 1000L

        val toPreserve = ArrayList<LibraryEntryEntity>()
        val toDeleteMediaIds = ArrayList<Int>()
        val staleDuplicateIds = ArrayList<Int>()
        for (local in localEntries) {
            val canonicalId = apiIdByMedia[local.mediaId]
            if (canonicalId != null) {
                // Media is on the server → the API row wins. A local row for the same media with a
                // different entry id is a stale duplicate (e.g. an id=0 optimistic-add row that never
                // got swapped for the real id) — drop it so the media isn't cached/rendered twice.
                if (local.id != canonicalId) staleDuplicateIds.add(local.id)
                continue
            }
            // Media not on the server: keep a just-added entry through the sync delay, else it's stale.
            val created = local.createdAt
            if (created != null && (now - created) <= recentThreshold) {
                toPreserve.add(local)
            } else {
                toDeleteMediaIds.add(local.mediaId)
            }
        }

        if (staleDuplicateIds.isNotEmpty()) deleteByIds(ownerId, staleDuplicateIds)
        if (toDeleteMediaIds.isNotEmpty()) deleteByMediaIds(ownerId, toDeleteMediaIds)
        insertAll(apiEntries)
        if (toPreserve.isNotEmpty()) {
            insertAll(toPreserve)
        }
    }

    @Query("DELETE FROM library_entries WHERE ownerId = :ownerId AND mediaId IN (:mediaIds)")
    suspend fun deleteByMediaIds(ownerId: Int, mediaIds: List<Int>)

    /** Delete specific entries by their MediaList (entry) id. Used to drop stale duplicate rows. */
    @Query("DELETE FROM library_entries WHERE ownerId = :ownerId AND id IN (:ids)")
    suspend fun deleteByIds(ownerId: Int, ids: List<Int>)

    /**
     * Update status and progress for a specific media entry in an account.
     */
    @Query("UPDATE library_entries SET status = :status, progress = :progress, lastUpdated = :timestamp WHERE ownerId = :ownerId AND mediaId = :mediaId")
    suspend fun updateStatusAndProgress(ownerId: Int, mediaId: Int, status: LibraryStatus, progress: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Delete a specific entry by mediaId for an account.
     */
    @Query("DELETE FROM library_entries WHERE ownerId = :ownerId AND mediaId = :mediaId")
    suspend fun deleteByMediaId(ownerId: Int, mediaId: Int)

    /**
     * Update an entire entry (matched by primary key; the entity carries its ownerId).
     */
    @androidx.room.Update
    suspend fun updateEntry(entry: LibraryEntryEntity)

    /**
     * Update status, progress, and completedAt when media is completed, in an account.
     */
    @Query("UPDATE library_entries SET status = :status, progress = :progress, completedAt = :completedAt, lastUpdated = :timestamp WHERE ownerId = :ownerId AND mediaId = :mediaId")
    suspend fun updateStatusProgressAndCompletedAt(
        ownerId: Int,
        mediaId: Int,
        status: LibraryStatus,
        progress: Int,
        completedAt: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Update status and startedAt when starting to watch/read, in an account.
     */
    @Query("UPDATE library_entries SET status = :status, startedAt = :startedAt, lastUpdated = :timestamp WHERE ownerId = :ownerId AND mediaId = :mediaId")
    suspend fun updateStatusAndStartedAt(
        ownerId: Int,
        mediaId: Int,
        status: LibraryStatus,
        startedAt: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Re-tag entries from one owner to another. Used to promote the migrated legacy library
     * (owner 0) to the account's real id once it is resolved.
     */
    @Query("UPDATE library_entries SET ownerId = :newOwnerId WHERE ownerId = :oldOwnerId")
    suspend fun reassignOwner(oldOwnerId: Int, newOwnerId: Int)

    /**
     * Delete every library entry across all accounts (e.g. encrypted-store reset recovery).
     */
    @Query("DELETE FROM library_entries")
    suspend fun deleteAll()
}
