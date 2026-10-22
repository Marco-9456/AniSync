package com.anisync.android.presentation.library

import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.LIBRARY_ALL_TAB_ID
import com.anisync.android.presentation.util.LIBRARY_FAVORITES_TAB_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins how library search groups its matches and how the library's sort direction reads. */
class LibrarySearchTest {

    private fun entry(mediaId: Int, status: LibraryStatus) = LibraryEntry(
        id = mediaId,
        mediaId = mediaId,
        titleRomaji = null,
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = "Title $mediaId",
        coverUrl = null,
        progress = 0,
        totalEpisodes = null,
        totalChapters = null,
        totalVolumes = null,
        type = null,
        status = status
    )

    private val tabOrder = listOf(
        LIBRARY_ALL_TAB_ID,
        "status:PAUSED",
        "status:CURRENT",
        "status:COMPLETED",
        LIBRARY_FAVORITES_TAB_ID,
        "Rewatch soon"
    )

    @Test
    fun `matches group by list in the order the rail lists them`() {
        val entries = listOf(
            entry(1, LibraryStatus.COMPLETED),
            entry(2, LibraryStatus.CURRENT),
            entry(3, LibraryStatus.PAUSED),
            entry(4, LibraryStatus.COMPLETED)
        )
        val groups = groupByList(entries, tabOrder)
        assertEquals(
            listOf(LibraryStatus.PAUSED, LibraryStatus.CURRENT, LibraryStatus.COMPLETED),
            groups.map { it.first }
        )
        assertEquals(listOf(1, 4), groups.last().second.map { it.mediaId })
    }

    @Test
    fun `a list missing from the tab order goes last`() {
        val groups = groupByList(
            listOf(entry(1, LibraryStatus.PLANNING), entry(2, LibraryStatus.CURRENT)),
            tabOrder
        )
        assertEquals(listOf(LibraryStatus.CURRENT, LibraryStatus.PLANNING), groups.map { it.first })
    }

    @Test
    fun `the rail lists all, then each list with matches, and falls back to all`() {
        val watching = entry(1, LibraryStatus.CURRENT)
        val done = entry(2, LibraryStatus.COMPLETED)
        val state = LibraryUiState(
            tabOrder = tabOrder,
            customListNames = listOf("Rewatch soon"),
            searchMatches = listOf(watching, done),
            searchMatchesByCategory = mapOf(
                "status:COMPLETED" to listOf(done),
                "status:CURRENT" to listOf(watching),
                "Rewatch soon" to listOf(done)
            ),
            activeSearchCategory = "status:PAUSED"
        )
        val view = librarySearchView(state)
        assertEquals(
            listOf(LIBRARY_ALL_TAB_ID, "status:CURRENT", "status:COMPLETED", "Rewatch soon"),
            view.categories.map { it.first.toId() }
        )
        assertEquals(listOf(2, 1, 1, 1), view.categories.map { it.second })
        // Paused holds no match, so the pick falls back to All.
        assertEquals(LibraryTab.All, view.active)
        assertEquals(2, view.entries.size)

        val custom = librarySearchView(state.copy(activeSearchCategory = "Rewatch soon"))
        assertEquals(LibraryTab.Custom("Rewatch soon"), custom.active)
        assertEquals(listOf(2), custom.entries.map { it.mediaId })
    }

    @Test
    fun `the stored flag is the column's natural order, the toggle shows the real one`() {
        // Score runs highest first when the flag is set, which is descending.
        assertFalse(LibrarySort.SCORE.flipDirection(true))
        assertTrue(LibrarySort.SCORE.flipDirection(false))
        // Title and airing soon run A to Z and soonest first, which is ascending.
        assertTrue(LibrarySort.TITLE.flipDirection(true))
        assertTrue(LibrarySort.AIRING_SOON.flipDirection(true))
        // Flipping twice gives back the stored flag.
        for (sort in LibrarySort.entries) {
            for (flag in listOf(true, false)) assertEquals(flag, sort.flipDirection(sort.flipDirection(flag)))
        }
    }
}
