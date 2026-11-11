package com.anisync.android.worker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.anisync.android.data.account.AccountStore
import com.anisync.android.data.account.TokenedApolloClientFactory
import com.anisync.android.data.listStatusMutation
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.R
import com.anisync.android.domain.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver that handles the "Add to Watching" action from planning notifications.
 * When a user taps the action button, this receiver updates the anime status to WATCHING
 * on AniList and dismisses the notification.
 */
@AndroidEntryPoint
class AddToWatchingReceiver : BroadcastReceiver() {

    @Inject
    lateinit var detailsRepository: DetailsRepository

    @Inject
    lateinit var libraryDao: LibraryDao

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var tokenedClients: TokenedApolloClientFactory

    companion object {
        const val ACTION_ADD_TO_WATCHING = "com.anisync.android.ACTION_ADD_TO_WATCHING"
        const val EXTRA_ACCOUNT_ID = "extra_account_id"
        const val EXTRA_MEDIA_ID = "extra_media_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_NOTIFICATION_TAG = "extra_notification_tag"
        const val EXTRA_MEDIA_TITLE = "extra_media_title"
        private const val TAG = "AddToWatchingReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ADD_TO_WATCHING) return

        val mediaId = intent.getIntExtra(EXTRA_MEDIA_ID, -1)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        // The worker posts under a per-account tag; cancelling without it never matches.
        val notificationTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG)
        val mediaTitle = intent.getStringExtra(EXTRA_MEDIA_TITLE) ?: "Anime"

        if (mediaId == -1) {
            Log.e(TAG, "Invalid mediaId received")
            return
        }

        // Dismiss the notification immediately
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationId != -1) {
            notificationManager.cancel(notificationTag, notificationId)
        }

        // Use goAsync() to extend the BroadcastReceiver's lifecycle for the coroutine
        val pendingResult = goAsync()

        val accountId = intent.getIntExtra(EXTRA_ACCOUNT_ID, NO_ACCOUNT)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = if (accountId == NO_ACCOUNT || accountId == accountStore.activeAccount.value?.id) {
                    detailsRepository.updateMediaListEntry(
                        mediaId = mediaId,
                        status = LibraryStatus.CURRENT
                    )
                } else {
                    addForAccount(accountId, mediaId)
                }

                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "Successfully added '$mediaTitle' to Watching list")
                        // Show success toast on main thread
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_added_to_watching, mediaTitle),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Failed to add '$mediaTitle' to Watching: ${result.message}", result.exception)
                        // Show error toast on main thread
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_update_failed_reason, result.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while adding to watching", e)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_update_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                // Signal that we're done with the broadcast
                pendingResult.finish()
            }
        }
    }

    /**
     * A notification stays in the tray across an account switch, so the action saves to the
     * account it was posted for rather than whichever one is active now.
     */
    private suspend fun addForAccount(accountId: Int, mediaId: Int): Result<Unit> {
        val account = accountStore.accounts.value.firstOrNull { it.id == accountId && !it.isExpired }
            ?: throw IllegalStateException("Account $accountId is signed out")
        return safeApiCall {
            val response = tokenedClients.create(account.token)
                .mutation(listStatusMutation(mediaId, LibraryStatus.CURRENT, progress = null))
                .execute()
            if (response.data?.SaveMediaListEntry == null || response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Sync failed")
            }
        }
    }
}

private const val NO_ACCOUNT = -1
