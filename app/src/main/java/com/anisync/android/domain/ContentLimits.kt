package com.anisync.android.domain

/**
 * Server-enforced character bounds per content type.
 *
 * Sourced from AniList GraphQL API constraints (provided by AniList devs ahead of
 * official documentation). Use these to pre-validate user input so we don't burn
 * a network round-trip on rejections.
 *
 * Bounds are inclusive: text is valid iff `length in min..max`.
 */
object ContentLimits {
    val TextActivity = Bounds(min = 5, max = 10_000)
    val MessageActivity = Bounds(min = 2, max = 10_000)
    val ThreadTitle = Bounds(min = 6, max = 120)
    val ThreadBody = Bounds(min = 0, max = 30_000)
    val ThreadComment = Bounds(min = 1, max = 12_000)
    val Reply = Bounds(min = 2, max = 8_000)

    // AniList review constraints: body min 2200 chars, summary 20..120 chars,
    // score 0..100. Body has no documented upper bound — cap high so the editor
    // never falsely rejects a long review.
    val ReviewBody = Bounds(min = 2_200, max = 200_000)
    val ReviewSummary = Bounds(min = 20, max = 120)
    val ReviewScore = Bounds(min = 0, max = 100)

    data class Bounds(val min: Int, val max: Int) {
        fun isValid(length: Int) = length in min..max
    }
}
