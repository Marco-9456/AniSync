package com.anisync.android.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.anisync.android.R
import com.anisync.android.data.NotificationBadgeStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug service for testing notification types without waiting for real events.
 * This service is only useful in debug builds for development and testing purposes.
 */
@Singleton
class NotificationDebugService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val notificationBadgeStore: NotificationBadgeStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val TAG = "NotificationDebug"
        
        // Debug notification IDs (separate range from real notifications)
        private const val DEBUG_WATCHING_ID = 900001
        private const val DEBUG_PLANNING_ID = 900002
        private const val DEBUG_ADVANCE_ID = 900003
        private const val DEBUG_IMMINENT_ID = 900004
        
        // Sample data for test notifications
        private const val SAMPLE_TITLE = "Solo Leveling"
        private const val SAMPLE_COVER_URL = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151807-m1gX3iwfIsLu.png"
        private const val SAMPLE_MEDIA_ID = 151807
        
        private const val GROUP_KEY_AIRING = "com.anisync.android.AIRING_GROUP"
        private const val GROUP_KEY_PLANNING = "com.anisync.android.PLANNING_GROUP"
    }

    /**
     * Send a test "Watching" notification.
     * Simulates: "Episode X has aired" for a show in the watching list.
     */
    fun sendTestWatchingNotification() {
        scope.launch {
            val notificationId = DEBUG_WATCHING_ID
            val title = SAMPLE_TITLE
            val episode = (1..24).random()
            val content = "Episode $episode has aired"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://notifications"))
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val largeIcon = loadImage(SAMPLE_COVER_URL)

            val builder = NotificationCompat.Builder(context, NotificationChannels.AIRING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_AIRING)
                .setContentIntent(pendingIntent)

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Sent test watching notification: $title - $content")
        }
    }

    /**
     * Send a test "Planning" notification.
     * Simulates: "Episode 1 is now available" for a show in the planning list.
     */
    fun sendTestPlanningNotification() {
        scope.launch {
            val notificationId = DEBUG_PLANNING_ID
            val title = SAMPLE_TITLE
            val content = "Episode 1 is now available"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://details/$SAMPLE_MEDIA_ID"))
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create "Add to Watching" action button
            val addToWatchingIntent = Intent(context, AddToWatchingReceiver::class.java).apply {
                action = AddToWatchingReceiver.ACTION_ADD_TO_WATCHING
                putExtra(AddToWatchingReceiver.EXTRA_MEDIA_ID, SAMPLE_MEDIA_ID)
                putExtra(AddToWatchingReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(AddToWatchingReceiver.EXTRA_MEDIA_TITLE, SAMPLE_TITLE)
            }
            val addToWatchingPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1000000,
                addToWatchingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val largeIcon = loadImage(SAMPLE_COVER_URL)

            val builder = NotificationCompat.Builder(context, NotificationChannels.PLANNING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_PLANNING)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_notification,
                    "Add to Watching",
                    addToWatchingPendingIntent
                )

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Sent test planning notification: $title - $content")
        }
    }

    /**
     * Send a test "Advance" notification (12h before premiere).
     * Simulates: "Episode 1 airs tomorrow at 3:00 PM"
     */
    fun sendTestAdvanceNotification() {
        scope.launch {
            val notificationId = DEBUG_ADVANCE_ID
            val title = SAMPLE_TITLE

            // Simulate airing time as tomorrow at a random afternoon hour
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, (14..20).random())
                set(Calendar.MINUTE, listOf(0, 30).random())
            }
            val airingDate = calendar.time
            
            val timeFormat = DateFormat.getTimeFormat(context)
            val formattedTime = timeFormat.format(airingDate)
            val content = "Episode 1 airs tomorrow at $formattedTime"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://details/$SAMPLE_MEDIA_ID"))
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val largeIcon = loadImage(SAMPLE_COVER_URL)

            val builder = NotificationCompat.Builder(context, NotificationChannels.UPCOMING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_PLANNING)
                .setContentIntent(pendingIntent)

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Sent test advance notification: $title - $content")
        }
    }

    /**
     * Send a test "Imminent" notification (2h or less before premiere).
     * Simulates: "Episode 1 airs in about 2 hours"
     */
    fun sendTestImminentNotification() {
        scope.launch {
            val notificationId = DEBUG_IMMINENT_ID
            val title = SAMPLE_TITLE

            val hoursUntil = (0..2).random()
            val content = when (hoursUntil) {
                0 -> "Episode 1 airs in less than an hour"
                1 -> "Episode 1 airs in about an hour"
                else -> "Episode 1 airs in about $hoursUntil hours"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("anisync://details/$SAMPLE_MEDIA_ID"))
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val largeIcon = loadImage(SAMPLE_COVER_URL)

            val builder = NotificationCompat.Builder(context, NotificationChannels.UPCOMING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_PLANNING)
                .setContentIntent(pendingIntent)

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Sent test imminent notification: $title - $content")
        }
    }

    /**
     * Simulate an unread inbox notification so the profile bell badge can
     * be tested without waiting for AniList to deliver a real one. The
     * server count is unaffected; the next viewer refresh will reconcile.
     */
    fun bumpInboxBadge() {
        notificationBadgeStore.bumpForDebug()
        Log.d(TAG, "Bumped inbox badge for debug")
    }

    /**
     * Clear all AniSync notifications (both real and debug).
     */
    fun clearAllNotifications() {
        notificationManager.cancelAll()
        Log.d(TAG, "Cleared all notifications")
    }

    private suspend fun loadImage(url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(256, 256)
            .allowHardware(false)
            .build()

        val result = imageLoader.execute(request)
        return if (result is SuccessResult) {
            result.drawable.toBitmap()
        } else {
            null
        }
    }
}
