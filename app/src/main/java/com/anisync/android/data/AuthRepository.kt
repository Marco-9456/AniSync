package com.anisync.android.data

import com.anisync.android.data.account.AccountStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth facade over [AccountStore]. Kept as the single dependency for the auth/session surface
 * ([com.anisync.android.di.AuthorizationInterceptor], the notification worker, [com.anisync.android.MainActivity])
 * so those call sites are unaffected by multi-account support.
 *
 * Intentionally depends **only** on [AccountStore] (no Apollo/Room) to stay out of the
 * ApolloClient → AuthorizationInterceptor → AuthRepository cycle. Account mutations that need the
 * network or cache clearing live in [com.anisync.android.data.account.AccountManager].
 */
@Singleton
class AuthRepository @Inject constructor(
    private val accountStore: AccountStore,
) {
    /** True whenever there is an active account. Drives the Login ↔ Main swap in MainActivity. */
    val isLoggedIn: Flow<Boolean> = accountStore.activeAccount.map { it != null }

    /**
     * Emitted when the API returns HTTP 401 for the active account (token expired/revoked).
     * The UI collects this to show a "session expired" dialog and drop to the login/account picker.
     */
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /** Active account's bearer token, or null when logged out. Read per-request by the interceptor. */
    fun getToken(): String? = accountStore.activeToken()

    /**
     * Called by the interceptor on a 401. Marks **only the active** account expired and clears the
     * active slot (other accounts are kept), then emits the session-expired event.
     */
    fun onSessionExpired() {
        accountStore.markActiveExpired()
        _sessionExpired.tryEmit(Unit)
    }
}
