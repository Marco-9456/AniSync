package com.anisync.android.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackTrailTest {

    private val graph = TrailNode("graph", key = null)
    private val library = TrailNode("library", key = "library", isTab = true)
    private val discover = TrailNode("discover", key = "discover", isTab = true)

    private fun page(entryId: String, key: String = entryId) = TrailNode(entryId, key)

    private fun BackTrail.pageIds() = pages.map { it.entryId }

    @Test
    fun `one page behind keeps the plain back`() {
        assertNull(backTrailOf(listOf(graph, library, page("a"))))
    }

    @Test
    fun `two pages behind list the page and the tab`() {
        val trail = backTrailOf(listOf(graph, library, page("a"), page("b")))!!

        assertEquals(listOf("a"), trail.pageIds())
        assertEquals(library, trail.tab)
    }

    @Test
    fun `pages run newest first`() {
        val trail = backTrailOf(
            listOf(graph, discover, page("a"), page("b"), page("c"), page("d"))
        )!!

        assertEquals(listOf("c", "b", "a"), trail.pageIds())
        assertEquals(4, trail.rowCount)
    }

    @Test
    fun `the trail stops at the nearest tab`() {
        // A tab switch from Library to Discover leaves Library underneath.
        val trail = backTrailOf(listOf(graph, library, discover, page("a"), page("b")))!!

        assertEquals(listOf("a"), trail.pageIds())
        assertEquals(discover, trail.tab)
    }

    @Test
    fun `a page visited twice is listed once at its latest position`() {
        val trail = backTrailOf(
            listOf(
                graph,
                library,
                page("first", key = "media:1"),
                page("character", key = "character:5"),
                page("again", key = "media:1"),
                page("staff", key = "staff:7"),
                page("current", key = "media:2"),
            )
        )!!

        assertEquals(listOf("staff", "again", "character"), trail.pageIds())
    }

    @Test
    fun `an earlier visit to the current page is left out`() {
        val trail = backTrailOf(
            listOf(
                graph,
                library,
                page("earlier", key = "media:1"),
                page("between", key = "media:2"),
                page("current", key = "media:1"),
            )
        )!!

        assertEquals(listOf("between"), trail.pageIds())
    }

    @Test
    fun `a trail that dedupes down to one row keeps the plain back`() {
        val stack = listOf(
            graph,
            library,
            page("earlier", key = "media:1"),
            page("current", key = "media:1"),
        )

        assertNull(backTrailOf(stack))
    }

    @Test
    fun `entries that are not pages are skipped`() {
        val trail = backTrailOf(
            listOf(library, page("a"), TrailNode("editor", key = null), page("b"), page("c"))
        )!!

        assertEquals(listOf("b", "a"), trail.pageIds())
    }

    @Test
    fun `a pane ends on its list`() {
        val trail = backTrailOf(listOf(graph, page("root"), page("b")), endsOnList = true)!!

        assertEquals(listOf("root"), trail.pageIds())
        assertNull(trail.tab)
        assertEquals(2, trail.rowCount)
    }

    @Test
    fun `a pane root alone keeps the plain back`() {
        assertNull(backTrailOf(listOf(graph, page("root")), endsOnList = true))
    }

    @Test
    fun `a tab never shows a trail`() {
        assertNull(backTrailOf(listOf(graph, library, discover)))
    }

    @Test
    fun `an empty stack shows no trail`() {
        assertNull(backTrailOf(emptyList()))
    }

    @Test
    fun `popping the top is a step back`() {
        assertTrue(wentBack(listOf("graph", "library", "a", "b"), listOf("graph", "library", "a")))
    }

    @Test
    fun `a jump down the trail is a step back`() {
        assertTrue(wentBack(listOf("graph", "library", "a", "b", "c"), listOf("graph", "library")))
    }

    @Test
    fun `opening a page is no step back`() {
        assertFalse(wentBack(listOf("graph", "library"), listOf("graph", "library", "a")))
    }

    @Test
    fun `a tab switch that drops pages is no step back`() {
        assertFalse(wentBack(listOf("graph", "library", "a", "b"), listOf("graph", "library", "discover")))
    }

    @Test
    fun `two backs inside the window make a streak`() {
        val streak = BackStreak(windowMillis = 3_000)

        assertFalse(streak.next(atMillis = 10_000))
        assertTrue(streak.next(atMillis = 12_500))
    }

    @Test
    fun `backs further apart than the window make no streak`() {
        val streak = BackStreak(windowMillis = 3_000)

        assertFalse(streak.next(atMillis = 10_000))
        assertFalse(streak.next(atMillis = 13_500))
        assertTrue(streak.next(atMillis = 14_000))
    }
}
