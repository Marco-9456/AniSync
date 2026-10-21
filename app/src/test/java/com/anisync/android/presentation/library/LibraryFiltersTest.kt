package com.anisync.android.presentation.library

import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Tag
import com.anisync.android.presentation.components.filtersheet.TriState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFiltersTest {

    private fun entry(mediaId: Int = 1, genres: List<String> = emptyList()) = LibraryEntry(
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
        status = LibraryStatus.PLANNING,
        genres = genres
    )

    private fun tag(
        name: String,
        rank: Int = 80,
        mediaSpoiler: Boolean = false,
        generalSpoiler: Boolean = false,
        adult: Boolean = false
    ) = Tag(
        name = name,
        category = "Theme",
        isMediaSpoiler = mediaSpoiler,
        isGeneralSpoiler = generalSpoiler,
        rank = rank,
        isAdult = adult
    )

    @Test
    fun `every included genre has to be there, not just one`() {
        val filters = LibraryFilters(genresIncluded = setOf("Action", "Comedy"))
        assertTrue(filters.matches(entry(genres = listOf("Action", "Comedy", "Drama")), emptyList()))
        assertFalse(filters.matches(entry(genres = listOf("Action", "Drama")), emptyList()))
    }

    @Test
    fun `an excluded genre rules the title out`() {
        val filters = LibraryFilters(genresExcluded = setOf("Romance"))
        assertFalse(filters.matches(entry(genres = listOf("Comedy", "Romance")), emptyList()))
        assertTrue(filters.matches(entry(genres = listOf("Comedy")), emptyList()))
    }

    @Test
    fun `included tags all have to match`() {
        val filters = LibraryFilters(tagsIncluded = setOf("Isekai", "Comedy"))
        assertTrue(filters.matches(entry(), listOf(tag("Isekai"), tag("Comedy"))))
        assertFalse(filters.matches(entry(), listOf(tag("Isekai"))))
    }

    @Test
    fun `a tag at or below rank 18 does not count either way`() {
        val include = LibraryFilters(tagsIncluded = setOf("Gambling"))
        val exclude = LibraryFilters(tagsExcluded = setOf("Gambling"))
        val faint = listOf(tag("Gambling", rank = 18))
        val present = listOf(tag("Gambling", rank = 19))

        assertFalse(include.matches(entry(), faint))
        assertTrue(exclude.matches(entry(), faint))
        assertTrue(include.matches(entry(), present))
        assertFalse(exclude.matches(entry(), present))
    }

    @Test
    fun `a spoiler tag still matches`() {
        val filters = LibraryFilters(tagsIncluded = setOf("Amnesia"))
        assertTrue(filters.matches(entry(), listOf(tag("Amnesia", mediaSpoiler = true))))
    }

    @Test
    fun `a title with no tags cached fails an included tag and passes an excluded one`() {
        assertFalse(LibraryFilters(tagsIncluded = setOf("Isekai")).matches(entry(), emptyList()))
        assertTrue(LibraryFilters(tagsExcluded = setOf("Isekai")).matches(entry(), emptyList()))
    }

    @Test
    fun `the active count includes and excludes genres and tags`() {
        val filters = LibraryFilters()
            .withGenre("Action", TriState.INCLUDED)
            .withGenre("Romance", TriState.EXCLUDED)
            .withTag("Isekai", TriState.INCLUDED)
            .withTag("Gore", TriState.EXCLUDED)
        assertEquals(4, filters.activeCount)
        assertFalse(filters.isEmpty)
    }

    @Test
    fun `moving a genre between states keeps it in one set at most`() {
        val included = LibraryFilters().withGenre("Action", TriState.INCLUDED)
        val excluded = included.withGenre("Action", TriState.EXCLUDED)
        val cleared = excluded.withGenre("Action", TriState.OFF)

        assertEquals(TriState.INCLUDED, included.genreState("Action"))
        assertEquals(TriState.EXCLUDED, excluded.genreState("Action"))
        assertEquals(emptySet<String>(), excluded.genresIncluded)
        assertTrue(cleared.isEmpty)
    }

    @Test
    fun `tag options only list tags that count somewhere`() {
        val options = libraryTagOptions(
            entries = listOf(entry(1), entry(2)),
            tagsByMedia = mapOf(
                1 to listOf(tag("Isekai"), tag("Gambling", rank = 13)),
                2 to listOf(tag("Isekai", rank = 60))
            )
        )
        assertEquals(listOf("Isekai"), options.map { it.name })
        assertEquals(2, options.single().titleCount)
    }

    @Test
    fun `a tag that spoils one title is a spoiler everywhere`() {
        val options = libraryTagOptions(
            entries = listOf(entry(1), entry(2), entry(3)),
            tagsByMedia = mapOf(
                1 to listOf(tag("Amnesia")),
                2 to listOf(tag("Amnesia", mediaSpoiler = true)),
                3 to listOf(tag("Time Skip", generalSpoiler = true), tag("Amnesia", rank = 10, mediaSpoiler = true))
            )
        ).associateBy { it.name }

        assertTrue(options.getValue("Amnesia").isSpoiler)
        assertTrue(options.getValue("Time Skip").isSpoiler)
    }

    @Test
    fun `a spoiler that does not count leaves the tag in the open`() {
        val options = libraryTagOptions(
            entries = listOf(entry(1), entry(2)),
            tagsByMedia = mapOf(
                1 to listOf(tag("Amnesia")),
                2 to listOf(tag("Amnesia", rank = 10, mediaSpoiler = true))
            )
        )
        assertFalse(options.single().isSpoiler)
    }

    @Test
    fun `titles the library does not hold are left out of the options`() {
        val options = libraryTagOptions(
            entries = listOf(entry(1)),
            tagsByMedia = mapOf(1 to listOf(tag("Isekai")), 2 to listOf(tag("Mecha")))
        )
        assertEquals(listOf("Isekai"), options.map { it.name })
    }
}
