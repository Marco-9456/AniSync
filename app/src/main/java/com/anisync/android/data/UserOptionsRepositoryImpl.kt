package com.anisync.android.data

import android.content.Context
import com.anisync.android.GetViewerQuery
import com.anisync.android.UpdateUserOptionsMutation
import com.anisync.android.data.account.AccountManager
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.AniListListActivityStatus
import com.anisync.android.domain.AniListStaffNameLanguage
import com.anisync.android.domain.AniListTitleLanguage
import com.anisync.android.domain.AniListUserOptions
import com.anisync.android.domain.ConflictState
import com.anisync.android.domain.LocalOptionsState
import com.anisync.android.domain.Result
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.UserOptionField
import com.anisync.android.domain.UserOptionsPatch
import com.anisync.android.domain.UserOptionsRepository
import com.anisync.android.domain.applyPatch
import com.anisync.android.domain.affectedFields
import com.anisync.android.domain.differingFields
import com.anisync.android.domain.patchForFields
import com.anisync.android.domain.takeFields
import com.anisync.android.worker.UserOptionsFlushWorker
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import com.anisync.android.type.MediaListStatus as ApiListStatus
import com.anisync.android.type.ScoreFormat as ApiScoreFormat
import com.anisync.android.type.UserStaffNameLanguage as ApiStaffNameLanguage
import com.anisync.android.type.UserTitleLanguage as ApiTitleLanguage

