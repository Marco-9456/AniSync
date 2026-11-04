package com.anisync.android.data

import com.anisync.android.data.local.dao.RecapDao
import com.anisync.android.data.local.entity.RecapEntity
import com.anisync.android.data.network.RequestPriority
import com.anisync.android.data.network.findApiError
import com.anisync.android.data.network.resolveRequestPriority
import com.anisync.android.data.util.ApiError
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.RecapPreview
import com.anisync.android.domain.RecapRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.recap.ActivityWindow
import com.anisync.android.domain.recap.FetchedPage
import com.anisync.android.domain.recap.ListActivityRecord
import com.anisync.android.domain.recap.Recap
import com.anisync.android.domain.recap.MAX_ACTIVITY_PAGES
import com.anisync.android.domain.recap.RecapMedia
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.RecapSettings
import com.anisync.android.domain.recap.backfillPeriods
import com.anisync.android.domain.recap.buildRecap
import com.anisync.android.domain.recap.expiredPeriods
import com.anisync.android.domain.recap.isCountable
import com.anisync.android.domain.recap.isFinishedOn
import com.anisync.android.domain.recap.keptUpCandidates
import com.anisync.android.domain.recap.readActivityWindow
import com.anisync.android.domain.recap.recapBannerCandidates
import com.anisync.android.domain.recap.recapsDueOn
import com.anisync.android.domain.recap.sliceForPeriod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Titles per GetRecapMedia request, AniList's page size. */
private const val MEDIA_BATCH_SIZE = 50

/** How long the month in progress is shown before it is fetched again. */
private const val IN_PROGRESS_MAX_AGE_MS = 6 * 60 * 60 * 1000L

/** How often one background request waits out the budget before the build gives up. */
private const val MAX_BUDGET_WAITS = 5


