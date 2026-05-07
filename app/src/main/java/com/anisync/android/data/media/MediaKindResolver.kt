package com.anisync.android.data.media

import com.anisync.android.domain.media.MediaKind

internal fun mediaKindFromMime(mime: String): MediaKind = when {
    mime.equals("image/gif", ignoreCase = true) -> MediaKind.Gif
    mime.startsWith("video/", ignoreCase = true) -> MediaKind.Video
    else -> MediaKind.Image
}
