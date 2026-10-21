package com.anisync.android.presentation.library

import androidx.compose.runtime.Immutable
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.Tag

/**
 * The rank a tag has to beat before it counts. AniList search makes the same cut through
 * `minimumTagRank`, which defaults to 18 and applies to included and excluded tags alike, so the
 * library and Search agree on what a tag filter matches.
 */
const val MIN_TAG_RANK = 18

fun Tag.countsForFilters(): Boolean = (rank ?: 0) > MIN_TAG_RANK

/** One tag the library's filter sheet can offer, summed over every title that carries it. */
@Immutable
data class LibraryTagOption(
    val name: String,
    val category: String,
    val isAdult: Boolean,
    /** A spoiler for at least one of the titles, which is enough to keep the name out of sight. */
    val isSpoiler: Boolean,
    /** How many titles carry it strongly enough to count. */
    val titleCount: Int
)

/**
 * The tags worth offering for [entries], sorted by name.
 *
 * Only tags that count on at least one title are listed, so every option can match something.
 */
fun libraryTagOptions(
    entries: List<LibraryEntry>,
    tagsByMedia: Map<Int, List<Tag>>
): List<LibraryTagOption> {
    val byName = HashMap<String, LibraryTagOption>()
    for (entry in entries) {
        for (tag in tagsByMedia[entry.mediaId].orEmpty()) {
            if (!tag.countsForFilters()) continue
            val spoiler = tag.isMediaSpoiler || tag.isGeneralSpoiler
            val known = byName[tag.name]
            byName[tag.name] = known?.copy(
                isSpoiler = known.isSpoiler || spoiler,
                titleCount = known.titleCount + 1
            ) ?: LibraryTagOption(
                name = tag.name,
                category = tag.category,
                isAdult = tag.isAdult,
                isSpoiler = spoiler,
                titleCount = 1
            )
        }
    }
    return byName.values.sortedBy { it.name }
}
