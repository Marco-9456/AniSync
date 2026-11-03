package com.anisync.android.presentation.settings

import com.anisync.android.data.RecapDelivery
import org.junit.Assert.assertEquals
import org.junit.Test

class RecapsSettingsUiStateTest {

    @Test
    fun notificationAndBanner_readsAsBannerOnly_whileNotificationsAreOff() {
        val state = RecapsSettingsUiState(delivery = RecapDelivery.NOTIFICATION_AND_BANNER, notificationsAllowed = false)
        assertEquals(RecapDelivery.BANNER_ONLY, state.effectiveDelivery)
    }

    @Test
    fun notificationAndBanner_staysChosen_whileNotificationsAreOn() {
        val state = RecapsSettingsUiState(delivery = RecapDelivery.NOTIFICATION_AND_BANNER, notificationsAllowed = true)
        assertEquals(RecapDelivery.NOTIFICATION_AND_BANNER, state.effectiveDelivery)
    }

    @Test
    fun bannerOnlyAndNowhere_ignoreTheNotificationSwitch() {
        for (delivery in listOf(RecapDelivery.BANNER_ONLY, RecapDelivery.NOWHERE)) {
            for (allowed in listOf(true, false)) {
                assertEquals(delivery, RecapsSettingsUiState(delivery = delivery, notificationsAllowed = allowed).effectiveDelivery)
            }
        }
    }
}
