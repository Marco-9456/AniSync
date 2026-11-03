package com.anisync.android.presentation.recap

import com.anisync.android.domain.StoredRecap
import javax.inject.Inject
import javax.inject.Singleton

/** A recap Developer Tools built for another user, waiting for its story to open. */
data class PreviewRecap(
    val recap: StoredRecap,
    val previous: StoredRecap?,
    val userName: String,
    val avatarUrl: String?,
)

/**
 * Hands a Developer Tools preview to the story without building it again. It lives in memory only
 * and is never stored, so a preview stays out of the shelf and is gone with the process.
 */
@Singleton
class RecapPreviewStore @Inject constructor() {
    @Volatile
    var preview: PreviewRecap? = null
}
