package com.anisync.android.presentation.profile.lists

import androidx.compose.runtime.Immutable
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.type.MediaType
import com.anisync.android.domain.ScoreFormat
import kotlin.math.abs

enum class ScoreVerdict { SAME, YOURS_HIGHER, THEIRS_HIGHER }

@Immutable
data class SharedTitle(val theirs: LibraryEntry, val yours: LibraryEntry) {
    /** Null unless both of you scored it. */
    val verdict: ScoreVerdict?
        get() {
            val their = theirs.scoreOutOf100() ?: return null
            val your = yours.scoreOutOf100() ?: return null
            return when {
                abs(their - your) < 0.5 -> ScoreVerdict.SAME
                your > their -> ScoreVerdict.YOURS_HIGHER
                else -> ScoreVerdict.THEIRS_HIGHER
            }
        }
}

@Immutable
data class ListComparison(
    val yourCount: Int,
    val theirCount: Int,
    val bothScored: List<SharedTitle>,
    val alsoShared: List<SharedTitle>,
    val theirTensNotYours: List<LibraryEntry>,
    val onlyYours: List<LibraryEntry>,
    val yourMean: Double?,
    val theirMean: Double?
) {
    val sharedCount: Int get() = bothScored.size + alsoShared.size
    val theirsNotYours: Int get() = theirCount - sharedCount
    val sameScore: SharedTitle? get() = bothScored.firstOrNull { it.verdict == ScoreVerdict.SAME }
}

private const val TOP_SCORE = 99.5

/**
 * The viewer's entries come from the Library cache, which keeps the raw score in the viewer's own
 * format, so [yourFormat] scales them. The owner's entries carry their format.
 */
fun compareLists(
    theirs: List<LibraryEntry>,
    yours: Collection<LibraryEntry>,
    type: MediaType,
    yourFormat: ScoreFormat
): ListComparison {
    val theirByMedia = theirs.distinctBy { it.mediaId }.associateBy { it.mediaId }
    val mine = yours
        .filter { it.type == null || it.type == type }
        .distinctBy { it.mediaId }
        .map { if (it.scoreFormat == null) it.copy(scoreFormat = yourFormat) else it }
    val shared = mine.mapNotNull { entry -> theirByMedia[entry.mediaId]?.let { SharedTitle(it, entry) } }
    val (bothScored, alsoShared) = shared.partition {
        it.theirs.scoreOutOf100() != null && it.yours.scoreOutOf100() != null
    }
    val myIds = mine.mapTo(HashSet()) { it.mediaId }
    return ListComparison(
        yourCount = mine.size,
        theirCount = theirByMedia.size,
        bothScored = bothScored.sortedByDescending { it.theirs.scoreOutOf100() },
        alsoShared = alsoShared,
        theirTensNotYours = theirByMedia.values.filter {
            it.mediaId !in myIds && (it.scoreOutOf100() ?: 0.0) >= TOP_SCORE
        },
        onlyYours = mine.filter { it.mediaId !in theirByMedia },
        yourMean = mine.mapNotNull { it.scoreOutOf100() }.averageOrNull(),
        theirMean = theirByMedia.values.mapNotNull { it.scoreOutOf100() }.averageOrNull()
    )
}

private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
