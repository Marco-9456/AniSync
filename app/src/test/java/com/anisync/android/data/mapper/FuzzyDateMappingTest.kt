package com.anisync.android.data.mapper

import com.apollographql.apollo.api.Optional
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Guards issue #85: fuzzy dates must round-trip through the UTC-anchored epoch
 * convention that Material3's DatePicker uses, regardless of the device timezone.
 */
class FuzzyDateMappingTest {

    /** Exactly what DatePicker.selectedDateMillis returns for the given day. */
    private fun datePickerMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `DatePicker millis map back to the same calendar day`() {
        // The reporter picked the 3rd and the app stored the 2nd.
        val picked = datePickerMillis(2026, 7, 3)

        val fuzzy = picked.toFuzzyDateInput()

        assertEquals(Optional.present(2026), fuzzy.year)
        assertEquals(Optional.present(7), fuzzy.month)
        assertEquals(Optional.present(3), fuzzy.day)
    }

    @Test
    fun `mapFuzzyDateToLong anchors to UTC midnight`() {
        val expected = datePickerMillis(2026, 7, 3)

        assertEquals(expected, mapFuzzyDateToLong(2026, 7, 3))
    }

    @Test
    fun `fuzzy date round-trips through epoch`() {
        val millis = mapFuzzyDateToLong(2026, 1, 1)!!

        val fuzzy = millis.toFuzzyDateInput()

        assertEquals(Optional.present(2026), fuzzy.year)
        assertEquals(Optional.present(1), fuzzy.month)
        assertEquals(Optional.present(1), fuzzy.day)
    }

    @Test
    fun `null year yields null millis`() {
        assertEquals(null, mapFuzzyDateToLong(null, 7, 3))
    }
}
