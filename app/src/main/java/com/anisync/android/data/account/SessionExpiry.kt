package com.anisync.android.data.account

/** An active session AniList refused, and the signed-in account the app moved to, if any. */
data class SessionExpiry(val expired: String, val switchedTo: String?)

/**
 * Marks the [active] account's session expired and moves to another signed-in account when there is
 * one, so a refused token never leaves the other accounts hidden behind the sign-in screen.
 *
 * Every request in flight fails the same way, so only the first acts. The rest find the account
 * already marked and get null, as does a call with nothing active.
 */
internal fun expireSession(
    active: Account?,
    accounts: List<Account>,
    markExpired: (id: Int, keepActive: Boolean) -> Unit,
    switchTo: (id: Int) -> Unit,
): SessionExpiry? {
    if (active == null || active.expiresAt == Account.REFUSED_AT) return null
    val fallback = accounts.firstOrNull { it.id != active.id && !it.isExpired && !it.isProvisional }
    // With somewhere to go the slot stays filled until the switch lands, so the screen never
    // passes through signed out.
    markExpired(active.id, fallback != null)
    fallback?.let { switchTo(it.id) }
    return SessionExpiry(expired = active.name, switchedTo = fallback?.name)
}
