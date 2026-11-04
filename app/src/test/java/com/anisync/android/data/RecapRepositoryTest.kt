package com.anisync.android.data

import com.anisync.android.data.local.dao.RecapDao
import com.anisync.android.data.local.entity.RecapEntity
import com.anisync.android.data.local.entity.RecapSeen
import com.anisync.android.data.local.entity.RecapStamp
import com.anisync.android.domain.Result
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.recap.ActivityWindow
import com.anisync.android.domain.recap.FetchedActivity
import com.anisync.android.domain.recap.FetchedPage
import com.anisync.android.domain.recap.RecapMedia
import com.anisync.android.domain.recap.RecapMediaType
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.RecapSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class RecapRepositoryTest {

    private val zone = ZoneOffset.ofHours(3)
    private val dao = FakeRecapDao()
    private val remote = FakeRecapRemote()
    private val env = FakeRecapEnvironment(zone, nowMillis = at(2026, 11, 7, 12) * 1000)
    private val repository = RecapRepositoryImpl(remote, dao, env)

    private val october = RecapPeriod.Month(YearMonth.of(2026, 10))
    private val september = RecapPeriod.Month(YearMonth.of(2026, 9))

    init {
        remote.media += title(10, "Frieren", episodes = 12)
        remote.media += title(20, "Dandadan", episodes = 12)
        remote.media += title(30, "Pluto", episodes = 8)
        remote.activities += listOf(
            activity(6, 30, "watched episode", "1 - 2", at(2025, 3, 15)),
            activity(1, 10, "watched episode", "1 - 4", at(2025, 6, 3)),
            activity(2, 10, "watched episode", "5 - 6", at(2026, 9, 10)),
            activity(3, 20, "watched episode", "1 - 3", at(2026, 9, 20)),
            activity(4, 10, "completed", null, at(2026, 10, 5)),
            activity(5, 20, "watched episode", "4", at(2026, 10, 9)),
            activity(8, 20, "rewatched episode", "1 - 2", at(2026, 10, 20)),
        )
    }

    @Test
    fun `a first build stores last year and the kept months, new only where the banner may point`() = runTest {
        assertTrue(repository.backfill() is Result.Success)
        val rows = dao.rows.values.associateBy { it.periodKey }
        assertEquals(setOf("2025") + (0..13).map { YearMonth.of(2025, 10).plusMonths(it.toLong()).toString() }, rows.keys)
        assertNull("the banner's month starts new", rows.getValue("2026-10").seenAt)
        assertNotNull("an older month starts seen", rows.getValue("2026-09").seenAt)
        assertNotNull("last year starts seen", rows.getValue("2025").seenAt)
        assertNull("the month in progress is not seen", rows.getValue("2026-11").seenAt)
    }

    @Test
    fun `a completion counts the episodes after the last one logged months before`() = runTest {
        repository.backfill()
        val recap = (repository.getRecap(october) as Result.Success).data!!.recap
        assertEquals("episodes 7 to 12 of Frieren, then one of Dandadan", 7, recap.episodes)
        assertEquals(1, recap.finished)
    }

    @Test
    fun `opening a recap while a build stores it keeps it seen`() = runTest {
        repository.backfill()
        assertNull(dao.rows.getValue(1 to "2026-10").seenAt)
        // The story opens October while a rebuild of it is between reading the stamps and writing.
        dao.duringWrite = { dao.markSeenNow(1, "2026-10", env.nowMillis) }
        repository.getRecap(october, refresh = true)
        assertNotNull(dao.rows.getValue(1 to "2026-10").seenAt)
    }

    @Test
    fun `a month retention dropped is cut from its year without coming back to Room`() = runTest {
        repository.backfill()
        val march = RecapPeriod.Month(YearMonth.of(2025, 3))
        val recap = repository.storedRecap(march)
        assertEquals(2, recap?.recap?.episodes)
        assertNull(dao.rows[1 to "2025-03"])
    }

    @Test
    fun `a recap opened before any first build still sees the earlier episodes`() = runTest {
        val recap = (repository.getRecap(october) as Result.Success).data!!.recap
        assertEquals(7, recap.episodes)
        assertTrue("the first build ran", dao.rows.containsKey(1 to "2025"))
    }

    @Test
    fun `a build that outlives an account switch stores nothing`() = runTest {
        remote.beforeFirstPage = { env.ownerFlow.value = 2 }
        assertTrue(repository.backfill() is Result.Error)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `a year added up from its months is cut where the months were`() = runTest {
        env.nowMillis = at(2026, 1, 2, 12) * 1000
        remote.activities += activity(7, 30, "watched episode", "3 - 4", at(2025, 1, 1, 1))
        repository.backfill()
        dao.rows.remove(1 to "2025")
        // The phone moves to UTC-5, where 01:00 on 1 January at UTC+3 is still 2024.
        env.zone = ZoneOffset.ofHours(-5)
        val year = (repository.getRecap(RecapPeriod.Year(2025)) as Result.Success).data!!.recap
        assertEquals("four episodes of Pluto and four of Frieren", 8, year.episodes)
    }

    @Test
    fun `every screen's recaps follow an open and a settings change`() = runTest {
        repository.backfill()
        fun List<StoredRecap>.october() = single { it.period == october }
        assertNull(repository.observeRecaps().first { list -> list.any { it.period == october } }.october().seenAt)
        repository.markSeen(october)
        assertNotNull(repository.observeRecaps().first { it.october().seenAt != null }.october().seenAt)
        env.settingsFlow.value = RecapSettings(countRepeats = true)
        val recounted = repository.observeRecaps().first { it.october().recap.episodes != 7 }.october()
        assertEquals("the two rewatched episodes now count", 9, recounted.recap.episodes)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int = 20): Long =
        LocalDateTime.of(year, month, day, hour, 0).atZone(zone).toEpochSecond()

    private fun activity(id: Int, mediaId: Int, status: String, progress: String?, createdAt: Long) =
        FetchedActivity(id, mediaId, status, progress, createdAt)

    private fun title(id: Int, name: String, episodes: Int) = RecapMedia(
        id = id,
        type = RecapMediaType.ANIME,
        title = name,
        format = "TV",
        episodes = episodes,
        chapters = null,
        duration = 24,
        genres = listOf("Adventure"),
        mainStudio = null,
        isAdult = false,
        startDate = null,
        endDate = null,
    )
}

/** Room's recaps table in memory, keyed by owner and period. */
internal class FakeRecapDao : RecapDao() {
    val rows = LinkedHashMap<Pair<Int, String>, RecapEntity>()
    private val changes = MutableStateFlow(0)

    /** Runs once at the start of the next write, as something landing mid-build would. */
    var duringWrite: (() -> Unit)? = null

    fun markSeenNow(owner: Int, periodKey: String, at: Long) {
        rows[owner to periodKey]?.takeIf { it.seenAt == null }?.let { rows[owner to periodKey] = it.copy(seenAt = at) }
    }

    private fun changed() {
        changes.value++
    }

    override fun observeAll(ownerId: Int): Flow<List<RecapEntity>> = changes.map { rows.values.filter { it.ownerId == ownerId } }

    override suspend fun getAll(ownerId: Int): List<RecapEntity> = rows.values.filter { it.ownerId == ownerId }

    override suspend fun get(ownerId: Int, periodKey: String): RecapEntity? = rows[ownerId to periodKey]

    override suspend fun getStamps(ownerId: Int): List<RecapStamp> =
        rows.values.filter { it.ownerId == ownerId }.map { RecapStamp(it.periodKey, it.isFinal, it.builtAt, it.seenAt) }

    override suspend fun getSeen(ownerId: Int, periodKeys: List<String>): List<RecapSeen> {
        duringWrite?.also { duringWrite = null }?.invoke()
        return periodKeys.mapNotNull { key -> rows[ownerId to key]?.let { RecapSeen(key, it.seenAt) } }
    }

    override fun observePayloadBytes(ownerId: Int): Flow<Long> =
        changes.map { rows.values.filter { it.ownerId == ownerId }.sumOf { it.payload.length.toLong() } }

    override suspend fun upsertAll(rows: List<RecapEntity>) {
        rows.forEach { this.rows[it.ownerId to it.periodKey] = it }
        changed()
    }

    override suspend fun markSeen(ownerId: Int, periodKey: String, seenAt: Long) {
        markSeenNow(ownerId, periodKey, seenAt)
        changed()
    }

    override suspend fun updateSettingsKey(ownerId: Int, periodKeys: List<String>, settingsKey: String) {
        periodKeys.forEach { key -> rows[ownerId to key]?.let { rows[ownerId to key] = it.copy(settingsKey = settingsKey) } }
        changed()
    }

    override suspend fun delete(ownerId: Int, periodKeys: List<String>) {
        periodKeys.forEach { rows.remove(ownerId to it) }
        changed()
    }

    override suspend fun deleteOwner(ownerId: Int) {
        rows.keys.removeAll { it.first == ownerId }
        changed()
    }
}

/** AniList's activities and titles, served the way the real queries page them. */
internal class FakeRecapRemote : RecapRemote {
    val activities = ArrayList<FetchedActivity>()
    val media = ArrayList<RecapMedia>()
    var requests = 0

    /** Runs once before the first page is served. */
    var beforeFirstPage: (() -> Unit)? = null

    override suspend fun activityPage(userId: Int, window: ActivityWindow, page: Int): FetchedPage {
        beforeFirstPage?.also { beforeFirstPage = null }?.invoke()
        requests++
        val inside = activities
            .filter { it.createdAt > window.createdAfter && it.createdAt < window.createdBefore }
            .sortedByDescending { it.id }
        val slice = inside.drop((page - 1) * 50).take(50)
        return FetchedPage(slice, hasNextPage = inside.size > page * 50)
    }

    override suspend fun media(ids: List<Int>): List<RecapMedia> {
        requests++
        return media.filter { it.id in ids }
    }

    override suspend fun airingPage(ids: List<Int>, window: ActivityWindow, page: Int): AiringPage {
        requests++
        return AiringPage(emptyList(), hasNextPage = false)
    }

    override suspend fun user(userId: Int): RecapUser? = null
}

internal class FakeRecapEnvironment(
    var zone: ZoneId,
    var nowMillis: Long,
    owner: Int? = 1,
) : RecapEnvironment {
    val ownerFlow = MutableStateFlow(owner)
    val settingsFlow = MutableStateFlow(RecapSettings())
    var kept = 13

    override fun owner(): Int? = ownerFlow.value
    override val owners: Flow<Int?> = ownerFlow
    override fun currentSettings(): RecapSettings = settingsFlow.value
    override val settings: Flow<RecapSettings> = settingsFlow
    override fun keptMonths(): Int = kept
    override fun zone(): ZoneId = zone
    override fun nowMillis(): Long = nowMillis
}
