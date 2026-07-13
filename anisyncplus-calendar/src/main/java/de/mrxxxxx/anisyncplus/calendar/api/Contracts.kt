package de.mrxxxxx.anisyncplus.calendar.api

import de.mrxxxxx.anisyncplus.calendar.domain.AniListMatchCandidate
import de.mrxxxxx.anisyncplus.calendar.domain.AniListUserMediaState
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldParsedDocument
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldRelease
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldSyncState
import de.mrxxxxx.anisyncplus.calendar.domain.EffectiveCalendarSnapshot
import de.mrxxxxx.anisyncplus.calendar.domain.EffectiveRelease
import de.mrxxxxx.anisyncplus.calendar.domain.TitleMatchDecision
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface AniWorldCalendarParser {
    fun parse(html: String, fetchedAt: Instant): AniWorldParsedDocument
}

interface AniWorldCalendarClient {
    suspend fun fetch(): AniWorldCalendarResponse
}

data class AniWorldCalendarResponse(
    val html: String,
    val fetchedAt: Instant,
    val httpStatus: Int
)

sealed interface AniWorldRefreshResult {
    data class Success(val snapshotId: String, val visibleGermanCount: Int) : AniWorldRefreshResult
    data class Failure(val errorType: String, val message: String, val httpStatus: Int? = null) : AniWorldRefreshResult
}

interface AniWorldCalendarRepository {
    fun observeSnapshot(): Flow<EffectiveCalendarSnapshot?>
    fun observeSyncState(): Flow<AniWorldSyncState>
    suspend fun refresh(): AniWorldRefreshResult
}

interface AniWorldRefreshCoordinator {
    suspend fun refresh(): AniWorldRefreshResult
}

interface AniWorldTitleMatcher {
    fun match(release: AniWorldRelease, candidates: List<AniListMatchCandidate>): TitleMatchDecision
}

interface AniWorldMatchCandidateProvider {
    suspend fun candidatesFor(rawTitle: String): List<AniListMatchCandidate>
}

interface AniListLibraryStateProvider {
    fun observeStates(): Flow<Map<Int, AniListUserMediaState>>
}

interface EffectiveReleaseRepository {
    fun observeSnapshot(): Flow<EffectiveCalendarSnapshot?>
    fun observeNextGermanReleases(mediaIds: Set<Int>): Flow<Map<Int, EffectiveRelease>>
    fun observeLatestReleasedGermanEpisodes(
        mediaIds: Set<Int>,
        now: Instant
    ): Flow<Map<Int, Int>>
}
