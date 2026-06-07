package com.anisync.android.presentation.settings

import androidx.lifecycle.ViewModel
import com.anisync.android.data.account.Account
import com.anisync.android.data.account.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Drives the account list (add / switch / remove / logout). All mutations are fire-and-forget into
 * [AccountManager], which runs them on its own scope and rebuilds the UI via the session epoch — so
 * this ViewModel being disposed by that rebuild can't interrupt the operation.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountManager.accounts
    val activeAccount: StateFlow<Account?> = accountManager.activeAccount

    fun switch(id: Int) = accountManager.switch(id)

    fun remove(id: Int) = accountManager.removeAccount(id)

    fun logoutActive() = accountManager.logoutActive()
}
