package com.anisync.android.data

import android.util.Log
import com.anisync.android.domain.MediaThemesRepository
import com.anisync.android.domain.RecapRepository
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.songCandidates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looks up the songs of recaps still to be opened, when they are built and when the banner or the
 * shelf shows them, so opening one only has to stream its song. Each recap is looked up once a
 * process, and only while songs are on.
 */
@Singleton
class RecapSongs @Inject constructor(
    private val recaps: RecapRepository,
    private val themes: MediaThemesRepository,
    private val appSettings: AppSettings,
) {
    private val done: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** Every stored recap waiting to be opened, the ones the banner and the shelf show. */
    suspend fun prefetchUnopened() = prefetch { recaps.observeRecaps().first().filter { it.isUnopened } }

    /** The recaps stored for [periods], read again rather than from a stream that may not have caught up. */
    suspend fun prefetch(periods: Collection<RecapPeriod>) =
        prefetch { periods.mapNotNull { recaps.storedRecap(it) }.filter { it.isUnopened } }

    private suspend fun prefetch(pick: suspend () -> List<StoredRecap>) {
        if (!appSettings.recapThemeSong.value) return
        try {
            for (recap in pick()) {
                if (!done.add("${recap.period.key}/${recap.builtAt}")) continue
                themes.themesFor(recap.songCandidates().map { it.mediaId })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Recap songs could not be looked up", e)
        }
    }

    private val StoredRecap.isUnopened: Boolean get() = isFinal && seenAt == null

    private companion object {
        const val TAG = "RecapSongs"
    }
}
