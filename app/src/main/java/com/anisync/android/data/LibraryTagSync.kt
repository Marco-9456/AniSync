package com.anisync.android.data

import com.anisync.android.domain.Tag
import com.anisync.android.fragment.LibraryMediaTags
import java.util.concurrent.TimeUnit

/** Tags shift slowly, so a week-old copy is still a fair picture of a title. */
internal val TAGS_MAX_AGE_MILLIS = TimeUnit.DAYS.toMillis(7)

/** The most titles one `Page(media(id_in:))` request returns. */
internal const val TAGS_PAGE_SIZE = 50

/** What the next tag fetch has to cover, worked out from what Room already holds. */
internal sealed interface TagSyncPlan {
    data object UpToDate : TagSyncPlan

    /** Few enough titles for one `media(id_in:)` page. */
    data class Titles(val mediaIds: List<Int>) : TagSyncPlan

    /**
     * More than one page's worth. Paging would cost a request per fifty titles, so the whole list
     * comes down in a single request instead.
     */
    data object WholeList : TagSyncPlan
}

internal fun planTagSync(
    libraryMediaIds: Collection<Int>,
    fetchedAt: Map<Int, Long>,
    now: Long
): TagSyncPlan {
    val due = libraryMediaIds.filter { it > 0 }.distinct().filter { id ->
        val at = fetchedAt[id]
        at == null || now - at > TAGS_MAX_AGE_MILLIS
    }
    return when {
        due.isEmpty() -> TagSyncPlan.UpToDate
        due.size <= TAGS_PAGE_SIZE -> TagSyncPlan.Titles(due)
        else -> TagSyncPlan.WholeList
    }
}

internal fun LibraryMediaTags.toTags(): List<Tag> = tags.orEmpty().filterNotNull().map { tag ->
    Tag(
        name = tag.name,
        category = tag.category.orEmpty(),
        isMediaSpoiler = tag.isMediaSpoiler == true,
        isGeneralSpoiler = tag.isGeneralSpoiler == true,
        rank = tag.rank,
        isAdult = tag.isAdult == true
    )
}
