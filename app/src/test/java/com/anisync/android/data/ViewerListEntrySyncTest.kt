package com.anisync.android.data

import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.data.local.toDomain
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.fragment.ViewerListEntryFields
import com.anisync.android.type.MediaListStatus
import com.anisync.android.type.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ViewerListEntrySyncTest {

    private val askedAt = 1_800_000_000_000L
    private val earlier = askedAt - 60_000L

    private fun row(
        id: Int = 7,
        updatedAt: Long? = 1_790_000_000_000L,
        lastUpdated: Long = earlier,
    ) = LibraryEntryEntity(
        id = id,
        ownerId = 1,
        mediaId = 120377,
        titleRomaji = "Cyberpunk: Edgerunners",
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = "Cyberpunk: Edgerunners",
        coverUrl = null,
        progress = 3,
        totalEpisodes = 10,
        totalChapters = null,
        totalVolumes = null,
        mediaType = MediaType.ANIME,
        status = LibraryStatus.CURRENT,
        nextAiringEpisode = null,
        timeUntilAiring = null,
        mediaStatus = "FINISHED",
        updatedAt = updatedAt,
        lastUpdated = lastUpdated,
    )

    private fun entry(
        id: Int = 7,
        updatedAtSeconds: Int? = 1_790_000_100,
        status: MediaListStatus? = MediaListStatus.COMPLETED,
        progress: Int? = 10,
        customLists: Any? = mapOf("Favourites" to true, "Rewatch" to false),
    ) = ViewerListEntryFields(
        __typename = "MediaList",
        id = id,
        status = status,
        progress = progress,
        progressVolumes = null,
        score = 85.0,
        advancedScores = mapOf("Story" to 9, "Visuals" to 0, "Audio" to "8.5"),
        repeat = null,
        priority = null,
        notes = "Watched on the train",
        `private` = null,
        hiddenFromStatusLists = null,
        startedAt = ViewerListEntryFields.StartedAt("FuzzyDate", 2026, 10, 20),
        completedAt = ViewerListEntryFields.CompletedAt("FuzzyDate", 2026, 10, 25),
        customLists = customLists,
        updatedAt = updatedAtSeconds,
        createdAt = 1_789_000_000,
    )

    /** The row was the same when the request went out as when the answer came back. */
    private fun plan(local: LibraryEntryEntity?, server: ViewerListEntryFields?) =
        planListEntrySync(local, local, server, askedAt)

    @Test
    fun `no row and no entry leave nothing to do`() {
        assertEquals(ListEntrySync.Keep, plan(null, null))
    }

    @Test
    fun `an entry the library has no row for is taken`() {
        val server = entry()
        assertEquals(ListEntrySync.Take(server), plan(null, server))
    }

    @Test
    fun `an entry saved elsewhere since the last sync replaces the row`() {
        val server = entry(updatedAtSeconds = 1_790_000_100)
        assertEquals(ListEntrySync.Take(server), plan(row(updatedAt = 1_790_000_000_000L), server))
    }

    @Test
    fun `an entry no newer than the row leaves a change still waiting to be sent`() {
        val server = entry(updatedAtSeconds = 1_790_000_000)
        assertEquals(ListEntrySync.Keep, plan(row(updatedAt = 1_790_000_000_000L), server))
    }

    @Test
    fun `a row written while the answer was on its way stays`() {
        val server = entry(updatedAtSeconds = 1_799_000_000)
        assertEquals(ListEntrySync.Keep, planListEntrySync(row(), row(lastUpdated = askedAt), server, askedAt))
        assertEquals(ListEntrySync.Keep, planListEntrySync(row(), row(lastUpdated = askedAt + 1), null, askedAt))
    }

    @Test
    fun `a row removed while the answer was on its way stays removed`() {
        assertEquals(ListEntrySync.Keep, planListEntrySync(row(), null, entry(), askedAt))
    }

    @Test
    fun `a title taken off the list elsewhere drops its row`() {
        assertEquals(ListEntrySync.Drop, plan(row(), null))
    }

    @Test
    fun `a row kept under another entry id is replaced`() {
        val server = entry(id = 605969824, updatedAtSeconds = 1)
        assertEquals(ListEntrySync.Take(server), plan(row(id = 0), server))
    }

    @Test
    fun `a row without AniList's timestamp takes any entry`() {
        val server = entry()
        assertEquals(ListEntrySync.Take(server), plan(row(updatedAt = null), server))
    }

    @Test
    fun `every list field comes from the entry and the title's fields stay`() {
        val merged = row().toDomain().withListEntry(entry())

        assertEquals(7, merged.id)
        assertEquals(LibraryStatus.COMPLETED, merged.status)
        assertEquals(10, merged.progress)
        assertEquals(85.0, merged.score)
        assertEquals(mapOf("Story" to 9.0, "Audio" to 8.5), merged.advancedScores)
        assertEquals(0, merged.rewatches)
        assertEquals(0, merged.priority)
        assertEquals("Watched on the train", merged.notes)
        assertEquals(false, merged.isPrivate)
        assertEquals(false, merged.hiddenFromStatusLists)
        assertEquals(1_792_454_400_000L, merged.startedAt)
        assertEquals(1_792_886_400_000L, merged.completedAt)
        assertEquals(1_790_000_100_000L, merged.updatedAt)
        assertEquals(1_789_000_000_000L, merged.createdAt)
        assertEquals(listOf("Favourites"), merged.customLists)

        assertEquals("Cyberpunk: Edgerunners", merged.titleUserPreferred)
        assertEquals(10, merged.totalEpisodes)
        assertEquals(MediaType.ANIME, merged.type)
    }

    @Test
    fun `an empty date reads as no date`() {
        val merged = row().toDomain().withListEntry(
            entry().copy(completedAt = ViewerListEntryFields.CompletedAt("FuzzyDate", null, null, null))
        )
        assertNull(merged.completedAt)
    }

    @Test
    fun `custom lists only count the ones the entry is on`() {
        assertEquals(listOf("A", "C"), mapOf("A" to true, "B" to false, "C" to true).enabledListNames())
        assertEquals(emptyList<String>(), null.enabledListNames())
        assertEquals(emptyList<String>(), listOf("A").enabledListNames())
    }
}
