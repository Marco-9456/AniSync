package com.anisync.android.data

import com.anisync.android.data.account.AccountManager
import com.anisync.android.data.account.AccountStore
import com.anisync.android.data.account.SessionExpiry
import com.anisync.android.data.account.expireSession
import dagger.Lazy
import com.anisync.android.data.network.SessionTokens
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
 * Depends on [AccountStore] alone at construction, to stay out of the ApolloClient →
 * AuthorizationInterceptor → AuthRepository cycle. Account mutations that need the network or cache
 * clearing live in [AccountManager], which an expired session reaches through [Lazy] for that reason.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val accountManager: Lazy<AccountManager>,
) : SessionTokens {
    /** True whenever there is an active account. Drives the Login ↔ Main swap in MainActivity. */
    val isLoggedIn: Flow<Boolean> = accountStore.activeAccount.map { it != null }

    /**
     * Emitted when the API refuses the active account's token (expired or revoked). The UI shows a
     * "session expired" dialog naming the account, and the account it moved to when there was one.
     */
    private val _sessionExpired = MutableSharedFlow<SessionExpiry>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<SessionExpiry> = _sessionExpired.asSharedFlow()

    /** Active account's bearer token, or null when logged out. Read per-request by the interceptor. */
    override fun getToken(): String? = accountStore.activeToken()

    /**
     * Called by the interceptor when the active token is refused. Marks **only the active** account
     * expired and moves to another signed-in account, or to sign in when there is none, then emits
     * the session-expired event.
     */
    override fun onSessionExpired() {
        val expiry = expireSession(
            active = accountStore.activeAccount.value,
            accounts = accountStore.accounts.value,
            markExpired = accountStore::markExpired,
            switchTo = { accountManager.get().switch(it) },
        ) ?: return
        _sessionExpired.tryEmit(expiry)
    }
}
