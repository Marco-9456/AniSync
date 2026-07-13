package de.mrxxxxx.anisyncplus.calendar.parser

import java.text.Normalizer
import java.util.Locale

object TitleNormalizer {
    private val seasonPattern = Regex("\\b(?:season|staffel)\\s*([ivxlcdm]+|\\d+)\\b", RegexOption.IGNORE_CASE)
    private val punctuation = Regex("[^\\p{L}\\p{N}]+")
    private val whitespace = Regex("\\s+")

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace('’', '\'')
        .replace('‘', '\'')
        .replace('–', '-')
        .replace('—', '-')
        .replace(seasonPattern) { match -> " season ${romanOrArabic(match.groupValues[1])} " }
        .replace(punctuation, " ")
        .replace(whitespace, " ")
        .trim()

    private fun romanOrArabic(value: String): String = value.toIntOrNull()?.toString()
        ?: romanToInt(value.uppercase(Locale.ROOT))?.toString()
        ?: value.lowercase(Locale.ROOT)

    private fun romanToInt(value: String): Int? {
        val values = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var total = 0
        var previous = 0
        for (character in value.reversed()) {
            val current = values[character] ?: return null
            total += if (current < previous) -current else current
            previous = current
        }
        return total.takeIf { it > 0 }
    }
}
