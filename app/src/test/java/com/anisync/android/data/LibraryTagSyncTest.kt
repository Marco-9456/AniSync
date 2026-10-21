package com.anisync.android.data

import com.anisync.android.fragment.LibraryMediaTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LibraryTagSyncTest {

    private val now = 1_800_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun `an empty library asks for nothing`() {
        assertEquals(TagSyncPlan.UpToDate, planTagSync(emptyList(), emptyMap(), now))
    }

    @Test
    fun `titles fetched this week are left alone`() {
        val fetched = mapOf(1 to now - day, 2 to now - 6 * day)
        assertEquals(TagSyncPlan.UpToDate, planTagSync(listOf(1, 2), fetched, now))
    }

    @Test
    fun `a week to the millisecond is not yet due`() {
        assertEquals(TagSyncPlan.UpToDate, planTagSync(listOf(1), mapOf(1 to now - 7 * day), now))
        assertEquals(
            TagSyncPlan.Titles(listOf(1)),
            planTagSync(listOf(1), mapOf(1 to now - 7 * day - 1), now)
        )
    }

    @Test
    fun `a few new or stale titles go out as one page`() {
        val fetched = mapOf(1 to now - day, 2 to now - 30 * day)
        assertEquals(TagSyncPlan.Titles(listOf(2, 3)), planTagSync(listOf(1, 2, 3), fetched, now))
    }

    @Test
    fun `a full page still fits one request`() {
        val ids = (1..TAGS_PAGE_SIZE).toList()
        assertEquals(TagSyncPlan.Titles(ids), planTagSync(ids, emptyMap(), now))
    }

    @Test
    fun `more than a page brings the whole list down instead`() {
        val ids = (1..TAGS_PAGE_SIZE + 1).toList()
        assertEquals(TagSyncPlan.WholeList, planTagSync(ids, emptyMap(), now))
    }

    @Test
    fun `duplicate and placeholder media ids are not fetched`() {
        assertEquals(TagSyncPlan.Titles(listOf(5)), planTagSync(listOf(0, 5, 5), emptyMap(), now))
    }

    @Test
    fun `missing flags and category read as false and blank`() {
        val tags = LibraryMediaTags(
            __typename = "Media",
            id = 1,
            tags = listOf(
                LibraryMediaTags.Tag(
                    __typename = "MediaTag",
                    name = "Isekai",
                    category = null,
                    rank = 91,
                    isAdult = null,
                    isMediaSpoiler = null,
                    isGeneralSpoiler = true
                ),
                null
            )
        ).toTags()

        assertEquals(1, tags.size)
        val tag = tags.single()
        assertEquals("Isekai", tag.name)
        assertEquals("", tag.category)
        assertEquals(91, tag.rank)
        assertFalse(tag.isAdult)
        assertFalse(tag.isMediaSpoiler)
        assertEquals(true, tag.isGeneralSpoiler)
    }
}
