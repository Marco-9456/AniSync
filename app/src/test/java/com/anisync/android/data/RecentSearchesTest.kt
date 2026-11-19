package com.anisync.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RecentSearchesTest {

    @Test
    fun `a new search goes to the front`() {
        assertEquals(listOf("frieren", "one piece"), listOf("one piece").withRecentSearch("frieren"))
    }

    @Test
    fun `searching again moves it up instead of adding a copy`() {
        val recent = listOf("one piece", "frieren", "bocchi")
        assertEquals(listOf("frieren", "one piece", "bocchi"), recent.withRecentSearch("frieren"))
    }

    @Test
    fun `a copy in other letter case is replaced by the latest spelling`() {
        val recent = listOf("one piece", "frieren")
        assertEquals(listOf("Frieren", "one piece"), recent.withRecentSearch("Frieren"))
    }

    @Test
    fun `spaces are trimmed and runs of them collapse`() {
        assertEquals(listOf("kimetsu no yaiba"), emptyList<String>().withRecentSearch("  kimetsu   no\tyaiba "))
        assertEquals(listOf("kimetsu no yaiba"), listOf("kimetsu no yaiba").withRecentSearch("kimetsu  no yaiba"))
    }

    @Test
    fun `a blank search changes nothing`() {
        val recent = listOf("one piece")
        assertSame(recent, recent.withRecentSearch("   "))
        assertSame(recent, recent.withRecentSearch(""))
    }

    @Test
    fun `a single letter is too short to keep`() {
        val recent = listOf("one piece")
        assertSame(recent, recent.withRecentSearch(" k "))
        assertEquals(listOf("k-on", "one piece"), recent.withRecentSearch("k-on"))
    }

    @Test
    fun `the oldest search leaves once the list is full`() {
        val full = (1..RECENT_SEARCH_LIMIT).map { "query $it" }
        val updated = full.withRecentSearch("newest")
        assertEquals(RECENT_SEARCH_LIMIT, updated.size)
        assertEquals("newest", updated.first())
        assertEquals("query ${RECENT_SEARCH_LIMIT - 1}", updated.last())
    }

    @Test
    fun `moving a search up in a full list keeps every other one`() {
        val full = (1..RECENT_SEARCH_LIMIT).map { "query $it" }
        val updated = full.withRecentSearch("query $RECENT_SEARCH_LIMIT")
        assertEquals(listOf("query $RECENT_SEARCH_LIMIT") + full.dropLast(1), updated)
    }
}
