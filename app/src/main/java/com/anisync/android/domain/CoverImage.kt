package com.anisync.android.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.anisync.android.data.CoverQuality
import kotlinx.serialization.Serializable

/**
 * Bundles every cover-image size AniList exposes, so the user's [CoverQuality]
 * preference can be applied at render time without refetching or rewriting URLs.
 *
 * AniList GraphQL field names do not match the CDN path segments — the CDN serves
 * `medium` under `/cover/small/`, `large` under `/cover/medium/`, and `extraLarge`
 * under `/cover/large/`. Keep [medium]/[large]/[extraLarge] aligned with the API
 * naming so mappers can copy fields straight across.
 *
 * AniList also returns the same URL for multiple sizes when a media item has no
 * higher-resolution variant available (e.g. `extraLarge` resolving to the same
 * `/cover/medium/...` URL as `large`). [preferred] handles that by falling back
 * to the next-smaller available URL — no client-side retry needed.
 */
@Immutable
@Serializable
data class CoverImage(
    val medium: String? = null,
    val large: String? = null,
    val extraLarge: String? = null
) {
    fun preferred(quality: CoverQuality): String? = when (quality) {
        CoverQuality.EXTRA_LARGE -> extraLarge ?: large ?: medium
        CoverQuality.LARGE -> large ?: medium ?: extraLarge
        CoverQuality.MEDIUM -> medium ?: large ?: extraLarge
    }

    companion object {
        /** Convenience for mappers: `null` if every URL is `null`, else a populated [CoverImage]. */
        fun of(medium: String?, large: String?, extraLarge: String?): CoverImage? {
            if (medium == null && large == null && extraLarge == null) return null
            val dedupLarge = if (large == medium) medium else large
            val dedupExtraLarge = if (extraLarge == dedupLarge) dedupLarge
            else if (extraLarge == medium) medium
            else extraLarge

            return CoverImage(medium, dedupLarge, dedupExtraLarge)
        }
    }
}

/**
 * Current cover-quality preference for the composition. Provided at the app root
 * from `AppSettings.coverQuality`; reads through to every Composable that renders
 * a cover so toggling the setting recomposes affected images instantly.
 */
val LocalCoverQuality = staticCompositionLocalOf { CoverQuality.LARGE }

/**
 * Picks the best URL for the current [LocalCoverQuality]. Use at AsyncImage call
 * sites: `AsyncImage(model = entry.cover.url(), ...)`.
 */
@Composable
@ReadOnlyComposable
fun CoverImage?.url(): String? = this?.preferred(LocalCoverQuality.current)