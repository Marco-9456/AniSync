package com.anisync.android.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.anisync.android.R

object NotificationChannels {
    const val AIRING_CHANNEL_ID = "airing_notifications"
    const val PLANNING_CHANNEL_ID = "planning_notifications"
    const val UPCOMING_CHANNEL_ID = "upcoming_notifications"
    const val THREAD_COMMENT_REPLY_CHANNEL_ID = "thread_comment_reply_notifications"
    const val THREAD_SUBSCRIBED_CHANNEL_ID = "thread_subscribed_notifications"
    const val THREAD_COMMENT_MENTION_CHANNEL_ID = "thread_comment_mention_notifications"
    const val THREAD_LIKE_CHANNEL_ID = "thread_like_notifications"
    const val THREAD_COMMENT_LIKE_CHANNEL_ID = "thread_comment_like_notifications"
    const val UPDATE_CHANNEL_ID = "update_notifications"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val airingChannel = NotificationChannel(
                AIRING_CHANNEL_ID,
                context.getString(R.string.notification_channel_watching),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_watching_desc)
            }

            val planningChannel = NotificationChannel(
                PLANNING_CHANNEL_ID,
                context.getString(R.string.notification_channel_planning),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_planning_desc)
            }

            val upcomingChannel = NotificationChannel(
                UPCOMING_CHANNEL_ID,
                context.getString(R.string.notification_channel_upcoming),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_upcoming_desc)
            }

            val threadCommentReplyChannel = NotificationChannel(
                THREAD_COMMENT_REPLY_CHANNEL_ID,
                context.getString(R.string.notification_channel_thread_comment_reply),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_thread_comment_reply_desc)
            }

            val threadSubscribedChannel = NotificationChannel(
                THREAD_SUBSCRIBED_CHANNEL_ID,
                context.getString(R.string.notification_channel_thread_subscribed),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_thread_subscribed_desc)
            }

            val threadCommentMentionChannel = NotificationChannel(
                THREAD_COMMENT_MENTION_CHANNEL_ID,
                context.getString(R.string.notification_channel_thread_comment_mention),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_thread_comment_mention_desc)
            }

            val threadLikeChannel = NotificationChannel(
                THREAD_LIKE_CHANNEL_ID,
                context.getString(R.string.notification_channel_thread_like),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_thread_like_desc)
            }

            val threadCommentLikeChannel = NotificationChannel(
                THREAD_COMMENT_LIKE_CHANNEL_ID,
                context.getString(R.string.notification_channel_thread_comment_like),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_thread_comment_like_desc)
            }

            val updateChannel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                context.getString(R.string.update_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.update_notification_channel_desc)
            }

            notificationManager.createNotificationChannels(
                listOf(
                    airingChannel,
                    planningChannel,
                    upcomingChannel,
                    threadCommentReplyChannel,
                    threadSubscribedChannel,
                    threadCommentMentionChannel,
                    threadLikeChannel,
                    threadCommentLikeChannel,
                    updateChannel
                )
            )
        }
    }
}
