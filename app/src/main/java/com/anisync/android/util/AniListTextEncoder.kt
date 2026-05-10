package com.anisync.android.util

/**
 * AniList stores post bodies in a `utf8` (3-byte) MySQL column, so a 4-byte UTF-8
 * codepoint silently truncates the rest of the post on save. The web client side-
 * steps this by emitting decimal HTML entities for any codepoint above U+FFFF.
 *
 * Pure and idempotent on already-encoded input (entities are pure ASCII). BMP
 * characters (CJK, Cyrillic, Arabic, etc.) pass through untouched.
 */
internal object AniListTextEncoder {
    fun encodeForAniList(input: String): String {
        if (input.isEmpty() || input.none { Character.isHighSurrogate(it) }) return input
        val out = StringBuilder(input.length + 16)
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            if (cp > 0xFFFF) {
                out.append("&#").append(cp).append(';')
            } else {
                out.append(input, i, i + Character.charCount(cp))
            }
            i += Character.charCount(cp)
        }
        return out.toString()
    }
}
