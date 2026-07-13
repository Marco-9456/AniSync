package de.mrxxxxx.anisyncplus.calendar.repository

import de.mrxxxxx.anisyncplus.calendar.api.*
import de.mrxxxxx.anisyncplus.calendar.domain.*
import de.mrxxxxx.anisyncplus.calendar.local.*
import de.mrxxxxx.anisyncplus.calendar.network.AniWorldClientException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class RoomAniWorldCalendarRepository @Inject constructor(
    private val dao: AniWorldCalendarDao,
    private val client: AniWorldCalendarClient,
    private val parser: AniWorldCalendarParser,
    private val matcher: AniWorldTitleMatcher,
    private val candidateProvider: AniWorldMatchCandidateProvider,
    private val libraryStateProvider: AniListLibraryStateProvider,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val parserDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AniWorldCalendarRepository, AniWorldRefreshCoordinator, EffectiveReleaseRepository {

    override fun observeSnapshot(): Flow<EffectiveCalendarSnapshot?> = combine(
        dao.observeActiveSnapshot(),
        dao.observeMappings(),
        libraryStateProvider.observeStates()
    ) { stored, mappings, libraryStates ->
        stored ?: return@combine null
        val bySource = mappings.associateBy(AniWorldMediaMappingEntity::sourceSeriesKey)
        val releases = stored.releases.asSequence()
            .map { it.toDomain() }
            .filter { it.language.isVisibleGerman }
            .sortedBy(AniWorldRelease::sourceOrder)
            .map { release ->
                val mapping = bySource[release.sourceSeriesKey]
                val status = mapping?.matchStatus() ?: MatchStatus.UNMATCHED
                val matchedId = mapping?.aniListMediaId?.takeIf {
                    status == MatchStatus.MATCHED || status == MatchStatus.MANUALLY_CONFIRMED
                }
                EffectiveRelease(
                    aniListMediaId = matchedId,
                    sourceTitle = release.rawTitle,
                    aniListTitle = matchedId?.let { mapping.aniListTitle },
                    coverImageUrl = matchedId?.let { mapping.coverImageUrl },
                    averageScore = matchedId?.let { mapping.averageScore },
                    releaseKind = release.releaseKind,
                    seasonNumber = release.seasonNumber,
                    episodeNumber = release.episodeNumber,
                    installmentNumber = release.installmentNumber,
                    instant = release.resolvedInstant,
                    sourceDate = release.sourceDate,
                    sourceLocalTime = release.sourceLocalTime,
                    isApproximate = release.isApproximate,
                    language = release.language,
                    matchStatus = status,
                    libraryStatus = matchedId?.let { libraryStates[it]?.status }
                )
            }.toList()
        EffectiveCalendarSnapshot(
            snapshotId = stored.snapshot.snapshotId,
            fetchedAt = Instant.ofEpochMilli(stored.snapshot.fetchedAtEpochMillis),
            rangeStart = java.time.LocalDate.ofEpochDay(stored.snapshot.rangeStartEpochDay),
            rangeEnd = java.time.LocalDate.ofEpochDay(stored.snapshot.rangeEndEpochDay),
            releases = releases
        )
    }.distinctUntilChanged()

    override fun observeSyncState(): Flow<AniWorldSyncState> = dao.observeSyncState()
        .map { it?.toDomain() ?: AniWorldSyncState() }
        .distinctUntilChanged()

    override fun observeNextGermanReleases(mediaIds: Set<Int>): Flow<Map<Int, EffectiveRelease>> =
        observeSnapshot().map { snapshot ->
            val now = clock.instant()
            snapshot?.releases.orEmpty().asSequence()
                .filter { it.aniListMediaId in mediaIds && it.language.isVisibleGerman }
                .filter { it.instant?.isAfter(now) == true }
                .groupBy { requireNotNull(it.aniListMediaId) }
                .mapValues { (_, releases) -> releases.minBy { requireNotNull(it.instant) } }
        }.distinctUntilChanged()

    override fun observeLatestReleasedGermanEpisodes(
        mediaIds: Set<Int>,
        now: Instant
    ): Flow<Map<Int, Int>> = observeSnapshot().map { snapshot ->
        snapshot?.releases.orEmpty().asSequence()
            .filter { it.aniListMediaId in mediaIds && it.language.isVisibleGerman }
            .filter { it.releaseKind == ReleaseKind.EPISODE && it.episodeNumber != null }
            .filter { it.instant?.let { releaseInstant -> !releaseInstant.isAfter(now) } == true }
            .groupBy { requireNotNull(it.aniListMediaId) }
            .mapValues { (_, releases) -> releases.maxOf { requireNotNull(it.episodeNumber) } }
    }.distinctUntilChanged()

    override suspend fun refresh(): AniWorldRefreshResult {
        val attemptAt = clock.instant()
        updateAttempt(attemptAt)
        val response = try {
            client.fetch()
        } catch (throwable: Throwable) {
            return recordFailure(throwable, attemptAt)
        }
        val parsed = try {
            withContext(parserDispatcher) { parser.parse(response.html, response.fetchedAt) }.also(::validate)
        } catch (throwable: Throwable) {
            return recordFailure(throwable, attemptAt, response.httpStatus)
        }

        val acceptedState = try {
            withContext(ioDispatcher) {
                val previous = dao.syncState() ?: emptySyncStateEntity()
                val success = previous.copy(
                    lastAttemptAtEpochMillis = attemptAt.toEpochMilli(),
                    lastSuccessAtEpochMillis = response.fetchedAt.toEpochMilli(),
                    lastErrorType = null,
                    lastErrorMessage = null,
                    httpStatus = response.httpStatus,
                    parsedCount = parsed.releases.size,
                    visibleGermanCount = parsed.visibleGermanCount,
                    matchedCount = 0,
                    ambiguousCount = 0,
                    unmatchedCount = parsed.releases.filter { it.language.isVisibleGerman }
                        .distinctBy { it.sourceSeriesKey }.size,
                    activeSnapshotId = parsed.snapshotId,
                    rangeStartEpochDay = parsed.rangeStart.toEpochDay(),
                    rangeEndEpochDay = parsed.rangeEnd.toEpochDay(),
                    parserVersion = parsed.parserVersion,
                    matcherVersion = MATCHER_VERSION
                )
                if (previous.activeSnapshotId == parsed.snapshotId) {
                    dao.upsertSyncState(success)
                } else {
                    dao.activateSnapshot(
                        parsed.toEntity(),
                        parsed.releases.map { it.toEntity(parsed.parserVersion) },
                        success
                    )
                }
                success
            }
        } catch (throwable: Throwable) {
            return recordFailure(throwable, attemptAt, response.httpStatus)
        }

        val counts = matchAcceptedSnapshot(parsed)
        withContext(ioDispatcher) {
            dao.upsertSyncState(
                acceptedState.copy(
                    matchedCount = counts.matched,
                    ambiguousCount = counts.ambiguous,
                    unmatchedCount = counts.unmatched
                )
            )
        }
        return AniWorldRefreshResult.Success(parsed.snapshotId, parsed.visibleGermanCount)
    }

    private suspend fun matchAcceptedSnapshot(parsed: AniWorldParsedDocument): MatchCounts = coroutineScope {
        val releases = parsed.releases.filter { it.language.isVisibleGerman }.distinctBy { it.sourceSeriesKey }
        val dispatcher = ioDispatcher.limitedParallelism(MATCH_PARALLELISM)
        val mappings = releases.map { release ->
            async(dispatcher) { resolveMapping(release, parsed.fetchedAt) }
        }.awaitAll()
        withContext(ioDispatcher) { dao.upsertMappings(mappings) }
        MatchCounts(
            matched = mappings.count { it.status == MatchStatus.MATCHED.name || it.status == MatchStatus.MANUALLY_CONFIRMED.name },
            ambiguous = mappings.count { it.status == MatchStatus.AMBIGUOUS.name },
            unmatched = mappings.count { it.status == MatchStatus.UNMATCHED.name }
        )
    }

    private suspend fun resolveMapping(
        release: AniWorldRelease,
        now: Instant
    ): AniWorldMediaMappingEntity {
        val previous = withContext(ioDispatcher) { dao.mapping(release.sourceSeriesKey) }
        if (previous?.isManual == true ||
            previous?.status == MatchStatus.MATCHED.name ||
            previous?.status == MatchStatus.MANUALLY_CONFIRMED.name
        ) return previous.copy(updatedAtEpochMillis = now.toEpochMilli())

        val decision = runCatching {
            matcher.match(release, candidateProvider.candidatesFor(release.rawTitle))
        }.getOrElse {
            return failedMapping(release, previous, now, "candidate_provider_error:${it::class.simpleName}")
        }
        val candidate = decision.candidate
        return AniWorldMediaMappingEntity(
            sourceSeriesKey = release.sourceSeriesKey,
            aniListMediaId = candidate?.mediaId,
            status = decision.status.name,
            confidence = decision.bestScore,
            reason = decision.reason,
            matcherVersion = decision.matcherVersion,
            isManual = false,
            createdAtEpochMillis = previous?.createdAtEpochMillis ?: now.toEpochMilli(),
            updatedAtEpochMillis = now.toEpochMilli(),
            candidateCount = decision.candidateCount,
            secondBestScore = decision.secondBestScore,
            scoreMargin = decision.scoreMargin,
            aniListTitle = candidate?.titleUserPreferred,
            coverImageUrl = candidate?.coverImageUrl,
            averageScore = candidate?.averageScore
        )
    }

    private fun failedMapping(
        release: AniWorldRelease,
        previous: AniWorldMediaMappingEntity?,
        now: Instant,
        reason: String
    ) = AniWorldMediaMappingEntity(
        sourceSeriesKey = release.sourceSeriesKey,
        aniListMediaId = null,
        status = MatchStatus.UNMATCHED.name,
        confidence = null,
        reason = reason,
        matcherVersion = MATCHER_VERSION,
        isManual = false,
        createdAtEpochMillis = previous?.createdAtEpochMillis ?: now.toEpochMilli(),
        updatedAtEpochMillis = now.toEpochMilli(),
        candidateCount = 0,
        secondBestScore = null,
        scoreMargin = null,
        aniListTitle = null,
        coverImageUrl = null,
        averageScore = null
    )

    private fun validate(document: AniWorldParsedDocument) {
        require(document.daySectionCount > 0) { "No valid AniWorld day sections" }
        require(document.rangeStart <= document.rangeEnd) { "Invalid AniWorld date range" }
        require(document.documentSha256.matches(Regex("[0-9a-f]{64}"))) { "Invalid document hash" }
        require(document.parserVersion == PARSER_VERSION) { "Unexpected parser version" }
        require(document.releases.all { it.snapshotId == document.snapshotId }) { "Mixed snapshot IDs" }
        require(document.releases.all { it.sourceDate in document.rangeStart..document.rangeEnd }) {
            "Release outside snapshot range"
        }
        require(document.releases.map { it.localId }.distinct().size == document.releases.size) {
            "Duplicate release IDs"
        }
    }

    private suspend fun updateAttempt(attemptAt: Instant) = withContext(ioDispatcher) {
        val previous = dao.syncState() ?: emptySyncStateEntity()
        dao.upsertSyncState(
            previous.copy(
                lastAttemptAtEpochMillis = attemptAt.toEpochMilli(),
                lastErrorType = null,
                lastErrorMessage = null,
                httpStatus = null
            )
        )
    }

    private suspend fun recordFailure(
        throwable: Throwable,
        attemptAt: Instant,
        knownHttpStatus: Int? = null
    ): AniWorldRefreshResult.Failure {
        val errorType = throwable::class.simpleName ?: "UnknownError"
        val message = throwable.message ?: errorType
        val httpStatus = knownHttpStatus ?: (throwable as? AniWorldClientException.HttpStatus)?.status
        runCatching {
            withContext(ioDispatcher) {
                val previous = dao.syncState() ?: emptySyncStateEntity()
                dao.upsertSyncState(
                    previous.copy(
                        lastAttemptAtEpochMillis = attemptAt.toEpochMilli(),
                        lastErrorType = errorType,
                        lastErrorMessage = message.take(MAX_ERROR_LENGTH),
                        httpStatus = httpStatus
                    )
                )
            }
        }
        return AniWorldRefreshResult.Failure(errorType, message, httpStatus)
    }

    private data class MatchCounts(val matched: Int, val ambiguous: Int, val unmatched: Int)

    private companion object {
        const val MATCH_PARALLELISM = 4
        const val MAX_ERROR_LENGTH = 500
    }
}
