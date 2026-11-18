package com.anisync.android.presentation.profile.lists

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.max
import com.anisync.android.presentation.components.filtersheet.TriState
import com.anisync.android.type.MediaFormat
import com.anisync.android.util.getTitle

enum class ProfileListSort(@StringRes val labelRes: Int) {
    UPDATED(R.string.profile_list_sort_updated),
    THEIR_SCORE(R.string.profile_list_sort_their_score),
    YOUR_SCORE(R.string.profile_list_sort_your_score),
    TITLE(R.string.profile_list_sort_title),
    AVERAGE_SCORE(R.string.profile_list_sort_average_score),
    POPULARITY(R.string.sort_popularity),
    RELEASE_DATE(R.string.profile_list_sort_release_date),
    ADDED(R.string.profile_list_sort_added),
    STARTED(R.string.profile_list_sort_started),
    FINISHED(R.string.profile_list_sort_finished),
    PROGRESS(R.string.profile_list_sort_progress),
    REWATCHES(R.string.profile_list_sort_rewatches)
}

enum class YourListFilter(@StringRes val labelRes: Int) {
    ANY(R.string.filter_any),
    ON_YOUR_LIST(R.string.profile_list_on_your_list),
    NOT_ON_YOUR_LIST(R.string.profile_list_not_on_your_list)
}

enum class ProfileListView { LIST, GRID }

enum class ProfileListSheet { NONE, SORT, FILTERS }

@Immutable
data class ProfileListFilters(
    val yourList: YourListFilter = YourListFilter.ANY,
    val formats: Set<MediaFormat> = emptySet(),
    val genres: Set<String> = emptySet(),
    val excludedGenres: Set<String> = emptySet(),
    /** Out of 100. [UNSCORED] keeps only unscored titles. */
    val scoreFloor: Int? = null
) {
    val isActive: Boolean
        get() = yourList != YourListFilter.ANY || formats.isNotEmpty() || genres.isNotEmpty() ||
            excludedGenres.isNotEmpty() || scoreFloor != null

    fun genreState(genre: String): TriState = when (genre) {
        in genres -> TriState.INCLUDED
        in excludedGenres -> TriState.EXCLUDED
        else -> TriState.OFF
    }

    fun withGenre(genre: String, state: TriState): ProfileListFilters = copy(
        genres = if (state == TriState.INCLUDED) genres + genre else genres - genre,
        excludedGenres = if (state == TriState.EXCLUDED) excludedGenres + genre else excludedGenres - genre
    )

    companion object {
        const val UNSCORED = 0
    }
}

@Immutable
data class ProfileListTools(
    val query: String = "",
    val sort: ProfileListSort = ProfileListSort.UPDATED,
    val ascending: Boolean = false,
    val filters: ProfileListFilters = ProfileListFilters(),
    val view: ProfileListView = ProfileListView.LIST,
    /** The status picked among a search's matches. Each new search starts on all of them. */
    val searchStatus: LibraryStatus? = null
)

@Immutable
data class ProfileListResults(
    val items: List<LibraryEntry> = emptyList(),
    val inScope: Int = 0,
    /** While searching, how many matches each status holds, which is what the chips then count. */
    val searchCounts: Map<LibraryStatus, Int>? = null,
    /** On someone else's list, how many titles each your-list filter would leave. */
    val yourListCounts: Map<YourListFilter, Int> = emptyMap(),
    /** The tab and chip these rows belong to, see [com.anisync.android.presentation.profile.contentKey]. */
    val key: String? = null
)

internal fun LibraryEntry.scoreOutOf100(): Double? {
    val raw = score ?: return null
    if (raw <= 0.0) return null
    return raw / (scoreFormat ?: ScoreFormat.POINT_100).max * 100.0
}

fun scoreFloors(format: ScoreFormat): List<Int> = when (format) {
    ScoreFormat.POINT_100, ScoreFormat.POINT_10, ScoreFormat.POINT_10_DECIMAL -> listOf(100, 90, 80, 70)
    ScoreFormat.POINT_5 -> listOf(100, 80, 60)
    ScoreFormat.POINT_3 -> listOf(100, 67)
}

/**
 * A search ignores the chip, so a title turns up whichever status it has, and the chips then count
 * the matches instead, [ProfileListTools.searchStatus] narrowing to one of them.
 */
fun applyListTools(
    entries: List<LibraryEntry>,
    status: LibraryStatus?,
    tools: ProfileListTools,
    viewer: Map<Int, LibraryEntry>,
    titleLanguage: TitleLanguage
): ProfileListResults {
    val query = tools.query.trim()
    val underChip = if (status == null) entries else entries.filter { it.status == status }
    if (query.isEmpty()) {
        return ProfileListResults(
            items = underChip.filter { it.passes(tools.filters, viewer) }.sortedFor(tools, viewer, titleLanguage),
            inScope = underChip.size
        )
    }
    val found = entries.filter { it.matches(query) && it.passes(tools.filters, viewer) }
    val shown = tools.searchStatus?.let { picked -> found.filter { it.status == picked } } ?: found
    return ProfileListResults(
        items = shown.sortedFor(tools, viewer, titleLanguage),
        inScope = underChip.size,
        searchCounts = found.groupingBy { it.status }.eachCount()
    )
}

