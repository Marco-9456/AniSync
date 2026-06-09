package com.anisync.android.data

import com.anisync.android.data.account.AccountManager
import com.anisync.android.domain.SyncUserOptionsUseCase
import com.anisync.android.domain.UserOptionsRepository
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the network side of the local-first options sync. Started once from the Application:
 *  - **Pull** fresh options whenever a (non-null) account becomes active — cold start with a signed-in
 *    account and after every account add/switch — so the app respects the latest web options.
 *  - **Flush** any pending local edits when the app goes to the background (a natural "user stopped
 *    interacting" point), complementing the per-edit debounce and the WorkManager safety net.
 */
@Singleton
class UserOptionsSyncManager @Inject constructor(
    private val accountManager: AccountManager,
    private val syncUserOptions: SyncUserOptionsUseCase,
    private val userOptionsRepository: UserOptionsRepository,
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

        // Lifecycle observers must be registered on the main thread.
        scope.launch(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        scope.launch { userOptionsRepository.flush() }
                    }
                }
            )
        }
    }
}
