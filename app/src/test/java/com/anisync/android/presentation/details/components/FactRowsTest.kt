package com.anisync.android.presentation.details.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FactRowsTest {

    private fun rows(vararg items: String) = factRows(items.toList()) { it == "Aired" }

    @Test
    fun `facts pair up in order and an odd one closes the grid alone`() {
        assertEquals(
            listOf(listOf("Status", "Episodes"), listOf("Duration", "Season"), listOf("Source")),
            rows("Status", "Episodes", "Duration", "Season", "Source")
        )
    }

    @Test
    fun `a wide fact at the end of a pair takes the next row where it stands`() {
        assertEquals(
            listOf(
                listOf("Status", "Episodes"),
                listOf("Duration", "Season"),
                listOf("Aired"),
                listOf("Source", "Studio")
            ),
            rows("Status", "Episodes", "Duration", "Season", "Aired", "Source", "Studio")
        )
    }

    @Test
    fun `a wide fact in the middle of a pair waits until the pair is full`() {
        assertEquals(
            listOf(listOf("Status", "Chapters"), listOf("Season", "Source"), listOf("Aired")),
            rows("Status", "Chapters", "Season", "Aired", "Source")
        )
    }

    @Test
    fun `a wide fact still shows when nothing comes to fill the pair`() {
        assertEquals(
            listOf(listOf("Status", "Chapters"), listOf("Season"), listOf("Aired")),
            rows("Status", "Chapters", "Season", "Aired")
        )
    }
}
