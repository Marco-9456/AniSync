package com.anisync.android.presentation.profile.lists

import androidx.compose.runtime.Immutable
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.profile.ProfileTab
import com.anisync.android.type.MediaType

/** What the list tabs, Overview and Compare show, worked out together from one snapshot of the lists. */
@Immutable
class ProfileListsDerived(
    val results: ProfileListResults = ProfileListResults(),
    /** The list tab [results] belong to, null on any other tab. */
    val resultsTab: ProfileTab? = null,
    val comparison: ListComparison? = null,
    val watching: List<LibraryEntry> = emptyList()
)

/**
 * Works out [ProfileListsDerived] off the main thread. Each part is kept until what it reads
 * changes, so a loading flag, the list or grid toggle or a tab switch does not redo the others.
 * It runs inside one flow, never two at once.
 */
class ProfileListDeriver(private val comparesWithViewer: Boolean) {
    private var resultsKey: ResultsKey? = null
    private var results = ProfileListResults()
    private var comparisonKey: ComparisonKey? = null
    private var comparison: ListComparison? = null
    private var watchingSource: List<LibraryEntry>? = null
    private var watching: List<LibraryEntry> = emptyList()

    fun derive(
        anime: List<LibraryEntry>,
        manga: List<LibraryEntry>,
        tab: ProfileTab,
        animeStatus: LibraryStatus?,
        mangaStatus: LibraryStatus?,
        compareType: MediaType,
        tools: ProfileListTools,
        viewer: Map<Int, LibraryEntry>,
        language: TitleLanguage,
        yourFormat: ScoreFormat
    ): ProfileListsDerived {
        val listTab = tab.takeIf { it.isList }
        if (listTab != null) {
            val onManga = listTab == ProfileTab.MANGA
            val key = ResultsKey(
                entries = if (onManga) manga else anime,
                status = if (onManga) mangaStatus else animeStatus,
                tools = tools.copy(view = ProfileListView.LIST),
                viewer = viewer,
                language = language
            )
            if (!key.sameAs(resultsKey)) {
                resultsKey = key
                val counts = if (comparesWithViewer) yourListCounts(key.entries, key.status, key.tools, viewer) else emptyMap()
                val shownStatus = if (key.tools.query.isNotBlank()) key.tools.searchStatus else key.status
                results = applyListTools(key.entries, key.status, key.tools, viewer, language).copy(
                    yourListCounts = counts,
                    key = "${listTab.name}_${shownStatus?.name ?: "ALL"}"
                )
            }
        }

        val compares = comparesWithViewer && (tab == ProfileTab.OVERVIEW || tab == ProfileTab.COMPARE)
        if (compares) {
            val type = if (tab == ProfileTab.COMPARE) compareType else MediaType.ANIME
            val theirs = if (type == MediaType.MANGA) manga else anime
            val key = ComparisonKey(theirs, viewer, type, yourFormat)
            if (!key.sameAs(comparisonKey)) {
                comparisonKey = key
                comparison = if (theirs.isEmpty()) null else compareLists(theirs, viewer.values, type, yourFormat)
            }
        }

        if (anime !== watchingSource) {
            watchingSource = anime
            watching = anime
                .filter { it.status == LibraryStatus.CURRENT || it.status == LibraryStatus.REPEATING }
                .distinctBy { it.mediaId }
                .sortedByDescending { it.updatedAt ?: 0L }
        }

        return ProfileListsDerived(
            results = if (listTab != null) results else ProfileListResults(),
            resultsTab = listTab,
            comparison = if (compares) comparison else null,
            watching = watching
        )
    }

    /** Lists and the viewer's library are compared by identity, the flows above only replace them when they change. */
    private class ResultsKey(
        val entries: List<LibraryEntry>,
        val status: LibraryStatus?,
        val tools: ProfileListTools,
        val viewer: Map<Int, LibraryEntry>,
        val language: TitleLanguage
    ) {
        fun sameAs(other: ResultsKey?) = other != null && entries === other.entries && status == other.status &&
            tools == other.tools && viewer === other.viewer && language == other.language
    }

    private class ComparisonKey(
        val theirs: List<LibraryEntry>,
        val viewer: Map<Int, LibraryEntry>,
        val type: MediaType,
        val format: ScoreFormat
    ) {
        fun sameAs(other: ComparisonKey?) = other != null && theirs === other.theirs && viewer === other.viewer &&
            type == other.type && format == other.format
    }
}
