package com.anisync.android.presentation.components

import com.anisync.android.domain.LibraryStatus
import com.anisync.android.ui.theme.ListIndicatorKind
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The indicator has one more state than [LibraryStatus] does: an entry can sit in custom lists
 * while carrying no standard status, and it still has to be marked as tracked.
 */
class ListIndicatorKindTest {

    @Test
    fun `each standard status maps to its own indicator`() {
        assertEquals(ListIndicatorKind.WATCHING, LibraryStatus.CURRENT.toIndicatorKind())
        assertEquals(ListIndicatorKind.REPEATING, LibraryStatus.REPEATING.toIndicatorKind())
        assertEquals(ListIndicatorKind.PLANNING, LibraryStatus.PLANNING.toIndicatorKind())
        assertEquals(ListIndicatorKind.PAUSED, LibraryStatus.PAUSED.toIndicatorKind())
        assertEquals(ListIndicatorKind.COMPLETED, LibraryStatus.COMPLETED.toIndicatorKind())
        assertEquals(ListIndicatorKind.DROPPED, LibraryStatus.DROPPED.toIndicatorKind())
    }

    @Test
    fun `an entry with no standard status falls back to the custom list indicator`() {
        assertEquals(ListIndicatorKind.CUSTOM, LibraryStatus.UNKNOWN.toIndicatorKind())
    }

    @Test
    fun `every status produces an indicator`() {
        val mapped = LibraryStatus.entries.map { it.toIndicatorKind() }
        assertEquals(LibraryStatus.entries.size, mapped.size)
        assertEquals(ListIndicatorKind.entries.toSet(), mapped.toSet())
    }
}
