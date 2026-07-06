package com.anisync.android.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.anisync.android.domain.LinkPreviewProvider

/**
 * Defines actions the [AniLinkRouter] can take when it recognizes a URL.
 * Screens provide callbacks for each supported in-app destination.
 */
data class AniLinkCallbacks(
    /** Navigate to the media details screen for a given AniList media ID. */
    val onMediaClick: ((mediaId: Int) -> Unit)? = null,
    /** Navigate to a forum thread, optionally scrolling to a specific comment. */
    val onThreadClick: ((threadId: Int, commentId: Int?) -> Unit)? = null,
    /** Navigate to a character details screen. */
    val onCharacterClick: ((characterId: Int) -> Unit)? = null,
    /** Navigate to a staff details screen. */
    val onStaffClick: ((staffId: Int) -> Unit)? = null,
    /** Navigate to a user profile screen. */
    val onUserClick: ((username: String) -> Unit)? = null,
    /** Navigate to a review detail screen. */
    val onReviewClick: ((reviewId: Int) -> Unit)? = null,
    /** Navigate to an activity detail screen. */
    val onActivityClick: ((activityId: Int) -> Unit)? = null
)

/**
 * CompositionLocal providing [AniLinkCallbacks] to composables in the tree.
 * When not provided, the router falls back to opening all links in the browser.
 */
val LocalAniLinkCallbacks = compositionLocalOf { AniLinkCallbacks() }

val LocalLinkPreviewProvider = staticCompositionLocalOf<LinkPreviewProvider?> { null }

/**
 * Centralized link router that intercepts recognizable AniList URLs and
 * navigates in-app, falling back to the system browser for everything else.
 *
 * **Supported URL patterns (in order of specificity):**
 *
 * | Pattern | In-App Action |
 * |---------|---------------|
 * | `anilist.co/anime/{id}[/{slug}]` | `onMediaClick(id)` |
 * | `anilist.co/manga/{id}[/{slug}]` | `onMediaClick(id)` |
 * | `anilist.co/forum/thread/{id}/comment/{id}` | `onThreadClick(threadId, commentId)` |
 * | `anilist.co/forum/thread/{id}[/{slug}]` | `onThreadClick(threadId, null)` |
 * | `anilist.co/character/{id}[/{slug}]` | `onCharacterClick(id)` |
 * | Everything else | `uriHandler.openUri(url)` |
 *
 * Trailing slashes, query strings, fragments, and a `www.` host prefix are tolerated on all
 * patterns — AniList's canonical share URLs end with a slash.
 *
 * Usage:
 * ```kotlin
 * val linkRouter = rememberAniLinkRouter()
 * // Then pass linkRouter::navigate as the onLinkClick callback
 * ```
 */
