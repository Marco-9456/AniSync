package com.anisync.android.data.local.entity

import androidx.room.Entity

/**
 * One account's recap of a month or a year.
 *
 * [payload] holds what AniList returned for the period, compact activities and title facts as JSON
 * (see [com.anisync.android.data.RecapPayload]), never the counted numbers. Recaps are counted from
 * it on read, so changing Include manga or Count rewatches needs no network.
 *
 * A row also exists for a fetched period that turned out to hold no recap, so the period is not
 * fetched again and later months can still see its activities.
 */
@Entity(tableName = "recaps", primaryKeys = ["ownerId", "periodKey"])
data class RecapEntity(
    val ownerId: Int,
    /** "2026-09" for a month, "2025" for a year. */
    val periodKey: String,
    /** [KIND_MONTH] or [KIND_YEAR]. */
    val kind: String,
    /** Epoch milliseconds of the fetch the payload came from. */
    val builtAt: Long,
    /** False while the period is still running, so the payload has to be fetched again later. */
    val isFinal: Boolean,
    /** The recap settings it was last counted with, see [com.anisync.android.domain.recap.RecapSettings.key]. */
    val settingsKey: String,
    /** Epoch milliseconds of the first time it was opened, null while it is new. */
    val seenAt: Long?,
    val payload: String,
) {
    companion object {
        const val KIND_MONTH = "MONTH"
        const val KIND_YEAR = "YEAR"
    }
}

/** A stored recap without its payload, for deciding what to keep. */
data class RecapStamp(
    val periodKey: String,
    val isFinal: Boolean,
    val builtAt: Long,
    val seenAt: Long?,
)
