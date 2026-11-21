package com.anisync.android.domain.recap

enum class RecapMediaType { ANIME, MANGA }

/** One list activity as AniList logged it, before anything is counted. */
data class ListActivityRecord(
    val id: Int,
    val mediaId: Int,
    val type: RecapMediaType,
    val status: String?,
    val progress: String?,
    /** Epoch seconds. */
    val createdAt: Long,
)

/** The episodes or chapters one activity adds once completions are resolved. */
data class CountedEvent(
    val activityId: Int,
    val mediaId: Int,
    val type: RecapMediaType,
    val createdAt: Long,
    val units: Int,
    /** The first and last unit this event covers, when they are known. */
    val range: IntRange?,
    val isRepeat: Boolean,
    val isCompletion: Boolean,
)

private enum class StatusKind { PROGRESS, REPEAT_PROGRESS, COMPLETED, REPEAT_COMPLETED, OTHER }

private fun statusKind(status: String?): StatusKind {
    val value = status?.trim()?.lowercase() ?: return StatusKind.OTHER
    return when {
        value.startsWith("rewatched episode") || value.startsWith("reread chapter") -> StatusKind.REPEAT_PROGRESS
        value.startsWith("watched episode") || value.startsWith("read chapter") -> StatusKind.PROGRESS
        value == "completed" -> StatusKind.COMPLETED
        value == "rewatched" || value == "reread" -> StatusKind.REPEAT_COMPLETED
        else -> StatusKind.OTHER
    }
}

/** True for an activity that can add episodes or chapters. Plans, pauses and drops never do. */
fun isCountable(status: String?): Boolean = statusKind(status) != StatusKind.OTHER

/**
 * True for an activity that moves where a title's progress stands: a readable range, or a
 * completion. The last one before a period is all a completion inside it needs to see.
 */
internal fun ListActivityRecord.movesProgress(): Boolean = when (statusKind(status)) {
    StatusKind.PROGRESS, StatusKind.REPEAT_PROGRESS -> parseProgress(progress) != null
    StatusKind.COMPLETED, StatusKind.REPEAT_COMPLETED -> true
    StatusKind.OTHER -> false
}

/**
 * The range a progress string covers. AniList logs one update as "5" and merges consecutive ones
 * into "1 - 3". Anything else is unreadable and returns null.
 */
fun parseProgress(progress: String?): IntRange? {
    val parts = progress?.split('-')?.map { it.trim() } ?: return null
    if (parts.isEmpty() || parts.size > 2) return null
    val first = parts.first().toIntOrNull() ?: return null
    val last = parts.last().toIntOrNull() ?: return null
    if (first < 0 || last < first) return null
    return first..last
}

/**
 * Turns activities into counted events, oldest first.
 *
 * Each episode or chapter of a title counts once. A range only adds the units past the furthest one
 * counted before it, because AniList can keep an update after merging it into a longer one, and
 * logs a save sent twice as two activities. The second of those is skipped outright.
 *
 * AniList logs the last episode of a series as a completion with no progress, so a completion adds
 * the units after the furthest one counted, or one when nothing was counted before it (the final
 * episode, or a one-episode title). Without a known count that one has no range, so it is neither
 * a start nor an episode a theme song can be matched to. [totalUnits] is the title's episode or
 * chapter count.
 * Pass every activity known before the period too, so the period can see where each title stood.
 */
fun countEvents(
    records: List<ListActivityRecord>,
    totalUnits: (mediaId: Int) -> Int?,
): List<CountedEvent> {
    val titles = HashMap<Int, TitleCount>()
    return records.sortedWith(compareBy({ it.createdAt }, { it.id })).mapNotNull { record ->
        titles.getOrPut(record.mediaId, ::TitleCount).count(record, totalUnits(record.mediaId)?.takeIf { it > 0 })
    }
}

/** Where the count of one title stands. */
private class TitleCount {
    private var reach: Int? = null
    private var rewatchReach: Int? = null
    private var previous: ListActivityRecord? = null

    /** What [record] adds, or null when it is no update that counts at all. */
    fun count(record: ListActivityRecord, total: Int?): CountedEvent? {
        val kind = statusKind(record.status)
        if (kind == StatusKind.OTHER) return null
        val sentTwice = previous?.let {
            it.createdAt == record.createdAt && it.status == record.status && it.progress == record.progress
        } == true
        previous = record
        if (sentTwice) return null
        return when (kind) {
            StatusKind.PROGRESS -> watch(record, rewatch = false)
            StatusKind.REPEAT_PROGRESS -> watch(record, rewatch = true)
            StatusKind.COMPLETED -> finish(record, total, rewatch = false)
            StatusKind.REPEAT_COMPLETED -> finish(record, total, rewatch = true)
            StatusKind.OTHER -> null
        }
    }

    private fun watch(record: ListActivityRecord, rewatch: Boolean): CountedEvent {
        val range = parseProgress(record.progress)
            ?: return record.toEvent(units = 1, range = null, isRepeat = rewatch, isCompletion = false)
        val counted = if (rewatch) rewatchReach else reach
        if (counted == null || range.last > counted) {
            if (rewatch) rewatchReach = range.last else reach = range.last
        }
        val fresh = maxOf(range.first, (counted ?: 0) + 1)..range.last
        return record.toEvent(units = fresh.units, range = fresh.takeUnless { it.isEmpty() }, isRepeat = rewatch, isCompletion = false)
    }

    private fun finish(record: ListActivityRecord, total: Int?, rewatch: Boolean): CountedEvent {
        val counted = if (rewatch) rewatchReach else reach
        val rest = if (counted != null && total != null) (counted + 1)..total else null
        if (rewatch) rewatchReach = null else if (total != null) reach = maxOf(counted ?: 0, total)
        return record.toEvent(
            units = rest?.units ?: 1,
            range = if (rest != null) rest.takeUnless { it.isEmpty() } else total?.let { it..it },
            isRepeat = rewatch,
            isCompletion = true,
        )
    }
}

private val IntRange.units: Int get() = if (isEmpty()) 0 else last - first + 1

private fun ListActivityRecord.toEvent(units: Int, range: IntRange?, isRepeat: Boolean, isCompletion: Boolean) =
    CountedEvent(
        activityId = id,
        mediaId = mediaId,
        type = type,
        createdAt = createdAt,
        units = units,
        range = range,
        isRepeat = isRepeat,
        isCompletion = isCompletion,
    )
