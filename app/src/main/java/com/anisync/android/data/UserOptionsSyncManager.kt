package com.anisync.android.data

import com.anisync.android.data.account.AccountManager
import com.anisync.android.domain.SyncUserOptionsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the network pull of AniList account options. Started once from the Application; pulls fresh
 * options whenever a (non-null) account becomes active — i.e. on cold start with a signed-in account
 * and after every account add/switch — so the app respects the latest web options (fixes the activity
 * feed leaking NSFW content blocked on the account).
 */
@Singleton
class UserOptionsSyncManager @Inject constructor(
    private val accountManager: AccountManager,
    private val syncUserOptions: SyncUserOptionsUseCase,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            accountManager.activeAccount
                .map { it?.id }
                .distinctUntilChanged()
                .collect { id ->
                    if (id != null) syncUserOptions()
                }
        }
    }
}
