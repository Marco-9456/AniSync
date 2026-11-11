package com.anisync.android.data

import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import com.apollographql.apollo.api.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListEntryEditsTest {

    private val opened = LibraryEntry(
        id = 7,
        mediaId = 21,
        titleRomaji = null,
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = "One Piece",
        coverUrl = null,
        progress = 3,
        totalEpisodes = 24,
        totalChapters = null,
        totalVolumes = null,
        type = MediaType.ANIME,
        status = LibraryStatus.CURRENT,
        score = 7.0,
        notes = "old note",
        customLists = listOf("Comfort")
    )

    @Test
    fun `a score edit sends the score alone`() {
        val edited = opened.copy(score = 9.0)

        val mutation = listEntryMutation(edited, ListEntryEdits.between(opened, edited), Optional.absent())

        assertEquals(Optional.present(9.0), mutation.score)
        assertEquals(Optional.Absent, mutation.progress)
        assertEquals(Optional.Absent, mutation.status)
        assertEquals(Optional.Absent, mutation.notes)
        assertEquals(Optional.Absent, mutation.customLists)
        assertEquals(Optional.Absent, mutation.startedAt)
    }

    @Test
    fun `an edit keeps what changed elsewhere since the editor opened`() {
        // AniList reported episode 5 while the sheet was still open on episode 3.
        val current = opened.copy(progress = 5)
        val edited = opened.copy(score = 9.0)

        val merged = ListEntryEdits.between(opened, edited).applyTo(current, edited)

        assertEquals(5, merged.progress)
        assertEquals(9.0, merged.score ?: 0.0, 0.0)
    }

    @Test
    fun `clearing a date sends an empty one`() {
        val dated = opened.copy(startedAt = 1_700_000_000_000L)
        val cleared = dated.copy(startedAt = null)

        val mutation = listEntryMutation(cleared, ListEntryEdits.between(dated, cleared), Optional.absent())

        val date = (mutation.startedAt as Optional.Present).value!!
        assertEquals(Optional.present(null), date.year)
        assertEquals(Optional.present(null), date.day)
    }

    @Test
    fun `clearing notes and score sends them empty`() {
        val cleared = opened.copy(notes = null, score = null)

        val mutation = listEntryMutation(cleared, ListEntryEdits.between(opened, cleared), Optional.absent())

        assertEquals(Optional.present(""), mutation.notes)
        assertEquals(Optional.present(0.0), mutation.score)
    }

    @Test
    fun `a new entry is sent whole`() {
        val mutation = listEntryMutation(opened, edits = null, advancedScores = Optional.absent())

        assertEquals(Optional.present(3), mutation.progress)
        assertEquals(Optional.present("old note"), mutation.notes)
        assertEquals(Optional.present(listOf("Comfort")), mutation.customLists)
        assertEquals(Optional.Absent, mutation.priority)
    }

    @Test
    fun `nothing changed means nothing to send`() {
        assertTrue(ListEntryEdits.between(opened, opened.copy()).isEmpty)
        assertTrue(ListEntryEdits.between(opened.copy(notes = null), opened.copy(notes = "")).isEmpty)
        assertTrue(ListEntryEdits.between(opened, opened.copy(customLists = listOf("Comfort"))).isEmpty)
    }
}
