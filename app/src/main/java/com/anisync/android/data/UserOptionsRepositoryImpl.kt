package com.anisync.android.data

import com.anisync.android.GetViewerQuery
import com.anisync.android.UpdateUserOptionsMutation
import com.anisync.android.data.account.AccountManager
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.AniListListActivityStatus
import com.anisync.android.domain.AniListStaffNameLanguage
import com.anisync.android.domain.AniListTitleLanguage
import com.anisync.android.domain.AniListUserOptions
import com.anisync.android.domain.Result
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.UserOptionsPatch
import com.anisync.android.domain.UserOptionsRepository
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
) : UserOptionsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _cachedOptions = MutableStateFlow(currentAccountId()?.let { store.read(it) })
    override val cachedOptions: StateFlow<AniListUserOptions?> = _cachedOptions.asStateFlow()

    init {
        // On account switch, instantly surface that account's cached options (offline) and re-mirror,
        // so effective settings reflect the active account without waiting on the network. A fresh
        // network pull is driven separately by UserOptionsSyncManager.
        scope.launch {
            accountManager.activeAccount
                .map { it?.id }
                .distinctUntilChanged()
                .collect { id ->
                    val cached = id?.let { store.read(it) }
                    _cachedOptions.value = cached
                    if (cached != null) mirrorToLocal(cached)
                }
        }
    }

    override suspend fun fetchOptions(): Result<AniListUserOptions> = safeApiCall {
        val response = apolloClient.query(GetViewerQuery())
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to load account options")
        }
        val viewer = response.data?.Viewer ?: throw Exception("Not signed in")
        viewer.toDomain().also { persist(it) }
    }

    override suspend fun updateOptions(patch: UserOptionsPatch): Result<AniListUserOptions> = safeApiCall {
        val response = apolloClient.mutation(patch.toMutation()).execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to update account options")
        }
        val user = response.data?.UpdateUser ?: throw Exception("Update returned no user")
        user.toDomain().also { persist(it) }
    }

    /** Persist to the per-account cache, update the live flow, and mirror behavior-affecting values. */
    private fun persist(options: AniListUserOptions) {
        currentAccountId()?.let { store.write(it, options) }
        _cachedOptions.value = options
        mirrorToLocal(options)
    }

    /**
     * Mirror the options that drive local app behavior into [AppSettings], honoring the device
     * override switches. Adult content and title language are skipped when their override is ON;
     * staff-name language and score format always follow the account (no local-only counterpart).
     */
    private fun mirrorToLocal(options: AniListUserOptions) {
        if (!appSettings.adultContentOverrideEnabled.value) {
            appSettings.setShowAdultContent(options.displayAdultContent)
        }
        if (!appSettings.titleLanguageOverrideEnabled.value) {
            options.titleLanguage?.let { appSettings.setTitleLanguage(it.toLocalTitleLanguage()) }
        }
        options.staffNameLanguage?.let { appSettings.setStaffNameLanguage(it.toLocalStaffNameLanguage()) }
        options.scoreFormat?.let { appSettings.setUserScoreFormat(it) }
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
