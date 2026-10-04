package com.anisync.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreOnCompleteTest {

    private fun entry(status: LibraryStatus, score: Double? = null) = LibraryEntry(
        id = 1,
        mediaId = 1,
        titleRomaji = null,
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = "Mushishi",
        coverUrl = null,
        progress = 26,
        totalEpisodes = 26,
        totalChapters = null,
        totalVolumes = null,
        type = null,
        status = status,
        score = score
    )

    @Test
    fun `finishing an unscored entry asks`() {
        assertTrue(shouldAskForScore(LibraryStatus.CURRENT, entry(LibraryStatus.COMPLETED)))
        assertTrue(shouldAskForScore(LibraryStatus.REPEATING, entry(LibraryStatus.COMPLETED, 0.0)))
    }

    @Test
    fun `finishing something only planned asks`() {
        assertTrue(shouldAskForScore(LibraryStatus.PLANNING, entry(LibraryStatus.COMPLETED)))
    }

    @Test
    fun `adding a new title straight to Completed does not ask`() {
        assertFalse(shouldAskForScore(null, entry(LibraryStatus.COMPLETED)))
    }

    @Test
    fun `an entry that already has a score does not ask`() {
        assertFalse(shouldAskForScore(LibraryStatus.CURRENT, entry(LibraryStatus.COMPLETED, 8.0)))
    }

    @Test
    fun `an entry that was already Completed does not ask again`() {
        assertFalse(shouldAskForScore(LibraryStatus.COMPLETED, entry(LibraryStatus.COMPLETED)))
    }

    @Test
    fun `an entry that did not end up Completed does not ask`() {
        assertFalse(shouldAskForScore(LibraryStatus.CURRENT, entry(LibraryStatus.CURRENT)))
        assertFalse(shouldAskForScore(LibraryStatus.CURRENT, null))
    }
    @Test
    fun `a note saved without a score keeps the score unset`() {
        val saved = entry(LibraryStatus.COMPLETED).withCompletionScore(0.0, emptyMap(), "Rewatch the finale")
        assertEquals(null, saved.score)
        assertEquals("Rewatch the finale", saved.notes)
    }

    @Test
    fun `a score saved with a blank note clears the note`() {
        val saved = entry(LibraryStatus.COMPLETED).copy(notes = "old").withCompletionScore(8.0, emptyMap(), "  ")
        assertEquals(8.0, saved.score)
        assertEquals(null, saved.notes)
    }
}
