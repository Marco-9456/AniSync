package de.mrxxxxx.anisyncplus.calendar.local

import androidx.room.Embedded
import androidx.room.Relation

data class AniWorldSnapshotWithReleases(
    @Embedded val snapshot: AniWorldSnapshotEntity,
    @Relation(
        parentColumn = "snapshotId",
        entityColumn = "snapshotId"
    )
    val releases: List<AniWorldReleaseEntity>
)
