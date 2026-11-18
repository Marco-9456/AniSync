package com.anisync.android.presentation.profile.lists

import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.profile.ProfileTab
import com.anisync.android.type.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ProfileListDeriverTest {

    private fun entry(mediaId: Int, status: LibraryStatus, updatedAt: Long, type: MediaType = MediaType.ANIME) = LibraryEntry(
        id = mediaId,
        mediaId = mediaId,
        titleRomaji = "Title $mediaId",
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = "Title $mediaId",
        coverUrl = null,
        progress = 0,
        totalEpisodes = 12,
        totalChapters = null,
        totalVolumes = null,
        type = type,
        format = null,
        status = status,
        score = 8.0,
        scoreFormat = ScoreFormat.POINT_10,
        updatedAt = updatedAt
    )

    private val anime = listOf(
        entry(1, LibraryStatus.COMPLETED, updatedAt = 4),
        entry(2, LibraryStatus.CURRENT, updatedAt = 1),
        entry(3, LibraryStatus.REPEATING, updatedAt = 3),
        entry(4, LibraryStatus.CURRENT, updatedAt = 2)
    )
    private val manga = listOf(entry(10, LibraryStatus.CURRENT, updatedAt = 1, type = MediaType.MANGA))
    private val viewer = mapOf(1 to entry(1, LibraryStatus.COMPLETED, updatedAt = 9))

    private fun ProfileListDeriver.run(tab: ProfileTab, tools: ProfileListTools = ProfileListTools(), compareType: MediaType = MediaType.ANIME) =
        derive(anime, manga, tab, null, null, compareType, tools, viewer, TitleLanguage.ROMAJI, ScoreFormat.POINT_10)

    @Test
    fun `the list or grid toggle keeps the worked out rows`() {
        val deriver = ProfileListDeriver(comparesWithViewer = true)
        val list = deriver.run(ProfileTab.ANIME)
        val grid = deriver.run(ProfileTab.ANIME, ProfileListTools(view = ProfileListView.GRID))
        assertSame(list.results, grid.results)
        assertEquals(ProfileTab.ANIME, grid.resultsTab)
        assertEquals(4, grid.results.items.size)
    }

    @Test
    fun `your list counts are only worked out on someone else's list`() {
        assertEquals(mapOf(YourListFilter.ON_YOUR_LIST to 1, YourListFilter.NOT_ON_YOUR_LIST to 3), ProfileListDeriver(true).run(ProfileTab.ANIME).results.yourListCounts)
        assertEquals(emptyMap<YourListFilter, Int>(), ProfileListDeriver(false).run(ProfileTab.ANIME).results.yourListCounts)
    }

    @Test
    fun `the comparison follows Overview and Compare and stays off elsewhere`() {
        val deriver = ProfileListDeriver(comparesWithViewer = true)
        val overview = deriver.run(ProfileTab.OVERVIEW).comparison
        assertNotNull(overview)
        assertEquals(1, overview!!.sharedCount)
        assertNull(deriver.run(ProfileTab.STATS).comparison)
        assertSame(overview, deriver.run(ProfileTab.OVERVIEW).comparison)
        assertEquals(0, deriver.run(ProfileTab.COMPARE, compareType = MediaType.MANGA).comparison!!.sharedCount)
        assertNull(ProfileListDeriver(comparesWithViewer = false).run(ProfileTab.OVERVIEW).comparison)
    }

    @Test
    fun `watching keeps current and rewatching titles, latest first`() {
        val watching = ProfileListDeriver(comparesWithViewer = false).run(ProfileTab.OVERVIEW).watching
        assertEquals(listOf(3, 4, 2), watching.map { it.mediaId })
    }
}
