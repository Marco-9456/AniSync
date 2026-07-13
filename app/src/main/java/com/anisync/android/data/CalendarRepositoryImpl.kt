package com.anisync.android.data

import com.anisync.android.data.anisyncplus.AniSyncPlusSettings
import com.anisync.android.domain.AiringEpisode
import com.anisync.android.domain.CalendarRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.Result
import de.mrxxxxx.anisyncplus.calendar.api.EffectiveReleaseRepository
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseKind
import de.mrxxxxx.anisyncplus.calendar.domain.SOURCE_ZONE_ID
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Upstream compatibility adapter for the existing calendar presentation contract.
 *
 * The target calendar reads only the local AniWorld snapshot. It deliberately performs no network
 * request and has no AniList airing fallback; manual refresh is owned by the ViewModel coordinator.
 */
class CalendarRepositoryImpl @Inject constructor(
    private val effectiveReleaseRepository: EffectiveReleaseRepository,
    private val aniSyncPlusSettings: AniSyncPlusSettings
) : CalendarRepository {
    override suspend fun getWeekSchedule(
        weekStartEpochSec: Long,
        weekEndEpochSec: Long
    ): Result<List<AiringEpisode>> {
        if (!aniSyncPlusSettings.aniWorldCalendarEnabled.value) return Result.Success(emptyList())
        val zone = ZoneId.of(SOURCE_ZONE_ID)
        val startDate = Instant.ofEpochSecond(weekStartEpochSec).atZone(zone).toLocalDate()
        val endDateExclusive = Instant.ofEpochSecond(weekEndEpochSec).atZone(zone).toLocalDate()
        val snapshot = effectiveReleaseRepository.observeSnapshot().first()
            ?: return Result.Success(emptyList())
        if (endDateExclusive <= snapshot.rangeStart || startDate > snapshot.rangeEnd) {
            return Result.Success(emptyList())
        }
        return Result.Success(
            snapshot.releases.asSequence()
                .filter { it.sourceDate in snapshot.rangeStart..snapshot.rangeEnd }
                .filter { it.sourceDate >= startDate && it.sourceDate < endDateExclusive }
                .map { release ->
                    val listStatus = release.libraryStatus.toLibraryStatus()
                    val mediaId = release.aniListMediaId
                    AiringEpisode(
                        id = listOf(
                            snapshot.snapshotId,
                            release.sourceTitle,
                            release.sourceDate,
                            release.sourceLocalTime,
                            release.releaseKind,
                            release.episodeNumber,
                            release.language
                        ).hashCode(),
                        episode = release.episodeNumber ?: 0,
                        airingAt = release.instant?.epochSecond ?: 0L,
                        mediaId = mediaId ?: -1,
                        titleRomaji = null,
                        titleEnglish = null,
                        titleNative = null,
                        titleUserPreferred = release.aniListTitle ?: release.sourceTitle,
                        coverImageUrl = release.coverImageUrl,
                        format = release.releaseKind.name,
                        averageScore = release.averageScore,
                        isOnList = listStatus == LibraryStatus.CURRENT,
                        listStatus = listStatus,
                        isAdult = false,
                        sourceDateEpochDay = release.sourceDate.toEpochDay(),
                        sourceLocalTimeMinutes = release.sourceLocalTime?.let { it.hour * 60 + it.minute },
                        isApproximate = release.isApproximate,
                        releaseKind = release.releaseKind.name,
                        languageLabel = release.language.displayLabel(),
                        isDetailsAvailable = mediaId != null,
                        hasSourceTime = release.sourceLocalTime != null
                    )
                }
                .sortedWith(
                    compareBy<AiringEpisode> { it.sourceDateEpochDay }
                        .thenBy { it.sourceLocalTimeMinutes ?: Int.MAX_VALUE }
                        .thenBy { it.id }
                )
                .toList()
        )
    }

    private fun String?.toLibraryStatus(): LibraryStatus? = when (this) {
        "CURRENT" -> LibraryStatus.CURRENT
        "PLANNING" -> LibraryStatus.PLANNING
        "COMPLETED" -> LibraryStatus.COMPLETED
        "PAUSED" -> LibraryStatus.PAUSED
        "DROPPED" -> LibraryStatus.DROPPED
        "REPEATING" -> LibraryStatus.REPEATING
        else -> null
    }

    private fun de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage.displayLabel(): String =
        when (this) {
            de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage.DE_SUB -> "DE-Sub"
            de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage.DE_DUB -> "DE-Dub"
            de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage.DE_SUB_AND_DUB -> "DE-Sub / DE-Dub"
            else -> name
        }
}
