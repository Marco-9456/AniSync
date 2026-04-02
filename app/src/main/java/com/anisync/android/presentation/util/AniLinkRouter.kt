package com.anisync.android.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
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
    val onCharacterClick: ((characterId: Int) -> Unit)? = null
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
        // Regex patterns ordered from most specific to least specific.

        /** `anilist.co/forum/thread/123/comment/456` */
        private val THREAD_COMMENT_REGEX = Regex(
            """https?://anilist\.co/forum/thread/(\d+)/comment/(\d+)""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/forum/thread/123` or `anilist.co/forum/thread/123/some-title` */
        private val THREAD_REGEX = Regex(
            """https?://anilist\.co/forum/thread/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/anime/16498` or `anilist.co/anime/16498/attack-on-titan` */
        private val ANIME_REGEX = Regex(
            """https?://anilist\.co/anime/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/manga/30002` or `anilist.co/manga/30002/berserk` */
        private val MANGA_REGEX = Regex(
            """https?://anilist\.co/manga/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )

        /** `anilist.co/character/40882` or `anilist.co/character/40882/levi` */
        private val CHARACTER_REGEX = Regex(
            """https?://anilist\.co/character/(\d+)(?:/[^/]*)?$""",
            RegexOption.IGNORE_CASE
        )
    }

    /**
     * Attempt to navigate in-app for recognized AniList URLs.
     * Falls back to the browser for all other URLs.
     */
    fun navigate(url: String) {
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

        // Fallback: open in browser
        try {
            uriHandler.openUri(url)
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
    return AniLinkRouter(callbacks, uriHandler)
}
