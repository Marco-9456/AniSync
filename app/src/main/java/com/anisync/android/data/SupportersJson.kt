package com.anisync.android.data

import com.anisync.android.domain.Supporter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * Reads `supporters.json`, which the Update Supporters workflow writes from Ko-fi's export and
 * GitHub Sponsors. An entry that does not make sense is dropped rather than failing the whole list.
 */
internal object SupportersJson {

    private val json = Json { ignoreUnknownKeys = true }

    private val newestFirst = compareByDescending<Supporter> { it.date }.thenBy { it.name.orEmpty() }

    /** The supporters in [text], newest first, or null when it is not a supporters file. */
    fun parse(text: String): List<Supporter>? {
        val file = runCatching { json.decodeFromString<SupportersFile>(text) }.getOrNull() ?: return null
        return file.supporters.mapNotNull { it.toSupporter() }.sortedWith(newestFirst)
    }

    @Serializable
    private class SupportersFile(val supporters: List<Entry>)

    @Serializable
    private class Entry(
        val name: String? = null,
        val date: String? = null,
        val url: String? = null,
    ) {
        fun toSupporter(): Supporter? {
            val day = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
            return Supporter(
                name = name?.trim()?.takeIf { it.isNotEmpty() },
                date = day,
                url = url?.takeIf { it.startsWith("https://") },
            )
        }
    }
}
