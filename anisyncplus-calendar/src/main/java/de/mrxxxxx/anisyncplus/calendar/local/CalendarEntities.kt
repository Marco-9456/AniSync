package de.mrxxxxx.anisyncplus.calendar.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "aniworld_snapshots")
data class AniWorldSnapshotEntity(
    @androidx.room.PrimaryKey val snapshotId: String,
    val fetchedAtEpochMillis: Long,
    val rangeStartEpochDay: Long,
    val rangeEndEpochDay: Long,
    val documentSha256: String,
    val parserVersion: String,
    val entryCount: Int,
    val germanVisibleEntryCount: Int,
    val sourceUrl: String
)

@Entity(
    tableName = "aniworld_release_entries",
    primaryKeys = ["localId", "snapshotId"],
    foreignKeys = [
        ForeignKey(
            entity = AniWorldSnapshotEntity::class,
            parentColumns = ["snapshotId"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("snapshotId"),
        Index("sourceSeriesKey"),
        Index(value = ["snapshotId", "sourceDateEpochDay"]),
        Index(value = ["snapshotId", "language"]),
        Index(value = ["snapshotId", "resolvedInstantEpochSeconds"])
    ]
)
data class AniWorldReleaseEntity(
    val localId: String,
    val snapshotId: String,
    val sourceSeriesKey: String,
    val sourceSlug: String?,
    val sourceUrl: String?,
    val rawTitle: String,
    val normalizedTitle: String,
    val sourceDateEpochDay: Long,
    val sourceLocalTimeMinutes: Int?,
    val sourceZoneId: String,
    val resolvedInstantEpochSeconds: Long?,
    val isApproximate: Boolean,
    val releaseKind: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val installmentNumber: Int?,
    val rawInstallmentToken: String?,
    val language: String,
    val rawLanguageMarkers: String,
    val parserVersion: String,
    val sourceOrder: Int,
    val diagnosticStatus: String
)

@Entity(
    tableName = "aniworld_media_mappings",
    indices = [Index("aniListMediaId"), Index("status")]
)
data class AniWorldMediaMappingEntity(
    @androidx.room.PrimaryKey val sourceSeriesKey: String,
    val aniListMediaId: Int?,
    val status: String,
    val confidence: Double?,
    val reason: String,
    val matcherVersion: String,
    val isManual: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val candidateCount: Int,
    val secondBestScore: Double?,
    val scoreMargin: Double?,
    val aniListTitle: String?,
    val coverImageUrl: String?,
    val averageScore: Int?
)

@Entity(tableName = "aniworld_sync_state")
data class AniWorldSyncStateEntity(
    @androidx.room.PrimaryKey val singletonId: Int = SINGLETON_ID,
    val lastAttemptAtEpochMillis: Long? = null,
    val lastSuccessAtEpochMillis: Long? = null,
    val lastErrorType: String? = null,
    val lastErrorMessage: String? = null,
    val httpStatus: Int? = null,
    val parsedCount: Int = 0,
    val visibleGermanCount: Int = 0,
    val matchedCount: Int = 0,
    val ambiguousCount: Int = 0,
    val unmatchedCount: Int = 0,
    val activeSnapshotId: String? = null,
    val rangeStartEpochDay: Long? = null,
    val rangeEndEpochDay: Long? = null,
    val parserVersion: String,
    val matcherVersion: String
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
