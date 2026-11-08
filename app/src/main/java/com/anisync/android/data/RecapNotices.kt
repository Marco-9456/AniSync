package com.anisync.android.data

import android.content.Context
import androidx.core.content.edit
import com.anisync.android.domain.recap.RecapPeriod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What has already been said about recaps, per account: which periods a notification announced,
 * so one never rings twice, and the month the Library banner was closed in, so it stays away until
 * the next one.
 */
@Singleton
class RecapNotices @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _bannerClosed = MutableStateFlow(readBannerClosed())

    /** The month each account closed the banner in. */
    val bannerClosed: StateFlow<Map<Int, YearMonth>> = _bannerClosed.asStateFlow()

    fun closeBanner(ownerId: Int, month: YearMonth) {
        prefs.edit { putString(bannerKey(ownerId), month.toString()) }
        _bannerClosed.value = _bannerClosed.value + (ownerId to month)
    }

    fun wasNotified(ownerId: Int, period: RecapPeriod): Boolean =
        period.key in prefs.getStringSet(notifiedKey(ownerId), emptySet()).orEmpty()

    /** Records [periods] as announced, keeping only the last few so the set never grows. */
    fun markNotified(ownerId: Int, periods: Collection<RecapPeriod>) {
        val known = prefs.getStringSet(notifiedKey(ownerId), emptySet()).orEmpty() + periods.map { it.key }
        val kept = known.sortedDescending().take(KEPT_NOTICES).toSet()
        prefs.edit { putStringSet(notifiedKey(ownerId), kept) }
    }

    /** Forgets an account that was removed. */
    fun clear(ownerId: Int) {
        prefs.edit {
            remove(bannerKey(ownerId))
            remove(notifiedKey(ownerId))
        }
        _bannerClosed.value = _bannerClosed.value - ownerId
    }

    private fun readBannerClosed(): Map<Int, YearMonth> = prefs.all.mapNotNull { (key, value) ->
        val owner = key.removePrefix(BANNER_PREFIX).takeIf { key.startsWith(BANNER_PREFIX) }?.toIntOrNull()
        val month = (value as? String)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        if (owner != null && month != null) owner to month else null
    }.toMap()

    private fun bannerKey(ownerId: Int) = "$BANNER_PREFIX$ownerId"

    private fun notifiedKey(ownerId: Int) = "$NOTIFIED_PREFIX$ownerId"

    private companion object {
        const val PREFS = "recap_notices"
        const val BANNER_PREFIX = "banner_closed_"
        const val NOTIFIED_PREFIX = "notified_"

        /** Two years of months and years, far more than retention keeps. */
        const val KEPT_NOTICES = 30
    }
}
