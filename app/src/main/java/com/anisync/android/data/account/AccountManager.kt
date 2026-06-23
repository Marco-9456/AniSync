package com.anisync.android.data.account

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.anisync.android.GetViewerQuery
import com.anisync.android.data.AppSettings
import com.anisync.android.data.NotificationBadgeStore
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.SavedForumThreadDao
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.PreferencesRepository
import com.anisync.android.widget.AiringTodayWidget
import com.anisync.android.widget.UpNextWidget
import com.anisync.android.widget.WeeklyCalendarWidget
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.apolloStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates account add / switch / remove and the local-state reset that makes switching safe.
 *
 * Switching is "reset + rebuild": local stores for the outgoing account are cleared (Apollo
 * normalized cache, account-scoped Room tables, notification dedup, per-account prefs, in-memory
 * caches), then the new account is activated and [sessionEpoch] is bumped. The UI keys the whole
 * `MainScreen` subtree on [sessionEpoch], so a bump tears down the NavController and every screen
 * ViewModel and rebuilds them — they then refetch the new account's data from network. `recreate()`
 * is intentionally NOT used: it preserves the ViewModelStore, so surviving ViewModels would sit on
 * the freshly-cleared (empty) caches and never refetch.
 *
 * Mutations run on an internal [scope] (not a caller's viewModelScope) so the subtree rebuild they
 * trigger can't cancel them mid-flight and strand the busy loader.
 */
@Singleton
class AccountManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountStore: AccountStore,
    private val apolloClient: ApolloClient,
    private val libraryDao: LibraryDao,
    private val savedForumThreadDao: SavedForumThreadDao,
    private val airingScheduleDao: AiringScheduleDao,
    private val preferencesRepository: PreferencesRepository,
    private val appSettings: AppSettings,
    private val notificationBadgeStore: NotificationBadgeStore,
    private val activityRepository: ActivityRepository,
    private val tokenedClientFactory: TokenedApolloClientFactory,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val accounts: StateFlow<List<Account>> get() = accountStore.accounts
    val activeAccount: StateFlow<Account?> get() = accountStore.activeAccount

    /**
     * Increments on every explicit active-account change (switch / add-new / remove-active / logout).
     * The UI keys `MainScreen` on this to force a full ViewModel rebuild + refetch. A silent identity
     * reconcile of the same session (see [reconcileActiveIfProvisional]) does NOT bump it.
     */
    private val _sessionEpoch = MutableStateFlow(0)
    val sessionEpoch: StateFlow<Int> = _sessionEpoch.asStateFlow()

    /** True while an add/switch/remove is in flight, so the UI can show a blocking loader. */
    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    sealed interface AddResult {
        data class Success(val accountId: Int) : AddResult
        data object Failed : AddResult
    }

    /**
     * Resolves the token's identity, stores the account, and makes it active (clearing the previous
     * account's local state first) unless it is a re-auth of the already-active account. Used for
     * both first login and adding a second account.
     *
     * Suspends so the caller (MainActivity) can surface a failure; runs on the activity's
     * lifecycleScope, which survives the subtree rebuild because the activity itself is not recreated.
     */
    suspend fun addAccount(token: String, expiresInSeconds: Long): AddResult = busy {
        val resolved = resolveAccount(token, expiresInSeconds) ?: return@busy AddResult.Failed
        val wasActive = accountStore.activeAccount.value?.id == resolved.id
        accountStore.addOrReplace(resolved)
        if (!wasActive) {
            clearLocalState()
            accountStore.switchTo(resolved.id)
            bumpEpoch()
            scope.launch { refreshWidgets() }
        }
        AddResult.Success(resolved.id)
    }

    /** Switches the active account (clears the outgoing account's local state first). Fire-and-forget. */
    fun switch(id: Int) {
        if (accountStore.activeAccount.value?.id == id) return
        scope.launch {
            busy {
                clearLocalState()
                accountStore.switchTo(id)
                bumpEpoch()
            }
            refreshWidgets()
        }
    }

    /**
     * Removes an account. If it is the active one, switches to another (or to "none") first and
     * resets local state; otherwise just drops it (the active account is untouched). Fire-and-forget.
     */
    fun removeAccount(id: Int) {
        scope.launch {
            busy {
                val token = accountStore.accounts.value.firstOrNull { it.id == id }?.token
                if (accountStore.activeAccount.value?.id == id) {
                    val next = accountStore.accounts.value.firstOrNull { it.id != id }
                    clearLocalState()
                    accountStore.switchTo(next?.id)
                    accountStore.remove(id)
                    bumpEpoch()
                    scope.launch { refreshWidgets() }
                } else {
                    accountStore.remove(id)
                }
                // Drop the removed account's notification dedup state and cached token client.
                preferencesRepository.clearForAccount(id)
                token?.let(tokenedClientFactory::evict)
            }
        }
    }

    /** Logs out the active account (switches to another if present, else to the login screen). */
    fun logoutActive() {
        val active = accountStore.activeAccount.value ?: return
        removeAccount(active.id)
    }

    /**
     * Syncs the active account's cached name/avatar to fresh values (e.g. whenever the user's own
     * profile loads), so the account switcher and AniList settings reflect the current picture even
     * after it's changed on AniList. No-ops if unchanged.
     */
    fun updateActiveDetails(name: String, avatarUrl: String?) =
        accountStore.updateActiveDetails(name, avatarUrl)

    /**
     * Startup reconcile (same session, so it does NOT bump [sessionEpoch]):
     *  - If the active account is a migrated legacy login (provisional id), resolve its real
     *    identity and promote it.
     *  - Claim any legacy library rows written before the per-account `ownerId` existed (they
     *    default to [Account.PROVISIONAL_ID] after the v18 migration) for the active account, so
     *    an upgrading user's existing library isn't stranded under owner 0.
     */
    suspend fun reconcileActiveAccount() {
        val active = accountStore.activeAccount.value ?: return
        val realId = if (active.isProvisional) {
            val resolved = resolveAccount(active.token, expiresInSeconds = 0L) ?: return
            accountStore.reconcileProvisional(resolved)
            resolved.id
        } else {
            active.id
        }
        if (realId > 0) {
            runCatching { libraryDao.reassignOwner(Account.PROVISIONAL_ID, realId) }
        }
    }

    private fun bumpEpoch() {
        _sessionEpoch.value += 1
    }

    private suspend fun <T> busy(block: suspend () -> T): T {
        _isBusy.value = true
        return try {
            block()
        } finally {
            _isBusy.value = false
        }
    }

    /**
     * Identity resolution: runs `GetViewer` with the candidate token on a throwaway client that has
     * no shared interceptor (no duplicate Authorization header) and no normalized cache (no pollution
     * of the active account's cache).
     */
    private suspend fun resolveAccount(token: String, expiresInSeconds: Long): Account? {
        val client = tokenedClientFactory.create(token)
        return try {
            val viewer = client.query(GetViewerQuery()).execute().data?.Viewer ?: return null
            Account(
                id = viewer.id,
                name = viewer.name ?: "",
                avatarUrl = viewer.avatar?.large,
                expiresAt = if (expiresInSeconds > 0) {
                    System.currentTimeMillis() + (expiresInSeconds - ONE_DAY_SECONDS) * 1000L
                } else {
                    0L
                },
                token = token,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clears the cross-account caches on switch. Library and own-profile are NOT wiped — they are
     * account-scoped in Room (by ownerId / user id) so each account's data persists and shows
     * instantly on switch-back. The Apollo cache is cleared to avoid the no-variable `GetViewer`
     * entry bleeding the wrong identity; library/profile read from Room so that's harmless.
     */
    private suspend fun clearLocalState() {
        withContext(Dispatchers.IO) {
            runCatching { apolloClient.apolloStore.clearAll() }
            savedForumThreadDao.deleteAll()
            airingScheduleDao.clearAll()
            // Notification dedup is per-account now (kept across switches) — not wiped here.
            appSettings.clearAccountScoped()
            activityRepository.clearViewerCache()
            notificationBadgeStore.reset()
        }
    }

    private suspend fun refreshWidgets() {
        runCatching {
            UpNextWidget().updateAll(context)
            AiringTodayWidget().updateAll(context)
            WeeklyCalendarWidget().updateAll(context)
        }
    }

    companion object {
        private const val ONE_DAY_SECONDS = 86_400L
    }
}