@Singleton
class UserOptionsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val store: UserOptionsStore,
    private val accountManager: AccountManager,
    private val appSettings: AppSettings,
    @ApplicationContext private val context: Context,
) : UserOptionsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flushMutex = Mutex()
    private var debounceJob: Job? = null

    private val _state = MutableStateFlow(currentAccountId()?.let { store.read(it) })

    override val cachedOptions: StateFlow<AniListUserOptions?> =
        _state.map { it?.local }.stateIn(scope, SharingStarted.Eagerly, _state.value?.local)

    override val conflict: StateFlow<ConflictState?> =
        _state.map { it?.conflict }.stateIn(scope, SharingStarted.Eagerly, _state.value?.conflict)

    init {
        // On account switch, load that account's cached state (offline) and re-mirror immediately.
        // A fresh network pull is driven separately by UserOptionsSyncManager.
        scope.launch {
            accountManager.activeAccount
                .map { it?.id }
                .distinctUntilChanged()
                .collect { id ->
                    val loaded = id?.let { store.read(it) }
                    _state.value = loaded
                    loaded?.local?.let { mirror(it) }
                }
        }
    }

    override suspend fun pull(): Result<Unit> = safeApiCall {
        val accountId = currentAccountId() ?: throw Exception("Not signed in")
        val response = apolloClient.query(GetViewerQuery())
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to load account options")
        }
        val fresh = response.data?.Viewer?.toDomain() ?: throw Exception("Not signed in")
        reconcile(accountId, fresh)
    }

    /**
     * Merge a fresh server snapshot into local state. Clean (non-dirty) fields adopt the server value;
     * a dirty field whose server value changed since our baseline becomes a conflict (local value is
     * kept until the user resolves it).
     */
    private fun reconcile(accountId: Int, fresh: AniListUserOptions) {
        val current = _state.value ?: LocalOptionsState()
        val all = UserOptionField.entries.toSet()
        // Dirty edits still genuinely pending: the local value disagrees with the fresh server value.
        // A dirty field the server already agrees with isn't pending (and isn't a conflict).
        val pending = current.dirty intersect current.local.differingFields(fresh)
        // A conflict is a pending edit where the server also moved since our baseline.
        val conflictFields = pending intersect current.serverSnapshot.differingFields(fresh)
        // Everything not pending adopts the server value (clean fields + converged dirty edits).
        val adopt = all - pending

        commit(
            accountId,
            current.copy(
                local = current.local.takeFields(adopt, fresh),
                // Baseline advances to the server everywhere except unresolved conflicts.
                serverSnapshot = current.serverSnapshot.takeFields(all - conflictFields, fresh),
                dirty = pending,
                conflict = if (conflictFields.isNotEmpty()) ConflictState(conflictFields, fresh) else null,
            ),
        )
    }

    override fun applyEdit(patch: UserOptionsPatch) {
        val accountId = currentAccountId() ?: return
        val current = _state.value ?: LocalOptionsState()
        commit(
            accountId,
            current.copy(
                local = current.local.applyPatch(patch),
                dirty = current.dirty + patch.affectedFields(),
            ),
        )
        scheduleFlush()
    }

    override suspend fun flush() {
        flushMutex.withLock {
            val accountId = currentAccountId() ?: return
            if ((_state.value?.dirty).isNullOrEmpty()) return

            // Refresh from the server first so a concurrent web change is detected as a conflict
            // rather than silently overwritten. Offline → keep the pending edits and retry later.
            if (pull() is Result.Error) return

            val state = _state.value ?: return
            val pushFields = state.dirty - (state.conflict?.fields ?: emptySet())
            if (pushFields.isEmpty()) return

            val patch = state.local.patchForFields(pushFields)
            val response = apolloClient.mutation(patch.toMutation()).execute()
            if (response.hasErrors()) return // keep dirty; a later flush retries
            val updated = response.data?.UpdateUser?.toDomain() ?: return

            commit(
                accountId,
                state.copy(
                    serverSnapshot = state.serverSnapshot.takeFields(pushFields, updated),
                    dirty = state.dirty - pushFields,
                ),
            )
        }
    }

    override fun resolveConflict(keepLocal: Boolean) {
        val accountId = currentAccountId() ?: return
        val state = _state.value ?: return
        val conflict = state.conflict ?: return

        val resolved = if (keepLocal) {
            // Re-baseline the conflicting fields to the server value so the next flush pushes local.
            state.copy(
                serverSnapshot = state.serverSnapshot.takeFields(conflict.fields, conflict.serverValues),
                conflict = null,
            )
        } else {
            // Adopt the website's values and drop the local edits for those fields.
            state.copy(
                local = state.local.takeFields(conflict.fields, conflict.serverValues),
                serverSnapshot = state.serverSnapshot.takeFields(conflict.fields, conflict.serverValues),
                dirty = state.dirty - conflict.fields,
                conflict = null,
            )
        }
        commit(accountId, resolved)
        if (keepLocal) scheduleFlush()
    }

    /** Persist, publish, and mirror behavior-affecting values into [AppSettings]. */
    private fun commit(accountId: Int, state: LocalOptionsState) {
        store.write(accountId, state)
        _state.value = state
        mirror(state.local)
    }

    private fun scheduleFlush() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(FLUSH_DEBOUNCE_MS)
            flush()
        }
        // Safety net: survives process death and waits for connectivity.
        UserOptionsFlushWorker.enqueue(context)
    }

    /** The options that drive local app behavior always follow the local working copy. */
    private fun mirror(local: AniListUserOptions) {
        appSettings.setShowAdultContent(local.displayAdultContent)
        local.titleLanguage?.let { appSettings.setTitleLanguage(it.toLocalTitleLanguage()) }
        local.staffNameLanguage?.let { appSettings.setStaffNameLanguage(it.toLocalStaffNameLanguage()) }
        local.scoreFormat?.let { appSettings.setUserScoreFormat(it) }
    }

    private fun currentAccountId(): Int? = accountManager.activeAccount.value?.id

    // ── Mapping: generated GraphQL types → domain ────────────────────────────────────────────────

    private fun GetViewerQuery.Viewer.toDomain(): AniListUserOptions = buildOptions(
        titleLanguage = options?.titleLanguage,
        displayAdultContent = options?.displayAdultContent,
        airingNotifications = options?.airingNotifications,
        profileColor = options?.profileColor,
        timezone = options?.timezone,
        activityMergeTime = options?.activityMergeTime,
        staffNameLanguage = options?.staffNameLanguage,
        restrictMessagesToFollowing = options?.restrictMessagesToFollowing,
        disabledListActivity = options?.disabledListActivity?.mapNotNull { it?.type to (it?.disabled ?: false) },
        scoreFormat = mediaListOptions?.scoreFormat,
    )

    private fun UpdateUserOptionsMutation.UpdateUser.toDomain(): AniListUserOptions = buildOptions(
        titleLanguage = options?.titleLanguage,
        displayAdultContent = options?.displayAdultContent,
        airingNotifications = options?.airingNotifications,
        profileColor = options?.profileColor,
        timezone = options?.timezone,
        activityMergeTime = options?.activityMergeTime,
        staffNameLanguage = options?.staffNameLanguage,
        restrictMessagesToFollowing = options?.restrictMessagesToFollowing,
        disabledListActivity = options?.disabledListActivity?.mapNotNull { it?.type to (it?.disabled ?: false) },
        scoreFormat = mediaListOptions?.scoreFormat,
    )

    private fun buildOptions(
        titleLanguage: ApiTitleLanguage?,
        displayAdultContent: Boolean?,
        airingNotifications: Boolean?,
        profileColor: String?,
        timezone: String?,
        activityMergeTime: Int?,
        staffNameLanguage: ApiStaffNameLanguage?,
        restrictMessagesToFollowing: Boolean?,
        disabledListActivity: List<Pair<ApiListStatus?, Boolean>>?,
        scoreFormat: ApiScoreFormat?,
    ): AniListUserOptions = AniListUserOptions(
        titleLanguage = titleLanguage.toDomain(),
        displayAdultContent = displayAdultContent ?: false,
        airingNotifications = airingNotifications ?: true,
        profileColor = profileColor,
        timezone = timezone,
        activityMergeTime = activityMergeTime,
        staffNameLanguage = staffNameLanguage.toDomain(),
        restrictMessagesToFollowing = restrictMessagesToFollowing ?: false,
        disabledListActivity = disabledListActivity
            ?.mapNotNull { (status, disabled) -> status.toDomain()?.let { it to disabled } }
            ?.toMap()
            .orEmpty(),
        scoreFormat = scoreFormat.toDomain(),
    )

    private fun UserOptionsPatch.toMutation(): UpdateUserOptionsMutation = UpdateUserOptionsMutation(
        displayAdultContent = displayAdultContent.toOptional(),
        titleLanguage = (titleLanguage?.toApi()).toOptional(),
        staffNameLanguage = (staffNameLanguage?.toApi()).toOptional(),
        scoreFormat = (scoreFormat?.toApi()).toOptional(),
        airingNotifications = airingNotifications.toOptional(),
        restrictMessagesToFollowing = restrictMessagesToFollowing.toOptional(),
        activityMergeTime = activityMergeTime.toOptional(),
        profileColor = profileColor.toOptional(),
        disabledListActivity = disabledListActivity?.map { (status, disabled) ->
            com.anisync.android.type.ListActivityOptionInput(
                disabled = Optional.present(disabled),
                type = Optional.present(status.toApi()),
            )
        }.toOptional(),
    )

    private companion object {
        const val FLUSH_DEBOUNCE_MS = 5_000L
    }
}

