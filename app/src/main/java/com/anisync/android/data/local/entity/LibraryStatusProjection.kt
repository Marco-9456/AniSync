package com.anisync.android.data.local.entity

import com.anisync.android.domain.LibraryStatus

/**
 * Two-column projection of [LibraryEntryEntity] used to tell, for a whole screen of media at once,
 * which titles are already on the account's lists. Reading full entities for that would pull every
 * cover URL and date field for the entire library on every emission.
 */
data class LibraryStatusProjection(
    val mediaId: Int,
    val status: LibraryStatus
)
