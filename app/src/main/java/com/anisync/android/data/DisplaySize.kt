package com.anisync.android.data

/**
 * How large AniSync draws everything, on top of the phone's own display and font size.
 *
 * Capped at 1.2 because a 411dp phone keeps 343dp of layout width there, and 1.3 would push it
 * under 320dp, where the Library rail and the queue rows start to truncate.
 */
enum class DisplaySize(val scale: Float) {
    SMALL(0.9f),
    DEFAULT(1f),
    LARGE(1.1f),
    LARGER(1.2f)
}
