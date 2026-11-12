package com.anisync.android.presentation.settings

import com.anisync.android.domain.Supporter
import java.util.Locale

data class SupportUiState(
    /** Null until the bundled list has been read. */
    val supporters: List<Supporter>? = null,
    val remindersEnabled: Boolean = true,
)

sealed interface SupportAction {
    /** Ko-fi or GitHub Sponsors was opened from the page. */
    data object TipOpened : SupportAction
    data class SetRemindersEnabled(val enabled: Boolean) : SupportAction
}

/** The first character of the name in upper case, or null for an anonymous tip. */
internal fun Supporter.initial(locale: Locale): String? {
    val trimmed = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).uppercase(locale)
}
