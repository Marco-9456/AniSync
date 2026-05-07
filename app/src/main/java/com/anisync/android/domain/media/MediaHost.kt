package com.anisync.android.domain.media

/**
 * Third-party services used to host user-uploaded media (images, GIFs, videos)
 * since AniList itself has no upload endpoint. The chosen URL is then embedded
 * in body markdown via AniList tags like `img(url)` or `webm(url)`.
 *
 * Persisted by name so reordering doesn't shift saved values.
 */
enum class MediaHost {
    /** catbox.moe — permanent free hosting, 200 MB / file, no key. */
    CATBOX,

    /** litterbox.catbox.moe — temporary (1 hour). */
    LITTERBOX_1H,

    /** litterbox.catbox.moe — temporary (24 hours). */
    LITTERBOX_24H,

    /** litterbox.catbox.moe — temporary (72 hours). */
    LITTERBOX_72H,

    /** imgur.com anonymous upload — needs a Client-ID. */
    IMGUR,

    /** User-supplied multipart endpoint. */
    CUSTOM
}
