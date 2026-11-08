package com.anisync.android.presentation.settings

import com.anisync.android.data.CardSize
import com.anisync.android.data.RecapDelivery
import java.time.LocalTime

data class RecapsSettingsUiState(
    val monthly: Boolean = true,
    val yearly: Boolean = true,
    val includeManga: Boolean = true,
    val countRepeats: Boolean = false,
    val showAdult: Boolean = false,
    /** The account's adult content setting, which names adult titles in recaps on its own. */
    val adultContent: Boolean = false,
    val themeSong: Boolean = true,
    val songsOnMobileData: Boolean = false,
    val delivery: RecapDelivery = RecapDelivery.NOTIFICATION_AND_BANNER,
    /** Settings > Notifications "Allow Notifications", without which no recap is announced. */
    val notificationsAllowed: Boolean = true,
    val time: LocalTime = LocalTime.of(9, 0),
    val hideScoresOnCards: Boolean = false,
    val defaultCardSize: CardSize = CardSize.STORY,
    /** Finished months with a recap on this device, null until the stored recaps are counted. */
    val storedMonths: Int? = null,
    val storedBytes: Long = 0L,
) {
    /** What actually happens when a recap is ready, a notification needing Allow Notifications. */
    val effectiveDelivery: RecapDelivery
        get() = if (!notificationsAllowed && delivery == RecapDelivery.NOTIFICATION_AND_BANNER) {
            RecapDelivery.BANNER_ONLY
        } else {
            delivery
        }
}

sealed interface RecapsSettingsAction {
    data class SetMonthly(val enabled: Boolean) : RecapsSettingsAction
    data class SetYearly(val enabled: Boolean) : RecapsSettingsAction
    data class SetIncludeManga(val enabled: Boolean) : RecapsSettingsAction
    data class SetCountRepeats(val enabled: Boolean) : RecapsSettingsAction
    data class SetShowAdult(val enabled: Boolean) : RecapsSettingsAction
    data class SetThemeSong(val enabled: Boolean) : RecapsSettingsAction
    data class SetSongsOnMobileData(val enabled: Boolean) : RecapsSettingsAction
    data class SetDelivery(val delivery: RecapDelivery) : RecapsSettingsAction
    data class SetTime(val time: LocalTime) : RecapsSettingsAction
    data class SetHideScoresOnCards(val hidden: Boolean) : RecapsSettingsAction
    data class SetDefaultCardSize(val size: CardSize) : RecapsSettingsAction
    data object ClearStored : RecapsSettingsAction
}
