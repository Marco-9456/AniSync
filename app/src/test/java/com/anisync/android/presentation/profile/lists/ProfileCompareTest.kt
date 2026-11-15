package com.anisync.android.presentation.profile.lists

import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.type.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileCompareTest {

    private fun entry(
        mediaId: Int,
        score: Double = 0.0,
        format: ScoreFormat? = ScoreFormat.POINT_10,
        type: MediaType? = MediaType.ANIME
    ) = LibraryEntry(
        id = mediaId,
        mediaId = mediaId,
        titleRomaji = "Title $mediaId",
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = "Title $mediaId",
        coverUrl = null,
        progress = 0,
        totalEpisodes = 12,
        totalChapters = null,
        totalVolumes = null,
        type = type,
        format = null,
        status = LibraryStatus.COMPLETED,
        score = score,
        scoreFormat = format
    )

    private val theirs = listOf(
        entry(1, score = 10.0),
        entry(2, score = 8.0),
        entry(3),
        entry(4, score = 10.0),
        entry(5, score = 6.0)
    )

    @Test
    fun `shared titles split by whether both people scored them`() {
        // The viewer's Library rows carry no format, so their 80 out of 100 is read in the viewer's format.
        val yours = listOf(entry(1, score = 80.0, format = null), entry(3, score = 70.0, format = null), entry(9, format = null))
        val result = compareLists(theirs, yours, MediaType.ANIME, ScoreFormat.POINT_100)
        assertEquals(listOf(1), result.bothScored.map { it.theirs.mediaId })
        assertEquals(listOf(3), result.alsoShared.map { it.theirs.mediaId })
        assertEquals(2, result.sharedCount)
        assertEquals(3, result.theirsNotYours)
        assertEquals(listOf(9), result.onlyYours.map { it.mediaId })
    }

    @Test
    fun `their top scores you have not added, and the means on one scale`() {
        val yours = listOf(entry(2, score = 8.0, format = null))
        val result = compareLists(theirs, yours, MediaType.ANIME, ScoreFormat.POINT_10)
        assertEquals(listOf(1, 4), result.theirTensNotYours.map { it.mediaId })
        assertEquals(80.0, result.yourMean!!, 0.001)
        assertEquals(85.0, result.theirMean!!, 0.001)
        assertEquals(2, result.sameScore?.theirs?.mediaId)
    }

    @Test
    fun `other media types and an empty list leave nothing to compare`() {
        val manga = listOf(entry(1, score = 9.0, type = MediaType.MANGA))
        val result = compareLists(theirs, manga, MediaType.ANIME, ScoreFormat.POINT_10)
        assertEquals(0, result.yourCount)
        assertEquals(0, result.sharedCount)
        assertNull(result.yourMean)
    }
}
