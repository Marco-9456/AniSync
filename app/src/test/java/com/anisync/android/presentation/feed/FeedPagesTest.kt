package com.anisync.android.presentation.feed

import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedPagesTest {

    private fun status(id: Int, likeCount: Int = 0) = UserActivity(
        id = id,
        type = ActivityType.TEXT,
        text = "hello",
        likeCount = likeCount,
        timestamp = id * 60_000L
    )

    private fun feed(vararg ids: Int) = ids.map { status(it) }

    private fun List<UserActivity>?.ids() = this?.map { it.id }

    @Test
    fun `new activity goes on top and the pages below stay`() {
        val merged = mergeFirstPage(
            loaded = feed(10, 9, 8, 7, 6, 5, 4, 3),
            firstPage = feed(12, 11, 10, 9)
        )

        assertEquals(listOf(12, 11, 10, 9, 8, 7, 6, 5, 4, 3), merged.ids())
    }

    @Test
    fun `the page's copy of an activity replaces the loaded one`() {
        val merged = mergeFirstPage(
            loaded = listOf(status(3), status(2, likeCount = 1), status(1)),
            firstPage = listOf(status(4), status(3), status(2, likeCount = 5))
        )

        assertEquals(listOf(4, 3, 2, 1), merged.ids())
        assertEquals(5, merged?.single { it.id == 2 }?.likeCount)
    }

    @Test
    fun `activity missing from inside the page is dropped`() {
        val merged = mergeFirstPage(
            loaded = feed(5, 4, 3, 2, 1),
            firstPage = feed(6, 5, 3, 2)
        )

        assertEquals(listOf(6, 5, 3, 2, 1), merged.ids())
    }

    @Test
    fun `a page that shares nothing with the feed cannot join it`() {
        assertNull(mergeFirstPage(loaded = feed(5, 4, 3), firstPage = feed(9, 8, 7, 6)))
    }

    @Test
    fun `an activity the page carries higher up is not listed twice`() {
        val merged = mergeFirstPage(
            loaded = feed(5, 4, 3, 2, 1),
            firstPage = feed(2, 6, 5, 4)
        )

        assertEquals(listOf(2, 6, 5, 4, 3, 1), merged.ids())
    }

    @Test
    fun `nothing loaded takes the page as it is`() {
        assertEquals(listOf(3, 2, 1), mergeFirstPage(loaded = emptyList(), firstPage = feed(3, 2, 1)).ids())
    }

    @Test
    fun `an empty page leaves the feed alone`() {
        assertEquals(listOf(3, 2, 1), mergeFirstPage(loaded = feed(3, 2, 1), firstPage = emptyList()).ids())
    }
}
