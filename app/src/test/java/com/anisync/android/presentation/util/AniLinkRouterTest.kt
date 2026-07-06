package com.anisync.android.presentation.util

import androidx.compose.ui.platform.UriHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM tests for [AniLinkRouter]'s URL -> destination routing. The router only needs an
 * [AniLinkCallbacks] and a [UriHandler]; both are trivially fakeable without the Android runtime.
 */
class AniLinkRouterTest {

    private class FakeUriHandler : UriHandler {
        var openedUri: String? = null
        override fun openUri(uri: String) {
            openedUri = uri
        }
    }

    private fun navigate(url: String, callbacks: AniLinkCallbacks): FakeUriHandler {
        val handler = FakeUriHandler()
        AniLinkRouter(callbacks, handler).navigate(url)
        return handler
    }

    @Test
    fun `anime url routes to onMediaClick and does not open browser`() {
        var mediaId: Int? = null
        val handler = navigate(
            "https://anilist.co/anime/16498/attack-on-titan",
            AniLinkCallbacks(onMediaClick = { mediaId = it })
        )
        assertEquals(16498, mediaId)
        assertNull(handler.openedUri)
    }

    @Test
    fun `manga url routes to onMediaClick`() {
        var mediaId: Int? = null
        navigate(
            "https://anilist.co/manga/30002/berserk",
            AniLinkCallbacks(onMediaClick = { mediaId = it })
        )
        assertEquals(30002, mediaId)
    }

    @Test
    fun `anime url without slug routes to onMediaClick`() {
        var mediaId: Int? = null
        navigate(
            "https://anilist.co/anime/1",
            AniLinkCallbacks(onMediaClick = { mediaId = it })
        )
        assertEquals(1, mediaId)
    }

    @Test
    fun `character url routes to onCharacterClick`() {
        var characterId: Int? = null
        navigate(
            "https://anilist.co/character/40882/levi",
            AniLinkCallbacks(onCharacterClick = { characterId = it })
        )
        assertEquals(40882, characterId)
    }

    @Test
    fun `staff url routes to onStaffClick`() {
        var staffId: Int? = null
        navigate(
            "https://anilist.co/staff/95075/tanaka-mayumi",
            AniLinkCallbacks(onStaffClick = { staffId = it })
        )
        assertEquals(95075, staffId)
    }

    @Test
    fun `user url routes to onUserClick with username`() {
        var username: String? = null
        navigate(
            "https://anilist.co/user/Josh/stats",
            AniLinkCallbacks(onUserClick = { username = it })
        )
        assertEquals("Josh", username)
    }

    @Test
    fun `forum thread url routes to onThreadClick with null comment`() {
        var captured: Pair<Int, Int?>? = null
        navigate(
            "https://anilist.co/forum/thread/123/some-title",
            AniLinkCallbacks(onThreadClick = { threadId, commentId -> captured = threadId to commentId })
        )
        assertEquals(123 to null, captured)
    }

    @Test
    fun `forum thread comment url routes to onThreadClick with comment id`() {
        var captured: Pair<Int, Int?>? = null
        navigate(
            "https://anilist.co/forum/thread/123/comment/456",
            AniLinkCallbacks(onThreadClick = { threadId, commentId -> captured = threadId to commentId })
        )
        // The comment pattern is the most specific and must win over the bare-thread pattern.
        assertEquals(123 to 456, captured)
    }

    @Test
    fun `review url routes to onReviewClick`() {
        var reviewId: Int? = null
        navigate(
            "https://anilist.co/review/12345",
            AniLinkCallbacks(onReviewClick = { reviewId = it })
        )
        assertEquals(12345, reviewId)
    }

    @Test
    fun `activity url routes to onActivityClick`() {
        var activityId: Int? = null
        navigate(
            "https://anilist.co/activity/98765/slug",
            AniLinkCallbacks(onActivityClick = { activityId = it })
        )
        assertEquals(98765, activityId)
    }

    @Test
    fun `anime url with trailing slash routes to onMediaClick`() {
        // AniList's canonical share URLs end with a slash (regression: these fell to the browser).
        var mediaId: Int? = null
        val handler = navigate(
            "https://anilist.co/anime/150672/Oshi-No-Ko/",
            AniLinkCallbacks(onMediaClick = { mediaId = it })
        )
        assertEquals(150672, mediaId)
        assertNull(handler.openedUri)
    }

    @Test
    fun `anime url without slug with trailing slash routes to onMediaClick`() {
        var mediaId: Int? = null
        navigate(
            "https://anilist.co/anime/99750/",
            AniLinkCallbacks(onMediaClick = { mediaId = it })
        )
        assertEquals(99750, mediaId)
    }

    @Test
    fun `manga url with query string routes to onMediaClick`() {
        var mediaId: Int? = null
        navigate(
            "https://anilist.co/manga/30002/berserk/?ref=share",
            AniLinkCallbacks(onMediaClick = { mediaId = it })
        )
        assertEquals(30002, mediaId)
    }

    @Test
    fun `anime url with www host routes to onMediaClick`() {
        var mediaId: Int? = null
        navigate(
            "https://www.anilist.co/anime/16498/attack-on-titan",
            AniLinkCallbacks(onMediaClick = { mediaId = it })
        )
        assertEquals(16498, mediaId)
    }

    @Test
    fun `character url with trailing slash routes to onCharacterClick`() {
        var characterId: Int? = null
        navigate(
            "https://anilist.co/character/40882/levi/",
            AniLinkCallbacks(onCharacterClick = { characterId = it })
        )
        assertEquals(40882, characterId)
    }

    @Test
    fun `forum thread url with trailing slash routes to onThreadClick`() {
        var captured: Pair<Int, Int?>? = null
        navigate(
            "https://anilist.co/forum/thread/123/some-title/",
            AniLinkCallbacks(onThreadClick = { threadId, commentId -> captured = threadId to commentId })
        )
        assertEquals(123 to null, captured)
    }

    @Test
    fun `user url with trailing slash routes to onUserClick`() {
        var username: String? = null
        navigate(
            "https://anilist.co/user/Josh/",
            AniLinkCallbacks(onUserClick = { username = it })
        )
        assertEquals("Josh", username)
    }

    @Test
    fun `anime url with fragment routes to onMediaClick`() {
        var mediaId: Int? = null
        navigate(
            "https://anilist.co/anime/16498#reviews",
            AniLinkCallbacks(onMediaClick = { mediaId = it })
        )
        assertEquals(16498, mediaId)
    }

    @Test
    fun `unrecognized url falls back to browser`() {
        var anyCallbackFired = false
        val handler = navigate(
            "https://example.com/some/page",
            AniLinkCallbacks(
                onMediaClick = { anyCallbackFired = true },
                onUserClick = { anyCallbackFired = true }
            )
        )
        assertEquals("https://example.com/some/page", handler.openedUri)
        assertEquals(false, anyCallbackFired)
    }

    @Test
    fun `browser fallback keeps the original url including query`() {
        val handler = navigate(
            "https://anilist.co/search/anime?genres=Action",
            AniLinkCallbacks(onMediaClick = { })
        )
        assertEquals("https://anilist.co/search/anime?genres=Action", handler.openedUri)
    }

    @Test
    fun `recognized url with no matching callback falls back to browser`() {
        // Mirrors a screen that didn't provide onMediaClick: the link must still resolve
        // (open in browser) rather than silently doing nothing.
        val handler = navigate(
            "https://anilist.co/anime/16498",
            AniLinkCallbacks()
        )
        assertEquals("https://anilist.co/anime/16498", handler.openedUri)
    }
}
