package com.anisync.android.data

import com.anisync.android.SaveMediaListEntryMutation
import com.anisync.android.data.mapper.toApiStatus
import com.anisync.android.data.mapper.toFuzzyDateInput
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.type.FuzzyDateInput
import com.apollographql.apollo.api.Optional

/**
 * Which list fields an edit changed, against the entry the edit started from.
 *
 * Saving only those means a field changed on the website or in another app since the editor opened
 * is left alone, instead of being written over with what this device last saw. AniList delisted an
 * app that did exactly that.
 */
internal data class ListEntryEdits(
    val status: Boolean,
    val progress: Boolean,
    val progressVolumes: Boolean,
    val score: Boolean,
    val advancedScores: Boolean,
    val repeat: Boolean,
    val priority: Boolean,
    val notes: Boolean,
    val startedAt: Boolean,
    val completedAt: Boolean,
    val customLists: Boolean,
    val isPrivate: Boolean,
    val hiddenFromStatusLists: Boolean
) {
    val isEmpty: Boolean
        get() = !(status || progress || progressVolumes || score || advancedScores || repeat ||
            priority || notes || startedAt || completedAt || customLists || isPrivate ||
            hiddenFromStatusLists)

    /** [base] with the changed fields taken from [edited] and the rest kept. */
    fun applyTo(base: LibraryEntry, edited: LibraryEntry): LibraryEntry = base.copy(
        status = if (status) edited.status else base.status,
        progress = if (progress) edited.progress else base.progress,
        progressVolumes = if (progressVolumes) edited.progressVolumes else base.progressVolumes,
        score = if (score) edited.score else base.score,
        advancedScores = if (advancedScores) edited.advancedScores else base.advancedScores,
        rewatches = if (repeat) edited.rewatches else base.rewatches,
        priority = if (priority) edited.priority else base.priority,
        notes = if (notes) edited.notes else base.notes,
        startedAt = if (startedAt) edited.startedAt else base.startedAt,
        completedAt = if (completedAt) edited.completedAt else base.completedAt,
        customLists = if (customLists) edited.customLists else base.customLists,
        isPrivate = if (isPrivate) edited.isPrivate else base.isPrivate,
        hiddenFromStatusLists = if (hiddenFromStatusLists) edited.hiddenFromStatusLists else base.hiddenFromStatusLists
    )

    companion object {
        fun between(from: LibraryEntry, to: LibraryEntry) = ListEntryEdits(
            status = from.status != to.status,
            progress = from.progress != to.progress,
            progressVolumes = (from.progressVolumes ?: 0) != (to.progressVolumes ?: 0),
            score = (from.score ?: 0.0) != (to.score ?: 0.0),
            advancedScores = from.advancedScores != to.advancedScores,
            repeat = from.rewatches != to.rewatches,
            priority = from.priority != to.priority,
            notes = from.notes.orEmpty() != to.notes.orEmpty(),
            startedAt = from.startedAt != to.startedAt,
            completedAt = from.completedAt != to.completedAt,
            customLists = from.customLists.toSet() != to.customLists.toSet(),
            isPrivate = from.isPrivate != to.isPrivate,
            hiddenFromStatusLists = from.hiddenFromStatusLists != to.hiddenFromStatusLists
        )
    }
}

// AniList clears a date when every part of it comes as null.
private val NO_DATE = FuzzyDateInput(
    year = Optional.present(null),
    month = Optional.present(null),
    day = Optional.present(null)
)

/**
 * The save for [entry]. With [edits] it carries only the changed fields, cleared ones included.
 * Without, the entry is new to AniList and goes whole, minus the empty fields and a default priority.
 */
internal fun listEntryMutation(
    entry: LibraryEntry,
    edits: ListEntryEdits?,
    advancedScores: Optional<List<Double>>
): SaveMediaListEntryMutation {
    fun <T> pick(changed: Boolean, value: () -> Optional<T>): Optional<T> =
        if (changed) value() else Optional.absent()

    if (edits == null) {
        return SaveMediaListEntryMutation(
            mediaId = Optional.present(entry.mediaId),
            status = Optional.present(entry.status.toApiStatus()),
            progress = Optional.present(entry.progress),
            progressVolumes = Optional.presentIfNotNull(entry.progressVolumes),
            score = Optional.presentIfNotNull(entry.score),
            advancedScores = advancedScores,
            repeat = Optional.present(entry.rewatches),
            // AniList's priority is a plain 0..255 Int and this client only writes 0/1/2, so an
            // untouched one is never sent.
            priority = pick(entry.priority != 0) { Optional.present(entry.priority) },
            notes = Optional.presentIfNotNull(entry.notes),
            startedAt = Optional.presentIfNotNull(entry.startedAt?.toFuzzyDateInput()),
            completedAt = Optional.presentIfNotNull(entry.completedAt?.toFuzzyDateInput()),
            customLists = Optional.present(entry.customLists),
            `private` = Optional.present(entry.isPrivate),
            hiddenFromStatusLists = Optional.present(entry.hiddenFromStatusLists)
        )
    }
    return SaveMediaListEntryMutation(
        mediaId = Optional.present(entry.mediaId),
        status = pick(edits.status) { Optional.present(entry.status.toApiStatus()) },
        progress = pick(edits.progress) { Optional.present(entry.progress) },
        progressVolumes = pick(edits.progressVolumes) { Optional.present(entry.progressVolumes ?: 0) },
        score = pick(edits.score) { Optional.present(entry.score ?: 0.0) },
        advancedScores = if (edits.advancedScores) advancedScores else Optional.absent(),
        repeat = pick(edits.repeat) { Optional.present(entry.rewatches) },
        priority = pick(edits.priority) { Optional.present(entry.priority) },
        notes = pick(edits.notes) { Optional.present(entry.notes.orEmpty()) },
        startedAt = pick(edits.startedAt) { Optional.present(entry.startedAt?.toFuzzyDateInput() ?: NO_DATE) },
        completedAt = pick(edits.completedAt) { Optional.present(entry.completedAt?.toFuzzyDateInput() ?: NO_DATE) },
        customLists = pick(edits.customLists) { Optional.present(entry.customLists) },
        `private` = pick(edits.isPrivate) { Optional.present(entry.isPrivate) },
        hiddenFromStatusLists = pick(edits.hiddenFromStatusLists) { Optional.present(entry.hiddenFromStatusLists) }
    )
}
