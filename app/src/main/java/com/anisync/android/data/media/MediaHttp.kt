package com.anisync.android.data.media

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttp clients for *downloading* user-embedded media (images via Coil,
 * videos via ExoPlayer). Both reuse one identity decision:
 *
 * **Browser User-Agent.** Embedded media is hosted on third-party CDNs (catbox.moe,
 * imgur, Discord, AniList's own CDN). Several of them bot-filter the default
 * `okhttp/x.y.z` / `ExoPlayerLib` User-Agent and answer `403`/`412` — surfacing as
 * "image won't load" or ExoPlayer's `ERROR_CODE_IO_BAD_HTTP_STATUS`
 * ("This video is no longer available") for a file that opens fine in a browser.
 * [com.anisync.android.di.MediaUploadModule] already learned this for catbox *uploads*;
 * the same hosts serve the `webm(...)`/`img(...)` links users paste into posts, so the
 * download path needs the same treatment. A real browser UA is accepted everywhere.
 *
 * Two clients because images and video want different transports:
 * - [imageClient] keeps HTTP/2 — AniList's CDN multiplexes a whole grid of covers over
 *   one connection, so pinning HTTP/1.1 would slow first paint.
 * - [videoClient] pins HTTP/1.1 — catbox's HTTP/2 edge intermittently RSTs streams
 *   (`PROTOCOL_ERROR`), which mid-playback reads turn into spurious playback errors.
 */
object MediaHttp {

    /** Recent Chrome-on-Android UA — universally accepted, unlike the library defaults. */
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request()
        val withUa = if (request.header("User-Agent") == null) {
            request.newBuilder().header("User-Agent", USER_AGENT).build()
        } else {
            request
        }
        chain.proceed(withUa)
    }

    /**
     * Client for Coil image requests. HTTP/2 retained for CDN multiplexing; timeouts a
     * touch longer than OkHttp defaults so a slow-but-alive mobile connection finishes the
     * fetch instead of failing a poster permanently (Coil does not auto-retry a hard fail).
     */
    val imageClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Client for ExoPlayer's data source. Pinned to HTTP/1.1 (see class doc) with a longer
     * read timeout to ride out streaming stalls instead of erroring the player.
     */
    val videoClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor(userAgentInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
