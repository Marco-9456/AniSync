package com.anisync.android.domain.media

/** Coarse classification used to pick the right AniList tag (`img(...)` vs `webm(...)`). */
enum class MediaKind { Image, Gif, Video }

data class UploadedMedia(
    val url: String,
    val mime: String,
    val kind: MediaKind
)
