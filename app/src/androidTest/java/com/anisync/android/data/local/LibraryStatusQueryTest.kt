package com.anisync.android.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the query behind the personal list indicators.
 *
 * The two things that would break the feature quietly are leaking another account's entries into
 * the map, and dropping manga because the caller only asked for one media type.
 */
@RunWith(AndroidJUnit4::class)
class LibraryStatusQueryTest {

    private lateinit var database: AppDatabase
    private val dao get() = database.libraryDao()

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun returnsOneStatusPerMediaAcrossBothTypes() = runBlocking {
        dao.insertAll(
            listOf(
                entry(id = 1, ownerId = 7, mediaId = 100, type = MediaType.ANIME, status = LibraryStatus.CURRENT),
                entry(id = 2, ownerId = 7, mediaId = 200, type = MediaType.MANGA, status = LibraryStatus.COMPLETED)
            )
        )

        val statuses = dao.observeListStatuses(ownerId = 7).first().associate { it.mediaId to it.status }

        assertEquals(2, statuses.size)
        assertEquals(LibraryStatus.CURRENT, statuses[100])
        assertEquals(LibraryStatus.COMPLETED, statuses[200])
    }

    @Test
    fun ignoresAnotherAccountsEntries() = runBlocking {
        dao.insertAll(
            listOf(
                entry(id = 1, ownerId = 7, mediaId = 100, type = MediaType.ANIME, status = LibraryStatus.CURRENT),
                entry(id = 2, ownerId = 8, mediaId = 300, type = MediaType.ANIME, status = LibraryStatus.DROPPED)
            )
        )

        val statuses = dao.observeListStatuses(ownerId = 7).first().associate { it.mediaId to it.status }

        assertEquals(1, statuses.size)
        assertNull(statuses[300])
    }

    @Test
    fun emitsAgainWhenAnEntryChangesList() = runBlocking {
        dao.insertAll(
            listOf(entry(id = 1, ownerId = 7, mediaId = 100, type = MediaType.ANIME, status = LibraryStatus.PLANNING))
        )
        assertEquals(
            LibraryStatus.PLANNING,
            dao.observeListStatuses(ownerId = 7).first().single().status
        )

        dao.insertOrReplace(
            entry(id = 1, ownerId = 7, mediaId = 100, type = MediaType.ANIME, status = LibraryStatus.CURRENT)
        )

        assertEquals(
            LibraryStatus.CURRENT,
            dao.observeListStatuses(ownerId = 7).first().single().status
        )
    }

    private fun entry(
        id: Int,
        ownerId: Int,
        mediaId: Int,
        type: MediaType,
        status: LibraryStatus
    ) = LibraryEntryEntity(
        id = id,
        ownerId = ownerId,
        mediaId = mediaId,
        titleRomaji = null,
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = "Title $mediaId",
        coverUrl = null,
        progress = 0,
        totalEpisodes = null,
        totalChapters = null,
        totalVolumes = null,
        mediaType = type,
        status = status,
        nextAiringEpisode = null,
        timeUntilAiring = null,
        mediaStatus = null
    )
}
