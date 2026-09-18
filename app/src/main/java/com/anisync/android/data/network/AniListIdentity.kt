package com.anisync.android.data.network

import android.os.Build
import com.anisync.android.BuildConfig

/**
 * How AniSync identifies itself to AniList.
 *
 * AniList's own guidance is that abusive clients get their IP blocked by hand, so being
 * recognisable is worth more here than blending in. That is the opposite of the call made for
 * third-party media CDNs in [com.anisync.android.data.media.MediaHttp], which send a browser
 * User-Agent because those hosts bot-filter library defaults.
 *
 * `Referer` is sent because AniList sits behind Cloudflare and at least one other client has seen
 * requests without it answered with a 403 from some regions. It costs nothing and the API does not
 * mind it.
 */
object AniListIdentity {

    const val ENDPOINT = "https://graphql.anilist.co"

    /** Matches the shape already used for animethemes.moe and for catbox uploads. */
    val USER_AGENT: String =
        "AniSync/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.SDK_INT}; " +
            "+https://github.com/Marco-9456/AniSync)"

    const val REFERER = "https://anilist.co"

    const val HEADER_USER_AGENT = "User-Agent"
    const val HEADER_REFERER = "Referer"
    const val HEADER_AUTHORIZATION = "Authorization"

    const val HEADER_RATE_LIMIT = "x-ratelimit-limit"
    const val HEADER_RATE_REMAINING = "x-ratelimit-remaining"
    const val HEADER_RETRY_AFTER = "retry-after"
}
