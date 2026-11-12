package com.anisync.android.presentation.settings

import com.anisync.android.domain.Supporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class SupporterInitialTest {

    private fun initial(name: String?, locale: Locale = Locale.ENGLISH) =
        Supporter(name = name, date = LocalDate.of(2026, 11, 12)).initial(locale)

    @Test
    fun firstLetterInUpperCase() {
        assertEquals("E", initial("ein Syndication"))
        assertEquals("N", initial("NGE"))
    }

    @Test
    fun leadingSpaceIsSkipped() {
        assertEquals("W", initial("  warren"))
    }

    @Test
    fun aCharacterOutsideTheBasicPlaneStaysWhole() {
        assertEquals("🍵", initial("🍵 tea"))
    }

    @Test
    fun upperCaseFollowsTheLocale() {
        assertEquals("İ", initial("ilker", Locale.forLanguageTag("tr")))
    }

    @Test
    fun anonymousHasNoInitial() {
        assertNull(initial(null))
    }
}