private fun <T : Any> T?.toOptional(): Optional<T> =
    if (this != null) Optional.present(this) else Optional.absent()

private fun ApiTitleLanguage?.toDomain(): AniListTitleLanguage? = when (this) {
    ApiTitleLanguage.ROMAJI -> AniListTitleLanguage.ROMAJI
    ApiTitleLanguage.ENGLISH -> AniListTitleLanguage.ENGLISH
    ApiTitleLanguage.NATIVE -> AniListTitleLanguage.NATIVE
    ApiTitleLanguage.ROMAJI_STYLISED -> AniListTitleLanguage.ROMAJI_STYLISED
    ApiTitleLanguage.ENGLISH_STYLISED -> AniListTitleLanguage.ENGLISH_STYLISED
    ApiTitleLanguage.NATIVE_STYLISED -> AniListTitleLanguage.NATIVE_STYLISED
    else -> null
}

private fun AniListTitleLanguage.toApi(): ApiTitleLanguage = when (this) {
    AniListTitleLanguage.ROMAJI -> ApiTitleLanguage.ROMAJI
    AniListTitleLanguage.ENGLISH -> ApiTitleLanguage.ENGLISH
    AniListTitleLanguage.NATIVE -> ApiTitleLanguage.NATIVE
    AniListTitleLanguage.ROMAJI_STYLISED -> ApiTitleLanguage.ROMAJI_STYLISED
    AniListTitleLanguage.ENGLISH_STYLISED -> ApiTitleLanguage.ENGLISH_STYLISED
    AniListTitleLanguage.NATIVE_STYLISED -> ApiTitleLanguage.NATIVE_STYLISED
}

