package com.anisync.android.data

import com.anisync.android.data.account.AccountStore
import com.anisync.android.domain.recap.RecapSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import javax.inject.Inject

/** What a recap build reads from outside Room: whose recaps they are, what counts, and the time. */
interface RecapEnvironment {
    /** The active account's id, null while nobody is signed in. */
    fun owner(): Int?

    val owners: Flow<Int?>

    fun currentSettings(): RecapSettings

    val settings: Flow<RecapSettings>

    fun keptMonths(): Int

    /** Read on every call, so a time zone change applies to the next build. */
    fun zone(): ZoneId

    fun nowMillis(): Long
}

class AppRecapEnvironment @Inject constructor(
    private val accountStore: AccountStore,
    private val appSettings: AppSettings,
) : RecapEnvironment {

    override fun owner(): Int? = accountStore.activeAccount.value?.id?.takeIf { it > 0 }

    override val owners: Flow<Int?> =
        accountStore.activeAccount.map { account -> account?.id?.takeIf { it > 0 } }.distinctUntilChanged()

    override fun currentSettings() = RecapSettings(
        includeManga = appSettings.recapIncludeManga.value,
        countRepeats = appSettings.recapCountRepeats.value,
        showAdult = appSettings.showAdultContent.value || appSettings.recapShowAdult.value,
    )

    override val settings: Flow<RecapSettings> = combine(
        appSettings.recapIncludeManga,
        appSettings.recapCountRepeats,
        appSettings.showAdultContent,
        appSettings.recapShowAdult,
    ) { manga, repeats, adult, recapAdult -> RecapSettings(manga, repeats, adult || recapAdult) }

    override fun keptMonths(): Int = appSettings.recapKeptMonths.value

    override fun zone(): ZoneId = ZoneId.systemDefault()

    override fun nowMillis(): Long = System.currentTimeMillis()
}
