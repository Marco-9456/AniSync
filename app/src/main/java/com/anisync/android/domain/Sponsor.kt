package com.anisync.android.domain

import androidx.annotation.StringRes
import com.anisync.android.R
import kotlinx.serialization.Serializable

@Serializable
data class Sponsor(
    val login: String,
    val name: String,
    val avatarUrl: String,
    val url: String,
    val tier: Int   // monthly USD: 5, 10, 20, ...
)

@Serializable
data class SponsorList(
    val updatedAt: String,
    val sponsors: List<Sponsor>
)

enum class SponsorTier(
    val minDollars: Int,
    @StringRes val label: Int
) {
    GENEROUS(20, R.string.sponsor_tier_generous),
    BACKER(10, R.string.sponsor_tier_backer);

    companion object {
        fun forAmount(monthlyDollars: Int): SponsorTier? = entries
            .sortedByDescending { it.minDollars }
            .firstOrNull { monthlyDollars >= it.minDollars }
    }
}
