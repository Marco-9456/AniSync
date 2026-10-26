package com.anisync.android.data

import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.data.mapper.mapFuzzyDateToLong
import com.anisync.android.data.mapper.toDomainStatus
import com.anisync.android.data.mapper.toScoreMap
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.fragment.ViewerListEntryFields

/** What one answer from AniList about the viewer's entry for a title means for its library row. */
internal sealed interface ListEntrySync {
    data object Keep : ListEntrySync

    data object Drop : ListEntrySync

    data class Take(val entry: ViewerListEntryFields) : ListEntrySync
}

/**
 * Decides what an answer requested at [askedAt] may do to [local], which was [localWhenAsked] when
 * the request went out.
 *
 * A row written or removed after [askedAt] stays as it is, since the answer can predate a change
 * made here while it was on its way. Otherwise AniList's `updatedAt` decides. It moves on every save
 * from any client and stays put for an edit that has not been sent yet, so a row that is only ahead
 * locally keeps it.
 */
internal fun planListEntrySync(
    localWhenAsked: LibraryEntryEntity?,
    local: LibraryEntryEntity?,
    server: ViewerListEntryFields?,
    askedAt: Long,
): ListEntrySync = when {
    local != null && local.lastUpdated >= askedAt -> ListEntrySync.Keep
    localWhenAsked != null && local == null -> ListEntrySync.Keep
    server == null -> if (local == null) ListEntrySync.Keep else ListEntrySync.Drop
    local == null || local.id != server.id -> ListEntrySync.Take(server)
    (server.updatedAtMillis() ?: 0L) > (local.updatedAt ?: 0L) -> ListEntrySync.Take(server)
    else -> ListEntrySync.Keep
}

/** [this] with every list field taken from [entry], read the way the library sync reads them. */
internal fun LibraryEntry.withListEntry(entry: ViewerListEntryFields): LibraryEntry = copy(
    id = entry.id,
    status = entry.status?.toDomainStatus() ?: LibraryStatus.UNKNOWN,
    progress = entry.progress ?: 0,
    progressVolumes = entry.progressVolumes,
    score = entry.score,
    advancedScores = entry.advancedScores.toScoreMap(),
    rewatches = entry.repeat ?: 0,
    priority = entry.priority ?: 0,
    notes = entry.notes,
    startedAt = entry.startedAt?.let { mapFuzzyDateToLong(it.year, it.month, it.day) },
    completedAt = entry.completedAt?.let { mapFuzzyDateToLong(it.year, it.month, it.day) },
    updatedAt = entry.updatedAtMillis(),
    createdAt = entry.createdAt?.toLong()?.times(1000L),
    customLists = entry.customLists.enabledListNames(),
    isPrivate = entry.`private` ?: false,
    hiddenFromStatusLists = entry.hiddenFromStatusLists ?: false,
)

private fun ViewerListEntryFields.updatedAtMillis(): Long? = updatedAt?.toLong()?.times(1000L)

/** AniList sends every custom list of the type with a flag saying whether the entry is on it. */
internal fun Any?.enabledListNames(): List<String> =
    (this as? Map<*, *>)?.mapNotNull { (name, on) -> (name as? String)?.takeIf { on == true } }.orEmpty()