private fun ApiStaffNameLanguage?.toDomain(): AniListStaffNameLanguage? = when (this) {
    ApiStaffNameLanguage.ROMAJI_WESTERN -> AniListStaffNameLanguage.ROMAJI_WESTERN
    ApiStaffNameLanguage.ROMAJI -> AniListStaffNameLanguage.ROMAJI
    ApiStaffNameLanguage.NATIVE -> AniListStaffNameLanguage.NATIVE
    else -> null
}

private fun AniListStaffNameLanguage.toApi(): ApiStaffNameLanguage = when (this) {
    AniListStaffNameLanguage.ROMAJI_WESTERN -> ApiStaffNameLanguage.ROMAJI_WESTERN
    AniListStaffNameLanguage.ROMAJI -> ApiStaffNameLanguage.ROMAJI
    AniListStaffNameLanguage.NATIVE -> ApiStaffNameLanguage.NATIVE
}

private fun ApiScoreFormat?.toDomain(): ScoreFormat? = when (this) {
    ApiScoreFormat.POINT_100 -> ScoreFormat.POINT_100
    ApiScoreFormat.POINT_10_DECIMAL -> ScoreFormat.POINT_10_DECIMAL
    ApiScoreFormat.POINT_10 -> ScoreFormat.POINT_10
    ApiScoreFormat.POINT_5 -> ScoreFormat.POINT_5
    ApiScoreFormat.POINT_3 -> ScoreFormat.POINT_3
    else -> null
}

private fun ScoreFormat.toApi(): ApiScoreFormat = when (this) {
    ScoreFormat.POINT_100 -> ApiScoreFormat.POINT_100
    ScoreFormat.POINT_10_DECIMAL -> ApiScoreFormat.POINT_10_DECIMAL
    ScoreFormat.POINT_10 -> ApiScoreFormat.POINT_10
    ScoreFormat.POINT_5 -> ApiScoreFormat.POINT_5
    ScoreFormat.POINT_3 -> ApiScoreFormat.POINT_3
}

private fun ApiListStatus?.toDomain(): AniListListActivityStatus? = when (this) {
    ApiListStatus.CURRENT -> AniListListActivityStatus.CURRENT
    ApiListStatus.PLANNING -> AniListListActivityStatus.PLANNING
    ApiListStatus.COMPLETED -> AniListListActivityStatus.COMPLETED
    ApiListStatus.DROPPED -> AniListListActivityStatus.DROPPED
    ApiListStatus.PAUSED -> AniListListActivityStatus.PAUSED
    ApiListStatus.REPEATING -> AniListListActivityStatus.REPEATING
    else -> null
}

private fun AniListListActivityStatus.toApi(): ApiListStatus = when (this) {
    AniListListActivityStatus.CURRENT -> ApiListStatus.CURRENT
    AniListListActivityStatus.PLANNING -> ApiListStatus.PLANNING
    AniListListActivityStatus.COMPLETED -> ApiListStatus.COMPLETED
    AniListListActivityStatus.DROPPED -> ApiListStatus.DROPPED
    AniListListActivityStatus.PAUSED -> ApiListStatus.PAUSED
    AniListListActivityStatus.REPEATING -> ApiListStatus.REPEATING
}