class AniLinkRouter(
    private val callbacks: AniLinkCallbacks,
    private val uriHandler: UriHandler
) {
    companion object {
        // Regex patterns ordered from most specific to least specific. They match against a
        // normalized URL (query/fragment stripped, trailing slash trimmed) — AniList's own
        // canonical URLs end with a slash, so matching the raw string would miss them.

        /** `anilist.co/forum/thread/123/comment/456` */
        private val THREAD_COMMENT_REGEX = Regex(
            """https?://(?:www\.)?anilist\.co/forum/thread/(\d+)/comment/(\d+)""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/forum/thread/123` or `anilist.co/forum/thread/123/some-title` */
        private val THREAD_REGEX = Regex(
            """https?://(?:www\.)?anilist\.co/forum/thread/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/anime/16498` or `anilist.co/anime/16498/attack-on-titan` */
        private val ANIME_REGEX = Regex(
            """https?://(?:www\.)?anilist\.co/anime/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/manga/30002` or `anilist.co/manga/30002/berserk` */
        private val MANGA_REGEX = Regex(
            """https?://(?:www\.)?anilist\.co/manga/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/character/40882` or `anilist.co/character/40882/levi` */
        private val CHARACTER_REGEX = Regex(
            """https?://(?:www\.)?anilist\.co/character/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/staff/95075` or `anilist.co/staff/95075/tanaka-mayumi` */
        private val STAFF_REGEX = Regex(
            """https?://(?:www\.)?anilist\.co/staff/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/user/Josh` or `anilist.co/user/Josh/stats` */
        private val USER_REGEX = Regex(
            """https?://(?:www\.)?anilist\.co/user/([^/?#]+)(?:/[^/?#]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/review/12345` or `anilist.co/review/12345/slug` */
        private val REVIEW_REGEX = Regex(
            """https?://(?:www\.)?anilist\.co/review/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/activity/98765` or `anilist.co/activity/98765/slug` */
        private val ACTIVITY_REGEX = Regex(
            """https?://(?:www\.)?anilist\.co/activity/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )
    }

    /**
     * Attempt to navigate in-app for recognized AniList URLs.
     * Falls back to the browser for all other URLs.
     */
    fun navigate(rawUrl: String) {
        val url = rawUrl.substringBefore('?').substringBefore('#').trimEnd('/')
        // Thread comment (most specific — must be checked before thread)
        THREAD_COMMENT_REGEX.find(url)?.let { match ->
            val threadId = match.groupValues[1].toIntOrNull()
            val commentId = match.groupValues[2].toIntOrNull()
            if (threadId != null && callbacks.onThreadClick != null) {
                callbacks.onThreadClick.invoke(threadId, commentId)
                return
            }
        }

        // Forum thread
        THREAD_REGEX.find(url)?.let { match ->
            val threadId = match.groupValues[1].toIntOrNull()
            if (threadId != null && callbacks.onThreadClick != null) {
                callbacks.onThreadClick.invoke(threadId, null)
                return
            }
        }

        // Anime
        ANIME_REGEX.find(url)?.let { match ->
            val mediaId = match.groupValues[1].toIntOrNull()
            if (mediaId != null && callbacks.onMediaClick != null) {
                callbacks.onMediaClick.invoke(mediaId)
                return
            }
        }

        // Manga
        MANGA_REGEX.find(url)?.let { match ->
            val mediaId = match.groupValues[1].toIntOrNull()
            if (mediaId != null && callbacks.onMediaClick != null) {
                callbacks.onMediaClick.invoke(mediaId)
                return
            }
        }

        // Character
        CHARACTER_REGEX.find(url)?.let { match ->
            val characterId = match.groupValues[1].toIntOrNull()
            if (characterId != null && callbacks.onCharacterClick != null) {
                callbacks.onCharacterClick.invoke(characterId)
                return
            }
        }

        // Staff
        STAFF_REGEX.find(url)?.let { match ->
            val staffId = match.groupValues[1].toIntOrNull()
            if (staffId != null && callbacks.onStaffClick != null) {
                callbacks.onStaffClick.invoke(staffId)
                return
            }
        }

        // Review
        REVIEW_REGEX.find(url)?.let { match ->
            val reviewId = match.groupValues[1].toIntOrNull()
            if (reviewId != null && callbacks.onReviewClick != null) {
                callbacks.onReviewClick.invoke(reviewId)
                return
            }
        }

        // Activity
        ACTIVITY_REGEX.find(url)?.let { match ->
            val activityId = match.groupValues[1].toIntOrNull()
            if (activityId != null && callbacks.onActivityClick != null) {
                callbacks.onActivityClick.invoke(activityId)
                return
            }
        }

        // User — last because username regex is greediest
        USER_REGEX.find(url)?.let { match ->
            val username = match.groupValues[1].takeIf { it.isNotBlank() }
            if (username != null && callbacks.onUserClick != null) {
                callbacks.onUserClick.invoke(username)
                return
            }
        }

        // Fallback: open in browser (the original URL — query/fragment may matter there)
        try {
            uriHandler.openUri(rawUrl)
        } catch (_: Exception) {
            // Silently ignore malformed or unresolvable URIs
        }
    }
}

/**
 * Creates and remembers an [AniLinkRouter] scoped to the current composition.
 * Reads callbacks from [LocalAniLinkCallbacks] and the browser handler from [LocalUriHandler].
 */
@Composable
fun rememberAniLinkRouter(): AniLinkRouter {
    val callbacks = LocalAniLinkCallbacks.current
    val uriHandler = LocalUriHandler.current
    return remember(callbacks, uriHandler) { AniLinkRouter(callbacks, uriHandler) }
}
