package com.anisync.android.domain

import com.anisync.android.domain.recap.Recap
import com.anisync.android.domain.recap.RecapMedia
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.RecapSettings
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId

/** A recap as the device keeps it, counted with the recap settings in force. */
data class StoredRecap(
    val period: RecapPeriod,
    val recap: Recap,
    /** The titles the period touched. */
    val media: Map<Int, RecapMedia>,
    /** False while the period is still running. */
    val isFinal: Boolean,
    /** Epoch milliseconds of the fetch it was counted from. */
    val builtAt: Long,
    /** Epoch milliseconds of the first time it was opened, null while it is new. */
    val seenAt: Long?,
)

/** What a Developer Tools build cost, next to the recap it built. */
data class RecapPreview(
    val recap: StoredRecap?,
    /** The month before a previewed month, counted from the same sweep, for the comparison. */
    val previous: StoredRecap? = null,
    /** Activities read, countable or not. */
    val activities: Int,
    val requests: Int,
    val millis: Long,
    /** Size of the payload a stored recap of the period would keep. */
    val payloadBytes: Int,
    /** Whose recap it is, for the preview story's header. */
    val userName: String? = null,
    val avatarUrl: String? = null,
)

/**
 * Monthly and yearly recaps of the active account's list activity.
 *
 * Room is the source of truth. Each stored period keeps the activities AniList logged for it, so a
 * recap is counted on the device, and counted again without the network when Include manga, Count
 * rewatches or adult content change. A month without a counted episode has no recap.
 *
 * Calls from a screen spend the request budget as interactive requests. The recap worker wraps its
 * calls in background priority, and a deferral there is waited out rather than restarting the build.
 */
interface RecapRepository {

    /** Every stored recap, newest first. Periods with nothing counted are left out. */
    fun observeRecaps(): Flow<List<StoredRecap>>

    /**
     * The recap for [period], from Room when it is stored and current, otherwise built from AniList
     * and stored. The month in progress is fetched again once it is six hours old, a running year is
     * added up from its months and never stored. Null when nothing in the period counts.
     */
    suspend fun getRecap(period: RecapPeriod, refresh: Boolean = false): Result<StoredRecap?>

    /**
     * The recap for [period] as the device already holds it, never fetched. A finished month can
     * still be cut out of its stored year. Null when nothing is stored or nothing counts.
     */
    suspend fun storedRecap(period: RecapPeriod): StoredRecap?

    /** [getRecap] for the month in progress, the one Overview and Recaps open on. */
    suspend fun monthInProgress(refresh: Boolean = false): Result<StoredRecap?>

    /**
     * The first build: one sweep over the current and the previous calendar year, stored as last
     * year's recap and every month retention keeps.
     */
    suspend fun backfill(): Result<Unit>

    /**
     * What the recap job does on the 1st: builds last month, and last year in January, unless they
     * are already final. Runs [backfill] first when nothing is stored yet. Returns the due recaps.
     */
    suspend fun buildDue(monthly: Boolean, yearly: Boolean): Result<List<StoredRecap>>

    suspend fun markSeen(period: RecapPeriod)

    /** True once anything is stored for the active account, even periods with nothing counted. */
    suspend fun hasStored(): Boolean

    /** How many bytes the active account's stored recaps take, periods with nothing counted included. */
    fun observeStoredBytes(): Flow<Long>

    /** Deletes every stored recap of the active account. */
    suspend fun clear()

    /** Deletes every stored recap of [ownerId], an account that was removed. */
    suspend fun clearOwner(ownerId: Int)

    /**
     * Developer Tools: builds [period] for any public user exactly as a first build would, counted
     * with [settings], without storing anything or touching the active account's recaps.
     */
    suspend fun previewRecap(userId: Int, period: RecapPeriod, zone: ZoneId, settings: RecapSettings): Result<RecapPreview>
}
