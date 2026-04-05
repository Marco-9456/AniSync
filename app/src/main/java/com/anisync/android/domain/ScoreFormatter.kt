package com.anisync.android.domain

enum class ScoreFormat {
    POINT_100,      // 0-100 integer
    POINT_10_DECIMAL, // 0-10 with 1 decimal (e.g., "7.5")
    POINT_10,       // 0-10 integer
    POINT_5,        // 0-5 stars
    POINT_3         // 0-3 smileys
}

/**
 * Formats a given score (usually out of 10 or 100 depending on how it's stored in the UI)
 * into a string representation based on the user's selected score format.
 */
fun formatScore(score: Double?, format: ScoreFormat): String {
    if (score == null || score == 0.0) return "No score"
    return when (format) {
        ScoreFormat.POINT_100 -> score.toInt().toString()
        ScoreFormat.POINT_10_DECIMAL -> String.format("%.1f", score)
        ScoreFormat.POINT_10 -> score.toInt().toString()
        ScoreFormat.POINT_5 -> "★".repeat(score.toInt().coerceIn(0, 5))
        ScoreFormat.POINT_3 -> when {
            score >= 3.0 -> ":)"
            score >= 2.0 -> ":|"
            score >= 1.0 -> ":("
            else -> "–"
        }
    }
}
