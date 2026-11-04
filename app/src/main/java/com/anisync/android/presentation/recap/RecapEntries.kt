package com.anisync.android.presentation.recap

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.work.WorkManager
import com.anisync.android.data.AppSettings
import com.anisync.android.data.RecapDelivery
import com.anisync.android.data.RecapNotices
import com.anisync.android.data.account.AccountStore
import com.anisync.android.domain.RecapRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.recapBannerCandidates
import com.anisync.android.worker.RecapWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import javax.inject.Inject

/** A finished recap as a shelf card, a grid tile or the Library banner shows it. */
@Immutable
data class RecapEntry(
    val period: RecapPeriod,
    val isNew: Boolean,
    val episodes: Int,
    val minutes: Int,
    val titles: Int,
    val topTitle: String?,
    val coverUrl: String?,
    val secondCoverUrl: String?,
    val seed: Color?,
) {
    val key: String get() = period.key
    val isYear: Boolean get() = period is RecapPeriod.Year
}

/** The month in progress, as the Overview and Recaps cards count it so far. */
@Immutable
data class RecapProgress(
    val month: YearMonth,
    val day: Int,
    val episodes: Int,
    val titles: Int,
    val activeDays: Int,
    val finished: Int,
    val updates: Int,
    val topTitle: String?,
    val topMinutes: Int,
    val coverUrl: String?,
    val secondCoverUrl: String?,
    val seed: Color?,
)

/** The year so far, opening on 1 January. */
@Immutable
data class RecapRunningYear(val year: Int, val episodes: Int, val months: List<Int>)

/** Everything Statistics shows about recaps on the viewer's own profile. */
@Immutable
data class RecapsUiModel(
    val entries: List<RecapEntry> = emptyList(),
    val inProgress: RecapProgress? = null,
    val runningYear: RecapRunningYear? = null,
    /** True while the first build runs, before anything is stored. */
    val building: Boolean = false,
) {
    val months: List<RecapEntry> get() = entries.filter { !it.isYear }
    val years: List<RecapEntry> get() = entries.filter { it.isYear }

    /**
     * What Overview's shelf shows: the recaps waiting to be opened, newest first, then the latest
     * year beside them. With nothing new it shows the month in progress instead.
     */
    val shelf: List<RecapEntry>
        get() {
            val fresh = entries.filter { it.isNew }
            if (fresh.isEmpty()) return emptyList()
            val year = years.firstOrNull()
            return if (year == null || year in fresh) fresh else fresh + year
        }
}

/**
 * Turns the stored recaps into what Library and Statistics show, and answers the questions only
 * they ask: whether the banner is due, and whether a first build has to start.
 */
class RecapEntries @Inject constructor(
    private val repository: RecapRepository,
    private val cards: RecapCardFactory,
    private val notices: RecapNotices,
    private val accountStore: AccountStore,
    private val appSettings: AppSettings,
    @ApplicationContext private val context: Context,
) {
    /** Every finished recap, newest first. */
    fun entries(): Flow<List<RecapEntry>> = repository.observeRecaps().map { stored ->
        stored.filter { it.isFinal }.map(::entry)
    }

    /**
     * The recap the Library banner points at: in the first week of a month, the newest one due
     * this week that is stored, not opened yet and not hidden this month, unless delivery is off.
     */
    fun banner(): Flow<RecapEntry?> = combine(
        entries(),
        notices.bannerClosed,
        appSettings.recapDelivery,
        accountStore.activeAccount,
    ) { entries, closed, delivery, account ->
        val today = LocalDate.now()
        val owner = account?.id ?: return@combine null
        if (delivery == RecapDelivery.NOWHERE) return@combine null
        if (closed[owner] == YearMonth.from(today)) return@combine null
        recapBannerCandidates(today).firstNotNullOfOrNull { period -> entries.firstOrNull { it.period == period && it.isNew } }
    }

    fun closeBanner() {
        val owner = accountStore.activeAccount.value?.id ?: return
        notices.closeBanner(owner, YearMonth.now())
    }

    /** True while a first build is queued or running. */
    fun firstBuildRunning(): Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(RecapWorker.FIRST_BUILD_NAME)
            .map { infos -> infos.any { !it.state.isFinished } }

    /** Starts the first build when nothing is stored for the account yet, true when it did. */
    suspend fun buildIfEmpty(): Boolean {
        if (repository.hasStored()) return false
        RecapWorker.firstBuild(context)
        return true
    }

    suspend fun inProgress(refresh: Boolean): RecapProgress? {
        val stored = (repository.monthInProgress(refresh) as? Result.Success)?.data ?: return null
        val month = (stored.period as? RecapPeriod.Month)?.month ?: return null
        val recap = stored.recap
        val titles = cards.titles(stored)
        val top = recap.ranked.firstOrNull()
        return RecapProgress(
            month = month,
            day = LocalDate.now().dayOfMonth.coerceAtMost(month.lengthOfMonth()),
            episodes = recap.episodes,
            titles = recap.titles,
            activeDays = recap.activeDays,
            finished = recap.finished,
            updates = recap.updates,
            topTitle = top?.let { titles[it.mediaId]?.title },
            topMinutes = top?.minutes ?: 0,
            coverUrl = top?.let { titles[it.mediaId]?.coverUrl },
            secondCoverUrl = recap.ranked.getOrNull(1)?.let { titles[it.mediaId]?.coverUrl },
            seed = top?.let { titles[it.mediaId]?.color },
        )
    }

    suspend fun runningYear(refresh: Boolean): RecapRunningYear? {
        val year = LocalDate.now().year
        val stored = (repository.getRecap(RecapPeriod.Year(year), refresh) as? Result.Success)?.data ?: return null
        val months = stored.recap.episodesByMonth ?: return null
        val current = LocalDate.now().monthValue
        return RecapRunningYear(year, stored.recap.episodes, Month.entries.take(current).map { months[it] ?: 0 })
    }

    private fun entry(stored: StoredRecap): RecapEntry {
        val titles = cards.titles(stored)
        val ranked = stored.recap.ranked
        val top = ranked.firstOrNull()?.let { titles[it.mediaId] }
        return RecapEntry(
            period = stored.period,
            isNew = stored.seenAt == null,
            episodes = stored.recap.episodes,
            minutes = stored.recap.minutes,
            titles = stored.recap.titles,
            topTitle = top?.title,
            coverUrl = top?.coverUrl,
            secondCoverUrl = ranked.getOrNull(1)?.let { titles[it.mediaId]?.coverUrl },
            seed = top?.color,
        )
    }
}
