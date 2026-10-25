package com.anisync.android.domain

import com.anisync.android.type.MediaSeason
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MediaSeasonsTest {

    @Test
    fun `December belongs to Fall`() {
        assertEquals(MediaSeason.FALL, seasonOfMonth(Calendar.DECEMBER))
    }

    @Test
    fun `each quarter maps to its season`() {
        assertEquals(MediaSeason.WINTER, seasonOfMonth(Calendar.JANUARY))
        assertEquals(MediaSeason.WINTER, seasonOfMonth(Calendar.MARCH))
        assertEquals(MediaSeason.SPRING, seasonOfMonth(Calendar.APRIL))
        assertEquals(MediaSeason.SPRING, seasonOfMonth(Calendar.JUNE))
        assertEquals(MediaSeason.SUMMER, seasonOfMonth(Calendar.JULY))
        assertEquals(MediaSeason.SUMMER, seasonOfMonth(Calendar.SEPTEMBER))
        assertEquals(MediaSeason.FALL, seasonOfMonth(Calendar.OCTOBER))
    }
}
