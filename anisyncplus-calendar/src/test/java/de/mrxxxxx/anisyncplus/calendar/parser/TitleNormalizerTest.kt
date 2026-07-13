package de.mrxxxxx.anisyncplus.calendar.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleNormalizerTest {
    @Test
    fun `normalizes unicode apostrophes dashes punctuation and whitespace`() {
        assertEquals(
            "cafe d art the show",
            TitleNormalizer.normalize("  Café D’Art — The.Show!  ")
        )
    }

    @Test
    fun `normalizes roman and Arabic season notation consistently`() {
        assertEquals(
            TitleNormalizer.normalize("Example Season II"),
            TitleNormalizer.normalize("Example Staffel 2")
        )
    }

    @Test
    fun `does not blindly remove years or title suffixes`() {
        assertEquals("example 2026 rebirth", TitleNormalizer.normalize("Example 2026: Rebirth"))
    }
}
