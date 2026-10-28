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
 * AniList logs the last episode of a series as a completion with no progress, so a completion adds
 * the units after the last one logged for that title, or one when nothing was logged before it
 * (the final episode, or a one-episode title). [totalUnits] is the title's episode or chapter count.
 * Pass every activity known before the period too, so a completion can see what came before it.
 */
fun countEvents(
    records: List<ListActivityRecord>,
    totalUnits: (mediaId: Int) -> Int?,
): List<CountedEvent> {
    val lastLogged = HashMap<Int, Int>()
    val events = ArrayList<CountedEvent>(records.size)
    for (record in records.sortedWith(compareBy({ it.createdAt }, { it.id }))) {
        val kind = statusKind(record.status)
        val total = totalUnits(record.mediaId)?.takeIf { it > 0 }
        when (kind) {
            StatusKind.PROGRESS, StatusKind.REPEAT_PROGRESS -> {
                val range = parseProgress(record.progress)
                range?.let { lastLogged[record.mediaId] = it.last }
                events += record.toEvent(
                    units = range?.let { it.last - it.first + 1 } ?: 1,
                    range = range,
                    isRepeat = kind == StatusKind.REPEAT_PROGRESS,
                    isCompletion = false,
                )
            }
            StatusKind.COMPLETED, StatusKind.REPEAT_COMPLETED -> {
                val previous = lastLogged[record.mediaId]
                val units: Int
                val range: IntRange?
                if (previous != null && total != null) {
                    units = (total - previous).coerceAtLeast(0)
                    range = if (units > 0) (previous + 1)..total else null
                } else {
                    units = 1
                    range = when {
                        total == null || total == 1 -> 1..1
                        else -> total..total
                    }
                }
                total?.let { lastLogged[record.mediaId] = it }
                events += record.toEvent(
                    units = units,
                    range = range,
                    isRepeat = kind == StatusKind.REPEAT_COMPLETED,
                    isCompletion = true,
                )
            }
            StatusKind.OTHER -> Unit
        }
    }
    return events
}

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