@Singleton
class RecapRepositoryImpl @Inject constructor(
    private val remote: RecapRemote,
    private val recapDao: RecapDao,
    private val env: RecapEnvironment,
) : RecapRepository {

    /** One build at a time, so the worker and an open screen never fetch the same period twice. */
    private val buildLock = Mutex()

    /** Decoded payloads by owner and period, reused until the row is rebuilt. */
    private val decoded = ConcurrentHashMap<String, Decoded>()

    /** Counted recaps by owner and period, reused until the row is rebuilt or the settings change. */
    private val counted = ConcurrentHashMap<String, Counted>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val zone: ZoneId get() = env.zone()

    private fun owner(): Int? = env.owner()

    private fun today(): LocalDate = Instant.ofEpochMilli(env.nowMillis()).atZone(zone).toLocalDate()

    private fun currentSettings() = env.currentSettings()

    /** One stream for every screen, so Library, Statistics and Settings never count the rows twice. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val recaps: Flow<List<StoredRecap>> =
        env.owners
            .flatMapLatest { owner ->
                if (owner == null) {
                    flowOf(emptyList())
                } else {
                    combine(recapDao.observeAll(owner), env.settings) { rows, settings ->
                        rows.mapNotNull { count(it, settings) }.sortedWith(newestFirst)
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override fun observeRecaps(): Flow<List<StoredRecap>> = recaps

    override suspend fun getRecap(period: RecapPeriod, refresh: Boolean): Result<StoredRecap?> = safeApiCall {
        val owner = owner() ?: return@safeApiCall null
        val today = today()
        when {
            period.firstDay.isAfter(today) -> null
            period is RecapPeriod.Year && !period.isFinishedOn(today) -> yearSoFar(owner, period, today, refresh)
            else -> {
                val row = rowFor(owner, period, today, refresh)
                val settings = currentSettings()
                if (row.settingsKey != settings.key) {
                    recapDao.updateSettingsKey(owner, listOf(row.periodKey), settings.key)
                }
                withContext(Dispatchers.Default) { count(row, settings) }
            }
        }
    }

    override suspend fun storedRecap(period: RecapPeriod): StoredRecap? {
        val owner = owner() ?: return null
        val row = recapDao.get(owner, period.key)?.takeIf { decode(it) != null }
            ?: monthOfStoredYear(owner, period, today())
            ?: return null
        return withContext(Dispatchers.Default) { count(row, currentSettings()) }
    }

    override suspend fun monthInProgress(refresh: Boolean): Result<StoredRecap?> =
        getRecap(RecapPeriod.Month(YearMonth.from(today())), refresh)

    override suspend fun backfill(): Result<Unit> = safeApiCall {
        val owner = owner() ?: return@safeApiCall
        buildLock.withLock { backfillLocked(owner, today()) }
    }

    override suspend fun buildDue(monthly: Boolean, yearly: Boolean): Result<List<StoredRecap>> = safeApiCall {
        val owner = owner() ?: return@safeApiCall emptyList()
        val today = today()
        val due = recapsDueOn(today.withDayOfMonth(1))
            .filter { if (it is RecapPeriod.Year) yearly else monthly }
            // Months first, so a year can be added up from them.
            .sortedBy { it is RecapPeriod.Year }
        val rows = buildLock.withLock {
            if (due.isNotEmpty() && recapDao.getStamps(owner).isEmpty()) backfillLocked(owner, today)
            val built = due.map { currentRow(owner, it, today, refresh = false) }
            retain(owner, today)
            built
        }
        val settings = currentSettings()
        withContext(Dispatchers.Default) { rows.mapNotNull { count(it, settings) } }
    }

    override suspend fun markSeen(period: RecapPeriod) {
        val owner = owner() ?: return
        recapDao.markSeen(owner, period.key, env.nowMillis())
    }

    override suspend fun hasStored(): Boolean {
        val owner = owner() ?: return false
        return recapDao.getStamps(owner).isNotEmpty()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeStoredBytes(): Flow<Long> =
        env.owners.flatMapLatest { owner -> if (owner == null) flowOf(0L) else recapDao.observePayloadBytes(owner) }

    override suspend fun clear() {
        val owner = owner() ?: return
        // Behind a running build, or the rows it is about to store would come straight back.
        buildLock.withLock {
            recapDao.deleteOwner(owner)
            forget(owner)
        }
    }

    // No lock: a build for an account that is gone stops at its owner check before storing.
    override suspend fun clearOwner(ownerId: Int) {
        recapDao.deleteOwner(ownerId)
        forget(ownerId)
    }

    /** Drops what memory holds for [owner], or only for [periodKeys] of it. */
    private fun forget(owner: Int, periodKeys: Collection<String>? = null) {
        val keys = periodKeys?.map { "$owner/$it" }?.toSet()
        val match: (String) -> Boolean = { key -> if (keys != null) key in keys else key.startsWith("$owner/") }
        decoded.keys.removeAll(match)
        counted.keys.removeAll(match)
    }

    override suspend fun applyRetention() {
        val owner = owner() ?: return
        retain(owner, today())
    }

    override suspend fun previewRecap(
        userId: Int,
        period: RecapPeriod,
        zone: ZoneId,
        settings: RecapSettings,
    ): Result<RecapPreview> = safeApiCall {
        val started = env.nowMillis()
        val tally = Tally()
        // The sweep a first build makes at the end of the period, so completions see as far back.
        val from = when (period) {
            is RecapPeriod.Month -> LocalDate.of(period.month.year - 1, 1, 1)
            is RecapPeriod.Year -> period.firstDay
        }
        val window = ActivityWindow(from.atStartOfDay(zone).toEpochSecond(), period.window(zone).to)
        val source = fetch(userId, window, tally)
        val airing = (period as? RecapPeriod.Year)?.let { airingFor(it, source, zone, tally) }
        val today = Instant.ofEpochMilli(started).atZone(zone).toLocalDate()
        // Through the stored form, so the numbers are what a stored recap would show.
        fun built(of: RecapPeriod, airing: Map<Int, Map<Int, Long>>?): Pair<StoredRecap?, Int> {
            val stored = recapPayload(zone, sliceForPeriod(of, zone, source.records), source.media, airing).encode()
            val payload = decodeRecapPayload(stored) ?: return null to stored.length
            val media = payload.mediaById()
            val recap = buildRecap(of, payload.records(media), media, settings, zone, payload.airingByTitle())?.let { recap ->
                StoredRecap(of, recap, media, of.isFinishedOn(today), started, seenAt = null)
            }
            return recap to stored.length
        }
        val (recap, previous) = withContext(Dispatchers.Default) {
            val (main, bytes) = built(period, airing)
            tally.payloadBytes = bytes
            // A month's sweep reaches back past the month before it, so that month comes for free.
            main to (period as? RecapPeriod.Month)?.let { built(it.previous, null).first }
        }
        val user = paced {
            tally.requests++
            remote.user(userId)
        }
        RecapPreview(
            recap = recap,
            previous = previous,
            activities = tally.activities,
            requests = tally.requests,
            millis = env.nowMillis() - started,
            payloadBytes = tally.payloadBytes,
            userName = user?.name,
            avatarUrl = user?.avatarUrl,
        )
    }

    /**
     * The row a screen shows for [period]. A current stored row needs no lock. When a background
     * build holds the lock, a stored row is served as it is rather than waiting behind the build.
     */
    private suspend fun rowFor(owner: Int, period: RecapPeriod, today: LocalDate, refresh: Boolean): RecapEntity {
        if (!refresh) {
            recapDao.get(owner, period.key)
                ?.takeIf { isCurrent(it, period, today) && decode(it) != null }
                ?.let { return it }
        }
        if (buildLock.tryLock()) {
            try {
                return currentRow(owner, period, today, refresh)
            } finally {
                buildLock.unlock()
            }
        }
        recapDao.get(owner, period.key)?.takeIf { decode(it) != null }?.let { return it }
        return buildLock.withLock { currentRow(owner, period, today, refresh) }
    }

    /** The stored row for [period] while it is still good, otherwise a freshly built one. Call under [buildLock]. */
    private suspend fun currentRow(owner: Int, period: RecapPeriod, today: LocalDate, refresh: Boolean): RecapEntity {
        backfillIfEmpty(owner, today)
        val row = recapDao.get(owner, period.key)
        if (row != null && !refresh && isCurrent(row, period, today) && decode(row) != null) return row
        if (!refresh) monthOfStoredYear(owner, period, today)?.let { return it }
        return when (period) {
            is RecapPeriod.Month -> buildMonth(owner, period, today)
            is RecapPeriod.Year -> buildYear(owner, period, today)
        }
    }

    private fun isCurrent(row: RecapEntity, period: RecapPeriod, today: LocalDate): Boolean =
        if (period.isFinishedOn(today)) {
            row.isFinal
        } else {
            env.nowMillis() - row.builtAt in 0 until IN_PROGRESS_MAX_AGE_MS
        }

    /**
     * A finished month cut out of its stored year, which already holds every activity it needs.
     * A month retention no longer keeps is cut for this read only, so it never comes back to Room.
     */
    private suspend fun monthOfStoredYear(owner: Int, period: RecapPeriod, today: LocalDate): RecapEntity? {
        if (period !is RecapPeriod.Month || !period.isFinishedOn(today)) return null
        val yearRow = recapDao.get(owner, period.month.year.toString())?.takeIf { it.isFinal } ?: return null
        val year = decode(yearRow) ?: return null
        val rows = rowsFor(owner, listOf(period), year.source, today, year.zone)
        val expired = expiredPeriods(listOf(period), today, env.keptMonths()).isNotEmpty()
        return if (expired) rows.single() else recapDao.upsertKeepingSeen(rows).single()
    }

    private suspend fun buildMonth(owner: Int, month: RecapPeriod.Month, today: LocalDate): RecapEntity {
        val window = month.window(zone)
        val source = storedBefore(owner, window.from) + fetch(owner, window)
        return store(owner, listOf(month), source, today).single()
    }

    /**
     * A finished year, added up from its twelve stored months when they are all final, otherwise
     * from one sweep of the year that also stores the months retention keeps.
     */
    private suspend fun buildYear(owner: Int, year: RecapPeriod.Year, today: LocalDate): RecapEntity {
        val rows = year.months.map { recapDao.get(owner, it.key) }
        val months = rows.map { row -> row?.takeIf { it.isFinal }?.let { decode(it) } }
        if (months.all { it != null }) {
            val parts = months.filterNotNull()
            val source = parts.fold(Source.EMPTY) { sum, month -> sum + month.source }
            // Cut where the months were cut, or a zone change since would move the year's edges.
            val zone = parts.first().zone
            return store(owner, listOf(year), source, today, zone, airing = airingFor(year, source, zone)).single()
        }
        val window = year.window(zone)
        val source = storedBefore(owner, window.from) + fetch(owner, window)
        val expired = expiredPeriods(year.months, today, env.keptMonths()).toSet()
        val missing = year.months.filterIndexed { index, month -> months[index] == null && month !in expired }
        return store(owner, listOf(year) + missing, source, today, airing = airingFor(year, source)).first()
    }

    /** The running year, added up from its months and never stored. It skips the kept up count, which needs airing times. */
    private suspend fun yearSoFar(owner: Int, year: RecapPeriod.Year, today: LocalDate, refresh: Boolean): StoredRecap? {
        val current = RecapPeriod.Month(YearMonth.from(today))
        val months = year.months.filter { !it.firstDay.isAfter(today) }
        val (source, builtAt, partsZone) = storedYearSoFar(owner, months, current, today, refresh) ?: buildLock.withLock {
            backfillIfEmpty(owner, today)
            val earlier = months.filter { it != current }.map { recapDao.get(owner, it.key)?.takeIf { row -> row.isFinal }?.let { decode(it) } }
            if (earlier.all { it != null }) {
                val running = currentRow(owner, current, today, refresh)
                val parts = earlier.filterNotNull() + listOfNotNull(decode(running))
                YearParts(parts.fold(Source.EMPTY) { sum, month -> sum + month.source }, running.builtAt, parts.firstOrNull()?.zone)
            } else {
                val window = ActivityWindow(year.window(zone).from, current.window(zone).to)
                val swept = storedBefore(owner, window.from) + fetch(owner, window)
                val expired = expiredPeriods(months, today, env.keptMonths()).toSet()
                val stored = store(owner, months.filterNot { it in expired }, swept, today)
                YearParts(swept, stored.maxOfOrNull { it.builtAt } ?: env.nowMillis(), zone)
            }
        }
        val yearZone = partsZone ?: zone
        return withContext(Dispatchers.Default) {
            buildRecap(year, source.records, source.media, currentSettings(), yearZone)?.let { recap ->
                val from = year.window(yearZone).from
                val titles = source.records.filter { it.createdAt >= from }.mapTo(HashSet()) { it.mediaId }
                StoredRecap(year, recap, source.media.filterKeys { it in titles }, isFinal = false, builtAt, seenAt = null)
            }
        }
    }

    /**
     * The running year from stored months alone: when every month is stored and the month in
     * progress is current, or when a background build holds the lock and any stored copy will do.
     */
    private suspend fun storedYearSoFar(
        owner: Int,
        months: List<RecapPeriod.Month>,
        current: RecapPeriod.Month,
        today: LocalDate,
        refresh: Boolean,
    ): YearParts? {
        if (refresh) return null
        val earlier = months.filter { it != current }.map { recapDao.get(owner, it.key)?.takeIf { row -> row.isFinal } }
        val running = recapDao.get(owner, current.key)
        if (earlier.any { it == null } || running == null) return null
        if (!isCurrent(running, current, today) && !buildLock.isLocked) return null
        val parts = (earlier.filterNotNull() + running).map { decode(it) ?: return null }
        return YearParts(parts.fold(Source.EMPTY) { sum, month -> sum + month.source }, running.builtAt, parts.first().zone)
    }

    /** The running year's activities, when they were fetched, and the zone their months were cut in. */
    private data class YearParts(val source: Source, val builtAt: Long, val zone: ZoneId?)

    /**
     * Runs the first build before anything else is built from nothing. A period built alone has no
     * lookback, so a completion in it could not see the episodes logged before it. Call under [buildLock].
     */
    private suspend fun backfillIfEmpty(owner: Int, today: LocalDate) {
        if (recapDao.getStamps(owner).isEmpty()) backfillLocked(owner, today)
    }

    /** Call under [buildLock]. */
    private suspend fun backfillLocked(owner: Int, today: LocalDate) {
        val periods = backfillPeriods(today, env.keptMonths())
        if (periods.isEmpty()) return
        val from = periods.minOf { it.firstDay }.atStartOfDay(zone).toEpochSecond()
        val to = RecapPeriod.Month(YearMonth.from(today)).window(zone).to
        val source = storedBefore(owner, from) + fetch(owner, ActivityWindow(from, to))
        val airing = periods.filterIsInstance<RecapPeriod.Year>().firstOrNull()?.let { airingFor(it, source) }
        store(owner, periods, source, today, airing = airing)
        retain(owner, today)
    }

    /**
     * Cuts each period out of [source] and stores it, a year with its [airing] times. A finished
     * period starts seen unless it is one the Library banner may still announce this week, and a
     * row that was already seen stays so.
     */
    private suspend fun store(
        owner: Int,
        periods: List<RecapPeriod>,
        source: Source,
        today: LocalDate,
        zone: ZoneId = this.zone,
        airing: Map<Int, Map<Int, Long>>? = null,
    ): List<RecapEntity> {
        val rows = rowsFor(owner, periods, source, today, zone, airing)
        // After a switch the rest was fetched as another viewer, who may not see a private list.
        if (env.owner() != owner) throw AccountChanged()
        return recapDao.upsertKeepingSeen(rows)
    }

    private class AccountChanged : Exception("The active account changed during a recap build")

    /** The rows [store] would write. */
    private suspend fun rowsFor(
        owner: Int,
        periods: List<RecapPeriod>,
        source: Source,
        today: LocalDate,
        zone: ZoneId = this.zone,
        airing: Map<Int, Map<Int, Long>>? = null,
    ): List<RecapEntity> {
        val now = env.nowMillis()
        val settingsKey = currentSettings().key
        val announced = recapBannerCandidates(today).toSet()
        val seen = recapDao.getStamps(owner).associate { it.periodKey to it.seenAt }
        val rows = withContext(Dispatchers.Default) {
            periods.map { period ->
                val finished = period.isFinishedOn(today)
                val payload = recapPayload(
                    zone,
                    sliceForPeriod(period, zone, source.records),
                    source.media,
                    airing.takeIf { period is RecapPeriod.Year },
                )
                RecapEntity(
                    ownerId = owner,
                    periodKey = period.key,
                    kind = if (period is RecapPeriod.Year) RecapEntity.KIND_YEAR else RecapEntity.KIND_MONTH,
                    builtAt = now,
                    isFinal = finished,
                    settingsKey = settingsKey,
                    seenAt = seen[period.key] ?: now.takeIf { finished && period !in announced },
                    payload = payload.encode(),
                )
            }
        }
        return rows
    }

    private suspend fun retain(owner: Int, today: LocalDate) {
        val periods = recapDao.getStamps(owner).mapNotNull { RecapPeriod.fromKey(it.periodKey) }
        val expired = expiredPeriods(periods, today, env.keptMonths())
        if (expired.isNotEmpty()) {
            recapDao.delete(owner, expired.map { it.key })
            forget(owner, expired.map { it.key })
        }
    }

    /** What the stored recaps hold from before [before], so a completion after it sees earlier episodes. */
    private suspend fun storedBefore(owner: Int, before: Long): Source {
        val rows = recapDao.getAll(owner)
        return withContext(Dispatchers.Default) {
            rows.mapNotNull(::decodeNow).fold(Source.EMPTY) { sum, row ->
                sum + Source(row.records.filter { it.createdAt < before }, row.media)
            }
        }
    }

    /** Every countable activity in [window], typed through its title. */
    private suspend fun fetch(userId: Int, window: ActivityWindow, tally: Tally? = null): Source {
        val activities = readActivityWindow(window) { part, page -> fetchActivityPage(userId, part, page, tally) }
            .filter { isCountable(it.status) }
        val media = fetchMedia(activities.mapTo(LinkedHashSet()) { it.mediaId }, tally)
        val records = activities.mapNotNull { activity ->
            media[activity.mediaId]?.let {
                ListActivityRecord(activity.id, activity.mediaId, it.type, activity.status, activity.progress, activity.createdAt)
            }
        }
        return Source(records, media)
    }

    private suspend fun fetchActivityPage(userId: Int, window: ActivityWindow, page: Int, tally: Tally?): FetchedPage {
        val result = paced {
            tally?.let { it.requests++ }
            remote.activityPage(userId, window, page)
        }
        tally?.let { it.activities += result.activities.size }
        return result
    }

    private suspend fun fetchMedia(ids: Collection<Int>, tally: Tally?): Map<Int, RecapMedia> {
        val media = HashMap<Int, RecapMedia>(ids.size)
        for (batch in ids.chunked(MEDIA_BATCH_SIZE)) {
            val titles = paced {
                tally?.let { it.requests++ }
                remote.media(batch)
            }
            titles.forEach { media[it.id] = it }
        }
        return media
    }

    /** Airing times for the series watched in [year], which only its kept up count needs. */
    private suspend fun airingFor(
        year: RecapPeriod.Year,
        source: Source,
        zone: ZoneId = this.zone,
        tally: Tally? = null,
    ): Map<Int, Map<Int, Long>> {
        val ids = keptUpCandidates(year, zone, source.records, source.media)
        return fetchAiring(ids, year.window(zone), tally)
    }

    private suspend fun fetchAiring(ids: Collection<Int>, window: ActivityWindow, tally: Tally?): Map<Int, Map<Int, Long>> {
        val airing = HashMap<Int, MutableMap<Int, Long>>()
        for (batch in ids.chunked(MEDIA_BATCH_SIZE)) {
            var page = 1
            while (page <= MAX_ACTIVITY_PAGES) {
                val result = paced {
                    tally?.let { it.requests++ }
                    remote.airingPage(batch, window, page)
                }
                result.rows.forEach { airing.getOrPut(it.mediaId) { HashMap() }[it.episode] = it.airingAt }
                if (!result.hasNextPage) break
                page++
            }
        }
        return airing
    }

    /**
     * Runs one request, and in the background waits out a deferral instead of failing.
     *
     * A recap is built from many requests that only count together. A worker that handed the
     * deferral back to WorkManager would throw away every page already read and start over on its
     * next attempt, into the same budget. A screen never waits here, it gets the countdown error.
     */
    private suspend fun <T> paced(request: suspend () -> T): T {
        var waits = 0
        while (true) {
            try {
                return request()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val seconds = when (val error = e.findApiError()) {
                    is ApiError.Deferred -> error.retryAfterSeconds
                    is ApiError.RateLimited -> error.retryAfterSeconds
                    else -> throw e
                }
                if (waits >= MAX_BUDGET_WAITS || resolveRequestPriority() != RequestPriority.Background) throw e
                waits++
                delay(seconds.coerceIn(1L, 120L) * 1000L)
            }
        }
    }

    private fun count(row: RecapEntity, settings: RecapSettings): StoredRecap? {
        val data = decodeNow(row) ?: return null
        val key = "${row.ownerId}/${row.periodKey}"
        val memo = counted[key]?.takeIf { it.builtAt == row.builtAt && it.settingsKey == settings.key }
        val recap = if (memo != null) {
            memo.recap
        } else {
            buildRecap(data.period, data.records, data.media, settings, data.zone, data.airing)
                .also { counted[key] = Counted(row.builtAt, settings.key, it) }
        } ?: return null
        return StoredRecap(data.period, recap, data.media, row.isFinal, row.builtAt, row.seenAt)
    }

    /** What the row built at [builtAt] counts to with the settings [settingsKey] names, null when nothing counts. */
    private class Counted(val builtAt: Long, val settingsKey: String, val recap: Recap?)

    /** The payload decoded off the main thread, which a story or a screen may be calling from. */
    private suspend fun decode(row: RecapEntity): Decoded? =
        decoded["${row.ownerId}/${row.periodKey}"]?.takeIf { it.builtAt == row.builtAt }
            ?: withContext(Dispatchers.Default) { decodeNow(row) }

    private fun decodeNow(row: RecapEntity): Decoded? {
        val cacheKey = "${row.ownerId}/${row.periodKey}"
        decoded[cacheKey]?.takeIf { it.builtAt == row.builtAt }?.let { return it }
        val period = RecapPeriod.fromKey(row.periodKey) ?: return null
        val payload = decodeRecapPayload(row.payload) ?: return null
        val media = payload.mediaById()
        return Decoded(row.builtAt, period, payload.zoneId, payload.records(media), media, payload.airingByTitle())
            .also { decoded[cacheKey] = it }
    }

    private class Decoded(
        val builtAt: Long,
        val period: RecapPeriod,
        val zone: ZoneId,
        val records: List<ListActivityRecord>,
        val media: Map<Int, RecapMedia>,
        val airing: Map<Int, Map<Int, Long>>?,
    ) {
        val source: Source get() = Source(records, media)
    }

    private data class Source(val records: List<ListActivityRecord>, val media: Map<Int, RecapMedia>) {
        operator fun plus(other: Source) = Source((records + other.records).distinctBy { it.id }, media + other.media)

        companion object {
            val EMPTY = Source(emptyList(), emptyMap())
        }
    }

    private class Tally {
        var requests = 0
        var activities = 0
        var payloadBytes = 0
    }

    private companion object {
        /** Latest period first, and a year before the December it ends with. */
        val newestFirst = compareByDescending<StoredRecap> { it.period.lastDay }
            .thenBy { if (it.period is RecapPeriod.Year) 0 else 1 }
    }
}
