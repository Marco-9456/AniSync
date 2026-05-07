package com.anisync.android.domain.media

/**
 * Display-size variants for embedded images. Maps to AniList's `imgN`, `imgN%`,
 * and bare `img` markdown forms (see `RichTextInlineParser.imageMdRegex`).
 *
 * Videos always emit `webm(url)` regardless — AniList ignores size hints there.
 */
sealed interface MediaSizeChoice {
    data object Small : MediaSizeChoice
    data object Medium : MediaSizeChoice
    data object Large : MediaSizeChoice
    data object Original : MediaSizeChoice
    data class CustomPx(val pixels: Int) : MediaSizeChoice
    data class CustomPercent(val percent: Int) : MediaSizeChoice

    companion object {
        val Default: MediaSizeChoice = Medium
    }
}

/** AniList markdown for an image, with size baked in per the user's choice. */
fun MediaSizeChoice.toImageMarkdown(url: String): String = when (this) {
    MediaSizeChoice.Small -> "img150($url)"
    MediaSizeChoice.Medium -> "img300($url)"
    MediaSizeChoice.Large -> "img50%($url)"
    MediaSizeChoice.Original -> "img($url)"
    is MediaSizeChoice.CustomPx -> "img${pixels}($url)"
    is MediaSizeChoice.CustomPercent -> "img${percent}%($url)"
}

/** AniList markdown for video content. Size is not honored by the renderer. */
fun videoMarkdown(url: String): String = "webm($url)"
