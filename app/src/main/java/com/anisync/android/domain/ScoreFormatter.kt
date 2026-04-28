package com.anisync.android.domain

enum class ScoreFormat {
    POINT_100,      // 0-100 integer
    POINT_10_DECIMAL, // 0-10 with 1 decimal (e.g., "7.5")
    POINT_10,       // 0-10 integer
    POINT_5,        // 0-5 stars
    POINT_3         // 0-3 smileys
}

private val STAR_STRINGS = arrayOf("", "★", "★★", "★★★", "★★★★", "★★★★★")

private fun formatOneDecimal(score: Double): String {
    val scaled = Math.round(score * 10.0)
    val whole = scaled / 10
    val frac = (scaled % 10).let { if (it < 0) -it else it }
    return "$whole.$frac"
}

fun formatScore(score: Double?, format: ScoreFormat): String {
    if (score == null || score == 0.0) return "No score"
    return when (format) {
        ScoreFormat.POINT_100 -> score.toInt().toString()
        ScoreFormat.POINT_10_DECIMAL -> formatOneDecimal(score)
        ScoreFormat.POINT_10 -> score.toInt().toString()
        ScoreFormat.POINT_5 -> STAR_STRINGS[score.toInt().coerceIn(0, 5)]
        ScoreFormat.POINT_3 -> when {
            score >= 3.0 -> ":)"
            score >= 2.0 -> ":|"
            score >= 1.0 -> ":("
            else -> "–"
        }
    }
}
