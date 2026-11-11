package com.anisync.android.presentation.library

import com.anisync.android.data.network.RequestPriority
import com.anisync.android.data.network.withRequestPriority
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.LibraryRepository
import kotlinx.coroutines.withTimeoutOrNull

private const val PROGRESS_CHECK_TIMEOUT_MS = 2_000L

/**
 * AniList's count for [mediaId] when it no longer matches [startedFrom], the count a +1 or -1 was
 * made from, or null to go ahead.
 *
 * A +1 saves an absolute number, so one counted from a row older than a change made on the website
 * or in another app would write that change over. It asks AniList once per burst of taps, even for
 * a row synced seconds ago, and a slow or refused check lets the save through as before.
 */
internal suspend fun progressChangedElsewhere(
    detailsRepository: DetailsRepository,
    libraryRepository: LibraryRepository,
    mediaId: Int,
    startedFrom: Int
): Int? {
    withTimeoutOrNull(PROGRESS_CHECK_TIMEOUT_MS) {
        withRequestPriority(RequestPriority.Prefetch) {
            detailsRepository.checkListEntry(mediaId)
        }
    }
    val current = libraryRepository.getEntry(mediaId)?.progress ?: return null
    return current.takeIf { it != startedFrom }
}
