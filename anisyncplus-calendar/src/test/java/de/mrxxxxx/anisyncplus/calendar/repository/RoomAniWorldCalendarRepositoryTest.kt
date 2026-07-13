package de.mrxxxxx.anisyncplus.calendar.repository

import android.content.Context
import androidx.room.Room
import de.mrxxxxx.anisyncplus.calendar.api.*
import de.mrxxxxx.anisyncplus.calendar.domain.*
import de.mrxxxxx.anisyncplus.calendar.local.*
import de.mrxxxxx.anisyncplus.calendar.matching.DefaultAniWorldTitleMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RoomAniWorldCalendarRepositoryTest {
    private lateinit var database: AniWorldCalendarDatabase
    private lateinit var dao: AniWorldCalendarDao
    private val client = FakeClient()
    private val parser = FakeParser()
    private val candidates = FakeCandidates()
    private val library = FakeLibrary()
    private val now = Instant.parse("2026-07-13T10:00:00Z")
    private lateinit var repository: RoomAniWorldCalendarRepository

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AniWorldCalendarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.calendarDao()
        repository = RoomAniWorldCalendarRepository(
            dao = dao,
            client = client,
            parser = parser,
            matcher = DefaultAniWorldTitleMatcher(),
            candidateProvider = candidates,
            libraryStateProvider = library,
            clock = Clock.fixed(now, ZoneOffset.UTC)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun firstSnapshotIsStoredAndEmittedConsistently() = runTest {
        parser.document = document("one", listOf(release("one", "a", now.plusSeconds(3600))))
        client.response = response()

        assertTrue(repository.refresh() is AniWorldRefreshResult.Success)

        val snapshot = repository.observeSnapshot().first { it != null }!!
        assertEquals("one", snapshot.snapshotId)
        assertEquals(1, snapshot.releases.size)
        assertEquals(1, dao.snapshotCount())
        assertEquals(1, dao.releaseCount("one"))
        assertEquals("one", repository.observeSyncState().first().activeSnapshotId)
    }

    @Test
    fun atomicSnapshotSwitchRollsBackOnDatabaseConstraint() = runTest {
        val first = document("one", listOf(release("one", "a", now)))
        dao.activateSnapshot(
            first.toEntity(),
            first.releases.map { it.toEntity(first.parserVersion) },
            syncFor(first)
        )
        val duplicated = release("two", "duplicate", now)
        val second = document("two", listOf(duplicated, duplicated))
        assertThrows(Exception::class.java) {
            runTest {
                dao.activateSnapshot(
                    second.toEntity(),
                    second.releases.map { it.toEntity(second.parserVersion) },
                    syncFor(second)
                )
            }
        }

        assertEquals("one", dao.syncState()!!.activeSnapshotId)
        assertEquals(1, dao.snapshotCount())
        assertEquals("one", dao.observeActiveSnapshot().first()!!.snapshot.snapshotId)
    }

    @Test
    fun successfulSwitchRemovesOldSnapshotButKeepsMapping() = runTest {
        val first = document("one", listOf(release("one", "a", now)))
        dao.activateSnapshot(first.toEntity(), first.releases.map { it.toEntity(PARSER_VERSION) }, syncFor(first))
        dao.upsertMapping(mapping("slug:a", 11))
        val second = document("two", listOf(release("two", "a", now.plusSeconds(60))))
        dao.activateSnapshot(second.toEntity(), second.releases.map { it.toEntity(PARSER_VERSION) }, syncFor(second))

        assertEquals(1, dao.snapshotCount())
        assertEquals(0, dao.releaseCount("one"))
        assertEquals(1, dao.releaseCount("two"))
        assertEquals(11, dao.mapping("slug:a")!!.aniListMediaId)
    }

    @Test
    fun refreshFailuresKeepCacheAndUpdateDiagnostics() = runTest {
        parser.document = document("one", listOf(release("one", "a", now)))
        client.response = response()
        repository.refresh()

        client.failure = IOException("offline")
        assertTrue(repository.refresh() is AniWorldRefreshResult.Failure)
        assertEquals("one", repository.observeSnapshot().first { it != null }!!.snapshotId)
        assertEquals("IOException", dao.syncState()!!.lastErrorType)

        client.failure = null
        parser.failure = IllegalStateException("dom changed")
        assertTrue(repository.refresh() is AniWorldRefreshResult.Failure)
        assertEquals("one", dao.syncState()!!.activeSnapshotId)
        assertEquals("IllegalStateException", dao.syncState()!!.lastErrorType)

        parser.failure = null
        parser.document = document("bad", listOf(release("other", "b", now)))
        assertTrue(repository.refresh() is AniWorldRefreshResult.Failure)
        assertEquals("one", dao.syncState()!!.activeSnapshotId)
        assertEquals("IllegalArgumentException", dao.syncState()!!.lastErrorType)
    }

    @Test
    fun structurallyValidEmptySnapshotIsAccepted() = runTest {
        parser.document = document("empty", emptyList())
        client.response = response()

        val result = repository.refresh()

        assertTrue(result is AniWorldRefreshResult.Success)
        assertEquals("empty", repository.observeSnapshot().first { it != null }!!.snapshotId)
        assertTrue(repository.observeSnapshot().first { it != null }!!.releases.isEmpty())
        assertEquals(0, dao.syncState()!!.visibleGermanCount)
    }

    @Test
    fun invalidEmptySnapshotIsRejectedAndOldCacheRemains() = runTest {
        parser.document = document("one", listOf(release("one", "a", now)))
        client.response = response()
        repository.refresh()
        parser.document = document("empty", emptyList()).copy(daySectionCount = 0)

        assertTrue(repository.refresh() is AniWorldRefreshResult.Failure)

        assertEquals("one", dao.syncState()!!.activeSnapshotId)
    }

    @Test
    fun resolverUsesEarliestFutureAndLatestReleasedNumericEpisodeOnly() = runTest {
        val releases = listOf(
            release("one", "a", now.minusSeconds(60), episode = 3),
            release("one", "a", now.plusSeconds(7200), episode = 5),
            release("one", "a", now.plusSeconds(3600), episode = 4, approximate = true),
            release("one", "film", now.minusSeconds(30), kind = ReleaseKind.FILM, episode = null),
            release("one", "special", now.minusSeconds(20), kind = ReleaseKind.SPECIAL, episode = null)
        )
        val document = document("one", releases)
        dao.activateSnapshot(document.toEntity(), releases.map { it.toEntity(PARSER_VERSION) }, syncFor(document))
        dao.upsertMappings(listOf(mapping("slug:a", 11), mapping("slug:film", 12), mapping("slug:special", 13)))

        val next = repository.observeNextGermanReleases(setOf(11, 12, 13)).first()
        val latest = repository.observeLatestReleasedGermanEpisodes(setOf(11, 12, 13), now).first()

        assertEquals(4, next[11]!!.episodeNumber)
        assertTrue(next[11]!!.isApproximate)
        assertEquals(3, latest[11])
        assertFalse(latest.containsKey(12))
        assertFalse(latest.containsKey(13))
    }

    @Test
    fun unmatchedAndUnknownLanguageNeverBecomeEffectiveReleaseData() = runTest {
        val unmatched = release("one", "unmatched", now.plusSeconds(1))
        val unknown = release("one", "unknown", now.plusSeconds(1), language = AniWorldReleaseLanguage.UNKNOWN)
        val document = document("one", listOf(unmatched, unknown))
        dao.activateSnapshot(document.toEntity(), document.releases.map { it.toEntity(PARSER_VERSION) }, syncFor(document))

        val snapshot = repository.observeSnapshot().first { it != null }!!
        val next = repository.observeNextGermanReleases(setOf(99)).first()

        assertEquals(1, snapshot.releases.size)
        assertEquals(MatchStatus.UNMATCHED, snapshot.releases.single().matchStatus)
        assertTrue(next.isEmpty())
    }

    @Test
    fun accountStateChangesAreReflectedFromTheSharedResolverFlow() = runTest {
        val release = release("one", "a", now.plusSeconds(1))
        val document = document("one", listOf(release))
        dao.activateSnapshot(document.toEntity(), listOf(release.toEntity(PARSER_VERSION)), syncFor(document))
        dao.upsertMapping(mapping("slug:a", 11))
        library.states.value = mapOf(11 to AniListUserMediaState(11, "CURRENT", 2))
        assertEquals("CURRENT", repository.observeSnapshot().first { it != null }!!.releases.single().libraryStatus)

        library.states.value = mapOf(11 to AniListUserMediaState(11, "PLANNING", 0))

        assertEquals("PLANNING", repository.observeSnapshot().first { it != null }!!.releases.single().libraryStatus)
    }

    @Test
    fun databaseNameIsIsolatedFromUpstream() {
        assertEquals("anisync_plus_calendar.db", AniWorldCalendarDatabase.DATABASE_NAME)
        assertNotEquals("anisync.db", AniWorldCalendarDatabase.DATABASE_NAME)
    }

    private fun response() = AniWorldCalendarResponse("<html/>", now, 200)

    private fun document(id: String, releases: List<AniWorldRelease>) = AniWorldParsedDocument(
        snapshotId = id,
        fetchedAt = now,
        rangeStart = LocalDate.of(2026, 7, 13),
        rangeEnd = LocalDate.of(2026, 7, 19),
        documentSha256 = "a".repeat(64),
        parserVersion = PARSER_VERSION,
        daySectionCount = 7,
        releases = releases
    )

    private fun release(
        snapshotId: String,
        key: String,
        instant: Instant?,
        kind: ReleaseKind = ReleaseKind.EPISODE,
        episode: Int? = 1,
        approximate: Boolean = false,
        language: AniWorldReleaseLanguage = AniWorldReleaseLanguage.DE_SUB
    ) = AniWorldRelease(
        localId = "release-${key}-${instant?.epochSecond}",
        snapshotId = snapshotId,
        sourceSeriesKey = "slug:${key}",
        sourceSlug = key,
        sourceUrl = "https://aniworld.to/anime/stream/${key}",
        rawTitle = key,
        normalizedTitle = key,
        sourceDate = LocalDate.of(2026, 7, 13),
        sourceLocalTime = instant?.atZone(ZoneId.of(SOURCE_ZONE_ID))?.toLocalTime() ?: LocalTime.NOON,
        sourceZoneId = ZoneId.of(SOURCE_ZONE_ID),
        resolvedInstant = instant,
        isApproximate = approximate,
        releaseKind = kind,
        seasonNumber = if (kind == ReleaseKind.EPISODE) 1 else null,
        episodeNumber = episode,
        installmentNumber = if (kind == ReleaseKind.FILM) 1 else null,
        rawInstallmentToken = null,
        language = language,
        rawLanguageMarkers = setOf("test"),
        sourceOrder = 0,
        diagnosticStatus = ReleaseDiagnosticStatus.VALID
    )

    private fun syncFor(document: AniWorldParsedDocument) = emptySyncStateEntity().copy(
        activeSnapshotId = document.snapshotId,
        lastSuccessAtEpochMillis = now.toEpochMilli(),
        parsedCount = document.releases.size,
        visibleGermanCount = document.visibleGermanCount,
        rangeStartEpochDay = document.rangeStart.toEpochDay(),
        rangeEndEpochDay = document.rangeEnd.toEpochDay()
    )

    private fun mapping(key: String, mediaId: Int) = AniWorldMediaMappingEntity(
        sourceSeriesKey = key,
        aniListMediaId = mediaId,
        status = MatchStatus.MATCHED.name,
        confidence = 1.0,
        reason = "test",
        matcherVersion = MATCHER_VERSION,
        isManual = false,
        createdAtEpochMillis = now.toEpochMilli(),
        updatedAtEpochMillis = now.toEpochMilli(),
        candidateCount = 1,
        secondBestScore = null,
        scoreMargin = 1.0,
        aniListTitle = key,
        coverImageUrl = "https://example.invalid/cover.jpg",
        averageScore = 80
    )

    private class FakeClient : AniWorldCalendarClient {
        lateinit var response: AniWorldCalendarResponse
        var failure: Throwable? = null
        override suspend fun fetch(): AniWorldCalendarResponse {
            failure?.let { throw it }
            return response
        }
    }

    private class FakeParser : AniWorldCalendarParser {
        lateinit var document: AniWorldParsedDocument
        var failure: Throwable? = null
        override fun parse(html: String, fetchedAt: Instant): AniWorldParsedDocument {
            failure?.let { throw it }
            return document
        }
    }

    private class FakeCandidates : AniWorldMatchCandidateProvider {
        override suspend fun candidatesFor(rawTitle: String): List<AniListMatchCandidate> = emptyList()
    }

    private class FakeLibrary : AniListLibraryStateProvider {
        val states = MutableStateFlow<Map<Int, AniListUserMediaState>>(emptyMap())
        override fun observeStates() = states
    }
}
