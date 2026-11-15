package com.anisync.android.presentation.profile.lists

import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.type.MediaFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileListToolsTest {

    private fun entry(
        mediaId: Int,
        title: String,
        status: LibraryStatus = LibraryStatus.COMPLETED,
        score: Double = 0.0,
        format: MediaFormat = MediaFormat.TV,
        genres: List<String> = emptyList(),
        updatedAt: Long? = null,
        english: String? = null
    ) = LibraryEntry(
        id = mediaId,
        mediaId = mediaId,
        titleRomaji = title,
        titleEnglish = english,
        titleNative = null,
        titleUserPreferred = title,
        coverUrl = null,
        progress = 0,
        totalEpisodes = 12,
        totalChapters = null,
        totalVolumes = null,
        type = null,
        format = format,
        status = status,
        score = score,
        scoreFormat = ScoreFormat.POINT_10,
        updatedAt = updatedAt,
        genres = genres
    )

    private val madoka = entry(9756, "Mahou Shoujo Madoka Magica", score = 10.0, english = "Puella Magi Madoka Magica", updatedAt = 3)
    private val rebellion = entry(11981, "Madoka Magica Movie 3", score = 10.0, format = MediaFormat.MOVIE, updatedAt = 2)
    private val horimiya = entry(124080, "Horimiya", score = 8.0, genres = listOf("Romance", "Comedy"), updatedAt = 5)
    private val gal = entry(111323, "Gal to Kyouryuu", status = LibraryStatus.CURRENT, genres = listOf("Comedy"), updatedAt = 4)
    private val all = listOf(madoka, rebellion, horimiya, gal)

    private fun run(
        status: LibraryStatus? = null,
        tools: ProfileListTools = ProfileListTools(),
        viewer: Map<Int, LibraryEntry> = emptyMap()
    ) = applyListTools(all, status, tools, viewer, TitleLanguage.ROMAJI)

    @Test
    fun `the chip narrows to one status and counts what it holds`() {
        val result = run(status = LibraryStatus.COMPLETED)
        assertEquals(listOf(horimiya, madoka, rebellion), result.items)
        assertEquals(3, result.inScope)
    }

    @Test
    fun `a search looks through every status and every title language`() {
        val result = run(status = LibraryStatus.CURRENT, tools = ProfileListTools(query = "puella"))
        assertEquals(listOf(madoka), result.items)
    }

    @Test
    fun `on your list keeps only titles the viewer has`() {
        val result = run(
            tools = ProfileListTools(filters = ProfileListFilters(yourList = YourListFilter.ON_YOUR_LIST)),
            viewer = mapOf(horimiya.mediaId to horimiya)
        )
        assertEquals(listOf(horimiya), result.items)
    }

    @Test
    fun `genres must all match and formats any`() {
        val genres = run(tools = ProfileListTools(filters = ProfileListFilters(genres = setOf("Romance", "Comedy"))))
        assertEquals(listOf(horimiya), genres.items)
        val movies = run(tools = ProfileListTools(filters = ProfileListFilters(formats = setOf(MediaFormat.MOVIE))))
        assertEquals(listOf(rebellion), movies.items)
    }

    @Test
    fun `score floors read on a hundred point scale and unscored stands apart`() {
        val tens = run(tools = ProfileListTools(filters = ProfileListFilters(scoreFloor = 100)))
        assertEquals(setOf(madoka, rebellion), tens.items.toSet())
        val unscored = run(tools = ProfileListTools(filters = ProfileListFilters(scoreFloor = ProfileListFilters.UNSCORED)))
        assertEquals(listOf(gal), unscored.items)
    }

    @Test
    fun `sorting keeps titles without the value at the end in both directions`() {
        val down = run(tools = ProfileListTools(sort = ProfileListSort.THEIR_SCORE))
        assertEquals(gal, down.items.last())
        val up = run(tools = ProfileListTools(sort = ProfileListSort.THEIR_SCORE, ascending = true))
        assertEquals(horimiya, up.items.first())
        assertEquals(gal, up.items.last())
    }

    @Test
    fun `your score orders by the viewer entry`() {
        val viewer = mapOf(
            gal.mediaId to gal.copy(score = 9.0),
            rebellion.mediaId to rebellion.copy(score = 6.0)
        )
        val result = run(tools = ProfileListTools(sort = ProfileListSort.YOUR_SCORE), viewer = viewer)
        assertEquals(listOf(gal, rebellion), result.items.take(2))
    }

    @Test
    fun `floors follow the owner's scale`() {
        assertEquals(listOf(100, 90, 80, 70), scoreFloors(ScoreFormat.POINT_10))
        assertEquals(listOf(100, 80, 60), scoreFloors(ScoreFormat.POINT_5))
    }
}
