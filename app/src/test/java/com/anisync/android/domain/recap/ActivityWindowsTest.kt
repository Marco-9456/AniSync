package com.anisync.android.domain.recap

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityWindowsTest {

    /** Serves activities the way AniList does: newest first, 50 a page, nothing past page 100. */
    private class FakeFeed(private val activities: List<FetchedActivity>) {
        var requests = 0
        var deepestPage = 0

        suspend fun page(window: ActivityWindow, page: Int): FetchedPage {
            requests++
            deepestPage = maxOf(deepestPage, page)
            check(page <= MAX_ACTIVITY_PAGES) { "Page depth exceeds maximum allowed" }
            val inWindow = activities
                .filter { it.createdAt > window.createdAfter && it.createdAt < window.createdBefore }
                .sortedByDescending { it.id }
            val start = (page - 1) * ACTIVITY_PAGE_SIZE
            val slice = inWindow.drop(start).take(ACTIVITY_PAGE_SIZE)
            return FetchedPage(slice, hasNextPage = start + slice.size < inWindow.size)
        }
    }

    private fun feed(count: Int, secondsApart: Long = 60, startAt: Long = 1_000_000): List<FetchedActivity> =
        (1..count).map { index ->
            FetchedActivity(index, mediaId = index % 7, status = "watched episode", progress = "1", createdAt = startAt + index * secondsApart)
        }

    private fun windowOf(activities: List<FetchedActivity>) =
        ActivityWindow(activities.minOf { it.createdAt }, activities.maxOf { it.createdAt } + 1)

    @Test
    fun `a window under the depth limit is read page by page`() = runTest {
        val activities = feed(120)
        val server = FakeFeed(activities)
        val read = readActivityWindow(windowOf(activities), server::page)
        assertEquals(120, read.size)
        assertEquals(3, server.requests)
    }

    @Test
    fun `a window of exactly 5000 needs no narrowing`() = runTest {
        val activities = feed(5000)
        val server = FakeFeed(activities)
        val read = readActivityWindow(windowOf(activities), server::page)
        assertEquals(5000, read.size)
        assertEquals(MAX_ACTIVITY_PAGES, server.requests)
    }

    @Test
    fun `past page 100 the rest is read as a narrower window`() = runTest {
        val activities = feed(12_345)
        val server = FakeFeed(activities)
        val read = readActivityWindow(windowOf(activities), server::page)
        assertEquals(activities.map { it.id }.toSet(), read.map { it.id }.toSet())
        assertEquals(12_345, read.size)
        assertTrue(server.deepestPage <= MAX_ACTIVITY_PAGES)
    }

    @Test
    fun `activities from the second of the cut are kept once`() = runTest {
        // Ten activities a second, so the cut at page 100 falls inside a second that spans two passes.
        val activities = (1..5_200).map { FetchedActivity(it, 1, "watched episode", "1", 2_000_000L + it / 10) }
        val server = FakeFeed(activities)
        val read = readActivityWindow(windowOf(activities), server::page)
        assertEquals(5_200, read.size)
        assertEquals(5_200, read.map { it.id }.toSet().size)
    }

    @Test
    fun `a second holding more than the limit stops instead of looping`() = runTest {
        val activities = (1..5_100).map { FetchedActivity(it, 1, "watched episode", "1", 3_000_000L) }
        val server = FakeFeed(activities)
        val read = readActivityWindow(windowOf(activities), server::page)
        assertEquals(5_000, read.size)
        assertEquals(MAX_ACTIVITY_PAGES, server.requests)
    }

    @Test
    fun `an empty window costs one request`() = runTest {
        val server = FakeFeed(emptyList())
        assertTrue(readActivityWindow(ActivityWindow(0, 100), server::page).isEmpty())
        assertEquals(1, server.requests)
    }
}
