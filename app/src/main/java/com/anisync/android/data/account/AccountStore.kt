@file:Suppress("DEPRECATION")

package com.anisync.android.data.account

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for signed-in accounts and which one is active.
 *
 * Storage split:
 *  - **Tokens** live in EncryptedSharedPreferences (`auth_prefs`), one key per account
 *    (`auth_token_<id>`). Secrets stay encrypted.
 *  - **Metadata** (id / name / avatar / expiry) and the active id live in a plain prefs file
 *    (`anisync_accounts`) as JSON, so the account list renders without decrypting or hitting network.
 *
 * Pure persistence — no network, no Apollo, no Room. That keeps it free of the
 * ApolloClient → AuthorizationInterceptor → AuthRepository dependency chain, so [AuthRepository]
 * can depend on it without a DI cycle. Network/cache orchestration lives in [AccountManager].
 */
@Singleton
class AccountStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val secure: SharedPreferences = createSecurePrefs()
    private val metaPrefs: SharedPreferences =
        context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _activeAccount = MutableStateFlow<Account?>(null)
    val activeAccount: StateFlow<Account?> = _activeAccount.asStateFlow()

    private var activeId: Int? = null

    init {
        load()
    }

    /** Synchronous active-token read for [AuthorizationInterceptor]. */
    fun activeToken(): String? = _activeAccount.value?.token

    /**
     * Inserts a new account or replaces an existing one with the same id (re-auth). Does not change
     * which account is active — callers switch explicitly via [switchTo].
     */
    fun addOrReplace(account: Account) {
        secure.edit().putString(tokenKey(account.id), account.token).apply()
        val list = _accounts.value.toMutableList()
        val idx = list.indexOfFirst { it.id == account.id }
        if (idx >= 0) list[idx] = account else list.add(account)
        _accounts.value = list
        persistMeta()
        recompute()
    }

    /** Sets the active account. `null` means "no active account" (drops to login/picker). */
    fun switchTo(id: Int?) {
        if (id != null && _accounts.value.none { it.id == id }) return
        activeId = id
        metaPrefs.edit().putInt(KEY_ACTIVE_ID, id ?: NO_ACTIVE).apply()
        recompute()
    }

    /**
     * Removes an account and its token. Refuses to remove the **active** account — the caller must
     * [switchTo] another (or `null`) first, so the app never ends up active-on-a-deleted-account.
     */
    fun remove(id: Int) {
        if (id == activeId) return
        secure.edit().remove(tokenKey(id)).apply()
        _accounts.value = _accounts.value.filterNot { it.id == id }
        persistMeta()
    }

    /** Marks the active account's token expired and clears the active slot (used on HTTP 401). */
    fun markActiveExpired() {
        val active = _activeAccount.value ?: return
        markExpired(active.id)
    }

    /**
     * Marks a specific account's token expired. If it is the active account, also clears the active
     * slot (drops to login/picker). A non-active account is just flagged — the active session is
     * untouched (used by the background notification worker on a per-account 401).
     */
    fun markExpired(id: Int) {
        val account = _accounts.value.firstOrNull { it.id == id } ?: return
        replaceById(account.copy(expiresAt = 1L))
        if (id == activeId) switchTo(null)
    }

    /** Updates the cached name/avatar of the active account (e.g. after an Edit Profile save). */
    fun updateActiveDetails(name: String, avatarUrl: String?) {
        val active = _activeAccount.value ?: return
        if (active.name == name && active.avatarUrl == avatarUrl) return
        replaceById(active.copy(name = name, avatarUrl = avatarUrl))
    }

    /**
     * Promotes the migrated provisional account (id [Account.PROVISIONAL_ID]) to its real identity,
     * re-keying the stored token from the placeholder id to the real one.
     */
    fun reconcileProvisional(real: Account) {
        val active = _activeAccount.value ?: return
        if (!active.isProvisional) return
        // A real account with this id already exists (rare): just drop the provisional.
        secure.edit()
            .remove(tokenKey(Account.PROVISIONAL_ID))
            .putString(tokenKey(real.id), real.token)
            .apply()
        val list = _accounts.value
            .filterNot { it.id == Account.PROVISIONAL_ID || it.id == real.id }
            .toMutableList()
        list.add(real)
        _accounts.value = list
        activeId = real.id
        metaPrefs.edit().putInt(KEY_ACTIVE_ID, real.id).apply()
        persistMeta()
        recompute()
    }

    private fun replaceById(updated: Account) {
        if (updated.token.isNotEmpty()) {
            secure.edit().putString(tokenKey(updated.id), updated.token).apply()
        }
        _accounts.value = _accounts.value.map { if (it.id == updated.id) updated else it }
        persistMeta()
        recompute()
    }

    private fun recompute() {
        _activeAccount.value = _accounts.value.firstOrNull { it.id == activeId }
    }

    private fun persistMeta() {
        val metas = _accounts.value.map { AccountMeta(it.id, it.name, it.avatarUrl, it.expiresAt) }
        metaPrefs.edit().putString(KEY_ACCOUNTS, json.encodeToString(metas)).apply()
    }

    private fun load() {
        migrateLegacyTokenIfNeeded()

        val metasJson = metaPrefs.getString(KEY_ACCOUNTS, null)
        val metas = if (metasJson != null) {
            runCatching { json.decodeFromString<List<AccountMeta>>(metasJson) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        // Drop any metadata whose token is missing (e.g. the encrypted store was reset).
        val loaded = metas.mapNotNull { m ->
            val token = secure.getString(tokenKey(m.id), null) ?: return@mapNotNull null
            Account(m.id, m.name, m.avatarUrl, m.expiresAt, token)
        }
        _accounts.value = loaded

        val storedActive = metaPrefs.getInt(KEY_ACTIVE_ID, NO_ACTIVE)
            .takeIf { it != NO_ACTIVE && loaded.any { a -> a.id == it } }
        // Can't start active on an expired token — force re-auth but keep the account listed.
        activeId = storedActive?.takeUnless { id -> loaded.first { it.id == id }.isExpired }

        recompute()
        // Rewrite metadata in case pruning removed orphaned entries.
        if (loaded.size != metas.size) persistMeta()
    }

    /**
     * Migrates the pre-multi-account single token (`auth_prefs/access_token`) into a provisional
     * account so existing users are not logged out. The real identity is resolved later.
     */
    private fun migrateLegacyTokenIfNeeded() {
        val legacy = secure.getString(LEGACY_TOKEN_KEY, null) ?: return
        if (metaPrefs.contains(KEY_ACCOUNTS)) {
            // Already on the new format; just retire the stale legacy key.
            secure.edit().remove(LEGACY_TOKEN_KEY).apply()
            return
        }
        secure.edit()
            .putString(tokenKey(Account.PROVISIONAL_ID), legacy)
            .remove(LEGACY_TOKEN_KEY)
            .apply()
        val meta = listOf(AccountMeta(Account.PROVISIONAL_ID, "", null, 0L))
        metaPrefs.edit()
            .putString(KEY_ACCOUNTS, json.encodeToString(meta))
            .putInt(KEY_ACTIVE_ID, Account.PROVISIONAL_ID)
            .apply()
    }

    // ── Encrypted store creation (with keystore-invalidation recovery) ──────────────────────────

    private fun buildMasterKey(): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private fun createSecurePrefs(): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS,
                buildMasterKey(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ).also { it.contains(LEGACY_TOKEN_KEY) } // force decrypt to surface AEADBadTagException now
        } catch (t: Throwable) {
            // Keystore key invalidated (OS upgrade, backup/restore, lockscreen change, etc.).
            // Wipe corrupt prefs + master key alias, then recreate. Tokens are lost; user re-logs in.
            Log.w(TAG, "EncryptedSharedPreferences unreadable, resetting", t)
            resetEncryptedStore()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS,
                buildMasterKey(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }

    private fun resetEncryptedStore() {
        runCatching { context.deleteSharedPreferences(SECURE_PREFS) }
        runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            if (ks.containsAlias(MASTER_KEY_ALIAS)) ks.deleteEntry(MASTER_KEY_ALIAS)
        }
    }

    @Serializable
    private data class AccountMeta(
        val id: Int,
        val name: String,
        val avatarUrl: String?,
        val expiresAt: Long,
    )

    companion object {
        private const val TAG = "AccountStore"
        private const val SECURE_PREFS = "auth_prefs"
        private const val META_PREFS = "anisync_accounts"
        private const val MASTER_KEY_ALIAS = MasterKey.DEFAULT_MASTER_KEY_ALIAS
        private const val LEGACY_TOKEN_KEY = "access_token"
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_ACTIVE_ID = "active_id"
        private const val NO_ACTIVE = Int.MIN_VALUE

        private fun tokenKey(id: Int) = "auth_token_$id"
    }
}
