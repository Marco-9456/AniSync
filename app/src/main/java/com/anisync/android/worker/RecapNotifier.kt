package com.anisync.android.worker

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil3.ImageLoader
import com.anisync.android.MainActivity
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import com.anisync.android.data.RecapDelivery
import com.anisync.android.data.RecapNotices
import com.anisync.android.data.account.AccountStore
import com.anisync.android.data.util.AppLocale
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.presentation.recap.RecapCardFactory
import com.anisync.android.util.getQuantityStringOrDefault
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Announces recaps the job built, once each: a big picture notification on the Recaps channel with
 * Watch and Share card, when the delivery setting asks for one, Allow Notifications is on and the
 * system lets the app post. The Library banner still shows when it is off.
 */
class RecapNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val cards: RecapCardFactory,
    private val notices: RecapNotices,
    private val appSettings: AppSettings,
    private val accountStore: AccountStore,
) {
    private val resources get() = AppLocale.wrap(context).resources
    private val locale: Locale get() = resources.configuration.locales[0] ?: Locale.getDefault()

    suspend fun announce(ownerId: Int, recaps: List<StoredRecap>) {
        if (appSettings.recapDelivery.value != RecapDelivery.NOTIFICATION_AND_BANNER) return
        if (!appSettings.notificationsEnabled.value) return
        if (!canPost()) return
        val fresh = recaps.filterNot { notices.wasNotified(ownerId, it.period) }
        if (fresh.isEmpty()) return
        // On 1 January the year comes before December, as the banner orders them.
        val posted = fresh.sortedBy { if (it.period is RecapPeriod.Year) 0 else 1 }.filter { post(ownerId, it) }
        notices.markNotified(ownerId, posted.map { it.period })
    }

    private fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private suspend fun post(ownerId: Int, stored: StoredRecap): Boolean = try {
        val resources = resources
        val recap = stored.recap
        val titles = cards.titles(stored)
        val period = stored.period
        val name = when (period) {
            is RecapPeriod.Month -> period.month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            is RecapPeriod.Year -> period.year.toString()
        }
        val episodes = resources.getQuantityStringOrDefault(
            context, R.plurals.share_recap_episode_count, recap.episodes, formatExactCount(recap.episodes, locale),
        )
        val titleCount = resources.getQuantityStringOrDefault(
            context, R.plurals.share_stats_title_count, recap.titles, formatExactCount(recap.titles, locale),
        )
        val top = recap.ranked.firstOrNull()?.let { titles[it.mediaId] }
        val text = if (top != null) {
            resources.getString(R.string.recap_notification_text, episodes, titleCount, top.title)
        } else {
            resources.getString(R.string.recap_notification_text_plain, episodes, titleCount)
        }
        val art = RecapNotificationArt.render(
            context,
            imageLoader,
            RecapArtSpec(
                headline = name,
                subline = (period as? RecapPeriod.Month)?.month?.year?.toString(),
                seed = top?.color,
                coverUrl = top?.coverUrl,
                secondCoverUrl = recap.ranked.getOrNull(1)?.let { titles[it.mediaId]?.coverUrl },
            ),
        )
        val watch = deepLink("anisync://recap/${period.key}", ownerId, requestCode(period, 0))
        val share = deepLink("anisync://studio/RECAP/0?periodKey=${period.key}", ownerId, requestCode(period, 1))
        val builder = NotificationCompat.Builder(context, NotificationChannels.RECAPS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(resources.getString(R.string.recap_notification_title, name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(art).setSummaryText(text))
            .setAutoCancel(true)
            .setContentIntent(watch)
            .addAction(0, resources.getString(R.string.recap_watch), watch)
            .addAction(0, resources.getString(R.string.recap_notification_share), share)
        if (accountStore.accounts.value.size > 1) {
            accountStore.accounts.value.firstOrNull { it.id == ownerId }?.let { builder.setSubText(it.name) }
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(tag(ownerId), period.key.hashCode(), builder.build())
        true
    } catch (e: SecurityException) {
        Log.w(TAG, "Recap notification refused", e)
        false
    }

    /** Takes a period's notification down once its story has been opened, from the notification or not. */
    fun dismiss(ownerId: Int, period: RecapPeriod) {
        NotificationManagerCompat.from(context).cancel(tag(ownerId), period.key.hashCode())
    }

    private fun tag(ownerId: Int) = "acct_${ownerId}_recap"

    /** Explicit, so a second installed build cannot turn the tap into a chooser. */
    private fun deepLink(uri: String, ownerId: Int, requestCode: Int): PendingIntent {
        val withAccount = if (uri.contains('?')) "$uri&account=$ownerId" else "$uri?account=$ownerId"
        val intent = Intent(Intent.ACTION_VIEW, withAccount.toUri(), context, MainActivity::class.java)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun requestCode(period: RecapPeriod, action: Int): Int = period.key.hashCode() * 4 + action

    private companion object {
        const val TAG = "RecapNotifier"
    }
}
