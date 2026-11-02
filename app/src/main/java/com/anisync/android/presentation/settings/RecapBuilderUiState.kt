package com.anisync.android.presentation.settings

import com.anisync.android.domain.RecapPreview
import com.anisync.android.domain.StoredRecap

/**
 * Developer Tools' recap builder: a recap for any public user, built the way a first build would
 * and never stored, beside the signed-in account's stored recaps and the job that builds them.
 */
data class RecapBuilderUiState(
    val userId: String = "",
    val period: String = "",
    val zone: String = "",
    val isBuilding: Boolean = false,
    /** True when the user id, period or zone cannot be read. */
    val inputError: Boolean = false,
    val preview: RecapPreview? = null,
    val buildError: String? = null,
    val stored: List<StoredRecap> = emptyList(),
    val includeManga: Boolean = true,
    val countRepeats: Boolean = false,
)

sealed interface RecapBuilderAction {
    data class SetUserId(val value: String) : RecapBuilderAction
    data class SetPeriod(val value: String) : RecapBuilderAction
    data class SetZone(val value: String) : RecapBuilderAction
    data object Build : RecapBuilderAction
    data object RunJob : RecapBuilderAction
    data object ClearStored : RecapBuilderAction
    data class SetIncludeManga(val enabled: Boolean) : RecapBuilderAction
    data class SetCountRepeats(val enabled: Boolean) : RecapBuilderAction
}
