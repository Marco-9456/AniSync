package com.anisync.android.data

import android.content.Context
import com.anisync.android.data.anisyncplus.AniSyncPlusSettings
import com.anisync.android.domain.Result
import de.mrxxxxx.anisyncplus.calendar.api.EffectiveReleaseRepository
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage
import de.mrxxxxx.anisyncplus.calendar.domain.EffectiveCalendarSnapshot
import de.mrxxxxx.anisyncplus.calendar.domain.EffectiveRelease
import de.mrxxxxx.anisyncplus.calendar.domain.MatchStatus
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class CalendarRepositoryImplTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val settings = AniSyncPlusSettings(context)
    private val effectiveRepository = FakeEffectiveReleaseRepository()
    private val repository = CalendarRepositoryImpl(effectiveRepository, settings)
    private val zone = ZoneId.of("Europe/Berlin")

    @After
    fun clearPreferences() {
        context.getSharedPreferences(AniSyncPlusSettings.PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun absentSnapshotReturnsEmptyWithoutAniListFallback() = runTest {
        val result = repository.getWeekSchedule(epoch(LocalDate.of(2026, 7, 13)), epoch(LocalDate.of(2026, 7, 20)))

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun partialOverlapUsesOnlyAniWorldSourceDaysAndCurrentMarksFollowing() = runTest {
        effectiveRepository.snapshot.value = EffectiveCalendarSnapshot(
            snapshotId = "snapshot",
            fetchedAt = Instant.parse("2026-07-13T10:00:00Z"),
            rangeStart = LocalDate.of(2026, 7, 15),
            rangeEnd = LocalDate.of(2026, 7, 20),
            releases = listOf(
                release(LocalDate.of(2026, 7, 14), 1, "CURRENT"),
                release(LocalDate.of(2026, 7, 15), 2, "CURRENT"),
                release(LocalDate.of(2026, 7, 16), 3, "PLANNING"),
                release(LocalDate.of(2026, 7, 17), null, null)
            )
        )

        val result = repository.getWeekSchedule(epoch(LocalDate.of(2026, 7, 13)), epoch(LocalDate.of(2026, 7, 20)))
        val episodes = (result as Result.Success).data

        assertEquals(listOf(2, 3, 0), episodes.map { it.episode })
        assertTrue(episodes.first().isOnList)
        assertFalse(episodes[1].isOnList)
        assertFalse(episodes[2].isOnList)
        assertFalse(episodes[2].isDetailsAvailable)
        assertEquals(LocalDate.of(2026, 7, 15).toEpochDay(), episodes.first().sourceDateEpochDay)
    }

    @Test
    fun disabledCalendarReturnsEmptyEvenWithCachedSnapshot() = runTest {
        effectiveRepository.snapshot.value = EffectiveCalendarSnapshot(
            snapshotId = "snapshot",
            fetchedAt = Instant.EPOCH,
            rangeStart = LocalDate.of(2026, 7, 13),
            rangeEnd = LocalDate.of(2026, 7, 19),
            releases = listOf(release(LocalDate.of(2026, 7, 13), 1, "CURRENT"))
        )
        settings.setAniWorldCalendarEnabled(false)

        val result = repository.getWeekSchedule(epoch(LocalDate.of(2026, 7, 13)), epoch(LocalDate.of(2026, 7, 20)))

        assertTrue((result as Result.Success).data.isEmpty())
    }

    private fun epoch(date: LocalDate): Long = date.atStartOfDay(zone).toEpochSecond()

    private fun release(date: LocalDate, episode: Int?, libraryStatus: String?) = EffectiveRelease(
        aniListMediaId = episode?.plus(100),
        sourceTitle = "Source ${episode ?: "unknown"}",
        aniListTitle = episode?.let { "Matched $it" },
        coverImageUrl = episode?.let { "https://example.invalid/$it.jpg" },
        averageScore = null,
        releaseKind = ReleaseKind.EPISODE,
        seasonNumber = 1,
        episodeNumber = episode,
        installmentNumber = null,
        instant = date.atTime(20, 15).atZone(zone).toInstant(),
        sourceDate = date,
        sourceLocalTime = LocalTime.of(20, 15),
        isApproximate = false,
        language = AniWorldReleaseLanguage.DE_SUB,
        matchStatus = if (episode == null) MatchStatus.UNMATCHED else MatchStatus.MATCHED,
        libraryStatus = libraryStatus
    )

    private class FakeEffectiveReleaseRepository : EffectiveReleaseRepository {
        val snapshot = MutableStateFlow<EffectiveCalendarSnapshot?>(null)

        override fun observeSnapshot(): Flow<EffectiveCalendarSnapshot?> = snapshot

        override fun observeNextGermanReleases(mediaIds: Set<Int>): Flow<Map<Int, EffectiveRelease>> =
            flowOf(emptyMap())

        override fun observeLatestReleasedGermanEpisodes(
            mediaIds: Set<Int>,
            now: Instant
        ): Flow<Map<Int, Int>> = flowOf(emptyMap())
    }
}
