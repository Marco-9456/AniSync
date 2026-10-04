package com.anisync.android.domain

/**
 * Whether a quick action that left an entry as [after] should ask for a score: it was already on a
 * list, has just moved into Completed from some other status, and there is no score on it to keep.
 *
 * An entry that was on no list at all ([statusBefore] null) never asks. That is how someone new to
 * AniList fills in years of history, one title after another straight into Completed, and a sheet
 * after every one of them would turn a backfill into a chore.
 *
 * The one rule every surface asks through, so finishing a title from the library and from its
 * details page can never disagree about when the score sheet appears.
 */
fun shouldAskForScore(statusBefore: LibraryStatus?, after: LibraryEntry?): Boolean =
    after != null &&
        statusBefore != null &&
        statusBefore != LibraryStatus.COMPLETED &&
        after.status == LibraryStatus.COMPLETED &&
        (after.score ?: 0.0) <= 0.0

/**
 * [this] with what the score sheet collected. A zero score means "not scored", the same as the edit
 * sheet, so a note saved on its own leaves the score as it was. A blank note clears it.
 */
fun LibraryEntry.withCompletionScore(
    score: Double,
    advancedScores: Map<String, Double>,
    notes: String
): LibraryEntry = copy(
    score = score.takeIf { it > 0 } ?: this.score,
    advancedScores = if (score > 0) advancedScores else this.advancedScores,
    notes = notes.takeIf { it.isNotBlank() }
)
