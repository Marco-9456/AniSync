package com.anisync.android

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.account.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds a cross-account deep link while the switch settles. It lives here rather than on the
 * activity because a cold start can recreate MainActivity (AppCompat applies a per-app locale that
 * way below API 33), which would cancel the wait and drop the link along with the old instance.
 */
@HiltViewModel
class AccountDeepLinkViewModel @Inject constructor(
    private val accountManager: AccountManager
) : ViewModel() {

    private val _pending = MutableStateFlow<PendingDeepLink?>(null)
    val pending: StateFlow<PendingDeepLink?> = _pending.asStateFlow()

    fun switchThenOpen(accountId: Int, uri: Uri) {
        val epochBefore = accountManager.sessionEpoch.value
        viewModelScope.launch {
            accountManager.switch(accountId)
            val newEpoch = accountManager.sessionEpoch.first { it != epochBefore }
            // Without these flags NavController pops the whole graph and leaves the target alone on
            // the stack, so back would leave the app instead of returning to the start tab.
            val intent = Intent(Intent.ACTION_VIEW, uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            _pending.value = PendingDeepLink(intent, newEpoch)
        }
    }

    fun consume() {
        _pending.value = null
    }
}
