package de.mrxxxxx.anisyncplus.calendar.parser

sealed class AniWorldParseException(message: String) : Exception(message) {
    class BlockPage(reason: String) :
        AniWorldParseException("AniWorld returned a block or challenge page ($reason)")
    class MissingContainer : AniWorldParseException("Expected #seriesContainer is missing")
    class MissingDaySections : AniWorldParseException("Expected section.calendarList elements are missing")
    class InvalidDate(value: String) : AniWorldParseException("Invalid calendar date: $value")
    class InvalidTime(value: String) : AniWorldParseException("Invalid calendar time: $value")
}
