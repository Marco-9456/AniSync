package com.anisync.android.data

import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaListStatus
import com.apollographql.apollo.api.Optional
import org.junit.Assert.assertEquals
import org.junit.Test

class ListStatusMutationTest {

    @Test
    fun `a status change leaves the progress on AniList alone`() {
        // Steins;Gate added on the website at episode 9 while its page here still said Add to list.
        val mutation = listStatusMutation(mediaId = 9253, status = LibraryStatus.CURRENT, progress = null)

        assertEquals(Optional.Absent, mutation.progress)
        assertEquals(Optional.present(MediaListStatus.CURRENT), mutation.status)
        assertEquals(Optional.present(9253), mutation.mediaId)
    }

    @Test
    fun `a progress to set goes out with the status`() {
        val mutation = listStatusMutation(mediaId = 9253, status = LibraryStatus.COMPLETED, progress = 24)

        assertEquals(Optional.present(24), mutation.progress)
        assertEquals(Optional.present(MediaListStatus.COMPLETED), mutation.status)
    }
}