/** How many titles [applyListTools] would show, without putting them in order. */
fun countListTools(
    entries: List<LibraryEntry>,
    status: LibraryStatus?,
    tools: ProfileListTools,
    viewer: Map<Int, LibraryEntry>
): Int {
    val query = tools.query.trim()
    if (query.isEmpty()) return entries.count { (status == null || it.status == status) && it.passes(tools.filters, viewer) }
    val picked = tools.searchStatus
    return entries.count { (picked == null || it.status == picked) && it.matches(query) && it.passes(tools.filters, viewer) }
}

/** What each your-list choice would leave, with the other filters kept. */
fun yourListCounts(
    entries: List<LibraryEntry>,
    status: LibraryStatus?,
    tools: ProfileListTools,
    viewer: Map<Int, LibraryEntry>
): Map<YourListFilter, Int> =
    listOf(YourListFilter.ON_YOUR_LIST, YourListFilter.NOT_ON_YOUR_LIST).associateWith { choice ->
        countListTools(entries, status, tools.copy(filters = tools.filters.copy(yourList = choice)), viewer)
    }

private fun LibraryEntry.passes(filters: ProfileListFilters, viewer: Map<Int, LibraryEntry>): Boolean =
    when (filters.yourList) {
        YourListFilter.ANY -> true
        YourListFilter.ON_YOUR_LIST -> mediaId in viewer
        YourListFilter.NOT_ON_YOUR_LIST -> mediaId !in viewer
    } &&
        (filters.formats.isEmpty() || format in filters.formats) &&
        (filters.genres.isEmpty() || genres.containsAll(filters.genres)) &&
        genres.none { it in filters.excludedGenres } &&
        passesScoreFloor(filters.scoreFloor)

private fun LibraryEntry.matches(query: String): Boolean =
    titleRomaji?.contains(query, ignoreCase = true) == true ||
        titleEnglish?.contains(query, ignoreCase = true) == true ||
        titleNative?.contains(query, ignoreCase = true) == true ||
        titleUserPreferred.contains(query, ignoreCase = true)

private fun LibraryEntry.passesScoreFloor(floor: Int?): Boolean {
    val score = scoreOutOf100()
    return when (floor) {
        null -> true
        ProfileListFilters.UNSCORED -> score == null
        // A hair under the floor still counts, so 4.5 stars clears "90 and up" after rounding.
        else -> score != null && score >= floor - 0.5
    }
}

private class SortRow(val entry: LibraryEntry, val title: String, val key: Double?)

/** Keys are worked out once per title, not on both sides of every comparison. */
private fun List<LibraryEntry>.sortedFor(
    tools: ProfileListTools,
    viewer: Map<Int, LibraryEntry>,
    titleLanguage: TitleLanguage
): List<LibraryEntry> {
    val byTitle = compareBy<SortRow> { it.title }
    if (tools.sort == ProfileListSort.TITLE) {
        val rows = map { SortRow(it, it.getTitle(titleLanguage).lowercase(), null) }
        return rows.sortedWith(if (tools.ascending) byTitle else byTitle.reversed()).map { it.entry }
    }
    val rows = map { SortRow(it, it.getTitle(titleLanguage).lowercase(), it.sortKey(tools.sort, viewer)) }
    val (keyed, blank) = rows.partition { it.key != null }
    val byKey = compareBy<SortRow> { it.key }
    val ordered = keyed.sortedWith((if (tools.ascending) byKey else byKey.reversed()).then(byTitle))
    return (ordered + blank.sortedWith(byTitle)).map { it.entry }
}

private fun LibraryEntry.sortKey(sort: ProfileListSort, viewer: Map<Int, LibraryEntry>): Double? = when (sort) {
    ProfileListSort.UPDATED -> updatedAt?.toDouble()
    ProfileListSort.THEIR_SCORE -> scoreOutOf100()
    ProfileListSort.YOUR_SCORE -> viewer[mediaId]?.score?.takeIf { it > 0.0 }
    ProfileListSort.AVERAGE_SCORE -> averageScore?.toDouble()
    ProfileListSort.POPULARITY -> popularity?.toDouble()
    ProfileListSort.RELEASE_DATE -> mediaStartDate?.toDouble()
    ProfileListSort.ADDED -> createdAt?.toDouble()
    ProfileListSort.STARTED -> startedAt?.toDouble()
    ProfileListSort.FINISHED -> completedAt?.toDouble()
    ProfileListSort.PROGRESS -> progress.toDouble()
    ProfileListSort.REWATCHES -> rewatches.toDouble()
    ProfileListSort.TITLE -> null
}
