package com.anisync.android.presentation.calendar

import android.content.Context
import org.robolectric.RuntimeEnvironment
import com.anisync.android.data.anisyncplus.AniSyncPlusSettings
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CalendarPolicyTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @After
    fun clearPreferences() {
        context.getSharedPreferences(AniSyncPlusSettings.PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun calendarDefaultsEnabledWithFilterMemoryDisabled() {
        val settings = AniSyncPlusSettings(context)

        assertTrue(settings.aniWorldCalendarEnabled.value)
        assertFalse(settings.rememberCalendarFilter.value)
        assertFalse(settings.calendarFollowingOnly.value)
    }

    @Test
    fun followingFilterPersistsOnlyWhileMemoryEnabled() {
        val settings = AniSyncPlusSettings(context)
        settings.setRememberCalendarFilter(true)
        settings.setCalendarFollowingOnly(true)

        assertTrue(AniSyncPlusSettings(context).calendarFollowingOnly.value)

        settings.setRememberCalendarFilter(false)
        assertFalse(settings.calendarFollowingOnly.value)
        assertFalse(AniSyncPlusSettings(context).calendarFollowingOnly.value)
    }

    @Test
    fun navigationRequiresOverlapAndAcceptsPartialOverlap() {
        val availableStart = LocalDate.of(2026, 7, 15)
        val availableEnd = LocalDate.of(2026, 7, 20)

        assertFalse(
            rangeOverlaps(
                LocalDate.of(2026, 7, 8),
                LocalDate.of(2026, 7, 14),
                availableStart,
                availableEnd
            )
        )
        assertTrue(
            rangeOverlaps(
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 7, 19),
                availableStart,
                availableEnd
            )
        )
        assertFalse(rangeOverlaps(availableStart, availableEnd, null, null))
    }
}
